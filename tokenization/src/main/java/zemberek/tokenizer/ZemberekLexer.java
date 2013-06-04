package zemberek.tokenizer;

import org.antlr.v4.runtime.*;
import zemberek.tokenizer.antlr.TurkishLexer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A wrapper for antlr generated lexer.
 */
public class ZemberekLexer {

    static final Logger logger = Logger.getLogger(ZemberekLexer.class.getName());
    boolean ignoreWhiteSpaces = true;

    private static final BaseErrorListener IGNORING_ERROR_LISTENER = new BaseErrorListener() {
        @Override
        public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine,
                                String msg, RecognitionException e) {
            // Do nothing.
        }
    };
    private static final BaseErrorListener LOGGING_ERROR_LISTENER = new BaseErrorListener() {
        @Override
        public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine,
                                String msg, RecognitionException e) {
            // Just log the error.
            logger.log(Level.WARNING, "Unknown token. Original error: " + msg);
        }
    };

    public ZemberekLexer() {
    }

    public ZemberekLexer(boolean ignoreWhiteSpaces) {
        this.ignoreWhiteSpaces = ignoreWhiteSpaces;
    }

    public List<Token> tokenizeAll(File file) throws IOException {
        ANTLRInputStream inputStream = new ANTLRFileStream(file.getAbsolutePath());
        return getAllTokens(createTurkishLexer(inputStream));
    }

    public List<Token> tokenizeAll(String input) {
        ANTLRInputStream inputStream = new ANTLRInputStream(input);
        return getAllTokens(createTurkishLexer(inputStream));
    }

    public Iterator<Token> getTokenIterator(String input) {
        ANTLRInputStream inputStream = new ANTLRInputStream(input);
        return new TokenIterator(createTurkishLexer(inputStream), ignoreWhiteSpaces);
    }

    public Iterator<Token> getTokenIterator(File file) throws IOException {
        ANTLRInputStream inputStream = new ANTLRFileStream(file.getAbsolutePath());
        return new TokenIterator(createTurkishLexer(inputStream), ignoreWhiteSpaces);
    }

    private static TurkishLexer createTurkishLexer(ANTLRInputStream inputStream) {
        TurkishLexer lexer = new TurkishLexer(inputStream);
        lexer.removeErrorListeners();
        lexer.addErrorListener(IGNORING_ERROR_LISTENER);
        return lexer;
    }

    private List<Token> getAllTokens(Lexer lexer) {
        List<Token> tokens = new ArrayList<Token>();
        for (Token token = lexer.nextToken();
             token.getType() != Token.EOF;
             token = lexer.nextToken()) {
            int type = token.getType();
            if (ignoreWhiteSpaces && (type == TurkishLexer.SpaceTab || type == TurkishLexer.NewLine)) {
                continue;
            }
            tokens.add(token);
        }
        return tokens;
    }

    private static class TokenIterator implements Iterator<Token> {

        Lexer lexer;
        Token token;
        boolean ignoreWhiteSpaces = true;

        private TokenIterator(Lexer lexer, boolean ignoreWhiteSpaces) {
            this.lexer = lexer;
            this.ignoreWhiteSpaces = ignoreWhiteSpaces;
        }

        @Override
        public boolean hasNext() {
            Token token = lexer.nextToken();
            if (token.getType() == Token.EOF)
                return false;
            while (ignoreWhiteSpaces && (token.getType() == TurkishLexer.SpaceTab || token.getType() == TurkishLexer.NewLine)) {
                token = lexer.nextToken();
                if (token.getType() == Token.EOF)
                    return false;
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
