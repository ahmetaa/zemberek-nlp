package zemberek.examples.normalization;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import zemberek.morphology.TurkishMorphology;
import zemberek.normalization.TurkishSentenceNormalizer;

public class NormalizeNoisyText {

  public static void main(String[] args) throws IOException {

    String[] examples = {
        "Yrn okua gidicem",
        "Tmm, yarin havuza giricem ve aksama kadar yaticam :)",
        "ah aynen ya annemde fark ettı siz evinizden cıkmayın diyo",
        "gercek mı bu? Yuh! Artık unutulması bile beklenmiyo",
        "Hayır hayat telaşm olmasa alacam buraları gökdelen dikicem.",
        "yok hocam kesınlıkle oyle birşey yok",
        "herseyi soyle hayatında olmaması gerek bence boyle ınsanların falan baskı yapıyosa"
    };

    // change paths with your normalization data root folder and language model file paths.
    // Example: https://drive.google.com/drive/folders/1tztjRiUs9BOTH-tb1v7FWyixl-iUpydW
    // download lm and normalization folders to some local directory.

    Path zemberekDataRoot = Paths.get("/home/aaa/zemberek-data");

    Path lookupRoot = zemberekDataRoot.resolve("normalization");
    Path lmPath = zemberekDataRoot.resolve("lm/lm.2gram.slm");
    TurkishMorphology morphology = TurkishMorphology.createWithDefaults();
    TurkishSentenceNormalizer normalizer = new
        TurkishSentenceNormalizer(morphology, lookupRoot, lmPath);

    for (String example : examples) {
      System.out.println(example);
      System.out.println(normalizer.normalize(example));
      System.out.println();
    }

  }

}
