package zemberek.morphology.structure;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

import java.util.*;

/**
 * a sequence of tokens. usually used for representing a sentence.
 * this class is immutable
 */
public class TokenSequence {

    final String[] words;
    public static final String SENTENCE_START = "<s>";
    public static final String SENTENCE_END = "</s>";

    public TokenSequence(List<String> words) {
        if (words == null)
            throw new IllegalArgumentException("cannot create a sequence with a null list.");
        this.words = words.toArray(new String[words.size()]);
    }

    public TokenSequence(String[] words) {
        if (words == null)
            throw new IllegalArgumentException("cannot create a sequence with a null list.");
        this.words = words.clone();
    }

    public TokenSequence(String spaceSeparatedWords) {
        this(Lists.newArrayList(separate(spaceSeparatedWords)));
    }

    public static class Builder {
        List<String> tokens = new ArrayList<String>();

        public Builder add(String token) {
            tokens.add(token);
            return this;
        }

        public Builder add(String... tokenz) {
            tokens.addAll(Arrays.asList(tokenz));
            return this;
        }

        public TokenSequence build() {
            return new TokenSequence(tokens);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    private static Iterable<String> separate(String spaceSeparatedWords) {
        return Splitter.on(" ").omitEmptyStrings().trimResults().split(spaceSeparatedWords);
    }

    public static TokenSequence fromStartEndTaggedSequence(String spaceSeparatedWords) {
        Iterator<String> it = separate(spaceSeparatedWords).iterator();
        List<String> list = new ArrayList<String>();
        while (it.hasNext()) {
            String s = it.next();
            if (s.equalsIgnoreCase(SENTENCE_START) || s.equalsIgnoreCase(SENTENCE_END))
                continue;
            list.add(s);
        }
        return new TokenSequence(list);
    }

    public String[] getTokens() {
        return words;
    }

    public String asString() {
        return Joiner.on(" ").join(words);
    }

    public int size() {
        return words.length;
    }

    public String get(int i) {
        return words[i];
    }

    public boolean isEmpty() {
        return words.length == 0;
    }

    public String last() {
        if (isEmpty())
            return "";
        return words[words.length - 1];
    }

    public String first() {
        if (isEmpty())
            return "";
        return words[0];
    }

    public String asASRCorpusSentence() {
        return SENTENCE_START + " " + asString() + " " + SENTENCE_END;
    }

    @Override
    public String toString() {
        return asString();
    }

    public List<String> getGrams(int gramSize) {
        int size = size();
        if (size < 2)
            return Collections.emptyList();
        if (size < gramSize)
            return Lists.newArrayList(asString());
        ArrayList<String> result = new ArrayList<String>(size());
        for (int i = 0; i < words.length - gramSize + 1; i++) {
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < gramSize; j++) {
                sb.append(words[i + j]);
                if (j != gramSize - 1)
                    sb.append(" ");
            }
            result.add(sb.toString());
        }
        return result;
    }
}
