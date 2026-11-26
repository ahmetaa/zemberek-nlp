package zemberek.examples.normalization;

import java.io.IOException;
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

    TurkishMorphology morphology = TurkishMorphology.createWithDefaults();
    TurkishSentenceNormalizer normalizer =
        TurkishSentenceNormalizer.createWithDefaults(morphology);

    for (String example : examples) {
      System.out.println(example);
      System.out.println(normalizer.normalize(example));
      System.out.println();
    }

  }

}
