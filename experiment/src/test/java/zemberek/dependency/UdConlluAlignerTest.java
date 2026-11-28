package zemberek.dependency;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import zemberek.dependency.UdConlluAligner.AlignmentResult;
import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.analysis.format.ConlluFormatter;
import zemberek.morphology.analysis.format.ConlluSentence;
import zemberek.morphology.analysis.format.ConlluToken;

public class UdConlluAlignerTest {

  private static TurkishMorphology morphology;
  private static UdConlluAligner aligner;

  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();

  @BeforeClass
  public static void setUpClass() {
    morphology = TurkishMorphology.createWithDefaults();
    aligner = new UdConlluAligner(morphology, ConlluFormatter.bounDefault());
  }

  @Test
  public void alignsGoldConlluPreservingHeadsAndReplacingMorphology() {
    String goldConllu =
        "# sent_id = test_1\n"
            + "# text = Yarın kar yağacak.\n"
            + "1\tYarın\tyarın\tNOUN\t_\tCase=Nom|Number=Sing|Person=3\t3\tobl\t_\t_\n"
            + "2\tkar\tkar\tNOUN\t_\tCase=Nom|Number=Sing|Person=3\t3\tnsubj\t_\t_\n"
            + "3\tyağacak\tyağ\tVERB\t_\tAspect=Perf|Number=Sing|Person=3|Polarity=Pos|Tense=Fut\t0\troot\t_\tSpaceAfter=No\n"
            + "4\t.\t.\tPUNCT\tPunc\t_\t3\tpunct\t_\t_\n"
            + "\n"
            + "# sent_id = test_2\n"
            + "# text = Evdeydi.\n"
            + "1-2\tEvdeydi\t_\t_\t_\t_\t_\t_\t_\tSpaceAfter=No\n"
            + "1\tEvde\tev\tNOUN\t_\tCase=Loc|Number=Sing|Person=3\t0\troot\t_\t_\n"
            + "2\tydi\ti\tAUX\tZero\tAspect=Perf|Evident=Fh|Number=Sing|Person=3|Polarity=Pos|Tense=Past\t1\tcop\t_\t_\n"
            + "3\t.\t.\tPUNCT\tPunc\t_\t1\tpunct\t_\t_\n";

    List<ConlluSentence> gold = ConlluSentence.parseAll(goldConllu);
    AlignmentResult result = aligner.alignTreebank(gold, false);

    Assert.assertEquals(2, result.stats().totalSentences());
    Assert.assertEquals(2, result.stats().exactMwtSentences());
    Assert.assertEquals(2, result.alignedSentences().size());
    Assert.assertEquals(7, result.stats().totalTokens());
    Assert.assertTrue("UPOS accuracy should be high on clean sample", result.stats().uposAccuracy() >= 85.0);

    // Gold syntactic heads and deprels must be preserved verbatim
    ConlluSentence s1 = result.alignedSentences().get(0);
    Assert.assertEquals(3, s1.tokenById(1).head());
    Assert.assertEquals("obl", s1.tokenById(1).deprel());
    Assert.assertEquals(0, s1.tokenById(3).head());
    Assert.assertEquals("root", s1.tokenById(3).deprel());

    ConlluSentence s2 = result.alignedSentences().get(1);
    Assert.assertEquals(1, s2.mwtSpans().size());
    Assert.assertEquals(0, s2.tokenById(1).head());
    Assert.assertEquals(1, s2.tokenById(2).head());
    Assert.assertEquals("cop", s2.tokenById(2).deprel());
  }

  @Test
  public void fallsBackGracefullyWhenMwtSplitDiffersFromGold() {
    // Gold keeps "Evdeydi" unsplit (1 token), while BOUN ConlluFormatter splits it into 2 tokens.
    String goldUnsplit =
        "# sent_id = mismatch_1\n"
            + "# text = Evdeydi.\n"
            + "1\tEvdeydi\tev\tNOUN\t_\tCase=Loc|Number=Sing|Person=3\t0\troot\t_\tSpaceAfter=No\n"
            + "2\t.\t.\tPUNCT\tPunc\t_\t1\tpunct\t_\t_\n";

    List<ConlluSentence> gold = ConlluSentence.parseAll(goldUnsplit);
    AlignmentResult nonStrict = aligner.alignTreebank(gold, false);
    Assert.assertEquals(0, nonStrict.stats().exactMwtSentences());
    Assert.assertEquals(1, nonStrict.alignedSentences().size());
    Assert.assertEquals(2, nonStrict.alignedSentences().getFirst().size());
    Assert.assertEquals(0, nonStrict.alignedSentences().getFirst().tokenById(1).head());
    Assert.assertEquals("root", nonStrict.alignedSentences().getFirst().tokenById(1).deprel());

    AlignmentResult strict = aligner.alignTreebank(gold, true);
    Assert.assertEquals(0, strict.alignedSentences().size());
  }

  @Test
  public void projectsRawTextFileToConllu() throws IOException {
    Path inTxt = tempFolder.newFile("raw.txt").toPath();
    Path outConllu = tempFolder.getRoot().toPath().resolve("projected.conllu");
    Files.writeString(
        inTxt,
        "Yarın kar yağacak.\nEvdeki çocuklar İstanbul'daydı.\n",
        StandardCharsets.UTF_8);

    int count = aligner.projectRawTextFile(inTxt, outConllu, false, -1);
    Assert.assertEquals(2, count);

    List<ConlluSentence> parsed = ConlluSentence.parseFile(outConllu);
    Assert.assertEquals(2, parsed.size());
    Assert.assertEquals("u_1", parsed.get(0).sentId());
    Assert.assertEquals("u_2", parsed.get(1).sentId());
    for (ConlluToken tok : parsed.get(0).tokens()) {
      Assert.assertFalse(tok.hasHead());
    }
    Assert.assertFalse(parsed.get(1).mwtSpans().isEmpty());
  }

  @Test
  public void rescuesBoundCopulaEncliticsAndUsesTurkishLocaleForLemmas() {
    // 1. Direct enclitic normalizer checks
    Assert.assertEquals("idi", UdConlluAligner.normalizeBoundCopulaEnclitic("ydi"));
    Assert.assertEquals("idik", UdConlluAligner.normalizeBoundCopulaEnclitic("ydık"));
    Assert.assertEquals("imiş", UdConlluAligner.normalizeBoundCopulaEnclitic("ymış"));
    Assert.assertEquals("ise", UdConlluAligner.normalizeBoundCopulaEnclitic("ysa"));
    Assert.assertEquals("iken", UdConlluAligner.normalizeBoundCopulaEnclitic("yken"));

    // 2. When MwtMode.NONE leaves "Evdeydi" unsplit while gold splits "Evde + ydi",
    //    fallback should rescue "ydi" -> AUX (lemma "i") instead of Unknown (X).
    UdConlluAligner noMwtAligner = new UdConlluAligner(morphology, ConlluFormatter.singleToken());
    String goldSplit =
        "# sent_id = enc_1\n"
            + "# text = Evdeydi.\n"
            + "1-2\tEvdeydi\t_\t_\t_\t_\t_\t_\t_\tSpaceAfter=No\n"
            + "1\tEvde\tev\tNOUN\t_\tCase=Loc|Number=Sing|Person=3\t0\troot\t_\t_\n"
            + "2\tydi\ti\tAUX\tZero\tAspect=Perf|Evident=Fh|Number=Sing|Person=3|Polarity=Pos|Tense=Past\t1\tcop\t_\t_\n"
            + "3\t.\t.\tPUNCT\tPunc\t_\t1\tpunct\t_\t_\n";
    AlignmentResult res = noMwtAligner.alignTreebank(ConlluSentence.parseAll(goldSplit), false);
    ConlluToken rescuedCop = res.alignedSentences().getFirst().tokenById(2);
    Assert.assertEquals("AUX", rescuedCop.upos());
    Assert.assertEquals("i", rescuedCop.lemma());

    // 3. Turkish ı/i distinction: gold lemma "kir" must NOT match Zemberek lemma "kır"
    String dotlessI =
        "# sent_id = tr_i\n"
            + "# text = Kır.\n"
            + "1\tKır\tkir\tNOUN\t_\tCase=Nom|Number=Sing|Person=3\t0\troot\t_\tSpaceAfter=No\n"
            + "2\t.\t.\tPUNCT\tPunc\t_\t1\tpunct\t_\t_\n";
    AlignmentResult trRes = aligner.alignTreebank(ConlluSentence.parseAll(dotlessI), false);
    // Token 1 is "kır" (not "kir"), token 2 is "." (matches ".") -> only 1 of 2 lemmas matches
    Assert.assertEquals(1, trRes.stats().lemmaMatches());
  }
}
