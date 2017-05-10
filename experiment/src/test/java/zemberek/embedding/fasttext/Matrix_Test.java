package zemberek.embedding.fasttext;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import zemberek.core.io.IOUtil;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

public class Matrix_Test {

    @Test
    public void saveLoadTest() throws IOException {
        saveLoad(1, 1);
        saveLoad(10, 10);
        saveLoad(10, 1);
        saveLoad(100000, 10);
        saveLoad(100001, 1);
        saveLoad(100010, 1);
    }

    @Test
    @Ignore("Run with Xms12G or more.")
    public void saveLoadLargeTest() throws IOException {
        saveLoad(11_000_000, 20);
    }


    private void saveLoad(int m, int n) throws IOException {
        File tempFile = File.createTempFile("foo", "bar");
        tempFile.deleteOnExit();
        Path p = tempFile.toPath();
        DataOutputStream dos = IOUtil.getDataOutputStream(p);

        Matrix_ ma = new Matrix_(m, n);
        int k = 0;
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                ma.data_[i * ma.n_ + j] = k * 0.01f;
                k++;
            }
        }
        ma.save(dos);
        dos.close();
        Assert.assertEquals(m * n * 4 + 8, tempFile.length());
        DataInputStream dis = IOUtil.getDataInputStream(p);
        ma = Matrix_.load(dis);
        k = 0;
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                Assert.assertEquals(k * 0.01f, ma.at(i,j), 0.1);
                k++;
            }
        }

        dis.close();
    }
}
