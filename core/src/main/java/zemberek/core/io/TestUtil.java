package zemberek.core.io;

import zemberek.core.ScoredItem;
import zemberek.core.collections.FloatValueMap;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class TestUtil {

    @SafeVarargs
    public static <T> boolean containsAllKeys(Map<T, ?> map, T... keys) {
        for (T key : keys) {
            if (!map.containsKey(key)) return false;
        }
        return true;
    }

    @SafeVarargs
    public static <V> boolean containsAllValues(Map<?, V> map, V... values) {
        for (V value : values) {
            if (!map.containsValue(value)) return false;
        }
        return true;
    }

    public static Path tempFileWithData(Collection<String> collection) throws IOException {
        Path temp = java.nio.file.Files.createTempFile("", "");
        temp.toFile().deleteOnExit();
        java.nio.file.Files.write(temp, collection);
        return temp;
    }

    @SafeVarargs
    public static <T> boolean containsAll(FloatValueMap<T> set, T... items) {
        for (T item : items) {
            if (!set.contains(item)) {
                return false;
            }
        }
        return true;
    }

    @SafeVarargs
    public static <T> boolean containsAll(List<ScoredItem<T>> list, T... items) {
        Set<T> set = new HashSet<>();
        set.addAll(list.stream().map(s1 -> s1.item).collect(Collectors.toList()));
        for (T item : items) {
            if (!set.contains(item)) {
                return false;
            }
        }
        return true;
    }


    public static Set<String> uniqueStrings(int amount, int stringLength) {
        return uniqueStrings(amount, stringLength, -1);
    }

    public static Set<String> uniqueStrings(int amount, int stringLength, int randomSeed) {
        Set<String> set = new HashSet<>(amount);

        Random r = randomSeed == -1 ? new Random() : new Random(randomSeed);
        while (set.size() < amount) {
            StringBuilder sb = new StringBuilder(stringLength);
            for (int i = 0; i < stringLength; i++) {
                sb.append((char) (r.nextInt(32) + 'a'));
            }
            set.add(sb.toString());
        }
        return set;
    }

}
