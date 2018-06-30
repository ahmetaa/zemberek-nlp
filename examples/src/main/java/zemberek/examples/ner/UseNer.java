package zemberek.examples.ner;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import zemberek.morphology.TurkishMorphology;
import zemberek.ner.NamedEntity;
import zemberek.ner.NerSentence;
import zemberek.ner.PerceptronNer;

public class UseNer {

  public static void main(String[] args) throws IOException {

    // assumes you generated a model in my-model directory.
    Path modelRoot = Paths.get("my-model");

    TurkishMorphology morphology = TurkishMorphology.createWithDefaults();

    PerceptronNer ner = PerceptronNer.loadModel(modelRoot, morphology);

    String sentence = "Ali Kaan yarın İstanbul'a gidecek.";

    NerSentence result = ner.findNamedEntities(sentence);

    List<NamedEntity> namedEntities = result.getNamedEntities();

    for (NamedEntity namedEntity : namedEntities) {
      System.out.println(namedEntity);
    }

  }

}
