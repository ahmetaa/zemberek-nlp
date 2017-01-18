package zemberek.core.collections;

import zemberek.core.StringPair;
import zemberek.core.text.TextUtil;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class BidirectionalIndexLookup<T> {
    UIntValueMap<T> indexLookup = new UIntValueMap<>();
    UIntMap<T> keyLookup = new UIntMap<>();

    public BidirectionalIndexLookup(UIntValueMap<T> indexLookup, UIntMap<T> keyLookup) {
        this.indexLookup = indexLookup;
        this.keyLookup = keyLookup;
    }

    public static BidirectionalIndexLookup<String> fromTextFileWithIndex(Path path, char delimiter) throws IOException {
        if (!path.toFile().exists()) {
            throw new IllegalArgumentException("File " + path + " does not exist.");
        }
        List<String> lines = TextUtil.loadLinesWithText(path);
        UIntValueMap<String> indexLookup = new UIntValueMap<>(lines.size());
        UIntMap<String> wordLookup = new UIntMap<>(lines.size());
        for (String line : lines) {
            StringPair pair = StringPair.fromString(line, delimiter);
            String word = pair.first;
            int index = Integer.parseInt(pair.second);
            if (indexLookup.contains(word)) {
                throw new IllegalArgumentException("Duplicated word in line : [" + line + "]");
            }
            if (wordLookup.containsKey(index)) {
                throw new IllegalArgumentException("Duplicated index in line : [" + line + "]");
            }
            if (index < 0) {
                throw new IllegalArgumentException("Index Value cannot be negative : [" + line + "]");
            }
            indexLookup.put(word, index);
            wordLookup.put(index, word);
        }
        return new BidirectionalIndexLookup<>(indexLookup, wordLookup);
    }

    public int getIndex(T key) {
        return indexLookup.get(key);
    }

    public T getKey(int index) {
        return keyLookup.get(index);
    }

    public boolean containsKey(T key) {
        return indexLookup.contains(key);
    }

    public Iterable<T> keys() {
        return indexLookup.getKeyList();
    }

    public int size() {
        return indexLookup.size();
    }
}
