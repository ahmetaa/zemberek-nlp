package zemberek.embedding.fasttext;

import zemberek.core.SpaceTabTokenizer;
import zemberek.core.text.BlockTextIterator;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

public class Dictionary {

    int id_type;

    public static final int TYPE_WORD = 0;
    public static final int TYPE_LABEL = 1;

    static final int MAX_VOCAB_SIZE = 10_000_000;
    static final int MAX_LINE_SIZE = 1024;

    Args args_;
    int[] word2int_;
    List<Entry> words_;
    float[] pdiscard_;
    int size_;
    int nwords_;
    int nlabels_;
    long ntokens_;

    static String EOS = "</s>";
    static String BOW = "<";
    static String EOW = ">";

    public static class Entry {
        String word;
        int count;
        int type;
        int[] subwords;
    }

    Dictionary(Args args) {
        args_ = args;
        size_ = 0;
        nwords_ = 0;
        nlabels_ = 0;
        ntokens_ = 0;
        word2int_ = new int[MAX_VOCAB_SIZE];
        Arrays.fill(word2int_, -1);
    }


    /**
     * This looks like a linear probing hash table.
     */
    int find(String w) {
        int h = hash(w) % MAX_VOCAB_SIZE;
        while (word2int_[h] != -1 && (!words_.get(word2int_[h]).word.equals(w))) {
            h = (h + 1) % MAX_VOCAB_SIZE;
        }
        return h;
    }

    void add(String w) {
        int h = find(w);
        ntokens_++;
        // if this is an empty slot. add a new entry.
        if (word2int_[h] == -1) {
            Entry e = new Entry();
            e.word = w;
            e.count = 1;
            e.type = (w.startsWith(args_.label)) ? TYPE_LABEL : TYPE_WORD;
            words_.add(e);
            word2int_[h] = size_++;
        } else {
            // or increment the count.
            words_.get(word2int_[h]).count++;
        }
    }

    int nwords() {
        return nwords_;
    }

    int nlabels() {
        return nlabels_;
    }

    long ntokens() {
        return ntokens_;
    }

    int[] getNgrams(int i) {
        assert (i >= 0);
        assert (i < nwords_);
        return words_.get(i).subwords;
    }

    int[] getNgrams(String word) {
        int i = getId(word);
        if (i >= 0) {
            return getNgrams(i);
        }
        return computeNgrams(BOW + word + EOW, i);
    }

    boolean discard(int id, float rand) {
        assert (id >= 0);
        assert (id < nwords_);
        if (args_.model == Args.model_name.sup) return false;
        return rand > pdiscard_[id];
    }

    int getId(String w) {
        int h = find(w);
        return word2int_[h];
    }

    int getType(int id) {
        assert (id >= 0);
        assert (id < size_);
        return words_.get(id).type;
    }

    String getWord(int id) {
        assert (id >= 0);
        assert (id < size_);
        return words_.get(id).word;
    }

    // TODO: hash algorithm is slightly different. original uses unsigned integers
    // and loops through bytes.
    int hash(String str) {
        int h = 0x811C_9DC5;
        for (int i = 0; i < str.length(); i++) {
            h = h ^ str.charAt(i);
            h = h * 16777619;
        }
        return h & 0x7fff_ffff;
    }

    int hash(String str, int start, int end) {
        int h = 0x811C_9DC5;
        for (int i = start; i < end; i++) {
            h = h ^ str.charAt(i);
            h = h * 16777619;
        }
        return h & 0x7fff_ffff;
    }

    /**
     * this algorithm is also slightly different than the original.
     * This basically computes the character ngrams from a word
     * But it does not use the ngram, instead it calculates a hash of it.
     * minn defines the minimum n-gram length, maxn defines the maximum ngram length.
     * For example, For word 'zemberek' minn = 3 and maxn = 6
     * these ngrams are calculated:
     * _ze, _zem, _zemb, _zembe
     * zem, zemb, zembe, zember
     * emb, embe, ember, embere
     * mbe, mber, mbere, mberek
     * ber, bere, berek, berek_
     * ere, erek, erek_
     * rek, rek_,
     * ek_
     *
     * If wordId is not -1, wordId value is added to result[0]

     */
    int[] computeNgrams(String word, int wordId) {

        if (word.length() < args_.minn) {
            return new int[0];
        }

        int endGram = args_.maxn < word.length() ? args_.maxn : word.length();
        int size = 0;
        for (int i = args_.minn; i <= endGram; i++) {
            size += (word.length() - i + 1);
        }

        int[] result;
        int counter;
        if(wordId==-1) {
            result = new int[size];
            counter = 0;
        } else {
            result = new int[size+1];
            result[0] = wordId;
            counter = 1;
        }

        for (int i = 0; i <= word.length() - args_.minn; i++) {
            int n = args_.minn;
            while (i + n <= word.length() && n <= endGram) {
                int hash = hash(word, i, i + n) % args_.bucket;
                result[counter] = nwords_ + hash;
                n++;
                counter++;
            }
        }
        return result;
    }

    void initNgrams() {
        for (int i = 0; i < size_; i++) {
            String  word = BOW + words_.get(i).word + EOW;
            // adds the wordId to the n-grams as well.
            words_.get(i).subwords = computeNgrams(word, i);
        }
    }

    void readFromFile(Path file) throws IOException {
        BlockTextIterator iterator = new BlockTextIterator(file, 100_000);
        SpaceTabTokenizer tokenizer = new SpaceTabTokenizer();
        while(iterator.hasNext()) {
            List<String> lines = iterator.next();
            for (String line : lines) {
                for(String word: tokenizer.split(line)) {
                    add(word);
                }
            }
        }
        //TODO: finish.
        //threshold(args_.minCount, args_.minCountLabel);
        //initTableDiscard();
        initNgrams();
    }

    public static void main(String[] args) {
        Args argz = new Args();
        argz.minn = 3;
        argz.maxn = 6;
        argz.bucket = 20_000_000;
        Dictionary dictionary = new Dictionary(argz);
        dictionary.computeNgrams("_zebe_",-1);
    }


}
