package zemberek.lm.apps;

import com.google.common.io.Files;
import com.google.common.io.Resources;

import org.junit.Assert;

import org.junit.Test;
import zemberek.lm.compression.SmoothLm;

import java.io.File;
import java.io.IOException;
import java.net.URL;

public class ConvertToSmoothLmTest {

    URL TINY_ARPA_URL = Resources.getResource("tiny.arpa");

    File getTinyArpaFile() throws IOException {
        File tmp = File.createTempFile("tiny", ".arpa");
        Files.copy(new File(TINY_ARPA_URL.getFile()), tmp);
        return tmp;
    }

    @Test
    public void testConversion() throws IOException {
        File arpaFile = getTinyArpaFile();
        File sm = new File(System.currentTimeMillis() + "-test-lm.smooth");
        sm.deleteOnExit();
        new CompressLm().execute(
                "-arpaFile",
                arpaFile.getAbsolutePath(),
                "-smoothFile",
                sm.getAbsolutePath(),
                "-spaceUsage", "16-16-16");
        Assert.assertTrue(sm.exists());
        SmoothLm lm = SmoothLm.builder(sm).build();
        Assert.assertEquals(3, lm.getOrder());
    }
}
