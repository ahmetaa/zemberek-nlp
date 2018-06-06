package zemberek.corpus;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import zemberek.core.logging.Log;

public class CorpusDbApp {

  CorpusDb db;

  public CorpusDbApp(CorpusDb db) {
    this.db = db;
  }

  public void addCorpora(List<Path> corpora) throws IOException {
    for (Path path : corpora) {
      Log.info("Adding %s", path);
      db.addDocs(path);
    }
  }


  public static void main(String[] args) throws Exception {
    Path dbRoot = Paths.get("/home/ahmetaa/data/zemberek/corpus-db");
    CorpusDb db = new CorpusDb(dbRoot);

    Path corporaRoot = Paths.get("/home/ahmetaa/data/zemberek/data/corpora/open-subtitles");
    List<Path> corpora = Files.walk(corporaRoot, 1)
        .filter(s -> s.toFile().isFile())
        .collect(Collectors.toList());

    CorpusDbApp app = new CorpusDbApp(db);
    app.addCorpora(corpora);
  }

}
