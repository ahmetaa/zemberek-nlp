package zemberek.normalization;

import com.google.common.io.Resources;
import zemberek.core.logging.Log;
import zemberek.core.turkish.TurkishAlphabet;
import zemberek.morphology.analysis.WordAnalysis;
import zemberek.morphology.analysis.WordAnalysisFormatter;
import zemberek.morphology.analysis.tr.TurkishMorphology;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

public class TurkishSpellChecker {

    TurkishMorphology turkishMorphology;
    WordAnalysisFormatter formatter = new WordAnalysisFormatter();
    CharacterGraphDecoder distanceMatcher = new CharacterGraphDecoder(1);

    public TurkishSpellChecker(TurkishMorphology turkishMorphology) throws IOException {
        this.turkishMorphology = turkishMorphology;
        List<String> vocab = Files.readAllLines(
                new File(Resources.getResource("zemberek-parsed-words-min10.txt").getFile()).toPath());
        Log.info("Spell checker vocab size = %d", vocab.size());
        distanceMatcher.buildDictionary(vocab);
    }

    public boolean check(String input) {
        List<WordAnalysis> analyses = turkishMorphology.analyze(input);
        WordAnalysisFormatter.CaseType caseType = formatter.guessCase(input);
        for (WordAnalysis analysis : analyses) {
            if (analysis.isUnknown()) {
                continue;
            }
            if (formatter.canBeFormatted(analysis, caseType)) {
                String formatted = formatter.formatToCase(analysis, caseType);
                if (input.equals(formatted)) {
                    return true;
                }
            }
        }
        return false;
    }

    public List<String> suggestForWord(String word) {
        WordAnalysisFormatter.CaseType caseType = formatter.guessCase(word);
        if (caseType == WordAnalysisFormatter.CaseType.MIXED_CASE ||
                caseType == WordAnalysisFormatter.CaseType.LOWER_CASE) {
            caseType = WordAnalysisFormatter.CaseType.DEFAULT_CASE;
        }
        String normalized = TurkishAlphabet.INSTANCE.normalize(word);
        List<String> strings = distanceMatcher.getSuggestionsSorted(normalized);
        Set<String> results = new LinkedHashSet<>(strings.size());
        for (String string : strings) {
            List<WordAnalysis> analyses = turkishMorphology.analyze(string);
            for (WordAnalysis analysis : analyses) {
                if (analysis.isUnknown()) {
                    continue;
                }
                String formatted = formatter.formatToCase(analysis, caseType);
                results.add(formatted);
            }
        }
        return new ArrayList<>(results);
    }
}
