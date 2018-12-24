package zemberek.core.embeddings;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import zemberek.core.SpaceTabTokenizer;
import zemberek.core.collections.IntIntMap;
import zemberek.core.collections.IntVector;
import zemberek.core.logging.Log;
import zemberek.core.text.BlockTextLoader;
import zemberek.core.text.TextChunk;

class Dictionary {

  static final int TYPE_WORD = 0;
  static final int TYPE_LABEL = 1;

  private static final int MAX_VOCAB_SIZE = 10_000_000;
  private static final int MAX_LINE_SIZE = 1024;
  static String EOS = "</s>";
  private static String BOW = "<";
  private static String EOW = ">";
  private static SpaceTabTokenizer tokenizer = new SpaceTabTokenizer();
  private Args args_;
  private int[] word2int_;
  private List<Entry> words_;
  private float[] pdiscard_;
  private int size_;
  private int nwords_;
  private int nlabels_;
  private long ntokens_;
  private int pruneidx_size_ = -1;
  private IntIntMap pruneidx_ = new IntIntMap();

  private Dictionary(Args args) {
    args_ = args;
    size_ = 0;
    nwords_ = 0;
    nlabels_ = 0;
    ntokens_ = 0;
    word2int_ = new int[MAX_VOCAB_SIZE];
    words_ = new ArrayList<>(100_000);
    Arrays.fill(word2int_, -1);
  }

  static int hash(String str) {
    return hash(str, 0, str.length());
  }

  static int hash(byte[] bytes) {
    int h = 0x811C_9DC5;
    for (byte b : bytes) {
      h = h ^ (int) b;
      h = h * 16777619;
    }
    return h & 0x7fff_ffff;
  }

  // original fasttext code uses this code:
  // uint32_t h = 2166136261;
  // for (size_t i = 0; i < str.size(); i++) {
  //   h = h ^ uint32_t(int8_t(str[i]));
  //   h = h * 16777619;
  // }
  //
  static int hash(String str, int start, int end) {
    int h = 0x811C_9DC5;
    for (int i = start; i < end; i++) {
      h = h ^ str.charAt(i);
      h = h * 16777619;
    }
    return h & 0x7fff_ffff;
  }

  static Dictionary readFromFile(Path file, final Args args) {

    Log.info("Initialize dictionary and histograms.");
    Dictionary dictionary = new Dictionary(args);

    Log.info("Loading text.");
    BlockTextLoader loader = BlockTextLoader.fromPath(file, 100_000);
    SpaceTabTokenizer tokenizer = new SpaceTabTokenizer();

    int blockCounter = 1;

    for (TextChunk lines : loader) {
      for (String line : lines) {
        List<String> split = tokenizer.splitToList(line);
        split.add(EOS);
        for (String word : split) {
          if (word.startsWith("#")) {
            continue;
          }
          dictionary.add(word);
        }
      }
      Log.info("Lines read: %d (thousands) ", blockCounter * 100);
      blockCounter++;
    }
    Log.info("Word + Label count = %d", dictionary.words_.size());
    Log.info("Removing word and labels with small counts. Min word = %d, Min Label = %d",
        args.minCount, args.minCountLabel);
    // now we have the histograms. Remove based on count.
    dictionary.words_.sort((e1, e2) -> {
      if (e1.type != e2.type) {
        return Integer.compare(e1.type, e2.type);
      } else {
        return Long.compare(e2.count, e1.count);
      }
    });

    //TODO: add threshold method.
    LinkedHashSet<Entry> all = new LinkedHashSet<>(dictionary.words_);
    List<Entry> toRemove = dictionary.words_
        .stream()
        .filter(s -> (s.type == TYPE_WORD && s.count < args.minCount ||
            s.type == TYPE_LABEL && s.count < args.minCountLabel))
        .collect(Collectors.toList());
    all.removeAll(toRemove);
    dictionary.words_ = new ArrayList<>(all);
    dictionary.size_ = 0;
    dictionary.nwords_ = 0;
    dictionary.nlabels_ = 0;
    Arrays.fill(dictionary.word2int_, -1);
    for (Entry e : dictionary.words_) {
      int i = dictionary.find(e.word);
      dictionary.word2int_[i] = dictionary.size_++;
      if (e.type == TYPE_WORD) {
        dictionary.nwords_++;
      }
      if (e.type == TYPE_LABEL) {
        dictionary.nlabels_++;
      }
    }
    Log.info("Word count = %d , Label count = %d", dictionary.nwords(), dictionary.nlabels());
    dictionary.initTableDiscard();
    dictionary.initNgrams();
    return dictionary;
  }

  static Dictionary load(DataInputStream dis, Args args) throws IOException {

    Dictionary dict = new Dictionary(args);

    dict.size_ = dis.readInt();
    dict.nwords_ = dis.readInt();
    dict.nlabels_ = dis.readInt();
    dict.ntokens_ = dis.readLong();
    dict.pruneidx_size_ = dis.readInt();
    for (int i = 0; i < dict.size_; i++) {
      Entry e = new Entry();
      e.word = dis.readUTF();
      e.count = dis.readInt();
      e.type = dis.readInt();
      dict.words_.add(e);
    }
    for (int i = 0; i < dict.pruneidx_size_; i++) {
      int first = dis.readInt();
      int second = dis.readInt();
      dict.pruneidx_.put(first, second);
    }
    dict.init();

    int word2IntSize = (int) Math.ceil(dict.size_ / 0.7);
    dict.word2int_ = new int[word2IntSize];
    Arrays.fill(dict.word2int_, -1);
    for (int i = 0; i < dict.size_; i++) {
      dict.word2int_[dict.find(dict.words_.get(i).word)] = i;
    }

    return dict;
  }

  void init() {
    initTableDiscard();
    initNgrams();
  }

  /**
   * This looks like a linear probing hash table.
   */
  private int find(String w) {
    return find(w, hash(w));
  }

  private int find(String w, int h) {
    int word2IntSize = word2int_.length;
    int id = h % word2IntSize;
    while (word2int_[id] != -1 && !words_.get(word2int_[id]).word.equals(w)) {
      id = (id + 1) % word2IntSize;
    }
    return id;
  }

  void add(String w) {
    addWithCount(w, 1);
  }

  private void addWithCount(String w, int count) {
    int h = find(w);
    // if this is an empty slot. add a new entry.
    ntokens_ += count;
    if (word2int_[h] == -1) {
      Entry e = new Entry();
      e.word = w;
      e.count = count;
      e.type = getType(w);
      words_.add(e);
      word2int_[h] = size_++;
    } else {
      // or increment the count.
      words_.get(word2int_[h]).count += count;
    }
  }

  private int getType(String w) {
    return (w.startsWith(args_.label)) ? TYPE_LABEL : TYPE_WORD;
  }

  int nwords() {
    return nwords_;
  }

  int nlabels() {
    return nlabels_;
  }

  long ntokens() {
    return ntokens_;
  }

  int[] getSubWords(int i) {
    assert (i >= 0);
    assert (i < nwords_);
    return words_.get(i).subwords;
  }

  public List<String> getLabels() {
    List<String> results = new ArrayList<>();
    for (Entry entry : words_) {
      if (entry.type == TYPE_LABEL) {
        results.add(entry.word);
      }
    }
    return results;
  }

  int[] getSubWords(String word) {
    int i = getId(word);
    if (i >= 0) {
      return getSubWords(i);
    }
    if (!word.equals(EOS)) {
      return computeSubWords(BOW + word + EOW, i);
    } else {
      return new int[0];
    }
  }

  private boolean discard(int id, float rand) {
    assert (id >= 0);
    assert (id < nwords_);
    return rand > pdiscard_[id];
  }

  int getId(String w, int h) {
    int index = find(w, h);
    return word2int_[index];
  }


  int getId(String w) {
    int h = find(w);
    return word2int_[h];
  }

  int getType(int id) {
    assert (id >= 0);
    assert (id < size_);
    return words_.get(id).type;
  }

  String getWord(int id) {
    assert (id >= 0);
    assert (id < size_);
    return words_.get(id).word;
  }

  private int[] computeSubWords(String word, int wordId) {
    int[] hashes = args_.subWordHashProvider.getHashes(word, wordId);
    IntVector k = new IntVector();
    for (int hash : hashes) {
      pushHash(k, hash % args_.bucket);
    }
    return k.copyOf();
  }

  private void initNgrams() {
    for (int i = 0; i < size_; i++) {
      String word = BOW + words_.get(i).word + EOW;
      // adds the wordId to the n-grams as well.
      if (!words_.get(i).word.equals(EOS)) {
        words_.get(i).subwords = computeSubWords(word, i);
      }
    }
  }

  private void initTableDiscard() {
    pdiscard_ = new float[size_];
    for (int i = 0; i < size_; i++) {
      float f = ((float) words_.get(i).count) / ntokens_;
      pdiscard_[i] = (float) (Math.sqrt(args_.t / f) + args_.t / f);
    }
  }

  int[] getCounts(int entry_type) {
    int[] counts = entry_type == TYPE_WORD ? new int[nwords_] : new int[nlabels_];
    int c = 0;
    for (Entry entry : words_) {
      if (entry.type == entry_type) {
        counts[c] = entry.count;
        c++;
      }
    }
    return counts;
  }

  //adds word level n-grams hash values to input word index Vector.
  // n=1 means uni-grams, no value is added.
  void addWordNgramHashes(IntVector line, int n) {
    if (n == 1) {
      return;
    }
    int line_size = line.size();
    for (int i = 0; i < line_size; i++) {
      long h = line.get(i);
      for (int j = i + 1; j < line_size && j < i + n; j++) {
        h = h * 116049371 + line.get(j);
        pushHash(line, (int) (h % args_.bucket));
      }
    }
  }

  void addWordNgramHashes(IntVector line, IntVector hashes, int n) {
    for (int i = 0; i < hashes.size(); i++) {
      long h = hashes.get(i);
      for (int j = i + 1; j < hashes.size() && j < i + n; j++) {
        h = h * 116049371 + hashes.get(j);
        pushHash(line, (int) (h % args_.bucket));
      }
    }
  }

  void pushHash(IntVector hashes, int id) {
    if (pruneidx_size_ == 0 || id < 0) {
      return;
    }
    if (pruneidx_size_ > 0) {
      if (pruneidx_.containsKey(id)) {
        id = pruneidx_.get(id);
      } else {
        return;
      }
    }
    hashes.add(nwords_ + id);
  }


  int getLine(
      String line,
      IntVector words,
      Random random) {

    int ntokens = 0;
    List<String> tokens = tokenizer.splitToList(line);

    for (String token : tokens) {
      if (token.startsWith("#")) {
        continue;
      }
      int h = hash(token);
      int wid = getId(token, h);
      if (wid < 0) {
        continue;
      }
      ntokens++;
      if (getType(wid) == TYPE_WORD && !discard(wid, random.nextFloat())) {
        words.add(wid);
      }
      if (ntokens > MAX_LINE_SIZE || token.equals(EOS)) {
        break;
      }
    }
    return ntokens;
  }

  void addSubwords(IntVector line, String token, int wid) {
    if (wid < 0) { // out of vocab
      if (!token.equals(EOS)) {
        computeSubWords(BOW + token + EOW, wid);
      }
    } else {
      if (args_.maxn <= 0) { // in vocab w/o subwords
        line.add(wid);
      } else { // in vocab w/ subwords
        int[] ngrams = getSubWords(wid);
        line.addAll(ngrams);
      }
    }
  }

  int getLine(
      String line,
      IntVector words,
      IntVector labels) {

    IntVector wordHashes = new IntVector();
    int ntokens = 0;
    List<String> tokens = tokenizer.splitToList(line);

    for (String token : tokens) {
      if (token.startsWith("#")) {
        continue;
      }
      int h = hash(token);
      int wid = getId(token, h);
      int type = wid < 0 ? getType(token) : getType(wid);
      ntokens++;
      if (type == TYPE_WORD) {
        addSubwords(words, token, wid);
        wordHashes.add(h);
      } else if (type == TYPE_LABEL) {
        labels.add(wid - nwords_);
      }
      if (token.equals(EOS)) {
        break;
      }
    }
    addWordNgramHashes(words, wordHashes, args_.wordNgrams);
    return ntokens;
  }


  String getLabel(int lid) {
    if (lid < 0 || lid >= nlabels_) {
      throw new IllegalArgumentException
          (String.format("Label id %d is out of range [0, %d]", lid, nlabels_));
    }
    return words_.get(lid + nwords_).word;
  }

  int[] prune(int[] idx) {
    IntVector words = new IntVector();
    IntVector ngrams = new IntVector();
    for (int i : idx) {
      if (i < nwords_) {
        words.add(i);
      } else {
        ngrams.add(i);
      }
    }
    words.sort();
    IntVector newIndexes = new IntVector(words.copyOf());
    if (ngrams.size() > 0) {
      int j = 0;
      for (int k = 0; k < ngrams.size(); k++) {
        int ngram = ngrams.get(k);
        pruneidx_.put(ngram - nwords_, j);
        j++;
      }
      newIndexes.addAll(ngrams);
    }
    pruneidx_size_ = pruneidx_.size();
    Arrays.fill(word2int_, -1);

    int j = 0;
    for (int i = 0; i < words_.size(); i++) {
      if (getType(i) == TYPE_LABEL || (j < words.size() && words.get(j) == i)) {
        words_.set(j, words_.get(i));
        word2int_[find(words_.get(j).word)] = j;
        j++;
      }
    }
    nwords_ = words.size();
    size_ = nwords_ + nlabels_;
    words_ = new ArrayList<>(words_.subList(0, size_));
    initNgrams();
    return newIndexes.copyOf();
  }

  void save(DataOutputStream out) throws IOException {
    out.writeInt(size_);
    out.writeInt(nwords_);
    out.writeInt(nlabels_);
    out.writeLong(ntokens_);
    out.writeInt(pruneidx_size_);
    for (int i = 0; i < size_; i++) {
      Entry e = words_.get(i);
      out.writeUTF(e.word);
      out.writeInt(e.count);
      out.writeInt(e.type);
    }
    for (int key : pruneidx_.getKeys()) {
      out.writeInt(key);
      out.writeInt(pruneidx_.get(key));
    }
  }

  public static class Entry {

    String word;
    int count;
    int type;
    int[] subwords = new int[0];
  }

}
