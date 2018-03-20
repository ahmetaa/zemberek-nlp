package zemberek.scratchpad;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import zemberek.langid.LanguageIdentifier;

public class ProperNounLanguage {

  public static void main(String[] args) throws IOException {
    List<String> candidates = Files.readAllLines(
        Paths.get("/home/ahmetaa/projects/zemberek-nlp/zemberek.proper.vocab")
    );

    List<String> potentiallyForeign = new ArrayList<>();
    LanguageIdentifier lid = LanguageIdentifier.fromInternalModelGroup("tr_group");
    for (String candidate : candidates) {
      String l = lid.identify(candidate);
      if (l.equals("en")) {
        potentiallyForeign.add(candidate);
      }
    }
    Files.write(
        Paths.get("/home/ahmetaa/projects/zemberek-nlp/zemberek.proper.vocab.en"),
        potentiallyForeign
    );

  }

}
