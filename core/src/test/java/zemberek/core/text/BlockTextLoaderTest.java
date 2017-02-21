package zemberek.core.text;

import org.junit.Assert;
import org.junit.Test;
import zemberek.core.io.TestUtil;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class BlockTextLoaderTest {


    @Test
    public void loadTest1() throws IOException {
        List<String> lines = new ArrayList<>();

        for (int i = 0; i < 10000; i++) {
            lines.add(String.valueOf(i));
        }
        Path path = TestUtil.tempFileWithData(lines);

        BlockTextLoader loader = new BlockTextLoader(path, 1000);
        int i = 0;
        List<String> read = new ArrayList<>();
        for (List<String> block : loader) {
            i++;
            read.addAll(block);
        }

        Assert.assertEquals(i, 10);
        Assert.assertEquals(lines, read);

        loader = new BlockTextLoader(path, 1001);

        i = 0;
        read = new ArrayList<>();
        for (List<String> block : loader) {
            i++;
            read.addAll(block);
        }

        Assert.assertEquals(i, 10);
        Assert.assertEquals(lines, read);


        loader = new BlockTextLoader(path, 100000);

        i = 0;
        read = new ArrayList<>();
        for (List<String> block : loader) {
            i++;
            read.addAll(block);
        }

        Assert.assertEquals(i, 1);
        Assert.assertEquals(lines, read);

    }
}
