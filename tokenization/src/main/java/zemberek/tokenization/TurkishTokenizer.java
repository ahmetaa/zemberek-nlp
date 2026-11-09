package zemberek.tokenization;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.IntStream;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.LexerNoViableAltException;
import org.antlr.v4.runtime.misc.Interval;
import zemberek.tokenization.Token.Type;
import zemberek.tokenization.antlr.CustomLexerATNSimulator;
import zemberek.tokenization.antlr.TurkishLexer;


/**
 * A wrapper for Antlr generated lexer.
 */
public class TurkishTokenizer {

  public static final TurkishTokenizer ALL = builder().acceptAll().build();
  public static final TurkishTokenizer DEFAULT = builder()
      .acceptAll()
      .ignoreTypes(Token.Type.NewLine, Token.Type.SpaceTab)
      .build();

  private final long acceptedTypeBits;

  private TurkishTokenizer(long acceptedTypeBits) {
    this.acceptedTypeBits = acceptedTypeBits;
  }

  public static Builder builder() {
    return new Builder();
  }

  private static TurkishLexer lexerInstance(CharStream inputStream) {
    TurkishLexer lexer = new TurkishLexer(inputStream);
    lexer.removeErrorListeners();
    return lexer;
  }

  public boolean isTypeAccepted(Token.Type i) {
    return typeAccepted(i);
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

  private List<Token> getAllTokens(TurkishLexer lexer) {
    List<Token> tokens = new ArrayList<>();
    TokenIterator iterator = new TokenIterator(this, lexer);
    while (iterator.hasNext()) {
      tokens.add(iterator.next());
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
    return convertType(token.getType());
  }

  private static Token.Type convertType(int tokenType) {
    switch (tokenType) {
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
            TurkishLexer.VOCABULARY.getDisplayName(tokenType));
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

    private final TurkishTokenizer tokenizer;
    private final TurkishLexer lexer;
    private final CharStream input;
    private final CustomLexerATNSimulator interp;
    private int deferredType;
    private int deferredStart;
    private int deferredStop;
    private Token nextToken;

    private TokenIterator(TurkishTokenizer tokenizer, TurkishLexer lexer) {
      this.tokenizer = tokenizer;
      this.lexer = lexer;
      this.input = lexer.getInputStream();
      this.interp = (CustomLexerATNSimulator) lexer.getInterpreter();
      this.nextToken = fetchNext();
    }

    private int matchNextToken() {
      while (true) {
        if (input.LA(1) == IntStream.EOF) {
          return org.antlr.v4.runtime.Token.EOF;
        }
        try {
          int ttype = interp.match(input, Lexer.DEFAULT_MODE);
          if (ttype != Lexer.SKIP) {
            return ttype;
          }
        } catch (LexerNoViableAltException e) {
          lexer.recover(e);
        }
      }
    }

    private Token fetchNext() {
      while (true) {
        int antlrType;
        int start;
        int stop;

        if (deferredType != 0) {
          antlrType = deferredType;
          start = deferredStart;
          stop = deferredStop;
          deferredType = 0;
        } else {
          antlrType = matchNextToken();
          if (antlrType == org.antlr.v4.runtime.Token.EOF) {
            return null;
          }
          start = interp.getTokenStartIndex();
          stop = input.index() - 1;
        }

        if ((antlrType == TurkishLexer.SpaceTab && tokenizer.typeIgnored(Type.SpaceTab))
            || (antlrType == TurkishLexer.NewLine && tokenizer.typeIgnored(Type.NewLine))) {
          continue;
        }

        if (antlrType == TurkishLexer.Word && input.LA(1) == '.') {
          int next2Type = matchNextToken();
          int next2Start = interp.getTokenStartIndex();
          int next2Stop = input.index() - 1;
          if (next2Type == TurkishLexer.Punctuation && next2Start == next2Stop) {
            String abbrev = input.getText(Interval.of(start, next2Stop));
            if (TurkishLexer.isAbbreviation(abbrev)) {
              if (tokenizer.typeIgnored(Type.Abbreviation)) {
                continue;
              }
              return new Token(abbrev, Type.Abbreviation, start, next2Stop);
            }
          }
          deferredType = next2Type;
          deferredStart = next2Start;
          deferredStop = next2Stop;
        }

        Token.Type type = convertType(antlrType);
        if (tokenizer.typeIgnored(type)) {
          continue;
        }
        return new Token(input.getText(Interval.of(start, stop)), type, start, stop);
      }
    }

    @Override
    public boolean hasNext() {
      return nextToken != null;
    }

    @Override
    public Token next() {
      if (nextToken == null) {
        throw new NoSuchElementException("No more tokens");
      }
      Token current = nextToken;
      nextToken = fetchNext();
      return current;
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException("Remove not supported");
    }
  }
}
