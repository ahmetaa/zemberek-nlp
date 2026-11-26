package zemberek.normalization;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import zemberek.lm.compression.SmoothLm;
import zemberek.morphology.TurkishMorphology;
import zemberek.normalization.v2.SpanLatticePerceptronNormalizer;

/**
 * Normalizes noisy Turkish text (misspellings, ASCII-typed text, informal speech, split/merged
 * tokens, and casing/punctuation) using {@link SpanLatticePerceptronNormalizer}.
 */
public class TurkishSentenceNormalizer {

  private final SpanLatticePerceptronNormalizer delegate;

  public TurkishSentenceNormalizer(SpanLatticePerceptronNormalizer delegate) {
    this.delegate = Objects.requireNonNull(delegate, "delegate cannot be null");
  }

  /**
   * Creates a ready-to-use sentence normalizer using Zemberek's bundled default models.
   */
  public static TurkishSentenceNormalizer createWithDefaults(TurkishMorphology morphology)
      throws IOException {
    return new TurkishSentenceNormalizer(
        SpanLatticePerceptronNormalizer.createWithDefaults(morphology));
  }

  /**
   * Creates a ready-to-use sentence normalizer using Zemberek's bundled models and a shared {@link
   * SmoothLm} instance.
   */
  public static TurkishSentenceNormalizer createWithDefaults(
      TurkishMorphology morphology, SmoothLm lm) throws IOException {
    return new TurkishSentenceNormalizer(
        SpanLatticePerceptronNormalizer.createWithDefaults(morphology, lm));
  }

  /**
   * Loads a sentence normalizer from an external model directory containing {@code knowledge.bin}
   * (or {@code knowledge.bin.gz}) and {@code perceptron.bin} (or {@code perceptron.bin.gz}) along
   * with a {@link SmoothLm} file.
   */
  public static TurkishSentenceNormalizer fromDirectory(
      TurkishMorphology morphology, Path modelDir, Path lmPath) throws IOException {
    return new TurkishSentenceNormalizer(
        SpanLatticePerceptronNormalizer.fromDirectory(morphology, modelDir, lmPath));
  }

  /**
   * Loads a sentence normalizer from explicit {@code knowledge.bin(.gz)} and
   * {@code perceptron.bin(.gz)} files with a preloaded {@link SmoothLm}.
   */
  public static TurkishSentenceNormalizer load(
      TurkishMorphology morphology, SmoothLm lm, Path knowledgePath, Path perceptronPath)
      throws IOException {
    return new TurkishSentenceNormalizer(
        SpanLatticePerceptronNormalizer.load(morphology, lm, knowledgePath, perceptronPath));
  }

  public SpanLatticePerceptronNormalizer getPerceptronNormalizer() {
    return delegate;
  }

  public String normalize(String sentence) {
    return delegate.normalize(sentence);
  }
}
