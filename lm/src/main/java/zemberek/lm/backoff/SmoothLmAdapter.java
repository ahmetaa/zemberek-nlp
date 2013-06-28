package zemberek.lm.backoff;


import zemberek.lm.LmVocabulary;
import zemberek.lm.compression.SmoothLm;

/**
 * This is an adapter for SmoothLm compressed language model
 */
public class SmoothLmAdapter implements NgramLanguageModel {

    public SmoothLm smoothLm;
    int count;

    public SmoothLmAdapter(SmoothLm smoothLm) {
        this.smoothLm = smoothLm;
    }

    @Override
    public double getUnigramProb(int id) {
        return smoothLm.getProbability(id);
    }

    @Override
    public double getNgramProb(int... ids) {
        count++;
        return smoothLm.getProbability(ids);
    }

    @Override
    public int getOrder() {
        return smoothLm.getOrder();
    }

    @Override
    public double getLogBase() {
        return smoothLm.getLogBase();
    }

    @Override
    public int gramCount(int order) {
        return smoothLm.getGramCount(order);
    }

    @Override
    public LmVocabulary getVocabulary() {
        return smoothLm.getVocabulary();
    }

    @Override
    public String explain(int... tokenIds) {
        return smoothLm.explain(tokenIds);
    }

    public void dump() {
        System.out.println(count);
    }
}
