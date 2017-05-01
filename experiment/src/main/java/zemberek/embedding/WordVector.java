package zemberek.embedding;

import zemberek.core.math.FloatArrays;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class WordVector {
    public final String word;
    private float[] vector;
    private float c;  // sum of the square of all vector values

    public WordVector(String word, float[] vector) {
        this.word = word;
        this.vector = vector;

        float sum = 0;
        for (float v : vector) {
            sum += v * v;
        }
        c = (float) Math.sqrt(sum);
    }

    public WordVector(String word) {
        this.word = word;
    }

    public float cosDistance(WordVector v) {
        float tmp = 0;
        for (int i = 0; i < vector.length; i++) {
            tmp += (vector[i] * v.vector[i]);
        }
        return tmp / (c * v.c);
    }

    public static void writeAsTxt(List<WordVector> list, Path out) throws IOException {
        try (PrintWriter pw = new PrintWriter(out.toFile())) {
            for (WordVector word : list) {
                pw.print(word.word);
                for (float f : word.vector) {
                    pw.print(" " + f);
                }
                pw.println();
            }
        }
    }

    public static List<WordVector> loadFromText(Path txtFile) throws IOException {
        List<WordVector> words = new ArrayList<>();
        List<String> lines = Files.readAllLines(txtFile);
        int lineCount = 0;
        for (String line : lines) {
            if (lineCount++ == 0) { // skip first line.
                continue;
            }
            line = line.trim();
            int index = line.indexOf(' ');
            String word = line.substring(0, index);
            String floats = line.substring(index + 1);
            float[] vector = FloatArrays.fromString(floats, " ");//
            words.add(new WordVector(word, vector));
        }
        return words;
    }

    public static void writeAsBinary(List<WordVector> wordVectors, Path out) throws IOException {
        try (DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(out.toFile())))) {
            dos.writeInt(wordVectors.size());
            dos.writeInt(wordVectors.get(0).vector.length);
            for (WordVector word : wordVectors) {
                dos.writeUTF(word.word);
                for (float f : word.vector) {
                    dos.writeFloat(f);
                }
            }
        }
    }

    public static List<WordVector> loadFromBinary(Path binFile) throws IOException {
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(binFile.toFile())))) {
            int wordSize = in.readInt();
            int vectorSize = in.readInt();
            List<WordVector> words = new ArrayList<>(wordSize);
            for (int i = 0; i < wordSize; i++) {
                String s = in.readUTF();
                float[] v = FloatArrays.deserializeRaw(in, vectorSize);
                words.add(new WordVector(s, v));
            }
            return words;

        }
    }
}
