package zemberek.grpc.server;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.lexicon.RootLexicon;
import zemberek.tokenization.TurkishTokenizer;

/**
 * Tentative class for holding shareable objects.
 */
public class ZemberekContext {

  final TurkishMorphology morphology;
  final TurkishTokenizer tokenizer;
  ZemberekGrpcConfiguration configuration;

  public ZemberekContext() {
    this(null);
  }

  public ZemberekContext(ZemberekGrpcConfiguration configuration) {
    tokenizer = TurkishTokenizer.ALL;
    this.configuration = configuration;
    this.morphology = createMorphology(configuration);
  }

  private static TurkishMorphology createMorphology(ZemberekGrpcConfiguration configuration) {
    if (configuration == null
        || (configuration.getExtraDictionaries().isEmpty() && !configuration.isIgnoreDiacritics())) {
      return TurkishMorphology.createWithDefaults();
    }
    try {
      RootLexicon.Builder lexiconBuilder = RootLexicon.builder().addDefaultLexicon();
      List<Path> extraDicts = configuration.getExtraDictionaries();
      if (!extraDicts.isEmpty()) {
        lexiconBuilder.addTextDictionaries(extraDicts.toArray(new Path[0]));
      }
      TurkishMorphology.Builder morphBuilder = TurkishMorphology.builder(lexiconBuilder.build());
      if (configuration.isIgnoreDiacritics()) {
        morphBuilder.ignoreDiacriticsInAnalysis();
      }
      return morphBuilder.build();
    } catch (IOException e) {
      throw new RuntimeException("Cannot load configured extra dictionaries.", e);
    }
  }

  public TurkishMorphology getMorphology() {
    return morphology;
  }

  public ZemberekGrpcConfiguration getConfiguration() {
    return configuration;
  }
}
