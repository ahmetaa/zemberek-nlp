package zemberek.lm.compression;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.io.Closeables;
import com.google.common.io.Files;
import com.google.common.io.LineProcessor;
import zemberek.core.SpaceTabTokenizer;
import zemberek.core.logging.Log;
import zemberek.core.quantization.DoubleLookup;
import zemberek.core.quantization.Quantizer;
import zemberek.core.quantization.QuantizerType;
import zemberek.core.text.TextConverter;
import zemberek.lm.LmVocabulary;

import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * This is a multiple file representation of an uncompressed backoff language model.
 * Files are:
 * <p/>info
 * <p/>int32 order
 * <p/>int32 1 gram count
 * <p/>int32 2 gram count
 * <p/>...
 * <p/>[n].gram
 * <p/>int32 order
 * <p/>int32 count
 * <p/>int32... id[0,..,n]
 * <p>
 * <p/>[n].prob
 * <p/>int32 count
 * <p/>float32 prob
 * <p/>....
 * <p>
 * <p/>[n].backoff
 * <p/>int32 count
 * <p/>float32 prob
 * <p/>....
 * <p>
 * <p/>vocab
 * <p/>int32 count
 * <p/>UTF-8... word0...n
 */
public class MultiFileUncompressedLm {

    public static String INFO_FILE_NAME = "info";
    public static String GRAM_IDS_FILE_SUFFIX = ".gram";
    public static String PROB_FILE_SUFFIX = ".prob";
    public static String BACKOFF_FILE_SUFFIX = ".backoff";
    public static String VOCAB_FILE_NAME = "vocab";

    int[] counts;
    int order;
    File dir;

    public MultiFileUncompressedLm(File dir) throws IOException {
        this.dir = dir;
        try (DataInputStream dis = new DataInputStream(new FileInputStream(getFile(INFO_FILE_NAME)))) {
            order = dis.readInt();
            counts = new int[order + 1];
            for (int i = 0; i < order; i++) {
                counts[i + 1] = dis.readInt();
            }
        }
    }

    public File getLmDir() {
        return dir;
    }

    public int getRankSize(File f) throws IOException {
        DataInputStream dis = new DataInputStream(new FileInputStream(f));
        int count = dis.readInt();
        dis.close();
        return count;
    }

    public int getRankBlockSize(File f) throws IOException {
        DataInputStream dis = new DataInputStream(new FileInputStream(f));
        dis.readInt();
        int blockSize = dis.readInt();
        dis.close();
        return blockSize;
    }

    public File getGramFile(int n) {
        return new File(dir, n + GRAM_IDS_FILE_SUFFIX);
    }

    public File getProbFile(int n) {
        return new File(dir, n + PROB_FILE_SUFFIX);
    }

    public File getProbRankFile(int n) {
        return new File(dir, n + PROB_FILE_SUFFIX + ".rank");
    }

    public File getBackoffRankFile(int n) {
        return new File(dir, n + BACKOFF_FILE_SUFFIX + ".rank");
    }

    public File getBackoffFile(int n) {
        return new File(dir, n + BACKOFF_FILE_SUFFIX);
    }

    public File getProbabilityLookupFile(int n) {
        return new File(dir, getProbFile(n).getName() + ".lookup");
    }

    public File getBackoffLookupFile(int n) {
        return new File(dir, getBackoffFile(n).getName() + ".lookup");
    }

    public int getOrder() {
        return order;
    }

    public int getCount(int n) {
        return counts[n];
    }

    private File getFile(String name) {
        return new File(dir, name);
    }

    public void generateRankFiles(int i, int bit, QuantizerType quantizerType) throws IOException {
        if (bit > 24)
            throw new IllegalArgumentException("Cannot generate rank file larger than 24 bits but it is:" + bit);
        Log.info("Calculating probabilty rank values for :" + i + " Grams");
        File probFile = getProbFile(i);
        generateRankFile(bit, i, probFile, new File(dir, i + PROB_FILE_SUFFIX + ".rank"), quantizerType);
        if (i < counts.length - 1) {
            File backoffFile = getBackoffFile(i);
            Log.info("Calculating back-off rank values for :" + i + " Grams");
            generateRankFile(bit, i, backoffFile, new File(dir, i + BACKOFF_FILE_SUFFIX + ".rank"), quantizerType);
        }
    }

    public void generateRankFiles(int bit, QuantizerType quantizerType) throws IOException {
        if (bit > 24)
            throw new IllegalArgumentException("Cannot generate rank file larger than 24 bits but it is:" + bit);
        for (int i = 1; i < counts.length; i++) {
            Log.info("Calculating probabilty lookup values for :" + i + " Grams");
            File probFile = getProbFile(i);
            generateRankFile(bit, i, probFile, new File(dir, i + PROB_FILE_SUFFIX + ".rank"), quantizerType);
            if (i < counts.length - 1) {
                File backoffFile = getBackoffFile(i);
                Log.info("Calculating lookup values for " + i + " Grams");
                generateRankFile(bit, i, backoffFile, new File(dir, i + BACKOFF_FILE_SUFFIX + ".rank"), quantizerType);
            }
        }
    }

    private void generateRankFile(int bit, int currentOrder, File probFile, File rankFile, QuantizerType quantizerType) throws IOException {
        try (DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(probFile)));
             DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(rankFile)))) {
            int count = dis.readInt();
            Quantizer quantizer = BinaryFloatFileReader.getQuantizer(probFile, bit, quantizerType);
            dos.writeInt(count);
            Log.info("Writing Rank file for " + currentOrder + " grams");
            int bytecount = (bit % 8 == 0 ? bit / 8 : bit / 8 + 1);
            if (bytecount == 0)
                bytecount = 1;
            dos.writeInt(bytecount);
            byte[] bytez = new byte[3];
            for (int j = 0; j < count; j++) {
                final int rank = quantizer.getQuantizationIndex(dis.readFloat());
                switch (bytecount) {
                    case 1:
                        dos.write(rank & 0xff);
                        break;
                    case 2:
                        dos.writeShort(rank & 0xffff);
                        break;
                    case 3:
                        bytez[0] = (byte) ((rank >>> 16) & 0xff);
                        bytez[1] = (byte) ((rank >>> 8) & 0xff);
                        bytez[2] = (byte) (rank & 0xff);
                        dos.write(bytez);
                        break;
                }
            }

            DoubleLookup lookup = quantizer.getDequantizer();
            Log.info("Writing lookups for " + currentOrder + " grams. Size= " + lookup.getRange());
            lookup.save(new File(dir, probFile.getName() + ".lookup"));

        }
    }

    public static MultiFileUncompressedLm generate(
            File arpaFile,
            File dir,
            String encoding,
            int fractionDigits) throws IOException {
        return generate(arpaFile, dir, encoding, fractionDigits, null);
    }

    public static MultiFileUncompressedLm generate(
            File arpaFile,
            File dir,
            String encoding,
            int fractionDigits,
            TextConverter transformer) throws IOException {
        if (dir.exists() && !dir.isDirectory()) {
            throw new IllegalArgumentException(dir + " is not a directory!");
        } else java.nio.file.Files.createDirectories(dir.toPath());

        long elapsedTime = Files.readLines(arpaFile, Charset.forName(encoding), new ArpaToBinaryConverter(dir, fractionDigits, transformer));
        Log.info("Multi file uncompressed binary model is generated in " + (double) elapsedTime / 1000d + " seconds");
        if (!new File(dir, INFO_FILE_NAME).exists()) {
            throw new IllegalStateException("Arpa file " + arpaFile + " cannot be parsed. Possibly incorrectly formatted or empty file is supplied.");
        }
        return new MultiFileUncompressedLm(dir);
    }

    public File getVocabularyFile() {
        return new File(dir, VOCAB_FILE_NAME);
    }

    private static class ArpaToBinaryConverter implements LineProcessor<Long> {

        public static final int DEFAULT_UNKNOWN_PROBABILTY = -20;
        int ngramCounter = 0;
        int _n;

        double fractionMultiplier;

        enum State {
            BEGIN, UNIGRAMS, NGRAMS, VOCABULARY
        }

        State state = State.BEGIN;
        List<Integer> ngramCounts = new ArrayList<>();
        boolean started = false;
        File dir;

        DataOutputStream gramOs;
        DataOutputStream probOs;
        DataOutputStream backoOffs;
        int order;
        long start;
        SpaceTabTokenizer tokenizer = new SpaceTabTokenizer();
        LmVocabulary.Builder vocabularyBuilder = new LmVocabulary.Builder();

        // This will be generated after reading unigrams.
        LmVocabulary lmVocabulary;
        TextConverter textConverter;


        ArpaToBinaryConverter(File dir, int fractionDigitCount, TextConverter textConverter) throws FileNotFoundException {
            Log.info("Generating multi file uncompressed language model from Arpa file in directory: %s", dir.getAbsolutePath());
            this.dir = dir;
            if (fractionDigitCount >= 0) {
                fractionMultiplier = Math.pow(10, fractionDigitCount);
            } else fractionMultiplier = 0;
            this.textConverter = textConverter;
            start = System.currentTimeMillis();
        }

        private float reduceFraction(float input) {
            if (fractionMultiplier != 0) {
                return (float) (Math.round(input * fractionMultiplier) / fractionMultiplier);
            } else return input;
        }

        private void newGramStream(int n) throws IOException {
            if (gramOs != null)
                gramOs.close();
            gramOs = getDos(n + GRAM_IDS_FILE_SUFFIX);
            gramOs.writeInt(n);
            gramOs.writeInt(ngramCounts.get(n - 1));
        }

        private void newProbStream(int n) throws IOException {
            if (probOs != null)
                probOs.close();
            probOs = getDos(n + PROB_FILE_SUFFIX);
            probOs.writeInt(ngramCounts.get(n - 1));
        }

        private void newBackoffStream(int n) throws IOException {
            if (backoOffs != null)
                backoOffs.close();
            backoOffs = getDos(n + BACKOFF_FILE_SUFFIX);
            backoOffs.writeInt(ngramCounts.get(n - 1));
        }

        public boolean processLine(String s) throws IOException {
            String clean = s.trim();
            switch (state) {
                // read n value and ngram counts.
                case BEGIN:
                    if (clean.startsWith("\\data\\"))
                        started = true;
                    else if (started && clean.startsWith("ngram")) {
                        started = true;
                        int count = 0, i = 0;
                        for (String str : Splitter.on("=").trimResults().split(clean)) {
                            if (i++ == 0)
                                continue;
                            count = Integer.parseInt(str);
                        }
                        ngramCounts.add(count);
                    } else if (started) {
                        state = State.UNIGRAMS;
                        newGramStream(1);
                        newProbStream(1);
                        newBackoffStream(1);
                        Log.info("Gram counts in Arpa file: " + Joiner.on(" ").join(ngramCounts));
                        Log.info("Writing unigrams.");
                        _n++;
                    }
                    break;

                // read ngrams. if unigram values, we store the strings and related indexes.
                case UNIGRAMS:
                    if (clean.length() == 0 || clean.startsWith("\\")) {
                        break;
                    }
                    String[] tokens = tokenizer.split(clean);
                    // parse probabilty
                    float logProbability = Float.parseFloat(tokens[0]);

                    String word = tokens[1];
                    float logBackoff = 0;
                    if (tokens.length == 3) {
                        logBackoff = Float.parseFloat(tokens[_n + 1]);
                    }
                    // write unigram id, log-probability and log-backoff value.
                    int wordIndex = vocabularyBuilder.add(word);
                    gramOs.writeInt(wordIndex);
                    probOs.writeFloat(reduceFraction(logProbability));

                    // if there are only ngrams, do not write backoff value.
                    if (ngramCounts.size() > 1) {
                        backoOffs.writeFloat(reduceFraction(logBackoff));
                    }

                    ngramCounter++;
                    if (ngramCounter == ngramCounts.get(0)) {
                        handleSpecialToken("<unk>");
                        handleSpecialToken("</s>");
                        handleSpecialToken("<s>");
                        lmVocabulary = vocabularyBuilder.generate();

                        ngramCounts.set(0, lmVocabulary.size());

                        // we write info file after reading unigrams because we may add special tokens to unigrams
                        // so count information may have been changed.
                        order = ngramCounts.size();
                        try (DataOutputStream infos = getDos(INFO_FILE_NAME)) {
                            infos.writeInt(order);
                            for (Integer ngramCount : ngramCounts) {
                                infos.writeInt(ngramCount);
                            }
                        }

                        ngramCounter = 0;
                        state = State.NGRAMS;
                        _n++;
                        // if there is only unigrams in the arpa file, exit
                        if (ngramCounts.size() == 1) {
                            state = State.VOCABULARY;
                        } else {
                            newGramStream(2);
                            newProbStream(2);
                            if (order > 2)
                                newBackoffStream(2);
                            Log.info("Writing 2-grams.");
                        }
                    }
                    break;

                case NGRAMS:
                    if (clean.length() == 0 || clean.startsWith("\\")) {
                        break;
                    }
                    tokens = tokenizer.split(clean);
                    logProbability = Float.parseFloat(tokens[0]);

                    for (int i = 0; i < _n; i++) {
                        int id = lmVocabulary.indexOf(tokens[i + 1]);
                        gramOs.writeInt(id);
                    }

                    // probabilities

                    probOs.writeFloat(reduceFraction(logProbability));
                    if (_n < ngramCounts.size()) {
                        logBackoff = 0;
                        if (tokens.length == _n + 2)
                            logBackoff = Float.parseFloat(tokens[_n + 1]);
                        backoOffs.writeFloat(reduceFraction(logBackoff));
                    }

                    if (ngramCounter > 0 && ngramCounter % 1000000 == 0)
                        Log.info(ngramCounter + " grams are written so far.");

                    ngramCounter++;
                    if (ngramCounter == ngramCounts.get(_n - 1)) {
                        ngramCounter = 0;
                        // if there is no more ngrams, exit
                        if (ngramCounts.size() == _n) {
                            state = State.VOCABULARY;
                        } else {
                            _n++;
                            newGramStream(_n);
                            newProbStream(_n);
                            if (order > _n) {
                                newBackoffStream(_n);
                            }
                            Log.info("Writing " + _n + "-grams.");
                        }
                    }
                    break;

                case VOCABULARY:
                    Closeables.close(gramOs, true);
                    Closeables.close(probOs, true);
                    Closeables.close(backoOffs, true);
                    Log.info("Writing model vocabulary.");
                    if (textConverter != null) {
                        lmVocabulary = new LmVocabulary.Builder().addAll(lmVocabulary.words()).generateDecoded(textConverter);
                    }
                    lmVocabulary.saveBinary(new File(dir, VOCAB_FILE_NAME));
                    return false; // we are done.
            }
            return true;
        }

        // adds undefined specials token with default probability.
        private void handleSpecialToken(String word) throws IOException {
            if (vocabularyBuilder.indexOf(word) == -1 && vocabularyBuilder.indexOf(word.toUpperCase()) == -1) {
                Log.warn("Special token " + word +
                        " does not exist in model. It is added with default [unknown word] probability: " +
                        DEFAULT_UNKNOWN_PROBABILTY);
                int index = vocabularyBuilder.add(word);
                gramOs.writeInt(index);
                probOs.writeFloat(DEFAULT_UNKNOWN_PROBABILTY);
                if (ngramCounts.size() > 1)
                    backoOffs.writeFloat(0);
            }
        }

        private DataOutputStream getDos(String name) throws FileNotFoundException {
            return new DataOutputStream(new BufferedOutputStream(new FileOutputStream(new File(dir, name)), 100000));
        }

        public Long getResult() {
            // just return the time..
            return System.currentTimeMillis() - start;
        }
    }
}
