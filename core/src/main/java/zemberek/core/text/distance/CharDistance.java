package zemberek.core.text.distance;

import zemberek.core.text.TokenSequence;

public class CharDistance implements StringDistance {

    @Override
    public int sourceSize(TokenSequence sourceSequence) {
        return sourceSequence.asString().length();
    }

    @Override
    public double distance(String sourceString, String targetString) {
        int n;
        double p[]; //'previous' cost array, horizontally
        double d[]; // cost array, horizontally
        double _d[]; //placeholder to assist in swapping p and d

        n = sourceString.length();
        p = new double[n + 1];
        d = new double[n + 1];

        final int m = targetString.length();
        if (n == 0 || m == 0) {
            return Math.abs(n - m);
        }

        // indexes into strings s and t
        int i; // iterates through s
        int j; // iterates through t

        char t_j; // jth char

        double cost; // cost

        for (i = 0; i <= n; i++) {
            p[i] = i;
        }

        for (j = 1; j <= m; j++) {
            t_j = targetString.charAt(j - 1);
            d[0] = j;

            for (i = 1; i <= n; i++) {
                cost = sourceString.charAt(i - 1) == t_j ? 0 : 1;
                // minimum of cell to the left+1, to the top+1, diagonally left and up +cost
                d[i] = Math.min(Math.min(d[i - 1] + 1, p[i] + 1), p[i - 1] + cost);
            }

            // copy current distance counts to 'previous row' distance counts
            _d = p;
            p = d;
            d = _d;
        }

        // our last action in the above loop was to switch d and p, so p now
        // actually has the most recent cost counts
        return p[n];
    }
}



