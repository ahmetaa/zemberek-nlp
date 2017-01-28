package zemberek.tokenizer;


import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Extracts tokens from sentences.
 * TODO: finish this.
 */
public class TurkishTokenizer {

    static class Token {
        String content;
        int startIndex;
        int type;
    }


    static Locale tr = new Locale("tr");

    private static final String TurkishLowerCase = "abcçdefgğhıijklmnoöprsştuüvyzxwq";
    private static final String TurkishUpperCase = TurkishLowerCase.toUpperCase(tr);

    private static final String boundaryDesicionChars = "'+-./:@&";
    private static final String boundaryChars = "!\"#$%'()*+,-./:;<=>?@[\\]^_{|}~¡¢£¤¥¦§¨©ª«¬®¯" +
            "°±²³´µ¶·¸¹º»¼½¾¿";


    public List<String> tokenize(String sentence) {
        List<String> sentences = new ArrayList<>();
        int begin = 0;
        for (int j = 0; j < sentence.length(); j++) {
            // skip if char cannot be a boundary char.
            char chr = sentence.charAt(j);
        }
        return sentences;
    }


}
