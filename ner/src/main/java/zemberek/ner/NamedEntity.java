package zemberek.ner;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a sequence of named entity tokens.
 */
public class NamedEntity {

  String type;
  List<NerToken> tokens;

  public NamedEntity(String type, List<NerToken> tokens) {
    this.type = type;
    this.tokens = tokens;
  }

  public List<String> getWords() {
    List<String> s = new ArrayList<>(tokens.size());
    for (NerToken token : tokens) {
      s.add(token.word);
    }
    return s;
  }

  public List<NerToken> getTokens() {
    return tokens;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    NamedEntity that = (NamedEntity) o;

    if (!type.equals(that.type)) {
      return false;
    }
    return tokens.equals(that.tokens);
  }

  @Override
  public int hashCode() {
    int result = type.hashCode();
    result = 31 * result + tokens.hashCode();
    return result;
  }

  public String content() {
    List<String> content = new ArrayList<>();
    for (NerToken token : tokens) {
      content.add(token.word);
    }
    return String.join(" ", content);
  }

  @Override
  public String toString() {
    return "[" + type + " " + content() + "]";
  }
}
