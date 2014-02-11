package zemberek.morphology.apps;

import com.google.common.base.Charsets;
import com.google.common.base.Splitter;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import org.junit.Ignore;
import org.junit.Test;
import zemberek.morphology.parser.MorphParse;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class TurkishMorphParserTest {

    @Test
    public void shouldCreateTurkishMorphParserSuccessfully() throws IOException {
        TurkishMorphParser parser = TurkishMorphParser.builder().addDefaultDictionaries().build();
        List<MorphParse> results = parser.parse("kedicik");
        for (MorphParse result : results) {
            System.out.println(result.formatNoEmpty());
            System.out.println(result.formatLong());
        }
    }

    @Test
    @Ignore("References local missing resources")
    public void shouldParse8MWords() throws Exception {
        final List<File> files = Arrays.asList(
                new File("D:\\devl\\data\\1MSentences\\tbmm_tokenized.txt"),
                new File("D:\\devl\\data\\1MSentences\\ntvmsnbc_tokenized.txt"),
                new File("D:\\devl\\data\\1MSentences\\radikal_tokenized.txt"),
                new File("D:\\devl\\data\\1MSentences\\zaman_tokenized.txt"),
                new File("D:\\devl\\data\\1MSentences\\milliyet-sondakika_tokenized.txt")
        );

        final LinkedList<String> words = new LinkedList<>();
        final HashSet<String> uniqueWords = new HashSet<>();

        for (File tokenizedFile : files) {
            final List<String> lines = Files.readLines(tokenizedFile, Charsets.UTF_8);
            for (String line : lines) {
                final ArrayList<String> strings = Lists.newArrayList(Splitter.on(" ").trimResults().omitEmptyStrings().split(line));
                words.addAll(strings);
                uniqueWords.addAll(strings);
            }
        }

        System.out.println("Number of words : " + words.size());
        System.out.println("Number of unique words : " + uniqueWords.size());
        System.out.println("======================");

        final TurkishMorphParser parser = TurkishMorphParser.builder().addDefaultDictionaries().addDefaultCache().build();

        final Stopwatch stopWatch = new Stopwatch();
        stopWatch.start();
        int i = 0;
        for (String word : words) {
            parser.parse(word);
            if (++i % 500 == 0)
                System.out.println("Finished " + i);
        }
        stopWatch.stop();

        System.out.println("Total time :" + stopWatch.toString());
        System.out.println("Nr of tokens : " + words.size());
        System.out.println("Avg time : " + (stopWatch.elapsed(TimeUnit.MILLISECONDS) * 1.0) / (words.size() * 1.0d) + " ms");
    }
}
