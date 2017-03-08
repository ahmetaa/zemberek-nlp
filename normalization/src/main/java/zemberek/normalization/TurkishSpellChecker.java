package zemberek.normalization;

import zemberek.morphology.analysis.WordAnalysis;
import zemberek.morphology.analysis.WordAnalysisFormatter;
import zemberek.morphology.analysis.tr.TurkishMorphology;

import java.util.Collections;
import java.util.List;

public class TurkishSpellChecker {

    TurkishMorphology turkishMorphology;
    WordAnalysisFormatter formatter = new WordAnalysisFormatter();

    public TurkishSpellChecker(TurkishMorphology turkishMorphology) {
        this.turkishMorphology = turkishMorphology;
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

    //TODO: not yet finished.
    public List<String> suggestForWord(String word) {
        return Collections.emptyList();
    }
}
