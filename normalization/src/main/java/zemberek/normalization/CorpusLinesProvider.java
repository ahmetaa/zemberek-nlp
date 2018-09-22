package zemberek.normalization;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import zemberek.core.logging.Log;
import zemberek.core.text.BlockTextLoader;
import zemberek.core.text.TextIO;
import zemberek.core.text.TextUtil;
import zemberek.tokenization.TurkishSentenceExtractor;

public class CorpusLinesProvider implements Iterable<TextChunk> {

  public static final int DEFAULT_BLOCK_SIZE = 10_000;

  List<Path> corpusPaths;
  int blockSize;

  public List<Path> getCorpusPaths() {
    // defensive copy.
    return new ArrayList<>(corpusPaths);
  }

  public int pathCount() {
    return corpusPaths.size();
  }

  CorpusLinesProvider(List<Path> corpusPaths, int blockSize) {
    this.corpusPaths = corpusPaths;
    this.blockSize = blockSize;
  }

  static CorpusLinesProvider fromCorpusPaths(List<Path> corpora) {
    return new CorpusLinesProvider(corpora, DEFAULT_BLOCK_SIZE);
  }


  static CorpusLinesProvider fromCorporaRoot(Path corporaRoot, Path folderListFile, int blockSize)
      throws IOException {
    List<String> rootNames = TextIO.loadLines(folderListFile, "#");
    List<Path> roots = new ArrayList<>();
    rootNames.forEach(s -> roots.add(corporaRoot.resolve(s)));

    List<Path> corpora = new ArrayList<>();
    for (Path corpusRoot : roots) {
      corpora.addAll(Files.walk(corpusRoot, 1)
          .filter(s -> s.toFile().isFile())
          .collect(Collectors.toList()));
    }
    Log.info("There are %d corpus files.", corpora.size());
    return new CorpusLinesProvider(corpora, blockSize);
  }

  @Override
  public Iterator<TextChunk> iterator() {
    return new CorpusLinesIterator(new ArrayDeque<>(corpusPaths));
  }

  class CorpusLinesIterator implements Iterator<TextChunk> {

    ArrayDeque<Path> paths;
    BlockTextLoader loader;
    Iterator<List<String>> iterator;
    TextChunk current;
    String currentId;
    int index;

    CorpusLinesIterator(ArrayDeque<Path> paths) {
      this.paths = paths;
    }

    @Override
    public boolean hasNext() {

      if (loader == null) {
        nextPath();
      }
      if (iterator.hasNext()) {
        current = new TextChunk(currentId + "-" + index, iterator.next());
        index++;
        return true;
      }

      if (paths.isEmpty()) {
        return false;
      }

      nextPath();
      if (iterator.hasNext()) {
        index = 0;
        current = new TextChunk(currentId + "-" + index, iterator.next());
        index++;
        return true;
      } else {
        return false;
      }
    }

    private void nextPath() {
      Path p = paths.remove();
      loader = new BlockTextLoader(p, blockSize);
      iterator = loader.iterator();
      currentId = p.toString();
    }

    @Override
    public TextChunk next() {
      return current;
    }
  }

  static List<String> cleanAndExtractSentences(List<String> input) {
    List<String> lines = input.stream()
        .filter(s -> !s.startsWith("<"))
        .map(TextUtil::normalizeSpacesAndSoftHyphens)
        .collect(Collectors.toList());
    return TurkishSentenceExtractor.DEFAULT.fromParagraphs(lines);
  }

}
