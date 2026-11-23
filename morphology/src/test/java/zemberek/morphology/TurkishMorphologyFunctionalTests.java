package zemberek.morphology;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Assert;
import org.junit.Test;
import zemberek.core.logging.Log;
import zemberek.core.turkish.SecondaryPos;
import zemberek.morphology.analysis.AnalysisCache;
import zemberek.morphology.analysis.RuleBasedAnalyzer;
import zemberek.morphology.analysis.SingleAnalysis;
import zemberek.morphology.analysis.WordAnalysis;
import zemberek.morphology.lexicon.RootLexicon;
import zemberek.morphology.lexicon.tr.TurkishDictionaryLoader;
import zemberek.tokenization.Token;

public class TurkishMorphologyFunctionalTests {

  private TurkishMorphology getEmptyTurkishMorphology() {
    return TurkishMorphology
        .builder()
        .disableCache()
        .build();
  }

  private TurkishMorphology getMorphology(String... lines) {
    return TurkishMorphology
        .builder()
        .setLexicon(lines)
        .disableCache()
        .build();
  }

  private TurkishMorphology getAsciiTolerantMorphology(String... lines) {
    RootLexicon lexicon = TurkishDictionaryLoader.load(lines);
    return TurkishMorphology
        .builder()
        .setLexicon(lines)
        .setLexicon(lexicon)
        .ignoreDiacriticsInAnalysis()
        .disableCache()
        .build();
  }

  @Test
  public void testWordsWithCircumflex() {
    TurkishMorphology morphology = getMorphology("zekâ");
    WordAnalysis result = morphology.analyze("zekâ");
    Assert.assertEquals(1, result.analysisCount());
  }

  @Test
  public void testPossibleProper2() {
    TurkishMorphology morphology = getMorphology("Air");
    Assert.assertEquals(0, morphology.analyze("Air'rrr").analysisCount());
    Assert.assertEquals(1, morphology.analyze("Air").analysisCount());
  }

  @Test
  public void testWordsWithDot() {
    TurkishMorphology morphology = getMorphology("Dr [P:Abbrv]");
    WordAnalysis result = morphology.analyze("Dr.");
    Assert.assertEquals(1, result.analysisCount());
  }

  @Test
  public void testRomanNumeral() {
    TurkishMorphology morphology = getMorphology("dört [P:Num,Card;A:Voicing]");
    WordAnalysis result = morphology.analyze("IV");
    Assert.assertEquals(1, result.analysisCount());
    Assert.assertEquals(
        SecondaryPos.RomanNumeral,
        result.getAnalysisResults().get(0).getDictionaryItem().secondaryPos);
  }

  @Test
  public void testRomanNumeral2() {
    // Instance with no dictionary item.
    TurkishMorphology morphology = getMorphology("dördüncü [P:Num,Ord]");
    WordAnalysis result = morphology.analyze("XXIV.");
    Assert.assertEquals(1, result.analysisCount());
    Assert.assertEquals(
        SecondaryPos.RomanNumeral,
        result.getAnalysisResults().get(0).getDictionaryItem().secondaryPos);
  }

  @Test
  public void testRomanNumeral3() {
    TurkishMorphology morphology = getMorphology("dört [P:Num,Card;A:Voicing]");
    WordAnalysis result = morphology.analyze("XXIV'ten");
    Assert.assertEquals(1, result.analysisCount());
    Assert.assertEquals(
        SecondaryPos.RomanNumeral,
        result.getAnalysisResults().get(0).getDictionaryItem().secondaryPos);
  }

  @Test
  public void testDate() {
    TurkishMorphology morphology = getMorphology("dört [P:Num,Card;A:Voicing]");
    WordAnalysis result = morphology.analyze("1.1.2014");
    Assert.assertEquals(1, result.analysisCount());
    Assert.assertEquals(
        SecondaryPos.Date,
        result.getAnalysisResults().get(0).getDictionaryItem().secondaryPos);
  }

  @Test
  public void testDate2() {
    TurkishMorphology morphology = getMorphology("dört [P:Num,Card;A:Voicing]");
    WordAnalysis result = morphology.analyze("1.1.2014'te");
    Assert.assertEquals(1, result.analysisCount());
    SingleAnalysis analysis = result.getAnalysisResults().get(0);
    Assert.assertEquals(
        SecondaryPos.Date,
        analysis.getDictionaryItem().secondaryPos);
    String lexical = analysis.formatLexical();
    Assert.assertTrue(lexical.endsWith("A3sg+Loc"));
  }

  @Test
  public void testUrl() {
    TurkishMorphology morphology = getMorphology();
    WordAnalysis result = morphology.analyze("www.foo.com");
    Assert.assertEquals(1, result.analysisCount());
    Assert.assertEquals(
        SecondaryPos.Url,
        result.getAnalysisResults().get(0).getDictionaryItem().secondaryPos);
    String lexical = result.getAnalysisResults().get(0).formatLexical();
    Assert.assertTrue(lexical.endsWith("A3sg"));
  }

  @Test
  public void testUrl2() {
    TurkishMorphology morphology = getMorphology();
    WordAnalysis result = morphology.analyze("www.foo.com'da");
    Assert.assertEquals(1, result.analysisCount());
    SingleAnalysis analysis = result.getAnalysisResults().get(0);
    Assert.assertEquals(
        SecondaryPos.Url,
        analysis.getDictionaryItem().secondaryPos);
    String lexical = analysis.formatLexical();
    Assert.assertTrue(lexical.endsWith("A3sg+Loc"));
    Assert.assertEquals("www.foo.com", analysis.getDictionaryItem().lemma);
  }

  @Test
  public void testHashTag() {
    TurkishMorphology morphology = getMorphology();
    WordAnalysis result = morphology.analyze("#haha_ha'ya");
    Assert.assertEquals(1, result.analysisCount());
    SingleAnalysis analysis = result.getAnalysisResults().get(0);
    Assert.assertEquals(
        SecondaryPos.HashTag,
        analysis.getDictionaryItem().secondaryPos);
    String lexical = analysis.formatLexical();
    Assert.assertTrue(lexical.endsWith("A3sg+Dat"));
    Assert.assertEquals("#haha_ha", analysis.getDictionaryItem().lemma);
  }

  @Test
  public void testHashTag2() {
    TurkishMorphology morphology = getMorphology();
    WordAnalysis result = morphology.analyze("#123'efefe");
    Assert.assertEquals(1, result.analysisCount());
    SingleAnalysis analysis = result.getAnalysisResults().get(0);
    Assert.assertEquals(
        SecondaryPos.HashTag,
        analysis.getDictionaryItem().secondaryPos);
    Assert.assertEquals(
        "#123'efefe",
        analysis.getDictionaryItem().lemma);
  }

  @Test
  public void testMention() {
    TurkishMorphology morphology = getMorphology();
    WordAnalysis result = morphology.analyze("@haha_ha'ya");
    Assert.assertEquals(1, result.analysisCount());
    SingleAnalysis analysis = result.getAnalysisResults().get(0);
    Assert.assertEquals(
        SecondaryPos.Mention,
        analysis.getDictionaryItem().secondaryPos);
    String lexical = analysis.formatLexical();
    Assert.assertTrue(lexical.endsWith("A3sg+Dat"));
    Assert.assertEquals("@haha_ha", analysis.getDictionaryItem().lemma);
    Assert.assertTrue(lexical.contains("@haha_ha"));
  }

  @Test
  public void testEmail() {
    TurkishMorphology morphology = getMorphology();
    WordAnalysis result = morphology.analyze("foo@bar.com'a");
    Assert.assertEquals(1, result.analysisCount());
    SingleAnalysis analysis = result.getAnalysisResults().get(0);
    Assert.assertEquals(
        SecondaryPos.Email,
        analysis.getDictionaryItem().secondaryPos);
    String lexical = analysis.formatLexical();
    Assert.assertTrue(lexical.endsWith("A3sg+Dat"));
    Assert.assertEquals("foo@bar.com", analysis.getDictionaryItem().lemma);
  }

  @Test
  public void testTime() {
    TurkishMorphology morphology = getMorphology("otuz [P:Num,Card]");
    WordAnalysis result = morphology.analyze("20:30'da");
    Assert.assertEquals(1, result.analysisCount());
    Assert.assertEquals(
        SecondaryPos.Clock,
        result.getAnalysisResults().get(0).getDictionaryItem().secondaryPos);
  }


  @Test
  public void testTime2() {
    TurkishMorphology morphology = getMorphology("dört [P:Num,Card;A:Voicing]");
    WordAnalysis result = morphology.analyze("10:24'te");
    Assert.assertEquals(1, result.analysisCount());
    SingleAnalysis analysis = result.getAnalysisResults().get(0);
    Assert.assertEquals(
        SecondaryPos.Clock,
        analysis.getDictionaryItem().secondaryPos);
    String lexical = analysis.formatLexical();
    Assert.assertTrue(lexical.endsWith("A3sg+Loc"));
  }

  @Test
  public void testRatio() {
    TurkishMorphology morphology = getMorphology("iki [P:Num,Card]");
    WordAnalysis result = morphology.analyze("1/2");
    Assert.assertEquals(1, result.analysisCount());
    Assert.assertEquals(
        SecondaryPos.Ratio,
        result.getAnalysisResults().get(0).getDictionaryItem().secondaryPos);
  }

  @Test
  public void testPercent() {
    TurkishMorphology morphology = getMorphology("iki [P:Num,Card]");
    String[] correct = {"%2", "%2'si", "%2.2'si", "%2,2'si"};
    for (String s : correct) {
      WordAnalysis result = morphology.analyze(s);
      Assert.assertEquals("Failed for " + s, 1, result.analysisCount());
      Assert.assertEquals("Failed for " + s,
          SecondaryPos.Percentage,
          result.getAnalysisResults().get(0).getDictionaryItem().secondaryPos);
    }
  }


  @Test
  public void testEmoticon() {
    TurkishMorphology morphology = getEmptyTurkishMorphology();
    WordAnalysis result = morphology.analyze(":)");
    Assert.assertEquals(1, result.analysisCount());
    Assert.assertEquals(
        SecondaryPos.Emoticon,
        result.getAnalysisResults().get(0).getDictionaryItem().secondaryPos);
  }

  @Test
  public void testEmoji() {
    TurkishMorphology morphology = getMorphology("güzel [P:Adj]");
    for (String emoji : new String[]{"🥰", "❤", "🇹🇷", "👨‍👩‍👧‍👦"}) {
      WordAnalysis result = morphology.analyze(emoji);
      Assert.assertEquals(1, result.analysisCount());
      Assert.assertEquals(
          SecondaryPos.Emoticon,
          result.getAnalysisResults().get(0).getDictionaryItem().secondaryPos);
    }
    var sentence = morphology.analyzeAndDisambiguate("güzel 🥰");
    Assert.assertEquals(2, sentence.size());
    Assert.assertEquals(
        SecondaryPos.Emoticon,
        sentence.bestAnalysis().get(1).getDictionaryItem().secondaryPos);
  }

  @Test
  public void testForeingLocale() {
    TurkishMorphology morphology = getMorphology("UNICEF [A:LocaleEn]");

    WordAnalysis result = morphology.analyze("Unicefte");
    Assert.assertEquals(1, result.analysisCount());

    morphology = getMorphology("UNICEF");
    result = morphology.analyze("Unicefte");
    Assert.assertEquals(0, result.analysisCount());
  }

  @Test
  public void testWordsWithDash() {
    // Instance with no dictionary item.
    TurkishMorphology morphology = getEmptyTurkishMorphology();
    WordAnalysis result = morphology.analyze("Blah-Foo'ya");
    Assert.assertEquals(1, result.analysisCount());
    Assert.assertEquals("Blah-foo", result.getAnalysisResults().get(0).getDictionaryItem().lemma);
  }

  @Test
  public void testSingleQuotePunctuation() {
    TurkishMorphology morphology = getMorphology("' [P:Punc]");
    WordAnalysis result = morphology.analyze("'");
    Assert.assertEquals(1, result.analysisCount());
    Assert.assertEquals("'", result.getAnalysisResults().get(0).getDictionaryItem().lemma);
  }

  @Test
  public void testAnalyzeSentenceWithDisabledCache() {
    TurkishMorphology morphology = getMorphology("elma", "' [P:Punc]");
    List<WordAnalysis> analyses = morphology.analyzeSentence("elma '");
    Assert.assertEquals(2, analyses.size());
    Assert.assertEquals(1, analyses.get(0).analysisCount());
    Assert.assertEquals(1, analyses.get(1).analysisCount());
  }

  @Test
  public void testMultiApostropheWords() {
    TurkishMorphology morphology = getMorphology("Kur'an [P:Prop]");
    WordAnalysis result = morphology.analyze("Kur'an'da");
    Assert.assertEquals(1, result.analysisCount());

    TurkishMorphology emptyMorphology = getEmptyTurkishMorphology();
    WordAnalysis oovResult = emptyMorphology.analyze("Eto'o'nun");
    Assert.assertEquals(2, oovResult.analysisCount());
    Assert.assertEquals("Eto'o", oovResult.getAnalysisResults().get(0).getDictionaryItem().lemma);
  }

  @Test
  public void testAbbreviationWithAndWithoutDerivations() {
    TurkishMorphology morphology = getMorphology(
        "ABD [P:Abbrv; Pr:abede]",
        "TL [P:Abbrv; Pr:tele]",
        "km [P:Abbrv; Pr:keme]",
        "CEO [P:Abbrv; Pr:siyo]",
        "GSYH [P:Abbrv; Pr:geseyehe]");
    Assert.assertEquals(1, morphology.analyze("ABD'li").analysisCount());
    Assert.assertEquals(1, morphology.analyze("ABD'siz").analysisCount());
    Assert.assertTrue(morphology.analyze("TL'lik").stream().anyMatch(a -> !a.isRuntime()));
    Assert.assertTrue(morphology.analyze("km'lik").stream().anyMatch(a -> !a.isRuntime()));
    Assert.assertTrue(morphology.analyze("CEO'su").stream().anyMatch(a -> !a.isRuntime()));
    Assert.assertTrue(morphology.analyze("CEO'sunun").stream().anyMatch(a -> !a.isRuntime()));
    Assert.assertTrue(morphology.analyze("CEO'ları").stream().anyMatch(a -> !a.isRuntime()));
    Assert.assertTrue(morphology.analyze("GSYH'si").stream().anyMatch(a -> !a.isRuntime()));
    Assert.assertTrue(morphology.analyze("CEO'm").stream().noneMatch(a -> !a.isRuntime()));
  }

  @Test
  public void testAlphanumericAndCountryCodeUrlTokens() {
    TurkishMorphology morphology = getEmptyTurkishMorphology();
    Assert.assertEquals(2, morphology.analyze("S3'ün").analysisCount());
    Assert.assertEquals(2, morphology.analyze("A3'ün").analysisCount());
    Assert.assertEquals(1, morphology.analyze("3G'ye").analysisCount());
    Assert.assertEquals(1, morphology.analyze("meb.gov.tr'den").analysisCount());
  }

  @Test
  public void testAnalysisCache() {
    AnalysisCache cache = AnalysisCache.builder().dynamicCacheSize(16, 128).build();
    AtomicInteger stringCalls = new AtomicInteger();
    AtomicInteger tokenCalls = new AtomicInteger();

    cache.getAnalysis("elma", k -> {
      stringCalls.incrementAndGet();
      return WordAnalysis.EMPTY_INPUT_RESULT;
    });
    cache.getAnalysis("elma", k -> {
      stringCalls.incrementAndGet();
      return WordAnalysis.EMPTY_INPUT_RESULT;
    });
    Assert.assertEquals(1, stringCalls.get());

    Token token = new Token("armut", Token.Type.Word, 0, 4);
    cache.getAnalysis(token, t -> {
      tokenCalls.incrementAndGet();
      return WordAnalysis.EMPTY_INPUT_RESULT;
    });
    cache.getAnalysis(token, t -> {
      tokenCalls.incrementAndGet();
      return WordAnalysis.EMPTY_INPUT_RESULT;
    });
    Assert.assertEquals(1, tokenCalls.get());
    Assert.assertTrue(cache.toString().contains("Hit rate:"));

    cache.invalidateDynamicCache();
    cache.getAnalysis("elma", k -> {
      stringCalls.incrementAndGet();
      return WordAnalysis.EMPTY_INPUT_RESULT;
    });
    Assert.assertEquals(2, stringCalls.get());

    AnalysisCache disabled = AnalysisCache.builder().disableDynamicCache().build();
    Assert.assertEquals("Cache is disabled.", disabled.toString());
  }

  @Test
  public void testUnidentifiedWordNoVowel() {
    TurkishMorphology morphology = getMorphology();
    WordAnalysis result = morphology.analyze("gnctrkcll");
    Assert.assertEquals(0, result.analysisCount());
  }

  @Test
  public void testAbbreviationVoicing_Issue_183() {
    TurkishMorphology morphology = getMorphology("Tübitak [P:Abbrv]");
    WordAnalysis result = morphology.analyze("Tübitak'a");
    Assert.assertEquals(1, result.analysisCount());
    result = morphology.analyze("Tübitaka");
    Assert.assertEquals(1, result.analysisCount());
    result = morphology.analyze("Tübitağa");
    Assert.assertEquals(0, result.analysisCount());
  }

  @Test
  public void testAbbreviationShouldNotGetBecomeOrAcquire_Issue218() {
    TurkishMorphology morphology = getMorphology("aa [P:Abbrv]");
    WordAnalysis result = morphology.analyze("aalaş");
    Assert.assertEquals(0, result.analysisCount());
    result = morphology.analyze("aalan");
    Assert.assertEquals(0, result.analysisCount());
  }

  @Test
  public void testAsciiTolerantMorphology() {
    // Instance with no dictionary item.
    TurkishMorphology morphology = getAsciiTolerantMorphology(
        "sıra", "şıra", "armut", "kazan", "ekonomik [P:Adj]", "insan");
    RuleBasedAnalyzer analyzer = morphology.getAnalyzer();
    List<SingleAnalysis> result;
    result = analyzer.analyze("ekonomık");
    Assert.assertTrue(containsAllDictionaryLemma(result, "ekonomik"));
    result = analyzer.analyze("sira");
    Assert.assertEquals(2, result.size());
    Assert.assertTrue(containsAllDictionaryLemma(result, "sıra", "şıra"));
    result = analyzer.analyze("siraci");
    Assert.assertTrue(containsAllDictionaryLemma(result, "sıra", "şıra"));
    result = analyzer.analyze("armutcuga");
    Assert.assertTrue(containsAllDictionaryLemma(result, "armut"));
    result = analyzer.analyze("kazancıga");
    Assert.assertTrue(containsAllDictionaryLemma(result, "kazan"));
    result = analyzer.analyze("kazanciga");
    Assert.assertTrue(containsAllDictionaryLemma(result, "kazan"));
    result = analyzer.analyze("kazançiğimizdan");
    Assert.assertTrue(containsAllDictionaryLemma(result, "kazan"));
    result = analyzer.analyze("ınsanların");
    Assert.assertTrue(containsAllDictionaryLemma(result, "insan"));
  }

  @Test
  public void testLexiconEngineAndPhoneticFixes() throws Exception {
    TurkishMorphology morphology = getMorphology(
        "API [Pr:api; P:Abbrv; A:LocaleEn]",
        "FBI [Pr:efbiay; P:Abbrv; A:LocaleEn]",
        "UNICEF [A:LocaleEn]",
        "LinkedIn [Pr:linktin; A:LocaleEn]",
        "IDEAL [A:LocaleEn, InverseHarmony]",
        "TOGG [Pr:tog; P:Abbrv]",
        "Netflix",
        "e-posta",
        "Hint-Avrupa",
        "Alm. [Pr:almanca; P:Abbrv]",
        "alver",
        "sıfır [P:Num]",
        "dört [P:Num; A:Voicing]",
        "beş [P:Num]",
        "yirmi [P:Num]"
    );

    // 1. LocaleEn uppercase 'I', lowercase 'i', mixed/ALL-CAPS Turkish apostrophe suffixes (+ serializer round-trip)
    java.nio.file.Path tmpBin = java.nio.file.Files.createTempFile("lexicon-test-", ".bin");
    try {
      zemberek.morphology.lexicon.DictionarySerializer.save(morphology.getLexicon(), tmpBin);
      TurkishMorphology reloaded = TurkishMorphology.builder()
          .setLexicon(zemberek.morphology.lexicon.DictionarySerializer.load(tmpBin))
          .build();
      for (String form : new String[]{
          "API", "api", "API'ye", "api'ye", "API'NİN",
          "FBI", "FBI'ın", "FBI'IN", "FBI'DAN",
          "UNICEF", "UNICEF'te", "UNICEF'İN", "UNICEF’İN", "Unicefte",
          "LinkedIn", "LinkedIn'de", "LINKEDIN'DE", "linkedin'de",
          "IDEAL", "IDEAL'de", "IDEAL'İN", "ideal'de"
      }) {
        Assert.assertTrue(
            "Expected lexical analysis for " + form,
            reloaded.analyze(form).stream().anyMatch(a -> !a.isRuntime()));
      }
      Assert.assertTrue(reloaded.analyze("API'ya").stream().noneMatch(a -> !a.isRuntime()));
      Assert.assertTrue(reloaded.analyze("FBI'in").stream().noneMatch(a -> !a.isRuntime()));

      // 2. -og abbreviation must not voice to *toğ
      Assert.assertTrue(reloaded.analyze("TOGG'un").analysisCount() > 0);
      Assert.assertTrue(reloaded.analyze("TOGG'a").analysisCount() > 0);

      // 3. -x -> -ks voiceless suffix harmony (both known dictionary item and unknown proper noun)
      Assert.assertTrue(reloaded.analyze("Netflix'te").analysisCount() > 0);
      Assert.assertTrue(reloaded.analyze("Netflix'ten").analysisCount() > 0);
      Assert.assertTrue(reloaded.analyze("SpaceX'te").analysisCount() > 0);

      // 4. Hyphenated and dotted lemmas preserve '-' in stem & generation without merging unhyphenated lemmas
      Assert.assertTrue(reloaded.analyze("e-posta").analysisCount() > 0);
      Assert.assertEquals("e-posta", reloaded.analyze("e-postaya").getAnalysisResults().get(0).getStem());
      Assert.assertEquals(
          java.util.List.of("e-postaya"),
          reloaded.getWordGenerator()
              .generate(reloaded.getLexicon().getItemById("e-posta_Noun"), "A3sg", "Pnon", "Dat")
              .stream().map(r -> r.surface).toList());
      Assert.assertTrue(reloaded.analyze("Hint-Avrupa").analysisCount() > 0);
      Assert.assertTrue(reloaded.analyze("Alm.").analysisCount() > 0);
      Assert.assertEquals(0, reloaded.analyze("al-ver").analysisCount());

      // 5. .00 clock times follow hour harmony (TDK rule: 20.00'de, 15.00'te, 04.00'e, 00.00'da)
      Assert.assertTrue(reloaded.analyze("20.00'de").analysisCount() > 0);
      Assert.assertTrue(reloaded.analyze("15.00'te").analysisCount() > 0);
      Assert.assertTrue(reloaded.analyze("04.00'e").analysisCount() > 0);
      Assert.assertTrue(reloaded.analyze("00.00'da").analysisCount() > 0);
    } finally {
      java.nio.file.Files.deleteIfExists(tmpBin);
    }
  }

  private boolean containsAllDictionaryLemma(List<SingleAnalysis> analyses, String... item) {

    for (String i : item) {
      boolean fail = true;
      for (SingleAnalysis s : analyses) {
        if (s.getDictionaryItem().lemma.contains(i)) {
          fail = false;
          break;
        }
      }
      if (fail) {
        Log.info("Failed to find item %s", i);
        return false;
      }
    }
    return true;
  }

}
