package zemberek.embedding;

import com.google.common.base.Stopwatch;
import zemberek.core.collections.UIntMap;
import zemberek.core.io.IOUtil;
import zemberek.core.logging.Log;
import zemberek.core.math.FloatArrays;
import zemberek.lm.LmVocabulary;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class WordVectorLookup {

    private LmVocabulary vocabulary;
    private UIntMap<Vector> vectors = new UIntMap<>();
    private int dimension;

    public static class Vector {
        int wordIndex;
        float[] array;

        public Vector(int wordIndex, float[] array) {
            this.wordIndex = wordIndex;
            this.array = array;
        }
    }

    public static WordVectorLookup loadFromText(Path txtFile) throws IOException {

        List<String> lines = Files.readAllLines(txtFile);
        // generate vocabulary
        LmVocabulary.Builder builder = new LmVocabulary.Builder();
        int lineCount = 0;
        for (String line : lines) {
            if (lineCount++ == 0) { // skip first line.
                continue;
            }
            int index = line.indexOf(' ');
            String word = line.substring(0, index);
            builder.add(word);
        }
        LmVocabulary vocabulary = builder.generate();

        UIntMap<Vector> vectors = new UIntMap<>(lines.size());
        lineCount = 0;

        for (String line : lines) {
            if (lineCount++ == 0) { // skip first line.
                continue;
            }
            line = line.trim();
            int index = line.indexOf(' ');
            String word = line.substring(0, index);
            String floats = line.substring(index + 1);
            float[] vector = FloatArrays.fromString(floats, " ");
            int wordIndex = vocabulary.indexOf(word);
            vectors.put(wordIndex, new Vector(wordIndex, vector));
        }
        return new WordVectorLookup(vocabulary, vectors);
    }

    public void saveToFolder(Path out) throws IOException {
        Files.createDirectories(out);
        vocabulary.saveBinary(out.resolve("vocabulary.bin").toFile());
        try (DataOutputStream dos = IOUtil.getDataOutputStream(out.resolve("word-vectors.bin"))) {
            dos.writeInt(vectors.size());
            dos.writeInt(dimension);
            for (Vector ve : vectors.getValuesSortedByKey()) {
                dos.writeInt(ve.wordIndex);
                FloatArrays.serializeRaw(dos, ve.array);
            }
        }
    }

    private WordVectorLookup(LmVocabulary vocabulary, UIntMap<Vector> vectors) {
        this.vocabulary = vocabulary;
        this.vectors = vectors;
        this.dimension = vectors.get(0).array.length;
    }

    private WordVectorLookup(LmVocabulary vocabulary, Vector[] vectorArray) {
        this.vocabulary = vocabulary;
        this.vectors = new UIntMap<>(vocabulary.size());
        for (Vector vector : vectorArray) {
            vectors.put(vector.wordIndex, vector);
        }
        this.dimension = vectors.get(0).array.length;
    }

    public static WordVectorLookup loadFromBinaryFast(Path vectorFile, Path vocabularyFile) throws IOException {

        LmVocabulary vocabulary = LmVocabulary.loadFromBinary(vocabularyFile.toFile());

        int wordCount;
        int vectorDimension;
        try (DataInputStream dis = IOUtil.getDataInputStream(vectorFile)) {
            wordCount = dis.readInt();
            vectorDimension = dis.readInt();
        }

        RandomAccessFile aFile = new RandomAccessFile(vectorFile.toFile(), "r");
        FileChannel inChannel = aFile.getChannel();

        long start = 8, size;
        int blockSize = 4 + vectorDimension * 4;

        Vector[] vectors = new Vector[wordCount];

        int wordCounter = 0;
        int wordBlockSize = 100_000;

        while (wordCounter < wordCount) {

            if (wordCounter + wordBlockSize > wordCount) {
                wordBlockSize = (wordCount - wordCounter);
            }
            size = blockSize * wordBlockSize;

            MappedByteBuffer buffer = inChannel.map(FileChannel.MapMode.READ_ONLY, start, size);
            buffer.load();

            start += size;

            int blockCounter = 0;
            while (blockCounter < wordBlockSize) {
                int wordIndex = buffer.getInt();
                float[] data = new float[vectorDimension];
                buffer.asFloatBuffer().get(data);
                vectors[wordCounter] = new Vector(wordIndex, data);
                wordCounter++;
                blockCounter++;
                buffer.position(blockCounter * blockSize);
            }
        }

        return new WordVectorLookup(vocabulary, vectors);

    }

    public float[] getVector(String word) {
        return vectors.get(vocabulary.indexOf(word)).array;
    }

    public boolean containsWord(String word) {
        return vocabulary.contains(word);
    }

    public static void main(String[] args) throws IOException {

        WordVectorLookup lookup = WordVectorLookup.loadFromText(
                Paths.get("/media/depo/data/aaa/corpora/model-large-min10.vec")
        );
        Path out = Paths.get("/media/depo/data/aaa/corpora/foo");
        lookup.saveToFolder(out);
        Stopwatch sw = Stopwatch.createStarted();
        loadFromBinaryFast(out.resolve("word-vectors.bin"), out.resolve("vocabulary.bin"));
        Log.info(sw.elapsed(TimeUnit.MILLISECONDS));
    }

}
