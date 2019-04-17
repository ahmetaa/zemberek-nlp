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
import zemberek.core.collections.Histogram;
import zemberek.core.turkish.Turkish;
import zemberek.corpus.Scripts;
import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.analysis.AnalysisFormatter;
import zemberek.morphology.analysis.AnalysisFormatters;
import zemberek.morphology.analysis.SentenceAnalysis;
import zemberek.morphology.analysis.SentenceWordAnalysis;
import zemberek.morphology.analysis.SingleAnalysis;
import zemberek.morphology.analysis.WordAnalysis;
import zemberek.morphology.lexicon.RootLexicon;
import zemberek.morphology.old_ambiguity.AbstractDisambiguator.DataSet;
import zemberek.morphology.old_ambiguity.AbstractDisambiguator.DataSetLoader;
import zemberek.morphology.old_ambiguity.AbstractDisambiguator.SentenceData;

public class DataConverter {

  static Map<String, String> lookup = new HashMap<>();

  static {
    lookup.put("yabancı+noun+a3sg", "yabancı+adj");
    lookup.put("değil+verb+pres+a3sg", "değil+verb+neg+pres+a3sg");
    lookup.put("yetkili+noun+a3pl+p3sg", "yetkili+adj^db+noun+zero+a3pl+acc");
    lookup.put("herkes+noun+a3sg", "herkes+pron+quant+a3pl");
    lookup.put("soyad+noun+a3sg+p3sg", "soyadı+noun+a3sg+p3sg");
    lookup.put("kamuoy+noun+a3sg+p3sg", "kamuoyu+noun+a3sg+p3sg");
    lookup.put("muhafazakar+noun+a3sg", "muhafazakar+adj");
    lookup.put("gazeteci+noun+a3sg", "gazete+noun+a3sg^db+noun+agt+a3sg");
    lookup.put("türk+noun+a3sg", "türk+noun+prop+a3sg");
    lookup.put("ilgili+noun+a3sg", "ilgili+adj");
    lookup.put("üzer+noun+a3sg+p3sg+dat", "üzeri+noun+a3sg+p3sg+dat");
    lookup.put("üzer+noun+a3sg+p3sg+loc", "üzeri+noun+a3sg+p3sg+loc");
    lookup.put("türkçe+adj", "türkçe+noun+prop+a3sg");
    lookup.put("cumhurbaşkan+noun+a3sg+p3sg", "cumhurbaşkanı+noun+a3sg+p3sg");
    lookup.put("yılbaş+noun+a3sg+p3sg", "yılbaşı+noun+a3sg+p3sg");
    lookup.put("değil+verb+pres+a3sg+cop", "değil+verb+neg+pres+a3sg+cop");
    lookup.put("ünlü+noun+a3sg", "ün+noun+a3sg^db+adj+with");
    lookup.put("gerek+noun+a3sg^db+adj+with", "gerekli+adj");
    lookup.put("erken+adj", "erken+adv");
    lookup.put("milletvekil+noun+a3sg+p3sg", "milletvekili+noun+a3sg+p3sg");
    lookup.put("işbirlik+noun+a3sg+p3sg", "işbirliği+noun+a3sg+p3sg");
    lookup.put("Meral", "işbirliği+noun+a3sg+p3sg");
    lookup.put("islami+adj", "islami+adj+prop");
    lookup.put("bir+num+dist", "birer+num+dist");
    lookup.put("müdürlük+noun+a3sg+p3sg", "müdürlüğü+noun+a3sg+p3sg");
    lookup.put("toplumsal+adj", "toplum+noun+a3sg^db+adj+related");
    lookup.put("üzer+noun+a3sg+p3sg+loc^db+adj+rel", "üzeri+noun+a3sg+loc+adj+rel");
    lookup.put("ol+verb^db+adj+prespart^db+noun+zero+a3pl", "ol+verb^db+noun+prespart+a3pl");
    lookup.put("herkes+noun+a3sg+gen", "herkes+pron+quant+a3pl+gen");
    lookup.put("müslüman+adj", "müslüman+noun+prop+a3sg");
    lookup.put("yazılı+noun+a3sg", "yazılı+adj");
    lookup.put("sorumlu+noun+a3sg^db+noun+ness+a3sg", "sorumluluk+noun+a3sg");
    lookup.put("işadam+noun+a3sg+p3sg", "işadamı+noun+a3sg+p3sg");
    lookup.put("değil+verb+pres+a1pl", "değil+verb+neg+pres+a1pl");
    lookup.put("değil+verb+pres+a1sg", "değil+verb+neg+pres+a1sg");
    lookup.put("değil+verb+past+a3sg", "değil+verb+neg+past+a3sg");
    lookup.put("kamuoy+noun+a3sg+p3sg+loc", "kamuoyu+noun+a3sg+p3sg+loc");
    lookup.put("çık+verb^db+verb+caus+past+a3sg", "çıkar+verb+past+a3sg");
  }


  public static void main(String[] args) throws IOException {

    Path dataPath = Paths.get("/home/ahmetaa/apps/Hasim_Sak_Data/data.test.txt");
    Path output = Paths.get("/media/ahmetaa/depo/ambiguity/sak.test");

    extract(dataPath, output);

  }

  private static void extract(Path dataPath, Path output) throws IOException {
    DataSet set = com.google.common.io.Files
        .asCharSource(dataPath.toFile(), Charsets.UTF_8).readLines(new DataSetLoader());

    TurkishMorphology morphology = TurkishMorphology.create(
        RootLexicon.builder().addTextDictionaryResources(
            "tr/master-dictionary.dict",
            "tr/non-tdk.dict",
            "tr/proper.dict",
            "tr/proper-from-corpus.dict",
            "tr/abbreviations.dict",
            "tr/person-names.dict").build());

    List<SentenceAnalysis> result = new ArrayList<>();
    Histogram<String> parseFails = new Histogram<>();
    for (SentenceData sentenceData : set) {
      //System.out.println(sentenceData.correctParse);
      List<String> tokens = Splitter.on(" ").splitToList(sentenceData.sentence());
      if (tokens.size() == 0 || tokens.size() != sentenceData.correctParse.size()) {
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
        p = p.replaceAll("\\+cop\\+a3sg", "+a3sg+cop");
        p = p.replaceAll("\\+Unable", "^DB+Verb+Able+Neg");
        if (lookup.containsKey(p)) {
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
          if (Character.isUpperCase(s.charAt(0)) && (p.contains("+noun") && !p.contains("prop"))) {
            String pp = p.replaceFirst("\\+noun", "\\+noun+prop");
            for (SingleAnalysis analysis : a) {
              String of = convert(analysis);
              if (of.equals(pp)) {
                best = analysis;
                break;
              }
            }
          }
        }
        if (best == null) {
          List<String> z = a.getAnalysisResults().stream()
              .map(DataConverter::convert)
              .collect(Collectors.toList());
          parseFails.add(s + " " + p);

        } else {
          correctList.add(new SentenceWordAnalysis(best, a));
        }
      }

      if (correctList.size() == tokens.size()) {
        result.add(new SentenceAnalysis(sentenceData.sentence(), correctList));
      }
    }

    Scripts.saveUnambiguous(result, output);

    parseFails.removeSmaller(3);
    parseFails.saveSortedByCounts(Paths.get("parse-fails.txt"), " ");

    System.out.format("Full Sentence Match  = %d in %d%n", result.size(), set.sentences.size());
  }

  static AnalysisFormatter formatter = AnalysisFormatters.OflazerStyleFormatter
      .usingDictionaryRoot();

  private static String convert(SingleAnalysis analysis) {
    String of = formatter.format(analysis);
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
