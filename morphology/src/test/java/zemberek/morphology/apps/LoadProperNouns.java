package zemberek.morphology.apps;

import zemberek.core.collections.Histogram;
import zemberek.core.io.Strings;
import zemberek.core.turkish.RootAttribute;
import zemberek.core.turkish.SecondaryPos;
import zemberek.morphology.analysis.WordAnalysis;
import zemberek.morphology.analysis.tr.TurkishMorphology;
import zemberek.morphology.structure.Turkish;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class LoadProperNouns {

    public static void main(String[] args) throws IOException {

        TurkishMorphology parserGenerator = TurkishMorphology.createWithDefaults();

        List<String> lines = Files.readAllLines(
                Paths.get("/home/afsina/Downloads/documents-export-2016-02-17/vocabulary-proper-full.tr.txt"));

        Histogram<String> histogram = new Histogram<>();

        Set<String> ignore = new HashSet<>(Files.readAllLines(Paths.get("morphology/src/main/resources/tr/proper-ignore")));

        for (String line : lines) {
            if (line.startsWith("_"))
                continue;
            line = line.trim();
            if (line.length() == 0) {
                continue;
            }
            String word = Strings.subStringUntilFirst(line, " ");
            int count = Integer.parseInt(Strings.subStringAfterFirst(line, " "));
            word = Turkish.capitalize(word.substring(1));

            if(count<50)
                continue;

            if (ignore.contains(word))
                continue;

            List<WordAnalysis> parses = parserGenerator.analyze(word);
            boolean found = false;
            for (WordAnalysis parse : parses) {
                if (parse.dictionaryItem.secondaryPos.equals(SecondaryPos.ProperNoun) &&
                        !parse.dictionaryItem.hasAttribute(RootAttribute.Runtime)) {
                    found = true;
                }
            }
            parserGenerator.invalidateCacheForWord(word);

            if (found)
                continue;

            if (word.length() < 4)
                continue;


            histogram.add(word, count);
        }

        histogram.removeSmaller(165);
        try (PrintWriter pw = new PrintWriter("proper")) {
            histogram.getSortedList(Turkish.STRING_COMPARATOR_ASC).forEach(pw::println);
        }

    }
}
