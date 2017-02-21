package zemberek.core.io;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.Map;

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

}
