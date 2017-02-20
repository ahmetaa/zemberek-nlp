package zemberek.core.collections;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

public class DynamicIntArrayTest {

    @Test
    public void testConstructor() {
        DynamicIntArray darray = new DynamicIntArray();
        Assert.assertEquals(0, darray.size());
        Assert.assertEquals(7, darray.capacity());
    }

    @Test
    public void testConstructor2() {
        DynamicIntArray darray = new DynamicIntArray(1);
        Assert.assertEquals(0, darray.size());
        Assert.assertEquals(1, darray.capacity());
    }

    @Test
    public void testAdd() {
        DynamicIntArray darray = new DynamicIntArray();
        for (int i = 0; i < 10000; i++) {
            darray.add(i);
        }
        Assert.assertEquals(10000, darray.size());
        for (int i = 0; i < 10000; i++) {
            Assert.assertEquals(i, darray.get(i));
        }
    }

    @Test
    public void testAddAll() {
        int[] d1 = {2, 4, 5, 17, -1, -2, 5, -123};
        DynamicIntArray darray = new DynamicIntArray();
        darray.addAll(d1);
        Assert.assertEquals(8, darray.size());
        Assert.assertArrayEquals(d1, darray.copyOf());
        darray.addAll(d1);
        Assert.assertEquals(16, darray.size());
        Assert.assertEquals(2, darray.get(0));
        Assert.assertEquals(-123, darray.get(15));
        Assert.assertArrayEquals(d1, Arrays.copyOfRange(darray.copyOf(), 8, 16));
    }

    @Test
    public void testTrimToSize() {
        DynamicIntArray darray = new DynamicIntArray();
        for (int i = 0; i < 10000; i++) {
            darray.add(i);
        }
        Assert.assertEquals(10000, darray.size());
        Assert.assertNotEquals(darray.size(), darray.capacity());
        darray.trimToSize();
        Assert.assertEquals(10000, darray.size());
        Assert.assertEquals(10000, darray.copyOf().length);
        Assert.assertEquals(darray.size(), darray.capacity());
    }

}
