package zemberek.examples.tokenization;

import com.google.common.base.Joiner;
import java.util.Iterator;
import java.util.List;
import zemberek.core.logging.Log;
import zemberek.tokenization.TurkishTokenizer;
import zemberek.tokenization.Token;

public class TurkishTokenizationExample {

  static TurkishTokenizer tokenizer = TurkishTokenizer.DEFAULT;

  public static void tokenIterator() {
    System.out.println("Low level tokenization iterator using Ant-lr Lexer.");
    String input = "İstanbul'a, merhaba!";
    System.out.println("Input = " + input);
    Iterator<Token> tokenIterator = tokenizer.getTokenIterator(input);
    while (tokenIterator.hasNext()) {
      Token token = tokenIterator.next();
      System.out.println(token);
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

  public static void customTokenizer() {
    TurkishTokenizer tokenizer = TurkishTokenizer
        .builder()
        .ignoreTypes(Token.Type.Punctuation, Token.Type.NewLine, Token.Type.SpaceTab)
        .build();
    List<Token> tokens = tokenizer.tokenize("Saat, 12:00.");
    for (Token token : tokens) {
      System.out.println(token);
    }
  }

  public static void main(String[] args) {
    System.out.println("Token Iterator Example -----");
    tokenIterator();
    System.out.println("Default Tokenization Example -----");
    simpleTokenization();
    System.out.println("Custom Tokenization Example -----");
    customTokenizer();
  }
}
