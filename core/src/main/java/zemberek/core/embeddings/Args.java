package zemberek.core.embeddings;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class Args {

  public double lr;
  public int lrUpdateRate;
  public int dim;
  public int ws;
  public int epoch;
  public int minCount;
  public int minCountLabel;
  public int neg;
  public int wordNgrams;
  public loss_name loss;
  public model_name model;
  public int bucket;
  public int minn;
  public int maxn;
  public int thread;
  public double t;
  public String label;
  public int verbose;
  public String pretrainedVectors;
  public SubWordHashProvider subWordHashProvider;

  public boolean qout = false;
  public boolean retrain = false;
  public boolean qnorm = false;
  public int cutoff = 0;
  public int dsub = 2;

  private Args() {
    dim = 100;
    ws = 5;
    epoch = 5;
    minCount = 5;
    minCountLabel = 0;
    neg = 5;
    bucket = 2_000_000;
    thread = Runtime.getRuntime().availableProcessors() / 2;
    lrUpdateRate = 100;
    t = 1e-4;
    label = "__label__";
    verbose = 2;
    pretrainedVectors = "";
  }

  public static Args forWordVectors(model_name modelName) {
    Args args = new Args();
    args.minn = 3;
    args.maxn = 6;
    args.subWordHashProvider =
        new EmbeddingHashProviders.CharacterNgramHashProvider(args.minn, args.maxn);
    args.lr = 0.05;
    args.loss = loss_name.negativeSampling;
    args.model = modelName;
    args.wordNgrams = 1;
    return args;
  }

  public static Args forSupervised() {
    Args args = new Args();
    args.minn = 0;
    args.maxn = 0;
    args.subWordHashProvider =
        new EmbeddingHashProviders.EmptySubwordHashProvider();
    args.lr = 0.1;
    args.loss = loss_name.softmax;
    args.model = model_name.supervised;
    args.wordNgrams = 2;
    return args;
  }

  public static Args load(DataInputStream in) throws IOException {
    Args args = new Args();
    args.dim = in.readInt();
    args.ws = in.readInt();
    args.epoch = in.readInt();
    args.minCount = in.readInt();
    args.neg = in.readInt();
    args.wordNgrams = in.readInt();
    int loss = in.readInt();
    if (loss == loss_name.hierarchicalSoftmax.index) {
      args.loss = loss_name.hierarchicalSoftmax;
    } else if (loss == loss_name.negativeSampling.index) {
      args.loss = loss_name.negativeSampling;
    } else if (loss == loss_name.softmax.index) {
      args.loss = loss_name.softmax;
    } else {
      throw new IllegalStateException("Unknown loss type.");
    }
    int model = in.readInt();
    if (model == model_name.cbow.index) {
      args.model = model_name.cbow;
    } else if (model == model_name.skipGram.index) {
      args.model = model_name.skipGram;
    } else if (model == model_name.supervised.index) {
      args.model = model_name.supervised;
    } else {
      throw new IllegalStateException("Unknown model type.");
    }
    args.bucket = in.readInt();
    args.minn = in.readInt();
    args.maxn = in.readInt();
    args.lrUpdateRate = in.readInt();
    args.t = in.readDouble();

    if (args.minn != 0) {
      args.subWordHashProvider = new EmbeddingHashProviders.CharacterNgramHashProvider(args.minn,
          args.maxn);
    } else {
      args.subWordHashProvider = new EmbeddingHashProviders.EmptySubwordHashProvider();
    }

    return args;
  }

  public void save(DataOutputStream out) throws IOException {
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

  public enum model_name {
    cbow(1), skipGram(2), supervised(3);

    int index;

    model_name(int i) {
      this.index = i;
    }
  }

  public enum loss_name {
    hierarchicalSoftmax(1), negativeSampling(2), softmax(3);

    int index;

    loss_name(int i) {
      this.index = i;
    }
  }

}
