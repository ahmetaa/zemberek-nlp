package zemberek.morphology.ambiguity;

import com.google.common.base.Charsets;
import com.google.common.base.Splitter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import zemberek.core.turkish.Turkish;
import zemberek.corpus.Scripts;
import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.analysis.AnalysisFormatters;
import zemberek.morphology.analysis.SentenceAnalysis;
import zemberek.morphology.analysis.SentenceWordAnalysis;
import zemberek.morphology.analysis.SingleAnalysis;
import zemberek.morphology.analysis.WordAnalysis;
import zemberek.morphology.old_ambiguity.AbstractDisambiguator.DataSet;
import zemberek.morphology.old_ambiguity.AbstractDisambiguator.DataSetLoader;
import zemberek.morphology.old_ambiguity.AbstractDisambiguator.SentenceData;

public class DataConverter {

  static Map<String, String> lookup = new HashMap<>();
  static {
    lookup.put("yabancı+noun+a3sg","yabancı+adj");
    lookup.put("değil+verb+pres+a3sg","değil+verb+neg+pres+a3sg");
    lookup.put("yetkili+noun+a3pl+p3sg","yetkili+adj^db+noun+zero+a3pl+acc");
    lookup.put("herkes+noun+a3sg","herkes+pron+quant+a3pl");
    lookup.put("soyad+noun+a3sg+p3sg","soyadı+noun+a3sg+p3sg");
    lookup.put("kamuoy+noun+a3sg+p3sg","kamuoyu+noun+a3sg+p3sg");
    lookup.put("muhafazakar+noun+a3sg","muhafazakar+adj");
    lookup.put("gazeteci+noun+a3sg","gazete+noun+a3sg^db+noun+agt+a3sg");
  }


  public static void main(String[] args) throws IOException {

    Path dataPath = Paths.get("/home/ahmetaa/apps/Hasim_Sak_Data/data.dev.txt");
    Path output = Paths.get("data/ambiguity/sak.dev");

    extract(dataPath, output);

  }

  private static void extract(Path dataPath, Path output) throws IOException {
    DataSet set = com.google.common.io.Files
        .asCharSource(dataPath.toFile(), Charsets.UTF_8).readLines(new DataSetLoader());

    TurkishMorphology morphology = TurkishMorphology.createWithDefaults();

    List<SentenceAnalysis> result = new ArrayList<>();
    for (SentenceData sentenceData : set) {
      //System.out.println(sentenceData.correctParse);
      List<String> tokens = Splitter.on(" ").splitToList(sentenceData.sentence());
      if(tokens.size()==0 || tokens.size()!=sentenceData.correctParse.size()) {
        continue;
      }

      List<SentenceWordAnalysis> correctList = new ArrayList<>();

      for (int i = 0; i < tokens.size(); i++) {
        String s = tokens.get(i);
        String p = sentenceData.correctParse.get(i);
        p = p.replaceAll("PCNom", "PCNOM");
        p = p.replaceAll("Pnon|Nom", "");
        p = p.replaceAll("\\+Pos\\+", "+");
        p = p.replaceAll("\\+Pos\\^DB", "^DB");
        p = p.replaceAll("[+]+", "+");
        p = p.replaceAll("[+]$", "");
        p = p.replaceAll("[+]\\^DB", "^DB");
        p = p.replaceAll("[.]", "");
        p = p.toLowerCase(Turkish.LOCALE);
        p = p.replaceAll("adverb", "adv");
        p = p.replaceAll("afterdoingso", "afterdoing");
        p = p.replaceAll("\\+cop\\+a3sg", "+a3sg+cop");
        if(lookup.containsKey(p)) {
          p = lookup.get(p);
        }

        WordAnalysis a = morphology.analyze(s);
        if (!a.isCorrect()) {
          break;
        }
        SingleAnalysis best = null;
        for (SingleAnalysis analysis : a) {
          String of = convert(analysis);
          if (of.equals(p)) {
            best = analysis;
            break;
          }
        }
        if (best == null) {
          List<String> z = a.getAnalysisResults().stream()
              .map(DataConverter::convert)
              .collect(Collectors.toList());

          //System.out.println("Not found [" + s + " " + p + "] - " + String.join(" ", z));
          break;
        }
        correctList.add(new SentenceWordAnalysis(best, a));
      }

      if (correctList.size() == tokens.size()) {
        result.add(new SentenceAnalysis(sentenceData.sentence(), correctList));
      }
    }

    Scripts.saveUnambiguous(result, output);

    System.out.format("Full Sentence Match  = %d in %d%n", result.size(), set.sentences.size());
  }

  private static String convert(SingleAnalysis analysis) {
    String of = AnalysisFormatters.OFLAZER_STYLE.format(analysis);
    of = of.replaceAll("[.]", "");
    of = of.replaceAll("Time", "");
    of = of.replaceAll("[+]+", "+");
    of = of.replaceAll("[+]$", "");
    of = of.replaceAll("[+]$", "");
    of = of.toLowerCase(Turkish.LOCALE);
    of = of.replaceAll("adverb", "adv");
    of = of.replaceAll("abbrv", "prop");

    return of;
  }



}
