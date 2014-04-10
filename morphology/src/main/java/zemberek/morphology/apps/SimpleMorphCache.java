package zemberek.morphology.apps;

import com.google.common.collect.Maps;
import zemberek.morphology.parser.MorphParse;
import zemberek.morphology.parser.MorphParser;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

public class SimpleMorphCache {
    private final HashMap<String, List<MorphParse>> cache;
    private long hit = 0;
    private long miss = 0;

    public SimpleMorphCache(MorphParser parser, List<String> wordList) throws IOException {
        cache = Maps.newHashMapWithExpectedSize(5000);
        for (String s : wordList) {
            cache.put(s, parser.parse(s));
        }
    }

    public List<MorphParse> parse(String s) {
        List<MorphParse> result = cache.get(s);
        if (result != null) {
            hit++;
        } else {
            miss++;
        }
        return result;
    }

    @Override
    public String toString() {
        return "Hits: " + hit + " Miss: " + miss + " Hit ratio: %" + (hit / (double)(hit + miss) * 100);
    }
}
