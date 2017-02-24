package zemberek.embedding.fasttext;

import zemberek.core.SpaceTabTokenizer;
import zemberek.core.collections.IntVector;
import zemberek.core.logging.Log;
import zemberek.core.text.BlockTextLoader;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

class Dictionary {

    static final int TYPE_WORD = 0;
    static final int TYPE_LABEL = 1;

    private static final int MAX_VOCAB_SIZE = 10_000_000;
    private static final int MAX_LINE_SIZE = 1024;

    private Args args_;
    private int[] word2int_;
    private List<Entry> words_;
    private float[] pdiscard_;
    private int size_;
    private int nwords_;
    private int nlabels_;
    private long ntokens_;

    static String EOS = "</s>";
    private static String BOW = "<";
    private static String EOW = ">";

    public static class Entry {
        String word;
        long count; // TODO: can be int.
        int type;
        int[] subwords;
    }

    private Dictionary(Args args) {
        args_ = args;
        size_ = 0;
        nwords_ = 0;
        nlabels_ = 0;
        ntokens_ = 0;
        word2int_ = new int[MAX_VOCAB_SIZE];
        words_ = new ArrayList<>(1_000_000);
        Arrays.fill(word2int_, -1);
    }

    /**
     * This looks like a linear probing hash table.
     */
    private int find(String w) {
        int h = hash(w) % MAX_VOCAB_SIZE;
        while (word2int_[h] != -1 && (!words_.get(word2int_[h]).word.equals(w))) {
            h = (h + 1) % MAX_VOCAB_SIZE;
        }
        return h;
    }

    void add(String w) {
        addWithCount(w, 1);
    }

    private void addWithCount(String w, int count) {
        int h = find(w);
        // if this is an empty slot. add a new entry.
        ntokens_ += count;
        if (word2int_[h] == -1) {
            Entry e = new Entry();
            e.word = w;
            e.count = count;
            e.type = (w.startsWith(args_.label)) ? TYPE_LABEL : TYPE_WORD;
            words_.add(e);
            word2int_[h] = size_++;
        } else {
            // or increment the count.
            words_.get(word2int_[h]).count += count;
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

    private boolean discard(int id, float rand) {
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

    // Hash algorithm is slightly different. original uses unsigned integers
    // and loops through bytes.
    private int hash(String str) {
        return hash(str, 0, str.length());
    }

    private int hash(String str, int start, int end) {
        //TODO: make it char based after the fix
        String sub = str.substring(start, end);
        int h = 0x811C_9DC5;
        for (byte b : sub.getBytes()) {
            h = h ^ (int) b;
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
     * <p>
     * If wordId is not -1, wordId value is added to result[0]
     */
    private int[] computeNgrams(String word, int wordId) {

        int endGram = args_.maxn < word.length() ? args_.maxn : word.length();
        int size = 0;
        for (int i = args_.minn; i <= endGram; i++) {
            size += (word.length() - i + 1);
        }

        int[] result;
        int counter;
        if (wordId == -1) {
            result = new int[size];
            counter = 0;
        } else {
            result = new int[size + 1];
            result[0] = wordId;
            counter = 1;
        }

        if (word.length() < args_.minn) {
            return result;
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

    private void initNgrams() {
        for (int i = 0; i < size_; i++) {
            String word = BOW + words_.get(i).word + EOW;
            // adds the wordId to the n-grams as well.
            words_.get(i).subwords = computeNgrams(word, i);
        }
    }


    static Dictionary readFromFile(Path file, final Args args) throws IOException {

        Log.info("Initialize dictionary and histograms.");
        Dictionary dictionary = new Dictionary(args);

        Log.info("Loading text.");
        BlockTextLoader loader = new BlockTextLoader(file, 100_000);
        SpaceTabTokenizer tokenizer = new SpaceTabTokenizer();

        int blockCounter = 1;

        for (List<String> lines : loader) {
            for (String line : lines) {
                List<String> split = tokenizer.splitToList(line);
                split.add(EOS);
                for (String word : split) {
                    dictionary.addWithCount(word, 1);
                }
            }
            Log.info("Lines read: %d (thousands) ", blockCounter * 100);
            blockCounter++;
        }
        Log.info("Word + Label count = %d", dictionary.words_.size());
        Log.info("Removing word and labels with small counts. Min word = %d, Min Label = %d",
                args.minCount, args.minCountLabel);
        // now we have the histograms. Remove based on count.
        dictionary.words_.sort((e1, e2) -> {
            if (e1.type != e2.type)
                return Integer.compare(e1.type, e2.type);
            else return Long.compare(e2.count, e1.count);
        });

        LinkedHashSet<Entry> all = new LinkedHashSet<>(dictionary.words_);
        List<Entry> toRemove = dictionary.words_
                .stream()
                .filter(s -> (s.type == TYPE_WORD && s.count < args.minCount ||
                        s.type == TYPE_LABEL && s.count < args.minCountLabel))
                .collect(Collectors.toList());
        all.removeAll(toRemove);
        dictionary.words_ = new ArrayList<>(all);
        dictionary.size_ = 0;
        dictionary.nwords_ = 0;
        dictionary.nlabels_ = 0;
        Arrays.fill(dictionary.word2int_, -1);
        for (Entry e : dictionary.words_) {
            int i = dictionary.find(e.word);
            dictionary.word2int_[i] = dictionary.size_++;
            if (e.type == TYPE_WORD) dictionary.nwords_++;
            if (e.type == TYPE_LABEL) dictionary.nlabels_++;
        }
        Log.info("Word count = %d , Label count = %d", dictionary.nwords(), dictionary.nlabels());
        dictionary.initTableDiscard();
        Log.info("Adding character n-grams for words.");
        dictionary.initNgrams();
        Log.info("Done.");
        return dictionary;
    }

    private void initTableDiscard() {
        pdiscard_ = new float[size_];
        for (int i = 0; i < size_; i++) {
            float f = ((float) words_.get(i).count) / ntokens_;
            pdiscard_[i] = (float) (Math.sqrt(args_.t / f) + args_.t / f);
        }
    }

    long[] getCounts(int entry_type) {
        long[] counts = entry_type == TYPE_WORD ? new long[nwords_] : new long[nlabels_];
        int c = 0;
        for (Entry entry : words_) {
            if (entry.type == entry_type) {
                counts[c] = entry.count;
                c++;
            }
        }
        return counts;
    }

    //adds word level n-grams. for n=1 means uni-grams, no value is returned.
    void addNgrams(IntVector line, int n) {
        int line_size = line.size();
        for (int i = 0; i < line_size; i++) {
            long h = line.get(i);
            for (int j = i + 1; j < line_size && j < i + n; j++) {
                h = h * 116049371 + line.get(j);
                line.add((int) (nwords_ + (h % args_.bucket)));
            }
        }
    }

    private static SpaceTabTokenizer tokenizer = new SpaceTabTokenizer();

    int getLine(
            String line,
            IntVector words,
            IntVector labels,
            Random random) {

        int ntokens = 0;
        List<String> tokens = tokenizer.splitToList(line);
        tokens.add(EOS);

        for (String token : tokens) {
            int wid = getId(token);
            if (wid < 0) continue;
            int type = getType(wid);
            ntokens++;
            //TODO: consider caching random.nextFloat
            if (type == TYPE_WORD && !discard(wid, random.nextFloat())) {
                words.add(wid);
            }
            if (type == TYPE_LABEL) {
                labels.add(wid - nwords_);
            }
            if (words.size() > MAX_LINE_SIZE && args_.model != Args.model_name.sup) {
                break;
            }
        }
        return ntokens;
    }

    String getLabel(int lid) {
        assert (lid >= 0);
        assert (lid < nlabels_);
        return words_.get(lid + nwords_).word;
    }

    void save(DataOutputStream out) throws IOException {
        out.writeInt(size_);
        out.writeInt(nwords_);
        out.writeInt(nlabels_);
        out.writeLong(ntokens_);
        for (int i = 0; i < size_; i++) {
            Entry e = words_.get(i);
            out.writeUTF(e.word);
            out.writeLong(e.count);
            out.writeInt(e.type);
        }
    }

    static Dictionary load(DataInputStream dis, Args args) throws IOException {

        Dictionary dict = new Dictionary(args);

        dict.size_ = dis.readInt();
        dict.nwords_ = dis.readInt();
        dict.nlabels_ = dis.readInt();
        dict.ntokens_ = dis.readLong();
        for (int i = 0; i < dict.size_; i++) {
            Entry e = new Entry();
            e.word = dis.readUTF();
            e.count = dis.readLong();
            e.type = dis.readInt();
            dict.words_.add(e);
            dict.word2int_[dict.find(e.word)] = i;
        }
        dict.initTableDiscard();
        dict.initNgrams();
        return dict;
    }
}
