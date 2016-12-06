package zemberek.core.collections;

import org.junit.Assert;
import org.junit.Test;

public class DynamicIntArrayTest {

    @Test
    public void testConstructor() {
        DynamicIntArray darray = new DynamicIntArray();
        Assert.assertEquals(0,darray.size());
        Assert.assertEquals(7,darray.capacity());
    }

    @Test
    public void testConstructor2() {
        DynamicIntArray darray = new DynamicIntArray(1);
        Assert.assertEquals(0,darray.size());
        Assert.assertEquals(1,darray.capacity());
    }

    @Test
    public void testAdd() {
        DynamicIntArray darray = new DynamicIntArray();
        for (int i = 0; i < 10000; i++) {
             darray.add(i);
        }
        Assert.assertEquals(10000,darray.size());
        for (int i = 0; i < 10000; i++) {
            Assert.assertEquals(i,darray.get(i));
        }
    }

    @Test
    public void testTrimToSize() {
        DynamicIntArray darray = new DynamicIntArray();
        for (int i = 0; i < 10000; i++) {
            darray.add(i);
        }
        Assert.assertEquals(10000,darray.size());
        Assert.assertNotEquals(darray.size(), darray.capacity());
        darray.trimToSize();
        Assert.assertEquals(10000,darray.size());
        Assert.assertEquals(10000,darray.copyOf().length);
        Assert.assertEquals(darray.size(), darray.capacity());
    }

}
