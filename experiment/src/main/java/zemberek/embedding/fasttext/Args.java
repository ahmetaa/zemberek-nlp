package zemberek.embedding.fasttext;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class Args {

    double lr;
    int lrUpdateRate;
    int dim;
    int ws;
    int epoch;
    int minCount;
    int minCountLabel;
    int neg;
    int wordNgrams;
    loss_name loss;
    model_name model;
    boolean threadSafe;
    int bucket;
    int minn;
    int maxn;
    int thread;
    double t;
    String label;
    int verbose;
    String pretrainedVectors;
    SubWordHashProvider subWordHashProvider;

    boolean qout = false;
    boolean retrain = false;
    boolean qnorm = false;
    int cutoff = 0;
    int dsub = 2;

    enum model_name {
        cbow(1), sg(2), sup(3);

        int index;

        model_name(int i) {
            this.index = i;
        }
    }

    enum loss_name {
        hs(1), ns(2), softmax(3);

        int index;

        loss_name(int i) {
            this.index = i;
        }
    }

    private Args() {
        dim = 100;
        ws = 5;
        epoch = 5;
        minCount = 5;
        minCountLabel = 0;
        neg = 5;
        bucket = 2000000;
        threadSafe = false;
        thread = 8;
        lrUpdateRate = 100;
        t = 1e-4;
        label = "__label__";
        verbose = 2;
        pretrainedVectors = "";
    }

    static Args forWordVectors(model_name modelName) {
        Args args = new Args();
        args.minn = 3;
        args.maxn = 6;
        args.subWordHashProvider =
                new Dictionary.CharacterNgramHashProvider(args.minn, args.maxn);
        args.lr = 0.05;
        args.loss = loss_name.ns;
        args.model = modelName;
        args.wordNgrams = 1;
        return args;
    }

    static Args forSupervised() {
        Args args = new Args();
        args.minn = 0;
        args.maxn = 0;
        args.subWordHashProvider =
                new Dictionary.EmptySubwordHashProvider();
        args.lr = 0.1;
        args.loss = loss_name.softmax;
        args.model = model_name.sup;
        args.wordNgrams = 2;
        return args;
    }

    void save(DataOutputStream out) throws IOException {
        out.writeInt(dim);
        out.writeInt(ws);
        out.writeInt(epoch);
        out.writeInt(minCount);
        out.writeInt(neg);
        out.writeInt(wordNgrams);
        out.writeInt(loss.index);
        out.writeInt(model.index);
        out.writeInt(bucket);
        out.writeInt(minn);
        out.writeInt(maxn);
        out.writeInt(lrUpdateRate);
        out.writeDouble(t);
    }

    static Args load(DataInputStream in) throws IOException {
        Args args = new Args();
        args.dim = in.readInt();
        args.ws = in.readInt();
        args.epoch = in.readInt();
        args.minCount = in.readInt();
        args.neg = in.readInt();
        args.wordNgrams = in.readInt();
        int loss = in.readInt();
        if (loss == loss_name.hs.index) {
            args.loss = loss_name.hs;
        } else if (loss == loss_name.ns.index) {
            args.loss = loss_name.ns;
        } else if (loss == loss_name.softmax.index) {
            args.loss = loss_name.softmax;
        } else throw new IllegalStateException("Unknown loss type.");
        int model = in.readInt();
        if (model == model_name.cbow.index) {
            args.model = model_name.cbow;
        } else if (model == model_name.sg.index) {
            args.model = model_name.sg;
        } else if (model == model_name.sup.index) {
            args.model = model_name.sup;
        } else throw new IllegalStateException("Unknown model type.");
        args.bucket = in.readInt();
        args.minn = in.readInt();
        args.maxn = in.readInt();
        args.lrUpdateRate = in.readInt();
        args.t = in.readDouble();

        if (args.minn != 0) {
            args.subWordHashProvider = new Dictionary.CharacterNgramHashProvider(args.minn, args.maxn);
        } else {
            args.subWordHashProvider = new Dictionary.EmptySubwordHashProvider();
        }

        return args;
    }

}
