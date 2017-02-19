package zemberek.embedding.fasttext;

import zemberek.core.collections.DynamicIntArray;

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
    int ntokens_;

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
        Arrays.fill(word2int_,-1);
    }


/*
    int find(String w) const {
        int h = hash(w) % MAX_VOCAB_SIZE;
        while (word2int_[h] != -1 && words_[word2int_[h]].word != w) {
            h = (h + 1) % MAX_VOCAB_SIZE;
        }
        return h;
    }
*/




}
