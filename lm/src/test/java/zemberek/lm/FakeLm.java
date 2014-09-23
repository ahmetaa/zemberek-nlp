package zemberek.lm;

import com.google.common.base.Joiner;
import zemberek.core.io.SimpleTextWriter;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class FakeLm {

    int unigramLength = 32 * 32 * 32 * 32;
    public String[] unigrams;
    public LmVocabulary vocabulary;
    public final int order;

    public FakeLm(int order) {
        this.order = order;
        String alphabet = "abcçdefgğhıijklmnoöpqrsştuüvwxyz";
        unigrams = new String[unigramLength];
        for (int i = 0; i < unigrams.length; i++) {
            StringBuilder sb = new StringBuilder();
            sb.append(alphabet.charAt((i >> 15) & 0x1f));
            sb.append(alphabet.charAt((i >> 10) & 0x1f));
            sb.append(alphabet.charAt((i >> 5) & 0x1f));
            sb.append(alphabet.charAt(i & 0x1f));
            unigrams[i] = sb.toString();
        }
        vocabulary = new LmVocabulary(unigrams);
    }

    public static class FakeGram {
        public int[] indexes;
        public String[] vals;
        public double prob;
        public double backoff;

        public FakeGram(int[] indexes, String[] vals, double prob, double backoff) {
            this.indexes = indexes;
            this.vals = vals;
            this.prob = prob;
            this.backoff = backoff;
        }
    }

    public FakeGram[] getNgramProbs(int o) {
        FakeGram[] probs = new FakeGram[unigramLength * o];
        int pp = 0;
        for (int i = 0; i < unigramLength; i++) {
            int[][] matrix = new int[o][o];
            int kk = i % unigramLength;
            for (int j = 0; j < matrix.length; j++) {
                for (int k = 0; k < matrix.length; k++) {
                    if (k == 0)
                        matrix[j][k] = i;
                    else matrix[j][k] = kk % unigramLength;
                    kk++;
                }
            }
            for (int t = 0; t < o; t++) {
                String[] blah = new String[o];
                int tt = 0;
                for (int val : matrix[t]) {
                    blah[tt++] = unigrams[val];
                }

                double p = (i % 1000) + (o * 1000) + 1;
                if (o < order)
                    probs[pp] = new FakeGram(matrix[t], blah, p / 10000, -p / 10000);
                else
                    probs[pp] = new FakeGram(matrix[t], blah, p / 10000, 0);
                pp++;
            }

        }
        return probs;
    }

    public void validate(FakeGram[] grams) {
        Set<String> set = new HashSet<>(grams.length);
        for (FakeGram gram : grams) {
            String s = Joiner.on(" ").join(gram.vals);
            if (set.contains(s))
                throw new IllegalStateException("Duplicated item:" + s);
            set.add(s);
        }
    }

    public void generateArpa(File fileName) throws IOException {
        System.out.println("unigrams = " + unigrams.length);
        SimpleTextWriter sw = SimpleTextWriter.keepOpenUTF8Writer(fileName);
        /*
        \data\
        ngram 1= 4
        ngram 2= 3
        ngram 3= 2
        */
        sw.writeLine("\\data\\");
        for (int o = 1; o <= order; o++) {
            sw.writeLine("ngram " + o + "=" + o * unigramLength);
        }
        for (int o = 1; o <= order; o++) {
            FakeGram[] probs = getNgramProbs(o);
            System.out.println("Validating..");
            validate(probs);
            System.out.println("Writing " + o + " grams.");
            sw.writeLine();
            sw.writeLine("\\" + o + "-grams:\n");
            for (FakeGram prob : probs) {
                if (o < order) {
                    sw.writeLine(String.format("%.4f %s %.4f", prob.prob, Joiner.on(" ").join(prob.vals), prob.backoff));
                } else {
                    sw.writeLine(String.format("%.4f %s", prob.prob, Joiner.on(" ").join(prob.vals)));
                }
            }
        }
        sw.writeLine();
        sw.writeLine("\\end\\");
    }

}
