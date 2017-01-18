package zemberek.langid.model;

/**
 * Character NGram model interface
 */
public interface CharNgramLanguageModel {
    /**
     * @param gram calculates log probability of a gram from this model.
     * @return natural log probability value.
     */
    double gramProbability(String gram);

    /**
     * Order of the model (usually 2,3,.)
     *
     * @return order
     */
    int getOrder();

    /**
     * model identifier String
     *
     * @return id.
     */
    String getId();
}
