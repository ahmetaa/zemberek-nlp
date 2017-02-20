package zemberek.embedding.fasttext;

public class Args {
    String input;
    String test;
    String output;
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
    int bucket;
    int minn;
    int maxn;
    int thread;
    double t;
    String label;
    int verbose;
    String pretrainedVectors;

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

    Args() {
        lr = 0.05;
        dim = 100;
        ws = 5;
        epoch = 5;
        minCount = 5;
        minCountLabel = 0;
        neg = 5;
        wordNgrams = 1;
        loss = loss_name.ns;
        model = model_name.sg;
        bucket = 2000000;
        minn = 3;
        maxn = 6;
        thread = 4;
        lrUpdateRate = 100;
        t = 1e-4;
        label = "__label__";
        verbose = 2;
        pretrainedVectors = "";
    }


}
