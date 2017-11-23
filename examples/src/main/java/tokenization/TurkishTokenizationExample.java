package tokenization;

import com.google.common.base.Joiner;
import java.util.Iterator;
import org.antlr.v4.runtime.Token;
import zemberek.tokenization.TurkishTokenizer;

public class TurkishTokenizationExample {

  static TurkishTokenizer tokenizer = TurkishTokenizer.DEFAULT;

  public static void tokenIterator() {
    System.out.println("Low level tokenization iterator using Ant-lr Lexer.");
    String input = "İstanbul'a, merhaba!";
    System.out.println("Input = " + input);
    Iterator<Token> tokenIterator = tokenizer.getTokenIterator(input);
    while (tokenIterator.hasNext()) {
      Token token = tokenIterator.next();
      System.out.println("Token= " + token.getText() + " Type=" + token.getType());
    }
  }

  public static void simpleTokenization() {
    System.out.println("Simple tokenization returns a list of token strings.");
    TurkishTokenizer tokenizer = TurkishTokenizer.DEFAULT;
    String input = "İstanbul'a, merhaba!";
    System.out.println("Input = " + input);
    System.out.println("Tokenization list = " +
        Joiner.on("|").join(tokenizer.tokenizeToStrings("İstanbul'a, merhaba!")));
  }

  public static void main(String[] args) {
    tokenIterator();
    System.out.println();
    simpleTokenization();
  }
}
