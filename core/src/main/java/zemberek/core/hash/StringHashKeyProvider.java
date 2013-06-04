package zemberek.core.hash;

import com.google.common.collect.Lists;

import java.util.List;

public class StringHashKeyProvider implements IntHashKeyProvider {
    List<String> strings;

    public StringHashKeyProvider(List<String> strings) {
        this.strings = strings;
    }

    public StringHashKeyProvider(Iterable<String> iterable) {
        this.strings = Lists.newArrayList(iterable);
    }

    @Override
    public int[] getKey(int index) {
        String s = strings.get(index);
        int[] chars = new int[s.length()];
        for (int i = 0; i < chars.length; i++) {
            chars[i]=s.charAt(i);
        }
        return chars;
    }

    @Override
    public int keyAmount() {
        return strings.size();
    }
}
