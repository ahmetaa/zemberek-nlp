package zemberek.core.text;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import zemberek.core.io.TestUtil;

public class BlockTextLoaderTest {

  @Test
  public void loadTest1() throws IOException {
    List<String> lines = new ArrayList<>();

    for (int i = 0; i < 10000; i++) {
      lines.add(String.valueOf(i));
    }
    Path path = TestUtil.tempFileWithData(lines);

    BlockTextLoader loader = BlockTextLoader.fromPath(path, 1000);
    int i = 0;
    List<String> read = new ArrayList<>();
    for (TextChunk block : loader) {
      i++;
      read.addAll(block.getData());
    }

    Assert.assertEquals(i, 10);
    Assert.assertEquals(lines, read);

    loader = BlockTextLoader.fromPath(path, 1001);

    i = 0;
    read = new ArrayList<>();
    for (TextChunk block : loader) {
      i++;
      read.addAll(block.getData());
    }

    Assert.assertEquals(i, 10);
    Assert.assertEquals(lines, read);

    loader = BlockTextLoader.fromPath(path, 100000);

    i = 0;
    read = new ArrayList<>();
    for (TextChunk block : loader) {
      i++;
      read.addAll(block.getData());
    }

    Assert.assertEquals(i, 1);
    Assert.assertEquals(lines, read);

    Files.delete(path);

  }
}
