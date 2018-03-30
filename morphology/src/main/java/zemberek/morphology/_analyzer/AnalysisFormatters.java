package zemberek.morphology._analyzer;

import java.util.List;
import java.util.stream.Collectors;
import zemberek.core.turkish.PrimaryPos;
import zemberek.core.turkish.SecondaryPos;
import zemberek.morphology._analyzer._SingleAnalysis.MorphemeSurface;
import zemberek.morphology.lexicon.DictionaryItem;

public class AnalysisFormatters {

  public static AnalysisFormatter DEFAULT_SURFACE = DefaultFormatter.surface();
  public static AnalysisFormatter DEFAULT_LEXICAL = DefaultFormatter.lexical();
  public static AnalysisFormatter OFLAZER_STYLE = new OflazerStyleFormatter();
  public static AnalysisFormatter LEXICAL_SEQUENCE = lexicalSequenceFormatter();
  public static AnalysisFormatter SURFACE_SEQUENCE = surfaceSequenceFormatter();

  public static AnalysisFormatter lexicalSequenceFormatter() {
    return analysis -> String.join(" + ", analysis.getMorphemesSurfaces().stream()
        .map(s -> s.morpheme.id)
        .collect(Collectors.toList()));
  }

  public static AnalysisFormatter surfaceSequenceFormatter() {
    return analysis ->
        String.join(" + ", analysis.getMorphemesSurfaces().stream()
            .map(MorphemeSurface::toMorphemeString)
            .collect(Collectors.toList()));
  }

  static class OflazerStyleFormatter implements AnalysisFormatter {

    @Override
    public String format(_SingleAnalysis analysis) {
      List<MorphemeSurface> surfaces = analysis.getMorphemesSurfaces();

      StringBuilder sb = new StringBuilder(surfaces.size() * 4);

      // root and suffix formatting
      sb.append(analysis.getStem()).append('+');
      DictionaryItem item = analysis.getItem();
      PrimaryPos pos = item.primaryPos;

      String posStr = pos == PrimaryPos.Adverb ? "Adverb" : pos.shortForm;

      sb.append(posStr).append("+");
      if (item.secondaryPos != SecondaryPos.None) {
        sb.append(item.secondaryPos.shortForm).append("+");
      }

      for (int i = 1; i < surfaces.size(); i++) {
        MorphemeSurface s = surfaces.get(i);
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

  static class DefaultFormatter implements AnalysisFormatter {

    boolean addSurface;

    static DefaultFormatter lexical() {
      return new DefaultFormatter(false);
    }

    static DefaultFormatter surface() {
      return new DefaultFormatter(true);
    }

    public DefaultFormatter(boolean addSurface) {
      this.addSurface = addSurface;
    }

    @Override
    public String format(_SingleAnalysis analysis) {
      List<MorphemeSurface> surfaces = analysis.getMorphemesSurfaces();

      StringBuilder sb = new StringBuilder(surfaces.size() * 5);

      // dictionary item formatting
      sb.append('[');
      DictionaryItem item = analysis.getItem();
      sb.append(item.lemma).append(':').append(item.primaryPos.shortForm);
      if (item.secondaryPos != SecondaryPos.None) {
        sb.append(',').append(item.secondaryPos.shortForm);
      }
      sb.append("] ");

      // root and suffix formatting
      sb.append(analysis.getStem()).append(':').append(surfaces.get(0).morpheme.id);
      if (surfaces.size() > 1) {
        sb.append("+");
      }
      for (int i = 1; i < surfaces.size(); i++) {
        MorphemeSurface s = surfaces.get(i);
        if (s.morpheme.derivational) {
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

}
