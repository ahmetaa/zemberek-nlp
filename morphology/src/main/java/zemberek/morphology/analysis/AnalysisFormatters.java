package zemberek.morphology.analysis;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import zemberek.core.turkish.PrimaryPos;
import zemberek.core.turkish.SecondaryPos;
import zemberek.morphology.analysis.SingleAnalysis.MorphemeData;
import zemberek.morphology.lexicon.DictionaryItem;
import zemberek.morphology.morphotactics.Morpheme;

public class AnalysisFormatters {

  /**
   * Default morphological Analysis formatter. Pipe `|` represents derivation boundary. Left side of
   * `→` represents derivation causing morpheme, right side represents the derivation result.
   * <pre>
   * kitap -> [kitap:Noun] kitap:Noun+A3s
   * kitaplarda -> [kitap:Noun] kitap:Noun+lar:A3pl+da:Loc
   * okut -> [okumak:Verb] oku:Verb|t:Caus→Verb+Imp+A2sg
   * </pre>
   */
  public static AnalysisFormatter DEFAULT = DefaultFormatter.surfaceAndLexical();

  /**
   * Default lexical morphological analysis formatter.
   * <pre>
   * kitap -> [kitap:Noun] Noun+A3s
   * kitaplarda -> [kitap:Noun] Noun+A3pl+Loc
   * okut -> [okumak:Verb] Verb|Caus→Verb+Imp+A2sg
   * </pre>
   */
  public static AnalysisFormatter DEFAULT_LEXICAL = DefaultFormatter.onlyLexical();

  /**
   * Default lexical morphological analysis formatter. But it will not contain Dictionary item
   * related data.
   * <pre>
   * kitap -> Noun+A3sg
   * kitaplarda -> Noun+A3pl+Loc
   * okut -> Verb|Caus→Verb+Imp+A2sg
   * </pre>
   */
  public static AnalysisFormatter DEFAULT_LEXICAL_ONLY_MORPHEMES = MorphemesFormatter.lexical();

  /**
   * Format's analysis result similar to tools developed by Kemal Oflazer. However, this does not
   * generate exactly same outputs as morpheme names and morphotactics are slightly different. For
   * example output will not contain "Pnon" or "Nom" morphemes.
   * <pre>
   *   kitaplarda -> kitap+Noun+A3pl+Loc
   *   kitapsız -> kitap+Noun+A3sg^DB+Adj+Without
   *   kitaplardaymış -> kitap+Noun+A3pl+Loc^DB+Verb+Zero+Narr+A3sg
   * </pre>
   */
  public static AnalysisFormatter OFLAZER_STYLE = new OflazerStyleFormatter();

  /**
   * Generates " + " separated Morpheme ids. Such as:
   * <pre>
   *   kitap -> Noun + A3sg
   *   kitaba -> Noun + A3sg + Dat
   *   kitapçığa -> Noun + A3sg + Dim + Noun + A3sg + Dat
   * </pre>
   */
  public static AnalysisFormatter LEXICAL_SEQUENCE = lexicalSequenceFormatter();

  /**
   * Generates " + " separated Surface forms and Morpheme ids:
   * <pre>
   *   kitap -> kitap:Noun + A3sg
   *   kitaba -> kitab:Noun + A3sg + a:Dat
   *   kitapçığa -> kitap:Noun + A3sg + çığ:Dim + Noun + A3sg + a:Dat
   * </pre>
   */
  public static AnalysisFormatter SURFACE_AND_LEXICAL_SEQUENCE = surfaceSequenceFormatter();

  /**
   * Generates space separated surface forms. Such as:
   * <pre>
   *   kitap -> kitap
   *   kitaba -> kitab a
   *   kitabımızdan -> kitab ımız dan
   * </pre>
   */
  public static AnalysisFormatter SURFACE_SEQUENCE = new OnlySurfaceFormatter();

  static AnalysisFormatter lexicalSequenceFormatter() {
    return analysis -> analysis.getMorphemeDataList().stream()
        .map(s -> s.morpheme.id)
        .collect(Collectors.joining(" + "));
  }

  static AnalysisFormatter surfaceSequenceFormatter() {
    return analysis ->
        analysis.getMorphemeDataList().stream()
            .map(MorphemeData::toMorphemeString)
            .collect(Collectors.joining(" + "));
  }

  public static class OflazerStyleFormatter implements AnalysisFormatter {

    private boolean useRoot = false;

    public OflazerStyleFormatter() {
    }

    private OflazerStyleFormatter(boolean useRoot) {
      this.useRoot = useRoot;
    }

    public static OflazerStyleFormatter usingDictionaryRoot() {
      return new OflazerStyleFormatter(true);
    }

    @Override
    public String format(SingleAnalysis analysis) {
      List<MorphemeData> surfaces = analysis.getMorphemeDataList();

      StringBuilder sb = new StringBuilder(surfaces.size() * 4);

      // root and suffix formatting

      String stemStr = useRoot ? analysis.getDictionaryItem().root : analysis.getStem();
      sb.append(stemStr).append('+');
      DictionaryItem item = analysis.getDictionaryItem();
      PrimaryPos pos = item.primaryPos;

      String posStr = pos == PrimaryPos.Adverb ? "Adverb" : pos.shortForm;

      sb.append(posStr);
      if (item.secondaryPos != SecondaryPos.None && item.secondaryPos != SecondaryPos.UnknownSec) {
        sb.append('+').append(item.secondaryPos.shortForm);
      }
      if (surfaces.size() > 1 && !surfaces.get(1).morpheme.derivational) {
        sb.append("+");
      }

      for (int i = 1; i < surfaces.size(); i++) {
        MorphemeData s = surfaces.get(i);
        if (s.morpheme.derivational) {
          sb.append("^DB+");
          sb.append(surfaces.get(i + 1).morpheme.id)
              .append("+"); // Oflazer first puts the derivation result morpheme.
          sb.append(s.morpheme.id);
          i++;
        } else {
          sb.append(s.morpheme.id);
        }
        if (i < surfaces.size() - 1 && !surfaces.get(i + 1).morpheme.derivational) {
          sb.append("+");
        }

      }
      return sb.toString();
    }
  }

  static class MorphemesFormatter implements AnalysisFormatter {

    boolean addSurface;

    static MorphemesFormatter lexical() {
      return new MorphemesFormatter(false);
    }

    static MorphemesFormatter surface() {
      return new MorphemesFormatter(true);
    }

    public MorphemesFormatter(boolean addSurface) {
      this.addSurface = addSurface;
    }

    @Override
    public String format(SingleAnalysis analysis) {
      List<MorphemeData> surfaces = analysis.getMorphemeDataList();

      StringBuilder sb = new StringBuilder(surfaces.size() * 4);

      // root and suffix formatting
      if (addSurface) {
        sb.append(analysis.getStem()).append(':');
      }
      sb.append(surfaces.get(0).morpheme.id);
      if (surfaces.size() > 1 && !surfaces.get(1).morpheme.derivational) {
        sb.append("+");
      }
      for (int i = 1; i < surfaces.size(); i++) {
        MorphemeData s = surfaces.get(i);
        Morpheme morpheme = s.morpheme;
        if (morpheme.derivational) {
          sb.append('|');
        }
        if (addSurface && s.surface.length() > 0) {
          sb.append(s.surface).append(':');
        }
        sb.append(s.morpheme.id);
        if (s.morpheme.derivational) {
          sb.append('→');
        } else if (i < surfaces.size() - 1 && !surfaces.get(i + 1).morpheme.derivational) {
          sb.append('+');
        }
      }
      return sb.toString();
    }
  }

  static class DefaultFormatter implements AnalysisFormatter {

    boolean addSurface;
    MorphemesFormatter morphemesFormatter;

    static DefaultFormatter onlyLexical() {
      return new DefaultFormatter(false);
    }

    static DefaultFormatter surfaceAndLexical() {
      return new DefaultFormatter(true);
    }

    DefaultFormatter(boolean addSurface) {
      this.addSurface = addSurface;
      morphemesFormatter = addSurface ? MorphemesFormatter.surface() :
          MorphemesFormatter.lexical();
    }

    @Override
    public String format(SingleAnalysis analysis) {
      List<MorphemeData> surfaces = analysis.getMorphemeDataList();

      StringBuilder sb = new StringBuilder(surfaces.size() * 5);

      // dictionary item formatting
      sb.append('[');
      DictionaryItem item = analysis.getDictionaryItem();
      sb.append(item.lemma).append(':').append(item.primaryPos.shortForm);
      if (item.secondaryPos != SecondaryPos.None) {
        sb.append(',').append(item.secondaryPos.shortForm);
      }
      sb.append("] ");

      // root and suffix formatting
      sb.append(morphemesFormatter.format(analysis));
      return sb.toString();
    }
  }

  static class OnlySurfaceFormatter implements AnalysisFormatter {

    @Override
    public String format(SingleAnalysis analysis) {
      List<String> tokens = new ArrayList<>(3);

      for (MorphemeData mSurface : analysis.getMorphemeDataList()) {
        if (mSurface.surface.length() > 0) {
          tokens.add(mSurface.surface);
        }
      }
      return String.join(" ", tokens);
    }
  }

}
