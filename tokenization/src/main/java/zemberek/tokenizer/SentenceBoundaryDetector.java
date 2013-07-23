package zemberek.tokenizer;

import java.util.List;

public interface SentenceBoundaryDetector {
    List<String> getSentences(String input);
}
