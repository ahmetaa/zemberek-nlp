package zemberek.embedding.fasttext;

import com.google.common.base.Stopwatch;
import zemberek.core.collections.IntVector;
import zemberek.core.io.IOUtil;
import zemberek.core.logging.Log;
import zemberek.core.text.BlockTextLoader;
import zemberek.core.text.TextIO;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

public class FastText {
    private Args args_;
    private Dictionary dict_;
    private Matrix input_;
    private Matrix output_;
    private Model model_;
    private AtomicLong tokenCount;

    private Stopwatch stopwatch;

    public FastText(Args args_, Dictionary dict_) {
        this.args_ = args_;
        this.dict_ = dict_;
    }

    // Sums all word and ngram vectors for a word and normalizes it.
    private Vector getVector(String word) {
        int[] ngrams = dict_.getNgrams(word);
        Vector vec = new Vector(args_.dim);
        for (int i : ngrams) {
            vec.addRow(input_, i);
        }
        if (ngrams.length > 0) {
            vec.mul(1.0f / ngrams.length);
        }
        return vec;
    }

    void saveVectors(Path outFilePath) throws IOException {
        try (PrintWriter pw = new PrintWriter(outFilePath.toFile(), "utf-8")) {
            pw.println(dict_.nwords() + " " + args_.dim);
            for (int i = 0; i < dict_.nwords(); i++) {
                String word = dict_.getWord(i);
                Vector vector = getVector(word);
                pw.println(word + " " + vector.asString());
            }
        }
    }

    void saveModel(Path outFilePath) throws IOException {
        try (DataOutputStream dos = IOUtil.getDataOutputStream(outFilePath)) {
            args_.save(dos);
            dict_.save(dos);
            input_.save(dos);
            output_.save(dos);
        }
    }

    void loadModel(Path path) throws IOException {
        try (DataInputStream dis = IOUtil.getDataInputStream(path)) {
            loadModel(dis);
        }
    }

    private void loadModel(DataInputStream dis) throws IOException {
        args_ = Args.load(dis);
        dict_ = Dictionary.load(dis, args_);
        input_ = Matrix.load(dis);
        output_ = Matrix.load(dis);
        model_ = new Model(input_, output_, args_, 0);
        if (args_.model == Args.model_name.sup) {
            model_.setTargetCounts(dict_.getCounts(Dictionary.TYPE_LABEL));
        } else {
            model_.setTargetCounts(dict_.getCounts(Dictionary.TYPE_WORD));
        }
        Log.info("Model loaded.");
    }

    private void printInfo(float progress, float loss) {
        float t = stopwatch.elapsed(TimeUnit.MILLISECONDS) / 1000f;
        float wst = (float) tokenCount.get() / t;
        float lr = (float) (args_.lr * (1.0f - progress));
        int eta = (int) (t / progress * (1 - progress) / args_.thread);
        int etah = eta / 3600;
        int etam = (eta - etah * 3600) / 60;

        Log.info("Progress: %.1f%% words/sec/thread: %.1f lr: %.6f loss:%.6f eta %d h %d m",
                100 * progress,
                wst,
                lr,
                loss,
                etah,
                etam);
    }

    private void supervised(Model model, float lr,
                            int[] line,
                            int[] labels) {
        if (labels.length == 0 || line.length == 0) return;
        int i = model.getRng().nextInt(labels.length);
        model.update(line, labels[i], lr);
    }

    private void cbow(Model model, float lr, int[] line) {
        for (int w = 0; w < line.length; w++) {
            int boundary = model.getRng().nextInt(args_.ws) + 1; // [1..args.ws]
            IntVector bow = new IntVector();
            for (int c = -boundary; c <= boundary; c++) {
                if (c != 0 && w + c >= 0 && w + c < line.length) {
                    int[] ngrams = dict_.getNgrams(line[w + c]);
                    bow.addAll(ngrams);
                }
            }
            model.update(bow.copyOf(), line[w], lr);
        }
    }

    private void skipgram(Model model, float lr, int[] line) {
        for (int w = 0; w < line.length; w++) {
            int boundary = model.getRng().nextInt(args_.ws) + 1; // [1..args.ws]
            int[] ngrams = dict_.getNgrams(line[w]);
            for (int c = -boundary; c <= boundary; c++) {
                if (c != 0 && w + c >= 0 && w + c < line.length) {
                    model.update(ngrams, line[w + c], lr);
                }
            }
        }
    }

    static class ScoreStringPair {
        final float score;
        final String string;

        ScoreStringPair(float score, String string) {
            this.score = score;
            this.string = string;
        }
    }

    void test(Path in, int k) throws IOException {
        int nexamples = 0, nlabels = 0;
        double precision = 0.0;
        String lineStr;
        BufferedReader reader = Files.newBufferedReader(in, StandardCharsets.UTF_8);
        while ((lineStr = reader.readLine()) != null) {
            IntVector line = new IntVector(), labels = new IntVector();
            dict_.getLine(lineStr, line, labels, model_.getRng());
            dict_.addNgrams(line, args_.wordNgrams);
            if (labels.size() > 0 && line.size() > 0) {
                List<Model.Pair> modelPredictions = model_.predict(line.copyOf(), k);
                for (Model.Pair pair : modelPredictions) {
                    if (labels.contains(pair.second)) {
                        precision += 1.0;
                    }
                }
                nexamples++;
                nlabels += labels.size();
            }
        }
        Log.info("P@%d: %.3f  R@%d: %.3f  Number of examples = %d",
                k, precision / (k * nexamples),
                k, precision / nlabels,
                nexamples);
    }

    Vector textVectors(List<String> paragraph) {
        Vector vec = new Vector(args_.dim);
        for (String s : paragraph) {
            IntVector line = new IntVector(), labels = new IntVector();
            dict_.getLine(s, line, labels, model_.getRng());
            if (line.size() == 0) {
                continue;
            }
            dict_.addNgrams(line, args_.wordNgrams);
            for (int i : line.copyOf()) {
                vec.addRow(input_, i);
            }
            vec.mul((float) (1.0 / line.size()));
        }
        return vec;
    }

    List<ScoreStringPair> predict(String line, int k) {
        IntVector words = new IntVector();
        IntVector labels = new IntVector();
        dict_.getLine(line, words, labels, model_.getRng());
        dict_.addNgrams(words, args_.wordNgrams);
        if (words.isempty()) {
            return Collections.emptyList();
        }
        Vector hidden = new Vector(args_.dim);
        Vector output = new Vector(dict_.nlabels());
        List<Model.Pair> modelPredictions = model_.predict(words.copyOf(), k, hidden, output);
        List<ScoreStringPair> result = new ArrayList<>(modelPredictions.size());
        for (Model.Pair pair : modelPredictions) {
            result.add(new ScoreStringPair(pair.first, dict_.getLabel(pair.second)));
        }
        return result;
    }

    private class TrainTask implements Callable<Model> {

        int threadId;
        Path input;
        int startCharIndex;

        TrainTask(int threadId, Path input, int startCharIndex) {
            this.threadId = threadId;
            this.input = input;
            this.startCharIndex = startCharIndex;
        }

        @Override
        public Model call() throws Exception {
            Model model = new Model(input_, output_, args_, threadId);
            if (args_.model == Args.model_name.sup) {
                model.setTargetCounts(dict_.getCounts(Dictionary.TYPE_LABEL));
            } else {
                model.setTargetCounts(dict_.getCounts(Dictionary.TYPE_WORD));
            }

            long ntokens = dict_.ntokens();
            long localTokenCount = 0;
            BlockTextLoader loader = new BlockTextLoader(input, StandardCharsets.UTF_8, 1000);
            Iterator<List<String>> it = loader.iteratorFromCharIndex(startCharIndex);
            float progress = 0f;
            while (true) {
                while (it.hasNext()) {
                    List<String> lines = it.next();
                    for (String lineStr : lines) {
                        if (tokenCount.get() >= args_.epoch * ntokens) {
                            if (threadId == 0 && args_.verbose > 0) {
                                printInfo(1.0f, model.getLoss());
                            }
                            return model;
                        }
                        IntVector line = new IntVector(15);
                        IntVector labels = new IntVector();
                        int wcount = dict_.getLine(lineStr, line, labels, model.getRng());
                        if (wcount == 0) {
                            continue;
                        }
                        localTokenCount += wcount;
                        progress = (float) ((1.0 * tokenCount.get()) / (args_.epoch * ntokens));
                        float lr = (float) (args_.lr * (1.0 - progress));
                        if (args_.model == Args.model_name.sup) {
                            dict_.addNgrams(line, args_.wordNgrams);
                            supervised(model, lr, line.copyOf(), labels.copyOf());
                        } else if (args_.model == Args.model_name.cbow) {
                            cbow(model, lr, line.copyOf());
                        } else if (args_.model == Args.model_name.sg) {
                            skipgram(model, lr, line.copyOf());
                        }
                        if (localTokenCount > args_.lrUpdateRate) {
                            tokenCount.getAndAdd(localTokenCount);
                            localTokenCount = 0;
                        }
                    }
                    if (threadId == 0 && args_.verbose > 1) {
                        printInfo(progress, model.getLoss());
                    }
                }
                // start from the beginning again.
                it = loader.iterator();
            }
        }
    }

    /**
     * Trains a model for the input. It generates input and output matrixes.
     * @param input
     */
    void train(Path input) throws Exception {

        if (args_.pretrainedVectors.length() != 0) {
            //TODO: implement this.
            //loadVectors(args_->pretrainedVectors);
        } else {
            input_ = new Matrix(dict_.nwords() + args_.bucket, args_.dim, false);
            input_.uniform(1.0f / args_.dim);
        }

        if (args_.model == Args.model_name.sup) {
            output_ = new Matrix(dict_.nlabels(), args_.dim, false);
        } else {
            output_ = new Matrix(dict_.nwords(), args_.dim, false);
        }

        stopwatch = Stopwatch.createStarted();
        tokenCount = new AtomicLong(0);

        ExecutorService es = Executors.newFixedThreadPool(args_.thread);
        CompletionService<Model> completionService = new ExecutorCompletionService<>(es);
        Log.info("Counting chars..");
        long charCount = TextIO.charCount(input, StandardCharsets.UTF_8);
        Log.info("Training started.");
        Stopwatch sw = Stopwatch.createStarted();
        for (int i = 0; i < args_.thread; i++) {
            completionService.submit(new TrainTask(i, input, (int) (i * charCount / args_.thread)));
        }
        es.shutdown();

        int c = 0;
        while (c < args_.thread) {
            completionService.take().get();
            c++;
        }
        Log.info("Training finished in %.1f seconds.",
                sw.elapsed(TimeUnit.MILLISECONDS) / 1000d);
        model_ = new Model(input_, output_, args_, 0);
    }

}
