package zemberek.morphology.analysis;

import zemberek.core.turkish.PrimaryPos;
import zemberek.core.turkish.RootAttribute;
import zemberek.core.turkish.SecondaryPos;
import zemberek.morphology.lexicon.DictionaryItem;
import zemberek.morphology.structure.Turkish;

public class WordAnalysisFormatter {

    /**
     * Formats the morphological analysis result's surface form.
     * Zemberek analyzer uses lowercase letters during operation. This methods creates the properly formatted
     * surface form of an analysis. For example this method returns [Ankara'ya] for the analysis of [ankaraya]
     *
     * @return formatted word analysis.
     */
    public String format(WordAnalysis analysis, String apostrophe) {
        DictionaryItem item = analysis.dictionaryItem;
        if (apostropheRequired(analysis)) {
            String ending = analysis.getEnding();
            return ending.length() > 0 ? item.lemma + apostrophe + analysis.getEnding() : item.lemma;
        } else {
            return analysis.getSurfaceForm();
        }
    }

    private boolean apostropheRequired(WordAnalysis analysis) {
        DictionaryItem item = analysis.dictionaryItem;
        return item.secondaryPos == SecondaryPos.ProperNoun
                || (item.primaryPos == PrimaryPos.Numeral && item.hasAttribute(RootAttribute.Runtime))
                || item.secondaryPos == SecondaryPos.Date;
    }

    /**
     * This method changes the case of the format of the morphological analysis result's surface form.
     * For example, for inputs ["ankaraya" and CaseType.UPPER_CASE] this method returns [ANKARA'YA]
     * Only LOWER_CASE, UPPER_CASE, TITLE_CASE and UPPER_CASE_ROOT_LOWER_CASE_ENDING are supported.
     * For other case options, returns empty string.
     *
     * @param analysis Morphological analysis result.
     * @param type     case type.
     * @return formatted result or empty string.
     */
    public String formatToCase(WordAnalysis analysis, CaseType type, String apostrophe) {
        String formatted = format(analysis, apostrophe);
        switch (type) {
            case DEFAULT_CASE:
                return formatted;
            case LOWER_CASE:
                return formatted.toLowerCase(Turkish.LOCALE);
            case UPPER_CASE:
                return formatted.toUpperCase(Turkish.LOCALE);
            case TITLE_CASE:
                return Turkish.capitalize(formatted);
            case UPPER_CASE_ROOT_LOWER_CASE_ENDING:
                String ending = analysis.getEnding();
                String lemmaUpper = analysis.getDictionaryItem().lemma.toUpperCase(Turkish.LOCALE);
                if (ending.length() == 0) {
                    return lemmaUpper;
                }
                if (apostropheRequired(analysis)) {
                    return lemmaUpper + apostrophe + ending;
                } else {
                    return lemmaUpper + ending;
                }
            default:
                return "";
        }
    }

    //TODO: write tests.
    public boolean canBeFormatted(WordAnalysis analysis, CaseType type) {
        boolean proper = analysis.getDictionaryItem().secondaryPos == SecondaryPos.ProperNoun;
        switch (type) {
            case LOWER_CASE:
                return !proper;
            case UPPER_CASE:
            case TITLE_CASE:
            case DEFAULT_CASE:
                return true;
            case UPPER_CASE_ROOT_LOWER_CASE_ENDING:
                return proper;
            default:
                return false;
        }
    }

    /**
     * Guesses the current case type of the word.
     * for example,
     * "ankaraya"  -> CaseType.LOWER_CASE
     * "Ankara'ya" -> CaseType.TITLE_CASE
     * "ANKARAYA"  -> CaseType.UPPER_CASE
     * "anKAraYA"  -> CaseType.MIXED_CASE
     * "ANKARA'ya" -> CaseType.UPPER_CASE_ROOT_LOWER_CASE_ENDING
     * "12"        -> CaseType.DEFAULT_CASE
     * "12'de"     -> CaseType.LOWER_CASE
     * "A"         -> CaseType.UPPER_CASE
     * "A1"        -> CaseType.UPPER_CASE
     *
     * @param input input word
     * @return guessed CaseType
     */
    public CaseType guessCase(String input) {
        boolean firstLetterUpperCase = false;
        int lowerCaseCount = 0;
        int upperCaseCount = 0;
        int letterCount = 0;

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (!Character.isAlphabetic(c)) {
                continue;
            }
            if (i == 0) {
                firstLetterUpperCase = Character.isUpperCase(c);
                if (firstLetterUpperCase) {
                    upperCaseCount++;
                } else {
                    lowerCaseCount++;
                }
            } else {
                if (Character.isUpperCase(c)) {
                    upperCaseCount++;
                } else if (Character.isLowerCase(c)) {
                    lowerCaseCount++;
                }
            }
            letterCount++;
        }
        if (letterCount == 0) {
            return CaseType.DEFAULT_CASE;
        }
        if (letterCount == lowerCaseCount) {
            return CaseType.LOWER_CASE;
        }
        if (letterCount == upperCaseCount) {
            return CaseType.UPPER_CASE;
        }
        if (firstLetterUpperCase && letterCount == lowerCaseCount + 1) {
            return letterCount == 1 ? CaseType.UPPER_CASE : CaseType.TITLE_CASE;
        }
        int apostropheIndex = input.indexOf('\'');
        if (apostropheIndex > 0 && apostropheIndex < input.length() - 1) {
            if (guessCase(input.substring(0, apostropheIndex)) == CaseType.UPPER_CASE &&
                    guessCase(input.substring(apostropheIndex + 1)) == CaseType.LOWER_CASE) {
                return CaseType.UPPER_CASE_ROOT_LOWER_CASE_ENDING;
            }
        }
        return CaseType.MIXED_CASE;
    }

    public enum CaseType {
        DEFAULT_CASE, // numbers are considered default case.
        LOWER_CASE,
        UPPER_CASE,
        TITLE_CASE,
        UPPER_CASE_ROOT_LOWER_CASE_ENDING,
        MIXED_CASE,
    }

}
