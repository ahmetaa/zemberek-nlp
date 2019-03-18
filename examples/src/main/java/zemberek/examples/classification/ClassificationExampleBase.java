package zemberek.examples.classification;

import com.google.common.base.Splitter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import zemberek.core.turkish.Turkish;
import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.analysis.SentenceAnalysis;
import zemberek.morphology.analysis.SentenceWordAnalysis;
import zemberek.morphology.analysis.SingleAnalysis;
import zemberek.normalization.TurkishSentenceNormalizer;
import zemberek.tokenization.TurkishTokenizer;
import zemberek.tokenization.Token;
import zemberek.tokenization.Token.Type;

public class ClassificationExampleBase {

  protected static TurkishMorphology morphology;
  protected static TurkishSentenceNormalizer normalizer;

  protected void generateSetWithLemmas(List<String> lines, Path lemmasPath) throws IOException {
    List<String> lemmas = lines
        .stream()
        .map(this::replaceWordsWithLemma)
        .map(this::removeNonWords)
        .map(s -> s.toLowerCase(Turkish.LOCALE))
        .collect(Collectors.toList());
    Files.write(lemmasPath, lemmas);
  }

  protected void generateSetWithSplit(List<String> lines, Path splitPath) throws IOException {
    List<String> lemmas = lines
        .stream()
        .map(this::splitWords)
        .map(this::removeNonWords)
        .map(s -> s.toLowerCase(Turkish.LOCALE))
        .collect(Collectors.toList());
    Files.write(splitPath, lemmas);
  }

  protected void generateSetTokenized(List<String> lines, Path tokenizedPath) throws IOException {
    List<String> tokenized = lines
        .stream()
        .map(s -> String.join(" ", TurkishTokenizer.DEFAULT.tokenizeToStrings(s)))
        .map(this::removeNonWords)
        .map(s -> s.toLowerCase(Turkish.LOCALE))
        .collect(Collectors.toList());
    Files.write(tokenizedPath, tokenized);
  }

  protected String splitWords(String sentence) {

    List<String> tokens = Splitter.on(" ").splitToList(sentence);
    // assume first is label. Remove label from sentence for morphological analysis.
    String label = tokens.get(0);
    tokens = tokens.subList(1, tokens.size());
    sentence = String.join(" ", tokens);

    if (sentence.length() == 0) {
      return sentence;
    }
    SentenceAnalysis analysis = morphology.analyzeAndDisambiguate(sentence);
    List<String> res = new ArrayList<>();
    // add label first.
    res.add(label);
    for (SentenceWordAnalysis e : analysis) {
      SingleAnalysis best = e.getBestAnalysis();
      String input = e.getWordAnalysis().getInput();
      if (best.isUnknown()) {
        res.add(input);
        continue;
      }
      List<String> lemmas = best.getLemmas();
      String l = lemmas.get(0);
      if (l.length() < input.length()) {
        res.add(l);
        String substring = input.substring(l.length());
        res.add("_" + substring);
      } else {
        res.add(l);
      }
    }
    return String.join(" ", res);
  }

  String processEnding(String input) {
    return input.replaceAll("[ae]", "A").
        replaceAll("[ıiuü]", "I")
        .replaceAll("[kğ]", "K")
        .replaceAll("[cç]", "C")
        .replaceAll("[dt]", "D");
  }

  protected String replaceWordsWithLemma(String sentence) {

    List<String> tokens = Splitter.on(" ").splitToList(sentence);
    // assume first is label. Remove label from sentence for morphological analysis.
    String label = tokens.get(0);
    tokens = tokens.subList(1, tokens.size());
    sentence = String.join(" ", tokens);

    if (sentence.length() == 0) {
      return sentence;
    }
    SentenceAnalysis analysis = morphology.analyzeAndDisambiguate(sentence);
    List<String> res = new ArrayList<>();
    // add label first.
    res.add(label);
    for (SentenceWordAnalysis e : analysis) {
      SingleAnalysis best = e.getBestAnalysis();
      if (best.isUnknown()) {
        res.add(e.getWordAnalysis().getInput());
        continue;
      }
      List<String> lemmas = best.getLemmas();
      res.add(lemmas.get(0));
    }
    return String.join(" ", res);
  }

  protected String removeNonWords(String sentence) {
    List<Token> docTokens = TurkishTokenizer.DEFAULT.tokenize(sentence);
    List<String> reduced = new ArrayList<>(docTokens.size());
    for (Token token : docTokens) {
      String text = token.getText();

      // skip label and ending words.
      if (text.startsWith("_") || text.contains("__")) {
        reduced.add(text);
        continue;
      }

      Token.Type type = token.getType();
      if (
          type == Token.Type.Mention ||
              type == Token.Type.HashTag ||
              type == Token.Type.URL ||
              type == Token.Type.Punctuation ||
              type == Type.RomanNumeral ||
              type == Token.Type.Time ||
              type == Token.Type.UnknownWord ||
              type == Token.Type.Unknown) {
        continue;
      }
      reduced.add(text);
    }
    return String.join(" ", reduced);
  }

}
