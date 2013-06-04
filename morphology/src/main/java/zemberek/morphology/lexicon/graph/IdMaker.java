package zemberek.morphology.lexicon.graph;

import java.util.Collections;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class IdMaker {
    Random random = new Random();
    Set<String> ids = Collections.synchronizedSet(new HashSet<String>());
    AtomicInteger counter = new AtomicInteger();
    int letterCount;

    public IdMaker(int letterCount) {
        this.letterCount = letterCount;
    }

    public String get() {
        String val = String.valueOf(this.counter.incrementAndGet());
        ids.add(val);
        return val;
    }

    public String getRandom() {
        StringBuilder sb = new StringBuilder(letterCount);
        for (int i = 0; i < letterCount; i++) {
            sb.append((char) (random.nextInt(25) + 'A'));
        }
        String res = sb.toString();
        if (!ids.contains(res)) {
            ids.add(res);
            return res;
        }
        return getRandom();
    }

    public String get(String toAppend) {
        return toAppend + "_" + get();
    }
}
