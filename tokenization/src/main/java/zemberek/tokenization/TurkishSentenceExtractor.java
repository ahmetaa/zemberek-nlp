package zemberek.tokenization;

import com.google.common.base.Splitter;
import com.google.common.io.Resources;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.regex.Pattern;
import zemberek.core.collections.FloatValueMap;
import zemberek.core.collections.UIntSet;
import zemberek.core.io.IOUtil;
import zemberek.core.logging.Log;
import zemberek.core.text.TextIO;
import zemberek.core.turkish.TurkishAlphabet;

/**
 * This class is used for extracting sentences from paragraphs. For making boundary decisions it
 * uses a combination of rules and a binary averaged perceptron model. It only breaks paragraphs
 * from [.!?…] symbols. <p> Use the static DEFAULT singleton for the TurkishSentenceExtractor
 * instance that uses the internal extraction model.
 */
public class TurkishSentenceExtractor extends PerceptronSegmenter {

  /**
   * A singleton instance that is generated from the default internal model.
   */
  public static final TurkishSentenceExtractor DEFAULT = Singleton.Instance.extractor;

  static final String BOUNDARY_CHARS = ".!?…";
  private static final Pattern LINE_BREAK_PATTERN = Pattern.compile("[\n\r]+");
  private boolean doNotSplitInDoubleQuotes = false;

  private TurkishSentenceExtractor(FloatValueMap<String> weights) {
    this.weights = weights;
  }

  private TurkishSentenceExtractor(FloatValueMap<String> weights,
      boolean doNotSplitInDoubleQuotes) {
    this.weights = weights;
    this.doNotSplitInDoubleQuotes = doNotSplitInDoubleQuotes;
  }

  private static TurkishSentenceExtractor fromDefaultModel() throws IOException {
    try (DataInputStream dis = IOUtil.getDataInputStream(
        Resources.getResource("tokenization/sentence-boundary-model.bin").openStream())) {
      return new TurkishSentenceExtractor(load(dis));
    }
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {

    boolean _doNotSplitInDoubleQuotes = false;
    FloatValueMap<String> _model;

    public Builder doNotSplitInDoubleQuotes() {
      this._doNotSplitInDoubleQuotes = true;
      return this;
    }

    public Builder useModelFromResource(String resource) throws IOException {
      try (DataInputStream dis = IOUtil.getDataInputStream(
          Resources.getResource(resource).openStream())) {
        this._model = load(dis);
      }
      return this;
    }

    public Builder useModelFromPath(Path path) throws IOException {
      try (DataInputStream dis = IOUtil.getDataInputStream(path)) {
        this._model = load(dis);
      }
      return this;
    }

    public Builder useDefaultModel() {
      String resource = "tokenization/sentence-boundary-model.bin";
      try {
        useModelFromResource(resource);
      } catch (IOException e) {
        throw new IllegalStateException("Cannot find internal resource:" + resource);
      }
      return this;
    }

    public TurkishSentenceExtractor build() {
      if (_model == null) {
        useDefaultModel();
      }
      return new TurkishSentenceExtractor(_model, _doNotSplitInDoubleQuotes);
    }
  }

  /**
   * Extracts sentences from a list if paragraph strings. This method does not split from line
   * breaks assuming paragraphs do not contain line breaks. <p> If content contains line breaks, use
   * {@link #fromDocument(String)}
   *
   * @param paragraphs a String List representing multiple paragraphs.
   * @return a list of String representing sentences.
   */
  public List<String> fromParagraphs(Collection<String> paragraphs) {
    List<String> result = new ArrayList<>();
    for (String paragraph : paragraphs) {
      result.addAll(fromParagraph(paragraph));
    }
    return result;
  }

  int[] boundaryIndexes(String paragraph) {
    List<Span> spans = extractToSpans(paragraph);
    int[] indexes = new int[spans.size() * 2];
    int i = 0;
    for (Span span : spans) {
      indexes[i] = span.start;
      indexes[i + 1] = span.end;
      i += 2;
    }
    return indexes;
  }

  // TODO: doNotSplitInDoubleQuotes may not be suitable for some cases.
  // such as for paragraph: "Merhaba. Nasılsın?"
  private List<Span> extractToSpans(String paragraph) {
    List<Span> spans = new ArrayList<>();
    List<Span> quoteSpans = null;
    if (doNotSplitInDoubleQuotes) {
      quoteSpans = doubleQuoteSpans(paragraph);
    }
    int begin = 0;
    for (int j = 0; j < paragraph.length(); j++) {

      // skip if char cannot be a boundary char.
      char chr = paragraph.charAt(j);
      if (BOUNDARY_CHARS.indexOf(chr) < 0) {
        continue;
      }

      // skip if breaking is not allowed between double quotes.
      if (doNotSplitInDoubleQuotes && quoteSpans != null && inSpan(j, quoteSpans)) {
        continue;
      }

      BoundaryData boundaryData = new BoundaryData(paragraph, j);
      if (boundaryData.nonBoundaryCheck()) {
        continue;
      }
      List<String> features = boundaryData.extractFeatures();
      double score = 0;
      for (String feature : features) {
        score += weights.get(feature);
      }
      if (score > 0) {
        Span span = new Span(begin, j + 1);
        if (span.length() > 0) {
          spans.add(span);
        }
        begin = j + 1;
      }
    }

    if (begin < paragraph.length()) {
      Span span = new Span(begin, paragraph.length());
      if (span.length() > 0) {
        spans.add(span);
      }
    }
    return spans;
  }

  private static final String doubleQuotes = "\"”“»«";

  /**
   * Finds double quote spans.
   */
  private List<Span> doubleQuoteSpans(String input) {
    List<Span> spans = new ArrayList<>();

    int start = -1;
    boolean started = false;
    for (int j = 0; j < input.length(); j++) {
      char c = input.charAt(j);
      if (doubleQuotes.indexOf(c) >= 0) {
        if (!started) {
          start = j;
          started = true;
        } else {
          spans.add(new Span(start, j));
          started = false;
        }
      }
    }
    return spans;
  }

  private boolean inSpan(int index, List<Span> spans) {
    for (Span span : spans) {
      if (span.start > index) {
        return false;
      }
      if (span.inSpan(index)) {
        return true;
      }
    }
    return false;
  }


  /**
   * Extracts sentences from a paragraph string. This method does not split from line breaks
   * assuming paragraphs do not contain line breaks. <p> If content contains line breaks, use {@link
   * #fromDocument(String)}
   *
   * @param paragraph a String representing a paragraph of text.
   * @return a list of String representing sentences.
   */
  public List<String> fromParagraph(String paragraph) {
    List<Span> spans = extractToSpans(paragraph);
    List<String> sentences = new ArrayList<>(spans.size());
    for (Span span : spans) {
      String sentence = span.getSubstring(paragraph).trim();
      if (sentence.length() > 0) {
        sentences.add(sentence);
      }
    }
    return sentences;
  }

  /**
   * Extracts sentences from a string that represents a document text. This method first splits the
   * String from line breaks to paragraphs. After that it calls {@link #fromParagraphs(Collection)}
   * for extracting sentences from multiple paragraphs.
   *
   * @param document a String List representing a complete document's text content.
   * @return a list of String representing sentences.
   */
  public List<String> fromDocument(String document) {
    List<String> lines = Splitter.on(LINE_BREAK_PATTERN).splitToList(document);
    return fromParagraphs(lines);
  }

  public char[] getBoundaryCharacters() {
    return BOUNDARY_CHARS.toCharArray();
  }

  private enum Singleton {
    Instance;
    TurkishSentenceExtractor extractor;

    Singleton() {
      try {
        extractor = fromDefaultModel();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  public static class TrainerBuilder {

    Path trainFile;
    int iterationCount = 20;
    int skipSpaceFrequency = 20;
    float learningRate = 0.1f;
    int lowerCaseFirstLetterFrequency = 20;
    boolean shuffleInput = false;

    public TrainerBuilder(Path trainFile) {
      this.trainFile = trainFile;
    }

    public TrainerBuilder iterationCount(int count) {
      this.iterationCount = count;
      return this;
    }

    public TrainerBuilder shuffleSentences() {
      this.shuffleInput = true;
      return this;
    }

    /**
     * In every [count] sentence, trainer skips the space after sentence boundary punctuation.
     */
    public TrainerBuilder skipSpaceFrequency(int count) {
      this.skipSpaceFrequency = count;
      return this;
    }

    public TrainerBuilder learningRate(float learningRate) {
      this.learningRate = learningRate;
      return this;
    }

    /**
     * In every [count] sentence, trainer lower cases the character after sentence boundary
     * punctuation.
     */
    public TrainerBuilder lowerCaseFirstLetterFrequency(int count) {
      this.lowerCaseFirstLetterFrequency = count;
      return this;
    }

    public Trainer build() {
      return new Trainer(this);
    }
  }

  public static class Trainer {

    private static Locale Turkish = new Locale("tr");
    private TrainerBuilder builder;

    private Trainer(TrainerBuilder builder) {
      this.builder = builder;
    }

    public static TrainerBuilder builder(Path trainFile) {
      return new TrainerBuilder(trainFile);
    }

    public TurkishSentenceExtractor train() throws IOException {
      FloatValueMap<String> weights = new FloatValueMap<>();
      List<String> sentences = TextIO.loadLines(builder.trainFile);
      FloatValueMap<String> averages = new FloatValueMap<>();

      Random rnd = new Random(1);

      int updateCount = 0;

      for (int i = 0; i < builder.iterationCount; i++) {

        Log.info("Iteration = %d", i + 1);

        UIntSet indexSet = new UIntSet();
        StringBuilder sb = new StringBuilder();
        int boundaryIndexCounter;
        int sentenceCounter = 0;
        if (builder.shuffleInput) {
          Collections.shuffle(sentences);
        }
        for (String sentence : sentences) {
          if (sentence.trim().length() == 0) {
            continue;
          }
          // sometimes make first letter of the sentence lower case.
          if (rnd.nextInt(builder.lowerCaseFirstLetterFrequency) == 0) {
            sentence = sentence.substring(0, 1).toLowerCase(Turkish) + sentence.substring(1);
          }
          sb.append(sentence);
          boundaryIndexCounter = sb.length() - 1;
          indexSet.add(boundaryIndexCounter);
          // in some sentences skip adding a space between sentences.
          if (rnd.nextInt(builder.skipSpaceFrequency) != 1
              && sentenceCounter < sentences.size() - 1) {
            sb.append(" ");
          }
          sentenceCounter++;
        }
        String joinedSentence = sb.toString();

        for (int j = 0; j < joinedSentence.length(); j++) {
          // skip if char cannot be a boundary char.
          char chr = joinedSentence.charAt(j);
          if (BOUNDARY_CHARS.indexOf(chr) < 0) {
            continue;
          }
          BoundaryData boundaryData = new BoundaryData(joinedSentence, j);
          if (boundaryData.nonBoundaryCheck()) {
            continue;
          }
          List<String> features = boundaryData.extractFeatures();
          float score = 0;
          for (String feature : features) {
            score += weights.get(feature);
          }
          float update = 0;
          // if we found no-boundary but it is a boundary
          if (score <= 0 && indexSet.contains(j)) {
            update = builder.learningRate;
          }
          // if we found boundary but it is not a boundary
          else if (score > 0 && !indexSet.contains(j)) {
            update = -builder.learningRate;
          }
          updateCount++;
          if (update != 0) {
            for (String feature : features) {
              double d = weights.incrementByAmount(feature, update);
              if (d == 0.0) {
                weights.remove(feature);
              }
              d = averages.incrementByAmount(feature, updateCount * update);
              if (d == 0.0) {
                averages.remove(feature);
              }
            }
          }
        }
      }
      for (String key : weights) {
        weights.set(key, weights.get(key) - averages.get(key) * 1f / updateCount);
      }

      return new TurkishSentenceExtractor(weights);
    }
  }

  static class BoundaryData {

    char currentChar;
    char previousLetter;
    char nextLetter;
    String previousTwoLetters;
    String nextTwoLetters;
    String currentWord;
    String nextWord;
    String currentWordNoPunctuation;
    String rightChunk;
    String rightChunkUntilBoundary;
    String leftChunk;
    String leftChunkUntilBoundary;

    BoundaryData(String input, int pointer) {

      previousLetter = pointer > 0 ? input.charAt(pointer - 1) : '_';
      nextLetter = pointer < input.length() - 1 ? nextLetter = input.charAt(pointer + 1) : '_';
      previousTwoLetters = pointer > 2 ? input.substring(pointer - 2, pointer) : "__";
      nextTwoLetters =
          pointer < input.length() - 3 ? input.substring(pointer + 1, pointer + 3) : "__";

      int previousSpace = findBackwardsSpace(input, pointer);
      if (previousSpace < 0) {
        previousSpace = 0;
      }
      leftChunk = input.substring(previousSpace, pointer);

      int previousBoundaryOrSpace = findBackwardsSpaceOrChar(input, pointer, '.');
      if (previousBoundaryOrSpace < 0) {
        previousBoundaryOrSpace = 0;
      }
      leftChunkUntilBoundary = previousBoundaryOrSpace == previousSpace ?
          leftChunk : input.substring(previousBoundaryOrSpace, pointer);

      int nextSpace = findForwardsSpace(input, pointer);
      rightChunk = "";
      if (pointer < input.length() - 1) {
        rightChunk = input.substring(pointer + 1, nextSpace);
      }
      int nextBoundaryOrSpace = findForwardsSpaceOrChar(input, pointer, '.');
      rightChunkUntilBoundary = nextSpace == nextBoundaryOrSpace ?
          rightChunk : input.substring(pointer + 1, nextBoundaryOrSpace);

      currentChar = input.charAt(pointer);
      currentWord = leftChunk + currentChar + rightChunk;
      currentWordNoPunctuation = currentWord.replaceAll("[.!?…]", "");

      StringBuilder sb = new StringBuilder();
      int j = nextSpace;
      while (j < input.length()) {
        char c = input.charAt(j);
        if (c == ' ') {
          break;
        }
        sb.append(c);
        j++;
      }
      nextWord = sb.toString();
    }

    int findBackwardsSpace(String input, int pos) {
      return findBackwardsSpaceOrChar(input, pos, ' ');
    }

    int findBackwardsSpaceOrChar(String input, int pos, char chr) {
      int i = pos - 1;
      while (i >= 0) {
        char c = input.charAt(i);
        if (c == ' ' || c == chr) {
          i++;
          break;
        }
        i--;
      }
      return i;
    }

    int findForwardsSpace(String input, int pos) {
      return findForwardsSpaceOrChar(input, pos, ' ');
    }

    int findForwardsSpaceOrChar(String input, int pos, char chr) {
      int j = pos + 1;
      while (j < input.length()) {
        char c = input.charAt(j);
        if (c == ' ' || c == chr) {
          break;
        }
        j++;
      }
      return j;
    }

    boolean nonBoundaryCheck() {
      return (leftChunkUntilBoundary.length() == 1)
          || nextLetter == '\''
          || BOUNDARY_CHARS.indexOf(nextLetter) >= 0
          || TurkishAbbreviationSet.contains(currentWord)
          || TurkishAbbreviationSet.contains(leftChunkUntilBoundary)
          || potentialWebSite(currentWord);
    }

    List<String> extractFeatures() {
      List<String> features = new ArrayList<>();
      features.add("1:" + Character.isUpperCase(previousLetter));
      features.add("1b:" + Character.isWhitespace(nextLetter));
      features.add("1a:" + previousLetter);
      features.add("1b:" + nextLetter);
      features.add("2p:" + previousTwoLetters);
      features.add("2n:" + nextTwoLetters);

      if (currentWord.length() > 0) {
        features.add("7c:" + Character.isUpperCase(currentWord.charAt(0)));
        features.add("9c:" + getMetaChars(currentWord));
      }

      if (rightChunk.length() > 0) {
        features.add("7r:" + Character.isUpperCase(rightChunk.charAt(0)));
        features.add("9r:" + getMetaChars(rightChunk));
        if (!TurkishAlphabet.INSTANCE.containsVowel(rightChunk)) {
          features.add("rcc:true");
        }
      }
      if (nextWord.length() > 0) {
        features.add("7n:" + Character.isUpperCase(nextWord.charAt(0)));
        features.add("9n:" + getMetaChars(nextWord));
      }

      if (leftChunk.length() > 0) {
        if (!TurkishAlphabet.INSTANCE.containsVowel(leftChunk)) {
          features.add("lcc:true");
        }
      }

      if (currentWordNoPunctuation.length() > 0) {
        boolean allUp = true;
        boolean allDigit = true;
        for (int j = 0; j < currentWordNoPunctuation.length(); j++) {
          char c = currentWordNoPunctuation.charAt(j);
          if (!Character.isUpperCase(c)) {
            allUp = false;
          }
          if (!Character.isDigit(c)) {
            allDigit = false;
          }
        }

        if (allUp) {
          features.add("11u:true");
        }
        if (allDigit) {
          features.add("11d:true");
        }
      }
      return features;
    }
  }


}