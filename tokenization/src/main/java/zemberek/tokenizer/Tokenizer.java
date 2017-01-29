package zemberek.tokenizer;


import java.util.List;

//TODO: better method names.
public interface Tokenizer {
    List<String> tokenStrings(String input);
}
