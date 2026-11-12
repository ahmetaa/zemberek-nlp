package zemberek.normalization.deasciifier.latticedeasciifier;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import zemberek.core.turkish.PrimaryPos;
import zemberek.normalization.deasciifier.latticedeasciifier.TurkishTextCodec.Casing;
import zemberek.normalization.deasciifier.latticedeasciifier.TurkishTextCodec.Token;

/**
 * Empirical lexical priors and orthographic shields for Turkish deasciification:
 * <ul>
 *   <li>Case-aware surface confusion priors {@code log P(surface | ascii, casing)} mined from
 *       hygiene-verified text (separating lowercase common words like {@code sisli}/{@code bölü}
 *       from capitalized proper names like {@code Şişli}/{@code Bolu})
 *   <li>Backoff corpus lemma log-counts ({@link QuantizedWeightTable})
 *   <li>Foreign / technical ASCII-keep words ({@code BIOS}, {@code Windows}, {@code ISO}, ...)
 *   <li>All-caps Turkish acronym restorations ({@code TÜİK}, {@code TÜBİTAK}, {@code ÖSYM}, ...)
 * </ul>
 */
public final class LexiconPriors {

  private static final int MAGIC = 0x5A4C5052; // "ZLPR"
  private static final int VERSION = 2;

  /** Empirical candidate distribution for an ASCII surface key. */
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
  private final Map<String, ConfusionSet> confusionMap;
  private final Map<String, ConfusionSet> properConfusionMap;
  private final QuantizedWeightTable lemmaLogCounts;

  public LexiconPriors(
      Set<String> minedForeignKeep,
      Map<String, ConfusionSet> minedConfusions,
      Map<String, ConfusionSet> minedProperConfusions,
      QuantizedWeightTable lemmaLogCounts) {
    Set<String> foreign = new HashSet<>(SEED_FOREIGN_KEEP);
    if (minedForeignKeep != null) {
      foreign.addAll(minedForeignKeep);
    }
    this.foreignKeepWords = Collections.unmodifiableSet(foreign);

    Map<String, ConfusionSet> conf = new HashMap<>();
    if (minedConfusions != null) {
      conf.putAll(minedConfusions);
    }
    for (var e : SEED_UNLISTED_TURKISH.entrySet()) {
      conf.putIfAbsent(
          e.getKey(), new ConfusionSet(new String[] {e.getValue()}, new float[] {0.0f}, 10));
    }
    this.confusionMap = Collections.unmodifiableMap(conf);
    this.properConfusionMap =
        minedProperConfusions != null ? Map.copyOf(minedProperConfusions) : Map.of();
    this.lemmaLogCounts = lemmaLogCounts != null ? lemmaLogCounts : QuantizedWeightTable.empty();
  }

  public static LexiconPriors defaultsOnly() {
    return new LexiconPriors(Set.of(), Map.of(), Map.of(), QuantizedWeightTable.empty());
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

  /** Returns the lowercase common-word confusion prior (with fallback to proper-noun priors). */
  public ConfusionSet getConfusion(String asciiLower) {
    ConfusionSet lower = confusionMap.get(asciiLower);
    return lower != null ? lower : properConfusionMap.get(asciiLower);
  }

  /**
   * Returns the casing-appropriate confusion prior: mid-sentence {@code TitleCase} tokens consult
   * {@code properConfusionMap} first ({@code Bolu}, {@code Şişli}, {@code Mevlânâ}), while
   * lowercase and sentence-initial tokens consult {@code confusionMap} first ({@code bölü}, {@code
   * sisli}).
   */
  public ConfusionSet getConfusion(String asciiLower, Casing casing, boolean sentenceStart) {
    if (!sentenceStart && casing == Casing.TITLE) {
      ConfusionSet proper = properConfusionMap.get(asciiLower);
      return proper != null ? proper : confusionMap.get(asciiLower);
    }
    return getConfusion(asciiLower);
  }

  public boolean hasLowercaseConfusion(String asciiLower) {
    return confusionMap.containsKey(asciiLower);
  }

  public boolean hasProperConfusion(String asciiLower) {
    return properConfusionMap.containsKey(asciiLower);
  }

  public boolean hasCircumflexReading(String asciiLower) {
    return hasCircumflexIn(confusionMap.get(asciiLower))
        || hasCircumflexIn(properConfusionMap.get(asciiLower));
  }

  private static boolean hasCircumflexIn(ConfusionSet cs) {
    if (cs == null) {
      return false;
    }
    for (String s : cs.surfaces) {
      if (containsCircumflex(s)) {
        return true;
      }
    }
    return false;
  }

  public boolean isTabooSurface(String lowerSurface) {
    return TABOO_SURFACES.contains(lowerSurface);
  }

  public float getLemmaLogCount(Candidate cand) {
    float v = lemmaLogCounts.get(cand.lemmaHash);
    if (v == 0.0f && cand.baseLemmaHash != cand.lemmaHash) {
      v = lemmaLogCounts.get(cand.baseLemmaHash);
    }
    return v;
  }

  public int confusionSize() {
    return confusionMap.size() + properConfusionMap.size();
  }

  public int commonConfusionSize() {
    return confusionMap.size();
  }

  public int properConfusionSize() {
    return properConfusionMap.size();
  }

  public int lemmaPriorSize() {
    return lemmaLogCounts.size();
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

    writeConfusionMap(out, confusionMap, true);
    writeConfusionMap(out, properConfusionMap, false);
    lemmaLogCounts.serialize(out);
  }

  private static void writeConfusionMap(
      DataOutputStream out, Map<String, ConfusionSet> map, boolean skipSeedDefaults)
      throws IOException {
    List<String> sortedKeys = new ArrayList<>(map.size());
    for (String k : map.keySet()) {
      ConfusionSet cs = map.get(k);
      if (skipSeedDefaults
          && SEED_UNLISTED_TURKISH.containsKey(k)
          && cs.surfaces.length == 1
          && cs.surfaces[0].equals(SEED_UNLISTED_TURKISH.get(k))) {
        continue;
      }
      sortedKeys.add(k);
    }
    Collections.sort(sortedKeys);

    out.writeInt(sortedKeys.size());
    for (String key : sortedKeys) {
      ConfusionSet cs = map.get(key);
      out.writeUTF(key);
      out.writeShort(Math.min(Short.MAX_VALUE, cs.totalCount));
      int m = Math.min(15, cs.surfaces.length);
      out.writeByte(m);
      for (int i = 0; i < m; i++) {
        out.writeUTF(cs.surfaces[i]);
        int q = Math.clamp(Math.round(cs.logProbs[i] * 16.0f), -127, 0);
        out.writeByte(q);
      }
    }
  }

  public static LexiconPriors deserialize(DataInputStream in) throws IOException {
    int magic = in.readInt();
    if (magic != MAGIC) {
      throw new IOException("Invalid LexiconPriors magic: " + Integer.toHexString(magic));
    }
    int version = in.readInt();
    if (version != 1 && version != VERSION) {
      throw new IOException("Unsupported LexiconPriors version: " + version);
    }

    int foreignCount = in.readInt();
    Set<String> extraForeign = new HashSet<>(foreignCount * 2 + 1);
    for (int i = 0; i < foreignCount; i++) {
      extraForeign.add(in.readUTF());
    }

    Map<String, ConfusionSet> conf = readConfusionMap(in);
    Map<String, ConfusionSet> properConf = (version >= 2) ? readConfusionMap(in) : Map.of();
    QuantizedWeightTable lemmaLogCounts = QuantizedWeightTable.deserialize(in);
    return new LexiconPriors(extraForeign, conf, properConf, lemmaLogCounts);
  }

  private static Map<String, ConfusionSet> readConfusionMap(DataInputStream in) throws IOException {
    int confCount = in.readInt();
    Map<String, ConfusionSet> conf = new HashMap<>((int) (confCount / 0.75f) + 16);
    for (int i = 0; i < confCount; i++) {
      String key = in.readUTF();
      int total = in.readShort() & 0xFFFF;
      int m = in.readByte() & 0xFF;
      String[] surfaces = new String[m];
      float[] logProbs = new float[m];
      for (int j = 0; j < m; j++) {
        surfaces[j] = in.readUTF();
        logProbs[j] = in.readByte() / 16.0f;
      }
      conf.put(key, new ConfusionSet(surfaces, logProbs, total));
    }
    return conf;
  }

  /**
   * Mines empirical surface confusion tables, proper-noun confusion tables, foreign ASCII-keep
   * words, and lemma log-counts from clean Turkish sentences.
   */
  public static LexiconPriors mineFromCorpus(
      List<String> cleanSentences,
      boolean[] circumflexIntact,
      MorphologicalCandidateGenerator morphGenerator,
      int maxConfusions,
      int maxProperConfusions,
      int maxForeignKeep,
      int maxLemmaPriors) {
    Map<String, Map<String, Integer>> lowerBuckets = new HashMap<>(65_536);
    Map<String, Map<String, Integer>> lowerCircBuckets = new HashMap<>(32_768);
    Map<String, Map<String, Integer>> sentStartBuckets = new HashMap<>(32_768);
    Map<String, Map<String, Integer>> properBuckets = new HashMap<>(32_768);
    Map<String, Map<String, Integer>> properCircBuckets = new HashMap<>(16_384);
    Map<String, Integer> pureAsciiCounts = new HashMap<>(8_192);
    Map<String, Integer> eOnlySurfaceCounts = new HashMap<>(4_096);
    Map<Long, Integer> lemmaCounts = new HashMap<>(32_768);

    for (int si = 0; si < cleanSentences.size(); si++) {
      String sent = cleanSentences.get(si);
      boolean circOk = circumflexIntact == null || circumflexIntact[si];
      List<Token> tokens = TurkishTextCodec.tokenize(sent);
      for (Token tok : tokens) {
        if (tok.immutable() || tok.numeralApostrophe() || tok.hasApostrophe()) {
          continue;
        }
        String orig = tok.original();
        if (orig.length() < 2 || orig.length() > 24) {
          continue;
        }
        if (tok.casing() == Casing.UPPER && !tok.hasPinnedTurkish()) {
          if (orig.equals(TurkishTextCodec.asciify(orig))) {
            pureAsciiCounts.merge(tok.asciiLower(), 1, Integer::sum);
          }
          continue;
        }

        String trLower = TurkishTextCodec.toTurkishLower(orig);
        String asciiLower = TurkishTextCodec.toAsciiLower(trLower);
        if (!hasAnyAmbiguousAscii(asciiLower)) {
          eOnlySurfaceCounts.merge(trLower, 1, Integer::sum);
          continue;
        }

        if (!tok.sentenceStart() && tok.casing() == Casing.TITLE) {
          properBuckets
              .computeIfAbsent(asciiLower, k -> new HashMap<>(2))
              .merge(trLower, 1, Integer::sum);
          if (circOk) {
            properCircBuckets
                .computeIfAbsent(asciiLower, k -> new HashMap<>(2))
                .merge(trLower, 1, Integer::sum);
          }
        } else if (tok.casing() == Casing.LOWER) {
          lowerBuckets
              .computeIfAbsent(asciiLower, k -> new HashMap<>(2))
              .merge(trLower, 1, Integer::sum);
          if (circOk) {
            lowerCircBuckets
                .computeIfAbsent(asciiLower, k -> new HashMap<>(2))
                .merge(trLower, 1, Integer::sum);
          }
        } else if (tok.sentenceStart() && tok.casing() == Casing.TITLE) {
          sentStartBuckets
              .computeIfAbsent(asciiLower, k -> new HashMap<>(2))
              .merge(trLower, 1, Integer::sum);
          if (circOk) {
            lowerCircBuckets
                .computeIfAbsent(asciiLower, k -> new HashMap<>(2))
                .merge(trLower, 1, Integer::sum);
          }
        }

        if (orig.equals(TurkishTextCodec.asciify(orig)) && !tok.sentenceStart()) {
          pureAsciiCounts.merge(asciiLower, 1, Integer::sum);
        }
      }
    }

    // Fold sentence-start TitleCase counts into lowerBuckets ONLY for words not dominated by
    // mid-sentence ProperNouns, so sentence-initial common words ("Ancak", "Özellikle", "Çünkü")
    // enrich lowercase priors while "Şişli"/"Bolu"/"Can" stay isolated in properBuckets.
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

    // Analyze each unique surface once to validate lexical items and accumulate lemma counts.
    Map<String, Integer> combinedSurfaceCounts = new HashMap<>(65_536);
    accumulateSurfaces(combinedSurfaceCounts, lowerBuckets);
    accumulateSurfaces(combinedSurfaceCounts, properBuckets);

    Set<String> validSurfaces = new HashSet<>(65_536);
    for (var se : combinedSurfaceCounts.entrySet()) {
      String surf = se.getKey();
      int cnt = se.getValue();
      Candidate bestAnalysis = morphGenerator.analyzeStrictBest(surf);
      if (bestAnalysis != null && bestAnalysis.pos != PrimaryPos.Unknown) {
        validSurfaces.add(surf);
        lemmaCounts.merge(bestAnalysis.lemmaHash, cnt, Integer::sum);
        if (bestAnalysis.baseLemmaHash != bestAnalysis.lemmaHash) {
          lemmaCounts.merge(bestAnalysis.baseLemmaHash, cnt, Integer::sum);
        }
      }
    }
    for (var e : eOnlySurfaceCounts.entrySet()) {
      Candidate bestAnalysis = morphGenerator.analyzeStrictBest(e.getKey());
      if (bestAnalysis != null && bestAnalysis.pos != PrimaryPos.Unknown) {
        lemmaCounts.merge(bestAnalysis.lemmaHash, e.getValue(), Integer::sum);
      }
    }

    // 1. Foreign ASCII-keep words.
    List<Map.Entry<String, Integer>> foreignCandidates = new ArrayList<>();
    for (var e : pureAsciiCounts.entrySet()) {
      String ascii = e.getKey();
      int pureCount = e.getValue();
      if (pureCount < 15
          || !hasCoreDiacriticCandidate(ascii)
          || ALLCAPS_TURKISH_ACRONYMS.containsKey(ascii)
          || validSurfaces.contains(ascii)) {
        continue;
      }
      int nonAsciiCount =
          countNonAscii(lowerBuckets.get(ascii), ascii)
              + countNonAscii(properBuckets.get(ascii), ascii);
      if ((double) pureCount / (pureCount + nonAsciiCount) >= 0.97
          && morphGenerator.analyzePlainWithFallbacks(ascii, null).isEmpty()) {
        foreignCandidates.add(e);
      }
    }
    foreignCandidates.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
    Set<String> minedForeign = new HashSet<>();
    for (int i = 0; i < Math.min(maxForeignKeep, foreignCandidates.size()); i++) {
      minedForeign.add(foreignCandidates.get(i).getKey());
    }

    // 2. Lowercase common-word confusion sets + mid-sentence ProperNoun confusion sets.
    Map<String, ConfusionSet> minedLower =
        selectTopConfusionSets(
            lowerBuckets,
            lowerCircBuckets,
            minedForeign,
            validSurfaces,
            morphGenerator,
            maxConfusions,
            false);

    Map<String, ConfusionSet> minedProper =
        selectTopConfusionSets(
            properBuckets,
            properCircBuckets,
            minedForeign,
            validSurfaces,
            morphGenerator,
            maxProperConfusions,
            true);

    // 3. Lemma log-counts table.
    long[] lemmaKeys = new long[lemmaCounts.size()];
    float[] lemmaVals = new float[lemmaCounts.size()];
    int p = 0;
    for (var e : lemmaCounts.entrySet()) {
      if (e.getValue() >= 3) {
        lemmaKeys[p] = e.getKey();
        lemmaVals[p] = (float) Math.log1p(e.getValue());
        p++;
      }
    }
    QuantizedWeightTable lemmaTable =
        QuantizedWeightTable.build(
            Arrays.copyOf(lemmaKeys, p), Arrays.copyOf(lemmaVals, p), maxLemmaPriors);

    return new LexiconPriors(minedForeign, minedLower, minedProper, lemmaTable);
  }

  private static Map<String, ConfusionSet> selectTopConfusionSets(
      Map<String, Map<String, Integer>> bucketsMap,
      Map<String, Map<String, Integer>> circBucketsMap,
      Set<String> minedForeign,
      Set<String> validSurfaces,
      MorphologicalCandidateGenerator morphGenerator,
      int maxEntries,
      boolean properMode) {
    if (maxEntries <= 0) {
      return Map.of();
    }
    record ScoredBucket(String key, ConfusionSet set, int priority) {}
    List<ScoredBucket> buckets = new ArrayList<>();

    for (var e : bucketsMap.entrySet()) {
      String ascii = e.getKey();
      if (SEED_FOREIGN_KEEP.contains(ascii) || minedForeign.contains(ascii)) {
        continue;
      }
      Map<String, Integer> rawAll = e.getValue();
      int allTotal = sumValues(rawAll);
      if (allTotal < 4) {
        continue;
      }

      Map<String, Integer> circRaw = circBucketsMap.get(ascii);
      int circTotal = circRaw != null ? sumValues(circRaw) : 0;
      boolean circumflexBucket =
          circRaw != null && hasEmpiricalCircumflexSurface(circRaw, circTotal);
      Map<String, Integer> raw = circumflexBucket ? circRaw : rawAll;
      int rawTotal = circumflexBucket ? circTotal : allTotal;
      int minRawTotal = circumflexBucket ? 2 : 4;
      if (rawTotal < minRawTotal) {
        continue;
      }

      List<Map.Entry<String, Integer>> valid = new ArrayList<>(raw.size());
      int keptTotal = 0;
      for (var se : raw.entrySet()) {
        String surf = se.getKey();
        if ("halâ".equals(surf) || "hâla".equals(surf)) {
          continue;
        }
        int cnt = se.getValue();
        boolean morphKnown = validSurfaces.contains(surf);
        double minShare = morphKnown ? 0.00025 : (properMode ? 0.08 : 0.025);
        int minAbs = morphKnown ? (circumflexBucket ? 1 : 2) : (properMode ? 8 : 5);
        if (cnt >= minAbs && (double) cnt / rawTotal >= minShare) {
          valid.add(se);
          keptTotal += cnt;
        }
      }
      if (valid.isEmpty()) {
        continue;
      }
      valid.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));

      String topSurf = valid.getFirst().getKey();
      boolean multiSurface = valid.size() >= 2;
      boolean singleCircumflexWord =
          circumflexBucket && valid.size() == 1 && containsCircumflex(topSurf);
      boolean singleUseful =
          properMode
              ? (valid.size() == 1 && keptTotal >= 12)
              : (valid.size() == 1
                  && !topSurf.equals(ascii)
                  && keptTotal >= 6
                  && (morphGenerator.analyzePlainAsciiUncached(ascii).size() != 1));
      if (!multiSurface && !singleCircumflexWord && !singleUseful) {
        continue;
      }

      int m = Math.min(6, valid.size());
      double denom = keptTotal + 0.5 * Math.max(2, m);
      String[] surfaces = new String[m];
      float[] logProbs = new float[m];
      for (int i = 0; i < m; i++) {
        surfaces[i] = valid.get(i).getKey();
        logProbs[i] = (float) Math.log((valid.get(i).getValue() + 0.5) / denom);
      }

      int scale = circumflexBucket ? 3 : 1;
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

  private static void accumulateSurfaces(
      Map<String, Integer> dest, Map<String, Map<String, Integer>> source) {
    for (Map<String, Integer> map : source.values()) {
      for (var se : map.entrySet()) {
        dest.merge(se.getKey(), se.getValue(), Integer::sum);
      }
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

  private static boolean hasEmpiricalCircumflexSurface(
      Map<String, Integer> circSurfaces, int circTotal) {
    int circCount = 0;
    for (var e : circSurfaces.entrySet()) {
      if (containsCircumflex(e.getKey())) {
        circCount += e.getValue();
      }
    }
    return circCount >= 3 && circTotal > 0 && (double) circCount / circTotal >= 0.002;
  }

  private static boolean containsCircumflex(String s) {
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      if (c == 'â' || c == 'Â' || c == 'î' || c == 'Î' || c == 'û' || c == 'Û') {
        return true;
      }
    }
    return false;
  }

  private static boolean hasCoreDiacriticCandidate(String asciiLower) {
    for (int i = 0; i < asciiLower.length(); i++) {
      switch (asciiLower.charAt(i)) {
        case 'c', 'g', 'i', 'o', 's', 'u' -> {
          return true;
        }
        default -> {}
      }
    }
    return false;
  }

  private static boolean hasAnyAmbiguousAscii(String asciiLower) {
    for (int i = 0; i < asciiLower.length(); i++) {
      switch (asciiLower.charAt(i)) {
        case 'a', 'c', 'g', 'i', 'o', 's', 'u' -> {
          return true;
        }
        default -> {}
      }
    }
    return false;
  }
}
