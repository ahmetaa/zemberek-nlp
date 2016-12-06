package zemberek.dependency;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.primitives.Ints;
import zemberek.core.collections.Histogram;
import zemberek.core.io.SimpleTextReader;
import zemberek.core.io.SimpleTextWriter;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ConllTreebankReader {
    public List<DependencySentence> readSentences(File connlFile) throws IOException {

        List<DependencySentence> sentences = new ArrayList<>();
        List<String> lines = new SimpleTextReader(connlFile).asStringList();

        List<DependencyItem> items = new ArrayList<>();
        for (String line : lines) {
            if (line.trim().length() == 0) {
                if (items.size() > 0)
                    sentences.add(new DependencySentence(items));
                items = new ArrayList<>();
            } else {
                items.add(DependencyItem.buildFromConnlLine(line));
            }
        }
        if (items.size() > 0)
            sentences.add(new DependencySentence(items));
        return sentences;
    }

    public void generateCrossValidationSets(
            List<DependencySentence> trainingSentences,
            List<DependencySentence> testSentences,
            File directory,
            String name,
            int split) throws IOException {

        directory.mkdirs();
        int chunkSizeTest = testSentences.size() / split;

        for (int i = 0; i < split; i++) {
            SimpleTextWriter stwTest = SimpleTextWriter.keepOpenUTF8Writer(new File(directory, name + "-test-" + i + ".conll"));
            SimpleTextWriter stwTrain = SimpleTextWriter.keepOpenUTF8Writer(new File(directory, name + "-train-" + i + ".conll"));
            int start = chunkSizeTest * i;
            int end = start + chunkSizeTest;
            if (i == split - 1)
                end = testSentences.size();
            List<DependencySentence> testSet = testSentences.subList(start, end);
            for (DependencySentence dependencySentence : testSet) {
                stwTest.writeLine(dependencySentence.getAsConnlString());
                stwTest.writeLine();
            }
            LinkedHashSet<DependencySentence> trainingSet = new LinkedHashSet<>(trainingSentences);
            for (DependencySentence dependencySentence : testSet) {
                trainingSet.remove(dependencySentence);
            }

            for (DependencySentence dependencySentence : trainingSet) {
                stwTrain.writeLine(dependencySentence.getAsConnlString());
                stwTrain.writeLine();
            }
        }
    }

    public void saveSentences(List<DependencySentence> sentences, File file) throws IOException {
        SimpleTextWriter stw = SimpleTextWriter.keepOpenUTF8Writer(file);
        for (DependencySentence sentence : sentences) {
            stw.writeLine(sentence.getAsConnlString());
            stw.writeLine();
        }
        stw.close();
    }

    public void dumpStats(List<DependencySentence> sentences, File statFile) throws IOException {
        Histogram<CoarsePosTag> coarsePos = new Histogram<>();
        Histogram<PosTag> pos = new Histogram<>();
        Histogram<DependencyRelation> depRelations = new Histogram<>();
        Histogram<String> morphItems = new Histogram<>();

        for (DependencySentence sentence : sentences) {
            for (DependencyItem item : sentence.items) {
                coarsePos.add(item.coarsePosTag);
                pos.add(item.posTag);
                depRelations.add(item.depRelation);
                morphItems.add(Lists.newArrayList(Splitter.on("|").trimResults().omitEmptyStrings().split(item.feats)));
            }
        }
        SimpleTextWriter writer = SimpleTextWriter.keepOpenUTF8Writer(statFile);
        writer.writeLine("Sentence count:" + sentences.size());
        writer.writeLine("\nCoarse POS values:\n");
        for (CoarsePosTag coarsePo : coarsePos.getSortedList()) {
            writer.writeLine(coarsePo.getAsConnlValue() + " : " + coarsePos.getCount(coarsePo));
        }
        writer.writeLine("\nPOS values:\n");
        for (PosTag posTag : pos.getSortedList()) {
            writer.writeLine(posTag.getAsConnlValue() + " : " + pos.getCount(posTag));
        }
        writer.writeLine("\nDEP Rel values:\n");
        for (DependencyRelation depRel : depRelations.getSortedList()) {
            writer.writeLine(depRel.getAsConnlString() + " : " + depRelations.getCount(depRel));
        }
        for (String morphItem : morphItems.getSortedList()) {
            writer.writeLine(morphItem + " : " + morphItems.getCount(morphItem));
        }
        writer.close();
    }

    public void separateShortAndLongSentences(List<DependencySentence> sentences, int shortAmount) throws IOException {
        List<DependencySentence> sentencesToBeSorted = new ArrayList<>(sentences);
        Collections.sort(sentencesToBeSorted,
                (o1, o2) -> Ints.compare(o1.items.size(), o2.items.size()));
        SimpleTextWriter stw = SimpleTextWriter.keepOpenUTF8Writer(new File("shorts.conll"));
        List<DependencySentence> shorts = new ArrayList<>();
        List<DependencySentence> longs = new ArrayList<>();
        for (DependencySentence dependencySentence : sentencesToBeSorted) {
            if (dependencySentence.lemmaCount() > 2 && shorts.size() < shortAmount)
                shorts.add(dependencySentence);
            else longs.add(dependencySentence);
        }
        for (DependencySentence dependencySentence : shorts) {
            stw.writeLine(dependencySentence.getAsConnlString());
            stw.writeLine();
        }
        stw.close();

        stw = SimpleTextWriter.keepOpenUTF8Writer(new File("longs.conll"));
        for (DependencySentence dependencySentence : longs) {
            stw.writeLine(dependencySentence.getAsConnlString());
            stw.writeLine();
        }
        stw.close();
    }

    public static void main(String[] args) throws IOException {
        ConllTreebankReader reader = new ConllTreebankReader();
        //List<DependencySentence> trainingAll = reader.readSentences(new File("data/treebank/tr/treebank-projective.conll"));
        List<DependencySentence> testAll = reader.readSentences(new File("data/treebank/tr/turkish-metu-sabanci-test.conll"));
        List<String> sentences = new ArrayList<>();
        for (DependencySentence depSentence : testAll) {
            String sentence = depSentence.getAsSentence();
            if (sentence.split("[ ]").length > 2) {
                sentences.add(sentence);
            }
        }
        SimpleTextWriter.oneShotUTF8Writer(new File("data/treebank/tr/turkish-metu-sabanci-test.txt")).writeLines(sentences);
        //reader.generateCrossValidationSets(trainingAll, testAll, new File("data/dp-experiments/10fold"), "default", 10);
    }


}
