package zemberek.examples.tokenization;

import com.google.common.base.Joiner;
import java.util.Iterator;
import org.antlr.v4.runtime.Token;
import zemberek.core.logging.Log;
import zemberek.tokenization.TurkishTokenizer;

public class TurkishTokenizationExample {

  static TurkishTokenizer tokenizer = TurkishTokenizer.DEFAULT;

  public static void tokenIterator() {
    Log.info("Low level tokenization iterator using Ant-lr Lexer.");
    String input = "İstanbul'a, merhaba!";
    Log.info("Input = " + input);
    Iterator<Token> tokenIterator = tokenizer.getTokenIterator(input);
    while (tokenIterator.hasNext()) {
      Token token = tokenIterator.next();
      Log.info("Token= " + token.getText() + " Type=" + token.getType());
    }
  }

  public static void simpleTokenization() {
    Log.info("Simple tokenization returns a list of token strings.");
    TurkishTokenizer tokenizer = TurkishTokenizer.DEFAULT;
    String input = "İstanbul'a, merhaba!";
    Log.info("Input = " + input);
    Log.info("Tokenization list = " +
        Joiner.on("|").join(tokenizer.tokenizeToStrings("İstanbul'a, merhaba!")));
  }

  public static void main(String[] args) {
    tokenIterator();
    Log.info();
    simpleTokenization();
  }
}
