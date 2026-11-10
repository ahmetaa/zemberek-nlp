package zemberek.tokenization;

import java.util.ArrayList;
import java.util.List;
import zemberek.tokenization.Token.Type;

/**
 * Reconstructs natural Turkish text spacing from a sequence of tokens or token strings produced by
 * {@link TurkishTokenizer}.
 */
public class TurkishDetokenizer {

  public static final TurkishDetokenizer DEFAULT = new TurkishDetokenizer();

  /**
   * Joins a list of token strings using standard Turkish punctuation, bracket, quote, currency, and
   * apostrophe attachment rules.
   */
  public String detokenize(List<String> tokens) {
    if (tokens == null || tokens.isEmpty()) {
      return "";
    }

    int n = tokens.size();
    StringBuilder sb = new StringBuilder(n * 6);
    String prev = null;
    boolean inDoubleQuote = false;
    boolean inSingleQuote = false;
    boolean prevAttachRight = false;
    boolean prevClosedQuoteOrBracket = false;
    boolean prevWasSuffixApostrophe = false;

    for (int i = 0; i < n; i++) {
      String curr = tokens.get(i);
      if (curr == null || curr.isEmpty()) {
        continue;
      }

      if (isLineBreak(curr)) {
        sb.append(curr);
        prev = curr;
        prevAttachRight = true;
        prevClosedQuoteOrBracket = false;
        prevWasSuffixApostrophe = false;
        continue;
      }

      boolean attachToPrev = (prev == null) || prevAttachRight || prevWasSuffixApostrophe;
      boolean nextAttachRight = false;
      boolean nextClosedQuoteOrBracket = false;
      boolean nextWasSuffixApostrophe = false;

      if ("\"".equals(curr)) {
        if (inDoubleQuote) {
          attachToPrev = true;
          inDoubleQuote = false;
          nextClosedQuoteOrBracket = true;
        } else {
          inDoubleQuote = true;
          nextAttachRight = true;
        }
      } else if (isApostropheChar(curr)
          && prevClosedQuoteOrBracket
          && hasFollowingLetterToken(tokens, i + 1)) {
        // Case/possessive suffix after a closing quote or bracket: "Soner"'e, (2020)'de
        attachToPrev = true;
        nextWasSuffixApostrophe = true;
      } else if ("'".equals(curr)) {
        if (inSingleQuote) {
          attachToPrev = true;
          inSingleQuote = false;
          nextClosedQuoteOrBracket = true;
        } else {
          inSingleQuote = true;
          nextAttachRight = true;
        }
      } else if ("‘".equals(curr)) {
        nextAttachRight = true;
      } else if ("’".equals(curr)) {
        attachToPrev = true;
        nextClosedQuoteOrBracket = true;
      } else if (isOpeningBracketOrQuote(curr)) {
        nextAttachRight = true;
      } else if (isClosingBracketOrQuote(curr)) {
        attachToPrev = true;
        nextClosedQuoteOrBracket = true;
      } else if (isLeftAttachingPunctuation(curr)) {
        attachToPrev = true;
      } else if ("/".equals(curr)) {
        attachToPrev = true;
        nextAttachRight = true;
      } else if (isCurrencySymbol(curr)) {
        String next = peekNextNonEmpty(tokens, i + 1);
        if (prev != null && startsWithDigitOrPercent(prev)) {
          attachToPrev = true;
        } else if (next != null && startsWithDigit(next)) {
          nextAttachRight = true;
        }
      } else if ("%".equals(curr)) {
        String next = peekNextNonEmpty(tokens, i + 1);
        if (next != null && startsWithDigit(next)) {
          nextAttachRight = true;
        }
      } else if ("°".equals(prev) && isTemperatureScale(curr)) {
        attachToPrev = true;
      }

      if (!attachToPrev) {
        sb.append(' ');
      }
      sb.append(curr);

      prev = curr;
      prevAttachRight = nextAttachRight;
      prevClosedQuoteOrBracket = nextClosedQuoteOrBracket;
      prevWasSuffixApostrophe = nextWasSuffixApostrophe;
    }

    return sb.toString();
  }

  /**
   * Joins a list of {@link Token} objects. If explicit {@link Type#SpaceTab} tokens are present
   * (such as output from {@link TurkishTokenizer#ALL}), exact token text is concatenated directly;
   * otherwise Turkish detokenization spacing rules are applied while preserving {@link Type#NewLine}
   * tokens.
   */
  public String detokenizeTokens(List<Token> tokens) {
    if (tokens == null || tokens.isEmpty()) {
      return "";
    }
    boolean hasExplicitSpaces = false;
    for (Token token : tokens) {
      if (token != null && token.getType() == Type.SpaceTab) {
        hasExplicitSpaces = true;
        break;
      }
    }
    if (hasExplicitSpaces) {
      StringBuilder sb = new StringBuilder(tokens.size() * 4);
      for (Token token : tokens) {
        if (token != null && token.getText() != null) {
          sb.append(token.getText());
        }
      }
      return sb.toString();
    }
    List<String> strings = new ArrayList<>(tokens.size());
    for (Token token : tokens) {
      if (token != null) {
        strings.add(token.getText());
      }
    }
    return detokenize(strings);
  }

  private static boolean isLineBreak(String s) {
    return "\n".equals(s) || "\r".equals(s) || "\r\n".equals(s);
  }

  private static boolean isApostropheChar(String s) {
    return "'".equals(s) || "’".equals(s);
  }

  private static boolean isOpeningBracketOrQuote(String s) {
    return "(".equals(s)
        || "[".equals(s)
        || "{".equals(s)
        || "“".equals(s)
        || "«".equals(s);
  }

  private static boolean isClosingBracketOrQuote(String s) {
    return ")".equals(s)
        || "]".equals(s)
        || "}".equals(s)
        || "”".equals(s)
        || "»".equals(s);
  }

  private static boolean isLeftAttachingPunctuation(String s) {
    return ",".equals(s)
        || ".".equals(s)
        || "!".equals(s)
        || "?".equals(s)
        || ":".equals(s)
        || ";".equals(s)
        || "...".equals(s)
        || "…".equals(s)
        || "°".equals(s)
        || "®".equals(s)
        || "™".equals(s)
        || "℠".equals(s);
  }

  private static boolean isCurrencySymbol(String s) {
    return "₺".equals(s)
        || "$".equals(s)
        || "€".equals(s)
        || "£".equals(s)
        || "¥".equals(s);
  }

  private static boolean isTemperatureScale(String s) {
    return "C".equals(s)
        || "F".equals(s)
        || "C.".equals(s)
        || "F.".equals(s)
        || s.startsWith("C'")
        || s.startsWith("C’")
        || s.startsWith("F'")
        || s.startsWith("F’");
  }

  private static boolean startsWithDigit(String s) {
    return !s.isEmpty() && Character.isDigit(s.charAt(0));
  }

  private static boolean startsWithDigitOrPercent(String s) {
    if (s.isEmpty()) {
      return false;
    }
    char c = s.charAt(0);
    return Character.isDigit(c) || (c == '%' && s.length() > 1 && Character.isDigit(s.charAt(1)));
  }

  private static String peekNextNonEmpty(List<String> tokens, int startIndex) {
    for (int i = startIndex; i < tokens.size(); i++) {
      String t = tokens.get(i);
      if (t != null && !t.isEmpty()) {
        return t;
      }
    }
    return null;
  }

  private static boolean hasFollowingLetterToken(List<String> tokens, int startIndex) {
    String next = peekNextNonEmpty(tokens, startIndex);
    return next != null && !isLineBreak(next) && Character.isLetter(next.charAt(0));
  }
}
