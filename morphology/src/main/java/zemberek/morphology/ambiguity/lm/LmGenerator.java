package zemberek.morphology.ambiguity.lm;

import edu.berkeley.nlp.lm.ConfigOptions;
import edu.berkeley.nlp.lm.StringWordIndexer;
import edu.berkeley.nlp.lm.io.ArpaLmReader;
import edu.berkeley.nlp.lm.io.LmReaders;

import java.io.File;
import java.util.Arrays;

public class LmGenerator {

    int order;

    public LmGenerator(int order) {
        this.order = order;
    }

    public void generateArpaLm(File corpus, File arpaFile) {
        final StringWordIndexer wordIndexer = new StringWordIndexer();
        wordIndexer.setStartSymbol(ArpaLmReader.START_SYMBOL);
        wordIndexer.setEndSymbol(ArpaLmReader.END_SYMBOL);
        wordIndexer.setUnkSymbol(ArpaLmReader.UNK_SYMBOL);
        LmReaders.createKneserNeyLmFromTextFiles(
                Arrays.asList(corpus.getAbsolutePath()), wordIndexer, order, arpaFile, new ConfigOptions());
    }
}
