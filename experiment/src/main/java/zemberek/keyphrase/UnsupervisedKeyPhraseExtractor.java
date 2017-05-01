package zemberek.keyphrase;

import org.antlr.v4.runtime.Token;
import zemberek.core.ScoredItem;
import zemberek.core.collections.Histogram;
import zemberek.core.logging.Log;
import zemberek.core.turkish.PrimaryPos;
import zemberek.corpus.WebCorpus;
import zemberek.corpus.WebDocument;
import zemberek.morphology.analysis.WordAnalysis;
import zemberek.morphology.analysis.tr.TurkishSentenceAnalyzer;
import zemberek.morphology.structure.Turkish;
import zemberek.tokenization.TurkishSentenceExtractor;
import zemberek.tokenization.TurkishTokenizer;
import zemberek.tokenization.antlr.TurkishLexer;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Based on [SGRank: Combining Statistical and Graphical Methods to Improve the State of the Art
 * in Unsupervised KeyPhrase Extraction] by Danesh et-al 2015
 * <pre>
 * Steps
 * 1- Extract Ngrams [t] from document [d] (n = 1..5).
 * 2- Eliminate Ngrams that contains punctuations, stop words and POS other than noun, adj and verb
 * 3- Eliminate counts less than a [threshold] depending on document size.
 *    for l < 1500 words : t = 0 , 1500 < l < 4500 : t = 2 , l > 4500 : t = 3
 * 4- Apply modified tf-idf. for n>1, document frequency is considered 1.
 * 5- Get top T=100 from step 4.
 * 6- Calculate Position of First Occurrence value PFO(t,d) = log(cutoffPosition/p(t,d))
 *    cutoff position is arbitrary (1000-3000). p(t,d) index of the first occurrence.
 * 7- Re-Rank formula is w(t,d) = (tf(t,d)-subSumCount(t,d)) * idf(t) * PFO(t,d) * TL(t)
 *    tf(t) = term frequency = (Number of times term t appears in a document) / (Total number of terms in the document).
 *    subSumCount(t,d) = sum of term frequencies of all terms included in the Top T list that subsume t.
 *    idf(t) = inverse document frequency = log(Total number of documents / Number of documents with term t in it).
 *             second value is 1 for n>1
 *    TL(t) = term length (words) - longer N-grams are better candidates.
 * 8- Get terms with positive values from 7.
 * 9- Create a graph. Vertices are terms. An edge is created for terms occur in a window of size d. d ~ 1000-1500
 * 10-Edge weights are: Wd(ti, tj) = SUM(tf(ti)) //TODO: continue.
 *
 * </pre>
 */
public class UnsupervisedKeyPhraseExtractor {

    static final TurkishSentenceExtractor extractor = TurkishSentenceExtractor.DEFAULT;
    static final TurkishTokenizer lexer = TurkishTokenizer.DEFAULT;
    CorpusStatistics statistics;
    final int order;
    TurkishSentenceAnalyzer sentenceAnalyzer;

    public UnsupervisedKeyPhraseExtractor(CorpusStatistics statistics, TurkishSentenceAnalyzer sentenceAnalyzer) {
        this.sentenceAnalyzer = sentenceAnalyzer;
        this.statistics = statistics;
        this.order = 3;
    }

    public UnsupervisedKeyPhraseExtractor(CorpusStatistics statistics, int termGramOrder) {
        this.statistics = statistics;
        this.order = termGramOrder;
    }

    static class Term {
        String[] words;
        int firstOccurrenceIndex;

        public Term(String[] words) {
            this.words = words;
        }

        public void setFirstOccurrenceIndex(int firstOccurrenceIndex) {
            this.firstOccurrenceIndex = firstOccurrenceIndex;
        }

        public int order() {
            return words.length;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Term ngram = (Term) o;
            return Arrays.deepEquals(words, ngram.words);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(words);
        }

        @Override
        public String toString() {
            return Arrays.toString(words);
        }

        boolean contains(Term t) {
            if (t.order() > order()) {
                return false;
            }
            for (int i = 0; i < words.length - t.order(); i++) {
                String word = words[i];
                if (!word.equals(t.words[0])) {
                    continue;
                } else if (t.order() == 1) {
                    return true;
                }
                boolean found = true;
                for (int j = 1; j < t.words.length; j++) {
                    String w = words[i + j];
                    if (!w.equals(t.words[j])) {
                        found = false;
                        break;
                    }
                }
                if (found) {
                    return true;
                }
            }
            return false;
        }
    }

    public List<ScoredItem<Term>> initialRank(List<Histogram<Term>> histograms) {
        List<ScoredItem<Term>> scores = new ArrayList<>(); // TODO: use a priority queue
        int termCount = histograms.get(0).size();
        for (Histogram<Term> ngramTerms : histograms) {
            for (Term term : ngramTerms) {
                double tf = ((double) ngramTerms.getCount(term)) / termCount;
                // add 1 for smoothing.
                double termDocCount = term.order() == 1 ? (statistics.documentFrequencies.getCount(term.words[0])) + 1 : 1;
                double idf = Math.log(statistics.documentCount / termDocCount);
                ScoredItem<Term> scoredItem = new ScoredItem<>(term, (float) (tf * idf));
                scores.add(scoredItem);
            }
        }
        Collections.sort(scores);
        return new ArrayList<>(scores.subList(0, scores.size()));
    }

    private float DEFAULT_CUTOFF_POSITION = 1000;

    public static String normalize(String s) {
        return s.toLowerCase(Turkish.LOCALE);
    }

    public List<ScoredItem<Term>> rescoreStatistics(
            List<Histogram<Term>> histograms,
            List<ScoredItem<Term>> initialScores) {

        int termCount = histograms.get(0).size();
        List<ScoredItem<Term>> scores = new ArrayList<>(); // TODO: use a priority queue

        for (ScoredItem<Term> si : initialScores) {
            Term term = si.item;
            // position of first occurrence
            double pfo = (double) Math.log(DEFAULT_CUTOFF_POSITION / (si.item.firstOccurrenceIndex + 1));
            double termLength = Math.sqrt(term.order());
            double tf = termCount - subSumCount(term, histograms, initialScores.subList(0, 100));
            // add 1 for smoothing.
            double termDocCount = term.order() == 1 ? (statistics.documentFrequencies.getCount(term.words[0])) + 1 : 1;
            double idf = Math.log(statistics.documentCount / termDocCount);
            ScoredItem<Term> scoredItem = new ScoredItem<>(term, (float) (tf * pfo * idf * termLength));
            scores.add(scoredItem);
        }
        Collections.sort(scores);
        return new ArrayList<>(scores.subList(0, scores.size()));
    }

    private int subSumCount(Term t, List<Histogram<Term>> histograms) {
        int sum = 0;
        for (int i = t.order(); i < order; i++) {
            for (Term t2 : histograms.get(i)) {
                if (t2.contains(t)) {
                    sum += histograms.get(i).getCount(t2);
                }
            }
        }
        return sum;
    }

    private int subSumCount(Term t, List<Histogram<Term>> histograms, List<ScoredItem<Term>> top) {
        int sum = 0;
        for (ScoredItem<Term> scoredItem : top) {
            Term t2 = scoredItem.item;
            if (t.order() >= t2.order()) {
                continue;
            }
            if (t2.contains(t)) {
                sum += histograms.get(t2.order() - 1).getCount(t2);
            }
        }
        return sum;
    }


    public List<Histogram<Term>> ngrams(List<String> paragraphs) {
        if (sentenceAnalyzer == null) {
            return wordNgrams(paragraphs);
        } else {
            return lemmaNgrams(paragraphs);
        }
    }


    private List<Histogram<Term>> wordNgrams(List<String> paragraphs) {

        List<Histogram<Term>> ngrams = new ArrayList<>(order + 1);
        for (int i = 0; i < order; i++) {
            ngrams.add(new Histogram<>(100));
        }

        int tokenCount = 0;

        List<String> sentences = extractor.fromParagraphs(paragraphs);
        for (String sentence : sentences) {
            List<Token> tokens = lexer.tokenize(sentence);

            for (int i = 0; i < order; i++) {
                collectGrams(tokens, ngrams.get(i), i + 1, tokenCount);
            }
            // TODO: should we count only term tokens?
            tokenCount += tokens.size();
        }
        return ngrams;
    }

    private List<Histogram<Term>> lemmaNgrams(List<String> paragraphs) {

        List<Histogram<Term>> ngrams = new ArrayList<>(order + 1);
        for (int i = 0; i < order; i++) {
            ngrams.add(new Histogram<>(100));
        }

        int tokenCount = 0;

        List<String> sentences = extractor.fromParagraphs(paragraphs);
        for (String sentence : sentences) {

            List<WordAnalysis> analysis = sentenceAnalyzer.bestParse(sentence);

            for (int i = 0; i < order; i++) {
                int currentOrder = i + 1;
                for (int j = 0; j < analysis.size() - currentOrder; j++) {
                    String[] words = new String[currentOrder];
                    boolean fail = false;
                    for (int k = 0; k < currentOrder; k++) {
                        WordAnalysis a = analysis.get(j + k);
                        if (!analysisAcceptable(a)) {
                            fail = true;
                            break;
                        }
                        String surface = a.getSurfaceForm();
                        if (TurkishStopWords.DEFAULT.contains(surface)) {
                            fail = true;
                            break;
                        }
                        List<String> lemmas = a.getLemmas();
                        words[k] = lemmas.get(lemmas.size() - 1);
                    }
                    if (!fail) {
                        Term term = new Term(words);
                        int count = ngrams.get(i).add(term);
                        if (count == 1) { // if this is the first time, set the first occurance index.
                            term.setFirstOccurrenceIndex(tokenCount + j);
                        }
                    }
                    tokenCount += analysis.size();
                }
            }
        }
        return ngrams;
    }

    void collectGrams(List<Token> tokens, Histogram<Term> grams, int order, int offset) {
        for (int i = 0; i < tokens.size() - order; i++) {
            String[] words = new String[order];
            boolean fail = false;
            for (int j = 0; j < order; j++) {
                Token t = tokens.get(i + j);

                if (!tokenTypeAccpetable(t)) {
                    fail = true;
                    break;
                }
                String word = normalize(t.getText());
                if (TurkishStopWords.DEFAULT.contains(word)) {
                    fail = true;
                    break;
                }

                words[j] = word;
            }
            if (!fail) {
                Term t = new Term(words);
                int count = grams.add(t);
                if (count == 1) { // if this is the first time, set the first occurance index.
                    t.setFirstOccurrenceIndex(offset + i);
                }
            }
        }
    }

    static boolean tokenTypeAccpetable(Token t) {
        return !(t.getType() == TurkishLexer.Punctuation ||
                t.getType() == TurkishLexer.Number ||
                t.getType() == TurkishLexer.RomanNumeral ||
                t.getType() == TurkishLexer.PercentNumeral ||
                t.getType() == TurkishLexer.Time ||
                t.getType() == TurkishLexer.Date ||
                t.getType() == TurkishLexer.Emoticon ||
                t.getType() == TurkishLexer.URL ||
                t.getType() == TurkishLexer.Email ||
                t.getType() == TurkishLexer.HashTag ||
                t.getType() == TurkishLexer.Unknown);
    }

    static boolean analysisAcceptable(WordAnalysis t) {
        //TODO: should we keep verb roots?
        return (t.getPos() == PrimaryPos.Noun ||
                t.getPos() == PrimaryPos.Adjective) && (t.getDictionaryItem().primaryPos != PrimaryPos.Verb);
    }

    static class CorpusStatistics {
        Histogram<String> termFrequencies;
        Histogram<String> documentFrequencies;
        int documentCount;

        CorpusStatistics(int expectedTermCount) {
            termFrequencies = new Histogram<>(expectedTermCount);
            documentFrequencies = new Histogram<>(expectedTermCount);
        }

        CorpusStatistics(
                Histogram<String> termFrequencies,
                Histogram<String> documentFrequencies,
                int documentCount) {
            this.termFrequencies = termFrequencies;
            this.documentFrequencies = documentFrequencies;
            this.documentCount = documentCount;
        }

        void serialize(DataOutputStream dos) throws IOException {
            dos.writeInt(documentCount);
            Histogram.serializeStringHistogram(termFrequencies, dos);
            Histogram.serializeStringHistogram(documentFrequencies, dos);
        }

        static CorpusStatistics deserialize(DataInputStream dis) throws IOException {
            int docCount = dis.readInt();
            Histogram<String> termFreq = Histogram.deserializeStringHistogram(dis);
            Histogram<String> docFreq = Histogram.deserializeStringHistogram(dis);
            return new CorpusStatistics(termFreq, docFreq, docCount);
        }
    }

    static CorpusStatistics collectCorpusStatistics(WebCorpus corpus) throws IOException {

        CorpusStatistics statistics = new CorpusStatistics(1_000_000);

        for (WebDocument document : corpus.getDocuments()) {
            Histogram<String> docHistogram = new Histogram<>();
            List<String> sentences = extractor.fromParagraphs(document.getLines());
            for (String sentence : sentences) {
                List<Token> tokens = lexer.tokenize(sentence);

                for (Token token : tokens) {
                    if (!tokenTypeAccpetable(token)) {
                        continue;
                    }
                    String s = normalize(token.getText());
                    if (TurkishStopWords.DEFAULT.contains(s)) {
                        continue;
                    }
                    docHistogram.add(s);
                }
            }
            statistics.termFrequencies.add(docHistogram);
            for (String s : docHistogram) {
                statistics.documentFrequencies.add(s);
            }
        }
        statistics.documentCount = corpus.documentCount();
        return statistics;
    }

    static CorpusStatistics collectCorpusStatisticsForLemmas(
            WebCorpus corpus, TurkishSentenceAnalyzer analyzer, int count) throws IOException {

        CorpusStatistics statistics = new CorpusStatistics(1_000_000);

        int docCount = 0;
        for (WebDocument document : corpus.getDocuments()) {
            Histogram<String> docHistogram = new Histogram<>();
            List<String> sentences = extractor.fromParagraphs(document.getLines());
            for (String sentence : sentences) {
                List<WordAnalysis> analysis = analyzer.bestParse(sentence);
                for (WordAnalysis w : analysis) {
                    if (!analysisAcceptable(w)) {
                        continue;
                    }
                    String s = w.getSurfaceForm();
                    if (TurkishStopWords.DEFAULT.contains(s)) {
                        continue;
                    }
                    List<String> lemmas = w.getLemmas();
                    docHistogram.add(lemmas.get(lemmas.size() - 1));
                }
            }
            statistics.termFrequencies.add(docHistogram);
            for (String s : docHistogram) {
                statistics.documentFrequencies.add(s);
            }
            if (docCount++ % 500 == 0) {
                Log.info("Doc count = %d", docCount);
            }
            if (count > 0 && docCount > count) {
                break;
            }
        }
        statistics.documentCount = count > 0 ? Math.min(count, corpus.documentCount()) : corpus.documentCount();
        return statistics;
    }

}
