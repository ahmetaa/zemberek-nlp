package zemberek.ner;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;

public class NerSentence {

  String content;
  List<NerToken> tokens;

  public NerSentence(String content, List<NerToken> tokens) {
    this.content = content;
    this.tokens = tokens;
  }

  public List<NamedEntity> getNamedEntities() {
    return getAllEntities().stream()
        .filter(s -> !s.type.equals(NerDataSet.OUT_TOKEN_TYPE)).collect(Collectors.toList());
  }

  public List<NamedEntity> getAllEntities() {
    List<NamedEntity> namedEntities = new ArrayList<>();
    List<NerToken> neTokens = new ArrayList<>();
    for (NerToken token : tokens) {
      if (token.position == NePosition.UNIT
          || token.position == NePosition.LAST ||
          token.position == NePosition.OUTSIDE) {
        neTokens.add(token);
        namedEntities.add(new NamedEntity(token.type, neTokens));
        neTokens = new ArrayList<>();
        continue;
      }
      neTokens.add(token);
    }
    return namedEntities;
  }

  public String getAsTrainingSentence(NerDataSet.AnnotationStyle style) {
    List<NamedEntity> all = getAllEntities();
    List<String> tokens = new ArrayList<>();
    for (NamedEntity namedEntity : all) {
      if (namedEntity.type.equals(NerDataSet.OUT_TOKEN_TYPE)) {
        tokens.add(namedEntity.tokens.get(0).word);
      } else {
        switch (style) {
          case ENAMEX:
            tokens.add("<b_enamex TYPE=\"" + namedEntity.type + "\">");
            break;
          case OPEN_NLP:
            tokens.add("<START:" + namedEntity.type + ">");
            break;
          case BRACKET:
            tokens.add("[" + namedEntity.type);
            break;
        }
        for (NerToken token : namedEntity.tokens) {
          tokens.add(token.word);
        }
        switch (style) {
          case ENAMEX:
            tokens.add("<e_enamex>");
            break;
          case OPEN_NLP:
            tokens.add("<END>");
            break;
          case BRACKET:
            tokens.add("]");
            break;
        }
      }
    }
    return String.join(" ", tokens);
  }

  List<NamedEntity> matchingNEs(List<NamedEntity> nes) {
    LinkedHashSet<NamedEntity> set = new LinkedHashSet<>(getNamedEntities());
    List<NamedEntity> result = new ArrayList<>();
    for (NamedEntity ne : nes) {
      if (set.contains(ne)) {
        result.add(ne);
      }
    }
    return result;
  }

  @Override
  public String toString() {
    return "NerSentence{" +
        "content='" + content + '\'' +
        ", tokens=" + tokens +
        '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    NerSentence that = (NerSentence) o;

    if (!content.equals(that.content)) {
      return false;
    }
    return tokens.equals(that.tokens);
  }

  @Override
  public int hashCode() {
    int result = content.hashCode();
    result = 31 * result + tokens.hashCode();
    return result;
  }
}
