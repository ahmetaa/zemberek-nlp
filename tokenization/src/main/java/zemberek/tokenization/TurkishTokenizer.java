package zemberek.tokenization;

import org.antlr.v4.runtime.*;
import zemberek.core.logging.Log;
import zemberek.tokenization.antlr.TurkishLexer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


/**
 * A wrapper for Antlr generated lexer.
 */
public class TurkishTokenizer {

    private static final int MAX_TOKEN_TYPE = TurkishLexer.VOCABULARY.getMaxTokenType();

    public static final TurkishTokenizer DEFAULT = builder()
            .acceptAll()
            .ignoreTypes(TurkishLexer.NewLine, TurkishLexer.SpaceTab)
            .build();

    public static final TurkishTokenizer ALL = builder().acceptAll().build();

    private long acceptedTypeBits;

    private static final BaseErrorListener IGNORING_ERROR_LISTENER = new BaseErrorListener() {
        @Override
        public void syntaxError(
                Recognizer<?, ?> recognizer,
                Object offendingSymbol,
                int line,
                int charPositionInLine,
                String msg,
                RecognitionException e) {
            Log.warn("Unknown token. Original error: %s ", msg);
            // Do nothing.
        }
    };
    private static final BaseErrorListener LOGGING_ERROR_LISTENER = new BaseErrorListener() {
        @Override
        public void syntaxError(Recognizer<?, ?> recognizer,
                                Object offendingSymbol,
                                int line,
                                int charPositionInLine,
                                String msg,
                                RecognitionException e) {
            // Just log the error.
            Log.warn("Unknown token. Original error: %s ", msg);
        }
    };

    private TurkishTokenizer(long acceptedTypeBits) {
        this.acceptedTypeBits = acceptedTypeBits;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private long acceptedTypeBits = ~0L;

        public Builder acceptTypes(int... types) {
            for (int i : types) {
                validateType(i);
                this.acceptedTypeBits |= (1L << i);
            }
            return this;
        }

        public Builder ignoreTypes(int... types) {
            for (int i : types) {
                validateType(i);
                this.acceptedTypeBits &= ~(1L << i);
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

    private static void validateType(int i) {
        if (i < 0) {
            throw new IllegalStateException("Token index cannot be negative. But it is : " + i);
        }

        if (i > MAX_TOKEN_TYPE) {
            throw new IllegalStateException("Token index cannot be larger than " + MAX_TOKEN_TYPE
                    + ". But it is : " + i);
        }
    }

    public boolean isTypeAccepted(int i) {
        validateType(i);
        return !typeAccepted(i);
    }

    public boolean isTypeIgnored(int i) {
        validateType(i);
        return !typeAccepted(i);
    }

    private boolean typeAccepted(int i) {
        return (acceptedTypeBits & (1L << i)) != 0;
    }

    private boolean typeIgnored(int i) {
        return (acceptedTypeBits & (1L << i)) == 0;
    }


    public List<Token> tokenize(File file) throws IOException {
        ANTLRInputStream inputStream = new ANTLRFileStream(file.getAbsolutePath());
        return getAllTokens(lexerInstance(inputStream));
    }

    public List<Token> tokenize(String input) {
        ANTLRInputStream inputStream = new ANTLRInputStream(input);
        return getAllTokens(lexerInstance(inputStream));
    }

    public List<String> tokenizeToStrings(String input) {
        List<String> tokenStrings = new ArrayList<>();
        for (Token token : getAllTokens(lexerInstance(new ANTLRInputStream(input)))) {
            tokenStrings.add(token.getText());
        }
        return tokenStrings;
    }

    public Iterator<Token> getTokenIterator(String input) {
        ANTLRInputStream inputStream = new ANTLRInputStream(input);
        return new TokenIterator(this, lexerInstance(inputStream));
    }

    public Iterator<Token> getTokenIterator(File file) throws IOException {
        ANTLRInputStream inputStream = new ANTLRFileStream(file.getAbsolutePath());
        return new TokenIterator(this, lexerInstance(inputStream));
    }

    private static TurkishLexer lexerInstance(ANTLRInputStream inputStream) {
        TurkishLexer lexer = new TurkishLexer(inputStream);
        lexer.removeErrorListeners();
        lexer.addErrorListener(IGNORING_ERROR_LISTENER);
        return lexer;
    }

    private List<Token> getAllTokens(Lexer lexer) {
        List<Token> tokens = new ArrayList<>();
        for (Token token = lexer.nextToken();
             token.getType() != Token.EOF;
             token = lexer.nextToken()) {
            int type = token.getType();
            if (typeIgnored(type)) {
                continue;
            }
            tokens.add(token);
        }
        return tokens;
    }

    private static class TokenIterator implements Iterator<Token> {

        TurkishLexer lexer;
        TurkishTokenizer tokenizer;
        Token token;

        private TokenIterator(TurkishTokenizer tokenizer, TurkishLexer lexer) {
            this.tokenizer = tokenizer;
            this.lexer = lexer;
        }

        @Override
        public boolean hasNext() {
            Token token = lexer.nextToken();
            if (token.getType() == Token.EOF) {
                return false;
            }
            while (tokenizer.typeIgnored(token.getType())) {
                token = lexer.nextToken();
                if (token.getType() == Token.EOF) {
                    return false;
                }
            }
            this.token = token;
            return true;
        }

        @Override
        public Token next() {
            return token;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("Remove not supported");
        }
    }
}
