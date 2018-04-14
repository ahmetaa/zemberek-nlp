package zemberek.morphology._ambiguity;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import zemberek.morphology._analyzer._TurkishMorphologicalAnalyzer;

public class _PerceptronAmbiguityResolverEvaluation {

  public static void main(String[] args) throws IOException {

    Path train = Paths.get("data/ambiguity/aljazeera-train");
    Path dev = Paths.get("data/ambiguity/open-subtitles-test");
    Path model = Paths.get("data/ambiguity/model");
    _TurkishMorphologicalAnalyzer analyzer = _TurkishMorphologicalAnalyzer.createDefault();

    _PerceptronAmbiguityResolver resolver =
        new _PerceptronAmbiguityResolverTrainer(analyzer).train(train, dev, 7);
    resolver.getModel().pruneNearZeroWeights();
    resolver.getModel().saveAsText(model);
    Path test = Paths.get("data/ambiguity/open-subtitles-test");
    resolver.test(test);
  }

}
