package zemberek.morphology.ambiguity.bert;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Assert;
import org.junit.Test;
import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.ambiguity.bert.BertAmbiguityResolver.DisambiguationRequest;
import zemberek.morphology.ambiguity.bert.BertAmbiguityResolver.DisambiguationResponse;
import zemberek.morphology.ambiguity.bert.BertAmbiguityResolver.PredictionItem;
import zemberek.morphology.ambiguity.bert.BertAmbiguityResolver.Predictor;
import zemberek.morphology.analysis.SentenceAnalysis;
import zemberek.morphology.lexicon.RootLexicon;

public class BertAmbiguityResolverTest {

  @Test
  public void testDirectDisambiguationWithPredictor() {
    // Mock predictor selecting candidate 1 (Pron,Quant) for Kimse and candidate 1 (Adj) for yok
    Predictor mockPredictor = request -> {
      Map<Integer, Integer> predictions = new HashMap<>();
      for (BertAmbiguityResolver.AmbiguousWordRequest w : request.words) {
        if ("Kimse".equals(w.surface)) {
          predictions.put(w.index, 1); // Pron,Quant
        } else if ("yok".equals(w.surface)) {
          predictions.put(w.index, 1); // Adj
        }
      }
      return predictions;
    };

    BertAmbiguityResolver resolver = BertAmbiguityResolver.fromPredictor(mockPredictor);

    TurkishMorphology morphology = TurkishMorphology.builder()
        .setLexicon(RootLexicon.getDefault())
        .setAmbiguityResolver(resolver)
        .build();

    SentenceAnalysis sentenceAnalysis = morphology.analyzeAndDisambiguate("Kimse yok.");
    Assert.assertEquals(3, sentenceAnalysis.size());

    // Token 0: Kimse -> Pron,Quant
    Assert.assertEquals("Kimse", sentenceAnalysis.getWordAnalyses().get(0).getWordAnalysis().getInput());
    Assert.assertEquals("Pron", sentenceAnalysis.bestAnalysis().get(0).getDictionaryItem().primaryPos.shortForm);
    Assert.assertEquals("Quant", sentenceAnalysis.bestAnalysis().get(0).getDictionaryItem().secondaryPos.shortForm);

    // Token 1: yok -> Adj
    Assert.assertEquals("yok", sentenceAnalysis.getWordAnalyses().get(1).getWordAnalysis().getInput());
    Assert.assertEquals("Adj", sentenceAnalysis.bestAnalysis().get(1).getDictionaryItem().primaryPos.shortForm);
  }

  @Test
  public void testFastPathUnambiguousSentences() {
    AtomicInteger callCount = new AtomicInteger(0);
    Predictor countingPredictor = request -> {
      callCount.incrementAndGet();
      return Collections.emptyMap();
    };

    BertAmbiguityResolver resolver = BertAmbiguityResolver.fromPredictor(countingPredictor);

    TurkishMorphology morphology = TurkishMorphology.builder()
        .setLexicon(RootLexicon.getDefault())
        .setAmbiguityResolver(resolver)
        .build();

    // "Eve geldim." has no ambiguous tokens
    SentenceAnalysis sentenceAnalysis = morphology.analyzeAndDisambiguate("Eve geldim.");
    Assert.assertEquals(3, sentenceAnalysis.size());

    // Verify the predictor was never invoked
    Assert.assertEquals(0, callCount.get());
  }

  @Test
  public void testFallbackOnPredictorFailure() {
    Predictor failingPredictor = request -> {
      throw new RuntimeException("Simulated connection timeout");
    };

    // Case 1: fallbackOnFailure = true (default)
    BertAmbiguityResolver fallbackResolver = BertAmbiguityResolver.builder()
        .predictor(failingPredictor)
        .fallbackOnFailure(true)
        .build();

    TurkishMorphology morphologyWithFallback = TurkishMorphology.builder()
        .setLexicon(RootLexicon.getDefault())
        .setAmbiguityResolver(fallbackResolver)
        .build();

    SentenceAnalysis analysis = morphologyWithFallback.analyzeAndDisambiguate("Kimse yok.");
    Assert.assertEquals(3, analysis.size());
    // Falls back to index 0
    Assert.assertEquals("kimse", analysis.bestAnalysis().get(0).getDictionaryItem().lemma);

    // Case 2: fallbackOnFailure = false
    BertAmbiguityResolver strictResolver = BertAmbiguityResolver.builder()
        .predictor(failingPredictor)
        .fallbackOnFailure(false)
        .build();

    TurkishMorphology strictMorphology = TurkishMorphology.builder()
        .setLexicon(RootLexicon.getDefault())
        .setAmbiguityResolver(strictResolver)
        .build();

    try {
      strictMorphology.analyzeAndDisambiguate("Kimse yok.");
      Assert.fail("Expected RuntimeException when fallbackOnFailure is false");
    } catch (RuntimeException expected) {
      Assert.assertTrue(expected.getMessage().contains("BERT disambiguation prediction failed"));
    }
  }

  @Test
  public void testHttpPredictorWithLocalServer() throws IOException {
    Gson gson = new Gson();

    // Start ephemeral HTTP server
    HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext("/disambiguate", new HttpHandler() {
      @Override
      public void handle(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
          exchange.sendResponseHeaders(405, -1);
          return;
        }

        InputStream is = exchange.getRequestBody();
        String requestBody = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        DisambiguationRequest req = gson.fromJson(requestBody, DisambiguationRequest.class);

        List<PredictionItem> predictions = new ArrayList<>();
        if (req != null && req.words != null) {
          for (BertAmbiguityResolver.AmbiguousWordRequest word : req.words) {
            // Predict candidate 1 for all ambiguous words
            predictions.add(new PredictionItem(word.index, 1));
          }
        }

        String jsonResponse = gson.toJson(new DisambiguationResponse(predictions));
        byte[] bytes = jsonResponse.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
          os.write(bytes);
        }
      }
    });

    server.start();
    int port = server.getAddress().getPort();

    try {
      BertAmbiguityResolver httpResolver = BertAmbiguityResolver.builder()
          .endpoint("http://127.0.0.1:" + port + "/disambiguate")
          .timeout(Duration.ofSeconds(2))
          .build();

      TurkishMorphology morphology = TurkishMorphology.builder()
          .setLexicon(RootLexicon.getDefault())
          .setAmbiguityResolver(httpResolver)
          .build();

      SentenceAnalysis result = morphology.analyzeAndDisambiguate("Kimse yok.");
      Assert.assertEquals(3, result.size());

      // Candidate 1 was picked over HTTP
      Assert.assertEquals("Pron", result.bestAnalysis().get(0).getDictionaryItem().primaryPos.shortForm);
      Assert.assertEquals("Adj", result.bestAnalysis().get(1).getDictionaryItem().primaryPos.shortForm);
    } finally {
      server.stop(0);
    }
  }

  @Test
  public void testSubprocessPredictorWithPythonScript() throws Exception {
    List<String> cmd = java.util.Arrays.asList(
        "python3", "experiment/scripts/serve_bert_disambiguator.py", "--pipe", "--mock");

    try (BertAmbiguityResolver.SubprocessPredictor subprocessPredictor =
             new BertAmbiguityResolver.SubprocessPredictor(cmd)) {

      BertAmbiguityResolver resolver = BertAmbiguityResolver.fromPredictor(subprocessPredictor);

      TurkishMorphology morphology = TurkishMorphology.builder()
          .setLexicon(RootLexicon.getDefault())
          .setAmbiguityResolver(resolver)
          .build();

      SentenceAnalysis result = morphology.analyzeAndDisambiguate("Kimse yok.");
      Assert.assertEquals(3, result.size());

      // Candidate 1 (Pron,Quant) and candidate 1 (Adj) should be chosen by mock python script
      Assert.assertEquals("Pron", result.bestAnalysis().get(0).getDictionaryItem().primaryPos.shortForm);
      Assert.assertEquals("Adj", result.bestAnalysis().get(1).getDictionaryItem().primaryPos.shortForm);
    }
  }
}
