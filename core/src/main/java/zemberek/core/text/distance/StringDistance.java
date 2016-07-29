package zemberek.core.text.distance;


import zemberek.core.text.TokenSequence;

/**
 * calculates distance between two strings.
 */
public interface StringDistance {

    /**
     * Distance between two strings.
     *
     * @param source source string
     * @param target target string.
     * @return distance.
     */
    double distance(String source, String target);

    /**
     * This class is used when ratio of the distance to the input
     * tokenSequence is needed to be calculated.
     *
     * @param sourceSequence source sequence
     * @return size of the token sequence. This may be different than the
     * actual size, because during the distance calculation size of the
     * source and target may be changed. such as, if we calculate "hello
     * there" and "hell there", and we use a letter edit distance, the input
     * size is not the token size =2 but the letter count =11.
     */
    int sourceSize(TokenSequence sourceSequence);
}
