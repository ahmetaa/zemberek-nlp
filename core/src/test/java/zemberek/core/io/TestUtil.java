package zemberek.core.io;

import java.util.Map;

public class TestUtil {

    public static <T> boolean containsAllKeys(Map<T, ?> map, T... keys) {
        for (T key : keys) {
            if (!map.containsKey(key)) return false;
        }
        return true;
    }

    public static <V> boolean containsAllValues(Map<?, V> map, V... values) {
        for (V value : values) {
            if (!map.containsValue(value)) return false;
        }
        return true;
    }

}
