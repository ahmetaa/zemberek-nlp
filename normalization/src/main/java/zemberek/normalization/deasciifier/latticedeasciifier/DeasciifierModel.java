package zemberek.normalization.deasciifier.latticedeasciifier;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Immutable binary model container for {@link LatticeDeasciifier}, bundling:
 * <ol>
 *   <li>{@link LexiconPriors} (case-aware surface confusion priors, foreign shield, acronyms, lemma
 *       counts)
 *   <li>{@link CharacterHarmonyModel} (5-state vowel-harmony character Perceptron)
 *   <li>{@link QuantizedWeightTable} (sentence-level Lattice Margin-Perceptron weights)
 * </ol>
 */
public record DeasciifierModel(
    LexiconPriors lexiconPriors,
    CharacterHarmonyModel characterModel,
    QuantizedWeightTable sentenceWeights) {

  public static final String DEFAULT_RESOURCE_PATH = "/deasciifier/lattice_deasciifier.bin";

  private static final int MAGIC = 0x5A4C444D; // "ZLDM"
  private static final int VERSION = 1;

  public DeasciifierModel {
    Objects.requireNonNull(lexiconPriors, "lexiconPriors must not be null");
    Objects.requireNonNull(characterModel, "characterModel must not be null");
    Objects.requireNonNull(sentenceWeights, "sentenceWeights must not be null");
  }

  private static final class DefaultHolder {
    static final DeasciifierModel INSTANCE = loadFromDefaultResource();

    private static DeasciifierModel loadFromDefaultResource() {
      InputStream in = DeasciifierModel.class.getResourceAsStream(DEFAULT_RESOURCE_PATH);
      if (in == null) {
        throw new IllegalStateException(
            "Default LatticeDeasciifier model resource not found on classpath: "
                + DEFAULT_RESOURCE_PATH);
      }
      try (in) {
        return load(in);
      } catch (IOException e) {
        throw new UncheckedIOException(
            "Failed to load default LatticeDeasciifier model from " + DEFAULT_RESOURCE_PATH, e);
      }
    }
  }

  /** Returns the shared default {@link DeasciifierModel} bundled on the classpath. */
  public static DeasciifierModel loadDefault() {
    return DefaultHolder.INSTANCE;
  }

  /** Saves the model as an uncompressed binary file. */
  public void save(Path path) throws IOException {
    if (path.getParent() != null) {
      Files.createDirectories(path.getParent());
    }
    try (OutputStream fos = Files.newOutputStream(path);
        DataOutputStream out = new DataOutputStream(new BufferedOutputStream(fos, 65_536))) {
      out.writeInt(MAGIC);
      out.writeInt(VERSION);
      lexiconPriors.serialize(out);
      characterModel.serialize(out);
      sentenceWeights.serialize(out);
    }
  }

  public static DeasciifierModel load(Path path) throws IOException {
    try (InputStream fis = Files.newInputStream(path)) {
      return load(fis);
    }
  }

  public static DeasciifierModel load(InputStream stream) throws IOException {
    try (DataInputStream in = new DataInputStream(new BufferedInputStream(stream, 65_536))) {
      int magic = in.readInt();
      if (magic != MAGIC) {
        throw new IOException("Invalid DeasciifierModel magic: " + Integer.toHexString(magic));
      }
      int version = in.readInt();
      if (version != VERSION) {
        throw new IOException("Unsupported DeasciifierModel version: " + version);
      }
      LexiconPriors priors = LexiconPriors.deserialize(in);
      CharacterHarmonyModel charModel = CharacterHarmonyModel.deserialize(in);
      QuantizedWeightTable sentWeights = QuantizedWeightTable.deserialize(in);
      return new DeasciifierModel(priors, charModel, sentWeights);
    }
  }
}
