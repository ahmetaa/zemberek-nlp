package zemberek.morphology.analysis;

import zemberek.core.turkish.SecondaryPos;
import zemberek.morphology.lexicon.DictionaryItem;
import zemberek.morphology.structure.Turkish;

public class WordAnalysisFormatter {

    String format(WordAnalysis analysis) {
        DictionaryItem item = analysis.dictionaryItem;
        if (item.secondaryPos == SecondaryPos.ProperNoun) {
            String ending = analysis.getEnding();
            return ending.length() > 0 ? item.lemma + "'" + analysis.getEnding() : item.lemma;
        } else {
            return analysis.getSurfaceForm();
        }
    }

    String formatToCase(WordAnalysis analysis, CaseType type) {
        String formatted = format(analysis);
        switch (type) {
            case DEFAULT:
                return formatted;
            case LOWER_CASE:
                return formatted.toLowerCase(Turkish.LOCALE);
            case UPPER_CASE:
                return formatted.toUpperCase(Turkish.LOCALE);
            case CAPITAL_CASE:
                return Turkish.capitalize(formatted);
            case UPPERCASE_ROOT_LOWER_CASE_ENDING:
                String ending = analysis.getEnding();
                String lemmaUpper = analysis.getDictionaryItem().lemma.toUpperCase(Turkish.LOCALE);
                if (ending.length() == 0) {
                    return lemmaUpper;
                }
                if (analysis.getDictionaryItem().secondaryPos == SecondaryPos.ProperNoun) {
                    return lemmaUpper + "'" + ending;
                } else {
                    return lemmaUpper + ending;
                }

            default:
                throw new IllegalStateException();
        }
    }

    enum CaseType {
        DEFAULT,
        LOWER_CASE,
        UPPER_CASE,
        CAPITAL_CASE,
        UPPERCASE_ROOT_LOWER_CASE_ENDING,
    }

}
