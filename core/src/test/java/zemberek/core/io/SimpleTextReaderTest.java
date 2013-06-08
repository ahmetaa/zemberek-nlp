package zemberek.core.io;

import com.google.common.io.Resources;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;

import static java.lang.System.getProperty;
import static java.lang.System.out;
import static org.junit.Assert.assertEquals;

public class SimpleTextReaderTest {

    private static String curDir = getProperty("user.dir");

    URL utf8_tr_with_bom = Resources.getResource("io/turkish_utf8_with_BOM.txt");
    URL multi_line_text_file = Resources.getResource("io/multi_line_text_file.txt");


    @Test
    public void testUtf8() throws IOException {
        String content = new SimpleTextReader(utf8_tr_with_bom.getFile(), "utf-8").asString();
        assertEquals(content, "\u015fey");
    }

    @Test
    public void multilineTest() throws IOException {
        List<String> list = new SimpleTextReader(multi_line_text_file.getFile()).asStringList();
        assertEquals(list.size(), 17);
        assertEquals(list.get(1), "uno");
        //test trim
        assertEquals(list.get(2), "  dos");
    }

    @Test
    public void multilineConstarintTest() throws IOException {
        List<String> list = new SimpleTextReader.Builder(multi_line_text_file.getFile())
                .allowMatchingRegexp("^[^#]")
                .ignoreWhiteSpaceLines()
                .trim()
                .build()
                .asStringList();
        assertEquals(list.size(), 12);
        assertEquals(list.get(0), "uno");
        assertEquals(list.get(1), "dos");
    }

    public void templateTest() throws IOException {
        SimpleTextReader.Template template = new SimpleTextReader.Template()
                .allowMatchingRegexp("^[^#]")
                .ignoreWhiteSpaceLines()
                .trim();
        List<File> files = Files.crawlDirectory(new File("blah"));
        for (File file : files) {
            SimpleTextReader sr = template.generateReader(file);
            //....
        }

    }

    @Test
    public void asStringTest() throws IOException {
        String a = new SimpleTextReader(multi_line_text_file.getFile()).asString();
        System.out.println(a);
    }

    @Test
    public void iterableTest() throws IOException {
        int i = 0;
        for (String s : new SimpleTextReader(multi_line_text_file.getFile()).getIterableReader()) {
            if (i == 1) assertEquals(s.trim(), "uno");
            if (i == 2) assertEquals(s.trim(), "dos");
            if (i == 3) assertEquals(s.trim(), "tres");
            i++;
        }
        assertEquals(i, 17);
    }

    @Test
    public void lineIteratorTest2() throws IOException {
        try (LineIterator li = new SimpleTextReader(multi_line_text_file.getFile()).getLineIterator()) {
            while (li.hasNext())
                out.println(li.next().toUpperCase());
        }
    }

    @Test
    public void lineIteratorWithConstraint() throws IOException {
        try (LineIterator li = new SimpleTextReader
                .Builder(multi_line_text_file.getFile())
                .ignoreWhiteSpaceLines()
                .trim()
                .build().getLineIterator()) {

            int i = 0;
            while (li.hasNext()) {
                String s = li.next();
                if (i == 0) assertEquals(s, "uno");
                if (i == 1) assertEquals(s, "dos");
                i++;
            }
        }
    }

}