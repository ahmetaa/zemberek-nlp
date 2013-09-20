package zemberek.lm;

/**
 * Represents an N-gram language model.
 */
public interface NgramLanguageModel {

    /**
     * Returns Log uni-gram probability value. id must be in vocabulary limits.
     *
     * @param id word id
     * @return log probability
     */
    double getUnigramProbability(int id);

    /**
     * Returns Log N-Gram probability.
     * If this is a back-off model, it makes with necessary back-off calculations when necessary
     *
     * @param ids word ids.
     * @return log probability
     */
    double getProbability(int... ids);

    /**
     * Order of language model
     *
     * @return order value.
     */
    int getOrder();

    /**
     * @return log base of the language mo0del 2,10,e, etc.
     */
    double getLogBase();

    /**
     * Gets the count of a particular gram size
     *
     * @param order gram order
     * @return how many items exist for this particular order n-gram
     */
    int getGramCount(int order);

    /**
     * It generates a String explaining the probability calculation for a given ngram.
     *
     * @param tokenIds n-gram index array
     * @return explanation String.
     */
    public String explain(int... tokenIds);

    /**
     * Vocabulary of this model.
     *
     * @return Vocabulary of this model.
     */
    LmVocabulary getVocabulary();
}
