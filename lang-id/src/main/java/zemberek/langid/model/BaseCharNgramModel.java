package zemberek.langid.model;

import com.google.common.collect.Lists;

import java.util.List;

public abstract class BaseCharNgramModel {
    public final String id;
    public final int order;
    public static final String UNKNOWN = "unk";
    protected BaseCharNgramModel(String id, int order) {
        this.id = id;
        this.order = order;
    }

    public List<String> getGram(String s, int order) {
        List<String> grams = Lists.newArrayList();
        for (int i = 0; i < (s.length() - order + 1); ++i) {
            grams.add(s.substring(i, i + order));
        }
        return grams;
    }

    public static List<String> getGrams(String input, int order) {
        List<String> grams = Lists.newArrayListWithCapacity(input.length());
        for (int i = 0; i < order - 1; i++) {
            if (i == input.length())
                return grams;
            grams.add(input.substring(0, i + 1));
        }
        for (int i = 0; i < input.length() - order +1; i++) {
            grams.add(input.substring(i, i + order));
        }
        return grams;
    }
}
