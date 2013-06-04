package zemberek.morphology.lexicon;

public class LexiconException extends RuntimeException {
    public LexiconException(String message) {
        super(message);
    }

    public LexiconException(String message, Throwable cause) {
        super(message, cause);
    }
}
