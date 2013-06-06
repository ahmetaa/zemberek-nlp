package zemberek.langid.train;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import zemberek.core.io.Files;
import zemberek.core.io.IOs;
import zemberek.core.io.SimpleTextWriter;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.zip.GZIPInputStream;

public class SubtitleExtractor {

    public List<String> ExtractFromStream(InputStream is, int minLength) throws XMLStreamException, IOException {
        List<String> lines = Lists.newArrayList();
        XMLStreamReader staxXmlReader = XMLInputFactory.newInstance().createXMLStreamReader(is);
        List<String> line = Lists.newArrayList();
        for (int event = staxXmlReader.next(); event != XMLStreamConstants.END_DOCUMENT; event = staxXmlReader.next()) {
            switch (event) {
                case XMLStreamConstants.START_ELEMENT:
                    String elName = staxXmlReader.getLocalName();
                    if (elName.equals("s")) {
                        String sentence = Joiner.on(" ").join(line)
                                .replaceAll("[\"\\(\\)+-]", "")
                                .replaceAll("([*]|[#]|[{]).+?([*]|[#]|[}])", " ")
                                .replaceAll("'[ ]+", "'")
                                .replaceAll("[.]+", ".")
                                .replaceAll("( )([,.?!])", "$2").trim();
                        if (sentence.length() >= minLength)
                            lines.add(sentence);
                        line = Lists.newArrayList();
                    }
                    if (elName.equals("w")) {
                        line.add(staxXmlReader.getElementText().trim());
                    }
                    break;
                case XMLStreamConstants.END_ELEMENT:
                    break;
                default:
                    break;
            }
        }
        return lines;
    }

    public void generateSets(File root, File outDir) throws IOException, XMLStreamException {
        List<File> dirs = Files.getDirectories(root, false);
        for (File dir : dirs) {
            extractDir(dir, outDir);
        }
    }

    private void extractDir(File dir, File outDir) throws XMLStreamException, IOException {
        String name = dir.getName().toLowerCase();
        LinkedHashSet<String> all = Sets.newLinkedHashSet();
        List<File> gzFiles = Files.crawlDirectory(dir, true, Files.extensionFilter(".gz"));
        for (File gzFile : gzFiles) {
            GZIPInputStream gis = null;
            try {
                gis = new GZIPInputStream(new FileInputStream(gzFile));
                List<String> lines = ExtractFromStream(gis, 7);
                all.addAll(lines);
            } finally {
                IOs.closeSilently(gis);
            }
        }
        List<String> allAsList = Lists.newArrayList(all);
        Collections.shuffle(allAsList);
        List<String> train = Lists.newArrayList(allAsList.subList(1000, all.size() - 1));
        List<String> test = Lists.newArrayList(allAsList.subList(0, 1000));

        System.out.println("Lang:" + name + " Train:" + train.size() + " Test:" + test.size());

        SimpleTextWriter.oneShotUTF8Writer(new File(outDir, name + "-train")).writeLines(train);
        SimpleTextWriter.oneShotUTF8Writer(new File(outDir, name + "-test")).writeLines(test);
    }

    public static void main(String[] args) throws XMLStreamException, IOException {
        new SubtitleExtractor().generateSets(
                new File("/home/kodlab/Downloads/OpenSubtitles/OpenSubtitles"),
                new File("/home/kodlab/data/language-data/subtitle"));
    }

}
