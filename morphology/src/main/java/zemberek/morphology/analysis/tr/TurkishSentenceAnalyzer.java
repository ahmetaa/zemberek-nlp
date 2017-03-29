package zemberek.morphology.analysis.tr;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import zemberek.core.io.Files;
import zemberek.core.text.TextUtil;
import zemberek.morphology.ambiguity.TurkishMorphDisambiguator;
import zemberek.morphology.ambiguity.Z3MarkovModelDisambiguator;
import zemberek.morphology.analysis.WordAnalysis;
import zemberek.morphology.analysis.SentenceAnalysis;
import zemberek.tokenization.TurkishTokenizer;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class TurkishSentenceAnalyzer extends BaseParser {

    private TurkishMorphology turkishMorphology;
    private TurkishMorphDisambiguator disambiguator;
    private TurkishTokenizer lexer = TurkishTokenizer.DEFAULT;

    /**
     * Generates a TurkishSentenceAnalyzer from a resource directory.
     * Resource directory needs to have dictionary and disambiguator model files.
     *
     * @param dataDir directory with dictionary and model files.
     * @throws java.io.IOException
     */
    public TurkishSentenceAnalyzer(File dataDir) throws IOException {
        if (!dataDir.isDirectory())
            throw new IllegalArgumentException(dataDir + " is not a directory.");
        if (!dataDir.exists()) {
            throw new IllegalArgumentException(dataDir + " Directory does not exist");
        }
        List<File> dicFiles = Files.crawlDirectory(dataDir, false, Files.extensionFilter("dict"));
        System.out.println("Loading Dictionaries:" + dicFiles.toString());
        if (dicFiles.size() == 0)
            throw new IllegalArgumentException("At least one dictionary file is required. (with txt extension)");
        turkishMorphology = TurkishMorphology.builder().addTextDictionaries(dicFiles.toArray(new File[dicFiles.size()])).build();
        System.out.println("Morph Parser Generated.");

        File rootSmoothLm = new File(dataDir, "root-lm.z3.slm");
        File igSmoothLm = new File(dataDir, "ig-lm.z3.slm");
        if (!rootSmoothLm.exists())
            throw new IllegalArgumentException("Cannot find root model in " + dataDir);
        if (!igSmoothLm.exists())
            throw new IllegalArgumentException("Cannot find suffix model in " + dataDir);
        disambiguator = new Z3MarkovModelDisambiguator(rootSmoothLm, igSmoothLm);
        System.out.println("Morph Disambiguator Generated.");
    }

    public TurkishSentenceAnalyzer(
            TurkishMorphology turkishMorphology,
            TurkishMorphDisambiguator disambiguator) {
        this.turkishMorphology = turkishMorphology;
        this.disambiguator = disambiguator;
    }

    public SentenceAnalysis analyze(String sentence) {
        SentenceAnalysis sentenceParse = new SentenceAnalysis();
        String preprocessed = preProcess(sentence);
        for (String s : Splitter.on(" ").omitEmptyStrings().trimResults().split(preprocessed)) {
            List<WordAnalysis> parses = turkishMorphology.analyze(s);
            sentenceParse.addParse(s, parses);
        }
        return sentenceParse;
    }

    public void disambiguate(SentenceAnalysis parseResult) {
        disambiguator.disambiguate(parseResult);
    }

    public String preProcess(String str) {
        String quotesHyphensNormalized = TextUtil.normalizeQuotesHyphens(str);
        return Joiner.on(" ").join(lexer.tokenizeToStrings(quotesHyphensNormalized));
    }

    /**
     * Returns the best parse of a sentence.
     *
     * @param sentence sentence
     * @return best parse.
     */
    public List<WordAnalysis> bestParse(String sentence) {
        SentenceAnalysis parse = analyze(sentence);
        disambiguate(parse);
        List<WordAnalysis> bestParse = Lists.newArrayListWithCapacity(parse.size());
        for (SentenceAnalysis.Entry entry : parse) {
            bestParse.add(entry.parses.get(0));
        }
        return bestParse;
    }

}


