package zemberek.normalization.deasciifier.nimbledeasciifier;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Immutable binary model container for {@link NimbleDeasciifier}.
 *
 * <p>Bundles a compact front-coded {@link NimbleLexicon}, a 5-state {@link NimbleCharModel}, and
 * a compact FP16 context Micro-Perceptron {@link NimbleFp16WeightTable} (~820-875 KB total on
 * disk, zero morphological-analyzer or external language-model dependency).
 */
public final class NimbleModel {

  public static final String DEFAULT_RESOURCE_PATH = "/deasciifier/nimble_deasciifier.bin";
  private static final int MAGIC = 0x5A4E444D; // "ZNDM"
  private static final int VERSION = 1;

  private final NimbleLexicon lexicon;
  private final NimbleCharModel charModel;
  private final NimbleFp16WeightTable sentenceWeights;

  public NimbleModel(
      NimbleLexicon lexicon, NimbleCharModel charModel, NimbleFp16WeightTable sentenceWeights) {
    this.lexicon = Objects.requireNonNull(lexicon, "lexicon must not be null");
    this.charModel = Objects.requireNonNull(charModel, "charModel must not be null");
    this.sentenceWeights =
        Objects.requireNonNull(sentenceWeights, "sentenceWeights must not be null");
  }

  public NimbleLexicon lexicon() {
    return lexicon;
  }

  public NimbleCharModel charModel() {
    return charModel;
  }

  public NimbleFp16WeightTable sentenceWeights() {
    return sentenceWeights;
  }

  public static NimbleModel loadDefault() {
    try (InputStream is = NimbleModel.class.getResourceAsStream(DEFAULT_RESOURCE_PATH)) {
      if (is == null) {
        throw new IllegalStateException(
            "Default NimbleDeasciifier model resource not found on classpath: "
                + DEFAULT_RESOURCE_PATH);
      }
      return load(is);
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to load default NimbleDeasciifier model.", e);
    }
  }

  public static NimbleModel load(Path path) throws IOException {
    try (InputStream is = Files.newInputStream(path)) {
      return load(is);
    }
  }

  public static NimbleModel load(InputStream is) throws IOException {
    try (DataInputStream in = new DataInputStream(new BufferedInputStream(is, 32_768))) {
      return deserialize(in);
    }
  }

  public void save(Path path) throws IOException {
    Path parent = path.getParent();
    if (parent != null) {
      Files.createDirectories(parent);
    }
    try (DataOutputStream out =
        new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(path), 32_768))) {
      serialize(out);
    }
  }

  public void serialize(DataOutputStream out) throws IOException {
    out.writeInt(MAGIC);
    out.writeInt(VERSION);
    lexicon.serialize(out);
    charModel.serialize(out);
    sentenceWeights.serialize(out);
  }

  public static NimbleModel deserialize(DataInputStream in) throws IOException {
    int magic = in.readInt();
    if (magic != MAGIC) {
      throw new IOException("Invalid NimbleModel magic: " + Integer.toHexString(magic));
    }
    int version = in.readInt();
    if (version != VERSION) {
      throw new IOException("Unsupported NimbleModel version: " + version);
    }
    NimbleLexicon lexicon = NimbleLexicon.deserialize(in);
    NimbleCharModel charModel = NimbleCharModel.deserialize(in);
    NimbleFp16WeightTable sentenceWeights = NimbleFp16WeightTable.deserialize(in);
    return new NimbleModel(lexicon, charModel, sentenceWeights);
  }
}
