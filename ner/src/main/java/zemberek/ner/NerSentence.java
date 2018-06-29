package zemberek.ner;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;

class NerSentence {

  String content;
  List<NerToken> tokens;

  public NerSentence(String content, List<NerToken> tokens) {
    this.content = content;
    this.tokens = tokens;
  }

  public List<NamedEntity> getNamedEntities() {
    return getNamedEntitiesAll().stream()
        .filter(s->!s.type.equals("OUT")).collect(Collectors.toList());
  }

  private List<NamedEntity> getNamedEntitiesAll() {
    List<NamedEntity> namedEntities = new ArrayList<>();
    List<NerToken> neTokens = new ArrayList<>();
    for (int i = 0; i < tokens.size(); i++) {
      NerToken token = tokens.get(i);
      if (token.position == NePosition.UNIT
          || token.position == NePosition.LAST ||
          token.position == NePosition.OUTSIDE ) {
        neTokens.add(token);
        namedEntities.add(new NamedEntity(token.type, neTokens));
        neTokens = new ArrayList<>();
        continue;
      }
      neTokens.add(token);
    }
    return namedEntities;
  }

  public String getAsTrainingSentence() {
    List<NamedEntity> all = getNamedEntitiesAll();
    List<String> tokens = new ArrayList<>();
    for (NamedEntity namedEntity : all) {
      if(namedEntity.type.equals("OUT")) {
        tokens.add(namedEntity.tokens.get(0).word);
      } else {
        tokens.add("[" + namedEntity.type);
        for(NerToken token : namedEntity.tokens) {
          tokens.add(token.word);
        }
        tokens.add("]");
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
