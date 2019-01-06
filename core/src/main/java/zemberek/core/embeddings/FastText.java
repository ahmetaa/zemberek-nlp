package zemberek.core.embeddings;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import zemberek.core.ScoredItem;
import zemberek.core.collections.IntVector;
import zemberek.core.embeddings.Args.model_name;
import zemberek.core.io.IOUtil;

public class FastText {

  public static final int FASTTEXT_VERSION = 11;
  public static final int FASTTEXT_FILEFORMAT_MAGIC_INT32 = 793712314;
  private Args args_;
  private Dictionary dict_;
  private Model model_;

  FastText(Args args_, Dictionary dict_, Model model) {
    this.args_ = args_;
    this.dict_ = dict_;
    this.model_ = model;
  }

  public void addInputVector(Vector vec, int ind) {
    if (model_.quant_) {
      vec.addRow(model_.qwi_, ind);
    } else {
      vec.addRow(model_.wi_, ind);
    }
  }

  public Dictionary getDictionary() {
    return dict_;
  }

  public Args getArgs() {
    return args_;
  }

  public Matrix getInputMatrix() {
    return model_.wi_;
  }

  public Matrix getOutputMatrix() {
    return model_.wo_;
  }

  public int getWordId(String word) {
    return dict_.getId(word);
  }

  public int getSubwordId(String word) {
    int h = Dictionary.hash(word) % args_.bucket;
    return dict_.nwords() + h;
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

  private Vector getWordVector(String word) {
    int[] ngrams = dict_.getSubWords(word);
    Vector vec = new Vector(args_.dim);
    for (int i : ngrams) {
      vec.addRow(model_.wi_, i);
    }
    if (ngrams.length > 0) {
      vec.mul(1.0f / ngrams.length);
    }
    return vec;
  }

  private Vector getSubWordVector(String word) {
    int h = Dictionary.hash(word) % args_.bucket;
    h = h + dict_.nwords();
    Vector vec = new Vector(args_.dim);
    addInputVector(vec, h);
    return vec;
  }

  public void saveVectors(Path outFilePath) throws IOException {
    try (PrintWriter pw = new PrintWriter(outFilePath.toFile(), "utf-8")) {
      pw.println(dict_.nwords() + " " + args_.dim);
      for (int i = 0; i < dict_.nwords(); i++) {
        String word = dict_.getWord(i);
        Vector vector = getWordVector(word);
        pw.println(word + " " + vector.asString());
      }
    }
  }

  public void saveOutput(Path outPath) throws IOException {

    int n = (args_.model == model_name.supervised) ? dict_.nlabels() : dict_.nwords();

    try (PrintWriter pw = new PrintWriter(outPath.toFile(), "utf-8")) {
      pw.println(dict_.nwords() + " " + args_.dim);
      for (int i = 0; i < n; i++) {
        String word = (args_.model == model_name.supervised) ?
            dict_.getLabel(i) : dict_.getWord(i);
        Vector vector = new Vector(args_.dim);
        vector.addRow(model_.wo_, i);
        pw.println(word + " " + vector.asString());
      }
    }
  }

  private static boolean checkModel(DataInputStream dis) throws IOException {
    int magic = dis.readInt();
    if (magic != FASTTEXT_FILEFORMAT_MAGIC_INT32) {
      return false;
    }
    int version = dis.readInt();
    return version == FASTTEXT_VERSION;
  }

  private void signModel(DataOutputStream dos) throws IOException {
    dos.writeInt(FASTTEXT_FILEFORMAT_MAGIC_INT32);
    dos.writeInt(FASTTEXT_VERSION);
  }

  public void saveModel(Path outFilePath) throws IOException {
    try (DataOutputStream dos = IOUtil.getDataOutputStream(outFilePath)) {
      signModel(dos);
      args_.save(dos);
      dict_.save(dos);
      dos.writeBoolean(model_.quant_);
      if (model_.quant_) {
        model_.qwi_.save(dos);
      } else {
        model_.wi_.save(dos);
      }
      dos.writeBoolean(args_.qout);
      if (model_.quant_ && args_.qout) {
        model_.qwo_.save(dos);
      } else {
        model_.wo_.save(dos);
      }
    }
  }


  public static FastText load(Path path) throws IOException {
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
    if (args_.model == Args.model_name.supervised) {
      model_.setTargetCounts(dict_.getCounts(Dictionary.TYPE_LABEL));
    } else {
      model_.setTargetCounts(dict_.getCounts(Dictionary.TYPE_WORD));
    }
    return new FastText(args_, dict_, model_);
  }

  public FastText quantize(Path path, Args qargs) throws IOException {
    try (DataInputStream dis = IOUtil.getDataInputStream(path)) {
      if (!checkModel(dis)) {
        throw new IllegalStateException("Model file has wrong file format.");
      }
      return quantize(dis, qargs);
    }
  }

  // selects ids of highest L2Norm valued embeddings.
  // Returns (word - subword) indexes.
  int[] selectEmbeddings(int cutoff) {

    Matrix input_ = model_.wi_;
    List<L2NormData> normIndexes = new ArrayList<>(input_.m_);
    int eosid = dict_.getId(Dictionary.EOS); // we want to retain EOS
    for (int i = 0; i < input_.m_; i++) {
      if (i == eosid) {
        continue;
      }
      normIndexes.add(new L2NormData(i, input_.l2NormRow(i)));
    }
    normIndexes.sort((a, b) -> Float.compare(b.l2Norm, a.l2Norm));

    int[] result = new int[cutoff];
    for (int i = 0; i < cutoff - 1; i++) {
      result[i] = normIndexes.get(i).index;
    }
    // add EOS.
    result[cutoff - 1] = eosid;
    return result;
  }

  public List<String> getLabels() {
    return getDictionary().getLabels();
  }

  static class L2NormData {

    final int index;
    final float l2Norm;

    public L2NormData(int index, float l2Norm) {
      this.index = index;
      this.l2Norm = l2Norm;
    }
  }

  FastText quantize(DataInputStream dis, Args qargs) throws IOException {
    Args args_ = Args.load(dis);
    if (args_.model != model_name.supervised) {
      throw new IllegalArgumentException("Only supervised models can be quantized.");
    }
    Dictionary dict_ = Dictionary.load(dis, args_);
    Model model_ = Model.load(dis, args_);

    args_.qout = qargs.qout;

    Matrix input = model_.wi_;
    if (qargs.cutoff > 0 && qargs.cutoff < input.m_) {
      int[] idx = selectEmbeddings(qargs.cutoff);
      idx = dict_.prune(idx);
      Matrix newInput = new Matrix(idx.length, args_.dim);
      for (int i = 0; i < idx.length; i++) {
        for (int j = 0; j < args_.dim; j++) {
          newInput.set(i, j, input.at(idx[i], j));
        }
      }
      model_.wi_ = newInput;
      // TODO: add retraining. It was hard because of the design differences
    }

    QMatrix qwi_ = new QMatrix(model_.wi_, qargs.dsub, qargs.qnorm);

    QMatrix qwo_ = model_.qwo_;
    if (qargs.qout) {
      qwo_ = new QMatrix(model_.wo_, 2, qargs.qnorm);
    }

    model_ = new Model(model_.wi_, model_.wo_, args_, 0);
    model_.quant_ = true;
    model_.setQuantizePointer(qwi_, qwo_, args_.qout);

    if (args_.model == Args.model_name.supervised) {
      model_.setTargetCounts(dict_.getCounts(Dictionary.TYPE_LABEL));
    } else {
      model_.setTargetCounts(dict_.getCounts(Dictionary.TYPE_WORD));
    }
    return new FastText(args_, dict_, model_);
  }

  public EvaluationResult test(Path in, int k) throws IOException {
    return test(in, k, -100f);
  }

  public static class EvaluationResult {

    public final float precision;
    public final float recall;
    public final int k;
    public final int numberOfExamples;

    public EvaluationResult(
        float precision,
        float recall,
        int k,
        int numberOfExamples) {
      this.precision = precision;
      this.recall = recall;
      this.k = k;
      this.numberOfExamples = numberOfExamples;
    }

    public void printTo(PrintStream stream) {
      stream.println(toString());
    }

    @Override
    public String toString() {
      return String.format(
          "P@%d: %.3f  R@%d: %.3f F@%d %.3f  Number of examples = %d",
          k, precision,
          k, recall,
          k, (2 * precision * recall) / (precision + recall),
          numberOfExamples);
    }
  }

  public EvaluationResult test(Path in, int k, float threshold) throws IOException {
    int nexamples = 0, nlabels = 0;
    float precision = 0.0f;
    String lineStr;
    BufferedReader reader = Files.newBufferedReader(in, StandardCharsets.UTF_8);
    while ((lineStr = reader.readLine()) != null) {
      IntVector words = new IntVector(), labels = new IntVector();
      dict_.getLine(lineStr, words, labels);
      if (labels.size() > 0 && words.size() > 0) {
        List<Model.FloatIntPair> modelPredictions = model_.predict(words.copyOf(), threshold, k);
        for (Model.FloatIntPair pair : modelPredictions) {
          if (labels.contains(pair.second)) {
            precision += 1.0f;
          }
        }
        nexamples++;
        nlabels += labels.size();
      }
    }
    return new EvaluationResult(
        precision / (k * nexamples),
        precision / nlabels,
        k,
        nexamples);
  }

  public void test(Path in, int k, float threshold, Meter meter) throws IOException {
    String lineStr;
    BufferedReader reader = Files.newBufferedReader(in, StandardCharsets.UTF_8);
    while ((lineStr = reader.readLine()) != null) {
      IntVector words = new IntVector(), labels = new IntVector();
      dict_.getLine(lineStr, words, labels);
      if (labels.size() > 0 && words.size() > 0) {
        List<Model.FloatIntPair> modelPredictions = model_.predict(words.copyOf(), threshold, k);
        meter.log(labels, modelPredictions);
      }
    }
  }

  Vector textVectors(List<String> paragraph) {
    Vector vec = new Vector(args_.dim);
    for (String s : paragraph) {
      IntVector line = new IntVector();
      dict_.getLine(s, line, model_.getRng());
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

  public float[] sentenceVector(String s) {
    Vector svec = new Vector(args_.dim);

    if (args_.model == model_name.supervised) {
      IntVector line = new IntVector();
      dict_.getLine(s, line, model_.getRng());
      for (int i : line.copyOf()) {
        addInputVector(svec, i);
      }
      if(line.size()>0) {
        svec.mul(1f/line.size());
      }
      return svec.getData();
    }

    IntVector line = new IntVector();
    dict_.getLine(s, line, model_.getRng());
    dict_.addWordNgramHashes(line, args_.wordNgrams);
    if (line.size() == 0) {
      return svec.getData();
    }

    int count = 0;
    for (int i : line.copyOf()) {

      Vector vec = getWordVector(dict_.getWord(i));
      float norm = vec.norm();

      if (norm > 0) {
        vec.mul(1f / norm);
        svec.addVector(vec);
        count++;
      }
    }
    if (count > 0) {
      svec.mul(1f / count);
    }
    return svec.getData();
  }

  Vector getSentenceVector(String s) {
    Vector svec = new Vector(args_.dim);
    if (args_.model == model_name.supervised) {
      IntVector line = new IntVector(), labels = new IntVector();
      dict_.getLine(s, line, labels);
      for (int i = 0; i < line.size(); i++) {
        addInputVector(svec, line.get(i));
      }
      if (!line.isempty()) {
        svec.mul(1.0f / line.size());
      }
    } else {
      String[] tokens = s.split("\\s+");
      int count = 0;
      for (String token : tokens) {
        if (token.length() == 0) {
          continue;
        }
        Vector vec = getWordVector(token);
        float norm = vec.norm();
        if (norm > 0) {
          vec.mul(1.0f / norm);
          svec.addVector(vec);
          count++;
        }
      }
      if (count > 0) {
        svec.mul(1.0f / count);
      }
    }
    return svec;
  }


  public List<ScoredItem<String>> predict(String line, int k) {
    return predict(line, k, -100f);
  }

  public List<ScoredItem<String>> predict(String line, int k, float threshold) {
    IntVector words = new IntVector();
    IntVector labels = new IntVector();
    dict_.getLine(line, words, labels);
    if (words.isempty()) {
      return Collections.emptyList();
    }
    List<Model.FloatIntPair> modelPredictions =
        model_.predict(words.copyOf(), threshold, k);
    List<ScoredItem<String>> result = new ArrayList<>(modelPredictions.size());
    for (Model.FloatIntPair pair : modelPredictions) {
      result.add(new ScoredItem<>(dict_.getLabel(pair.second), pair.first));
    }
    return result;
  }

}
