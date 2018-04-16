package zemberek.morphology.generator;

import com.google.common.collect.Lists;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import zemberek.morphology.analysis.AnalyzerTestBase;
import zemberek.morphology.generator.Generator;
import zemberek.morphology.generator.Generator.GenerationResult;

public class GeneratorTest extends AnalyzerTestBase {

  @Test
  public void testGeneration1() {
    Generator generator = new Generator(getMorphotactics("elma"));
    List<String> morphemes = Lists.newArrayList("A3pl", "P1pl");
    List<GenerationResult> results = generator.generateWithIds(
        "elma",
        morphemes
    );
    Assert.assertTrue(results.size() > 0);
    Assert.assertEquals("elmalarımız", results.get(0).surface);
  }

  @Test
  public void testGeneration2() {
    Generator generator = new Generator(getMorphotactics("elma"));
    List<String> morphemes = Lists.newArrayList("Noun", "A3pl", "P1pl");
    List<GenerationResult> results = generator.generateWithIds(
        "elma",
        morphemes
    );
    Assert.assertTrue(results.size() > 0);
    Assert.assertEquals("elmalarımız", results.get(0).surface);
  }

  @Test
  public void testGeneration3() {
    Generator generator = new Generator(getMorphotactics("elma"));
    List<String> morphemes = Lists.newArrayList("Noun", "With");
    List<GenerationResult> results = generator.generateWithIds(
        "elma",
        morphemes
    );
    Assert.assertTrue(results.size() > 0);
    Assert.assertEquals("elmalı", results.get(0).surface);
  }


}
