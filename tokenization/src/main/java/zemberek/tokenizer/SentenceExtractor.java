package zemberek.tokenizer;

import java.util.List;

public interface SentenceExtractor {
    List<String> extract(String paragraph);

    List<String> extract(List<String> paragraphs);
}
