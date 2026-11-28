package zemberek.morphology.analysis.format;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import zemberek.core.turkish.PrimaryPos;
import zemberek.morphology.analysis.AnalysisFormatter;
import zemberek.morphology.analysis.SentenceAnalysis;
import zemberek.morphology.analysis.SentenceWordAnalysis;
import zemberek.morphology.analysis.SingleAnalysis;
import zemberek.morphology.analysis.SingleAnalysis.MorphemeData;
import zemberek.morphology.analysis.SingleAnalysis.MorphemeGroup;
import zemberek.morphology.morphotactics.Morpheme;
import zemberek.morphology.morphotactics.TurkishMorphotactics;

/**
 * Exports Zemberek morphological and disambiguated sentence analyses into Universal Dependencies v2
 * 10-column {@code CoNLL-U} format.
 *
 * <p>Populates {@code ID, FORM, LEMMA, UPOS, XPOS, FEATS} and {@code MISC} ({@code DerivedFrom=},
 * {@code SpaceAfter=No}, and optional pipe-escaped Zemberek analysis), leaving syntactic dependency
 * columns ({@code HEAD, DEPREL, DEPS}) as {@code _}.
 *
 * <p>Supports autonomous Multi-Word Token ({@code i-j}) splitting for Turkish bound copular
 * auxiliaries ({@code İstanbul'daydı -> İstanbul'da + ydı}, {@code başlıyorlarmış -> başlıyorlar + mış})
 * and bound relativizer {@code -ki} ({@code Evdeki -> Evde + ki}, {@code seninkiyle -> senin + kiyle}).
 */
public final class ConlluFormatter implements AnalysisFormatter {

  public enum Dialect {
    /** Modern UD v2.14+ / UD Turkic Harmonized conventions ({@code UD_Turkish-BOUN}). */
    UD_TURKIC_BOUN,
    /** {@code UD_Turkish-IMST} conventions ({@code Mood=Ind}, {@code var/yok=ADJ}, {@code -ki=ADP}). */
    UD_IMST
  }

  public enum MwtMode {
    /** Splits overt bound copulas and {@code -ki} ({@code evdeki -> evde + ki}). */
    COPULA_AND_KI,
    /** Splits overt bound copulas only; keeps {@code -ki} inside the word. */
    COPULA_ONLY,
    /** Never emits {@code i-j} Multi-Word Token lines (strict 1-to-1 token alignment). */
    NONE
  }

  private static final Set<String> PRIMARY_VERB_TAM_IDS = Set.of(
      "Pres", "Past", "Narr", "Cond", "Prog1", "Prog2",
      "Aor", "Fut", "Imp", "Opt", "Desr", "Neces");

  private static final Set<String> SECONDARY_COPULA_TAM_IDS = Set.of("Past", "Narr", "Cond");

  private record MwtSplit(
      String form1,
      UdFeatureMapper.ProjectedToken part1,
      String form2,
      UdFeatureMapper.ProjectedToken part2,
      String derivedFrom) {}

  private final Dialect dialect;
  private final MwtMode mwtMode;
  private final boolean includeDerivedFrom;
  private final boolean includeZemberekInMisc;

  private ConlluFormatter(Builder builder) {
    this.dialect = builder.dialect;
    this.mwtMode = builder.mwtMode;
    this.includeDerivedFrom = builder.includeDerivedFrom;
    this.includeZemberekInMisc = builder.includeZemberekInMisc;
  }

  public static ConlluFormatter bounDefault() {
    return builder().build();
  }

  public static ConlluFormatter imstDefault() {
    return builder().dialect(Dialect.UD_IMST).mwtMode(MwtMode.COPULA_AND_KI).build();
  }

  public static ConlluFormatter singleToken() {
    return builder().mwtMode(MwtMode.NONE).build();
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private Dialect dialect = Dialect.UD_TURKIC_BOUN;
    private MwtMode mwtMode = MwtMode.COPULA_AND_KI;
    private boolean includeDerivedFrom = true;
    private boolean includeZemberekInMisc = false;

    public Builder dialect(Dialect dialect) {
      this.dialect = dialect;
      return this;
    }

    public Builder mwtMode(MwtMode mwtMode) {
      this.mwtMode = mwtMode;
      return this;
    }

    public Builder includeDerivedFrom(boolean includeDerivedFrom) {
      this.includeDerivedFrom = includeDerivedFrom;
      return this;
    }

    public Builder includeZemberekInMisc(boolean includeZemberekInMisc) {
      this.includeZemberekInMisc = includeZemberekInMisc;
      return this;
    }

    public ConlluFormatter build() {
      return new ConlluFormatter(this);
    }
  }

  @Override
  public String format(SingleAnalysis analysis) {
    UdFeatureMapper.ProjectedToken p =
        UdFeatureMapper.projectFull(analysis, analysis.surfaceForm(), dialect);
    return p.lemma() + "\t" + p.upos() + "\t" + p.xpos() + "\t" + p.feats();
  }

  public String formatSentence(SentenceAnalysis sentenceAnalysis) {
    return formatSentence(null, sentenceAnalysis);
  }

  public String formatSentence(String sentId, SentenceAnalysis sentenceAnalysis) {
    StringBuilder sb = new StringBuilder(sentenceAnalysis.size() * 64 + 64);
    if (sentId != null && !sentId.isEmpty()) {
      sb.append("# sent_id = ").append(sentId).append('\n');
    }
    String rawSentence = sentenceAnalysis.getSentence() != null ? sentenceAnalysis.getSentence() : "";
    if (!rawSentence.isEmpty()) {
      sb.append("# text = ").append(rawSentence).append('\n');
    }

    List<SentenceWordAnalysis> words = sentenceAnalysis.getWordAnalyses();
    boolean[] spaceAfterNo = computeSpaceAfterNo(rawSentence, words);

    int syntacticId = 1;
    for (int i = 0; i < words.size(); i++) {
      SentenceWordAnalysis swa = words.get(i);
      String form = swa.getWordAnalysis().getInput();
      SingleAnalysis best = swa.getBestAnalysis();
      boolean noSpace = spaceAfterNo[i];

      MwtSplit split = (mwtMode == MwtMode.NONE || best.isUnknown())
          ? null
          : trySplitMwt(form, best);

      if (split != null) {
        int id1 = syntacticId++;
        int id2 = syntacticId++;
        appendRow(sb, id1 + "-" + id2, form, "_", "_", "_", "_",
            buildMisc(split.derivedFrom(), noSpace, best));
        appendRow(sb, String.valueOf(id1), split.form1(),
            split.part1().lemma(), split.part1().upos(), split.part1().xpos(), split.part1().feats(), "_");
        appendRow(sb, String.valueOf(id2), split.form2(),
            split.part2().lemma(), split.part2().upos(), split.part2().xpos(), split.part2().feats(), "_");
      } else {
        UdFeatureMapper.ProjectedToken proj = UdFeatureMapper.projectFull(best, form, dialect);
        appendRow(sb, String.valueOf(syntacticId++), form,
            proj.lemma(), proj.upos(), proj.xpos(), proj.feats(),
            buildMisc(proj.derivedFrom(), noSpace, best));
      }
    }
    return sb.toString();
  }

  public String formatSentences(List<SentenceAnalysis> sentences) {
    StringBuilder sb = new StringBuilder(sentences.size() * 256);
    for (int i = 0; i < sentences.size(); i++) {
      if (i > 0) {
        sb.append('\n');
      }
      sb.append(formatSentence(String.valueOf(i + 1), sentences.get(i)));
    }
    return sb.toString();
  }

  private MwtSplit trySplitMwt(String form, SingleAnalysis analysis) {
    int gc = analysis.groupCount();

    if (gc >= 2) {
      // Case 1a: Nominal/adjectival copular converb '-yken' (Zero -> Verb | While -> Adv, e.g. varken, toplantıdayken)
      if (gc >= 3) {
        MorphemeGroup prevGrp = analysis.getGroup(gc - 2);
        MorphemeGroup lastGrp = analysis.getGroup(gc - 1);
        List<MorphemeData> prevMds = prevGrp.getMorphemes();
        List<MorphemeData> lastMds = lastGrp.getMorphemes();
        if (prevMds.size() >= 2
            && prevMds.getFirst().morpheme == TurkishMorphotactics.zero
            && prevMds.get(1).morpheme.pos == PrimaryPos.Verb
            && !lastMds.isEmpty()
            && lastMds.getFirst().morpheme == TurkishMorphotactics.while_) {
          String copSurf = prevGrp.surfaceForm() + lastGrp.surfaceForm();
          if (canSliceTail(form, copSurf)) {
            int cut = form.length() - copSurf.length();
            String form1 = form.substring(0, cut);
            String form2 = form.substring(cut);
            List<MorphemeData> copMds = new ArrayList<>(prevMds.subList(2, prevMds.size()));
            copMds.addAll(lastMds);
            return new MwtSplit(
                form1,
                UdFeatureMapper.projectGroupRange(analysis, 0, gc - 3, null, form1, dialect),
                form2,
                UdFeatureMapper.projectBoundCopula(copMds, dialect),
                extractDerivedFrom(analysis, gc - 3));
          }
        }
      }

      // Case 1b: Final group is Zero -> Verb (e.g. İstanbul'daydı, vardı, evdeyim, evdedir)
      MorphemeGroup lastGrp = analysis.getGroup(gc - 1);
      List<MorphemeData> lastMds = lastGrp.getMorphemes();
      if (lastMds.size() >= 2
          && lastMds.getFirst().morpheme == TurkishMorphotactics.zero
          && lastMds.get(1).morpheme.pos == PrimaryPos.Verb
          && lastMds.stream().noneMatch(md ->
              md.morpheme == TurkishMorphotactics.while_
                  || md.morpheme == TurkishMorphotactics.asIf)) {
        String copSurf = lastGrp.surfaceForm();
        if (canSliceTail(form, copSurf)) {
          int cut = form.length() - copSurf.length();
          String form1 = form.substring(0, cut);
          String form2 = form.substring(cut);
          return new MwtSplit(
              form1,
              UdFeatureMapper.projectGroupRange(analysis, 0, gc - 2, null, form1, dialect),
              form2,
              UdFeatureMapper.projectBoundCopula(lastMds.subList(2, lastMds.size()), dialect),
              extractDerivedFrom(analysis, gc - 2));
        }
      }

      // Case 2: Bound -ki (adnominal 'evdeki' or pronominal/inflected 'evdekiler', 'seninkiyle')
      if (mwtMode == MwtMode.COPULA_AND_KI) {
        MwtSplit kiSplit = trySplitBoundKi(form, analysis, gc);
        if (kiSplit != null) {
          return kiSplit;
        }
      }
    }

    // Case 3: Compound-tense finite Verb in last group (başlıyorlarmış, ağlayacaktı, geliyorduk)
    MorphemeGroup lastGrp = analysis.getGroup(gc - 1);
    if (lastGrp.getPos() == PrimaryPos.Verb
        && !"değil".equals(analysis.getDictionaryItem().root)
        && !"imek".equals(analysis.getDictionaryItem().lemma)) {
      List<MorphemeData> lastMds = lastGrp.getMorphemes();
      int secIdx = findSecondaryCopulaIndex(lastMds);
      if (secIdx > 0) {
        StringBuilder tailSb = new StringBuilder();
        for (int k = secIdx; k < lastMds.size(); k++) {
          tailSb.append(lastMds.get(k).surface);
        }
        String copSurf = tailSb.toString();
        if (canSliceTail(form, copSurf)) {
          int cut = form.length() - copSurf.length();
          String form1 = form.substring(0, cut);
          String form2 = form.substring(cut);

          List<MorphemeData> hostMds = new ArrayList<>();
          for (int g = 0; g < gc - 1; g++) {
            hostMds.addAll(analysis.getGroup(g).getMorphemes());
          }
          hostMds.addAll(lastMds.subList(0, secIdx));

          UdFeatureMapper.ProjectedToken p1 =
              UdFeatureMapper.projectGroupRange(analysis, 0, gc - 1, hostMds, form1, dialect);
          UdFeatureMapper.ProjectedToken p2 =
              UdFeatureMapper.projectBoundCopula(lastMds.subList(secIdx, lastMds.size()), dialect);
          return new MwtSplit(form1, p1, form2, p2, p1.derivedFrom());
        }
      }
    }
    return null;
  }

  private MwtSplit trySplitBoundKi(String form, SingleAnalysis analysis, int gc) {
    int relGroupIdx = -1;
    for (int g = 1; g < gc; g++) {
      Morpheme d = analysis.getGroup(g).getMorphemes().getFirst().morpheme;
      if (d == TurkishMorphotactics.rel) {
        relGroupIdx = g;
      } else if (relGroupIdx >= 0 && UdFeatureMapper.LEXICAL_DERIVATION_IDS.contains(d.id)) {
        return null;
      }
    }
    if (relGroupIdx < 0) {
      return null;
    }

    StringBuilder kiSurfSb = new StringBuilder();
    List<MorphemeData> kiMds = new ArrayList<>();
    for (int g = relGroupIdx; g < gc; g++) {
      MorphemeGroup grp = analysis.getGroup(g);
      kiSurfSb.append(grp.surfaceForm());
      kiMds.addAll(grp.getMorphemes());
    }
    String kiSurf = kiSurfSb.toString();
    if (!canSliceTail(form, kiSurf)) {
      return null;
    }

    boolean isInflectedPronominal = relGroupIdx < gc - 1
        || analysis.getGroup(relGroupIdx).getPos() == PrimaryPos.Pronoun;
    int cut = form.length() - kiSurf.length();
    String form1 = form.substring(0, cut);
    String form2 = form.substring(cut);
    UdFeatureMapper.ProjectedToken p1 =
        UdFeatureMapper.projectGroupRange(analysis, 0, relGroupIdx - 1, null, form1, dialect);
    UdFeatureMapper.ProjectedToken p2 =
        UdFeatureMapper.projectBoundKi(kiMds.subList(1, kiMds.size()), isInflectedPronominal, dialect);
    return new MwtSplit(form1, p1, form2, p2, p1.derivedFrom());
  }

  private String extractDerivedFrom(SingleAnalysis analysis, int endGroup) {
    return UdFeatureMapper.projectGroupRange(
        analysis, 0, endGroup, null, analysis.surfaceForm(), dialect).derivedFrom();
  }

  private static int findSecondaryCopulaIndex(List<MorphemeData> mds) {
    boolean seenPrimary = false;
    for (int i = 0; i < mds.size(); i++) {
      String id = mds.get(i).morpheme.id;
      if (!seenPrimary) {
        seenPrimary = PRIMARY_VERB_TAM_IDS.contains(id);
      } else if (SECONDARY_COPULA_TAM_IDS.contains(id)) {
        for (int j = i + 1; j < mds.size(); j++) {
          String nextId = mds.get(j).morpheme.id;
          if ("While".equals(nextId) || "AsIf".equals(nextId)) {
            return -1;
          }
        }
        return i;
      }
    }
    return -1;
  }

  private static boolean canSliceTail(String form, String normalizedTail) {
    if (form == null || normalizedTail == null || normalizedTail.isEmpty()) {
      return false;
    }
    int tLen = normalizedTail.length();
    if (tLen >= form.length()) {
      return false;
    }
    String tailSlice = form.substring(form.length() - tLen);
    return tailSlice.indexOf('\'') < 0 && tailSlice.indexOf('’') < 0;
  }

  private String buildMisc(String derivedFrom, boolean spaceAfterNo, SingleAnalysis analysis) {
    List<String> items = new ArrayList<>(3);
    if (includeDerivedFrom && derivedFrom != null && !derivedFrom.isEmpty()) {
      items.add("DerivedFrom=" + sanitizeMiscValue(derivedFrom));
    }
    if (spaceAfterNo) {
      items.add("SpaceAfter=No");
    }
    if (includeZemberekInMisc && !analysis.isUnknown()) {
      items.add("Zemberek=" + sanitizeMiscValue(analysis.formatLong()));
    }
    return items.isEmpty() ? "_" : String.join("|", items);
  }

  private static String sanitizeMiscValue(String val) {
    return val.replace("|", "^DB").replace("\t", " ").replace("\n", " ").replace("\r", " ");
  }

  private static boolean[] computeSpaceAfterNo(String sentence, List<SentenceWordAnalysis> words) {
    int n = words.size();
    boolean[] noSpace = new boolean[n];
    if (sentence == null || sentence.isEmpty() || n == 0) {
      return noSpace;
    }
    int[] starts = new int[n];
    int[] ends = new int[n];
    int cursor = 0;
    for (int i = 0; i < n; i++) {
      String w = words.get(i).getWordAnalysis().getInput();
      int idx = findTokenInSentence(sentence, w, cursor);
      starts[i] = idx;
      ends[i] = idx >= 0 ? idx + w.length() : -1;
      if (idx >= 0) {
        cursor = ends[i];
      }
    }
    for (int i = 0; i + 1 < n; i++) {
      if (ends[i] >= 0 && starts[i + 1] == ends[i]) {
        noSpace[i] = true;
      }
    }
    return noSpace;
  }

  private static int findTokenInSentence(String sentence, String token, int cursor) {
    if (token == null || token.isEmpty()) {
      return -1;
    }
    int idx = sentence.indexOf(token, cursor);
    if (idx >= 0) {
      return idx;
    }
    return token.indexOf('\'') >= 0
        ? sentence.indexOf(token.replace('\'', '’'), cursor)
        : -1;
  }

  private static void appendRow(
      StringBuilder sb,
      String id,
      String form,
      String lemma,
      String upos,
      String xpos,
      String feats,
      String misc) {
    sb.append(id).append('\t')
        .append(form.isEmpty() ? "_" : form).append('\t')
        .append(lemma.isEmpty() ? "_" : lemma).append('\t')
        .append(upos).append('\t')
        .append(xpos).append('\t')
        .append(feats).append('\t')
        .append("_\t_\t_\t")
        .append(misc)
        .append('\n');
  }
}
