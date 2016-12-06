package zemberek.lm;

import zemberek.core.hash.MultiLevelMphf;

import java.util.Arrays;

/**
 * Base class for language models.
 * Contains common Language model parameters, cache etc.
 */
public abstract class BaseLanguageModel {

    protected int order;
    protected LmVocabulary vocabulary;

    protected BaseLanguageModel(int order, LmVocabulary vocabulary) {
        this.order = order;
        this.vocabulary = vocabulary;
    }

    protected BaseLanguageModel() {
    }

    /**
     * This is a simple cache that may be useful if ngram queries exhibit strong temporal locality.
     * Cache stores key values so it does not produce false positives by itself. However underlying lm may do.
     */
    public static class LookupCache {
        final float[] probabilities;
        final int[][] keys;
        final int modulo;
        public static final int DEFAULT_LOOKUP_CACHE_SIZE = 1 << 17;
        NgramLanguageModel model;
        int hit;
        int miss;

        /**
         * Generates a cache with 2^14 slots.
         */
        public LookupCache(NgramLanguageModel model) {
            this(model, DEFAULT_LOOKUP_CACHE_SIZE);
        }

        /**
         * Generates a cache where slotSize is the maximum power of two less than the size.
         */
        public LookupCache(NgramLanguageModel model, int size) {
            this.model = model;
            int k = size < DEFAULT_LOOKUP_CACHE_SIZE ? 2 : DEFAULT_LOOKUP_CACHE_SIZE;
            while (k < size) {
                k <<= 1;
            }
            modulo = k - 1;
            probabilities = new float[k];
            keys = new int[k][model.getOrder()];
        }

        /**
         * @return probability of the input data. If value is already cached, it returns immediately.
         * Otherwise it calculates the probability using the model reference inside and returns the value.
         * Overrides the previous keys and probability value.
         */
        public float get(int[] data) {
            int fastHash = MultiLevelMphf.hash(data,-1);
            int slotHash = fastHash & modulo;
            if (Arrays.equals(data, keys[slotHash])) {
                hit++;
                return probabilities[slotHash];
            } else {
                miss++;
                float probability = data.length == 3 ?
                        model.getTriGramProbability(data[0], data[1], data[2], fastHash) : model.getProbability(data);
                probabilities[slotHash] = probability;
                System.arraycopy(data, 0, keys[slotHash], 0, data.length);
                return probability;
            }
        }

        public int getHit() {
            return hit;
        }

        public int getMiss() {
            return miss;
        }
    }

    protected int[] head(int[] arr) {
        if (arr.length == 1)
            return new int[0];
        int[] head = new int[arr.length - 1];
        System.arraycopy(arr, 0, head, 0, arr.length - 1);
        return head;
    }

    protected int[] tail(int[] arr) {
        if (arr.length == 1)
            return new int[0];
        int[] head = new int[arr.length - 1];
        System.arraycopy(arr, 1, head, 0, arr.length - 1);
        return head;
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
}
