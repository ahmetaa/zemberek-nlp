package zemberek.ner;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

class NerSentence {
    String content;
    List<NerToken> tokens;

    public NerSentence(String content, List<NerToken> tokens) {
        this.content = content;
        this.tokens = tokens;
    }

    public List<NamedEntity> getNamedEntities() {
        List<NamedEntity> namedEntities = new ArrayList<>();
        List<NerToken> neTokens = new ArrayList<>();
        for (int i = 0; i < tokens.size(); i++) {
            NerToken token = tokens.get(i);
            if (token.position == NePosition.UNIT || token.position == NePosition.LAST) {
                neTokens.add(token);
                namedEntities.add(new NamedEntity(token.type, neTokens));
                neTokens = new ArrayList<>();
                continue;
            }
            if (token.position == NePosition.OUTSIDE) {
                continue;
            }
            neTokens.add(token);
        }
        return namedEntities;
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
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NerSentence that = (NerSentence) o;

        if (!content.equals(that.content)) return false;
        return tokens.equals(that.tokens);
    }

    @Override
    public int hashCode() {
        int result = content.hashCode();
        result = 31 * result + tokens.hashCode();
        return result;
    }
}
