package zemberek.examples.normalization;

import zemberek.morphology.TurkishMorphology;
import zemberek.normalization.NumberTextConverter;

public class NumberConversion {

    public static void main(String[] args) {
        TurkishMorphology morphology = TurkishMorphology.createWithDefaults();
        NumberTextConverter numberTextConverter = new NumberTextConverter(morphology);
        String[] examples = {"yirmi 4 milyon, yüz 80 iki bin altmış 3 kişi geldi","yirmi 4 milyon yüz 80 iki bin altmış 3 ekmek aldım", "sekiz yüz elli 1 buçuk"};
        System.out.println("Convert textual numbers to numerically values");
        for (String example: examples) {
            System.out.println("Example: " + example);
            String s = numberTextConverter.replaceTextualNumberWithNumerically(example);
            System.out.println("Response: " + s);
        }
    }

}
