package zemberek.tokenizer;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.antlr.v4.runtime.Token;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * A Naive sentence splitter based on lexer's capabilities.
 */
public class LexerSentenceExtractor implements SentenceExtractor {
    private ZemberekLexer lexer;
    private static Set<String> sentenceEnd = Sets.newHashSet(".", "!", "?", "...", ":");

    /**
     * Generates a Lexer inside that does tokenizes white spaces along with others.
     */
    public LexerSentenceExtractor() {
        this.lexer = new ZemberekLexer(false);
    }

    @Override
    public List<String> extract(String paragraph) {
        return Lists.newArrayList(new SentenceIterator(lexer.getTokenIterator(paragraph)));
    }

    @Override
    public List<String> extract(List<String> paragraphs) {
        List<String> result = new ArrayList<>();
        for (String paragraph : paragraphs) {
            result.addAll(extract(paragraph));
        }
        return result;
    }

    public List<String> getSentences(File file) throws IOException {
        return Lists.newArrayList(new SentenceIterator(lexer.getTokenIterator(file)));
    }

    public Iterator<String> getSentenceIterator(File file) throws IOException {
        return new SentenceIterator(lexer.getTokenIterator(file));
    }

    public Iterator<String> getSentenceIterator(String str) {
        return new SentenceIterator(lexer.getTokenIterator(str));
    }

    private static class SentenceIterator implements Iterator<String> {
        Iterator<Token> tokenIterator;
        String sentence;

        private SentenceIterator(Iterator<Token> tokenIterator) {
            this.tokenIterator = tokenIterator;
        }

        @Override
        public boolean hasNext() {
            StringBuilder sb = new StringBuilder();
            while (tokenIterator.hasNext()) {
                Token token = tokenIterator.next();
                if (sentenceEnd.contains(token.getText())) {
                    sb.append(token.getText());
                    this.sentence = sb.toString();
                    return true;
                }
                sb.append(token.getText());
            }
            sentence = sb.toString();
            return sentence.length() > 0;
        }

        @Override
        public String next() {
            return sentence.trim();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("Remove not supported");
        }
    }

}
