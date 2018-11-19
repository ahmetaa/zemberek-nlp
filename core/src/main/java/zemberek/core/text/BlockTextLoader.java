package zemberek.core.text;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import zemberek.core.logging.Log;

public class BlockTextLoader implements Iterable<TextChunk> {

  List<Path> corpusPaths;
  int blockSize;

  public List<Path> getCorpusPaths() {
    // defensive copy.
    return new ArrayList<>(corpusPaths);
  }

  public int getBlockSize() {
    return blockSize;
  }

  public int pathCount() {
    return corpusPaths.size();
  }

  BlockTextLoader(List<Path> corpusPaths, int blockSize) {
    corpusPaths.sort(Comparator.comparing(a -> a.toFile().getAbsolutePath()));
    this.corpusPaths = corpusPaths;
    this.blockSize = blockSize;
  }

  public static BlockTextLoader fromPaths(List<Path> corpora) {
    return new BlockTextLoader(corpora, _SingleLoader.DEFAULT_BLOCK_SIZE);
  }

  public static BlockTextLoader fromDirectory(Path directoryPath) throws IOException {
    List<Path> paths = Files.walk(directoryPath, 1)
        .filter(s -> s.toFile().isFile())
        .collect(Collectors.toList());
    return new BlockTextLoader(paths, _SingleLoader.DEFAULT_BLOCK_SIZE);
  }

  public static BlockTextLoader fromPaths(List<Path> corpora, int blockSize) {
    return new BlockTextLoader(corpora, blockSize);
  }

  public static BlockTextLoader fromPath(Path corpus, int blockSize) {
    return new BlockTextLoader(Collections.singletonList(corpus), blockSize);
  }

  public static BlockTextLoader fromDirectoryRoot(
      Path corporaRoot,
      Path folderListFile,
      int blockSize) throws IOException {
    List<String> rootNames = TextIO.loadLines(folderListFile, "#");
    List<Path> roots = new ArrayList<>();
    rootNames.forEach(s -> roots.add(corporaRoot.resolve(s)));

    List<Path> corpora = new ArrayList<>();
    for (Path corpusRoot : roots) {
      corpora.addAll(Files.walk(corpusRoot, 1)
          .filter(s -> s.toFile().isFile())
          .collect(Collectors.toList()));
    }
    corpora.sort(Comparator.comparing(a -> a.toFile().getAbsolutePath()));
    Log.info("There are %d corpus files.", corpora.size());
    return new BlockTextLoader(corpora, blockSize);
  }

  @Override
  public Iterator<TextChunk> iterator() {
    return new CorpusLinesIterator(new ArrayDeque<>(corpusPaths));
  }

  public static Iterator<TextChunk> singlePathIterator(Path path, int blockSize) {
    return new _SingleLoader(path, blockSize).iterator();
  }

  public static Iterator<TextChunk> iteratorFromCharIndex(
      Path path,
      int blockSize,
      long charIndex) {
    return new _SingleLoader(path, blockSize).iteratorFromCharIndex(charIndex);
  }

  class CorpusLinesIterator implements Iterator<TextChunk> {

    ArrayDeque<Path> paths;
    Path currentPath;
    _SingleLoader loader;
    Iterator<TextChunk> iterator;
    TextChunk current;
    int index;
    int sourceIndex;

    CorpusLinesIterator(ArrayDeque<Path> paths) {
      this.paths = paths;
    }

    @Override
    public boolean hasNext() {

      if (loader == null) {
        nextPath();
      }
      if (iterator.hasNext()) {
        current = iterator.next();
        index++;
        return true;
      }

      if (paths.isEmpty()) {
        return false;
      }

      nextPath();
      if (iterator.hasNext()) {
        index = 0;
        current = iterator.next();
        index++;
        return true;
      } else {
        return false;
      }
    }

    private void nextPath() {
      Path p = paths.remove();
      loader = new _SingleLoader(p, sourceIndex, blockSize);
      sourceIndex++;
      currentPath = p;
      iterator = loader.iterator();
    }

    @Override
    public TextChunk next() {
      return current;
    }
  }

  private static class _SingleLoader implements Iterable<TextChunk> {

    // by default load 10,000 lines.
    static final int DEFAULT_BLOCK_SIZE = 10_000;

    final Path path;
    final int blockSize;
    final Charset charset;
    final int sourceIndex;

    _SingleLoader(Path path, Charset charset, int sourceIndex, int blockSize) {
      this.path = path;
      this.charset = charset;
      this.blockSize = blockSize;
      this.sourceIndex = sourceIndex;
    }

    _SingleLoader(Path path, int blockSize) {
      this(path, StandardCharsets.UTF_8, 0, blockSize);
    }

    _SingleLoader(Path path, int sourceIndex, int blockSize) {
      this(path, StandardCharsets.UTF_8, sourceIndex, blockSize);
    }

    /**
     * Returns an Iterator that loads [blocksize] lines in each iteration.
     */
    @Override
    public Iterator<TextChunk> iterator() {
      try {
        BufferedReader reader = Files.newBufferedReader(path, charset);
        return new TextIterator(reader);
      } catch (IOException e) {
        e.printStackTrace();
        throw new RuntimeException(e);
      }
    }

    /**
     * Returns an Iterator that loads [blocksize] lines in each iteration. It starts loading from
     * [charIndex] value of the content.
     */
    Iterator<TextChunk> iteratorFromCharIndex(long charIndex) {
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

    private class TextIterator implements Iterator<TextChunk>, AutoCloseable {

      int blockIndex = 0;
      List<String> currentBlock;
      boolean finished = false;
      private BufferedReader br;

      TextIterator(BufferedReader br) {
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
      public TextChunk next() {
        TextChunk chunk = new TextChunk(
            path.toFile().getName(),
            sourceIndex,
            blockIndex,
            currentBlock);
        blockIndex++;
        return chunk;
      }

      @Override
      public void close() throws Exception {
        if (br != null) {
          br.close();
        }
      }
    }
  }

}
