package zemberek.dependency;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Objects;
import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.analysis.SentenceAnalysis;
import zemberek.morphology.analysis.format.ConlluFormatter;
import zemberek.morphology.analysis.format.ConlluSentence;

/**
 * High-level Turkish Universal Dependencies v2 dependency parser backed by
 * {@link TurkishMorphology}, {@link ConlluFormatter}, and {@link BiaffineDependencyModel}.
 *
 * <p>Typical usage:
 * <pre>{@code
 * TurkishMorphology morphology = TurkishMorphology.createWithDefaults();
 * TurkishDependencyParser parser = TurkishDependencyParser.fromModelFile(
 *     morphology, Paths.get("/path/to/dep_boun_v1.bin.gz"));
 *
 * DependencyTree tree = parser.parse("Prof. Dr. Fethi Bey saat 14.30'da Türkiye'ye döndü.");
 * System.out.println(tree.toConllu());
 * }</pre>
 */
public final class TurkishDependencyParser implements DependencyParser {

  /** Default classpath resource path for the bundled UD_Turkish-BOUN model. */
  public static final String DEFAULT_MODEL_RESOURCE = "/tr/dependency/dep_boun_v1.bin.gz";

  private final TurkishMorphology morphology;
  private final BiaffineDependencyModel model;
  private final ConlluFormatter formatter;

  public TurkishDependencyParser(
      TurkishMorphology morphology,
      BiaffineDependencyModel model,
      ConlluFormatter formatter) {
    this.morphology = Objects.requireNonNull(morphology, "morphology");
    this.model = Objects.requireNonNull(model, "model");
    this.formatter = Objects.requireNonNull(formatter, "formatter");
  }

  /**
   * Creates a parser backed by the bundled default {@code UD_Turkish-BOUN} model at
   * {@link #DEFAULT_MODEL_RESOURCE}.
   *
   * <p>Note: the default binary model is not yet bundled in the JAR until Phase 4 training
   * completes; until then, load a trained model via {@link #fromModelFile(TurkishMorphology, Path)}.
   */
  public static TurkishDependencyParser createWithDefaults(TurkishMorphology morphology)
      throws IOException {
    return fromResource(morphology, DEFAULT_MODEL_RESOURCE);
  }

  /** Loads a {@code ZDEP} model (`.bin` or `.bin.gz`) from the local filesystem. */
  public static TurkishDependencyParser fromModelFile(
      TurkishMorphology morphology, Path modelFile) throws IOException {
    return new TurkishDependencyParser(
        morphology, BiaffineDependencyModel.load(modelFile), ConlluFormatter.bounDefault());
  }

  /** Loads a {@code ZDEP} model from a classpath resource. */
  public static TurkishDependencyParser fromResource(
      TurkishMorphology morphology, String resourcePath) throws IOException {
    InputStream is = TurkishDependencyParser.class.getResourceAsStream(resourcePath);
    if (is == null) {
      throw new IOException(
          "Dependency model resource not found on classpath: " + resourcePath
              + ". Train or supply a model via TurkishDependencyParser.fromModelFile(morphology, path).");
    }
    try (InputStream closeable = is) {
      return new TurkishDependencyParser(
          morphology, BiaffineDependencyModel.load(closeable), ConlluFormatter.bounDefault());
    }
  }

  /** Returns a copy of this parser using a custom {@link ConlluFormatter} (e.g. different dialect/MWT mode). */
  public TurkishDependencyParser withConlluFormatter(ConlluFormatter customFormatter) {
    return new TurkishDependencyParser(morphology, model, customFormatter);
  }

  public BiaffineDependencyModel getModel() {
    return model;
  }

  @Override
  public DependencyTree parse(String sentence) {
    if (sentence == null || sentence.isBlank()) {
      return new DependencyTree(
          new ConlluSentence(null, sentence == null ? "" : sentence, java.util.List.of(), java.util.List.of()));
    }
    SentenceAnalysis analysis = morphology.analyzeAndDisambiguate(sentence);
    return parse(analysis);
  }

  @Override
  public DependencyTree parse(SentenceAnalysis analysis) {
    ConlluSentence projected = formatter.projectSentence(analysis);
    return parse(projected);
  }

  @Override
  public DependencyTree parse(ConlluSentence projected) {
    return model.parse(projected);
  }
}
