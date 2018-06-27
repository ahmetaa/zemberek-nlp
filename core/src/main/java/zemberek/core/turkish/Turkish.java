package zemberek.core.turkish;

import java.text.Collator;
import java.util.Comparator;
import java.util.Locale;


public class Turkish {

  public static final Locale LOCALE = new Locale("tr");
  public static final TurkishAlphabet Alphabet = TurkishAlphabet.INSTANCE;
  public static final Collator COLLATOR = Collator.getInstance(LOCALE);
  public static final Comparator<String> STRING_COMPARATOR_ASC = new TurkishStringComparator();

  public static String capitalize(String word) {
    if (word.length() == 0) {
      return word;
    }
    return word.substring(0, 1).toUpperCase(LOCALE) + word.substring(1).toLowerCase(LOCALE);
  }

  private static class TurkishStringComparator implements Comparator<String> {

    public int compare(String o1, String o2) {
      return COLLATOR.compare(o1, o2);
    }
  }
}
