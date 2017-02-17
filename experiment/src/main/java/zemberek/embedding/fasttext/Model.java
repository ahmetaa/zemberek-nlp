package zemberek.embedding.fasttext;

import java.util.List;
import java.util.Random;

public class Model {

    static class Node {
        int parent;
        int left;
        int right;
        int count;
        boolean binary;
    }

    Matrix wi_;
    Matrix wo_;
    Args args_;
    Vector hidden_;
    Vector output_;
    Vector grad_;
    int hsz_;
    int isz_;
    int osz_;
    float loss_;
    int nexamples_;
    float t_sigmoid;
    float t_log;
    // used for negative sampling:
    int[] negatives;
    int negpos;
    // used for hierarchical softmax:
    List<List<Integer>> paths;
    List<List<Boolean>> codes;
    List<Node> tree;

    static final int NEGATIVE_TABLE_SIZE = 10000000;

    Random random = new Random();

    Model(Matrix wi,
          Matrix wo,
          Args args,
          int seed) {
        hidden_ = new Vector(args.dim);
        output_ = new Vector(wo.m_);
        grad_ = new Vector(args.dim);
        random = new Random(seed);
        wi_ = wi;
        wo_ = wo;
        args_ = args;
        isz_ = wi.m_;
        osz_ = wo.m_;
        hsz_ = args.dim;
        negpos = 0;
        loss_ = 0.0f;
        nexamples_ = 1;
        //initSigmoid();
        //initLog();
    }

}
