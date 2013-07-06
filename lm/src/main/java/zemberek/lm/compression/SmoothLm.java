package zemberek.lm.compression;

import zemberek.core.logging.Log;
import zemberek.core.math.LogMath;
import zemberek.lm.LmVocabulary;
import zemberek.core.hash.LargeNgramMphf;
import zemberek.core.hash.Mphf;
import zemberek.core.hash.MultiLevelMphf;
import zemberek.core.quantization.DoubleLookup;

import java.io.*;
import java.util.Arrays;
import java.util.logging.Logger;

/**
 * SmoothLm is a compressed, optionally quantized, randomized back-off n-gram language model.
 * It uses Minimal Perfect Hash functions for compression, This means actual n-gram values are not stored in the model.
 * This is a lossy model because for non existing n-grams it may return an existing n-gram probability value.
 * Probability of this happening depends on the fingerprint hash length. This value is determined during the model creation.
 * Regularly 8,16 or 24 bit fingerprints are used and false positive probability for an non existing n-gram is
 * (probability of an n-gram does not exist in LM)*1/(2^fingerprint bit size).
 * SmoothLm also provides quantization for even more compactness. So probability and back-off values can be quantized to
 * 8, 16 or 24 bits.
 */
public class SmoothLm {

    public static final double DEFAULT_LOG_BASE = 10;
    public static final double DEFAULT_UNIGRAM_WEIGHT = 1;
    public static final double DEFAULT_UNKNOWN_BACKOFF_PENALTY = 0;
    public static final double DEFAULT_STUPID_BACKOFF_ALPHA = 0.4;
    private static final double DEFAULT_UNKNOWN_TOKEN_PROBABILITY = -20;

    private final int version;
    private final int order;
    private final Mphf[] mphfs;

    private final DoubleLookup[] probabilityLookups;
    private final DoubleLookup[] backoffLookups;
    private final GramDataArray[] ngramData;

    private double[] unigramProbs;
    private double[] unigramBackoffs;

    int[] counts;

    MphfType type;

    private final LmVocabulary vocabulary;

    public static final double LOG_ZERO = -Math.log(Double.MAX_VALUE);

    private double logBase;
    private double unigramWeight;
    private double unknownBackoffPenalty;
    private boolean useStupidBackoff = false;
    private double stupidBackoffLogAlpha;
    private double stupidBackoffAlpha;

    /**
     * Builder is used for instantiating the compressed language model.
     * Default values:
     * <p>Log Base = e
     * <p>Unknown backoff penalty = 0
     * <p>Default unigram weight = 1
     * <p>Use Stupid Backoff = false
     * <p>Stupid Backoff alpha value = 0.4
     */
    public static class Builder {
        private double _logBase = DEFAULT_LOG_BASE;
        private double _unknownBackoffPenalty = DEFAULT_UNKNOWN_BACKOFF_PENALTY;
        private double _unigramWeight = DEFAULT_UNIGRAM_WEIGHT;
        private boolean _useStupidBackoff = false;
        private double _stupidBackoffAlpha = DEFAULT_STUPID_BACKOFF_ALPHA;
        private DataInputStream _dis;

        public Builder(InputStream is) {
            this._dis = new DataInputStream(new BufferedInputStream(is));
        }

        public Builder(File file) throws FileNotFoundException {
            this._dis = new DataInputStream(new BufferedInputStream(new FileInputStream(file)));
        }

        public Builder logBase(double logBase) {
            this._logBase = logBase;
            return this;
        }

        public Builder unknownBackoffPenalty(double unknownPenalty) {
            this._unknownBackoffPenalty = unknownPenalty;
            return this;
        }

        public Builder unigramWeight(double weight) {
            this._unigramWeight = weight;
            return this;
        }

        public Builder useStupidBackoff() {
            this._useStupidBackoff = true;
            return this;
        }

        public Builder stupidBackoffAlpha(double alphaValue) {
            this._stupidBackoffAlpha = alphaValue;
            return this;
        }

        public SmoothLm build() throws IOException {
            return new SmoothLm(
                    _dis,
                    _logBase,
                    _unigramWeight,
                    _unknownBackoffPenalty,
                    _useStupidBackoff,
                    _stupidBackoffAlpha);
        }
    }

    public static Builder builder(InputStream is) throws IOException {
        return new Builder(is);
    }

    public static Builder builder(File modelFile) throws IOException {
        return new Builder(modelFile);
    }

    private SmoothLm(
            DataInputStream dis,
            double logBase,
            double unigramWeight,
            double unknownBackoffPenalty,
            boolean useStupidBackoff,
            double stupidBackoffAlpha) throws IOException {
        this(dis); // load the lm data.
        // Now apply necessary transformations and configurations
        this.unigramWeight = unigramWeight;
        this.unknownBackoffPenalty = unknownBackoffPenalty;
        this.useStupidBackoff = useStupidBackoff;
        this.stupidBackoffAlpha = stupidBackoffAlpha;

        if (logBase != DEFAULT_LOG_BASE) {
            Log.info("Changing log base from " + DEFAULT_LOG_BASE + " to " + logBase);
            changeLogBase(logBase);
            this.stupidBackoffLogAlpha = Math.log(stupidBackoffAlpha) / Math.log(logBase);
        } else {
            this.stupidBackoffLogAlpha = Math.log(stupidBackoffAlpha) / Math.log(DEFAULT_LOG_BASE);
        }

        this.logBase = logBase;

        if (unigramWeight != DEFAULT_UNIGRAM_WEIGHT) {
            Log.info("Applying unigram smoothing with unigram weight: " + unigramWeight);
            applyUnigramSmoothing(unigramWeight);
        }

        if (useStupidBackoff) {
            Log.info("Lm will use stupid back off with alpha value: " + stupidBackoffAlpha);
        }
    }

    /**
     * Returns human readable information about the model.
     */
    public String info() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Order : %d%n", order));

        for (int i = 1; i < ngramData.length; i++) {
            GramDataArray gramDataArray = ngramData[i];
            sb.append(String.format("%d Grams: Count= %d  Fingerprint Bits= %d  Probabilty Bits= %d  Back-off bits= %d%n",
                    i,
                    gramDataArray.count,
                    gramDataArray.fpSize * 8,
                    gramDataArray.probSize * 8,
                    gramDataArray.backoffSize * 8));
        }
        sb.append(String.format("Log Base              : %.2f%n", logBase));
        sb.append(String.format("Unigram Weight        : %.2f%n", unigramWeight));
        sb.append(String.format("Using Stupid Back-off?: %s%n", useStupidBackoff ? "Yes" : "No"));
        if (useStupidBackoff)
            sb.append(String.format("Stupid Back-off Alpha Value   : %.2f%n", stupidBackoffAlpha));
        sb.append(String.format("Unknown Back-off N-gram penalty: %.2f%n", unknownBackoffPenalty));
        return sb.toString();
    }

    public static enum MphfType {
        SMALL, LARGE
    }

    public int getVersion() {
        return version;
    }

    public int getOrder() {
        return order;
    }

    public LmVocabulary getVocabulary() {
        return vocabulary;
    }

    public int getGramCount(int n) {
        return ngramData[n].count;
    }

    private SmoothLm(DataInputStream dis) throws IOException {
        this.version = dis.readInt();
        int typeInt = dis.readInt();
        if (typeInt == 0)
            type = MphfType.SMALL;
        else
            type = MphfType.LARGE;

        this.logBase = dis.readDouble();
        this.order = dis.readInt();

        counts = new int[order + 1];
        for (int i = 1; i <= order; i++) {
            counts[i] = dis.readInt();
        }

        // probability lookups
        probabilityLookups = new DoubleLookup[order + 1];
        for (int i = 1; i <= order; i++) {
            probabilityLookups[i] = DoubleLookup.getLookup(dis);
        }
        // backoff lookups
        backoffLookups = new DoubleLookup[order + 1];
        for (int i = 1; i < order; i++) {
            backoffLookups[i] = DoubleLookup.getLookup(dis);
        }

        //load fingerprint, probability and backoff data.
        ngramData = new GramDataArray[order + 1];
        for (int i = 1; i <= order; i++) {
            ngramData[i] = new GramDataArray(dis);
        }

        // we take the unigram probability data out to get rid of rank lookups for speed.
        int unigramCount = ngramData[1].count;
        unigramProbs = new double[unigramCount];
        unigramBackoffs = new double[unigramCount];
        for (int i = 0; i < unigramCount; i++) {
            final int probability = ngramData[1].getProbabilityRank(i);
            unigramProbs[i] = probabilityLookups[1].get(probability);
            final int backoff = ngramData[1].getBackoffRank(i);
            unigramBackoffs[i] = backoffLookups[1].get(backoff);
        }

        // load MPHFs
        if (type == MphfType.LARGE) {
            mphfs = new LargeNgramMphf[order + 1];
            for (int i = 2; i <= order; i++) {
                mphfs[i] = LargeNgramMphf.deserialize(dis);
            }
        } else {
            mphfs = new MultiLevelMphf[order + 1];
            for (int i = 2; i <= order; i++) {
                mphfs[i] = MultiLevelMphf.deserialize(dis);
            }
        }

        // load vocabulary
        vocabulary = LmVocabulary.loadFromDataInputStream(dis);

        // in case special tokens that does not exist in the actual unigrams are added (such as <unk>)
        // we adjust unigram data accordingly.
        int vocabularySize = vocabulary.size();
        if (vocabularySize > unigramCount) {
            ngramData[1].count = vocabularySize;
            unigramProbs = Arrays.copyOf(unigramProbs, vocabularySize);
            unigramBackoffs = Arrays.copyOf(unigramBackoffs, vocabularySize);
            for (int i = unigramCount; i < vocabularySize; i++) {
                unigramProbs[i] = DEFAULT_UNKNOWN_TOKEN_PROBABILITY;
                unigramBackoffs[i] = 0;
            }
        }

        dis.close();
    }

    /**
     * Retrieves the dequantized log probability of an n-gram.
     *
     * @param wordIndexes token-index sequence. A word index is the unigram index value.
     * @return dequantized log probability of the n-gram. if n-gram does not exist, it returns LOG_ZERO
     */
    public double getProbabilityValue(int... wordIndexes) {

        final int ng = wordIndexes.length;
        if (ng == 1) {
            return unigramProbs[wordIndexes[0]];
        }

        int quickHash = MultiLevelMphf.hash(wordIndexes, -1);

        int index = mphfs[ng].get(wordIndexes, quickHash);

        if (ngramData[ng].checkFingerPrint(quickHash, index))
            return probabilityLookups[ng].get(ngramData[ng].getProbabilityRank(index));
        else {
            return LOG_ZERO;
        }
    }

    /**
     * Retrieves the dequantized backoff value of an n-gram.
     *
     * @param wordIndexes word-index sequence. A word index is the unigram index value.
     * @return dequantized log back-off value of the n-gram.
     *         if n-gram does not exist, it returns unknownBackoffPenalty value.
     */
    public double getBackoffValue(int... wordIndexes) {
        if (useStupidBackoff)
            return stupidBackoffLogAlpha;
        final int ng = wordIndexes.length;
        if (ng == 1) {
            return unigramBackoffs[wordIndexes[0]];
        }
        final int quickHash = MultiLevelMphf.hash(wordIndexes, -1);

        final int nGramIndex = mphfs[ng].get(wordIndexes, quickHash);

        if (ngramData[ng].checkFingerPrint(quickHash, nGramIndex))
            return backoffLookups[ng].get(ngramData[ng].getBackoffRank(nGramIndex));
        else {
            return unknownBackoffPenalty;
        }
    }

    /**
     * Calculates the dequantized probability value for an n-gram. If n-gram does not exist, it applies
     * backoff calculation.
     *
     * @param wordIndexes word index sequence. A word index is the unigram index value.
     * @return dequantized log backoff value of the n-gram. if there is no backoff value or n-gram does not exist,
     *         it returns LOG_ZERO. This mostly happens in the condition that words queried does not exist
     *         in the vocabulary.
     * @throws IllegalArgumentException if wordIndexes sequence length is zero or more than n value.
     */
    double getProbabilityRecursive(int... wordIndexes) {

        if (wordIndexes.length == 0 || wordIndexes.length > order)
            throw new IllegalArgumentException(
                    "At least one or max Gram Count" + order + " tokens are required. But it is:" + wordIndexes.length);

        double result = 0;
        double probability = getProbabilityValue(wordIndexes);
        if (probability == LOG_ZERO) { // if probability does not exist.
            if (wordIndexes.length == 1)
                return LOG_ZERO;
            double backoffValue = useStupidBackoff ? stupidBackoffLogAlpha : getBackoffValue(head(wordIndexes));
            result = result + backoffValue + getProbabilityRecursive(tail(wordIndexes));
        } else {
            result = probability;
        }
        return result;
    }

    private static final int BIT_MASK21 = (1 << 21) - 1;

    /**
     * Returns the log probability value of an encoded Trigram. Trigram is embedded into an 64 bit long value as:
     * [1bit empty][21 bits gram-3][21 bits gram-2][21 bits gram-1]
     *
     * @param encodedTrigram encoded trigram
     * @return log probability.
     */
    public double getEncodedTrigramProbability(long encodedTrigram) {
        int fingerPrint = MultiLevelMphf.hash(encodedTrigram, 3, -1);
        int nGramIndex = mphfs[3].get(encodedTrigram, 3, fingerPrint);
        double result = 0;
        if (ngramData[3].checkFingerPrint(fingerPrint, nGramIndex)) { // if p(c|a,b) exist
            return probabilityLookups[3].get(ngramData[3].getProbabilityRank(nGramIndex));
        } else { // we back off to two grams. p(c|a,b) ~ b(a,b) + p(c|b)
            if (useStupidBackoff)
                result += stupidBackoffLogAlpha;
            else {
                fingerPrint = MultiLevelMphf.hash(encodedTrigram, 2, -1);
                nGramIndex = mphfs[2].get(encodedTrigram, 2, fingerPrint);
                if (ngramData[2].checkFingerPrint(fingerPrint, nGramIndex)) { // if backoff (a,b) exist
                    result += backoffLookups[2].get(ngramData[2].getBackoffRank(nGramIndex));
                } else result += unknownBackoffPenalty;
            }
            long encodedBigram = encodedTrigram >>> 21;
            fingerPrint = MultiLevelMphf.hash(encodedBigram, 2, -1);
            nGramIndex = mphfs[2].get(encodedBigram, 2, fingerPrint);
            if (ngramData[2].checkFingerPrint(fingerPrint, nGramIndex)) { // if p(b|c) exists
                return result + probabilityLookups[2].get(ngramData[2].getProbabilityRank(nGramIndex));
            } else { // p(b|c) ~ b(b) + p(c)
                result += unigramProbs[((int) (encodedBigram >> 21))];
                if (useStupidBackoff)
                    return result + stupidBackoffLogAlpha;
                return result + unigramBackoffs[((int) (encodedBigram & BIT_MASK21))];
            }
        }
    }

    private int[] head(int[] arr) {
        if (arr.length == 1)
            return new int[0];
        int[] head = new int[arr.length - 1];
        System.arraycopy(arr, 0, head, 0, arr.length - 1);
        return head;
    }

    private int[] tail(int[] arr) {
        if (arr.length == 1)
            return new int[0];
        int[] head = new int[arr.length - 1];
        System.arraycopy(arr, 1, head, 0, arr.length - 1);
        return head;
    }

    /**
     * This is the non recursive log probability calculation. It is more complicated but faster.
     *
     * @param words word array
     * @return log probability.
     */
    public double getProbability(String... words) {
        return getProbability(vocabulary.toIndexes(words));
    }

    /**
     * This is the non recursive log probability calculation. It is more complicated but faster.
     *
     * @param wordIndexes word index array
     * @return log probability.
     */
    public double getProbability(int... wordIndexes) {
        int n = wordIndexes.length;

        if (n == 0 || n > order)
            throw new IllegalArgumentException(
                    "At least one or max Gram Count" + order + " tokens are required. But it is:" + wordIndexes.length);
        if (n == 1)
            return unigramProbs[wordIndexes[0]];
        if (n == 2) {
            double prob = getProbabilityValue(wordIndexes);
            if (prob == LOG_ZERO) {
                return unigramBackoffs[wordIndexes[0]] + unigramProbs[wordIndexes[1]];
            } else {
                return prob;
            }
        }
        int begin = 0;
        double result = 0;
        int gram = n;
        while (gram > 1) {
            // try to find P(N|begin..N-1)
            int fingerPrint = MultiLevelMphf.hash(wordIndexes, begin, n, -1);
            int nGramIndex = mphfs[gram].get(wordIndexes, begin, n, fingerPrint);
            if (!ngramData[gram].checkFingerPrint(fingerPrint, nGramIndex)) { // if there is no probability value, back off to B(begin..N-1)
                if (useStupidBackoff) {
                    if (gram == 2)
                        return result + unigramProbs[wordIndexes[n - 1]] + stupidBackoffLogAlpha;
                    else
                        result += stupidBackoffLogAlpha;
                } else {
                    // we are already backed off to unigrams because no bigram found. So we return only P(N)+B(N-1)
                    if (gram == 2) {
                        return result + unigramProbs[wordIndexes[n - 1]] + unigramBackoffs[wordIndexes[begin]];
                    }
                    fingerPrint = MultiLevelMphf.hash(wordIndexes, begin, n - 1, -1);
                    nGramIndex = mphfs[gram - 1].get(wordIndexes, begin, n - 1, fingerPrint);
                    if (ngramData[gram - 1].checkFingerPrint(fingerPrint, nGramIndex)) { //if backoff available, we add it to resutlt.
                        result += backoffLookups[gram - 1].get(ngramData[gram - 1].getBackoffRank(nGramIndex));
                    } else
                        result += unknownBackoffPenalty;
                }
            } else {
                // we have found the P(N|begin..N-1) we return the accumulated result.
                return result + probabilityLookups[gram].get(ngramData[gram].getProbabilityRank(nGramIndex));
            }
            begin++;
            gram = n - begin;
        }
        return result;
    }

    /**
     * This method is used when calculating probability of an ngram sequence, how many times it backed off to lower order
     * n-gram calculations.
     *
     * @param tokens n-gram strings
     * @return if no back-off, returns 0 if none of the n-grams exist (Except 1 gram), it returns order-1
     */
    public int getBackoffCount(String... tokens) {
        return getBackoffCount(vocabulary.toIndexes(tokens));
    }

    /**
     * This method is used when calculating probability of an ngram sequence, how many times it backed off to lower order
     * n-gram calculations.
     *
     * @param wordIndexes n-gram index array
     * @return if no back-off, returns 0 if none of the n-grams exist (Except 1 gram), it returns order-1
     */
    public int getBackoffCount(int... wordIndexes) {
        int n = wordIndexes.length;
        if (n == 0 || n > order)
            throw new IllegalArgumentException(
                    "At least one or " + order + " tokens are required. But it is:" + wordIndexes.length);
        if (n == 1) return 0;
        if (n == 2) return getProbabilityValue(wordIndexes) == LOG_ZERO ? 1 : 0;

        int begin = 0;
        int backoffCount = 0;
        int gram = n;
        while (gram > 1) {
            // try to find P(N|begin..N-1)
            int fingerPrint = MultiLevelMphf.hash(wordIndexes, begin, n, -1);
            int nGramIndex = mphfs[gram].get(wordIndexes, begin, n, fingerPrint);
            if (!ngramData[gram].checkFingerPrint(fingerPrint, nGramIndex)) { //  back off to B(begin..N-1)
                backoffCount++;
            } else {
                return backoffCount;
            }
            begin++;
            gram = n - begin;
        }
        return backoffCount;
    }

    public String getProbabilityExpression(int... wordIndexes) {
        int last = wordIndexes[wordIndexes.length - 1];
        StringBuilder sb = new StringBuilder("p(" + vocabulary.getWord(last));
        if (wordIndexes.length > 1)
            sb.append("|");
        for (int j = 0; j < wordIndexes.length - 1; j++) {
            sb.append(vocabulary.getWord(wordIndexes[j]));
            if (j < wordIndexes.length - 2)
                sb.append(",");
        }
        sb.append(")");
        return sb.toString();
    }

    public String getBackoffExpression(int... wordIndexes) {
        StringBuilder sb = new StringBuilder("BO(");
        for (int j = 0; j < wordIndexes.length; j++) {
            sb.append(vocabulary.getWord(wordIndexes[j]));
            if (j < wordIndexes.length - 1)
                sb.append(",");
        }
        sb.append(")");
        return sb.toString();
    }

    /**
     * It generates a single line String that explains the probability calculations.
     *
     * @param wordIndexes n-gram index array
     * @return explanation String.
     */
    public String explain(int... wordIndexes) {
        return explain(new Explanation(), wordIndexes).sb.toString();
    }

    private Explanation explain(Explanation exp, int... wordIndexes) {
        double probability = getProbabilityValue(wordIndexes);
        exp.sb.append(getProbabilityExpression(wordIndexes));
        if (probability == LOG_ZERO) { // if probability does not exist.
            exp.sb.append("=[");
            double backOffValue = getBackoffValue(head(wordIndexes));
            String backOffStr = getBackoffExpression(head(wordIndexes));
            exp.sb.append(backOffStr).append("=").append(fmt(backOffValue)).append(" + ");
            exp.score = exp.score + backOffValue + explain(exp, tail(wordIndexes)).score;
            exp.sb.append("]");
        } else {
            exp.score = probability;
        }
        exp.sb.append("= ").append(fmt(exp.score));
        return exp;
    }

    private static class Explanation {
        StringBuilder sb = new StringBuilder();
        double score = 0;
    }


    private String fmt(double d) {
        return String.format("%.3f", d);
    }

    /**
     * Changes the log base. Generally probabilities in language models are log10 based. But some applications uses their
     * own log base. Such as Sphinx uses 1.0003 as their base, some other uses e.
     *
     * @param newBase new logBase
     */
    private void changeLogBase(double newBase) {
        DoubleLookup.changeBase(unigramProbs, logBase, newBase);
        DoubleLookup.changeBase(unigramBackoffs, logBase, newBase);
        for (int i = 2; i < probabilityLookups.length; i++) {
            probabilityLookups[i].changeBase(logBase, newBase);
            if (i < probabilityLookups.length - 1)
                backoffLookups[i].changeBase(logBase, newBase);
        }
        this.logBase = newBase;
    }

    /**
     * This method applies more smoothing to unigram log probability values. Some ASR engines does this.
     *
     * @param unigramWeight weight factor.
     */
    private void applyUnigramSmoothing(double unigramWeight) {
        double logUnigramWeigth = Math.log(unigramWeight);
        double inverseLogUnigramWeigth = Math.log(1 - unigramWeight);
        double logUniformUnigramProbability = -Math.log(unigramProbs.length);
        // apply uni-gram weight. This applies smoothing to unigrams. As lowering high probabilities and
        // adding gain to small probabilities.
        // uw = uni-gram weight  , uniformProb = 1/#unigram
        // so in linear domain, we apply this to all probability values as: p(w1)*uw + uniformProb * (1-uw) to
        // maintain the probability total is one while smoothing the values.
        // this converts to log(p(w1)*uw + uniformProb*(1-uw) ) which is calculated with log probabilities
        // a = log(p(w1)) + log(uw) and b = -log(#unigram)+log(1-uw) applying logsum(a,b)
        // approach is taken from Sphinx-4
        for (int i = 0; i < unigramProbs.length; i++) {
            double p1 = unigramProbs[i] + logUnigramWeigth;
            double p2 = logUniformUnigramProbability + inverseLogUnigramWeigth;
            unigramProbs[i] = LogMath.logSumExact(p1, p2);
        }
    }

    /**
     * Returns the logarithm base of the values used in this model.
     *
     * @return log base
     */
    public double getLogBase() {
        return logBase;
    }
}