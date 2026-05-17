package zemberek.examples.normalization;

import zemberek.morphology.TurkishMorphology;
import zemberek.normalization.WordSegmenter;

import java.io.IOException;
import java.util.List;

public class WordSegment {

    public static void main(String[] args) throws IOException {
        TurkishMorphology morphology = TurkishMorphology.createWithDefaults();

        WordSegmenter segmenter = new WordSegmenter(morphology);
        String[] examples = {"istanbulyağmurluolacak", "benimlegelirmisin"};
        for (String example: examples) {
            System.out.println("Example is : " + example);
            List<String> wordBreak = segmenter.wordBreak(example,4);
            wordBreak.forEach(System.out::println);
            System.out.println("---------------------------");
        }

    }
}
