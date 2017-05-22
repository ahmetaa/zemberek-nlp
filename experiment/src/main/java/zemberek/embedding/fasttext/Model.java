package zemberek.embedding.fasttext;

import zemberek.core.collections.IntVector;
import zemberek.core.logging.Log;

import java.io.DataInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

class Model {

    private static class Node {
        int parent;
        int left;
        int right;
        long count;
        boolean binary;
    }

    static class FloatIntPair implements Comparable<FloatIntPair> {

        final float first;
        final int second;

        FloatIntPair(float first, int second) {
            this.first = first;
            this.second = second;
        }

        @Override
        public int compareTo(FloatIntPair o) {
            // descending.
            return Float.compare(o.first, first);
        }
    }

    Matrix_ wi_;
    Matrix_ wo_;

    QMatrix qwi_;
    QMatrix qwo_;

    private Args args_;
    private Vector output_;
    private int hsz_;
    private int isz_;
    private int osz_;
    private float loss_;
    private long nexamples_;
    private static float[] t_sigmoid;
    private static float[] t_log;

    // used for negative sampling:
    private int[] negatives;
    private int negpos;

    // used for hierarchical softmax:
    private List<IntVector> paths = new ArrayList<>();
    private List<IntVector> codes = new ArrayList<>();
    private Node[] tree;

    boolean quant_;

    private static final int NEGATIVE_TABLE_SIZE = 10000000;

    private static final int SIGMOID_TABLE_SIZE = 512;
    private static final int MAX_SIGMOID = 8;
    private static final int LOG_TABLE_SIZE = 512;

    private Random rng;

    static {
        initLog();
        initSigmoid();
    }

    Model(Matrix_ wi,
          Matrix_ wo,
          Args args,
          int seed) {
        output_ = new Vector(wo.m_);
        rng = new Random(seed);
        wi_ = wi;
        wo_ = wo;
        args_ = args;
        isz_ = wi.m_;
        osz_ = wo.m_;
        hsz_ = args.dim;
        negpos = 0;
        loss_ = 0.0f;
        nexamples_ = 1;
    }

    Model(Model model, int seed) {
        this(model.wi_, model.wo_, model.args_, seed);
    }

    static Model load(DataInputStream dis, Args args_) throws IOException {

        Log.info("Loading Matrices.");
        boolean quant_input = dis.readBoolean();

        Matrix_ input_;
        Matrix_ output_;
        QMatrix qInput_ = null;
        QMatrix qOutput_ = null;

        if (quant_input) {
            qInput_ = new QMatrix();
            qInput_.load(dis);
            input_ = new Matrix_(); // empty
        } else {
            input_ = Matrix_.load(dis);
        }

        //TODO: I dont like this. we should not override Args value like this.
        args_.qout = dis.readBoolean();

        if (quant_input && args_.qout) {
            qOutput_ = new QMatrix();
            qOutput_.load(dis);
            output_ = new Matrix_(); // empty
        } else {
            output_ = Matrix_.load(dis);
        }


        Model model_ = new Model(input_, output_, args_, 0);
        model_.quant_ = quant_input;
        model_.setQuantizePointer(qInput_, qOutput_, args_.qout);

        return model_;
    }

    void setQuantizePointer(QMatrix qwi, QMatrix qwo, boolean qout) {
        qwi_ = qwi;
        qwo_ = qwo;
        if (qout) {
            osz_ = qwo_.getM();
        }
    }

    Random getRng() {
        return rng;
    }

    private float binaryLogistic(Vector grad, Vector hidden, int target, boolean label, float lr) {
        float score = sigmoid(wo_.dotRow(hidden, target));
        float alpha = lr * ((label ? 1f : 0f) - score);
        grad.addRow(wo_, target, alpha);
        wo_.addRow(hidden, target, alpha);
        if (label) {
            return -log(score);
        } else {
            return -log((float) (1.0 - score));
        }
    }

    private float negativeSampling(Vector grad, Vector hidden, int target, float lr) {
        float loss = 0.0f;
        for (int n = 0; n <= args_.neg; n++) {
            if (n == 0) {
                loss += binaryLogistic(grad, hidden, target, true, lr);
            } else {
                loss += binaryLogistic(grad, hidden, getNegative(target), false, lr);
            }
        }
        return loss;
    }

    private float hierarchicalSoftmax(Vector grad_, Vector hidden, int target, float lr) {
        float loss = 0.0f;
        IntVector binaryCode = codes.get(target);
        IntVector pathToRoot = paths.get(target);
        for (int i = 0; i < pathToRoot.size(); i++) {
            loss += binaryLogistic(grad_, hidden, pathToRoot.get(i), binaryCode.get(i) == 1, lr);
        }
        return loss;
    }

    private void computeOutputSoftmax(Vector hidden, Vector output) {
        if (quant_ && args_.qout) {
            output.mul(qwo_, hidden);
        } else {
            output.mul(wo_, hidden);
        }
        float max = output.data_[0], z = 0.0f;
        for (int i = 0; i < osz_; i++) {
            max = Math.max(output.data_[i], max);
        }
        for (int i = 0; i < osz_; i++) {
            output.data_[i] = (float) Math.exp(output.data_[i] - max);
            z += output.data_[i];
        }
        for (int i = 0; i < osz_; i++) {
            output.data_[i] /= z;
        }
    }

    private float softmax(Vector grad_, Vector hidden_, int target, float lr) {
        computeOutputSoftmax(hidden_, output_);
        for (int i = 0; i < osz_; i++) {
            float label = (i == target) ? 1.0f : 0.0f;
            float alpha = lr * (label - output_.data_[i]);
            grad_.addRow(wo_, i, alpha);
            wo_.addRow(hidden_, i, alpha);
        }
        return -log(output_.data_[target]);
    }

    Vector computeHidden(int[] input) {
        Vector hidden = new Vector(hsz_);
        for (int i : input) {
            if (quant_) {
                hidden.addRow(qwi_, i);
            } else {
                hidden.addRow(wi_, i);
            }
        }
        hidden.mul((float) (1.0 / input.length));
        return hidden;
    }

    private static final Comparator<FloatIntPair> PAIR_COMPARATOR =
            (l, r) -> Float.compare(l.first, r.first);

    List<FloatIntPair> predict(int k,
                               Vector hidden,
                               Vector output) {
        assert (k > 0);
        PriorityQueue<FloatIntPair> heap = new PriorityQueue<>(k + 1, PAIR_COMPARATOR);
        if (args_.loss == Args.loss_name.hs) {
            dfs(k, 2 * osz_ - 2, 0.0f, heap, hidden);
        } else {
            findKBest(k, heap, hidden, output);
        }
        List<FloatIntPair> result = new ArrayList<>(heap);
        Collections.sort(result);
        return result;
    }

    List<FloatIntPair> predict(int[] input, int k) {
        Vector hidden_ = computeHidden(input);
        return predict(k, hidden_, output_);
    }

    private void findKBest(int k,
                           PriorityQueue<FloatIntPair> heap,
                           Vector hidden,
                           Vector output) {
        computeOutputSoftmax(hidden, output);
        for (int i = 0; i < osz_; i++) {
            if (heap.size() == k && log(output.data_[i]) < heap.peek().first) {
                continue;
            }
            heap.add(new FloatIntPair(log(output.data_[i]), i));
            if (heap.size() > k) {
                heap.remove();
            }
        }
    }

    //todo: check here.
    private void dfs(int k,
                     int node,
                     float score,
                     PriorityQueue<FloatIntPair> heap,
                     Vector hidden) {
        if (heap.size() == k && score < heap.peek().first) {
            return;
        }

        if (tree[node].left == -1 && tree[node].right == -1) {
            heap.add(new FloatIntPair(score, node));
            if (heap.size() > k) {
                heap.remove();
            }
            return;
        }
        float f;
        if (quant_ && args_.qout) {
            f = sigmoid(qwo_.dotRow(hidden, node - osz_));
        } else {
            f = sigmoid(wo_.dotRow(hidden, node - osz_));
        }

        dfs(k, tree[node].left, score + log((float) (1.0 - f)), heap, hidden);
        dfs(k, tree[node].right, score + log(f), heap, hidden);
    }


    void update(int[] input, int target, float lr) {
        assert (target >= 0);
        assert (target < osz_);
        if (input.length == 0) return;
        Vector hidden_ = computeHidden(input);
        Vector grad_ = new Vector(hsz_);
        if (args_.loss == Args.loss_name.ns) {
            loss_ += negativeSampling(grad_, hidden_, target, lr);
        } else if (args_.loss == Args.loss_name.hs) {
            loss_ += hierarchicalSoftmax(grad_, hidden_, target, lr);
        } else {
            loss_ += softmax(grad_, hidden_, target, lr);
        }
        nexamples_ += 1;

        if (args_.model == Args.model_name.sup) {
            grad_.mul((float) (1.0 / input.length));
        }

        for (int i : input) {
            wi_.addRow(grad_, i, 1.0f);
        }
    }

    void setTargetCounts(long[] counts) {
        assert (counts.length == osz_);
        if (args_.loss == Args.loss_name.ns) {
            initTableNegatives(counts);
        }
        if (args_.loss == Args.loss_name.hs) {
            buildTree(counts);
        }
    }

    private void initTableNegatives(long[] counts) {
        IntVector vec = new IntVector(counts.length * 10);
        float z = 0.0f;
        for (long count : counts) {
            z += (float) Math.pow(count, 0.5);
        }
        for (int i = 0; i < counts.length; i++) {
            float c = (float) Math.pow(counts[i], 0.5);
            for (int j = 0; j < c * NEGATIVE_TABLE_SIZE / z; j++) {
                vec.add(i);
            }
        }
        vec.shuffle(rng);
        negatives = vec.copyOf();
    }

    private int getNegative(int target) {
        int negative;
        do {
            negative = negatives[negpos];
            negpos = (negpos + 1) % negatives.length;
        } while (target == negative);
        return negative;
    }

    private void buildTree(long[] counts) {
        tree = new Node[2 * osz_ - 1];
        for (int i = 0; i < 2 * osz_ - 1; i++) {
            tree[i] = new Node();
            tree[i].parent = -1;
            tree[i].left = -1;
            tree[i].right = -1;
            tree[i].count = (long) 1e15;
            tree[i].binary = false;
        }
        for (int i = 0; i < osz_; i++) {
            tree[i].count = counts[i];
        }
        int leaf = osz_ - 1;
        int node = osz_;
        for (int i = osz_; i < 2 * osz_ - 1; i++) {
            int[] mini = new int[2];
            for (int j = 0; j < 2; j++) {
                if (leaf >= 0 && tree[leaf].count < tree[node].count) {
                    mini[j] = leaf--;
                } else {
                    mini[j] = node++;
                }
            }
            tree[i].left = mini[0];
            tree[i].right = mini[1];
            tree[i].count = tree[mini[0]].count + tree[mini[1]].count;
            tree[mini[0]].parent = i;
            tree[mini[1]].parent = i;
            tree[mini[1]].binary = true;
        }
        for (int i = 0; i < osz_; i++) {
            IntVector path = new IntVector();
            IntVector code = new IntVector();
            int j = i;
            while (tree[j].parent != -1) {
                path.add(tree[j].parent - osz_);
                code.add(tree[j].binary ? 1 : 0);
                j = tree[j].parent;
            }
            paths.add(path);
            codes.add(code);
        }
    }

    float getLoss() {
        return loss_ / nexamples_;
    }

    private static void initSigmoid() {
        t_sigmoid = new float[SIGMOID_TABLE_SIZE + 1];
        for (int i = 0; i < SIGMOID_TABLE_SIZE + 1; i++) {
            float x = i * 2f * MAX_SIGMOID / SIGMOID_TABLE_SIZE - MAX_SIGMOID;
            t_sigmoid[i] = (float) (1.0d / (1.0d + (float) Math.exp(-x)));
        }
    }

    private static void initLog() {
        t_log = new float[LOG_TABLE_SIZE + 1];
        for (int i = 0; i < LOG_TABLE_SIZE + 1; i++) {
            float x = (i + 1e-5f) / LOG_TABLE_SIZE;
            t_log[i] = (float) Math.log(x);
        }
    }

    private float log(float x) {
        if (x > 1.0f) {
            return 0.0f;
        }
        int i = (int) (x * LOG_TABLE_SIZE);
        return t_log[i];
    }

    private float sigmoid(float x) {
        if (x < -MAX_SIGMOID) {
            return 0.0f;
        } else if (x > MAX_SIGMOID) {
            return 1.0f;
        } else {
            int i = (int) ((x + MAX_SIGMOID) * SIGMOID_TABLE_SIZE / MAX_SIGMOID / 2);
            return t_sigmoid[i];
        }
    }
}
