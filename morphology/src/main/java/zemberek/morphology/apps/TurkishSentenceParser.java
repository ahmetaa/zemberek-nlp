package zemberek.morphology.apps;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import zemberek.core.io.Files;
import zemberek.core.turkish.SecondaryPos;
import zemberek.morphology.ambiguity.TurkishMorphDisambiguator;
import zemberek.morphology.ambiguity.Z3MarkovModelDisambiguator;
import zemberek.morphology.lexicon.DictionaryItem;
import zemberek.morphology.parser.MorphParse;
import zemberek.morphology.parser.SentenceMorphParse;
import zemberek.morphology.structure.Turkish;
import zemberek.tokenizer.ZemberekLexer;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class TurkishSentenceParser {

    private TurkishWordParserGenerator morphParser;
    private UnidentifiedTokenParser unidentifiedTokenParser;
    private TurkishMorphDisambiguator disambiguator;
    private ZemberekLexer lexer = new ZemberekLexer();

    /**
     * Generates a TurkishSentenceParser from a resource directory.
     * Resource directory needs to have dictionary and disambiguator model files.
     *
     * @param dataDir directory with dictionary and model files.
     * @throws java.io.IOException
     */
    public TurkishSentenceParser(File dataDir) throws IOException {
        if (!dataDir.isDirectory())
            throw new IllegalArgumentException(dataDir + " is not a directory.");
        if (!dataDir.exists()) {
            throw new IllegalArgumentException(dataDir + " Directory does not exist");
        }
        List<File> dicFiles = Files.crawlDirectory(dataDir, false, Files.extensionFilter("dict"));
        System.out.println("Loading Dictionaries:" + dicFiles.toString());
        if (dicFiles.size() == 0)
            throw new IllegalArgumentException("At least one dictionary file is required. (with txt extension)");
        morphParser = TurkishWordParserGenerator.builder().addTextDictFiles(dicFiles.toArray(new File[dicFiles.size()])).build();
        System.out.println("Morph Parser Generated.");
        this.unidentifiedTokenParser = new UnidentifiedTokenParser(morphParser);
        File rootSmoothLm = new File(dataDir, "root-lm.z3.slm");
        File igSmoothLm = new File(dataDir, "ig-lm.z3.slm");
        if (!rootSmoothLm.exists())
            throw new IllegalArgumentException("Cannot find root model in " + dataDir);
        if (!igSmoothLm.exists())
            throw new IllegalArgumentException("Cannot find suffix model in " + dataDir);
        disambiguator = new Z3MarkovModelDisambiguator(rootSmoothLm, igSmoothLm);
        System.out.println("Morph Disambiguator Generated.");

    }

    public TurkishSentenceParser(
            TurkishWordParserGenerator morphParser,
            TurkishMorphDisambiguator disambiguator) {
        this.morphParser = morphParser;
        this.unidentifiedTokenParser = new UnidentifiedTokenParser(morphParser);
        this.disambiguator = disambiguator;
    }

    public SentenceMorphParse parse(String sentence) {
        SentenceMorphParse sentenceParse = new SentenceMorphParse();
        String preprocessed = preProcess(sentence);
        for (String s : Splitter.on(" ").omitEmptyStrings().trimResults().split(preprocessed)) {
            String normalized = morphParser.normalize(s); // TODO: may cause problem for some foreign words.
            List<MorphParse> res = morphParser.parseCached(normalized);
            if (res.size() == 0 || (Character.isUpperCase(s.charAt(0)) && !hasProperParse(res)))
                res.addAll(unidentifiedTokenParser.parse(s));
            if (res.size() == 0) {
                res.add(new MorphParse(DictionaryItem.UNKNOWN, normalized, Lists.newArrayList(MorphParse.InflectionalGroup.UNKNOWN)));
            }
            sentenceParse.addParse(s, res);
        }
        return sentenceParse;
    }

    private boolean hasProperParse(List<MorphParse> results) {
        for (MorphParse res : results) {
            if (res.dictionaryItem.secondaryPos == SecondaryPos.ProperNoun)
                return true;
        }
        return false;
    }

    public void disambiguate(SentenceMorphParse parseResult) {
        disambiguator.disambiguate(parseResult);
    }

    public String preProcess(String str) {
        String quotesHyphensNormalized = Turkish.normalizeQuotesHyphens(str);
        return Joiner.on(" ").join(lexer.tokenStrings(quotesHyphensNormalized));
    }

    /**
     * Returns the best parse of a sentence.
     *
     * @param sentence sentence
     * @return best parse.
     */
    public List<MorphParse> bestParse(String sentence) {
        SentenceMorphParse parse = parse(sentence);
        disambiguate(parse);
        List<MorphParse> bestParse = Lists.newArrayListWithCapacity(parse.size());
        for (SentenceMorphParse.Entry entry : parse) {
            bestParse.add(entry.parses.get(0));
        }
        return bestParse;
    }

}


