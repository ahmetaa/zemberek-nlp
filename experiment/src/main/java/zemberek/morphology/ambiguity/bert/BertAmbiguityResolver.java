package zemberek.morphology.ambiguity.bert;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import zemberek.core.logging.Log;
import zemberek.core.turkish.PrimaryPos;
import zemberek.core.turkish.SecondaryPos;
import zemberek.morphology.ambiguity.AmbiguityResolver;
import zemberek.morphology.analysis.AnalysisFormatters;
import zemberek.morphology.analysis.SentenceAnalysis;
import zemberek.morphology.analysis.SentenceWordAnalysis;
import zemberek.morphology.analysis.SingleAnalysis;
import zemberek.morphology.analysis.WordAnalysis;
import zemberek.morphology.lexicon.DictionaryItem;
import zemberek.morphology.morphotactics.Morpheme;

/**
 * Morphological ambiguity resolver that delegates contextual disambiguation decisions to a neural
 * transformer model (such as BERTurk / Turkish BERT).
 *
 * <p>Supports HTTP model servers (FastAPI/Flask/Triton), local subprocesses, and custom predictors
 * while seamlessly implementing Zemberek's {@link AmbiguityResolver} interface.
 */
public class BertAmbiguityResolver implements AmbiguityResolver {

  private final Predictor predictor;
  private final boolean fallbackOnFailure;

  public BertAmbiguityResolver(Predictor predictor) {
    this(predictor, true);
  }

  public BertAmbiguityResolver(Predictor predictor, boolean fallbackOnFailure) {
    this.predictor = Objects.requireNonNull(predictor, "predictor must not be null");
    this.fallbackOnFailure = fallbackOnFailure;
  }

  public static BertAmbiguityResolver fromEndpoint(String url) {
    return builder().endpoint(url).build();
  }

  public static BertAmbiguityResolver fromEndpoint(URI uri, Duration timeout) {
    return builder().endpoint(uri).timeout(timeout).build();
  }

  public static BertAmbiguityResolver fromPredictor(Predictor predictor) {
    return new BertAmbiguityResolver(predictor);
  }

  public static Builder builder() {
    return new Builder();
  }

  @Override
  public SentenceAnalysis disambiguate(String sentence, List<WordAnalysis> allAnalyses) {
    if (allAnalyses == null || allAnalyses.isEmpty()) {
      return new SentenceAnalysis(sentence, Collections.emptyList());
    }

    // Step 1: Detect ambiguous words
    List<AmbiguousWordRequest> ambiguousWords = new ArrayList<>();
    for (int i = 0; i < allAnalyses.size(); i++) {
      WordAnalysis wa = allAnalyses.get(i);
      if (wa.analysisCount() > 1) {
        ambiguousWords.add(toWordRequest(i, wa));
      }
    }

    // Fast path: if no words are ambiguous, directly build sentence analysis without calling model
    if (ambiguousWords.isEmpty()) {
      List<SentenceWordAnalysis> wordAnalyses = new ArrayList<>(allAnalyses.size());
      for (WordAnalysis wa : allAnalyses) {
        SingleAnalysis sa = wa.analysisCount() > 0 ? wa.getAnalysisResults().get(0) : SingleAnalysis.unknown(wa.getInput());
        wordAnalyses.add(new SentenceWordAnalysis(sa, wa));
      }
      return new SentenceAnalysis(sentence, wordAnalyses);
    }

    // Step 2: Query the predictor for ambiguous tokens
    DisambiguationRequest request = new DisambiguationRequest(sentence, ambiguousWords);
    Map<Integer, Integer> predictions = Collections.emptyMap();
    try {
      predictions = predictor.predict(request);
      if (predictions == null) {
        predictions = Collections.emptyMap();
      }
    } catch (Exception e) {
      if (!fallbackOnFailure) {
        throw new RuntimeException("BERT disambiguation prediction failed for sentence: " + sentence, e);
      }
      Log.warn("BERT predictor failed for sentence '%s'. Falling back to first candidate: %s",
          sentence, e.getMessage());
    }

    // Step 3: Construct resolved SentenceAnalysis
    List<SentenceWordAnalysis> resultWords = new ArrayList<>(allAnalyses.size());
    for (int i = 0; i < allAnalyses.size(); i++) {
      WordAnalysis wa = allAnalyses.get(i);
      List<SingleAnalysis> analyses = wa.getAnalysisResults();
      if (analyses.isEmpty()) {
        resultWords.add(new SentenceWordAnalysis(SingleAnalysis.unknown(wa.getInput()), wa));
      } else if (analyses.size() == 1) {
        resultWords.add(new SentenceWordAnalysis(analyses.get(0), wa));
      } else {
        int selectedCandidateId = predictions.getOrDefault(i, 0);
        if (selectedCandidateId < 0 || selectedCandidateId >= analyses.size()) {
          Log.warn("Predicted candidate ID %d out of bounds for token '%s' (max %d). Falling back to 0.",
              selectedCandidateId, wa.getInput(), analyses.size() - 1);
          selectedCandidateId = 0;
        }
        resultWords.add(new SentenceWordAnalysis(analyses.get(selectedCandidateId), wa));
      }
    }

    return new SentenceAnalysis(sentence, resultWords);
  }

  private AmbiguousWordRequest toWordRequest(int index, WordAnalysis wa) {
    List<SingleAnalysis> analyses = wa.getAnalysisResults();
    List<CandidateAnalysis> candidates = new ArrayList<>(analyses.size());
    for (int cId = 0; cId < analyses.size(); cId++) {
      SingleAnalysis sa = analyses.get(cId);
      List<String> morphemes = new ArrayList<>();
      for (Morpheme m : sa.getMorphemes()) {
        morphemes.add(m.id);
      }
      DictionaryItem item = sa.getDictionaryItem();
      String pos = item.primaryPos != null ? item.primaryPos.shortForm : PrimaryPos.Unknown.shortForm;
      String secPos = (item.secondaryPos != null && item.secondaryPos != SecondaryPos.None)
          ? item.secondaryPos.shortForm
          : "None";
      candidates.add(new CandidateAnalysis(
          cId,
          sa.formatLong(),
          item.lemma,
          pos,
          secPos,
          AnalysisFormatters.OFLAZER_STYLE.format(sa),
          morphemes,
          sa.containsInformalMorpheme()
      ));
    }
    return new AmbiguousWordRequest(index, wa.getInput(), candidates);
  }

  // --- Predictor Interface and Implementations ---

  @FunctionalInterface
  public interface Predictor {
    /**
     * Given a disambiguation request with ambiguous tokens and their candidates,
     * returns a map of tokenIndex -> selected candidate ID.
     */
    Map<Integer, Integer> predict(DisambiguationRequest request) throws Exception;
  }

  /**
   * HTTP REST client predictor for remote or locally served transformer models.
   */
  public static class HttpPredictor implements Predictor {

    private final URI endpoint;
    private final Duration timeout;
    private final Gson gson;

    public HttpPredictor(URI endpoint, Duration timeout) {
      this.endpoint = Objects.requireNonNull(endpoint, "endpoint must not be null");
      this.timeout = Objects.requireNonNull(timeout, "timeout must not be null");
      this.gson = new Gson();
    }

    @Override
    public Map<Integer, Integer> predict(DisambiguationRequest request) throws Exception {
      String jsonBody = gson.toJson(request);

      java.net.URL url = endpoint.toURL();
      java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
      conn.setRequestMethod("POST");
      conn.setConnectTimeout((int) timeout.toMillis());
      conn.setReadTimeout((int) timeout.toMillis());
      conn.setDoOutput(true);
      conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
      conn.setRequestProperty("Accept", "application/json");

      byte[] postData = jsonBody.getBytes(StandardCharsets.UTF_8);
      conn.setFixedLengthStreamingMode(postData.length);
      try (OutputStream os = conn.getOutputStream()) {
        os.write(postData);
      }

      int statusCode = conn.getResponseCode();
      if (statusCode < 200 || statusCode >= 300) {
        throw new IOException("HTTP " + statusCode + " from disambiguation server at " + endpoint);
      }

      String responseBody;
      try (InputStream is = conn.getInputStream();
           BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
          sb.append(line);
        }
        responseBody = sb.toString();
      }

      DisambiguationResponse resp = gson.fromJson(responseBody, DisambiguationResponse.class);
      if (resp == null || resp.predictions == null) {
        return Collections.emptyMap();
      }

      Map<Integer, Integer> map = new HashMap<>();
      for (PredictionItem item : resp.predictions) {
        map.put(item.index, item.selectedCandidateId);
      }
      return map;
    }

    public URI getEndpoint() {
      return endpoint;
    }
  }

  /**
   * Subprocess predictor that runs an interactive Python script communicating over stdin/stdout.
   */
  public static class SubprocessPredictor implements Predictor, AutoCloseable {

    private final Process process;
    private final BufferedReader reader;
    private final BufferedWriter writer;
    private final Gson gson = new Gson();

    public SubprocessPredictor(List<String> command) throws IOException {
      ProcessBuilder pb = new ProcessBuilder(command);
      pb.redirectError(ProcessBuilder.Redirect.INHERIT);
      this.process = pb.start();
      this.reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
      this.writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));
    }

    @Override
    public synchronized Map<Integer, Integer> predict(DisambiguationRequest request) throws Exception {
      String line = gson.toJson(request);
      writer.write(line);
      writer.newLine();
      writer.flush();

      String responseLine = reader.readLine();
      if (responseLine == null) {
        throw new IOException("Subprocess terminated unexpectedly.");
      }

      DisambiguationResponse resp = gson.fromJson(responseLine, DisambiguationResponse.class);
      if (resp == null || resp.predictions == null) {
        return Collections.emptyMap();
      }
      Map<Integer, Integer> map = new HashMap<>();
      for (PredictionItem item : resp.predictions) {
        map.put(item.index, item.selectedCandidateId);
      }
      return map;
    }

    @Override
    public void close() {
      try {
        writer.close();
      } catch (Exception ignored) {
      }
      try {
        reader.close();
      } catch (Exception ignored) {
      }
      process.destroy();
    }
  }

  // --- Data Transfer Objects ---

  public static class DisambiguationRequest {
    public String sentence;
    public List<AmbiguousWordRequest> words;

    public DisambiguationRequest() {
    }

    public DisambiguationRequest(String sentence, List<AmbiguousWordRequest> words) {
      this.sentence = sentence;
      this.words = words;
    }
  }

  public static class AmbiguousWordRequest {
    public int index;
    public String surface;
    public List<CandidateAnalysis> candidates;

    public AmbiguousWordRequest() {
    }

    public AmbiguousWordRequest(int index, String surface, List<CandidateAnalysis> candidates) {
      this.index = index;
      this.surface = surface;
      this.candidates = candidates;
    }
  }

  public static class CandidateAnalysis {
    public int id;
    public String analysis;
    public String lemma;
    public String pos;
    public String secondary_pos;
    public String oflazer_style;
    public List<String> morphemes;
    public boolean is_informal;

    public CandidateAnalysis() {
    }

    public CandidateAnalysis(
        int id,
        String analysis,
        String lemma,
        String pos,
        String secondary_pos,
        String oflazer_style,
        List<String> morphemes,
        boolean is_informal) {
      this.id = id;
      this.analysis = analysis;
      this.lemma = lemma;
      this.pos = pos;
      this.secondary_pos = secondary_pos;
      this.oflazer_style = oflazer_style;
      this.morphemes = morphemes;
      this.is_informal = is_informal;
    }
  }

  public static class DisambiguationResponse {
    public List<PredictionItem> predictions;

    public DisambiguationResponse() {
    }

    public DisambiguationResponse(List<PredictionItem> predictions) {
      this.predictions = predictions;
    }
  }

  public static class PredictionItem {
    public int index;
    @SerializedName("selected_candidate_id")
    public int selectedCandidateId;

    public PredictionItem() {
    }

    public PredictionItem(int index, int selectedCandidateId) {
      this.index = index;
      this.selectedCandidateId = selectedCandidateId;
    }
  }

  // --- Builder ---

  public static class Builder {
    private URI endpoint;
    private Duration timeout = Duration.ofSeconds(3);
    private Predictor predictor;
    private boolean fallbackOnFailure = true;

    public Builder endpoint(String url) {
      this.endpoint = URI.create(url);
      return this;
    }

    public Builder endpoint(URI endpoint) {
      this.endpoint = endpoint;
      return this;
    }

    public Builder timeout(Duration timeout) {
      this.timeout = timeout;
      return this;
    }

    public Builder predictor(Predictor predictor) {
      this.predictor = predictor;
      return this;
    }

    public Builder fallbackOnFailure(boolean fallbackOnFailure) {
      this.fallbackOnFailure = fallbackOnFailure;
      return this;
    }

    public BertAmbiguityResolver build() {
      if (predictor != null) {
        return new BertAmbiguityResolver(predictor, fallbackOnFailure);
      }
      if (endpoint == null) {
        throw new IllegalArgumentException("Either endpoint or predictor must be provided");
      }
      Predictor httpPredictor = new HttpPredictor(endpoint, timeout);
      return new BertAmbiguityResolver(httpPredictor, fallbackOnFailure);
    }
  }
}
