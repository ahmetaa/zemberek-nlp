package zemberek.core.io;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SimpleTextWriterTest {

    File tmpDir;
    File tmpFile;

    @Before
    public void before() {
        tmpDir = com.google.common.io.Files.createTempDir();
        tmpDir.deleteOnExit();
        tmpFile = new File(tmpDir, "jcaki.txt");
        tmpFile.deleteOnExit();
    }

    @After
    public void after() {
        tmpFile.delete();
    }

    @Test
    public void WriteStringTest() throws IOException {
        new SimpleTextWriter(tmpFile).write("Hello World!");
        Assert.assertEquals(new SimpleTextReader(tmpFile).asString(), "Hello World!");
        new SimpleTextWriter(tmpFile).write(null);
        Assert.assertEquals(new SimpleTextReader(tmpFile).asString(), "");
        new SimpleTextWriter(tmpFile).write("");
        Assert.assertEquals(new SimpleTextReader(tmpFile).asString(), "");
    }

    @Test
    public void WriteStringKeepOpenTest() throws IOException {
        try (SimpleTextWriter sfw = new SimpleTextWriter
                .Builder(tmpFile)
                .keepOpen()
                .build()) {
            sfw.write("Hello");
            sfw.write("Merhaba");
            sfw.write("");
            sfw.write(null);
        }
        Assert.assertEquals("HelloMerhaba", new SimpleTextReader(tmpFile).asString());

    }

    @Test(expected = IOException.class)
    public void keepOpenExcepionTest() throws IOException {
        SimpleTextWriter sfw = new SimpleTextWriter
                .Builder(tmpFile)
                .build();
        sfw.write("Hello");
        sfw.write("Now it will throw an exception..");
    }

    @Test
    public void WriteMultiLineStringTest() throws IOException {
        List<String> strs = new ArrayList<String>(Arrays.asList("Merhaba", "Dunya", ""));
        new SimpleTextWriter(tmpFile).writeLines(strs);
        List<String> read = new SimpleTextReader(tmpFile).asStringList();
        for (int i = 0; i < read.size(); i++) {
            Assert.assertEquals(read.get(i), strs.get(i));
        }
    }
}
