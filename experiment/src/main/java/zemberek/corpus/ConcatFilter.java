package zemberek.corpus;

import com.google.common.base.Splitter;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.util.TokenFilterFactory;
import org.apache.lucene.util.AttributeSource;

public final class ConcatFilter extends TokenFilter {

  Deque<String> lemmas = new ArrayDeque<>();

  private AttributeSource.State current;
  private final CharTermAttribute termAttribute = addAttribute(CharTermAttribute.class);
  private final PositionIncrementAttribute posIncrAtt = addAttribute(
      PositionIncrementAttribute.class);

  public ConcatFilter(TokenStream input) {
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
    lemmas = new ArrayDeque<>(Splitter.on('|').splitToList(word));
    return true;
  }

  public static class Factory extends TokenFilterFactory {

    public Factory(Map<String, String> args) {
      super(args);
    }

    @Override
    public TokenStream create(TokenStream input) {
      return new ConcatFilter(input);
    }
  }
}
