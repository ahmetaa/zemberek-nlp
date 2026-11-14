package zemberek.core.turkish;

import java.text.Collator;
import java.util.Comparator;
import java.util.Locale;

public class Turkish {

  public static final Locale LOCALE = Locale.of("tr");
  public static final TurkishAlphabet Alphabet = TurkishAlphabet.INSTANCE;
  public static final Collator COLLATOR = Collator.getInstance(LOCALE);
  public static final Comparator<String> STRING_COMPARATOR_ASC = COLLATOR::compare;

  public static char toLower(char c) {
    return switch (c) {
      case 'I' -> 'ı';
      case 'İ' -> 'i';
      default -> Character.toLowerCase(c);
    };
  }

  public static char toUpper(char c) {
    return switch (c) {
      case 'i' -> 'İ';
      case 'ı' -> 'I';
      default -> Character.toUpperCase(c);
    };
  }

  public static String capitalize(String word) {
    if (word.isEmpty()) {
      return word;
    }
    return word.substring(0, 1).toUpperCase(LOCALE) + word.substring(1).toLowerCase(LOCALE);
  }
}
