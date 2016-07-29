package zemberek.core.text.distance;

import zemberek.core.text.TokenSequence;

/**
 * This class is used for comparing single tokens.
 */
public class WordDistance implements StringDistance {

    @Override
    public int sourceSize(TokenSequence sourceSequence) {
        return sourceSequence.size();
    }

    @Override
    public double distance(String token1, String token2) {
        if (token1.equals(token2))
            return 0;
        else
            return 1;
    }
}
