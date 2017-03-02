package zemberek.core.collections;

import org.junit.Assert;
import org.junit.Test;

public class SparseFloatMatrixTest {

    private static final double DELTA = 0.0001;

    @Test
    public void getSetTest() {
        SparseFloatMatrix m = new SparseFloatMatrix(1000, 100);
        Assert.assertEquals(1000, m.m);
        Assert.assertEquals(100, m.n);
        for (int i = 0; i < m.m; i++) {
            for (int j = 0; j < m.n; j++) {
                Assert.assertEquals(0, m.get(i, j), DELTA);
            }
        }

        for (int i = 0; i < m.m; i++) {
            for (int j = 0; j < m.n; j++) {
                m.set(i, j, i * j / 1000f);
            }
        }

        for (int i = 0; i < m.m; i++) {
            for (int j = 0; j < m.n; j++) {
                Assert.assertEquals(i * j / 1000f, m.get(i, j), DELTA);
            }
        }
    }
}
