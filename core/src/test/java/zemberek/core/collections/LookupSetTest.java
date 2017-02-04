package zemberek.core.collections;

import org.junit.Assert;
import org.junit.Test;

public class LookupSetTest {

    @Test
    public void addTest() {
        Foo f1 = new Foo("abc", 1);
        Foo f2 = new Foo("abc", 2);

        LookupSet<Foo> fooSet = new LookupSet<>();
        Assert.assertTrue(fooSet.add(f1));
        Assert.assertFalse(fooSet.add(f2));
    }

    @Test
    public void lookupTest() {
        Foo f1 = new Foo("abc", 1);
        Foo f2 = new Foo("abc", 2);

        LookupSet<Foo> fooSet = new LookupSet<>();
        Assert.assertNull(fooSet.lookup(f1));
        Assert.assertNull(fooSet.lookup(f2));
        fooSet.add(f1);
        Assert.assertEquals(1, fooSet.lookup(f1).b);
        Assert.assertEquals(1, fooSet.lookup(f2).b);
        fooSet.add(f2);
        Assert.assertEquals(1, fooSet.lookup(f1).b);
        Assert.assertEquals(1, fooSet.lookup(f2).b);
    }

    @Test
    public void getOrAddTest() {
        Foo f1 = new Foo("abc", 1);
        Foo f2 = new Foo("abc",2);

        LookupSet<Foo> fooSet = new LookupSet<>();
        Assert.assertEquals(1, fooSet.getOrAdd(f1).b);
        Assert.assertEquals(1, fooSet.getOrAdd(f2).b);
    }

    @Test
    public void removeTest() {
        Foo f1 = new Foo("abc", 1);
        Foo f2 = new Foo("abc", 2);

        LookupSet<Foo> fooSet = new LookupSet<>();
        Assert.assertEquals(1, fooSet.getOrAdd(f1).b);
        Assert.assertEquals(1, fooSet.getOrAdd(f2).b);
        Assert.assertEquals(1, fooSet.remove(f2).b);
        Assert.assertEquals(2, fooSet.getOrAdd(f2).b);
    }

    static class Foo {
        String a;
        int b;

        Foo(String a, int b) {
            this.a = a;
            this.b = b;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Foo foo = (Foo) o;
            return a.equals(foo.a);
        }

        @Override
        public int hashCode() {
            return a.hashCode();
        }
    }
}
