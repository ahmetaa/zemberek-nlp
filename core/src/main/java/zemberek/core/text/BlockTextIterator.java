package zemberek.core.text;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Provides blocks of text.
 */
public class BlockTextIterator implements Iterator<List<String>>, AutoCloseable {

    List<String> all;
    public int blockSize;
    int pointer;
    public int totalBlockSize;
    List<String> currentBlock;
    BufferedReader br;

    public BlockTextIterator(List<String> all) {
        this(all, 10000);
    }

    /**
     * Loads lines from `path`. Sets block size to 10000.
     * If block size is larger than lines in file, it is set to file line count.
     *
     * @throws IOException
     */
    public BlockTextIterator(Path path) throws IOException {
        this(path, 10000);
    }

    /**
     * Sets block size to `blockSize`.
     * If block size is larger than lines in file, it is set to file line count.
     */
    public BlockTextIterator(List<String> all, int blockSize) {
        this.all = all;
        this.blockSize = blockSize >= all.size() ? all.size() : blockSize;
        int t = all.size() / blockSize;
        totalBlockSize = t * blockSize < all.size() ? t + 1 : t;
    }

    /**
     * Loads lines from `path`. Sets block size to `blockSize`.
     * If block size is larger than lines in file, it is set to file line count.
     *
     * @throws IOException
     */
    public BlockTextIterator(Path path, int blockSize) throws IOException {
        long all = countLines(path);
        int t = (int) (all / blockSize);
        this.blockSize = blockSize >= all ? (int) all : blockSize;
        totalBlockSize = t * blockSize < all ? t + 1 : t;
        br = Files.newBufferedReader(path, StandardCharsets.UTF_8);
    }

    private long countLines(Path p) throws IOException {
        long lineCount = 0;
        try (BufferedReader br = Files.newBufferedReader(p, StandardCharsets.UTF_8)) {
            while (br.readLine() != null) {
                lineCount++;
            }
        }
        return lineCount;
    }

    @Override
    public boolean hasNext() {
        if (br != null) {
            int blockCounter = 0;
            String line;
            currentBlock = new ArrayList<>(blockSize);
            while (blockCounter < blockSize) {
                try {
                    line = br.readLine();
                    if (line != null) {
                        currentBlock.add(line);
                    } else {
                        break;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    return false;
                }
                blockCounter++;
            }
            return currentBlock.size() > 0;
        } else {
            if (pointer >= all.size())
                return false;
            int end = pointer + blockSize > all.size() ? all.size() : pointer + blockSize;
            currentBlock = all.subList(pointer, end);
            pointer += blockSize;
            return true;
        }
    }

    @Override
    public List<String> next() {
        return currentBlock;
    }

    @Override
    public void close() throws Exception {
        if (br != null) {
            br.close();
        }
    }
}
