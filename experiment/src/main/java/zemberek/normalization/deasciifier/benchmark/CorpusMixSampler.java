package zemberek.normalization.deasciifier.benchmark;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import zemberek.normalization.deasciifier.latticedeasciifier.Candidate;
import zemberek.normalization.deasciifier.latticedeasciifier.LexiconPriors;
import zemberek.normalization.deasciifier.latticedeasciifier.MorphologicalCandidateGenerator;
import zemberek.normalization.deasciifier.latticedeasciifier.TurkishTextCodec;
import zemberek.normalization.deasciifier.latticedeasciifier.TurkishTextCodec.Casing;
import zemberek.normalization.deasciifier.latticedeasciifier.TurkishTextCodec.Token;

/**
 * Multi-register Turkish corpus sampler, benchmark loader, and data-hygiene verifier over
 * {@code ~/data/turkish}.
 *
 * <p>Enforces two critical data-hygiene filters across web, forum, and subtitle corpora:
 * <ol>
 *   <li><b>Raw ASCII contamination guard</b>: rejects partially/fully un-diacritized Turkish lines
 *       ({@code icin}, {@code degil}, {@code cok}, {@code boyle}, {@code turkiye}, ...).
 *   <li><b>Windows-1254 / {@code Turkish.LOCALE}-lowercased dotless-{@code ı} corruption guard</b>:
 *       rejects encoding-corrupted lines containing {@code bır}, {@code bı}, {@code gıbı},
 *       {@code dıye}, {@code ıcın}, {@code degıl}, {@code hıc}, {@code bıle}, etc.
 * </ol>
 */
public final class CorpusMixSampler {

  public enum MixMode {
    /** Formal-heavy round-robin across {@code clean/} only (~56% news/legal/parliament). */
    FORMAL_CLEAN_ONLY,
    /**
     * Optimal 5-pillar multi-register mix across {@code refined/}, {@code noisy/}, and
     * {@code clean/}: 30% News, 20% Encyclopedia/Science/Legal, 18% Tech/Brands/Code-Switched,
     * 17% Colloquial/Noisy Web Forums, 15% Spoken Conversational Subtitles.
     */
    BALANCED_FIVE_PILLAR
  }

  public record InputGoldPair(String input, String gold) {}

  public record StratifiedCorpus(List<String> sentences, boolean[] circumflexIntact) {
    public StratifiedCorpus subCorpus(int count) {
      int k = Math.min(count, sentences.size());
      boolean[] subCirc = new boolean[k];
      System.arraycopy(circumflexIntact, 0, subCirc, 0, k);
      return new StratifiedCorpus(sentences.subList(0, k), subCirc);
    }
  }

  private record PillarSpec(String name, double weight, List<Path> files) {}

  private record TaggedItem(String text, boolean circIntact) {}

  private static final Set<String> CIRCUMFLEX_INTACT_FILES =
      Set.of(
          "vikipedi.txt",
          "www.bilim.org.txt",
          "tbmm-tutanak.txt",
          "radikal.txt");

  private static final Set<String> RAW_ASCII_SENTINELS =
      Set.of(
          "icin", "degil", "cok", "boyle", "soyle", "cunku", "gore", "ustelik", "ozellikle",
          "turkiye", "istanbul", "ogrenci", "calisma", "yapiyor", "oldugu", "oldugunu", "karsi",
          "buyuk", "kucuk", "guzel", "simdi", "bugun", "butun", "dogru", "uzerinde", "arasinda",
          "ulke", "dunya", "gunluk", "tesekkur", "lutfen", "gercekten", "hicbir", "hersey",
          "birsey", "arkadas", "arkadaslar");

  private static final Set<String> DOTLESS_I_CORRUPTION_SENTINELS =
      Set.of(
          "bır", "bı", "gıbı", "dıye", "ıcın", "degıl", "hıc", "bıle", "senın", "benı", "yenı",
          "ıkı", "yıne", "ederım", "dedı", "dedım", "ıle", "ıse", "ıyı", "ılgılı", "turkıye",
          "türkıye", "ıstanbul", "sımdı", "kısı", "kım", "kımse", "bızım", "sızın", "benım");

  private CorpusMixSampler() {}

  public static List<String> loadLines(Path path) throws IOException {
    List<String> out = new ArrayList<>();
    try (BufferedReader br = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
      String line;
      while ((line = br.readLine()) != null) {
        String trimmed = normalizeWhitespace(line);
        if (!trimmed.isEmpty() && !trimmed.startsWith("#")) {
          out.add(trimmed);
        }
      }
    }
    return out;
  }

  public static List<InputGoldPair> loadInputGoldPairs(Path path) throws IOException {
    List<InputGoldPair> out = new ArrayList<>();
    try (BufferedReader br = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
      String line;
      while ((line = br.readLine()) != null) {
        String raw = line.trim();
        if (raw.isEmpty() || raw.startsWith("#")) {
          continue;
        }
        int tab = raw.indexOf('\t');
        if (tab <= 0 || tab >= raw.length() - 1) {
          throw new IllegalArgumentException("Expected input\\tgold TSV line in " + path + ": " + raw);
        }
        out.add(
            new InputGoldPair(
                normalizeWhitespace(raw.substring(0, tab)),
                normalizeWhitespace(raw.substring(tab + 1))));
      }
    }
    return out;
  }

  public static List<String> loadDirectory(Path dir) throws IOException {
    List<Path> files = new ArrayList<>();
    try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir, "*.txt")) {
      for (Path p : ds) {
        files.add(p);
      }
    }
    Collections.sort(files);
    List<String> all = new ArrayList<>();
    for (Path f : files) {
      all.addAll(loadLines(f));
    }
    return all;
  }

  public static Set<Long> buildExclusionSignatures(List<List<String>> suites) {
    Set<Long> hashes = new HashSet<>(65_536);
    for (List<String> suite : suites) {
      for (String s : suite) {
        hashes.add(canonicalSignature(s));
      }
    }
    return hashes;
  }

  /**
   * Builds 5 deterministic held-out domain evaluation suites ({@code targetPerSuite} sentences
   * each, e.g. {@code 5 * 2,500 = 12,500} sentences total) across all 5 registers of
   * {@code ~/data/turkish}, verifying every sentence for authentic diacritization.
   */
  public static Map<String, List<String>> sampleHeldOutFiveDomainSuites(
      Path dataRoot,
      int targetPerSuite,
      Set<Long> existingForbidden,
      MorphologicalCandidateGenerator morph,
      long seed)
      throws IOException {
    Path ref = dataRoot.resolve("refined");
    Path noisy = dataRoot.resolve("noisy");
    Path clean = dataRoot.resolve("clean");

    Map<String, List<String>> suites = new LinkedHashMap<>();
    Set<Long> seen = new HashSet<>(existingForbidden);
    LexiconPriors seedPriors = LexiconPriors.defaultsOnly();

    suites.put(
        "HeldOut-News-Economy-2500",
        sampleVerifiedSuiteFromSources(
            existingFiles(
                ref.resolve("radikal.txt"),
                ref.resolve("cnn-turk.txt"),
                ref.resolve("www.haberturk.com.txt"),
                ref.resolve("www.dunya.com.txt")),
            targetPerSuite,
            false,
            seen,
            morph,
            seedPriors,
            seed + 11L));

    suites.put(
        "HeldOut-Wiki-Science-Legal-2500",
        sampleVerifiedSuiteFromSources(
            existingFiles(
                ref.resolve("vikipedi.txt"),
                ref.resolve("www.bilim.org.txt"),
                ref.resolve("www.defenceturk.com.txt"),
                ref.resolve("yargitay-karar.txt")),
            targetPerSuite,
            false,
            seen,
            morph,
            seedPriors,
            seed + 22L));

    suites.put(
        "HeldOut-Tech-Brands-CodeSwitched-2500",
        sampleVerifiedSuiteFromSources(
            existingFiles(
                ref.resolve("forum.shiftdelete.net.txt"),
                ref.resolve("www.sikayetim.com.txt"),
                ref.resolve("onedio.com.txt"),
                ref.resolve("wowturkey.com.txt")),
            targetPerSuite,
            true,
            seen,
            morph,
            seedPriors,
            seed + 33L));

    suites.put(
        "HeldOut-Colloquial-Noisy-Forums-2500",
        sampleVerifiedSuiteFromSources(
            existingFiles(
                noisy.resolve("kadinlar-klubu.txt"),
                noisy.resolve("www.kizlarsoruyor.com.txt"),
                ref.resolve("forum.memurlar.net.txt"),
                ref.resolve("www.nurturia.com.tr.txt")),
            targetPerSuite,
            true,
            seen,
            morph,
            seedPriors,
            seed + 44L));

    suites.put(
        "HeldOut-Spoken-Subtitles-2500",
        sampleVerifiedSuiteFromSources(
            existingFiles(
                ref.resolve("open-subtitles-tr-2018.txt"),
                clean.resolve("open-subtitles.txt")),
            targetPerSuite,
            false,
            seen,
            morph,
            seedPriors,
            seed + 55L));

    return suites;
  }

  private static List<String> sampleVerifiedSuiteFromSources(
      List<Path> sources,
      int targetTotal,
      boolean requireNoisyOrTechSignal,
      Set<Long> seen,
      MorphologicalCandidateGenerator morph,
      LexiconPriors seedPriors,
      long seed)
      throws IOException {
    if (sources.isEmpty() || targetTotal <= 0) {
      return List.of();
    }
    int perSourceQuota = (targetTotal + sources.size() - 1) / sources.size();
    List<String> collected = new ArrayList<>(targetTotal + 64);
    Random rng = new Random(seed);

    for (Path src : sources) {
      List<String> pool = new ArrayList<>(perSourceQuota * 2);
      try (BufferedReader br = Files.newBufferedReader(src, StandardCharsets.UTF_8)) {
        String raw;
        int scanned = 0;
        while ((raw = br.readLine()) != null
            && pool.size() < perSourceQuota * 2
            && scanned < 260_000) {
          scanned++;
          if ((scanned & 3) != 3) {
            continue;
          }
          String s = normalizeWhitespace(raw);
          if (!isAuthenticTurkishSentence(s)) {
            continue;
          }
          long sig = canonicalSignature(s);
          if (seen.contains(sig)) {
            continue;
          }
          if (requireNoisyOrTechSignal && !hasInterestingNoisyOrTechSignal(s, seedPriors)) {
            continue;
          }
          if (hasLazyUndiacritizedTurkishWord(s, morph, seedPriors)) {
            continue;
          }
          seen.add(sig);
          pool.add(s);
        }
      }
      Collections.shuffle(pool, rng);
      int take = Math.min(perSourceQuota, pool.size());
      for (int i = 0; i < take; i++) {
        collected.add(pool.get(i));
      }
    }
    Collections.shuffle(collected, rng);
    return collected.size() > targetTotal
        ? new ArrayList<>(collected.subList(0, targetTotal))
        : collected;
  }

  /**
   * Samples {@code targetCount} hygiene-verified training sentences according to {@code mode},
   * strictly excluding any sentence whose signature appears in {@code forbiddenSignatures}.
   */
  public static StratifiedCorpus sampleTrainingCorpus(
      Path dataRoot, int targetCount, MixMode mode, Set<Long> forbiddenSignatures, long seed)
      throws IOException {
    return switch (mode) {
      case FORMAL_CLEAN_ONLY ->
          sampleFormalCleanRoundRobin(
              dataRoot.resolve("clean"), targetCount, forbiddenSignatures, seed);
      case BALANCED_FIVE_PILLAR ->
          sampleBalancedFivePillar(dataRoot, targetCount, forbiddenSignatures, seed);
    };
  }

  private static StratifiedCorpus sampleBalancedFivePillar(
      Path dataRoot, int targetCount, Set<Long> forbiddenSignatures, long seed) throws IOException {
    Path ref = dataRoot.resolve("refined");
    Path noisy = dataRoot.resolve("noisy");
    Path clean = dataRoot.resolve("clean");

    List<PillarSpec> pillars =
        List.of(
            new PillarSpec(
                "News-Economy",
                0.30,
                existingFiles(
                    ref.resolve("radikal.txt"),
                    ref.resolve("cnn-turk.txt"),
                    ref.resolve("www.haberturk.com.txt"),
                    ref.resolve("www.dunya.com.txt"),
                    ref.resolve("ntv-750k-sentences.txt"),
                    ref.resolve("star-gazete.txt"),
                    ref.resolve("www.iha.com.tr.txt"))),
            new PillarSpec(
                "Wiki-Science-Legal",
                0.20,
                existingFiles(
                    ref.resolve("vikipedi.txt"),
                    ref.resolve("www.bilim.org.txt"),
                    ref.resolve("www.defenceturk.com.txt"),
                    ref.resolve("yargitay-karar.txt"),
                    ref.resolve("tbmm-tutanak.txt"),
                    ref.resolve("www.hukuki.net.txt"))),
            new PillarSpec(
                "Tech-Brands-CodeSwitched",
                0.18,
                existingFiles(
                    ref.resolve("forum.shiftdelete.net.txt"),
                    ref.resolve("onedio.com.txt"),
                    ref.resolve("www.sikayetim.com.txt"),
                    ref.resolve("wowturkey.com.txt"))),
            new PillarSpec(
                "Colloquial-Noisy-Forums",
                0.17,
                existingFiles(
                    noisy.resolve("kadinlar-klubu.txt"),
                    noisy.resolve("www.kizlarsoruyor.com.txt"),
                    ref.resolve("forum.memurlar.net.txt"),
                    ref.resolve("www.nurturia.com.tr.txt"),
                    ref.resolve("www.tvyorumlari.com.txt"))),
            new PillarSpec(
                "Spoken-Subtitles",
                0.15,
                existingFiles(
                    ref.resolve("open-subtitles-tr-2018.txt"),
                    clean.resolve("open-subtitles.txt"),
                    ref.resolve("subtitle-tr-10M-sentences.txt"))));

    List<TaggedItem> collected = new ArrayList<>(targetCount + 1_024);
    Set<Long> seen = new HashSet<>(targetCount * 2);

    for (PillarSpec pillar : pillars) {
      int quota = (int) Math.round(targetCount * pillar.weight());
      if (pillar.files().isEmpty() || quota <= 0) {
        continue;
      }
      collected.addAll(readRoundRobinFromFiles(pillar.files(), quota, forbiddenSignatures, seen));
    }

    Random rng = new Random(seed);
    Collections.shuffle(collected, rng);
    int finalCount = Math.min(targetCount, collected.size());
    List<String> sentences = new ArrayList<>(finalCount);
    boolean[] circ = new boolean[finalCount];
    for (int i = 0; i < finalCount; i++) {
      TaggedItem ts = collected.get(i);
      sentences.add(ts.text());
      circ[i] = ts.circIntact();
    }
    return new StratifiedCorpus(sentences, circ);
  }

  private static StratifiedCorpus sampleFormalCleanRoundRobin(
      Path cleanDir, int targetCount, Set<Long> forbiddenSignatures, long seed) throws IOException {
    List<Path> files =
        existingFiles(
            cleanDir.resolve("vikipedi.txt"),
            cleanDir.resolve("radikal.txt"),
            cleanDir.resolve("cnn-turk.txt"),
            cleanDir.resolve("www.haberturk.com.txt"),
            cleanDir.resolve("www.dunya.com.txt"),
            cleanDir.resolve("ntv-750k-sentences.txt"),
            cleanDir.resolve("www.bilim.org.txt"),
            cleanDir.resolve("www.defenceturk.com.txt"),
            cleanDir.resolve("onedio.com.txt"),
            cleanDir.resolve("open-subtitles.txt"),
            cleanDir.resolve("forum.shiftdelete.net.txt"),
            cleanDir.resolve("forum.memurlar.net.txt"),
            cleanDir.resolve("tbmm-tutanak.txt"),
            cleanDir.resolve("yargitay-karar.txt"),
            cleanDir.resolve("www.hukuki.net.txt"),
            cleanDir.resolve("star-gazete.txt"),
            cleanDir.resolve("www.iha.com.tr.txt"),
            cleanDir.resolve("wowturkey.com.txt"));

    Set<Long> seen = new HashSet<>(targetCount * 2);
    var tagged = readRoundRobinFromFiles(files, targetCount, forbiddenSignatures, seen);
    Collections.shuffle(tagged, new Random(seed));
    int n = Math.min(targetCount, tagged.size());
    List<String> sentences = new ArrayList<>(n);
    boolean[] circ = new boolean[n];
    for (int i = 0; i < n; i++) {
      sentences.add(tagged.get(i).text());
      circ[i] = tagged.get(i).circIntact();
    }
    return new StratifiedCorpus(sentences, circ);
  }

  private static List<TaggedItem> readRoundRobinFromFiles(
      List<Path> files, int quota, Set<Long> forbidden, Set<Long> seen) throws IOException {
    List<TaggedItem> out = new ArrayList<>(quota);
    List<BufferedReader> readers = new ArrayList<>(files.size());
    boolean[] fileCirc = new boolean[files.size()];
    boolean[] active = new boolean[files.size()];
    try {
      for (int i = 0; i < files.size(); i++) {
        readers.add(Files.newBufferedReader(files.get(i), StandardCharsets.UTF_8));
        fileCirc[i] = CIRCUMFLEX_INTACT_FILES.contains(files.get(i).getFileName().toString());
        active[i] = true;
      }
      int openCount = files.size();
      while (out.size() < quota && openCount > 0) {
        for (int i = 0; i < readers.size() && out.size() < quota; i++) {
          if (!active[i]) {
            continue;
          }
          int attempts = 0;
          while (attempts < 12) {
            attempts++;
            String line = readers.get(i).readLine();
            if (line == null) {
              active[i] = false;
              openCount--;
              break;
            }
            String s = normalizeWhitespace(line);
            if (!isAuthenticTurkishSentence(s)) {
              continue;
            }
            long sig = canonicalSignature(s);
            if (forbidden.contains(sig) || !seen.add(sig)) {
              continue;
            }
            out.add(new TaggedItem(s, fileCirc[i]));
            break;
          }
        }
      }
    } finally {
      for (BufferedReader br : readers) {
        try {
          br.close();
        } catch (IOException ignored) {
        }
      }
    }
    return out;
  }

  /**
   * Verifies that {@code s} is genuine Turkish text free from raw-ASCII stripping, Windows-1254
   * dotless-{@code ı} corruption, UTF-8 mojibake, and HTML/markup artifacts.
   */
  public static boolean isAuthenticTurkishSentence(String s) {
    int len = s.length();
    if (len < 18 || len > 320) {
      return false;
    }
    if (s.indexOf('\uFFFD') >= 0
        || s.contains("Ã")
        || s.contains("Ä")
        || s.contains("Å")
        || s.contains("<http")
        || s.contains("[quote")
        || s.contains("href=")) {
      return false;
    }

    int letters = 0;
    int upperLetters = 0;
    int trDiacritics = 0;
    int ambiguousAscii = 0;
    for (int i = 0; i < len; i++) {
      char c = s.charAt(i);
      if (TurkishTextCodec.isLetter(c)) {
        letters++;
        if (Character.isUpperCase(c)) {
          upperLetters++;
        }
        if (TurkishTextCodec.isPinnedTurkishDiacritic(c)) {
          trDiacritics++;
        } else {
          char low = (c >= 'A' && c <= 'Z') ? (char) (c + 32) : c;
          if (low == 'c' || low == 'g' || low == 'i' || low == 'o' || low == 's' || low == 'u') {
            ambiguousAscii++;
          }
        }
      }
    }
    if (letters < 14 || upperLetters * 2 > letters) {
      return false;
    }
    if (trDiacritics == 0 && ambiguousAscii >= 3) {
      return false;
    }
    if (ambiguousAscii >= 10 && (double) trDiacritics / (trDiacritics + ambiguousAscii) < 0.11) {
      return false;
    }

    List<Token> tokens = TurkishTextCodec.tokenize(s);
    if (tokens.size() < 3 || tokens.size() > 50) {
      return false;
    }
    for (Token tok : tokens) {
      if (tok.original().length() > 22 && !tok.immutable()) {
        return false;
      }
      if (tok.sentenceStart() && tok.original().charAt(0) == 'ı') {
        return false;
      }
      if (tok.casing() == Casing.MIXED && tok.original().indexOf('ı') >= 0) {
        return false;
      }
      if (tok.immutable() || tok.casing() == Casing.UPPER) {
        continue;
      }
      String trLower = TurkishTextCodec.toTurkishLower(tok.original());
      if (trLower.endsWith("smile")
          || RAW_ASCII_SENTINELS.contains(trLower)
          || DOTLESS_I_CORRUPTION_SENTINELS.contains(trLower)) {
        return false;
      }
    }
    return true;
  }

  private static boolean hasInterestingNoisyOrTechSignal(String s, LexiconPriors seedPriors) {
    for (Token tok : TurkishTextCodec.tokenize(s)) {
      if (tok.immutable()
          && tok.original().length() >= 4
          && !Character.isDigit(tok.original().charAt(0))) {
        return true;
      }
      String low = TurkishTextCodec.toTurkishLower(tok.original());
      if (TurkishTextCodec.hasLetterElongation(tok.asciiLower())
          || hasFinalDoubleLetter(tok.asciiLower())) {
        return true;
      }
      if (low.endsWith("cam")
          || low.endsWith("cem")
          || low.endsWith("yom")
          || low.endsWith("yoz")
          || low.endsWith("yolar")
          || low.equals("bişey")
          || low.equals("birşey")
          || low.equals("herşey")
          || low.equals("yanlız")
          || low.equals("şarz")
          || low.equals("tabiki")
          || low.equals("bikaç")) {
        return true;
      }
      if (seedPriors.isForeignAsciiKeep(tok.asciiLower(), tok.casing())) {
        return true;
      }
    }
    return false;
  }

  private static boolean hasLazyUndiacritizedTurkishWord(
      String s, MorphologicalCandidateGenerator morph, LexiconPriors seedPriors) {
    for (Token tok : TurkishTextCodec.tokenize(s)) {
      if (tok.immutable() || tok.numeralApostrophe() || tok.hasPinnedTurkish()) {
        continue;
      }
      if (tok.casing() == Casing.UPPER || TurkishTextCodec.isCamelCaseIdentifier(tok.original())) {
        continue;
      }
      if (seedPriors.isForeignAsciiKeep(tok.asciiLower(), tok.casing())) {
        continue;
      }
      if (!tok.sentenceStart() && tok.casing() == Casing.TITLE) {
        continue;
      }
      String low = TurkishTextCodec.toTurkishLower(tok.original());
      if (morph.analyzeStrictBest(low) != null) {
        continue;
      }
      List<Candidate> asciiCands = morph.analyzePlainWithFallbacks(tok.asciiLower(), null);
      if (!asciiCands.isEmpty()) {
        return true;
      }
    }
    return false;
  }

  private static boolean hasFinalDoubleLetter(String asciiLower) {
    int n = asciiLower.length();
    if (n < 5) {
      return false;
    }
    char c = asciiLower.charAt(n - 1);
    return c >= 'a' && c <= 'z' && c == asciiLower.charAt(n - 2);
  }

  private static List<Path> existingFiles(Path... candidates) {
    List<Path> out = new ArrayList<>(candidates.length);
    for (Path p : candidates) {
      if (Files.exists(p)) {
        out.add(p);
      }
    }
    return out;
  }

  private static String normalizeWhitespace(String line) {
    String t = line.trim();
    if (t.indexOf('\t') < 0 && !t.contains("  ")) {
      return t;
    }
    StringBuilder sb = new StringBuilder(t.length());
    boolean prevWs = false;
    for (int i = 0; i < t.length(); i++) {
      char c = t.charAt(i);
      if (Character.isWhitespace(c)) {
        if (!prevWs) {
          sb.append(' ');
          prevWs = true;
        }
      } else {
        sb.append(c);
        prevWs = false;
      }
    }
    return sb.toString();
  }

  public static long canonicalSignature(String s) {
    String ascii = TurkishTextCodec.toAsciiLower(s).toLowerCase(Locale.ROOT);
    long h = 0xcbf29ce484222325L;
    for (int i = 0; i < ascii.length(); i++) {
      char c = ascii.charAt(i);
      if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')) {
        h ^= c;
        h *= 0x100000001b3L;
      }
    }
    return h;
  }
}
