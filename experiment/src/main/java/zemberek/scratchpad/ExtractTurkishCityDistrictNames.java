package zemberek.scratchpad;

import com.google.common.base.Splitter;
import com.google.common.collect.Sets;
import zemberek.morphology.analysis.tr.TurkishMorphology;
import zemberek.morphology.lexicon.DictionaryItem;
import zemberek.morphology.structure.Turkish;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Extracts city, district and village names from a csv file. File is downloaded from
 * public postal code file.
 */
public class ExtractTurkishCityDistrictNames {

    public static void extractSingleWords(Path input, Path output) throws IOException {
        LinkedHashSet<String> provinces = new LinkedHashSet<>();
        LinkedHashSet<String> cities = new LinkedHashSet<>();
        LinkedHashSet<String> districts = new LinkedHashSet<>();
        LinkedHashSet<String> villages = new LinkedHashSet<>();

        List<String> lines = Files.readAllLines(input, StandardCharsets.UTF_8);
        int i = 0;
        for (String line : lines) {
            if (i == 0) { // skip first line.
                i++;
                continue;
            }
            List<String> tokens = Splitter.on('\t').trimResults().omitEmptyStrings().splitToList(line);
            if (tokens.size() != 5) {
                System.out.println("Unexpected data in " + line);
                continue;
            }
            provinces.addAll(clean(tokens.get(0)));
            cities.addAll(clean(tokens.get(1)));
            cities.addAll(clean(tokens.get(2)));
            String districtOrVillage = tokens.get(3);
            if (districtOrVillage.endsWith("MAH.")) {
                districts.addAll(clean(districtOrVillage));
            } else {
                villages.addAll(clean(districtOrVillage));
            }
        }

        try (PrintWriter pw = new PrintWriter(output.toFile(), "utf-8")) {
            pw.println("## --------- Provinces --------");
            provinces.forEach(pw::println);
            pw.println("## --------- Cities    --------");
            cities.forEach(pw::println);
            pw.println("## --------- Districts --------");
            districts.forEach(pw::println);
            pw.println("## --------- Villages  --------");
            villages.forEach(pw::println);
        }

        LinkedHashSet<String> all = new LinkedHashSet<>();
        all.addAll(provinces);
        all.addAll(cities);
        all.addAll(districts);
        all.addAll(villages);

        try (PrintWriter pw = new PrintWriter(output.toFile() + ".all", "utf-8")) {
            all.stream().sorted(Turkish.STRING_COMPARATOR_ASC).forEach(pw::println);
        }
    }

    private void save(Collection<String> data, PrintWriter pw) {
        data.stream().sorted(Turkish.STRING_COMPARATOR_ASC).forEach(pw::println);
    }

    static Set<String> avoid = Sets.newHashSet("MAH.", "OSB", "OSB.", "MEZRASI", "KÖYÜ", "MERKEZKÖYLER");


    public static List<String> clean(String in) {
        in = in.replaceAll("[-()]", " ").replaceAll("^\\.|[/\\\\]", "").trim();
        in = in.replaceAll("OSB ", " OSB ");
        List<String> words = Splitter.on(' ').trimResults().omitEmptyStrings().splitToList(in);
        List<String> result = new ArrayList<>();
        for (final String word : words) {
            if (avoid.stream().filter(s -> s.contains(word)).count() > 0) {
                continue;
            }
            if (word.matches("[\\d ]+|^\\d+.*|")) {
                continue;
            }
            if (word.contains(".") || word.contains("_")) {
                continue;
            }

            result.add(Turkish.capitalize(word));
        }
        return result;
    }

    public static void main(String[] args) throws IOException {
        Path extracted = Paths.get("locations-tr.txt");
        extractSingleWords(
                Paths.get("/media/depo/data/aaa/corpora/pk_list_29.04.2016.csv"),
                extracted);
        Path exceptZemberek = Paths.get("locations-tr.dict");
        removeZemberekDictionaryWordsFromList(extracted, exceptZemberek);
    }


    private static void removeZemberekDictionaryWordsFromList(Path input, Path out) throws IOException {
        LinkedHashSet<String> list = new LinkedHashSet<>(Files.readAllLines(input, StandardCharsets.UTF_8));
        System.out.println("Total amount of lines = " + list.size());
        TurkishMorphology morphology = TurkishMorphology.builder().addTextDictionaryResources(
                "tr/master-dictionary.dict",
                "tr/secondary-dictionary.dict",
                "tr/non-tdk.dict",
                "tr/proper.dict",
                "tr/proper-from-corpus.dict",
                "tr/abbreviations.dict"
        ).build();
        List<String> toRemove = new ArrayList<>();
        for (DictionaryItem item : morphology.getLexicon()) {
            if (list.contains(item.lemma)) {
                toRemove.add(item.lemma);
            }
        }
        System.out.println("Total amount to remove = " + toRemove.size());
        list.removeAll(toRemove);
        try (PrintWriter pw = new PrintWriter(out.toFile(), "utf-8")) {
            list.forEach(pw::println);
        }
    }
}
