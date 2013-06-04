package zemberek.morphology.apps;

import zemberek.core.structure.TurkishAlphabet;

import java.util.Locale;

public abstract class BaseParser {

    public static final TurkishAlphabet alphabet = new TurkishAlphabet();

    static final Locale TR = new Locale("tr");

    public String normalize(String input) {
        StringBuilder sb = new StringBuilder(input.length());
        input = input.toLowerCase(TR);
        for (char c : input.toCharArray()) {
            if (alphabet.isValid(c))
                sb.append(c);
        }
        return sb.toString();
    }

}
