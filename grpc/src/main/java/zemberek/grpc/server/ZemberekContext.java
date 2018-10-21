package zemberek.grpc.server;

import zemberek.morphology.TurkishMorphology;
import zemberek.tokenization.TurkishTokenizer;

/**
 * Tentative class for holding shareable objects.
 */
public class ZemberekContext {

  final TurkishMorphology morphology;
  final TurkishTokenizer tokenizer;
  ZemberekGrpcConfiguration configuration;

  public ZemberekContext() {
    tokenizer = TurkishTokenizer.ALL;
    morphology = TurkishMorphology.createWithDefaults();
  }

  public ZemberekContext(ZemberekGrpcConfiguration configuration) {
    tokenizer = TurkishTokenizer.ALL;
    morphology = TurkishMorphology.createWithDefaults();
    this.configuration = configuration;
  }

}
