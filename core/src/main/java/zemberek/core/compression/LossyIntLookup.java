package zemberek.core.compression;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import zemberek.core.collections.FloatValueMap;
import zemberek.core.hash.Mphf;
import zemberek.core.hash.MultiLevelMphf;
import zemberek.core.hash.StringHashKeyProvider;
import zemberek.core.io.IOUtil;

/**
 * This is a compact integer value lookup. Keys are considered as Strings.
 * There may be false positives (that it can return values of other keys for an input.
 * But the probability of occuring this is very low.
 */
public class LossyIntLookup {

  Mphf mphf;
  int[] data; // contains fingerprints and actual data.

  public LossyIntLookup(Mphf mphf, int[] data) {
    this.mphf = mphf;
    this.data = data;
  }

  public int get(String s) {
    int index = mphf.get(s) * 2;
    int fingerprint = MultiLevelMphf.hash(s, -1);
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
    int index = mphf.get(s) * 2;
    int fingerprint = getFingerprint(s);
    if (fingerprint == data[index]) {
      return Float.intBitsToFloat(data[index + 1]);
    } else {
      return 0;
    }
  }

  private static int getFingerprint(String s) {
    return s.hashCode() & 0x7ffffff;
  }

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

  public void serialize(Path path) throws IOException {
    try (DataOutputStream dos = IOUtil.getDataOutputStream(path)) {
      dos.writeInt(data.length);
      for (int d : data) {
        dos.writeInt(d);
      }
      mphf.serialize(dos);
    }
  }

  public static LossyIntLookup deserialize(DataInputStream dis) throws IOException {
    int length = dis.readInt();
    int[] data = new int[length];
    for (int i = 0; i < data.length; i++) {
      data[i] = dis.readInt();
    }
    Mphf mphf = MultiLevelMphf.deserialize(dis);
    return new LossyIntLookup(mphf, data);
  }

}
