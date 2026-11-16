package zemberek.apps.demo;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;
import zemberek.apps.demo.NormalizationInspector.NormalizeResponse;
import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.analysis.SentenceAnalysis;
import zemberek.morphology.analysis.SentenceWordAnalysis;
import zemberek.morphology.analysis.SingleAnalysis;
import zemberek.normalization.TurkishSentenceNormalizer;
import zemberek.normalization.v2.SpanLatticePerceptronNormalizer;
import zemberek.tokenization.Token;
import zemberek.tokenization.TurkishSentenceExtractor;
import zemberek.tokenization.TurkishTokenizer;

/**
 * Local interactive HTTP server ({@code jdk.httpserver} + {@code Gson}) for Zemberek NLP:
 * <ul>
 *   <li><b>Normalization</b>: {@link SpanLatticePerceptronNormalizer} ({@code v2.4}) alongside
 *       {@link TurkishSentenceNormalizer} ({@code Zemberek HEAD}) with interactive multi-span
 *       lattice inspection.
 *   <li><b>Deasciifiers</b>: {@code LatticeDeasciifier} ({@code DISAMBIGUATION_ONLY}, {@code NONE},
 *       {@code FULL}, {@code KnownWordsOnly}) vs. classic {@code Deasciifier}.
 *   <li><b>Morphology &amp; Disambiguation</b>: {@link TurkishMorphology#analyzeAndDisambiguate}.
 *   <li><b>Tokenization &amp; Sentence Boundary Extraction</b>: {@link TurkishTokenizer} and
 *       {@link TurkishSentenceExtractor}.
 * </ul>
 */
public final class ZemberekDemoServer {

  public static final int DEFAULT_PORT = 8080;
  public static final String DEFAULT_HOST = "127.0.0.1";

  private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

  private static final Path USER_HOME = Paths.get(System.getProperty("user.home", "."));

  private static final List<Path> CANDIDATE_MODEL_DIRS =
      List.of(
          USER_HOME.resolve("data/turkish/normalization/norm_v2_models"),
          Paths.get("experiment/target/demo_models_v23"),
          Paths.get("../experiment/target/demo_models_v23"));

  private static final Path DEFAULT_ZEMBEREK_NORM_ROOT =
      USER_HOME.resolve("data/turkish/normalization/zemberek_norm_data");

  private static final List<Path> DEV_STATIC_DIRS =
      List.of(
          Paths.get("apps/src/main/resources/webdemo"),
          Paths.get("src/main/resources/webdemo"));

  private final TurkishMorphology morphology;
  private final SpanLatticePerceptronNormalizer perceptron;
  private final TurkishSentenceNormalizer zemberekHead;
  private final NormalizationInspector normInspector;
  private final DeasciifierInspector deasciifierInspector;
  private final double startupSeconds;
  private final Object engineLock = new Object();

  public ZemberekDemoServer(
      TurkishMorphology morphology,
      SpanLatticePerceptronNormalizer perceptron,
      TurkishSentenceNormalizer zemberekHead,
      double startupSeconds) {
    this.morphology = morphology;
    this.perceptron = perceptron;
    this.zemberekHead = zemberekHead;
    this.startupSeconds = startupSeconds;
    this.normInspector = new NormalizationInspector(perceptron, zemberekHead);
    this.deasciifierInspector = new DeasciifierInspector(morphology);
  }

  public record EngineMeta(
      String id,
      String label,
      String ramLabel,
      String testSliceErrPct,
      String nasty5kErrPct,
      String cleanFprPct,
      String speedTokSec) {}

  public record ServerInfoResponse(
      String appTitle,
      String version,
      boolean loadedFromCache,
      double startupSeconds,
      List<String> modules,
      List<EngineMeta> engines,
      List<DeasciifierInspector.DeasciifierEngineMeta> deasciifiers) {}

  public record MorphCandidateDto(
      String formatted,
      String lemma,
      String stem,
      String ending,
      String primaryPos,
      String secondaryPos,
      boolean selected) {}

  public record MorphWordDto(
      int index,
      String surface,
      boolean unknown,
      String bestLemma,
      String bestPos,
      String bestFormatted,
      List<MorphCandidateDto> candidates) {}

  public record MorphResponse(
      String text, double latencyMs, int tokenCount, List<MorphWordDto> words) {}

  public record TokenDto(int index, String text, String type, int start, int end) {}

  public record TokenizeResponse(
      String text,
      double latencyMs,
      List<String> sentences,
      int tokenCount,
      List<TokenDto> tokens) {}

  public static ZemberekDemoServer create(Path modelDirOverride, Path zemberekNormRootOverride)
      throws IOException {
    long t0 = System.nanoTime();
    System.out.println("[ZemberekDemoServer] Initializing TurkishMorphology...");
    TurkishMorphology morphology = TurkishMorphology.createWithDefaults();

    String envNormRoot = System.getenv("ZEMBEREK_NORM_ROOT");
    Path normRoot =
        zemberekNormRootOverride != null
            ? zemberekNormRootOverride
            : (envNormRoot != null && !envNormRoot.isBlank()
                ? Paths.get(envNormRoot)
                : DEFAULT_ZEMBEREK_NORM_ROOT);
    Path lm2GramPath = normRoot.resolve("lm.2gram.slm");
    Path normDataDir = normRoot.resolve("normalization");

    String envModelDir = System.getenv("ZEMBEREK_NORM_MODELS");
    Path effectiveModelDirOverride =
        modelDirOverride != null
            ? modelDirOverride
            : (envModelDir != null && !envModelDir.isBlank() ? Paths.get(envModelDir) : null);
    Path modelDir = resolveModelDir(effectiveModelDirOverride);
    System.out.println(
        "[ZemberekDemoServer] Loading SpanLatticePerceptronNormalizer (v2.4) from "
            + modelDir.toAbsolutePath()
            + " ...");
    SpanLatticePerceptronNormalizer perceptron =
        SpanLatticePerceptronNormalizer.fromDirectory(morphology, modelDir, lm2GramPath);

    TurkishSentenceNormalizer zemberekHead = null;
    if (Files.isDirectory(normDataDir) && Files.exists(lm2GramPath)) {
      zemberekHead = new TurkishSentenceNormalizer(morphology, normDataDir, lm2GramPath);
    }

    double sec = (System.nanoTime() - t0) / 1e9;
    System.out.printf(
        Locale.US,
        "[ZemberekDemoServer] Models loaded in %.2fs (Perceptron=%.2f MB, %,d sparse features)%n",
        sec,
        perceptron.modelRamBytes() / (1024.0 * 1024.0),
        perceptron.activeSparseFeatureCount());
    return new ZemberekDemoServer(morphology, perceptron, zemberekHead, sec);
  }

  private static Path resolveModelDir(Path override) throws IOException {
    if (override != null) {
      if (Files.isDirectory(override)) {
        return override;
      }
      throw new IOException("Model directory does not exist: " + override);
    }
    for (Path cand : CANDIDATE_MODEL_DIRS) {
      if (Files.isDirectory(cand)
          && (Files.exists(cand.resolve("perceptron.bin"))
              || Files.exists(cand.resolve("perceptron_v23.bin")))) {
        return cand;
      }
    }
    throw new IOException(
        "Could not locate SpanLatticePerceptronNormalizer snapshot directory; pass --model-dir <dir>.");
  }

  public HttpServer start(String host, int port) throws IOException {
    warmup();
    HttpServer server = HttpServer.create(new InetSocketAddress(host, port), 0);
    server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());

    server.createContext("/api/info", this::handleInfo);
    server.createContext("/api/normalize", this::handleNormalize);
    server.createContext("/api/deasciify", this::handleDeasciify);
    server.createContext("/api/morphology", this::handleMorphology);
    server.createContext("/api/tokenize", this::handleTokenize);
    server.createContext("/", this::handleStatic);

    server.start();
    System.out.println("========================================================================");
    System.out.printf(
        Locale.US, "  Zemberek NLP Web Demo running at: http://%s:%d/%n", host, port);
    System.out.println(
        "  Active modules : Normalization (v2.4), Deasciifiers, Morphology, Tokenization");
    System.out.println("  Press Ctrl+C to stop.");
    System.out.println("========================================================================");
    return server;
  }

  public static void main(String[] args) throws Exception {
    int port = DEFAULT_PORT;
    String host = DEFAULT_HOST;
    Path modelDir = null;
    Path normRoot = null;

    for (int i = 0; i < args.length; i++) {
      String a = args[i];
      if (("--port".equals(a) || "-p".equals(a)) && i + 1 < args.length) {
        port = Integer.parseInt(args[++i]);
      } else if ("--host".equals(a) && i + 1 < args.length) {
        host = args[++i];
      } else if (("--model-dir".equals(a) || "--cache-dir".equals(a)) && i + 1 < args.length) {
        modelDir = Paths.get(args[++i]);
      } else if ("--norm-root".equals(a) && i + 1 < args.length) {
        normRoot = Paths.get(args[++i]);
      }
    }

    ZemberekDemoServer app = create(modelDir, normRoot);
    app.start(host, port);
  }

  public void warmup() {
    synchronized (engineLock) {
      normInspector.normalizeAndInspect(
          "yarın bende gelicem ama saat 7 de orda olurmusun", "perceptron", true, true);
      deasciifierInspector.warmup();
      morphology.analyzeAndDisambiguate("Yarın ben de geleceğim.");
    }
  }

  private void handleInfo(HttpExchange ex) throws IOException {
    if (!"GET".equalsIgnoreCase(ex.getRequestMethod())) {
      sendJson(ex, 405, Map.of("error", "Method not allowed"));
      return;
    }
    String percRam =
        String.format(Locale.US, "%.2f MB", perceptron.modelRamBytes() / (1024.0 * 1024.0));

    List<EngineMeta> engines = new ArrayList<>(2);
    engines.add(
        new EngineMeta(
            "perceptron",
            "Span-Lattice Perceptron (v2.4 · PA-I)",
            percRam,
            "80.79%",
            "90.41%",
            "1.55%",
            "13,088 tok/s"));
    if (zemberekHead != null) {
      engines.add(
          new EngineMeta(
              "zemberek_head",
              "Zemberek HEAD (Baseline)",
              "Rule + SmoothLm",
              "61.82%",
              "80.98%",
              "1.68%",
              "11,550 tok/s"));
    }

    ServerInfoResponse resp =
        new ServerInfoResponse(
            "Zemberek NLP Demo",
            "v2.4 (75K Multi-Regime)",
            true,
            Math.round(startupSeconds * 100.0) / 100.0,
            List.of("normalization", "deasciifiers", "morphology", "tokenization"),
            engines,
            DeasciifierInspector.engineMetadata());
    sendJson(ex, 200, resp);
  }

  private void handleDeasciify(HttpExchange ex) throws IOException {
    if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) {
      sendJson(ex, 405, Map.of("error", "Method not allowed"));
      return;
    }
    try {
      JsonObject body = readJsonBody(ex);
      String text =
          body.has("text") && !body.get("text").isJsonNull() ? body.get("text").getAsString() : "";
      if (text.length() > 20_000) {
        text = text.substring(0, 20_000);
      }
      String engine =
          body.has("engine") && !body.get("engine").isJsonNull()
              ? body.get("engine").getAsString()
              : "lattice_disambig";
      boolean compareAll = !body.has("compareAll") || body.get("compareAll").getAsBoolean();

      DeasciifierInspector.DeasciifyResponse res;
      synchronized (engineLock) {
        res = deasciifierInspector.deasciifyAndInspect(text, engine, compareAll);
      }
      sendJson(ex, 200, res);
    } catch (Exception e) {
      sendJson(
          ex, 500, Map.of("error", e.getMessage() == null ? "Internal error" : e.getMessage()));
    }
  }

  private void handleNormalize(HttpExchange ex) throws IOException {
    if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) {
      sendJson(ex, 405, Map.of("error", "Method not allowed"));
      return;
    }
    try {
      JsonObject body = readJsonBody(ex);
      String text =
          body.has("text") && !body.get("text").isJsonNull() ? body.get("text").getAsString() : "";
      if (text.length() > 20_000) {
        text = text.substring(0, 20_000);
      }
      String engine =
          body.has("engine") && !body.get("engine").isJsonNull()
              ? body.get("engine").getAsString()
              : "perceptron";
      boolean inspect = !body.has("inspect") || body.get("inspect").getAsBoolean();
      boolean compareAll = body.has("compareAll") && body.get("compareAll").getAsBoolean();

      NormalizeResponse res;
      synchronized (engineLock) {
        res = normInspector.normalizeAndInspect(text, engine, inspect, compareAll);
      }
      sendJson(ex, 200, res);
    } catch (Exception e) {
      sendJson(
          ex, 500, Map.of("error", e.getMessage() == null ? "Internal error" : e.getMessage()));
    }
  }

  private void handleMorphology(HttpExchange ex) throws IOException {
    if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) {
      sendJson(ex, 405, Map.of("error", "Method not allowed"));
      return;
    }
    try {
      JsonObject body = readJsonBody(ex);
      String text =
          body.has("text") && !body.get("text").isJsonNull()
              ? body.get("text").getAsString().trim()
              : "";
      if (text.length() > 5_000) {
        text = text.substring(0, 5_000);
      }
      if (text.isEmpty()) {
        sendJson(ex, 200, new MorphResponse("", 0.0, 0, List.of()));
        return;
      }

      List<MorphWordDto> words = new ArrayList<>();
      long t0 = System.nanoTime();
      synchronized (engineLock) {
        List<String> sents = TurkishSentenceExtractor.DEFAULT.fromParagraph(text);
        if (sents.isEmpty()) {
          sents = List.of(text);
        }
        int idx = 0;
        for (String s : sents) {
          if (s.isBlank()) {
            continue;
          }
          SentenceAnalysis sa = morphology.analyzeAndDisambiguate(s);
          for (SentenceWordAnalysis swa : sa.getWordAnalyses()) {
            SingleAnalysis best = swa.getBestAnalysis();
            String input = swa.getWordAnalysis().getInput();
            List<MorphCandidateDto> cands = new ArrayList<>();
            for (SingleAnalysis ca : swa.getWordAnalysis()) {
              cands.add(
                  new MorphCandidateDto(
                      ca.formatLong(),
                      ca.getDictionaryItem().lemma,
                      ca.getStem(),
                      ca.getEnding(),
                      ca.getPos().shortForm,
                      ca.getDictionaryItem().secondaryPos.shortForm,
                      ca.equals(best)));
            }
            words.add(
                new MorphWordDto(
                    idx++,
                    input,
                    best.isUnknown(),
                    best.isUnknown() ? "?" : best.getDictionaryItem().lemma,
                    best.getPos().shortForm,
                    best.formatLong(),
                    cands));
          }
        }
      }
      double ms = (System.nanoTime() - t0) / 1e6;
      sendJson(
          ex, 200, new MorphResponse(text, Math.round(ms * 1000.0) / 1000.0, words.size(), words));
    } catch (Exception e) {
      sendJson(
          ex, 500, Map.of("error", e.getMessage() == null ? "Internal error" : e.getMessage()));
    }
  }

  private void handleTokenize(HttpExchange ex) throws IOException {
    if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) {
      sendJson(ex, 405, Map.of("error", "Method not allowed"));
      return;
    }
    try {
      JsonObject body = readJsonBody(ex);
      String text =
          body.has("text") && !body.get("text").isJsonNull() ? body.get("text").getAsString() : "";
      if (text.length() > 10_000) {
        text = text.substring(0, 10_000);
      }
      long t0 = System.nanoTime();
      List<String> sents =
          text.isBlank() ? List.of() : TurkishSentenceExtractor.DEFAULT.fromParagraph(text);
      List<Token> rawToks = text.isEmpty() ? List.of() : TurkishTokenizer.DEFAULT.tokenize(text);
      double ms = (System.nanoTime() - t0) / 1e6;

      List<TokenDto> dtos = new ArrayList<>(rawToks.size());
      for (int i = 0; i < rawToks.size(); i++) {
        Token t = rawToks.get(i);
        dtos.add(new TokenDto(i, t.getText(), t.getType().name(), t.getStart(), t.getEnd()));
      }
      sendJson(
          ex,
          200,
          new TokenizeResponse(
              text, Math.round(ms * 1000.0) / 1000.0, sents, dtos.size(), dtos));
    } catch (Exception e) {
      sendJson(
          ex, 500, Map.of("error", e.getMessage() == null ? "Internal error" : e.getMessage()));
    }
  }

  private void handleStatic(HttpExchange ex) throws IOException {
    String path = ex.getRequestURI().getPath();
    if (path == null || path.equals("/") || path.isBlank()) {
      path = "/index.html";
    }
    String cleanName =
        switch (path) {
          case "/index.html",
              "/deasciifiers",
              "/deasciifier",
              "/normalization",
              "/morphology",
              "/tokenization" ->
              "index.html";
          case "/styles.css" -> "styles.css";
          case "/app.js" -> "app.js";
          default -> null;
        };
    if (cleanName == null) {
      byte[] msg = "Not Found".getBytes(StandardCharsets.UTF_8);
      ex.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
      ex.sendResponseHeaders(404, msg.length);
      try (OutputStream os = ex.getResponseBody()) {
        os.write(msg);
      }
      return;
    }

    String contentType =
        switch (cleanName) {
          case "styles.css" -> "text/css; charset=utf-8";
          case "app.js" -> "application/javascript; charset=utf-8";
          default -> "text/html; charset=utf-8";
        };

    byte[] bytes = loadStaticAsset(cleanName);
    if (bytes == null) {
      byte[] msg = ("Missing static asset: " + cleanName).getBytes(StandardCharsets.UTF_8);
      ex.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
      ex.sendResponseHeaders(500, msg.length);
      try (OutputStream os = ex.getResponseBody()) {
        os.write(msg);
      }
      return;
    }

    ex.getResponseHeaders().set("Content-Type", contentType);
    ex.getResponseHeaders().set("Cache-Control", "no-store");
    ex.sendResponseHeaders(200, bytes.length);
    try (OutputStream os = ex.getResponseBody()) {
      os.write(bytes);
    }
  }

  private static byte[] loadStaticAsset(String fileName) throws IOException {
    for (Path dir : DEV_STATIC_DIRS) {
      Path diskFile = dir.resolve(fileName);
      if (Files.exists(diskFile)) {
        return Files.readAllBytes(diskFile);
      }
    }
    try (InputStream is = ZemberekDemoServer.class.getResourceAsStream("/webdemo/" + fileName)) {
      return is == null ? null : is.readAllBytes();
    }
  }

  private static JsonObject readJsonBody(HttpExchange ex) throws IOException {
    try (InputStream is = ex.getRequestBody()) {
      String raw = new String(is.readAllBytes(), StandardCharsets.UTF_8);
      if (raw.isBlank()) {
        return new JsonObject();
      }
      return JsonParser.parseString(raw).getAsJsonObject();
    }
  }

  private static void sendJson(HttpExchange ex, int status, Object payload) throws IOException {
    byte[] bytes = GSON.toJson(payload).getBytes(StandardCharsets.UTF_8);
    ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
    ex.getResponseHeaders().set("Cache-Control", "no-store");
    ex.sendResponseHeaders(status, bytes.length);
    try (OutputStream os = ex.getResponseBody()) {
      os.write(bytes);
    }
  }
}
