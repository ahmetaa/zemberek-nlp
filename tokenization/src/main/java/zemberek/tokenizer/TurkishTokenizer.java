package zemberek.tokenizer;


import zemberek.core.collections.FixedBitVector;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Extracts tokens from sentences.
 * TODO: finish this.
 */
class TurkishTokenizer implements Tokenizer {

    public TurkishTokenizer() {
    }

    static class Token {

        String content;
        int startIndex;
        int type;

        public Token(String content, int startIndex, int type) {
            this.content = content;
            this.startIndex = startIndex;
            this.type = type;
        }
    }

    static Locale tr = new Locale("tr");

    private static final String TurkishLowerCase = "abcçdefgğhıijklmnoöprsştuüvyzxwq";

    private static final String TurkishUpperCase = TurkishLowerCase.toUpperCase(tr);
    private static final String boundaryDesicionChars = "'+-./:@&";

    private static final String singleTokenChars = "!\"#$%()*+,-./:;<=>?@[\\]^_{|}~¡¢£¤¥¦§¨©ª«¬®¯" +
            "°±²³´µ¶·¸¹º»¼½¾¿";

    private static FixedBitVector singleTokenLookup = generateBitLookup(singleTokenChars);

    private static FixedBitVector generateBitLookup(String characters) {

        int max = 0;
        for (char c : characters.toCharArray()) {
            if (c > max) {
                max = c;
            }
        }

        FixedBitVector result = new FixedBitVector(max+1);
        for (char c : characters.toCharArray()) {
            result.set(c);
        }

        return result;
    }

    public List<Token> tokenize(String sentence) {
        List<Token> tokens = new ArrayList<>();

        int tokenBegin = 0;

        char previous = 0;

        StringBuilder tokenContent = new StringBuilder();
        for (int j = 0; j < sentence.length(); j++) {
            // skip if char cannot be a boundary char.
            char chr = sentence.charAt(j);
            //if (chr==' ' || chr=='\t' || chr=='\n' || chr=='\r') {
            if (Character.isWhitespace(chr)) {
                if (tokenContent.length() > 0) {
                    tokens.add(new Token(tokenContent.toString(), tokenBegin, 0));
                    tokenContent = new StringBuilder();
                }
                tokenBegin = j;
                continue;
            }
            //if (chr < 255 && singleTokenLookup.get(chr)) {
            if (chr < singleTokenLookup.length && singleTokenLookup.get(chr)) {
                // add previous toke if available.
                if (tokenContent.length() > 0) {
                    tokens.add(new Token(tokenContent.toString(), tokenBegin, 0));
                    tokenContent = new StringBuilder();
                }
                tokenBegin = j;
                // add single symbol token.
                tokens.add(new Token(String.valueOf(chr), tokenBegin, 0));
                continue;
            }
            tokenContent.append(chr);
        }
        // add remaining token.
        if (tokenContent.length() > 0) {
            tokens.add(new Token(tokenContent.toString(), tokenBegin, 0));
        }
        return tokens;
    }


    @Override
    public List<String> tokenStrings(String input) {
        List<Token> tokens = tokenize(input);
        List<String> stringTokens = new ArrayList<>(tokens.size());
        for (Token token : tokens) {
            stringTokens.add(token.content);
        }
        return stringTokens;
    }


}
