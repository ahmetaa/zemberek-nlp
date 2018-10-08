package zemberek.morphology.generator;

import com.google.common.collect.Lists;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import zemberek.morphology.analysis.AnalyzerTestBase;
import zemberek.morphology.generator.WordGenerator.Result;
import zemberek.morphology.lexicon.DictionaryItem;
import zemberek.morphology.morphotactics.TurkishMorphotactics;

public class WordGeneratorTest extends AnalyzerTestBase {

  @Test
  public void testGeneration1() {
    WordGenerator wordGenerator = new WordGenerator(getMorphotactics("elma"));
    List<String> morphemes = Lists.newArrayList("A3pl", "P1pl");
    List<Result> results = wordGenerator.generate(
        "elma",
        morphemes
    );
    Assert.assertTrue(results.size() > 0);
    Assert.assertEquals("elmalarımız", results.get(0).surface);
  }

  @Test
  public void testGeneration2() {
    WordGenerator wordGenerator = new WordGenerator(getMorphotactics("elma"));
    List<String> morphemes = Lists.newArrayList("Noun", "A3pl", "P1pl");
    List<Result> results = wordGenerator.generate(
        "elma",
        morphemes
    );
    Assert.assertTrue(results.size() > 0);
    Assert.assertEquals("elmalarımız", results.get(0).surface);
  }

  @Test
  public void testGeneration3() {
    WordGenerator wordGenerator = new WordGenerator(getMorphotactics("elma"));
    List<String> morphemes = Lists.newArrayList("Noun", "With");
    List<Result> results = wordGenerator.generate(
        "elma",
        morphemes
    );
    Assert.assertTrue(results.size() > 0);
    Assert.assertEquals("elmalı", results.get(0).surface);
  }

  @Test
  public void testGeneration4() {
    TurkishMorphotactics mo = getMorphotactics("elma");
    WordGenerator wordGenerator = new WordGenerator(mo);
    List<String> morphemes = Lists.newArrayList("Noun", "A3pl", "P1pl");
    List<Result> results = wordGenerator.generate(
        mo.getRootLexicon().getItemById("elma_Noun"),
        TurkishMorphotactics.getMorphemes(morphemes)
    );
    Assert.assertTrue(results.size() > 0);
    Assert.assertEquals("elmalarımız", results.get(0).surface);
  }

  @Test
  public void testGeneration5() {
    TurkishMorphotactics mo = getMorphotactics("yapmak");
    WordGenerator wordGenerator = new WordGenerator(mo);
    List<String> morphemes = Lists.newArrayList("Verb", "Opt", "A1pl");
    DictionaryItem item = mo.getRootLexicon().getItemById("yapmak_Verb");
    List<Result> results = wordGenerator.generate(
        item,
        TurkishMorphotactics.getMorphemes(morphemes)
    );
    Assert.assertTrue(results.size() > 0);
    Assert.assertEquals("yapalım", results.get(0).surface);
  }


}
