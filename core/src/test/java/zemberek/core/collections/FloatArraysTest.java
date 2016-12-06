package zemberek.core.collections;

import org.junit.Assert;
import org.junit.Test;

public class FloatArraysTest {

    @Test
    public void multiplyToFirstTest() {
        float[] expected = {0, 2, 6, 12, 20};
        float[] first = {0, 1, 2, 3, 4};
        FloatArrays.multiplyToFirst(
                first,
                new float[]{0, 2, 3, 4, 5});
        Assert.assertArrayEquals(expected, first, 0.0001f);
    }

}
