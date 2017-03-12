package zemberek.normalization;

import zemberek.core.ScoredItem;
import zemberek.core.turkish.TurkishAlphabet;
import zemberek.lm.DummyLanguageModel;
import zemberek.lm.NgramLanguageModel;
import zemberek.morphology.analysis.WordAnalysis;
import zemberek.morphology.analysis.WordAnalysisFormatter;
import zemberek.morphology.analysis.tr.TurkishMorphology;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class TurkishSpellChecker {

    TurkishMorphology morphology;
    WordAnalysisFormatter formatter = new WordAnalysisFormatter();
    CharacterGraphDecoder decoder;

    public TurkishSpellChecker(TurkishMorphology morphology) throws IOException {
        this.morphology = morphology;
        StemEndingGraph graph = new StemEndingGraph(morphology);
        decoder = new CharacterGraphDecoder(graph.stemGraph);
    }

    public boolean check(String input) {
        List<WordAnalysis> analyses = morphology.analyze(input);
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

    public List<String> suggestForWord(String word, NgramLanguageModel languageModel) {
        String normalized = TurkishAlphabet.INSTANCE.normalize(word);
        List<String> strings = decoder.getSuggestionsSorted(normalized);

        strings = rankWithUnigramProbability(strings, languageModel);

        WordAnalysisFormatter.CaseType caseType = formatter.guessCase(word);
        if (caseType == WordAnalysisFormatter.CaseType.MIXED_CASE ||
                caseType == WordAnalysisFormatter.CaseType.LOWER_CASE) {
            caseType = WordAnalysisFormatter.CaseType.DEFAULT_CASE;
        }
        Set<String> results = new LinkedHashSet<>(strings.size());
        for (String string : strings) {
            List<WordAnalysis> analyses = morphology.analyze(string);
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

    private static final NgramLanguageModel DUMMY_LM = new DummyLanguageModel();

    public List<String> suggestForWord(String word) {
        return suggestForWord(word, DUMMY_LM);
    }

    public List<String> rankWithUnigramProbability(List<String> strings, NgramLanguageModel lm) {
        List<ScoredItem<String>> results = new ArrayList<>(strings.size());
        for (String string : strings) {
            int wordIndex = lm.getVocabulary().indexOf(string);
            results.add(new ScoredItem<>(string, lm.getUnigramProbability(wordIndex)));
        }
        results.sort(ScoredItem.STRING_COMP_DESCENDING);
        return results.stream().map(s -> s.item).collect(Collectors.toList());
    }
}
