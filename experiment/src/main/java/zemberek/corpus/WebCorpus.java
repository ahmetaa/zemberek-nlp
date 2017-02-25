package zemberek.corpus;

import com.google.common.hash.Hashing;
import zemberek.core.text.TextConsumer;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class WebCorpus {
    String source;
    String id;

    List<WebDocument> pages = new ArrayList<>();

    public WebCorpus(String source, String id, List<WebDocument> pages) {
        this.source = source;
        this.id = id;
        this.pages = pages;
    }

    static WebCorpus loadFromText(String source, Path corpusFile) throws IOException {
        List<String> allLines = Files.readAllLines(corpusFile, StandardCharsets.UTF_8);

        List<WebDocument> pages = new ArrayList<>(allLines.size() / 10);

        TextConsumer textConsumer = new TextConsumer(allLines);
        textConsumer.moveUntil(s -> s.startsWith("<doc id="));
        while (!textConsumer.finished()) {
            String meta = textConsumer.current();
            textConsumer.advance();
            List<String> pageData = textConsumer.moveUntil(s -> s.startsWith("<doc id="));
            WebDocument e = WebDocument.fromText(meta, pageData);
            if (e != null) {
                pages.add(e);
            }
        }
        return new WebCorpus(source, corpusFile.toFile().getName(), pages);
    }

    @Override
    public String toString() {
        return source + "-" + id;
    }

    public int totalPageLineCount() {
        int total = 0;
        for (WebDocument page : pages) {
            for (String line : page.lines) {
                if (line.length() == 0)
                    continue;
                total++;
            }
        }
        return total;
    }

    public int uniquePageLineCount() {

        Set<Long> hashes = new HashSet<>(100000);
        for (WebDocument page : pages) {
            for (String line : page.lines) {
                hashes.add(Hashing.murmur3_128().hashUnencodedChars(line).asLong());
            }
        }
        return hashes.size();
    }

    public void save(Path outRoot, boolean onlyContent) throws IOException {

        Path resolve = outRoot.resolve(source);
        Files.createDirectories(resolve);

        try (PrintWriter p = new PrintWriter(resolve.resolve(id).toFile(), "utf-8")) {
            for (WebDocument page : pages) {
                if (!onlyContent) {
                    p.println(page.getDocumentHeader());
                }
                p.println(page.content());
                if (!onlyContent) {
                    p.println("</doc>");
                }
            }
        }
    }
}

