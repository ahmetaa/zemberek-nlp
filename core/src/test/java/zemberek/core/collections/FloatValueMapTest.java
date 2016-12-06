package zemberek.core.collections;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

public class FloatValueMapTest {

    @Test
    public void testValues() {
        FloatValueMap<String> set = new FloatValueMap<>();
        set.set("a", 7);
        set.set("b", 2);
        set.set("c", 3);
        set.set("d", 4);
        set.set("d", 5); // overwrite

        Assert.assertEquals(4, set.size());
        float[] values = set.values();
        Arrays.sort(values);
        Assert.assertTrue(Arrays.equals(new float[]{2f, 3f, 5f, 7f}, values));

    }
}
