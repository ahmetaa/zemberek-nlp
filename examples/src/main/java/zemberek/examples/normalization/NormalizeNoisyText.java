package zemberek.examples.normalization;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import zemberek.lm.compression.SmoothLm;
import zemberek.morphology.TurkishMorphology;
import zemberek.normalization.TurkishSentenceNormalizer;

public class NormalizeNoisyText {

  public static void main(String[] args) throws IOException {

    String[] examples = {
        "Tmm, yarin havuza giricem ve aksama kadar yaticam :)",
        "ah aynen ya annemde fark ettı siz evinizden cıkmayın diyo",
        "gercek mı bu? Yuh! Artık unutulması bile beklenmiyo",
        "Hayır hayat telaşm olmasa alacam buraları gökdelen dikicem."
    };

    Path lookupRoot = Paths.get("/media/ahmetaa/depo/normalization/test-small");
    Path lmPath = Paths.get("/media/ahmetaa/depo/normalization/lm.slm");
    SmoothLm lm = SmoothLm.builder(lmPath).logBase(Math.E).build();
    TurkishMorphology morphology = TurkishMorphology.createWithDefaults();
    TurkishSentenceNormalizer normalizer = new
        TurkishSentenceNormalizer(morphology, lookupRoot, lm);

    for (String example : examples) {
      System.out.println(example);
      System.out.println(String.join(" ",normalizer.normalize(example)));
      System.out.println();
    }

  }

}
