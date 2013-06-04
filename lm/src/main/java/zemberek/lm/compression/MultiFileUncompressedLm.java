package zemberek.lm.compression;

import com.google.common.base.Splitter;
import com.google.common.io.Closeables;
import com.google.common.io.Files;
import com.google.common.io.LineProcessor;
import zemberek.core.WhiteSpaceTokenizer;
import zemberek.core.quantization.DoubleLookup;
import zemberek.core.quantization.Quantizer;
import zemberek.core.quantization.QuantizerType;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;

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
 * <p/>
 * <p/>[n].prob
 * <p/>int32 count
 * <p/>float32 prob
 * <p/>....
 * <p/>
 * <p/>[n].backoff
 * <p/>int32 count
 * <p/>float32 prob
 * <p/>....
 * <p/>
 * <p/>vocab
 * <p/>int32 count
 * <p/>UTF-8... word0...n
 */
public class MultiFileUncompressedLm {

    int[] counts;
    int order;
    File dir;
    int[] probabilityRankCount;
    int[] backoffRankCount;

    public MultiFileUncompressedLm(File dir) throws IOException {
        this.dir = dir;
        DataInputStream dis = new DataInputStream(new FileInputStream(getFile("info")));
        order = dis.readInt();
        counts = new int[order + 1];
        for (int i = 0; i < order; i++) {
            counts[i + 1] = dis.readInt();
        }
        dis.close();
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
        return new File(dir, n + ".gram");
    }

    public File getProbFile(int n) {
        return new File(dir, n + ".prob");
    }

    public File getProbRankFile(int n) {
        return new File(dir, n + ".prob.rank");
    }

    public File getBackoffRankFile(int n) {
        return new File(dir, n + ".backoff.rank");
    }

    public File getBackoffFile(int n) {
        return new File(dir, n + ".backoff");
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

    public static void main(String[] args) throws IOException {

        // MultiFileUncompressedLm lm = new MultiFileUncompressedLm(new File("/home/ahmetaa/data/lm/kn/tmp"));
        // lm.generateRankFiles(3, 10, QuantizerType.BINNING);


/*
        MultiFileUncompressedLm lm = generate(new File("/home/ahmetaa/data/lm/turkish/22kasim.lm"),
                new File("/home/ahmetaa/data/lm/turkish/multi"), "utf-8");
*/

        //    MultiFileUncompressedLm lm = new MultiFileUncompressedLm(new File("/home/ahmetaa/data/lm/turkish/multi"));

        MultiFileUncompressedLm lm = generate(new File("test/data/tiny.arpa"),
                new File("/home/ahmetaa/data/lm/turkish/tiny"), "utf-8");

        System.out.println(Arrays.toString(lm.counts));
        lm.generateRankFiles(24, QuantizerType.BINNING);

    }

    public void generateRankFiles(int i, int bit, QuantizerType quantizerType) throws IOException {
        if (bit > 24)
            throw new IllegalArgumentException("Cannot generate rank file larger than 24 bits but it is:" + bit);
        System.out.println("Calculating probabilty rank values for :" + i + " Grams");
        File probFile = getProbFile(i);
        generateRankFile(bit, i, probFile, new File(dir, i + ".prob.rank"), quantizerType);
        if (i < counts.length - 1) {
            File backoffFile = getBackoffFile(i);
            System.out.println("Calculating back-off rank values for :" + i + " Grams");
            generateRankFile(bit, i, backoffFile, new File(dir, i + ".backoff.rank"), quantizerType);
        }
    }

    public void generateRankFiles(int bit, QuantizerType quantizerType) throws IOException {
        if (bit > 24)
            throw new IllegalArgumentException("Cannot generate rank file larger than 24 bits but it is:" + bit);
        for (int i = 1; i < counts.length; i++) {
            System.out.println("Calculating probabilty rank values for :" + i + " Grams");
            File probFile = getProbFile(i);
            generateRankFile(bit, i, probFile, new File(dir, i + ".prob.rank"), quantizerType);
            if (i < counts.length - 1) {
                File backoffFile = getBackoffFile(i);
                System.out.println("Calculating back-off rank values for :" + i + " Grams");
                generateRankFile(bit, i, backoffFile, new File(dir, i + ".backoff.rank"), quantizerType);
            }
        }
    }

    private void generateRankFile(int bit, int i, File probFile, File rankFile, QuantizerType quantizerType) throws IOException {
        try (DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(probFile), 100000));
             DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(rankFile), 100000))) {
            int count = dis.readInt();
            Quantizer quantizer = BinaryFloatFileReader.getQuantizer(probFile, bit, quantizerType);
            dos.writeInt(count);
            System.out.println("Writing Ranked file for:" + i);
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
            System.out.println("Writing lookups for:" + i);

            DoubleLookup lookup = quantizer.getDequantizer();
            System.out.println("Lookup size:" + lookup.getRange());
            lookup.save(new File(dir, probFile.getName() + ".lookup"));

        }
    }

    public static MultiFileUncompressedLm generate(File arpaFile, File dir, String encoding) throws IOException {
        if (dir.exists() && !dir.isDirectory()) {
            throw new IllegalArgumentException(dir + " is not a directory!");
        }
        if (!dir.exists() && !dir.mkdirs())
            throw new IllegalArgumentException("Cannot create directory:" + dir);

        long elapsedTime = Files.readLines(arpaFile, Charset.forName(encoding), new ArpaToBinaryConverter(dir));
        System.out.println("generated in " + (double) elapsedTime / 1000d + " seconds");
        return new MultiFileUncompressedLm(dir);
    }

    public File getVocabularyFile() {
        return new File(dir, "vocab");
    }

    private static class ArpaToBinaryConverter implements LineProcessor<Long> {

        Map<String, Integer> unigramIdMap = new HashMap<String, Integer>();
        List<String> vocabulary = new ArrayList<String>();
        int ngramCounter = 0;
        int _n;

        enum State {
            BEGIN, UNIGRAMS, NGRAMS, VOCABULARY
        }

        State state = State.BEGIN;
        List<Integer> ngramCounts = new ArrayList<Integer>();
        boolean started = false;
        File dir;

        DataOutputStream gramOs;
        DataOutputStream probOs;
        DataOutputStream backoffOs;
        int order;
        long start;
        WhiteSpaceTokenizer tokenizer = new WhiteSpaceTokenizer();

        ArpaToBinaryConverter(File dir) throws FileNotFoundException {
            this.dir = dir;
            start = System.currentTimeMillis();
        }

        private void newGramStream(int n) throws IOException {
            if (gramOs != null)
                gramOs.close();
            gramOs = getDos(n + ".gram");
            gramOs.writeInt(n);
            gramOs.writeInt(ngramCounts.get(n - 1));
        }

        private void newProbStream(int n) throws IOException {
            if (probOs != null)
                probOs.close();
            probOs = getDos(n + ".prob");
            probOs.writeInt(ngramCounts.get(n - 1));
        }

        private void newBackoffStream(int n) throws IOException {
            if (backoffOs != null)
                backoffOs.close();
            backoffOs = getDos(n + ".backoff");
            backoffOs.writeInt(ngramCounts.get(n - 1));
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

                        order = ngramCounts.size();
                        DataOutputStream infos = getDos("info");
                        infos.writeInt(order);
                        for (Integer ngramCount : ngramCounts) {
                            infos.writeInt(ngramCount);
                        }
                        infos.close();
                        state = State.UNIGRAMS;
                        newGramStream(1);
                        newProbStream(1);
                        newBackoffStream(1);
                        int i = 0;
                        for (Integer count : ngramCounts) {
                            System.out.println((++i) + "gram count:" + count);
                        }
                        System.out.println("Writing unigrams.");
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
                    if (tokens.length == 3)
                        logBackoff = Float.parseFloat(tokens[_n + 1]);
                    // write unigram id, log-probability and log-backoff value.
                    gramOs.writeInt(ngramCounter);
                    probOs.writeFloat(logProbability);

                    // if there are only ngrams, do not write backoff value.
                    if (ngramCounts.size() > 1)
                        backoffOs.writeFloat(logBackoff);

                    unigramIdMap.put(word, ngramCounter++);
                    vocabulary.add(word);
                    if (ngramCounter == ngramCounts.get(0)) {
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
                            System.out.println("Writing 2-grams.");
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
                        int id = unigramIdMap.get(tokens[i + 1]);
                        gramOs.writeInt(id);
                    }

                    // probabilities

                    probOs.writeFloat(logProbability);
                    if (_n < ngramCounts.size()) {
                        logBackoff = 0;
                        if (tokens.length == _n + 2)
                            logBackoff = Float.parseFloat(tokens[_n + 1]);
                        backoffOs.writeFloat(logBackoff);
                    }

                    if (ngramCounter % 500000 == 0)
                        System.out.print(".");

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
                            System.out.println("\nWriting " + _n + "-grams.");
                        }
                    }
                    break;

                case VOCABULARY:
                    Closeables.close(gramOs, true);
                    Closeables.close(probOs, true);
                    Closeables.close(backoffOs, true);
                    DataOutputStream voDo = getDos("vocab");
                    voDo.writeInt(vocabulary.size());
                    System.out.println("Writing vocabulary.");
                    for (String w : vocabulary) {
                        voDo.writeUTF(w);
                    }
                    voDo.close();
                    return false; // we are done.
            }
            return true;
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
