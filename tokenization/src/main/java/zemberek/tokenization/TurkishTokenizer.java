package zemberek.tokenization;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.ConsoleErrorListener;
import org.antlr.v4.runtime.Lexer;
import zemberek.tokenization.Token.Type;
import zemberek.tokenization.antlr.TurkishLexer;


/**
 * A wrapper for Antlr generated lexer.
 */
public class TurkishTokenizer {

  public static final TurkishTokenizer ALL = builder().acceptAll().build();
  private static final int MAX_TOKEN_TYPE = TurkishLexer.VOCABULARY.getMaxTokenType();
  public static final TurkishTokenizer DEFAULT = builder()
      .acceptAll()
      .ignoreTypes(Token.Type.NewLine, Token.Type.SpaceTab)
      .build();

  private static final BaseErrorListener IGNORING_ERROR_LISTENER = new ConsoleErrorListener();

  private long acceptedTypeBits;

  private TurkishTokenizer(long acceptedTypeBits) {
    this.acceptedTypeBits = acceptedTypeBits;
  }

  public static Builder builder() {
    return new Builder();
  }

  private static TurkishLexer lexerInstance(CharStream inputStream) {
    TurkishLexer lexer = new TurkishLexer(inputStream);
    lexer.removeErrorListeners();
    lexer.addErrorListener(IGNORING_ERROR_LISTENER);
    return lexer;
  }

  public boolean isTypeAccepted(Token.Type i) {
    return !typeAccepted(i);
  }

  public boolean isTypeIgnored(Token.Type i) {
    return !typeAccepted(i);
  }

  private boolean typeAccepted(Token.Type i) {
    return (acceptedTypeBits & (1L << i.ordinal())) != 0;
  }

  private boolean typeIgnored(Token.Type i) {
    return (acceptedTypeBits & (1L << i.ordinal())) == 0;
  }


  public List<Token> tokenize(File file) throws IOException {
    return getAllTokens(lexerInstance(CharStreams.fromPath(file.toPath())));
  }

  public List<Token> tokenize(String input) {
    return getAllTokens(lexerInstance(CharStreams.fromString(input)));
  }

  public List<Token> tokenize(Reader reader) throws IOException {
    return getAllTokens(lexerInstance(CharStreams.fromReader(reader)));
  }

  public List<String> tokenizeToStrings(String input) {
    List<Token> tokens = tokenize(input);
    List<String> tokenStrings = new ArrayList<>(tokens.size());
    for (Token token : tokens) {
      tokenStrings.add(token.getText());
    }
    return tokenStrings;
  }

  public Iterator<Token> getTokenIterator(String input) {
    return new TokenIterator(this, lexerInstance(CharStreams.fromString(input)));
  }

  public Iterator<Token> getTokenIterator(File file) throws IOException {
    return new TokenIterator(this, lexerInstance(CharStreams.fromPath(file.toPath())));
  }

  public Iterator<Token> getTokenIterator(Reader reader) throws IOException {
    return new TokenIterator(this, lexerInstance(CharStreams.fromReader(reader)));
  }

  private List<Token> getAllTokens(Lexer lexer) {
    List<Token> tokens = new ArrayList<>();
    for (org.antlr.v4.runtime.Token token = lexer.nextToken();
        token.getType() != org.antlr.v4.runtime.Token.EOF;
        token = lexer.nextToken()) {
      Token.Type type = convertType(token);
      if (typeIgnored(type)) {
        continue;
      }
      tokens.add(convert(token));
    }
    return tokens;
  }

  public static Token convert(org.antlr.v4.runtime.Token token) {
    return new Token(token.getText(), convertType(token), token.getStartIndex(), token.getStopIndex());
  }

  public static Token convert(org.antlr.v4.runtime.Token token, Token.Type type) {
    return new Token(token.getText(), type, token.getStartIndex(), token.getStopIndex());
  }

  public static Token.Type convertType(org.antlr.v4.runtime.Token token) {
    switch (token.getType()) {
      case TurkishLexer.SpaceTab:
        return Type.SpaceTab;
      case TurkishLexer.Word:
        return Type.Word;
      case TurkishLexer.Number:
        return Type.Number;
      case TurkishLexer.Abbreviation:
        return Type.Abbreviation;
      case TurkishLexer.AbbreviationWithDots:
        return Type.AbbreviationWithDots;
      case TurkishLexer.Date:
        return Type.Date;
      case TurkishLexer.Email:
        return Type.Email;
      case TurkishLexer.Emoticon:
        return Type.Emoticon;
      case TurkishLexer.HashTag:
        return Type.HashTag;
      case TurkishLexer.Mention:
        return Type.Mention;
      case TurkishLexer.MetaTag:
        return Type.MetaTag;
      case TurkishLexer.NewLine:
        return Type.NewLine;
      case TurkishLexer.RomanNumeral:
        return Type.RomanNumeral;
      case TurkishLexer.PercentNumeral:
        return Type.PercentNumeral;
      case TurkishLexer.Time:
        return Type.Time;
      case TurkishLexer.Unknown:
        return Type.Unknown;
      case TurkishLexer.UnknownWord:
        return Type.UnknownWord;
      case TurkishLexer.URL:
        return Type.URL;
      case TurkishLexer.Punctuation:
        return Type.Punctuation;
      case TurkishLexer.WordAlphanumerical:
        return Type.WordAlphanumerical;
      case TurkishLexer.WordWithSymbol:
        return Type.WordWithSymbol;
      default:
        throw new IllegalStateException("Unidentified token type =" +
            TurkishLexer.VOCABULARY.getDisplayName(token.getType()));
    }
  }

  public static class Builder {

    private long acceptedTypeBits = ~0L;

    public Builder acceptTypes(Token.Type... types) {
      for (Token.Type i : types) {
        this.acceptedTypeBits |= (1L << i.ordinal());
      }
      return this;
    }

    public Builder ignoreTypes(Token.Type... types) {
      for (Token.Type i : types) {
        this.acceptedTypeBits &= ~(1L << i.ordinal());
      }
      return this;
    }

    public Builder ignoreAll() {
      this.acceptedTypeBits = 0L;
      return this;
    }

    public Builder acceptAll() {
      this.acceptedTypeBits = ~0L;
      return this;
    }

    public TurkishTokenizer build() {
      return new TurkishTokenizer(acceptedTypeBits);
    }
  }

  private static class TokenIterator implements Iterator<Token> {

    TurkishLexer lexer;
    TurkishTokenizer tokenizer;
    org.antlr.v4.runtime.Token token;
    Token.Type type;

    private TokenIterator(TurkishTokenizer tokenizer, TurkishLexer lexer) {
      this.tokenizer = tokenizer;
      this.lexer = lexer;
    }

    @Override
    public boolean hasNext() {
      org.antlr.v4.runtime.Token token = lexer.nextToken();
      if (token.getType() == org.antlr.v4.runtime.Token.EOF) {
        return false;
      }
      type = convertType(token);
      while (tokenizer.typeIgnored(type)) {
        token = lexer.nextToken();
        if (token.getType() == org.antlr.v4.runtime.Token.EOF) {
          return false;
        }
        type = convertType(token);
      }
      this.token = token;
      return true;
    }

    @Override
    public Token next() {
      return convert(token, type);
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException("Remove not supported");
    }
  }
}
