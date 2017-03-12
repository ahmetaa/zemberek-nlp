package zemberek.lm;

public class DummyLanguageModel implements NgramLanguageModel{

    LmVocabulary vocabulary;

    public DummyLanguageModel() {
        vocabulary = new LmVocabulary.Builder().generate();
    }

    @Override
    public float getUnigramProbability(int id) {
        return 0;
    }

    @Override
    public boolean ngramExists(int... wordIndexes) {
        return false;
    }

    @Override
    public float getProbability(int... ids) {
        return 0;
    }

    @Override
    public float getTriGramProbability(int id0, int id1, int id2) {
        return 0;
    }

    @Override
    public float getTriGramProbability(int id0, int id1, int id2, int fingerPrint) {
        return 0;
    }

    @Override
    public int getOrder() {
        return 0;
    }

    @Override
    public LmVocabulary getVocabulary() {
        return vocabulary;
    }
}
