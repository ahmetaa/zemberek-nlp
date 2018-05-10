package zemberek.core.hash;

import com.google.common.base.Stopwatch;
import com.google.common.io.Files;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class LargeNgramMphfTest {

  @Test
  @Ignore("Contains File I/O, test manually.")
  public void NgramFileMPHFTest() throws IOException {
    int[] gramCounts = {1, 2, 4, 8, 10, 100, 1000, 10000, 100000, 1000000};
    for (int gramCount : gramCounts) {
      System.out.println("Gram Count = " + gramCount);
      int order = 5;
      final int[][] arr = new int[gramCount][order];
      for (int i = 0; i < gramCount; i++) {
        for (int j = 0; j < order; j++) {
          arr[i][j] = i;
        }
      }
      File file = generateBinaryGramFile(order, gramCount, arr);

      Stopwatch sw = Stopwatch.createStarted();
      LargeNgramMphf mphf = LargeNgramMphf.generate(file, 20);
      System.out.println("Generation time:" + sw.elapsed(TimeUnit.MILLISECONDS));
      sw.reset().start();
      for (int[] key : arr) {
        mphf.get(key);
      }
      System.out.println(sw.elapsed(TimeUnit.MILLISECONDS));

      System.out.println("Verifying Results:");
      Set<Integer> results = new HashSet<>(gramCount);
      for (int[] key : arr) {
        int res = mphf.get(key);
        Assert.assertTrue("unexpected result:" + res, res >= 0 && res < gramCount);
        results.add(res);
      }
      Assert.assertEquals(gramCount, results.size());
      System.out.println("------------------------------------------");
    }
  }

  private File generateBinaryGramFile(int order, int gramCount, int[][] keys) throws IOException {
    File tempDir = Files.createTempDir();
    File file = new File(tempDir, "grams");
    System.out.println("writing");
    DataOutputStream dos = new DataOutputStream(
        new BufferedOutputStream(new FileOutputStream(file), 1000000));
    dos.writeInt(order);
    dos.writeInt(gramCount);
    for (int j = 0; j < keys.length; j++) {
      for (int i = 0; i < order; i++) {
        dos.writeInt(j);
      }
    }
    dos.close();
    return file;
  }
}
