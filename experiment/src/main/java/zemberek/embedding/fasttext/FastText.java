package zemberek.embedding.fasttext;

import com.google.common.base.Stopwatch;
import zemberek.core.logging.Log;

import java.util.concurrent.TimeUnit;

public class FastText {
    Args args_;
    Dictionary dict_;
    Matrix input_;
    Matrix output_;
    Model model_;
    int tokenCount;

    Stopwatch stopwatch;

    // TODO: change to java style. return a vector.
    void getVector(Vector vec, String word) {
        int[] ngrams = dict_.getNgrams(word);
        vec.zero();  //TODO: may not be necessary.
        for (int i : ngrams) {
            // TODO: this is curious. why add the bucket hash values?
            vec.addRow(input_, i);
        }
        if (ngrams.length > 0) {
            vec.mul(1.0f / ngrams.length);
        }
    }


    void printInfo(float progress, float loss) {
        float t = stopwatch.elapsed(TimeUnit.MILLISECONDS) / 1000f;
        float wst = (float) tokenCount / t;
        float lr = (float) (args_.lr * (1.0f - progress));
        int eta = (int) (t / progress * (1 - progress) / args_.thread);
        int etah = eta / 3600;
        int etam = (eta - etah * 3600) / 60;

        Log.info("Progress: %.1f%% words/sec/thread: %.1f lr: %.6f loss:%.6f eta %d h %d m",
                100 * progress,
                wst,
                lr,
                loss,
                etah, etam);
    }


}
