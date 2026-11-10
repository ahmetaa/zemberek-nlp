package zemberek.core.compression;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PushbackInputStream;
import java.nio.file.Path;
import java.util.List;
import zemberek.core.collections.FloatValueMap;
import zemberek.core.hash.Mphf;
import zemberek.core.hash.MultiLevelMphf;
import zemberek.core.hash.StringHashKeyProvider;
import zemberek.core.io.Bytes;
import zemberek.core.io.IOUtil;

/**
 * This is a compact integer value lookup. Keys are considered as Strings. There may be false
 * positives (that it can return values of other keys for an input). But the probability of occurring
 * this is very low.
 */
public class LossyIntLookup {

  private static final int BITMAP_MASK = 0xffffff;
  private static final int[] POW31 = new int[256];

  static {
    POW31[0] = 1;
    for (int i = 1; i < POW31.length; i++) {
      POW31[i] = POW31[i - 1] * 31;
    }
  }

  private Mphf mphf; // Minimal perfect hash function that provides string to integer index lookup.
  private int[] data; // contains fingerprints and actual data.
  private final long[] fingerprintBitmap;

  private static final int MAGIC = 0xcafebeef;

  private LossyIntLookup(Mphf mphf, int[] data) {
    this.mphf = mphf;
    this.data = data;
    long[] bitmap = new long[1 << (24 - 6)];
    for (int i = 0; i < data.length; i += 2) {
      int fp = data[i] & BITMAP_MASK;
      bitmap[fp >>> 6] |= (1L << fp);
    }
    this.fingerprintBitmap = bitmap;
  }

  public int get(String s) {
    int fingerprint = getFingerprint(s);
    int b = fingerprint & BITMAP_MASK;
    if ((fingerprintBitmap[b >>> 6] & (1L << b)) == 0L) {
      return 0;
    }
    int index = mphf.get(s) * 2;
    if (fingerprint == data[index]) {
      return data[index + 1];
    } else {
      return 0;
    }
  }

  public int size() {
    return data.length / 2;
  }

  public float getAsFloat(String s) {
    return Float.intBitsToFloat(get(s));
  }

  /**
   * Evaluates {@code getAsFloat(prefix + suffix)}, using the 24-bit fingerprint bitmap and
   * polynomial string hash to reject absent keys without allocating the concatenated string.
   */
  public float getAsFloat(String prefix, String suffix) {
    int sLen = suffix.length();
    if (sLen < POW31.length) {
      int b = (prefix.hashCode() * POW31[sLen] + suffix.hashCode()) & BITMAP_MASK;
      if ((fingerprintBitmap[b >>> 6] & (1L << b)) == 0L) {
        return 0f;
      }
    }
    return Float.intBitsToFloat(get(prefix + suffix));
  }

  private static int getFingerprint(String s) {
    return s.hashCode() & 0x7ffffff;
  }

  /**
   * Generates a LossyIntLookup from a String->Float lookup
   */
  public static LossyIntLookup generate(FloatValueMap<String> lookup) {
    List<String> keyList = lookup.getKeyList();
    StringHashKeyProvider provider = new StringHashKeyProvider(keyList);
    MultiLevelMphf mphf = MultiLevelMphf.generate(provider);
    int[] data = new int[keyList.size() * 2];
    for (String s : keyList) {
      int index = mphf.get(s);
      data[index * 2] = getFingerprint(s); // fingerprint
      data[index * 2 + 1] = Float.floatToIntBits(lookup.get(s)); // data in int form
    }
    return new LossyIntLookup(mphf, data);
  }

  /**
   * Serialized this data structure to a binary file.
   */
  public void serialize(Path path) throws IOException {
    try (DataOutputStream dos = IOUtil.getDataOutputStream(path)) {
      dos.writeInt(MAGIC);
      dos.writeInt(data.length);
      for (int d : data) {
        dos.writeInt(d);
      }
      mphf.serialize(dos);
    }
  }

  /**
   * Checks if input {@link DataInputStream} [dis] contains a serialized LossyIntLookup.
   */
  public static boolean checkStream(DataInputStream dis) throws IOException {
    PushbackInputStream pis = new PushbackInputStream(dis, 4);
    byte[] fourBytes = new byte[4];
    int c = dis.read(fourBytes);
    if (c < 4) {
      return false;
    }
    int magic = Bytes.toInt(fourBytes, true);
    pis.unread(fourBytes);
    return magic == MAGIC;
  }

  /**
   * Deseializes a LossyIntLookup structure from a {@link DataInputStream} [dis]
   */
  public static LossyIntLookup deserialize(DataInputStream dis) throws IOException {
    long magic = dis.readInt();
    if (magic != MAGIC) {
      throw new IllegalStateException("File does not carry expected value in the beginning.");
    }
    int length = dis.readInt();
    int[] data = new int[length];
    for (int i = 0; i < data.length; i++) {
      data[i] = dis.readInt();
    }
    Mphf mphf = MultiLevelMphf.deserialize(dis);
    return new LossyIntLookup(mphf, data);
  }

}
