package zemberek.morphology.ambiguity.dataset;

import com.google.gson.Gson;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import zemberek.morphology.ambiguity.dataset.DisambiguationCandidateExtractor.CandidateRecord;
import zemberek.morphology.ambiguity.dataset.DisambiguationCandidateExtractor.Config;
import zemberek.morphology.ambiguity.dataset.DisambiguationCandidateExtractor.ExtractionMetadata;
import zemberek.morphology.ambiguity.dataset.DisambiguationCandidateExtractor.ExtractionResult;
import zemberek.morphology.ambiguity.dataset.DisambiguationCandidateExtractor.SentenceRecord;
import zemberek.morphology.ambiguity.dataset.DisambiguationCandidateExtractor.TokenRecord;

public class DisambiguationCandidateExtractorTest {

  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();

  @Test
  public void testExtractionOnAmbiguousSentences() throws IOException {
    Path inputFile = tempFolder.newFile("sample_input.txt").toPath();
    Path outputFile = tempFolder.getRoot().toPath().resolve("candidates.jsonl");

    String sampleText =
        "Prof. Dr. Ahmet Bey açıklama yaptı.\n"
            + "Yarın kar yağacak. Bu koyun çok sevimli.\n";
    Files.write(inputFile, sampleText.getBytes(StandardCharsets.UTF_8));

    Config config = new Config.Builder()
        .inputPath(inputFile)
        .outputPath(outputFile)
        .includeAllSentences(true)
        .build();

    ExtractionResult result = DisambiguationCandidateExtractor.extract(config);

    Assert.assertTrue("Output JSONL file must exist", Files.exists(result.jsonlPath));
    Assert.assertTrue("Output meta.json file must exist", Files.exists(result.metaPath));

    Gson gson = new Gson();
    List<String> lines = Files.readAllLines(result.jsonlPath, StandardCharsets.UTF_8);
    Assert.assertTrue("At least one sentence should be extracted", lines.size() >= 2);

    boolean foundKar = false;
    boolean foundKoyun = false;

    for (String line : lines) {
      SentenceRecord record = gson.fromJson(line, SentenceRecord.class);
      Assert.assertNotNull(record.sentence_id);
      Assert.assertNotNull(record.text);
      Assert.assertFalse(record.tokens.isEmpty());

      for (TokenRecord token : record.tokens) {
        Assert.assertNotNull(token.surface);
        Assert.assertTrue(token.char_start >= 0);
        Assert.assertTrue(token.char_end >= token.char_start);

        if ("kar".equalsIgnoreCase(token.surface)) {
          foundKar = true;
          Assert.assertTrue("Word 'kar' must be ambiguous", token.is_ambiguous);
          Assert.assertTrue("Word 'kar' must have >= 2 candidates", token.candidates.size() >= 2);
          for (CandidateRecord c : token.candidates) {
            Assert.assertNotNull(c.zemberek_key);
            Assert.assertNotNull(c.lemma);
            Assert.assertNotNull(c.pos);
            Assert.assertNotNull(c.oflazer_style);
            Assert.assertFalse(c.morphemes.isEmpty());
          }
        }

        if ("koyun".equalsIgnoreCase(token.surface)) {
          foundKoyun = true;
          Assert.assertTrue("Word 'koyun' must be ambiguous", token.is_ambiguous);
          Assert.assertTrue("Word 'koyun' must have >= 2 candidates", token.candidates.size() >= 2);
        }
      }
    }

    Assert.assertTrue("Expected ambiguous token 'kar' in output", foundKar);
    Assert.assertTrue("Expected ambiguous token 'koyun' in output", foundKoyun);

    // Verify metadata
    try (BufferedReader metaReader = Files.newBufferedReader(result.metaPath, StandardCharsets.UTF_8)) {
      ExtractionMetadata meta = gson.fromJson(metaReader, ExtractionMetadata.class);
      Assert.assertEquals("1.0", meta.schema_version);
      Assert.assertEquals("sample_input.txt", meta.provenance.source_file);
      Assert.assertEquals("zemberek-nlp", meta.provenance.parser);
      Assert.assertTrue(meta.statistics.sentence_count > 0);
      Assert.assertTrue(meta.statistics.token_count > 0);
      Assert.assertTrue(meta.statistics.ambiguous_token_count > 0);
      Assert.assertTrue(meta.statistics.ambiguity_rate > 0.0);
    }
  }

  @Test
  public void testMaxSentencesLimit() throws IOException {
    Path inputFile = tempFolder.newFile("sample_multi.txt").toPath();
    Path outputFile = tempFolder.getRoot().toPath().resolve("candidates_limited.jsonl");

    String sampleText = "Birinci cümle burada. İkinci cümle burada. Üçüncü cümle burada.";
    Files.write(inputFile, sampleText.getBytes(StandardCharsets.UTF_8));

    Config config = new Config.Builder()
        .inputPath(inputFile)
        .outputPath(outputFile)
        .includeAllSentences(true)
        .maxSentences(1)
        .build();

    ExtractionResult result = DisambiguationCandidateExtractor.extract(config);
    List<String> lines = Files.readAllLines(result.jsonlPath, StandardCharsets.UTF_8);
    Assert.assertEquals("Should have strictly 1 sentence when maxSentences=1", 1, lines.size());
    Assert.assertEquals(1, result.metadata.statistics.sentence_count);
  }
}

