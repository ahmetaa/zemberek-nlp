package zemberek.tokenizer;


import com.google.common.io.Resources;
import zemberek.core.collections.FixedBitVector;
import zemberek.core.collections.FloatValueMap;
import zemberek.core.io.IOUtil;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Extracts tokens from sentences.
 * TODO: finish this.
 */
class TurkishTokenizer implements Tokenizer {

    private FloatValueMap<String> weights = new FloatValueMap<>();

    public static TurkishTokenizer fromInternalModel() throws IOException {
        try (DataInputStream dis = IOUtil.getDataInputStream(
                Resources.getResource("tokenizer/sentence-boundary-model.bin").openStream())) {
            return load(dis);
        }
    }

    private static TurkishTokenizer load(DataInputStream dis) throws IOException {
        int size = dis.readInt();
        FloatValueMap<String> features = new FloatValueMap<>((int) (size * 1.5));
        for (int i = 0; i < size; i++) {
            features.set(dis.readUTF(), dis.readFloat());
        }
        return new TurkishTokenizer(features);
    }

    private TurkishTokenizer(FloatValueMap<String> weights) {
        this.weights = weights;
    }

    static class Span {

        final int start;
        final int end;

        public Span(int start, int end) {
            this.start = start;
            this.end = end;
        }
    }

    private static Locale tr = new Locale("tr");

    private static final String TurkishLowerCase = "abcçdefgğhıijklmnoöprsştuüvyzxwq";
    private static final String TurkishUpperCase = TurkishLowerCase.toUpperCase(tr);

    private static final String boundaryDesicionChars = "'+-./:@&";

    private static final String singleTokenChars = "!\"#$%()*+,-./:;<=>?@[\\]^_{|}~¡¢£¤¥¦§¨©ª«¬®¯" +
            "°±²³´µ¶·¸¹º»¼½¾¿";

    private static FixedBitVector singleTokenLookup = generateBitLookup(singleTokenChars);
    private static FixedBitVector boundaryDesicionLookup = generateBitLookup(boundaryDesicionChars);

    private static FixedBitVector generateBitLookup(String characters) {

        int max = 0;
        for (char c : characters.toCharArray()) {
            if (c > max) {
                max = c;
            }
        }

        FixedBitVector result = new FixedBitVector(max + 1);
        for (char c : characters.toCharArray()) {
            result.set(c);
        }

        return result;
    }

    private List<Span> tokenizeSpan(String sentence) {
        List<Span> tokens = new ArrayList<>();

        int tokenBegin = 0;
        char previous = 0;

        for (int j = 0; j < sentence.length(); j++) {
            // skip if char cannot be a boundary char.
            char chr = sentence.charAt(j);
            //if (chr==' ' || chr=='\t' || chr=='\n' || chr=='\r') {
            if (Character.isWhitespace(chr)) {
                if (tokenBegin < j) {
                    tokens.add(new Span(tokenBegin, j));
                }
                tokenBegin = j;
                continue;
            }
            if (chr < singleTokenLookup.length && singleTokenLookup.get(chr)) {
                // add previous toke if available.
                if (tokenBegin < j) {
                    tokens.add(new Span(tokenBegin, j));
                }
                // add single symbol token.
                tokens.add(new Span(j, j + 1));
                tokenBegin = j + 1;
                continue;
            }
            if (chr < boundaryDesicionLookup.length && boundaryDesicionLookup.get(chr)) {
                // TODO: make it work.
                if (weights.get("foo") > 0) {
                    if (tokenBegin < j) {
                        tokens.add(new Span(tokenBegin, j));
                    }
                    tokenBegin = j;
                }
            }
        }
        // add remaining token.
        if (tokenBegin < sentence.length()) {
            tokens.add(new Span(tokenBegin, sentence.length()));
        }
        return tokens;
    }

    private List<String> tokenize(String sentence) {
        List<String> tokens = new ArrayList<>();
        List<Span> spans = tokenizeSpan(sentence);
        for (Span span : spans) {
            tokens.add(sentence.substring(span.start, span.end));
        }
        return tokens;
    }


    @Override
    public List<String> tokenStrings(String input) {
        return tokenize(input);
    }


}
