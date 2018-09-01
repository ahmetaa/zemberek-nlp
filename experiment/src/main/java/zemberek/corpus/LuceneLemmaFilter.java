package zemberek.corpus;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.util.TokenFilterFactory;
import org.apache.lucene.util.AttributeSource;
import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.analysis.WordAnalysis;

public class LuceneLemmaFilter extends TokenFilter {

  private Deque<String> lemmas = new ArrayDeque<>();

  private AttributeSource.State current;
  private final CharTermAttribute termAttribute = addAttribute(CharTermAttribute.class);
  private final PositionIncrementAttribute posIncrAtt = addAttribute(
      PositionIncrementAttribute.class);

  private static final TurkishMorphology morphology = Singleton.INSNTANCE.morphology;

  private enum Singleton {
    INSNTANCE;
    TurkishMorphology morphology;

    Singleton() {
      morphology = TurkishMorphology.createWithDefaults();
    }
  }

  public LuceneLemmaFilter(TokenStream input) {
    super(input);
  }

  @Override
  public final boolean incrementToken() throws IOException {
    if (lemmas.size() > 0) {
      String l = lemmas.remove();
      restoreState(current);
      termAttribute.setEmpty().append(l);
      posIncrAtt.setPositionIncrement(0);
      return true;
    }
    if (!input.incrementToken()) {
      return false;
    }

    if (addLemmas()) {
      current = captureState();
    }

    return true;
  }

  private boolean addLemmas() {
    String word = termAttribute.toString();
    WordAnalysis analysis = morphology.analyze(word);
    Set<String> l = new HashSet<>(5);
    //l.add(word);
    analysis.forEach(s -> l.addAll(s.getLemmas()));
    lemmas = new ArrayDeque<>(l);
    return true;
  }

  public static class Factory extends TokenFilterFactory {

    public Factory(Map<String, String> args) {
      super(args);
    }

    @Override
    public TokenStream create(TokenStream input) {
      return new LuceneLemmaFilter(input);
    }
  }
}

