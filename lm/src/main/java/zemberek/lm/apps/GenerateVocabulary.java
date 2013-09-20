package zemberek.lm.apps;

import com.google.common.collect.Lists;
import org.kohsuke.args4j.Option;
import zemberek.core.CommandLineApplication;
import zemberek.core.Histogram;
import zemberek.core.SpaceTabTokenizer;
import zemberek.core.io.SimpleTextWriter;
import zemberek.core.logging.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.text.Collator;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class GenerateVocabulary extends CommandLineApplication {

    @Option(name = "-corpus",
            usage = "Text corpus file path.",
            required = true)
    File corpus;

    @Option(name = "-top",
            usage = "Size of the resulting vocabulary. Most frequent n words will be kept.")
    int top = -1;

    @Option(name = "-outFile",
            usage = "path of the output file. If not given, a file named [vocabulary] will be generated in working path.",
            required = true)
    File outFile = null;

    @Option(name = "-sorted",
            usage = "If used, vocabulary file will be ordered by words. Otherwise vocabulary is ordered by frequency.")
    boolean ordered = false;

    @Option(name = "-sortLocale",
            usage = "Locale of to be used the orderText corpus file path. By default English locale is used. This value is applied only if -sorted flag is used.")
    String sortLocale;

    @Override
    protected String getDescription() {
        return "Generates vocabulary from a given corpus.";
    }

    @Override
    protected void run() throws Exception {
        if (!corpus.exists()) {
            throw new IllegalArgumentException("Can not find the corpus file: " + corpus);
        }
        if (top < -1 || top == 0)
            throw new IllegalArgumentException("Illegal value for n: " + top);

        Collator collator = Collator.getInstance(Locale.ENGLISH);
        if (sortLocale != null) {
            collator = Collator.getInstance(new Locale(sortLocale));
        }
        Log.info("Processing corpus: %s", corpus);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(corpus), "utf-8"))) {
            String line;
            Histogram<String> histogram = new Histogram<>(50000);
            SpaceTabTokenizer tokenizer = new SpaceTabTokenizer();
            int count = 0;
            while ((line = reader.readLine()) != null) {
                List<String> words = Lists.newArrayList(tokenizer.split(line));
                if (words.isEmpty())
                    continue;
                histogram.add(words);
                if (count % 500000 == 0 && count != 0)
                    Log.info("%d lines processed. Vocabulary Size: %d", count, histogram.size());
                count++;
            }
            Log.info("A total of %d lines have been processed. Vocabulary Size: %d", count, histogram.size());

            if (top >= histogram.size())
                top = histogram.size();
            else Log.info("Top %d words will be used.", top);
            List<String> result = histogram.getMostFrequent(top);
            Log.info("Coverage: %.3f", 100d * ((double) histogram.totalCount(result)) / histogram.totalCount());
            if (ordered) {
                Log.info("Sorting file with word order.");
                Collections.sort(result, collator);
            }
            com.google.common.io.Files.createParentDirs(outFile);
            Log.info("Saving to vocabulary file: %s", outFile);
            SimpleTextWriter.oneShotUTF8Writer(outFile).writeLines(result);
            Log.info("Done.");
        }
    }

    public static void main(String[] args) {
        new GenerateVocabulary().execute(args);
    }
}
