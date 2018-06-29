package zemberek.core.data;

import java.io.DataInputStream;
import java.io.IOException;
import java.nio.file.Path;
import zemberek.core.compression.LossyIntLookup;
import zemberek.core.io.IOUtil;

public class CompressedWeights implements WeightLookup {

  LossyIntLookup lookup;

  public CompressedWeights(LossyIntLookup lookup) {
    this.lookup = lookup;
  }

  @Override
  public float get(String key) {
    return lookup.getAsFloat(key);
  }

  @Override
  public int size() {
    return lookup.size();
  }

  public void serialize(Path path) throws IOException {
    lookup.serialize(path);
  }

  public static CompressedWeights deserialize(Path path) throws IOException {
    LossyIntLookup lookup = LossyIntLookup.deserialize(IOUtil.getDataInputStream(path));
    return new CompressedWeights(lookup);
  }

  public static boolean isCompressed(DataInputStream dis) throws IOException {
    return LossyIntLookup.checkStream(dis);
  }

  public static boolean isCompressed(Path path) throws IOException {
    try (DataInputStream dis = IOUtil.getDataInputStream(path)) {
      return isCompressed(dis);
    }
  }

  public static boolean isCompressed(String resource) throws IOException {
    try (DataInputStream dis = IOUtil.getDataInputStream(resource)) {
      return isCompressed(dis);
    }
  }


  public static CompressedWeights deserialize(String resource) throws IOException {
    try (DataInputStream dis = IOUtil.getDataInputStream(resource)) {
      return new CompressedWeights(LossyIntLookup.deserialize(dis));
    }
  }
}

