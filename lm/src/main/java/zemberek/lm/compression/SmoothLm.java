package zemberek.lm.compression;

import zemberek.core.hash.LargeNgramMphf;
import zemberek.core.hash.Mphf;
import zemberek.core.hash.MultiLevelMphf;
import zemberek.core.logging.Log;
import zemberek.core.math.LogMath;
import zemberek.core.quantization.FloatLookup;
import zemberek.lm.BaseLanguageModel;
import zemberek.lm.LmVocabulary;
import zemberek.lm.NgramLanguageModel;

import java.io.*;
import java.util.Arrays;

import static java.lang.String.format;
import static java.util.Locale.ENGLISH;

/**
 * SmoothLm is a compressed, optionally quantized, randomized back-off n-gram language model.
 * It uses Minimal Perfect Hash functions for compression, This means actual n-gram values are not stored in the model.
 * Implementation is similar with the systems described in Gutthrie and Hepple's
 * 'Storing the Web in Memory: Space Efficient Language Models with Constant Time Retrieval (2010)' paper.
 * This is a lossy model because for non existing n-grams it may return an existing n-gram probability value.
 * Probability of this happening depends on the fingerprint hash length. This value is determined during the model creation.
 * Regularly 8,16 or 24 bit fingerprints are used and false positive probability for an non existing n-gram is
 * (probability of an n-gram does not exist in LM)*1/(2^fingerprint bit size).
 * SmoothLm also provides quantization for even more compactness. So probability and back-off values can be quantized to
 * 8, 16 or 24 bits.
 */
public class SmoothLm extends BaseLanguageModel implements NgramLanguageModel {

    public static final float DEFAULT_LOG_BASE = 10;
    public static final float DEFAULT_UNIGRAM_WEIGHT = 1;
    public static final float DEFAULT_UNKNOWN_BACKOFF_PENALTY = 0;
    public static final float DEFAULT_STUPID_BACKOFF_ALPHA = 0.4f;
    public static final int DEFAULT_UNKNOWN_TOKEN_PROBABILITY = -20;

    private final int version;

    private final Mphf[] mphfs;

    private final FloatLookup[] probabilityLookups;
    private final FloatLookup[] backoffLookups;
    private final GramDataArray[] ngramData;

    private float[] unigramProbs;
    private float[] unigramBackoffs;

    int[] counts;

    MphfType type;

    private float logBase;
    private float unigramWeight;
    private float unknownBackoffPenalty;
    private boolean useStupidBackoff = false;
    private float stupidBackoffLogAlpha;
    private float stupidBackoffAlpha;
    private boolean countFalsePositives;

    int falsePositiveCount;

    // used for debug purposes for calculation false-positive ratio.
    NgramIds ngramIds;

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
        private float _logBase = DEFAULT_LOG_BASE;
        private float _unknownBackoffPenalty = DEFAULT_UNKNOWN_BACKOFF_PENALTY;
        private float _unigramWeight = DEFAULT_UNIGRAM_WEIGHT;
        private boolean _useStupidBackoff = false;
        private float _stupidBackoffAlpha = DEFAULT_STUPID_BACKOFF_ALPHA;
        private DataInputStream _dis;
        private File _ngramIds;

        public Builder(InputStream is) {
            this._dis = new DataInputStream(new BufferedInputStream(is));
        }

        public Builder(File file) throws FileNotFoundException {
            this._dis = new DataInputStream(new BufferedInputStream(new FileInputStream(file)));
        }

        public Builder logBase(double logBase) {
            this._logBase = (float) logBase;
            return this;
        }

        public Builder unknownBackoffPenalty(double unknownPenalty) {
            this._unknownBackoffPenalty = (float) unknownPenalty;
            return this;
        }

        public Builder ngramKeyFilesDirectory(File dir) {
            this._ngramIds = dir;
            return this;
        }

        public Builder unigramWeight(double weight) {
            this._unigramWeight = (float) weight;
            return this;
        }

        public Builder useStupidBackoff() {
            this._useStupidBackoff = true;
            return this;
        }

        public Builder useStupidBackoff(boolean useStupidBackoff) {
            this._useStupidBackoff = useStupidBackoff;
            return this;
        }

        public Builder stupidBackoffAlpha(double alphaValue) {
            this._stupidBackoffAlpha = (float) alphaValue;
            return this;
        }

        public SmoothLm build() throws IOException {
            return new SmoothLm(
                    _dis,
                    _logBase,
                    _unigramWeight,
                    _unknownBackoffPenalty,
                    _useStupidBackoff,
                    _stupidBackoffAlpha,
                    _ngramIds);
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
            float logBase,
            float unigramWeight,
            float unknownBackoffPenalty,
            boolean useStupidBackoff,
            float stupidBackoffAlpha,
            File ngramKeyFileDir) throws IOException {
        this(dis); // load the lm data.
        // Now apply necessary transformations and configurations
        this.unigramWeight = unigramWeight;
        this.unknownBackoffPenalty = unknownBackoffPenalty;
        this.useStupidBackoff = useStupidBackoff;
        this.stupidBackoffAlpha = stupidBackoffAlpha;

        if (logBase != DEFAULT_LOG_BASE) {
            Log.debug("Changing log base from " + DEFAULT_LOG_BASE + " to " + logBase);
            changeLogBase(logBase);
            this.stupidBackoffLogAlpha = (float) (Math.log(stupidBackoffAlpha) / Math.log(logBase));
        } else {
            this.stupidBackoffLogAlpha = (float) (Math.log(stupidBackoffAlpha) / Math.log(DEFAULT_LOG_BASE));
        }

        this.logBase = logBase;

        if (unigramWeight != DEFAULT_UNIGRAM_WEIGHT) {
            Log.debug("Applying unigram smoothing with unigram weight: " + unigramWeight);
            applyUnigramSmoothing(unigramWeight);
        }

        if (useStupidBackoff) {
            Log.debug("Lm will use stupid back off with alpha value: " + stupidBackoffAlpha);
        }
        if (ngramKeyFileDir != null) {
            if (!ngramKeyFileDir.exists())
                Log.warn("Ngram id file directory %s does not exist. Continue without loading.", ngramKeyFileDir);
            else {
                Log.info("Loading actual n-gram id data.");
                this.ngramIds = new NgramIds(this.order, ngramKeyFileDir, mphfs);
            }
        }
    }

    /**
     * Returns human readable information about the model.
     */
    public String info() {
        StringBuilder sb = new StringBuilder();
        sb.append(format(ENGLISH, "Order : %d%n", order));

        for (int i = 1; i < ngramData.length; i++) {
            GramDataArray gramDataArray = ngramData[i];
            if (i == 1) {
                sb.append(format(ENGLISH, "1 Grams: Count= %d%n", unigramProbs.length));
                continue;
            }
            sb.append(format(ENGLISH, "%d Grams: Count= %d  Fingerprint Bits= %d  Probabilty Bits= %d  Back-off bits= %d%n",
                    i,
                    gramDataArray.count,
                    gramDataArray.fpSize * 8,
                    gramDataArray.probSize * 8,
                    gramDataArray.backoffSize * 8));
        }
        sb.append(format(ENGLISH, "Log Base              : %.2f%n", logBase));
        sb.append(format(ENGLISH, "Unigram Weight        : %.2f%n", unigramWeight));
        sb.append(format(ENGLISH, "Using Stupid Back-off?: %s%n", useStupidBackoff ? "Yes" : "No"));
        if (useStupidBackoff)
            sb.append(format(ENGLISH, "Stupid Back-off Alpha Value   : %.2f%n", stupidBackoffAlpha));
        sb.append(format(ENGLISH, "Unknown Back-off N-gram penalty: %.2f%n", unknownBackoffPenalty));
        return sb.toString();
    }

    public static enum MphfType {
        SMALL, LARGE
    }

    public double getStupidBackoffLogAlpha() {
        return stupidBackoffLogAlpha;
    }

    private SmoothLm(DataInputStream dis) throws IOException {

        this.version = dis.readInt();
        int typeInt = dis.readInt();
        if (typeInt == 0)
            type = MphfType.SMALL;
        else
            type = MphfType.LARGE;

        this.logBase = (float) dis.readDouble();
        this.order = dis.readInt();

        counts = new int[order + 1];
        for (int i = 1; i <= order; i++) {
            counts[i] = dis.readInt();
        }

        // probability lookups
        probabilityLookups = new FloatLookup[order + 1];
        for (int i = 1; i <= order; i++) {
            // because we serialize values as doubles
            probabilityLookups[i] = FloatLookup.getLookupFromDouble(dis);
        }
        // backoff lookups
        backoffLookups = new FloatLookup[order + 1];
        for (int i = 1; i < order; i++) {
            // because we serialize values as doubles
            backoffLookups[i] = FloatLookup.getLookupFromDouble(dis);
        }

        //load fingerprint, probability and backoff data.
        ngramData = new GramDataArray[order + 1];
        for (int i = 1; i <= order; i++) {
            ngramData[i] = new GramDataArray(dis);
        }

        // we take the unigram probability data out to get rid of rank look-ups for speed.
        int unigramCount = ngramData[1].count;
        unigramProbs = new float[unigramCount];
        unigramBackoffs = order > 1 ? new float[unigramCount] : new float[0];
        for (int i = 0; i < unigramCount; i++) {
            final int probability = ngramData[1].getProbabilityRank(i);
            unigramProbs[i] = probabilityLookups[1].get(probability);
            if (order > 1) {
                final int backoff = ngramData[1].getBackoffRank(i);
                unigramBackoffs[i] = backoffLookups[1].get(backoff);
            }
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

    public int getVersion() {
        return version;
    }

    @Override
    public float getUnigramProbability(int id) {
        return getProbability(id);
    }

    @Override
    public int getOrder() {
        return order;
    }

    @Override
    public LmVocabulary getVocabulary() {
        return vocabulary;
    }

    /**
     * returns an LookupCache instance with 2^16 slots.
     * It is generally faster to use the cache's check(int...) method for getting probability of a word sequence.
     */
    public LookupCache getCache() {
        return new LookupCache(this);
    }

    /**
     * returns an LookupCache instance with 2^[bits] slots.
     * It is generally faster to use the cache's check(int...) method for getting probability of a word sequence.
     */
    public LookupCache getCache(int bits) {
        return new LookupCache(this, bits);
    }

    /**
     * Gets the count of a particular gram size
     *
     * @param n gram order
     * @return how many items exist for this particular order n-gram
     */
    public int getGramCount(int n) {
        return ngramData[n].count;
    }

    /**
     * @return true if ngram exists in the lm.
     * if actual key data is loaded during the construction of the compressed lm, the value returned
     * by this function cannot be wrong. If not, the return value may be a false positive.
     */
    @Override
    public boolean ngramExists(int... wordIndexes) {
        if (wordIndexes.length < 1 || wordIndexes.length > order - 1) {
            throw new IllegalArgumentException("Amount of tokens must be between 1 and " +
                    order + " But it is " + wordIndexes.length);
        }
        final int order = wordIndexes.length;
        if (order == 1) {
            return wordIndexes[0] >= 0 && wordIndexes[0] < unigramProbs.length;
        }
        int quickHash = MultiLevelMphf.hash(wordIndexes, -1);
        int index = mphfs[order].get(wordIndexes, quickHash);
        if (ngramIds == null) {
            return ngramData[order].checkFingerPrint(quickHash, index);
        }
        return ngramIds.exists(wordIndexes, index);
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
            return LogMath.LOG_ZERO;
        }
    }

    /**
     * Retrieves the dequantized log probability of an n-gram.
     *
     * @param w0 token-index 0.
     * @param w1 token-index 1.
     * @return dequantized log probability of the n-gram. if n-gram does not exist, it returns LOG_ZERO
     */
    public float getBigramProbabilityValue(int w0, int w1) {

        int quickHash = MultiLevelMphf.hash(w0, w1, -1);

        int index = mphfs[2].get(w0, w1, quickHash);

        if (ngramData[2].checkFingerPrint(quickHash, index))
            return probabilityLookups[2].get(ngramData[2].getProbabilityRank(index));
        else {
            return LogMath.LOG_ZERO_FLOAT;
        }
    }

    private boolean isFalsePositive(int... wordIndexes) {
        int length = wordIndexes.length;
        if (length < 2)
            return false;
        int quickHash = MultiLevelMphf.hash(wordIndexes, -1);
        int index = mphfs[length].get(wordIndexes, quickHash);
        return ngramData[length].checkFingerPrint(quickHash, index) // fingerprint matches
                && !ngramIds.exists(wordIndexes, index); // but not the exact keys.
    }

    /**
     * Retrieves the dequantized backoff value of an n-gram.
     *
     * @param wordIndexes word-index sequence. A word index is the unigram index value.
     * @return dequantized log back-off value of the n-gram.
     * if n-gram does not exist, it returns unknownBackoffPenalty value.
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
     * Retrieves the dequantized backoff value of an n-gram.
     *
     * @param w0 token-index 0.
     * @param w1 token-index 1.
     * @return dequantized log back-off value of the n-gram.
     * if n-gram does not exist, it returns unknownBackoffPenalty value.
     */
    public float getBigramBackoffValue(int w0, int w1) {
        if (useStupidBackoff)
            return stupidBackoffLogAlpha;
        final int quickHash = MultiLevelMphf.hash(w0, w1, -1);

        final int nGramIndex = mphfs[2].get(w0, w1, quickHash);

        if (ngramData[2].checkFingerPrint(quickHash, nGramIndex))
            return backoffLookups[2].get(ngramData[2].getBackoffRank(nGramIndex));
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
     * it returns LOG_ZERO. This mostly happens in the condition that words queried does not exist
     * in the vocabulary.
     * @throws IllegalArgumentException if wordIndexes sequence length is zero or more than n value.
     */
    double getProbabilityRecursive(int... wordIndexes) {

        if (wordIndexes.length == 0 || wordIndexes.length > order)
            throw new IllegalArgumentException(
                    "At least one or max Gram Count" + order + " tokens are required. But it is:" + wordIndexes.length);

        double result = 0;
        double probability = getProbabilityValue(wordIndexes);
        if (probability == LogMath.LOG_ZERO) { // if probability does not exist.
            if (wordIndexes.length == 1)
                return LogMath.LOG_ZERO;
            double backoffValue = useStupidBackoff ? stupidBackoffLogAlpha : getBackoffValue(head(wordIndexes));
            result = result + backoffValue + getProbabilityRecursive(tail(wordIndexes));
        } else {
            result = probability;
        }
        return result;
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
     * For Debugging purposes only.
     * Counts false positives generated for an ngram.
     *
     * @param wordIndexes word index array
     */
    public void countFalsePositives(int... wordIndexes) {
        if (wordIndexes.length == 0 || wordIndexes.length > order)
            throw new IllegalArgumentException(
                    "At least one or max Gram Count" + order + " tokens are required. But it is:" + wordIndexes.length);
        if (wordIndexes.length == 1)
            return;
        if (isFalsePositive(wordIndexes)) {
            this.falsePositiveCount++;
        }
        if (getProbability(wordIndexes) == LogMath.LOG_ZERO) {
            if (isFalsePositive(head(wordIndexes))) // check back-off false positive
                falsePositiveCount++;
            countFalsePositives(tail(wordIndexes));
        }
    }

    public boolean ngramIdsAvailable() {
        return ngramIds != null;
    }

    public int getFalsePositiveCount() {
        return falsePositiveCount;
    }

    /**
     * This is the non recursive log probability calculation. It is more complicated but faster.
     *
     * @param wordIndexes word index array
     * @return log probability.
     */
    public float getProbability(int... wordIndexes) {
        int n = wordIndexes.length;

        switch (n) {
            case 1:
                return unigramProbs[wordIndexes[0]];
            case 2:
                return getBigramProbability(wordIndexes[0], wordIndexes[1]);
            case 3:
                return getTriGramProbability(wordIndexes);
            default:
                break;
        }
        int begin = 0;
        float result = 0;
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
                    if (gram == 2) {  // we are already backed off to unigrams because no bigram found. So we return only P(N)+B(N-1)
                        return result + unigramProbs[wordIndexes[n - 1]] + unigramBackoffs[wordIndexes[begin]];
                    }
                    fingerPrint = MultiLevelMphf.hash(wordIndexes, begin, n - 1, -1);
                    nGramIndex = mphfs[gram - 1].get(wordIndexes, begin, n - 1, fingerPrint);
                    if (ngramData[gram - 1].checkFingerPrint(fingerPrint, nGramIndex)) { //if backoff available, we add it to result.
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

    public float getBigramProbability(int w0, int w1) {
        float prob = getBigramProbabilityValue(w0, w1);
        if (prob == LogMath.LOG_ZERO_FLOAT) {
            if (useStupidBackoff)
                return stupidBackoffLogAlpha + unigramProbs[w1];
            else
                return unigramBackoffs[w0] + unigramProbs[w1];
        } else {
            return prob;
        }
    }

    /**
     * @param w0 token-index 0.
     * @param w1 token-index 1.
     * @param w2 token-index 2.
     * @return log probability.
     */
    public float getTriGramProbability(int w0, int w1, int w2) {
        int fingerPrint = MultiLevelMphf.hash(w0, w1, w2, -1);
        return getTriGramProbability(w0, w1, w2, fingerPrint);
    }

    /**
     * @param w0 token-index 0.
     * @param w1 token-index 1.
     * @param w2 token-index 2.
     * @return log probability.
     */
    public float getTriGramProbability(int w0, int w1, int w2, int fingerPrint) {
        int nGramIndex = mphfs[3].get(w0, w1, w2, fingerPrint);
        if (!ngramData[3].checkFingerPrint(fingerPrint, nGramIndex)) { //3 gram does not exist.
            return getBigramBackoffValue(w0, w1) + getBigramProbability(w1, w2);
        } else return probabilityLookups[3].get(ngramData[3].getProbabilityRank(nGramIndex));
    }

    /**
     * @return log probability.
     */
    public float getTriGramProbability(int... w) {
        int fingerPrint = MultiLevelMphf.hash(w, -1);
        int nGramIndex = mphfs[3].get(w, fingerPrint);
        if (!ngramData[3].checkFingerPrint(fingerPrint, nGramIndex)) { //3 gram does not exist.
            return getBigramBackoffValue(w[0], w[1]) + getBigramProbability(w[1], w[2]);
        } else return probabilityLookups[3].get(ngramData[3].getProbabilityRank(nGramIndex));
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
        if (n == 2) return getProbabilityValue(wordIndexes) == LogMath.LOG_ZERO ? 1 : 0;

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
        if (probability == LogMath.LOG_ZERO) { // if probability does not exist.
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
        return format(ENGLISH, "%.3f", d);
    }

    /**
     * Changes the log base. Generally probabilities in language models are log10 based. But some applications uses their
     * own log base. Such as Sphinx uses 1.0003 as their base, some other uses e.
     *
     * @param newBase new logBase
     */
    private void changeLogBase(double newBase) {
        FloatLookup.changeBase(unigramProbs, logBase, newBase);
        FloatLookup.changeBase(unigramBackoffs, logBase, newBase);
        for (int i = 2; i < probabilityLookups.length; i++) {
            probabilityLookups[i].changeBase(logBase, newBase);
            if (i < probabilityLookups.length - 1)
                backoffLookups[i].changeBase(logBase, newBase);
        }
        this.logBase = (float) newBase;
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
        // apply uni-gram weight. This applies smoothing to uni-grams. As lowering high probabilities and
        // adding gain to small probabilities.
        // uw = uni-gram weight  , uniformProb = 1/#unigram
        // so in linear domain, we apply this to all probability values as: p(w1)*uw + uniformProb * (1-uw) to
        // maintain the probability total to one while smoothing the values.
        // this converts to log(p(w1)*uw + uniformProb*(1-uw)) which is calculated with log probabilities
        // a = log(p(w1)) + log(uw) and b = -log(#unigram)+log(1-uw) applying logSum(a,b)
        // approach is taken from Sphinx-4
        for (int i = 0; i < unigramProbs.length; i++) {
            double p1 = unigramProbs[i] + logUnigramWeigth;
            double p2 = logUniformUnigramProbability + inverseLogUnigramWeigth;
            unigramProbs[i] = (float) LogMath.logSum(p1, p2);
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

    /**
     * This class contains actual n-gram key information in flat arrays.
     * This is only useful for debugging purposes to check the false-positive ration of the compressed LM
     * It has a limitation that for an order, key_count*order value must be lower than Integer.MAX_VALUE.
     * Otherwise it does not load the information.
     */
    static class NgramIds {
        // flat arrays carrying actual ngram information.
        int[][] ids;

        NgramIds(int order, File idFileDir, Mphf[] mphfs) throws IOException {
            ids = new int[order + 1][];
            for (int i = 2; i <= order; i++) {
                // TODO: check consistency of the file names.
                File idFile = new File(idFileDir, i + ".gram");
                Log.info("Loading from: " + idFile);
                if (!idFile.exists()) {
                    Log.warn("Cannot find n-gram id file " + idFile.getAbsolutePath());
                    continue;
                }
                try (DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(idFile)))) {
                    dis.readInt(); // skip order.
                    int keyAmount = dis.readInt();
                    if ((long) keyAmount * i > Integer.MAX_VALUE) {
                        Log.warn("Cannot load key file as flat array. Too much index values.");
                        continue;
                    }
                    ids[i] = new int[keyAmount * i];
                    int[] data = new int[i];
                    int k = 0;
                    while (k < keyAmount) {
                        // load the k.th gram ids and calculate mphf for that.
                        for (int j = 0; j < i; ++j) {
                            data[j] = dis.readInt();
                        }
                        int mphfIndex = mphfs[i].get(data);
                        // put data to flat array with mphfIndex val.
                        System.arraycopy(data, 0, ids[i], mphfIndex * i, i);
                        k++;
                    }
                }
            }
        }

        boolean exists(int[] indexes, int mphfIndex) {
            int order = indexes.length;
            int index = mphfIndex * order;
            for (int i = 0; i < order; i++) {
                if (ids[order][index + i] != indexes[i])
                    return false;
            }
            return true;
        }
    }
}