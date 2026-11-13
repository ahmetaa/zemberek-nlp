package zemberek.normalization.deasciifier.nimbledeasciifier;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import zemberek.core.turkish.TurkishAlphabet;
import zemberek.normalization.deasciifier.nimbledeasciifier.TurkishTextCodec.Casing;
import zemberek.normalization.deasciifier.nimbledeasciifier.TurkishTextCodec.Token;

/**
 * Compact, front-coded + diacritic-bitmask lexical prior and orthographic shield store for {@link
 * NimbleDeasciifier}.
 *
 * <p>Stores three compact, zero-morphology dictionaries:
 * <ol>
 *   <li>Multi-surface lowercase confusion sets ({@code uc -> {üç, uç}}, {@code kar -> {kar, kâr}})
 *       used as candidates for the context Micro-Perceptron;
 *   <li>Error-driven single-surface lowercase residuals (words that standalone {@link
 *       NimbleCharModel} mispredicts or that carry disambiguating circumflexes), encoded into a
 *       primitive open-addressed bitmask table with zero runtime {@code String} keys;
 *   <li>Case-separated mid-sentence {@code TitleCase} proper-noun entries ({@code Şişli}, {@code
 *       Bolu}, {@code Çukurova}), plus foreign ASCII shields and ALL-CAPS Turkish acronyms.
 * </ol>
 */
public final class NimbleLexicon {

  private static final int MAGIC = 0x5A4E4C58; // "ZNLX"
  private static final int VERSION = 1;
  private static final int MAX_WORD_LEN = 24;

  /** Empirical candidate distribution for an ambiguous ASCII surface key. */
  public record ConfusionSet(String[] surfaces, float[] logProbs, int totalCount) {
    public float logProbOf(String lowerSurface, float unseenDefault) {
      for (int i = 0; i < surfaces.length; i++) {
        if (surfaces[i].equals(lowerSurface)) {
          return logProbs[i];
        }
      }
      return unseenDefault;
    }
  }

  /** Primitive open-addressed table mapping 64-bit ASCII key hash to diacritic bitmasks. */
  private static final class SingleResidualTable {
    private static final long EMPTY = 0L;

    private final long[] keyHashes;
    private final int[] coreMasks;
    private final int[] circMasks;
    private final int mask;
    private final int count;

    private SingleResidualTable(int capacityPowerOfTwo, int count) {
      this.keyHashes = new long[capacityPowerOfTwo];
      this.coreMasks = new int[capacityPowerOfTwo];
      this.circMasks = new int[capacityPowerOfTwo];
      this.mask = capacityPowerOfTwo - 1;
      this.count = count;
    }

    static SingleResidualTable fromEncoded(long[] hashes, int[] cores, int[] circs, int size) {
      int cap = 16;
      while (cap * 6 < size * 10) {
        cap <<= 1;
      }
      SingleResidualTable table = new SingleResidualTable(cap, size);
      for (int i = 0; i < size; i++) {
        long h = hashes[i] == EMPTY ? 1L : hashes[i];
        int slot = ((int) h) & table.mask;
        while (table.keyHashes[slot] != EMPTY) {
          slot = (slot + 1) & table.mask;
        }
        table.keyHashes[slot] = h;
        table.coreMasks[slot] = cores[i];
        table.circMasks[slot] = circs[i];
      }
      return table;
    }

    String resolve(String asciiKey) {
      if (count == 0 || asciiKey.length() > MAX_WORD_LEN) {
        return null;
      }
      long h = NimbleHash.hashString(asciiKey);
      if (h == EMPTY) {
        h = 1L;
      }
      int slot = ((int) h) & mask;
      while (true) {
        long existing = keyHashes[slot];
        if (existing == h) {
          int core = coreMasks[slot];
          int circ = circMasks[slot];
          if ((core & ~toggleableCoreMask(asciiKey)) != 0
              || (circ & ~toggleableCircMask(asciiKey)) != 0) {
            return null;
          }
          return decodeBitmask(asciiKey, core, circ);
        }
        if (existing == EMPTY) {
          return null;
        }
        slot = (slot + 1) & mask;
      }
    }

    int size() {
      return count;
    }

    long ramBytesUsed() {
      return (long) keyHashes.length * (8L + 4L + 4L) + 32L;
    }
  }

  private static final Set<String> SEED_FOREIGN_KEEP =
      Set.of(
          "bios", "ram", "rom", "iso", "cpu", "gpu", "usb", "hdmi", "wifi", "pdf", "html",
          "http", "https", "url", "uri", "api", "sdk", "sql", "xml", "json", "css", "php",
          "linux", "windows", "macos", "android", "ios", "iphone", "ipad", "apple",
          "google", "microsoft", "youtube", "facebook", "twitter", "instagram", "whatsapp",
          "linkedin", "github", "docker", "kubernetes", "online", "superonline", "offline",
          "link", "email", "server", "client", "browser", "router", "bluetooth", "ssd",
          "hdd", "lcd", "oled", "uhd", "raid", "rfid", "midi", "ibm", "intel", "nvidia",
          "amd", "cisco", "oracle", "sap", "cia", "fbi", "nsa", "nato", "uefa", "fifa",
          "fiba", "nba", "ceo", "cfo", "cto", "coo", "vip", "dns", "vpn", "tcp", "udp",
          "ssh", "ftp", "smtp", "imap", "gui", "cli", "iot", "erp", "crm", "seo", "gif",
          "jpeg", "jpg", "png", "mp3", "mp4", "mkv", "rar", "exe", "dll", "apk", "ikea",
          "bim", "migros", "carrefour", "starbucks", "netflix", "spotify", "amazon", "tesla",
          "spacex", "boeing", "airbus", "ford", "fiat", "audi", "mini", "nikon", "canon",
          "sony", "samsung", "xiaomi", "huawei", "asus", "acer", "dell", "lenovo", "philips",
          "bosch", "siemens", "lg", "sdn");

  private static final Set<String> SEED_ALLCAPS_FOREIGN =
      Set.of(
          "it", "ai", "ui", "ux", "id", "ip", "os", "pc", "tv", "cd", "dvd", "hd", "sd",
          "sim", "pin", "bit", "ram", "rom", "iso", "bios", "cia", "fbi", "ibm", "avi", "zip",
          "bbc", "cnn", "ntv", "usb", "cpu", "gpu", "ssd");

  private static final Map<String, String> ALLCAPS_TURKISH_ACRONYMS =
      Map.ofEntries(
          Map.entry("tuik", "tüik"),
          Map.entry("tubitak", "tübitak"),
          Map.entry("osym", "ösym"),
          Map.entry("iett", "iett"),
          Map.entry("iski", "iski"),
          Map.entry("aihm", "aihm"),
          Map.entry("dhmi", "dhmi"),
          Map.entry("disk", "disk"),
          Map.entry("tisk", "tisk"),
          Map.entry("igdas", "iğdaş"),
          Map.entry("tedas", "tedaş"),
          Map.entry("botas", "botaş"),
          Map.entry("tusas", "tusaş"),
          Map.entry("rtuk", "rtük"),
          Map.entry("tusiad", "tüsiad"),
          Map.entry("musiad", "müsiad"),
          Map.entry("turk", "türk"),
          Map.entry("turksat", "türksat"),
          Map.entry("turkcell", "turkcell"),
          Map.entry("ibb", "ibb"),
          Map.entry("ito", "ito"),
          Map.entry("itu", "itü"),
          Map.entry("odtu", "odtü"),
          Map.entry("ytu", "ytü"),
          Map.entry("ktu", "ktü"),
          Map.entry("ab", "ab"),
          Map.entry("abd", "abd"),
          Map.entry("tbmm", "tbmm"),
          Map.entry("kktc", "kktc"),
          Map.entry("tcdd", "tcdd"),
          Map.entry("thy", "thy"),
          Map.entry("tsk", "tsk"),
          Map.entry("sgk", "sgk"),
          Map.entry("kdv", "kdv"),
          Map.entry("trt", "trt"),
          Map.entry("ptt", "ptt"),
          Map.entry("kyk", "kyk"),
          Map.entry("meb", "meb"),
          Map.entry("egm", "egm"),
          Map.entry("hgs", "hgs"),
          Map.entry("ogs", "ogs"),
          Map.entry("pkk", "pkk"),
          Map.entry("chp", "chp"),
          Map.entry("akp", "akp"),
          Map.entry("mhp", "mhp"),
          Map.entry("tobb", "tobb"),
          Map.entry("bddk", "bddk"),
          Map.entry("spk", "spk"),
          Map.entry("epdk", "epdk"),
          Map.entry("kvkk", "kvkk"),
          Map.entry("kobi", "kobi"),
          Map.entry("toki", "toki"),
          Map.entry("afad", "afad"),
          Map.entry("umke", "umke"),
          Map.entry("iskur", "işkur"),
          Map.entry("kosgeb", "kosgeb"),
          Map.entry("otv", "ötv"),
          Map.entry("mit", "mit"),
          Map.entry("ais", "ais"),
          Map.entry("ales", "ales"),
          Map.entry("kpss", "kpss"),
          Map.entry("yks", "yks"),
          Map.entry("lgs", "lgs"),
          Map.entry("tff", "tff"),
          Map.entry("tcmb", "tcmb"),
          Map.entry("tmsf", "tmsf"));

  private static final Map<String, String> SEED_UNLISTED_TURKISH =
      Map.ofEntries(
          Map.entry("superpozisyon", "süperpozisyon"),
          Map.entry("superpozisyonu", "süperpozisyonu"),
          Map.entry("tensor", "tensör"),
          Map.entry("tensorler", "tensörler"),
          Map.entry("tensorlere", "tensörlere"),
          Map.entry("kubit", "kübit"),
          Map.entry("kubitler", "kübitler"),
          Map.entry("kubitlerin", "kübitlerin"),
          Map.entry("kubitlerinin", "kübitlerinin"),
          Map.entry("sarj", "şarj"),
          Map.entry("sarji", "şarjı"),
          Map.entry("sarzim", "şarzım"),
          Map.entry("sarzi", "şarzı"),
          Map.entry("yanliz", "yanlız"),
          Map.entry("yanlizca", "yanlızca"),
          Map.entry("birsey", "birşey"),
          Map.entry("birseyler", "birşeyler"),
          Map.entry("bisey", "bişey"),
          Map.entry("biseyler", "bişeyler"),
          Map.entry("bisi", "bişi"),
          Map.entry("bise", "bişe"),
          Map.entry("bikac", "bikaç"),
          Map.entry("hicbi", "hiçbi"),
          Map.entry("hicbirsey", "hiçbirşey"),
          Map.entry("hersey", "herşey"),
          Map.entry("insallah", "inşallah"),
          Map.entry("masallah", "maşallah"),
          Map.entry("napalim", "napalım"),
          Map.entry("naptin", "naptın"),
          Map.entry("sagol", "sağol"),
          Map.entry("sagolun", "sağolun"),
          Map.entry("yazikki", "yazıkki"),
          Map.entry("hosbulduk", "hoşbulduk"),
          Map.entry("hosgeldin", "hoşgeldin"),
          Map.entry("hosgeldiniz", "hoşgeldiniz"),
          Map.entry("arkdaslar", "arkdaşlar"),
          Map.entry("ogretmnler", "öğretmnler"),
          Map.entry("orjinal", "orjinal"));

  private static final Set<String> TABOO_SURFACES =
      Set.of("sik", "siki", "sikin", "sikti", "göt", "götü", "göte", "amcık");

  private final Set<String> foreignKeepWords;
  private final Map<String, ConfusionSet> lowerConfusions;
  private final SingleResidualTable residualSingles;
  private final Map<String, ConfusionSet> properConfusions;
  // Raw encoded residual entries retained solely to make serialize() lossless if re-saved.
  private final byte[] serializedResidualsBlock;

  private NimbleLexicon(
      Set<String> extraForeignKeep,
      Map<String, ConfusionSet> lowerConfusions,
      SingleResidualTable residualSingles,
      Map<String, ConfusionSet> properConfusions,
      byte[] serializedResidualsBlock) {
    Set<String> foreign = new HashSet<>(SEED_FOREIGN_KEEP);
    if (extraForeignKeep != null) {
      foreign.addAll(extraForeignKeep);
    }
    this.foreignKeepWords = Collections.unmodifiableSet(foreign);
    this.lowerConfusions = Collections.unmodifiableMap(lowerConfusions);
    this.residualSingles = residualSingles;
    this.properConfusions = Collections.unmodifiableMap(properConfusions);
    this.serializedResidualsBlock = serializedResidualsBlock;
  }

  public static NimbleLexicon defaultsOnly() {
    Map<String, ConfusionSet> conf = new HashMap<>();
    for (var e : SEED_UNLISTED_TURKISH.entrySet()) {
      conf.put(e.getKey(), new ConfusionSet(new String[] {e.getValue()}, new float[] {0.0f}, 10));
    }
    return new NimbleLexicon(
        Set.of(), conf, SingleResidualTable.fromEncoded(new long[0], new int[0], new int[0], 0), Map.of(), new byte[4]);
  }

  public boolean isForeignAsciiKeep(String asciiLower, Casing casing) {
    if ("yok".equals(asciiLower)) {
      return false;
    }
    if (casing == Casing.UPPER) {
      if (ALLCAPS_TURKISH_ACRONYMS.containsKey(asciiLower)) {
        return false;
      }
      if (SEED_ALLCAPS_FOREIGN.contains(asciiLower)) {
        return true;
      }
    }
    return foreignKeepWords.contains(asciiLower);
  }

  public String getAllCapsTurkishAcronym(String asciiLower) {
    return ALLCAPS_TURKISH_ACRONYMS.get(asciiLower);
  }

  public ConfusionSet getLowerConfusion(String asciiLower) {
    ConfusionSet cs = lowerConfusions.get(asciiLower);
    if (cs != null) {
      return cs;
    }
    String seed = SEED_UNLISTED_TURKISH.get(asciiLower);
    return seed != null ? new ConfusionSet(new String[] {seed}, new float[] {0.0f}, 10) : null;
  }

  public String getSingleResidual(String asciiLower) {
    return residualSingles.resolve(asciiLower);
  }

  public ConfusionSet getProperConfusion(String asciiLower) {
    return properConfusions.get(asciiLower);
  }

  /**
   * Checks whether {@code asciiLower} has at least one known surface in common, residual, proper,
   * or acronym tables.
   */
  public boolean hasKnownAsciiKey(String asciiLower) {
    return lowerConfusions.containsKey(asciiLower)
        || SEED_UNLISTED_TURKISH.containsKey(asciiLower)
        || residualSingles.resolve(asciiLower) != null
        || properConfusions.containsKey(asciiLower)
        || ALLCAPS_TURKISH_ACRONYMS.containsKey(asciiLower);
  }

  public boolean hasCompatibleKnownSurface(String original, String asciiLower, boolean pinDotlessI) {
    ConfusionSet lower = getLowerConfusion(asciiLower);
    if (lower != null) {
      for (String s : lower.surfaces) {
        if (TurkishTextCodec.isCompatibleWithInput(original, s, pinDotlessI)) {
          return true;
        }
      }
    }
    String res = residualSingles.resolve(asciiLower);
    if (res != null && TurkishTextCodec.isCompatibleWithInput(original, res, pinDotlessI)) {
      return true;
    }
    ConfusionSet proper = properConfusions.get(asciiLower);
    if (proper != null) {
      for (String s : proper.surfaces) {
        if (TurkishTextCodec.isCompatibleWithInput(original, s, pinDotlessI)) {
          return true;
        }
      }
    }
    String acr = ALLCAPS_TURKISH_ACRONYMS.get(asciiLower);
    return acr != null && TurkishTextCodec.isCompatibleWithInput(original, acr, pinDotlessI);
  }

  public boolean hasCircumflexReading(String asciiLower) {
    return hasCircumflexIn(lowerConfusions.get(asciiLower))
        || hasCircumflexIn(properConfusions.get(asciiLower));
  }

  private static boolean hasCircumflexIn(ConfusionSet cs) {
    if (cs == null) {
      return false;
    }
    for (String s : cs.surfaces) {
      if (TurkishAlphabet.INSTANCE.containsCircumflex(s)) {
        return true;
      }
    }
    return false;
  }

  public boolean isTabooSurface(String lowerSurface) {
    return TABOO_SURFACES.contains(lowerSurface);
  }

  public int lowerConfusionCount() {
    return lowerConfusions.size();
  }

  public int residualSingleCount() {
    return residualSingles.size();
  }

  public int properConfusionCount() {
    return properConfusions.size();
  }

  public long ramBytesUsed() {
    long approx = foreignKeepWords.size() * 48L + residualSingles.ramBytesUsed();
    approx += lowerConfusions.size() * 140L;
    approx += properConfusions.size() * 110L;
    return approx;
  }

  public void serialize(DataOutputStream out) throws IOException {
    out.writeInt(MAGIC);
    out.writeInt(VERSION);

    List<String> extraForeign = new ArrayList<>();
    for (String w : foreignKeepWords) {
      if (!SEED_FOREIGN_KEEP.contains(w)) {
        extraForeign.add(w);
      }
    }
    Collections.sort(extraForeign);
    out.writeInt(extraForeign.size());
    for (String w : extraForeign) {
      out.writeUTF(w);
    }

    writeFrontCodedMap(out, lowerConfusions, true);
    out.writeInt(serializedResidualsBlock.length);
    out.write(serializedResidualsBlock);
    writeFrontCodedMap(out, properConfusions, false);
  }

  public static NimbleLexicon deserialize(DataInputStream in) throws IOException {
    int magic = in.readInt();
    if (magic != MAGIC) {
      throw new IOException("Invalid NimbleLexicon magic: " + Integer.toHexString(magic));
    }
    int version = in.readInt();
    if (version != VERSION) {
      throw new IOException("Unsupported NimbleLexicon version: " + version);
    }

    int foreignCount = in.readInt();
    Set<String> extraForeign = new HashSet<>(foreignCount * 2 + 1);
    for (int i = 0; i < foreignCount; i++) {
      extraForeign.add(in.readUTF());
    }

    Map<String, ConfusionSet> lowerConf = readFrontCodedMap(in);
    int resBlockLen = in.readInt();
    byte[] resBlock = new byte[resBlockLen];
    in.readFully(resBlock);
    SingleResidualTable residuals = readResidualsBlock(resBlock);
    Map<String, ConfusionSet> properConf = readFrontCodedMap(in);
    return new NimbleLexicon(extraForeign, lowerConf, residuals, properConf, resBlock);
  }

  private static void writeFrontCodedMap(
      DataOutputStream out, Map<String, ConfusionSet> map, boolean skipSeedUnlisted)
      throws IOException {
    List<String> sortedKeys = new ArrayList<>(map.size());
    for (var e : map.entrySet()) {
      String key = e.getKey();
      ConfusionSet cs = e.getValue();
      if (key.length() > MAX_WORD_LEN) {
        continue;
      }
      if (skipSeedUnlisted
          && SEED_UNLISTED_TURKISH.containsKey(key)
          && cs.surfaces.length == 1
          && cs.surfaces[0].equals(SEED_UNLISTED_TURKISH.get(key))) {
        continue;
      }
      if (canEncodeAll(key, cs.surfaces)) {
        sortedKeys.add(key);
      }
    }
    Collections.sort(sortedKeys);

    out.writeInt(sortedKeys.size());
    String prevKey = "";
    for (String key : sortedKeys) {
      byte[] asciiBytes = key.getBytes(StandardCharsets.US_ASCII);
      int shared = sharedPrefixLen(prevKey, key);
      int suffixLen = asciiBytes.length - shared;
      out.writeByte(shared);
      out.writeByte(suffixLen);
      out.write(asciiBytes, shared, suffixLen);

      ConfusionSet cs = map.get(key);
      int m = Math.min(15, cs.surfaces.length);
      int anyCircFlag = 0;
      int[] coreMasks = new int[m];
      int[] circMasks = new int[m];
      for (int i = 0; i < m; i++) {
        coreMasks[i] = encodeCoreMask(key, cs.surfaces[i]);
        circMasks[i] = encodeCircMask(key, cs.surfaces[i]);
        if (circMasks[i] != 0) {
          anyCircFlag = 0x80;
        }
      }

      boolean hasHeaderCirc = anyCircFlag != 0;
      out.writeByte((m & 0x0F) | anyCircFlag);
      out.writeShort(Math.min(Short.MAX_VALUE, cs.totalCount));
      for (int i = 0; i < m; i++) {
        writeVarInt(out, coreMasks[i]);
        if (hasHeaderCirc) {
          writeVarInt(out, circMasks[i]);
        }
        if (m > 1) {
          int q = Math.clamp(Math.round(cs.logProbs[i] * 16.0f), -127, 0);
          out.writeByte(q);
        }
      }
      prevKey = key;
    }
  }

  private static Map<String, ConfusionSet> readFrontCodedMap(DataInputStream in)
      throws IOException {
    int count = in.readInt();
    Map<String, ConfusionSet> map = new HashMap<>((int) (count / 0.75f) + 16);
    byte[] keyBuf = new byte[64];
    int prevLen = 0;

    for (int i = 0; i < count; i++) {
      int shared = in.readUnsignedByte();
      int suffixLen = in.readUnsignedByte();
      int totalLen = shared + suffixLen;
      if (totalLen > keyBuf.length) {
        keyBuf = Arrays.copyOf(keyBuf, totalLen);
      }
      in.readFully(keyBuf, shared, suffixLen);
      String key = new String(keyBuf, 0, totalLen, StandardCharsets.US_ASCII);
      prevLen = totalLen;

      int flags = in.readUnsignedByte();
      int m = flags & 0x0F;
      boolean hasCirc = (flags & 0x80) != 0;
      int totalCount = in.readUnsignedShort();

      String[] surfaces = new String[m];
      float[] logProbs = new float[m];
      for (int j = 0; j < m; j++) {
        int core = readVarInt(in);
        int circ = hasCirc ? readVarInt(in) : 0;
        surfaces[j] = decodeBitmask(key, core, circ);
        logProbs[j] = m > 1 ? in.readByte() / 16.0f : 0.0f;
      }
      map.put(key, new ConfusionSet(surfaces, logProbs, totalCount));
    }
    return map;
  }

  private static byte[] encodeResidualsBlock(Map<String, String> residuals) throws IOException {
    List<String> sortedKeys = new ArrayList<>(residuals.size());
    for (var e : residuals.entrySet()) {
      String k = e.getKey();
      String v = e.getValue();
      if (k.length() <= MAX_WORD_LEN && canEncodeSurface(k, v)) {
        sortedKeys.add(k);
      }
    }
    Collections.sort(sortedKeys);

    ByteArrayOutputStream baos = new ByteArrayOutputStream(sortedKeys.size() * 8 + 16);
    try (DataOutputStream out = new DataOutputStream(baos)) {
      out.writeInt(sortedKeys.size());
      String prevKey = "";
      for (String key : sortedKeys) {
        byte[] asciiBytes = key.getBytes(StandardCharsets.US_ASCII);
        int shared = sharedPrefixLen(prevKey, key);
        int suffixLen = asciiBytes.length - shared;
        String surf = residuals.get(key);
        int core = encodeCoreMask(key, surf);
        int circ = encodeCircMask(key, surf);
        int hasCircFlag = circ != 0 ? 0x80 : 0;
        out.writeByte(shared);
        out.writeByte(suffixLen | hasCircFlag);
        out.write(asciiBytes, shared, suffixLen);
        writeVarInt(out, core);
        if (circ != 0) {
          writeVarInt(out, circ);
        }
        prevKey = key;
      }
    }
    return baos.toByteArray();
  }

  private static SingleResidualTable readResidualsBlock(byte[] block) throws IOException {
    try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(block))) {
      int count = in.readInt();
      if (count == 0) {
        return SingleResidualTable.fromEncoded(new long[0], new int[0], new int[0], 0);
      }
      long[] hashes = new long[count];
      int[] cores = new int[count];
      int[] circs = new int[count];
      byte[] keyBuf = new byte[64];

      for (int i = 0; i < count; i++) {
        int shared = in.readUnsignedByte();
        int lenAndFlag = in.readUnsignedByte();
        boolean hasCirc = (lenAndFlag & 0x80) != 0;
        int suffixLen = lenAndFlag & 0x7F;
        int totalLen = shared + suffixLen;
        if (totalLen > keyBuf.length) {
          keyBuf = Arrays.copyOf(keyBuf, totalLen);
        }
        in.readFully(keyBuf, shared, suffixLen);
        String key = new String(keyBuf, 0, totalLen, StandardCharsets.US_ASCII);
        hashes[i] = NimbleHash.hashString(key);
        cores[i] = readVarInt(in);
        circs[i] = hasCirc ? readVarInt(in) : 0;
      }
      return SingleResidualTable.fromEncoded(hashes, cores, circs, count);
    }
  }

  static final class CorpusBuckets {
    final Map<String, Map<String, Integer>> lowerBuckets;
    final Map<String, Map<String, Integer>> lowerCircBuckets;
    final Map<String, Map<String, Integer>> properBuckets;
    final Map<String, Map<String, Integer>> properCircBuckets;
    final Set<String> minedForeign;
    final Set<String> validSurfaces;
    final boolean hasValidator;

    CorpusBuckets(
        Map<String, Map<String, Integer>> lowerBuckets,
        Map<String, Map<String, Integer>> lowerCircBuckets,
        Map<String, Map<String, Integer>> properBuckets,
        Map<String, Map<String, Integer>> properCircBuckets,
        Set<String> minedForeign,
        Set<String> validSurfaces,
        boolean hasValidator) {
      this.lowerBuckets = lowerBuckets;
      this.lowerCircBuckets = lowerCircBuckets;
      this.properBuckets = properBuckets;
      this.properCircBuckets = properCircBuckets;
      this.minedForeign = minedForeign;
      this.validSurfaces = validSurfaces;
      this.hasValidator = hasValidator;
    }

    Set<String> foreignAndSeedSet() {
      Set<String> combined = new HashSet<>(SEED_FOREIGN_KEEP.size() + minedForeign.size() + 16);
      combined.addAll(SEED_FOREIGN_KEEP);
      combined.addAll(minedForeign);
      return combined;
    }
  }

  /**
   * Collects lowercase, proper-noun, circumflex, and foreign-word token counts in a single pass
   * over {@code cleanSentences}.
   */
  static CorpusBuckets collectBuckets(
      List<String> cleanSentences,
      boolean[] circumflexIntact,
      Predicate<String> knownValidSurface,
      int maxForeignKeep) {
    Map<String, Map<String, Integer>> lowerBuckets = new HashMap<>(65_536);
    Map<String, Map<String, Integer>> lowerCircBuckets = new HashMap<>(32_768);
    Map<String, Map<String, Integer>> sentStartBuckets = new HashMap<>(32_768);
    Map<String, Map<String, Integer>> properBuckets = new HashMap<>(32_768);
    Map<String, Map<String, Integer>> properCircBuckets = new HashMap<>(16_384);
    Map<String, Integer> pureAsciiCounts = new HashMap<>(8_192);

    for (int si = 0; si < cleanSentences.size(); si++) {
      String sent = cleanSentences.get(si);
      boolean circOk = circumflexIntact == null || circumflexIntact[si];
      for (Token tok : TurkishTextCodec.tokenize(sent)) {
        if (tok.immutable() || tok.numeralApostrophe()) {
          continue;
        }
        if (tok.hasApostrophe()) {
          // Record mid-sentence TitleCase apostrophe stems ("Cukurova'daki" -> "Çukurova").
          int apos = tok.apostropheIndex();
          if (!tok.sentenceStart()
              && tok.casing() == Casing.TITLE
              && apos >= 2
              && apos <= MAX_WORD_LEN) {
            String stemOrig = tok.original().substring(0, apos);
            String stemLower = TurkishTextCodec.toTurkishLower(stemOrig);
            String stemAscii = TurkishTextCodec.toAsciiLower(stemLower);
            if (TurkishTextCodec.hasAmbiguousAscii(stemAscii)) {
              recordSurfaceObservation(
                  properBuckets, properCircBuckets, circOk, stemAscii, stemLower);
            }
            if (stemOrig.equals(TurkishTextCodec.asciify(stemOrig))) {
              pureAsciiCounts.merge(stemAscii, 1, Integer::sum);
            }
          }
          continue;
        }

        String orig = tok.original();
        if (orig.length() < 2 || orig.length() > MAX_WORD_LEN) {
          continue;
        }
        if (tok.casing() == Casing.UPPER && !tok.hasPinnedTurkish()) {
          continue;
        }

        String trLower = TurkishTextCodec.toTurkishLower(orig);
        String asciiLower = TurkishTextCodec.toAsciiLower(trLower);
        if (!TurkishTextCodec.hasAmbiguousAscii(asciiLower)) {
          continue;
        }

        if (!tok.sentenceStart() && tok.casing() == Casing.TITLE) {
          recordSurfaceObservation(properBuckets, properCircBuckets, circOk, asciiLower, trLower);
        } else if (tok.casing() == Casing.LOWER) {
          recordSurfaceObservation(lowerBuckets, lowerCircBuckets, circOk, asciiLower, trLower);
        } else if (tok.sentenceStart() && tok.casing() == Casing.TITLE) {
          recordSurfaceObservation(sentStartBuckets, lowerCircBuckets, circOk, asciiLower, trLower);
        }

        if (orig.equals(TurkishTextCodec.asciify(orig)) && !tok.sentenceStart()) {
          pureAsciiCounts.merge(asciiLower, 1, Integer::sum);
        }
      }
    }

    for (var e : sentStartBuckets.entrySet()) {
      String ascii = e.getKey();
      int properTotal = sumValues(properBuckets.getOrDefault(ascii, Map.of()));
      int lowerTotal = sumValues(lowerBuckets.getOrDefault(ascii, Map.of()));
      if (properTotal > lowerTotal * 2 && properTotal >= 10) {
        continue;
      }
      Map<String, Integer> dest = lowerBuckets.computeIfAbsent(ascii, k -> new HashMap<>(2));
      for (var se : e.getValue().entrySet()) {
        dest.merge(se.getKey(), se.getValue(), Integer::sum);
      }
    }

    Set<String> validSurfaces = new HashSet<>(65_536);
    if (knownValidSurface != null) {
      for (Map<String, Integer> m : lowerBuckets.values()) {
        for (String surf : m.keySet()) {
          if (!validSurfaces.contains(surf) && knownValidSurface.test(surf)) {
            validSurfaces.add(surf);
          }
        }
      }
      for (Map<String, Integer> m : properBuckets.values()) {
        for (String surf : m.keySet()) {
          if (!validSurfaces.contains(surf) && knownValidSurface.test(surf)) {
            validSurfaces.add(surf);
          }
        }
      }
    }

    List<Map.Entry<String, Integer>> foreignCandidates = new ArrayList<>();
    for (var e : pureAsciiCounts.entrySet()) {
      String ascii = e.getKey();
      int pureCount = e.getValue();
      if (pureCount < 10
          || !TurkishTextCodec.hasCoreDiacriticAscii(ascii)
          || ALLCAPS_TURKISH_ACRONYMS.containsKey(ascii)
          || validSurfaces.contains(ascii)
          || !isPureAsciiLetters(ascii)) {
        continue;
      }
      int nonAsciiCount =
          countNonAscii(lowerBuckets.get(ascii), ascii)
              + countNonAscii(properBuckets.get(ascii), ascii);
      if ((double) pureCount / (pureCount + nonAsciiCount) >= 0.97) {
        foreignCandidates.add(e);
      }
    }
    foreignCandidates.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
    Set<String> minedForeign = new HashSet<>();
    for (int i = 0; i < Math.min(maxForeignKeep, foreignCandidates.size()); i++) {
      minedForeign.add(foreignCandidates.get(i).getKey());
    }

    return new CorpusBuckets(
        lowerBuckets,
        lowerCircBuckets,
        properBuckets,
        properCircBuckets,
        minedForeign,
        validSurfaces,
        knownValidSurface != null);
  }

  static NimbleLexicon buildFromBuckets(
      CorpusBuckets buckets,
      NimbleCharModel charModel,
      int maxLowerConfusions,
      int maxSingleResiduals,
      int maxProperConfusions) {
    LowerSelection lowerSel =
        selectLowerEntries(
            buckets.lowerBuckets,
            buckets.lowerCircBuckets,
            buckets.minedForeign,
            buckets.validSurfaces,
            buckets.hasValidator,
            charModel,
            maxLowerConfusions,
            maxSingleResiduals);

    Map<String, ConfusionSet> properSel =
        selectProperEntries(
            buckets.properBuckets,
            buckets.properCircBuckets,
            buckets.lowerBuckets,
            buckets.minedForeign,
            buckets.validSurfaces,
            buckets.hasValidator,
            charModel,
            maxProperConfusions);

    try {
      byte[] resBlock = encodeResidualsBlock(lowerSel.residuals);
      SingleResidualTable resTable = readResidualsBlock(resBlock);
      return new NimbleLexicon(
          buckets.minedForeign, lowerSel.confusions, resTable, properSel, resBlock);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to encode NimbleLexicon residuals.", e);
    }
  }

  /**
   * Mines lowercase confusion sets, error-driven single-surface residuals, mid-sentence proper-noun
   * priors, and foreign ASCII-keep words from clean sentences without requiring morphology at
   * runtime.
   *
   * @param knownValidSurface optional offline surface validator (e.g. from {@code
   *     MorphologicalCandidateGenerator}); pass {@code null} for 100% morphology-free mining
   */
  public static NimbleLexicon mineFromCorpus(
      List<String> cleanSentences,
      boolean[] circumflexIntact,
      NimbleCharModel charModel,
      Predicate<String> knownValidSurface,
      int maxLowerConfusions,
      int maxSingleResiduals,
      int maxProperConfusions,
      int maxForeignKeep) {
    CorpusBuckets buckets =
        collectBuckets(cleanSentences, circumflexIntact, knownValidSurface, maxForeignKeep);
    return buildFromBuckets(
        buckets, charModel, maxLowerConfusions, maxSingleResiduals, maxProperConfusions);
  }

  private record LowerSelection(
      Map<String, ConfusionSet> confusions, Map<String, String> residuals) {}

  private static LowerSelection selectLowerEntries(
      Map<String, Map<String, Integer>> bucketsMap,
      Map<String, Map<String, Integer>> circBucketsMap,
      Set<String> minedForeign,
      Set<String> validSurfaces,
      boolean hasValidator,
      NimbleCharModel charModel,
      int maxMultiConfusions,
      int maxSingleResiduals) {
    record ScoredMulti(String key, ConfusionSet set, int priority) {}
    record ScoredSingle(String key, String surface, int priority) {}

    List<ScoredMulti> multiBuckets = new ArrayList<>();
    List<ScoredSingle> singleBuckets = new ArrayList<>();

    for (var e : bucketsMap.entrySet()) {
      String ascii = e.getKey();
      if (ascii.length() > MAX_WORD_LEN
          || !isPureAsciiLetters(ascii)
          || SEED_FOREIGN_KEEP.contains(ascii)
          || minedForeign.contains(ascii)) {
        continue;
      }
      Map<String, Integer> rawAll = e.getValue();
      int allTotal = sumValues(rawAll);
      if (allTotal < (hasValidator ? 2 : 3)) {
        continue;
      }

      Map<String, Integer> circRaw = circBucketsMap.get(ascii);
      int circTotal = circRaw != null ? sumValues(circRaw) : 0;
      boolean disambigCirc = hasDisambiguatingCircumflexMinimalPair(circRaw, circTotal);
      Map<String, Integer> raw = disambigCirc ? circRaw : rawAll;
      int rawTotal = disambigCirc ? circTotal : allTotal;
      if (disambigCirc && rawTotal < 2) {
        continue;
      }

      List<Map.Entry<String, Integer>> valid =
          filterValidSurfaces(ascii, raw, rawTotal, validSurfaces, hasValidator, disambigCirc, false);
      if (!disambigCirc && !valid.isEmpty()) {
        valid = mergeNonDisambiguatingCircumflexVariants(valid, circRaw);
      }
      if (valid.isEmpty()) {
        continue;
      }
      int keptTotal = 0;
      for (var se : valid) {
        keptTotal += se.getValue();
      }
      valid.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));

      if (valid.size() >= 2) {
        int m = Math.min(6, valid.size());
        double denom = keptTotal + 0.5 * Math.max(2, m);
        String[] surfaces = new String[m];
        float[] logProbs = new float[m];
        for (int i = 0; i < m; i++) {
          surfaces[i] = valid.get(i).getKey();
          logProbs[i] = (float) Math.log((valid.get(i).getValue() + 0.5) / denom);
        }
        int scale = disambigCirc ? 3 : 1;
        int priority = (keptTotal + valid.get(1).getValue() * 6) * scale;
        multiBuckets.add(
            new ScoredMulti(ascii, new ConfusionSet(surfaces, logProbs, keptTotal), priority));
      } else {
        String topSurf = valid.getFirst().getKey();
        if (TurkishAlphabet.INSTANCE.containsCircumflex(topSurf)) {
          multiBuckets.add(
              new ScoredMulti(
                  ascii,
                  new ConfusionSet(new String[] {topSurf}, new float[] {0.0f}, keptTotal),
                  keptTotal * 2));
        } else if (charModel != null && keptTotal >= (hasValidator ? 2 : 4)) {
          String pred = standaloneCharPrediction(charModel, ascii);
          if (!pred.equals(TurkishTextCodec.stripCircumflex(topSurf))) {
            singleBuckets.add(new ScoredSingle(ascii, topSurf, keptTotal));
          }
        }
      }
    }

    multiBuckets.sort((a, b) -> Integer.compare(b.priority(), a.priority()));
    Map<String, ConfusionSet> multiMap = new HashMap<>();
    for (int i = 0; i < Math.min(maxMultiConfusions, multiBuckets.size()); i++) {
      ScoredMulti b = multiBuckets.get(i);
      multiMap.put(b.key(), b.set());
    }

    singleBuckets.sort((a, b) -> Integer.compare(b.priority(), a.priority()));
    Map<String, String> singleMap = new HashMap<>();
    for (int i = 0; i < Math.min(maxSingleResiduals, singleBuckets.size()); i++) {
      ScoredSingle s = singleBuckets.get(i);
      if (!multiMap.containsKey(s.key())) {
        singleMap.put(s.key(), s.surface());
      }
    }

    return new LowerSelection(multiMap, singleMap);
  }

  private static Map<String, ConfusionSet> selectProperEntries(
      Map<String, Map<String, Integer>> properBuckets,
      Map<String, Map<String, Integer>> properCircBuckets,
      Map<String, Map<String, Integer>> lowerBuckets,
      Set<String> minedForeign,
      Set<String> validSurfaces,
      boolean hasValidator,
      NimbleCharModel charModel,
      int maxEntries) {
    if (maxEntries <= 0) {
      return Map.of();
    }
    record ScoredBucket(String key, ConfusionSet set, int priority) {}
    List<ScoredBucket> buckets = new ArrayList<>();

    for (var e : properBuckets.entrySet()) {
      String ascii = e.getKey();
      if (ascii.length() > MAX_WORD_LEN
          || !isPureAsciiLetters(ascii)
          || SEED_FOREIGN_KEEP.contains(ascii)
          || minedForeign.contains(ascii)) {
        continue;
      }
      Map<String, Integer> rawAll = e.getValue();
      int allTotal = sumValues(rawAll);
      if (allTotal < 3) {
        continue;
      }
      Map<String, Integer> circRaw = properCircBuckets.get(ascii);
      int circTotal = circRaw != null ? sumValues(circRaw) : 0;
      boolean disambigCirc = hasDisambiguatingCircumflexMinimalPair(circRaw, circTotal);
      Map<String, Integer> raw = disambigCirc ? circRaw : rawAll;
      int rawTotal = disambigCirc ? circTotal : allTotal;
      if (disambigCirc && rawTotal < 2) {
        continue;
      }

      List<Map.Entry<String, Integer>> valid =
          filterValidSurfaces(ascii, raw, rawTotal, validSurfaces, hasValidator, disambigCirc, true);
      if (!disambigCirc && !valid.isEmpty()) {
        valid = mergeNonDisambiguatingCircumflexVariants(valid, circRaw);
      }
      if (valid.isEmpty()) {
        continue;
      }
      int keptTotal = 0;
      for (var se : valid) {
        keptTotal += se.getValue();
      }
      valid.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));

      String topSurf = valid.getFirst().getKey();
      boolean multiSurface = valid.size() >= 2;
      boolean singleCirc = valid.size() == 1 && TurkishAlphabet.INSTANCE.containsCircumflex(topSurf);
      boolean differsFromLower =
          differsFromDominantLower(lowerBuckets.get(ascii), topSurf);
      boolean missedByCharModel =
          charModel != null && keptTotal >= 4 && !standaloneCharPrediction(charModel, ascii).equals(TurkishTextCodec.stripCircumflex(topSurf));

      if (!multiSurface && !singleCirc && !differsFromLower && !missedByCharModel) {
        continue;
      }

      int m = Math.min(6, valid.size());
      double denom = keptTotal + 0.5 * Math.max(2, m);
      String[] surfaces = new String[m];
      float[] logProbs = new float[m];
      for (int i = 0; i < m; i++) {
        surfaces[i] = valid.get(i).getKey();
        logProbs[i] = m > 1 ? (float) Math.log((valid.get(i).getValue() + 0.5) / denom) : 0.0f;
      }
      int scale = disambigCirc ? 3 : 1;
      int secondCount = valid.size() >= 2 ? valid.get(1).getValue() : keptTotal / 2;
      int priority = (keptTotal + secondCount * 6) * scale;
      buckets.add(new ScoredBucket(ascii, new ConfusionSet(surfaces, logProbs, keptTotal), priority));
    }

    buckets.sort((a, b) -> Integer.compare(b.priority(), a.priority()));
    Map<String, ConfusionSet> out = new HashMap<>();
    for (int i = 0; i < Math.min(maxEntries, buckets.size()); i++) {
      ScoredBucket b = buckets.get(i);
      out.put(b.key(), b.set());
    }
    return out;
  }

  private static List<Map.Entry<String, Integer>> filterValidSurfaces(
      String ascii,
      Map<String, Integer> raw,
      int rawTotal,
      Set<String> validSurfaces,
      boolean hasValidator,
      boolean circumflexBucket,
      boolean properMode) {
    List<Map.Entry<String, Integer>> valid = new ArrayList<>(raw.size());
    for (var se : raw.entrySet()) {
      String surf = se.getKey();
      if ("halâ".equals(surf) || "hâla".equals(surf) || !canEncodeSurface(ascii, surf)) {
        continue;
      }
      int cnt = se.getValue();
      boolean morphKnown = !hasValidator || validSurfaces.contains(surf);
      double minShare =
          hasValidator
              ? (morphKnown ? 0.00025 : (properMode ? 0.08 : 0.025))
              : (properMode ? 0.05 : 0.012);
      int minAbs =
          hasValidator
              ? (morphKnown ? (circumflexBucket ? 1 : 2) : (properMode ? 5 : 4))
              : (properMode ? 5 : 4);
      if (cnt >= minAbs && (double) cnt / rawTotal >= minShare) {
        valid.add(se);
      }
    }
    return valid;
  }

  private static boolean differsFromDominantLower(
      Map<String, Integer> lowerMap, String properTopSurf) {
    if (lowerMap == null || lowerMap.isEmpty()) {
      return false;
    }
    String bestLower = null;
    int bestCnt = 0;
    for (var e : lowerMap.entrySet()) {
      if (e.getValue() > bestCnt) {
        bestCnt = e.getValue();
        bestLower = e.getKey();
      }
    }
    return bestCnt >= 5 && bestLower != null && !bestLower.equals(properTopSurf);
  }

  private static boolean hasDisambiguatingCircumflexMinimalPair(
      Map<String, Integer> circRaw, int circTotal) {
    if (circRaw == null
        || circTotal < 2
        || !hasEmpiricalCircumflexSurface(circRaw, circTotal)) {
      return false;
    }
    for (String surf : circRaw.keySet()) {
      if (NimbleDeasciifier.startsWithDisambiguatingPrefix(surf)) {
        return true;
      }
    }
    return false;
  }

  private static List<Map.Entry<String, Integer>> mergeNonDisambiguatingCircumflexVariants(
      List<Map.Entry<String, Integer>> validAll, Map<String, Integer> circRaw) {
    Map<String, Integer> groupedCounts = new HashMap<>(validAll.size());
    for (var se : validAll) {
      groupedCounts.merge(
          TurkishTextCodec.stripCircumflex(se.getKey()), se.getValue(), Integer::sum);
    }

    Map<String, String> bestCircByStripped = new HashMap<>(4);
    if (circRaw != null && !circRaw.isEmpty()) {
      Map<String, Integer> circGroupTotal = new HashMap<>(groupedCounts.size());
      Map<String, Integer> bestCircCnt = new HashMap<>(4);
      for (var se : circRaw.entrySet()) {
        String surf = se.getKey();
        if ("halâ".equals(surf) || "hâla".equals(surf)) {
          continue;
        }
        String stripped = TurkishTextCodec.stripCircumflex(surf);
        if (!groupedCounts.containsKey(stripped)) {
          continue;
        }
        int cnt = se.getValue();
        circGroupTotal.merge(stripped, cnt, Integer::sum);
        if (TurkishAlphabet.INSTANCE.containsCircumflex(surf)
            && cnt > bestCircCnt.getOrDefault(stripped, 0)) {
          bestCircCnt.put(stripped, cnt);
          bestCircByStripped.put(stripped, surf);
        }
      }
      for (var entry : bestCircCnt.entrySet()) {
        String stripped = entry.getKey();
        int circCnt = entry.getValue();
        int groupCircTot = circGroupTotal.getOrDefault(stripped, 0);
        if (circCnt < 2 || circCnt * 5 < groupCircTot) {
          bestCircByStripped.remove(stripped);
        }
      }
    }

    List<Map.Entry<String, Integer>> merged = new ArrayList<>(groupedCounts.size());
    for (var e : groupedCounts.entrySet()) {
      String stripped = e.getKey();
      String canonical = bestCircByStripped.getOrDefault(stripped, stripped);
      merged.add(Map.entry(canonical, e.getValue()));
    }
    merged.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
    return merged;
  }

  private static String standaloneCharPrediction(NimbleCharModel charModel, String asciiLower) {
    Token tok =
        new Token(
            0,
            asciiLower.length(),
            asciiLower,
            asciiLower,
            Casing.LOWER,
            -1,
            false,
            false,
            false,
            ' ',
            false);
    return charModel.decodeTop1(tok, true);
  }

  private static boolean canEncodeAll(String asciiKey, String[] surfaces) {
    for (String s : surfaces) {
      if (!canEncodeSurface(asciiKey, s)) {
        return false;
      }
    }
    return true;
  }

  private static boolean canEncodeSurface(String asciiKey, String surface) {
    if (asciiKey.length() != surface.length() || asciiKey.length() > MAX_WORD_LEN) {
      return false;
    }
    int core = encodeCoreMask(asciiKey, surface);
    int circ = encodeCircMask(asciiKey, surface);
    return decodeBitmask(asciiKey, core, circ).equals(surface);
  }

  private static int toggleableCoreMask(String asciiKey) {
    int mask = 0;
    for (int i = 0; i < asciiKey.length(); i++) {
      char c = asciiKey.charAt(i);
      if (c == 'c' || c == 'g' || c == 'i' || c == 'o' || c == 's' || c == 'u') {
        mask |= (1 << i);
      }
    }
    return mask;
  }

  private static int toggleableCircMask(String asciiKey) {
    int mask = 0;
    for (int i = 0; i < asciiKey.length(); i++) {
      char c = asciiKey.charAt(i);
      if (c == 'a' || c == 'i' || c == 'u') {
        mask |= (1 << i);
      }
    }
    return mask;
  }

  private static int encodeCoreMask(String asciiKey, String surface) {
    int mask = 0;
    for (int i = 0; i < asciiKey.length(); i++) {
      char sc = surface.charAt(i);
      char ac = asciiKey.charAt(i);
      if (sc != ac && (sc == 'ç' || sc == 'ğ' || sc == 'ı' || sc == 'ö' || sc == 'ş' || sc == 'ü')) {
        mask |= (1 << i);
      }
    }
    return mask;
  }

  private static int encodeCircMask(String asciiKey, String surface) {
    int mask = 0;
    for (int i = 0; i < asciiKey.length(); i++) {
      char sc = surface.charAt(i);
      if (sc == 'â' || sc == 'î' || sc == 'û') {
        mask |= (1 << i);
      }
    }
    return mask;
  }

  private static String decodeBitmask(String asciiKey, int coreMask, int circMask) {
    if (coreMask == 0 && circMask == 0) {
      return asciiKey;
    }
    char[] out = asciiKey.toCharArray();
    for (int i = 0; i < out.length; i++) {
      int bit = 1 << i;
      if ((circMask & bit) != 0) {
        out[i] =
            switch (out[i]) {
              case 'a' -> 'â';
              case 'i' -> 'î';
              case 'u' -> 'û';
              default -> out[i];
            };
      } else if ((coreMask & bit) != 0) {
        out[i] =
            switch (out[i]) {
              case 'c' -> 'ç';
              case 'g' -> 'ğ';
              case 'i' -> 'ı';
              case 'o' -> 'ö';
              case 's' -> 'ş';
              case 'u' -> 'ü';
              default -> out[i];
            };
      }
    }
    return new String(out);
  }

  private static int sharedPrefixLen(String a, String b) {
    int n = Math.min(255, Math.min(a.length(), b.length()));
    int i = 0;
    while (i < n && a.charAt(i) == b.charAt(i)) {
      i++;
    }
    return i;
  }

  private static boolean isPureAsciiLetters(String s) {
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      if (c < 'a' || c > 'z') {
        return false;
      }
    }
    return !s.isEmpty();
  }

  private static void writeVarInt(DataOutputStream out, int v) throws IOException {
    int u = v;
    while ((u & ~0x7F) != 0) {
      out.writeByte((u & 0x7F) | 0x80);
      u >>>= 7;
    }
    out.writeByte(u);
  }

  private static int readVarInt(DataInputStream in) throws IOException {
    int shift = 0;
    int result = 0;
    while (true) {
      int b = in.readUnsignedByte();
      result |= (b & 0x7F) << shift;
      if ((b & 0x80) == 0) {
        return result;
      }
      shift += 7;
    }
  }

  private static int countNonAscii(Map<String, Integer> surfaces, String ascii) {
    if (surfaces == null) {
      return 0;
    }
    int count = 0;
    for (var se : surfaces.entrySet()) {
      if (!se.getKey().equals(ascii)) {
        count += se.getValue();
      }
    }
    return count;
  }

  private static int sumValues(Map<String, Integer> map) {
    int s = 0;
    for (int v : map.values()) {
      s += v;
    }
    return s;
  }

  private static void recordSurfaceObservation(
      Map<String, Map<String, Integer>> primaryBuckets,
      Map<String, Map<String, Integer>> circBuckets,
      boolean circOk,
      String asciiLower,
      String trLower) {
    primaryBuckets.computeIfAbsent(asciiLower, k -> new HashMap<>(2)).merge(trLower, 1, Integer::sum);
    if (circOk) {
      circBuckets.computeIfAbsent(asciiLower, k -> new HashMap<>(2)).merge(trLower, 1, Integer::sum);
    }
  }

  private static boolean hasEmpiricalCircumflexSurface(
      Map<String, Integer> circSurfaces, int circTotal) {
    int circCount = 0;
    for (var e : circSurfaces.entrySet()) {
      if (TurkishAlphabet.INSTANCE.containsCircumflex(e.getKey())) {
        circCount += e.getValue();
      }
    }
    return circCount >= 3 && circTotal > 0 && (double) circCount / circTotal >= 0.002;
  }
}
