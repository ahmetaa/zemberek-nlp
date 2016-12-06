package zemberek.morphology.ambiguity;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import zemberek.core.io.SimpleTextWriter;
import zemberek.lm.apps.CompressLm;
import zemberek.lm.compression.SmoothLm;
import zemberek.morphology.analysis.WordAnalysis;
import zemberek.morphology.analysis.SentenceAnalysis;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * This implementation is based on
 * "Dilek Z. Hakkani-Tür, Kemal Oflazer and Gökhan Tür
 * Statistical Morphological Disambiguation for Agglutinative Languages
 * Computers and the Humanities 36: 381–410, 2002." paper.
 * This is the exact implementation of the Model-A system described in the paper.
 * Model-A basically uses 3-gram root and multiplication of current IG's (Inflectional Group) with previous two last IG probabilities.
 * Simple Viterbi decoding is utilized for finding the best parse. A predefined penalty is applied to unknown word probabilities.
 * Kneser-Ney Lm generation is done with BerkeleyLm library
 * Language model compression and fast random access is provided via SmoothLm library.
 */
public class Z3ModelA extends Z3AbstractDisambiguator implements TurkishMorphDisambiguator {
    public static final double UNKNOWN_PROB = -10;
    SmoothLm rootLm;
    SmoothLm igLm;

    Ambiguous startWord;
    Ambiguous endWord;

    public Z3ModelA(File rootLm, File igLm) throws IOException {
        this.rootLm = SmoothLm.builder(rootLm).build();
        this.igLm = SmoothLm.builder(igLm).build();
        int beginSymbolId = this.rootLm.getVocabulary().getSentenceStartIndex();
        int endSymbolId = this.rootLm.getVocabulary().getSentenceEndIndex();
        int beginIgId = this.igLm.getVocabulary().indexOf(START_IG);
        int endIgId = this.igLm.getVocabulary().indexOf(END_IG);

        startWord = new Ambiguous(new int[]{beginSymbolId}, new int[][]{{beginIgId}});
        endWord = new Ambiguous(new int[]{endSymbolId}, new int[][]{{endIgId}});
    }

    public static void generateBinaryLm(File arpaFile, File binaryFile) throws IOException {
        new CompressLm().execute(
                "-in",
                arpaFile.getAbsolutePath(),
                "-out",
                binaryFile.getAbsolutePath(),
                "-spaceUsage", "16-8-8");
    }

    public void test(File testFile) throws IOException {
        DataSet testSet = Files.readLines(testFile, Charsets.UTF_8, new DataSetLoader());
        int hit = 0, total = 0;
        Stopwatch sw = Stopwatch.createStarted();
        Random r = new Random(5);
        for (SentenceData sentence : testSet.sentences) {
            for (Z3WordData word : sentence.words) {
                Collections.shuffle(word.allParses, r);
            }
            Ambiguous[] seq = getAmbiguousSequence(sentence);
            int[] bestSeq = bestSequence(seq);
            int j = 0;
            for (int parseIndex : bestSeq) {
                Z3WordData wordData = sentence.words.get(j);
                if (wordData.allParses.get(parseIndex).equals(wordData.correctParse)) {
                    hit++;
                } else {
                    System.out.println("miss:" + wordData.word + " Correct:" + wordData.correctParse + " : " + wordData.allParses.get(parseIndex));
                }
                total++;
                j++;
            }
        }
        System.out.println("Elapsed: " + sw.elapsed(TimeUnit.MILLISECONDS) + "ms.");
        System.out.println("Total: " + total + " hit: " + hit);
        System.out.println(String.format("Accuracy:%.3f%%", (double) hit / total * 100));
        if (sw.elapsed(TimeUnit.MILLISECONDS) > 0) {
            System.out.println("Approximate performance: " + (1000L * total / sw.elapsed(TimeUnit.MILLISECONDS)) + " per second.");
        }
    }

    public static void generateTrainingCorpus(File trainingFile, File rootCorpus, File igCorpus) throws IOException {
        DataSet trainingSet = Files.readLines(trainingFile, Charsets.UTF_8, new DataSetLoader());
        System.out.println("Amount of sentences in training set:" + trainingSet.sentences.size());
        System.out.println("Amount of tokens in training set:" + trainingSet.tokenCount());
        SimpleTextWriter rootWriter = SimpleTextWriter.keepOpenUTF8Writer(rootCorpus);
        System.out.println("Generating Lemma Corpus.");
        for (SentenceData sentenceData : trainingSet) {
            List<String> roots = Lists.newArrayList("<s>");
            for (Z3WordData word : sentenceData.words) {
                Z3WordParse parse = new Z3WordParse(word.correctParse);
                roots.add(parse.root);
            }
            roots.add("</s>");
            rootWriter.writeLine(Joiner.on(" ").join(roots));
        }
        rootWriter.close();
        SimpleTextWriter igWriter = SimpleTextWriter.keepOpenUTF8Writer(igCorpus);
        System.out.println("Generating IG Corpus.");
        Z3WordParse start = new Z3WordParse(SENTENCE_START_PARSE);
        Z3WordParse end = new Z3WordParse(SENTENCE_END_PARSE);
        for (SentenceData sentenceData : trainingSet) {
            if(sentenceData.words.size()==0)
                continue;
            Z3WordParse first = start;
            Z3WordParse second = new Z3WordParse(sentenceData.words.get(0).correctParse);
            for (int i = 1; i < sentenceData.words.size(); i++) {
                Z3WordParse third = new Z3WordParse(sentenceData.words.get(i).correctParse);
                for (int j = 0; j < third.igs.size(); j++) {
                    igWriter.writeLine(first.getLastIg() + " " + second.getLastIg() + " " + third.igs.get(j));
                }
                first = second;
                second = third;
            }
            igWriter.writeLine(first.getLastIg() + " " + second.getLastIg() + " " + end.getLastIg());
        }
        igWriter.close();
    }

    public int[] bestSequence(Ambiguous[] ambiguousSeq) {
        Hypothesis[] hypotheses = {Hypothesis.EMPTY_HYPOTHESIS};
        for (int i = 2; i < ambiguousSeq.length; i++) { // we skip the two <s> words.
            Ambiguous word = ambiguousSeq[i];
            // there is exactly parse amount of live Hypothesis at a time
            Hypothesis[] newHypotheses = new Hypothesis[word.size()];
            for (int parseIndex = 0; parseIndex < word.size(); parseIndex++) {
                double bestScore = Double.NEGATIVE_INFINITY;
                Hypothesis bestHyp = hypotheses[0];
                for (Hypothesis hyp : hypotheses) {
                    int[] rootTrigram = {
                            ambiguousSeq[i - 2].roots[hyp.twoBeforeIndex],
                            ambiguousSeq[i - 1].roots[hyp.oneBeforeIndex],
                            word.roots[parseIndex]
                    };
                    double rootLmScore = containsUnknown(rootTrigram) ? UNKNOWN_PROB : rootLm.getProbability(rootTrigram);
                    int igTrigram[] = {
                            ambiguousSeq[i - 2].lastIgs[hyp.twoBeforeIndex],
                            ambiguousSeq[i - 1].lastIgs[hyp.oneBeforeIndex],
                            -1
                    };
                    double igLmScore = 0;
                    for (int j = 0; j < word.igs[parseIndex].length; j++) {
                        igTrigram[2] = word.igs[parseIndex][j];
                        igLmScore += containsUnknown(igTrigram) ? UNKNOWN_PROB : igLm.getProbability(igTrigram);
                    }
                    double total = hyp.score + rootLmScore + igLmScore;
                    if (total > bestScore) {
                        bestScore = total;
                        bestHyp = hyp;
                    }
                }
                newHypotheses[parseIndex] = new Hypothesis(bestHyp, bestScore, bestHyp.oneBeforeIndex, parseIndex);
            }
            hypotheses = newHypotheses;
        }
        // find the best parse index sequence using backtracking
        int[] result = new int[ambiguousSeq.length - 3];
        int j = result.length - 1;
        Hypothesis h = hypotheses[0].previous; // the hypothesis before the </s>
        while (h.previous != null) { // keep until previous hypothesis is EMPTY_HYPOTHESIS
            result[j] = h.oneBeforeIndex;
            h = h.previous;
            j--;
        }
        return result;
    }

    private boolean containsUnknown(int[] indexes) {
        for (int i : indexes) {
            if (i == -1) return true;
        }
        return false;
    }

    @Override
    public void disambiguate(SentenceAnalysis sentenceParse) {
        Ambiguous[] ambiguousSeq = getAmbiguousSequence(sentenceParse);
        int[] bestSequence = bestSequence(ambiguousSeq);
        for (int i = 0; i < bestSequence.length; i++) {
            List<WordAnalysis> results = sentenceParse.getParses(i);
            if (results.size() == 1)
                continue;
            WordAnalysis tmp = results.get(0);
            results.set(0, results.get(bestSequence[i]));
            results.set(bestSequence[i], tmp);
        }
    }

    static class Hypothesis {
        static final Hypothesis EMPTY_HYPOTHESIS = new Hypothesis(null, 0.0d, 0, 0);
        Hypothesis previous;
        double score;
        int twoBeforeIndex, oneBeforeIndex;

        Hypothesis(Hypothesis previous, double score, int twoBeforeIndex, int oneBeforeIndex) {
            this.previous = previous;
            this.score = score;
            this.twoBeforeIndex = twoBeforeIndex;
            this.oneBeforeIndex = oneBeforeIndex;
        }
    }

    public Ambiguous[] getAmbiguousSequence(SentenceAnalysis sentence) {
        Ambiguous[] awords = new Ambiguous[sentence.size() + 3];
        awords[0] = startWord;
        awords[1] = startWord;
        int i = 2;
        for (SentenceAnalysis.Entry entry : sentence) {
            int[] roots = new int[entry.parses.size()];
            int[][] igs = new int[entry.parses.size()][];
            int j = 0;
            for (WordAnalysis parse : entry.parses) {
                String rootPart = parse.dictionaryItem.lemma;
                roots[j] = rootLm.getVocabulary().indexOf(rootPart);
                igs[j] = new int[parse.inflectionalGroups.size()];
                for (int k = 0; j < parse.inflectionalGroups.size(); k++) {
                    igs[j][k] = igLm.getVocabulary().indexOf(parse.inflectionalGroups.get(k).formatNoSurface());
                }
                j++;
            }
            awords[i] = new Ambiguous(roots, igs);
            i++;
        }
        awords[i] = endWord;
        return awords;
    }

    public Ambiguous[] getAmbiguousSequence(SentenceData sentence) {
        Ambiguous[] awords = new Ambiguous[sentence.size() + 3];
        awords[0] = startWord;
        awords[1] = startWord;
        int i = 2;
        for (Z3WordData word : sentence.words) {
            int[] roots = new int[word.allParses.size()];
            int[][] igs = new int[word.allParses.size()][];
            int j = 0;
            for (String parseStr : word.allParses) {
                Z3WordParse parse = new Z3WordParse(parseStr);
                String rootPart = parse.root;
                roots[j] = rootLm.getVocabulary().indexOf(rootPart);
                igs[j] = new int[parse.igs.size()];
                for (int k = 0;k < parse.igs.size(); k++) {
                    igs[j][k] = igLm.getVocabulary().indexOf(parse.igs.get(k));
                }
                j++;
            }
            awords[i] = new Ambiguous(roots, igs);
            i++;
        }
        awords[i] = endWord;
        return awords;
    }

    static class Ambiguous {
        int[] roots;
        int[][] igs;
        int[] lastIgs;

        Ambiguous(int[] roots, int[][] igs) {
            this.roots = roots;
            this.igs = igs;
            this.lastIgs = new int[roots.length];
            for (int i = 0; i < lastIgs.length; i++) {
                lastIgs[i] = igs[i][igs[i].length - 1];
            }
        }

        int size() {
            return roots.length;
        }
    }
}