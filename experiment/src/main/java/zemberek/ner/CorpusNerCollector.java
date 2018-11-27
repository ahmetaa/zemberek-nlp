package zemberek.ner;

import com.google.common.collect.Sets;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import zemberek.core.logging.Log;
import zemberek.core.text.BlockTextLoader;
import zemberek.core.text.TextChunk;
import zemberek.core.turkish.SecondaryPos;
import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.analysis.SingleAnalysis;
import zemberek.morphology.analysis.WordAnalysis;
import zemberek.morphology.lexicon.DictionaryItem;
import zemberek.morphology.lexicon.RootLexicon;
import zemberek.ner.NerDataSet.AnnotationStyle;
import zemberek.normalization.TextCleaner;

public class CorpusNerCollector {

  public static void main(String[] args) throws IOException {

    Path corporaRoot = Paths.get("/media/ahmetaa/depo/corpora");
    Path corpusDirList = corporaRoot.resolve("ner-list");
    Path outRoot = Paths.get("/media/ahmetaa/depo/ner/out");

    Files.createDirectories(outRoot);

    BlockTextLoader corpusProvider = BlockTextLoader
        .fromDirectoryRoot(corporaRoot, corpusDirList, 10_000);

    // assumes you generated a model in my-model directory.
    Path modelRoot = Paths.get("my-model");

    TurkishMorphology morphology = TurkishMorphology
        .builder()
        .setLexicon(RootLexicon.getDefault())
        .disableUnidentifiedTokenAnalyzer()
        .build();

    PerceptronNer ner = PerceptronNer.loadModel(modelRoot, morphology);

    Set<String> illegal = Sets.newHashSet(".", ",", "!", "?", ":");

    List<String> lines = new ArrayList<>();
    int c = 0;
    int k = 0;
    for (TextChunk chunk : corpusProvider) {
      LinkedHashSet<String> sentences =
          new LinkedHashSet<>(TextCleaner.cleanAndExtractSentences(chunk.getData()));
      for (String sentence : sentences) {
        if (sentence.length() > 100) {
          continue;
        }
        NerSentence result = ner.findNamedEntities(sentence);
        int neCount = result.getNamedEntities().size();
        List<NamedEntity> nes = result.getNamedEntities();
        boolean badNamedEntity = false;
        for (NamedEntity ne : nes) {
          for(NerToken token : ne.tokens) {
            if(illegal.contains(token.word)) {
              badNamedEntity = true;
              break;
            }
            WordAnalysis a = morphology.analyze(token.word);
            for (SingleAnalysis analysis : a) {
              DictionaryItem item = analysis.getDictionaryItem();
              if(item.secondaryPos!= SecondaryPos.Abbreviation &&
                  item.secondaryPos!= SecondaryPos.ProperNoun
              ) {
                badNamedEntity = true;
                break;
              }
            }
          }
          if(badNamedEntity) {
            break;
          }
        }
        if (badNamedEntity) {
          continue;
        }
        if (neCount > 0 && neCount < 3) {
          lines.add(result.getAsTrainingSentence(AnnotationStyle.BRACKET));
          c++;
          if (c == 1000) {
            Path out = outRoot.resolve(chunk.id + "-" + k);
            Files.write(out, lines);
            Log.info("%s created. ", out);
            lines = new ArrayList<>();
            c = 0;
            k++;
            if(k>10) {
              System.exit(0);
            }
          }
        }
      }
    }
  }

}
