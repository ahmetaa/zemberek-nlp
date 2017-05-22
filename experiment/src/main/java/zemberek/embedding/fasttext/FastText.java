package zemberek.embedding.fasttext;

import com.google.common.base.Stopwatch;
import zemberek.core.ScoredItem;
import zemberek.core.collections.IntVector;
import zemberek.core.io.IOUtil;
import zemberek.core.logging.Log;
import zemberek.core.text.BlockTextLoader;
import zemberek.core.text.TextIO;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

public class FastText {
    private Args args_;
    private Dictionary dict_;
    private Model model_;

    boolean quant_ = false; // TODO: this can be removed because it already exists in Model

    public static final int FASTTEXT_VERSION = 11;
    public static final int FASTTEXT_FILEFORMAT_MAGIC_INT32 = 793712314;

    private FastText(Args args_, Dictionary dict_, Model model) {
        this.args_ = args_;
        this.dict_ = dict_;
        this.model_ = model;
        quant_ = model.quant_;
    }

    // Sums all word and ngram vectors for a word and normalizes it.
    private Vector getVector(String word) {
        int[] ngrams = dict_.getNgrams(word);
        Vector vec = new Vector(args_.dim);
        for (int i : ngrams) {
            vec.addRow(model_.wi_, i);
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

    void saveOutput(Path outPath) throws IOException {
        try (PrintWriter pw = new PrintWriter(outPath.toFile(), "utf-8")) {
            pw.println(dict_.nwords() + " " + args_.dim);
            for (int i = 0; i < dict_.nwords(); i++) {
                String word = dict_.getWord(i);
                Vector vector = new Vector(args_.dim);
                vector.addRow(model_.wo_, i);
                pw.println(word + " " + vector.asString());
            }
        }
    }

    static boolean checkModel(Path in) throws IOException {
        try (DataInputStream dis = IOUtil.getDataInputStream(in)) {
            int magic = dis.readInt();
            if (magic != FASTTEXT_FILEFORMAT_MAGIC_INT32) {
                return false;
            }
            int version = dis.readInt();
            if (version != FASTTEXT_VERSION) {
                return false;
            }
        }
        return true;
    }

    static boolean checkModel(DataInputStream dis) throws IOException {
        int magic = dis.readInt();
        if (magic != FASTTEXT_FILEFORMAT_MAGIC_INT32) {
            return false;
        }
        int version = dis.readInt();
        if (version != FASTTEXT_VERSION) {
            return false;
        }
        return true;
    }

    void signModel(DataOutputStream dos) throws IOException {
        dos.writeInt(FASTTEXT_FILEFORMAT_MAGIC_INT32);
        dos.writeInt(FASTTEXT_VERSION);
    }

    void saveModel(Path outFilePath) throws IOException {
        try (DataOutputStream dos = IOUtil.getDataOutputStream(outFilePath)) {
            signModel(dos);
            args_.save(dos);
            dict_.save(dos);
            dos.writeBoolean(quant_);
            if (quant_) {
                model_.qwi_.save(dos);
            } else {
                model_.wi_.save(dos);
            }
            dos.writeBoolean(args_.qout);
            if (quant_ && args_.qout) {
                model_.qwo_.save(dos);
            } else {
                model_.wo_.save(dos);
            }
        }
    }

    static FastText load(Path path) throws IOException {
        try (DataInputStream dis = IOUtil.getDataInputStream(path)) {
            if (!checkModel(dis)) {
                throw new IllegalStateException("Model file has wrong file format.");
            }
            return load(dis);
        }
    }

    static FastText load(DataInputStream dis) throws IOException {
        Args args_ = Args.load(dis);
        Dictionary dict_ = Dictionary.load(dis, args_);
        Model model_ = Model.load(dis, args_);
        if (args_.model == Args.model_name.sup) {
            model_.setTargetCounts(dict_.getCounts(Dictionary.TYPE_LABEL));
        } else {
            model_.setTargetCounts(dict_.getCounts(Dictionary.TYPE_WORD));
        }
        Log.info("Model loaded.");
        return new FastText(args_, dict_, model_);
    }

    FastText quantize(Path path, Args qargs) throws IOException {
        try (DataInputStream dis = IOUtil.getDataInputStream(path)) {
            if (!checkModel(dis)) {
                throw new IllegalStateException("Model file has wrong file format.");
            }
            return quantize(dis, qargs);
        }
    }

    static class L2NormData {
        int index;
        float l2Norm;
    }

    int[] selectEmbeddings(int cutoff) {
        Matrix_ input_ = model_.wi_;
        L2NormData[] normIndexes = new L2NormData[input_.m_ - 1];
        int eosid = dict_.getId(Dictionary.EOS);
        int k = 0;
        for (int i = 0; i < input_.m_; i++) {
            if (i == eosid) {
                continue;
            }
            normIndexes[k].l2Norm = input_.l2NormRow(i);
            normIndexes[k].index = i;
            k++;
        }
        Arrays.sort(normIndexes,
                (a, b) -> Float.compare(normIndexes[a.index].l2Norm, normIndexes[b.index].l2Norm));
        int[] result = new int[cutoff - 1];
        for (int i = 0; i < result.length; i++) {
            result[i] = normIndexes[i].index;
        }
        // add EOS explicitly.
        result[cutoff] = eosid;
        return result;
    }

    FastText quantize(DataInputStream dis, Args qargs) throws IOException {
        Args args_ = Args.load(dis);
        Dictionary dict_ = Dictionary.load(dis, args_);
        Model model_ = Model.load(dis, args_);
        if (args_.model == Args.model_name.sup) {
            model_.setTargetCounts(dict_.getCounts(Dictionary.TYPE_LABEL));
        } else {
            model_.setTargetCounts(dict_.getCounts(Dictionary.TYPE_WORD));
        }

        args_.qout = qargs.qout;
        //TODO: add  cutoff operations later.

        model_.qwi_ = new QMatrix(model_.wi_, qargs.dsub, qargs.qnorm);

        if (args_.qout) {
            model_.qwo_ = new QMatrix(model_.wo_, 2, qargs.qnorm);
        }

        model_.quant_ = true;
        return new FastText(args_, dict_, model_);
    }


    void test(Path in, int k) throws IOException {
        int nexamples = 0, nlabels = 0;
        double precision = 0.0;
        String lineStr;
        BufferedReader reader = Files.newBufferedReader(in, StandardCharsets.UTF_8);
        while ((lineStr = reader.readLine()) != null) {
            IntVector line = new IntVector(), labels = new IntVector();
            dict_.getLine(lineStr, line, labels, model_.getRng());
            dict_.addWordNgramHashes(line, args_.wordNgrams);
            if (labels.size() > 0 && line.size() > 0) {
                List<Model.FloatIntPair> modelPredictions = model_.predict(line.copyOf(), k);

                for (Model.FloatIntPair pair : modelPredictions) {
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
            dict_.addWordNgramHashes(line, args_.wordNgrams);
            for (int i : line.copyOf()) {
                vec.addRow(model_.wi_, i);
            }
            vec.mul((float) (1.0 / line.size()));
        }
        return vec;
    }

    Vector textVector(String s) {
        Vector vec = new Vector(args_.dim);
        IntVector line = new IntVector(), labels = new IntVector();
        dict_.getLine(s, line, labels, model_.getRng());
        dict_.addWordNgramHashes(line, args_.wordNgrams);
        if (line.size() == 0) {
            return vec;
        }
        for (int i : line.copyOf()) {
            vec.addRow(model_.wi_, i);
        }
        vec.mul((float) (1.0 / line.size()));
        return vec;
    }

    List<ScoredItem<String>> predict(String line, int k) {
        IntVector words = new IntVector();
        IntVector labels = new IntVector();
        dict_.getLine(line, words, labels, model_.getRng());
        dict_.addWordNgramHashes(words, args_.wordNgrams);
        if (words.isempty()) {
            return Collections.emptyList();
        }
        Vector output = new Vector(dict_.nlabels());
        Vector hidden = model_.computeHidden(words.copyOf());
        List<Model.FloatIntPair> modelPredictions = model_.predict(k, hidden, output);
        List<ScoredItem<String>> result = new ArrayList<>(modelPredictions.size());
        for (Model.FloatIntPair pair : modelPredictions) {
            result.add(new ScoredItem<>(dict_.getLabel(pair.second), pair.first));
        }
        return result;
    }

    private static class TrainTask implements Callable<Model> {

        int threadId;
        Path input;
        int startCharIndex;
        Stopwatch stopwatch;
        AtomicLong tokenCount;
        Model model;
        Dictionary dictionary;
        Args args_;

        TrainTask(int threadId,
                  Path input,
                  int startCharIndex,
                  Model model,
                  Stopwatch stopwatch,
                  Dictionary dictionary,
                  Args args_,
                  AtomicLong tokenCount) {
            this.threadId = threadId;
            this.input = input;
            this.startCharIndex = startCharIndex;
            this.model = model;
            this.stopwatch = stopwatch;
            this.tokenCount = tokenCount;
            this.dictionary = dictionary;
            this.args_ = args_;
        }

        private void printInfo(float progress, float loss) {
            float t = stopwatch.elapsed(TimeUnit.MILLISECONDS) / 1000f;
            float wst = (float) tokenCount.get() / t;
            float lr = (float) (args_.lr * (1.0f - progress));
            int eta = (int) (t / progress * (1 - progress) / args_.thread);
            int etah = eta / 3600;
            int etam = (eta - etah * 3600) / 60;

            Log.info("Progress: %.1f%% words/sec/thread: %.0f lr: %.6f loss:%.6f eta %dh%dm",
                    100 * progress,
                    wst,
                    lr,
                    loss,
                    etah,
                    etam);
        }

        private void supervised(Model model,
                                float lr,
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
                        int[] ngrams = dictionary.getNgrams(line[w + c]);
                        bow.addAll(ngrams);
                    }
                }
                model.update(bow.copyOf(), line[w], lr);
            }
        }

        private void skipgram(Model model, float lr, int[] line) {
            for (int w = 0; w < line.length; w++) {
                int boundary = model.getRng().nextInt(args_.ws) + 1; // [1..args.ws]
                int[] ngrams = dictionary.getNgrams(line[w]);
                for (int c = -boundary; c <= boundary; c++) {
                    if (c != 0 && w + c >= 0 && w + c < line.length) {
                        model.update(ngrams, line[w + c], lr);
                    }
                }
            }
        }

        @Override
        public Model call() throws Exception {

            if (args_.model == Args.model_name.sup) {
                model.setTargetCounts(dictionary.getCounts(Dictionary.TYPE_LABEL));
            } else {
                model.setTargetCounts(dictionary.getCounts(Dictionary.TYPE_WORD));
            }

            long ntokens = dictionary.ntokens();
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
                        int wcount = dictionary.getLine(lineStr, line, labels, model.getRng());
                        if (wcount == 0) {
                            continue;
                        }
                        localTokenCount += wcount;
                        progress = (float) ((1.0 * tokenCount.get()) / (args_.epoch * ntokens));
                        float lr = (float) (args_.lr * (1.0 - progress));

                        if (args_.model == Args.model_name.sup) {
                            dictionary.addWordNgramHashes(line, args_.wordNgrams);
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
     * Trains a model for the input with given arguments, returns a FastText instance.
     * Input can be a text corpus, or a corpus with text and labels.
     */
    static FastText train(Path input, Args args_) throws Exception {

        Dictionary dict_ = Dictionary.readFromFile(input, args_);

        Matrix_ input_ = null;
        if (args_.pretrainedVectors.length() != 0) {
            //TODO: implement this.
            //loadVectors(args_->pretrainedVectors);
        } else {
            input_ = new Matrix_(dict_.nwords() + args_.bucket, args_.dim);
            input_.uniform(1.0f / args_.dim);
        }

        Matrix_ output_;
        if (args_.model == Args.model_name.sup) {
            output_ = new Matrix_(dict_.nlabels(), args_.dim);
        } else {
            output_ = new Matrix_(dict_.nwords(), args_.dim);
        }

        Model model_ = new Model(input_, output_, args_, 0);
        if (args_.model == Args.model_name.sup) {
            model_.setTargetCounts(dict_.getCounts(Dictionary.TYPE_LABEL));
        } else {
            model_.setTargetCounts(dict_.getCounts(Dictionary.TYPE_WORD));
        }

        Stopwatch stopwatch = Stopwatch.createStarted();
        AtomicLong tokenCount = new AtomicLong(0);

        ExecutorService es = Executors.newFixedThreadPool(args_.thread);
        CompletionService<Model> completionService = new ExecutorCompletionService<>(es);
        long charCount = TextIO.charCount(input, StandardCharsets.UTF_8);
        Log.info("Training started.");
        Stopwatch sw = Stopwatch.createStarted();
        for (int i = 0; i < args_.thread; i++) {
            // Here a model per thread is generated. It uses references to global model's input and output matrices.
            // AFAIK, original Fasttext does not care about thread safety of those matrices.
            Model threadModel = new Model(model_, i);

            completionService.submit(new TrainTask(
                    i,
                    input,
                    (int) (i * charCount / args_.thread),
                    threadModel,
                    stopwatch,
                    dict_,
                    args_,
                    tokenCount));
        }
        es.shutdown();

        int c = 0;
        while (c < args_.thread) {
            completionService.take().get();
            c++;
        }
        Log.info("Training finished in %.1f seconds.",
                sw.elapsed(TimeUnit.MILLISECONDS) / 1000d);

        return new FastText(args_, dict_, model_);
    }

}
