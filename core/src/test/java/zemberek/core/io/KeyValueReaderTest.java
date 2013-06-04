package zemberek.core.io;

import com.google.common.io.Resources;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Map;

public class KeyValueReaderTest {

    URL key_value_colon_separator = Resources.getResource("io/key-value-colon-separator.txt");

    @Test
    public void testReader() throws IOException {
        Map<String, String> map = new KeyValueReader(":")
                .loadFromFile(new File(key_value_colon_separator.getFile()));
        Assert.assertEquals(map.size(), 4);
        Assert.assertTrue(TestUtil.containsAllKeys(map, "1", "2", "3", "4"));
        Assert.assertTrue(TestUtil.containsAllValues(map, "bir", "iki", "uc", "dort"));
    }
}
