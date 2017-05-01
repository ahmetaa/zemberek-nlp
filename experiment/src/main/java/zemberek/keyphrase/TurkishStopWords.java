package zemberek.keyphrase;

import com.google.common.collect.Sets;
import com.google.common.io.Resources;
import zemberek.core.turkish.PrimaryPos;
import zemberek.morphology.analysis.tr.TurkishMorphology;
import zemberek.morphology.lexicon.DictionaryItem;
import zemberek.morphology.lexicon.RootLexicon;
import zemberek.morphology.structure.Turkish;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class TurkishStopWords {

    private Set<String> stopWords = new LinkedHashSet<>();

    public static TurkishStopWords DEFAULT = Languages.TR.instance;

    TurkishStopWords(Set<String> stopWords) {
        this.stopWords = stopWords;
    }

    private static TurkishStopWords turkish() throws IOException {
        Path path = new File(Resources.getResource("stop-words.tr.txt").getFile()).toPath();
        return new TurkishStopWords(new LinkedHashSet<>((Files.readAllLines(path, StandardCharsets.UTF_8))));
    }

    public void save(Path path) throws IOException {
        List<String> str = new ArrayList<>(stopWords);
        str.sort(Turkish.STRING_COMPARATOR_ASC);
        Files.write(path, str, StandardCharsets.UTF_8);
    }

    enum Languages {
       TR;

       TurkishStopWords instance;

        Languages() {
            try {
                instance = turkish();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean contains(String s) {
        return stopWords.contains(s);
    }

    static TurkishStopWords loadFromPath(Path wordList) throws IOException {
        return new TurkishStopWords(new LinkedHashSet<>(Files.readAllLines(wordList, StandardCharsets.UTF_8)));
    }

    static TurkishStopWords generateFromDictionary() throws IOException {
        Set<PrimaryPos> pos = Sets.newHashSet(
                PrimaryPos.Adverb,
                PrimaryPos.Conjunction,
                PrimaryPos.Determiner,
                PrimaryPos.Interjection,
                PrimaryPos.PostPositive,
                PrimaryPos.Numeral,
                PrimaryPos.Pronoun,
                PrimaryPos.Question
        );

        TurkishMorphology morphology = TurkishMorphology.createWithDefaults();
        Set<String> set = new HashSet<>();
        RootLexicon lexicon = morphology.getLexicon();
        for (DictionaryItem item : lexicon) {
            if (pos.contains(item.primaryPos)) {
                set.add(item.lemma);
            }
        }
        List<String> str = new ArrayList<>(set);
        str.sort(Turkish.STRING_COMPARATOR_ASC);
        return new TurkishStopWords(new LinkedHashSet<>(str));
    }
}
