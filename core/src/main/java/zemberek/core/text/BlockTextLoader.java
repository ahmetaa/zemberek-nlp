package zemberek.core.text;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class BlockTextLoader implements Iterable<List<String>> {

    private Path path;
    private int blockSize;
    private Charset charset;

    public BlockTextLoader(Path path, Charset charset, int blockSize) throws IOException {
        this.path = path;
        this.charset = charset;
        this.blockSize = blockSize;
    }

    public BlockTextLoader(Path path, int blockSize) throws IOException {
        this(path, StandardCharsets.UTF_8, blockSize);
    }

    @Override
    public Iterator<List<String>> iterator() {
        try {
            BufferedReader reader = Files.newBufferedReader(path, charset);
            return new TextIterator(reader);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public Iterator<List<String>> iteratorFromCharIndex(long charIndex) {
        try {
            BufferedReader reader = Files.newBufferedReader(path, charset);
            long k = reader.skip(charIndex);
            if (k != charIndex) {
                throw new IllegalStateException("Cannot skip " + charIndex + " skip returned " + k);
            }
            if (charIndex != 0) { // skip first line
                reader.readLine();
            }
            return new TextIterator(reader);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private class TextIterator implements Iterator<List<String>>, AutoCloseable {

        private BufferedReader br;
        List<String> currentBlock;
        boolean finished = false;

        public TextIterator(BufferedReader br) {
            this.br = br;
        }

        @Override
        public boolean hasNext() {
            if (finished) {
                return false;
            }
            int lineCounter = 0;
            currentBlock = new ArrayList<>(blockSize);
            String line;
            try {
                while (lineCounter < blockSize) {
                    line = br.readLine();
                    if (line != null) {
                        currentBlock.add(line);
                    } else {
                        br.close();
                        finished = true;
                        break;
                    }
                    lineCounter++;
                }
                return currentBlock.size() > 0;
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
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

}
