package zemberek.langid.train;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;
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
        List<Path> dirs = Files.walk(root.toPath()).filter(s -> s.toFile().isDirectory()).collect(Collectors.toList());
        for (Path dir : dirs) {
            extractDir(dir.toFile(), outDir);
        }
    }

    private void extractDir(File dir, File outDir) throws XMLStreamException, IOException {
        String name = dir.getName().toLowerCase();
        LinkedHashSet<String> all = Sets.newLinkedHashSet();
        List<Path> gzFiles = Files.walk(dir.toPath())
                .filter(s -> s.toFile().getName().endsWith(".gz"))
                .collect(Collectors.toList());
        for (Path gzFile : gzFiles) {
            try (GZIPInputStream gis = new GZIPInputStream(new FileInputStream(gzFile.toFile()))) {
                List<String> lines = ExtractFromStream(gis, 7);
                all.addAll(lines);
            }
        }
        List<String> allAsList = Lists.newArrayList(all);
        Collections.shuffle(allAsList);
        List<String> train = Lists.newArrayList(allAsList.subList(1000, all.size() - 1));
        List<String> test = Lists.newArrayList(allAsList.subList(0, 1000));

        System.out.println("Lang:" + name + " Train:" + train.size() + " Test:" + test.size());

        Files.write(outDir.toPath().resolve(name + "-train"), train);
        Files.write(outDir.toPath().resolve(name + "-test"), test);
    }

    public static void main(String[] args) throws XMLStreamException, IOException {
        new SubtitleExtractor().generateSets(
                new File("/home/kodlab/Downloads/OpenSubtitles/OpenSubtitles"),
                new File("/home/kodlab/data/language-data/subtitle"));
    }

}
