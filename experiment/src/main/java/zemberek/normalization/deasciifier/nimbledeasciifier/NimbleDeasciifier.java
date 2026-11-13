package zemberek.normalization.deasciifier.nimbledeasciifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import zemberek.normalization.deasciifier.nimbledeasciifier.TurkishTextCodec.Casing;
import zemberek.normalization.deasciifier.nimbledeasciifier.TurkishTextCodec.SqueezedWord;
import zemberek.normalization.deasciifier.nimbledeasciifier.TurkishTextCodec.Token;
import zemberek.normalization.deasciifier.nimbledeasciifier.NimbleCharModel.ScoredSurface;
import zemberek.normalization.deasciifier.nimbledeasciifier.NimbleLexicon.ConfusionSet;

/**
 * Nimble, morphology-free Turkish deasciifier.
 *
 * <p>Provides a sub-megabyte, zero-dependency alternative to {@code LatticeDeasciifier} without
 * loading {@code TurkishMorphology}:
 * <ol>
 *   <li><b>Span & Orthographic Shields</b>: Preserves URLs, emails, handles, alphanumeric codes,
 *       Roman numerals, CamelCase brands, foreign terms, and pinned Turkish diacritics, while
 *       resolving numerals ({@code 3'üncü}, {@code 6'şar}), acronyms ({@code TÜİK}, {@code ÖTV'nin}),
 *       and expressive elongations ({@code çoooook}).
 *   <li><b>5-State Vowel-Harmony Character Perceptron</b>: Decodes OOVs, typos, apostrophe stems
 *       with cross-apostrophe context, and regular inflections in flat primitive DP arrays.
 *   <li><b>Front-Coded Confusion & Residual Tables + Context Micro-Perceptron</b>: Disambiguates
 *       high-impact Turkish homographs and circumflex minimal pairs via Pass-1 trigram beam search
 *       ({@code beamWidth = 4}) and Pass-2 bidirectional SOV wide-context refinement.
 * </ol>
 *
 * <p>Thread-safe and stateless after construction.
 */
public final class NimbleDeasciifier {

  /** Controls how circumflex vowels ({@code â, î, û}) are emitted during deasciification. */
  public enum CircumflexMode {
    /** Never inserts new circumflexes; preserves any circumflexes already present in the input. */
    NONE,
    /** Inserts circumflexes only on meaning-disambiguating minimal pairs (default). */
    DISAMBIGUATION_ONLY,
    /** Allows circumflexes on any corpus entry supported by the model. */
    FULL
  }

  public static final CircumflexMode DEFAULT_CIRCUMFLEX_MODE = CircumflexMode.DISAMBIGUATION_ONLY;
  public static final int DEFAULT_BEAM_WIDTH = 4;
  private static final int MAX_PLAIN_TOKEN_CACHE = 32_768;

  static final Set<String> DISAMBIGUATING_CIRCUMFLEX_PREFIXES =
      Set.of(
          "kâr", "hâlâ", "âdet", "âmâ", "dâhi", "dâhice", "âşık", "âşığ", "hâkim", "âlem", "yâr",
          "şûra", "tâbi", "vâris", "resmî", "askerî", "millî", "tarihî", "dinî", "ilmî",
          "edebî");

  private static final Set<String> QUESTION_CLITICS =
      Set.of(
          "mi", "misin", "misiniz", "midir", "midirler", "miydi", "miydik", "miydin",
          "miydiniz", "miyim", "miyiz", "miymis",
          "mu", "musun", "musunuz", "mudurlar", "muydu", "muyduk", "muydun",
          "muydunuz", "muyum", "muyuz", "muymus");

  private static final Set<String> PALATAL_FRONT_ROUNDED_STEMS =
      Set.of(
          "alkol", "gol", "rol", "kontrol", "petrol", "protokol", "sembol", "idol",
          "metropol", "mentol", "parol", "bandrol", "monopol");

  private static final Set<String> PALATAL_FRONT_UNROUNDED_STEMS =
      Set.of(
          "saat", "dikkat", "hakikat", "harf", "kalp", "festival", "terminal", "sinyal",
          "kristal", "metal", "moral", "normal", "marjinal", "orijinal", "radikal", "global",
          "final", "lokal", "müzikal", "resital", "karnaval", "mareşal", "amiral", "general",
          "liberal", "kemal", "cemal", "celal", "bilal", "hilal", "meral", "zuhal");

  private static final float[] SINGLE_ZERO_PRIOR = {0.0f};

  static final class TokenColumn {
    final NimbleCandidate[] candidates;
    final boolean isQuestionClitic;
    final float[] unaryScores;

    TokenColumn(
        NimbleCandidate[] candidates, boolean isQuestionClitic, float[] unaryScores) {
      this.candidates = candidates;
      this.isQuestionClitic = isQuestionClitic;
      this.unaryScores = unaryScores;
    }

    boolean isAmbiguous() {
      return candidates.length >= 2;
    }
  }

  private record BeamHyp(
      NimbleCandidate prev, NimbleCandidate current, float score, BeamHyp back) {}

  private final NimbleModel model;
  private final NimbleLexicon lexicon;
  private final NimbleCharModel charModel;
  private final NimbleWeightTable sentenceWeights;
  private final int beamWidth;
  private final CircumflexMode circumflexMode;
  private final ConcurrentHashMap<String, NimbleCandidate[]> plainTokenCache =
      new ConcurrentHashMap<>(4096);

  public static NimbleDeasciifier createWithDefaults() {
    return new NimbleDeasciifier(NimbleModel.loadDefault());
  }

  public NimbleDeasciifier(NimbleModel model) {
    this(model, DEFAULT_BEAM_WIDTH, DEFAULT_CIRCUMFLEX_MODE);
  }

  public NimbleDeasciifier(NimbleModel model, int beamWidth, CircumflexMode circumflexMode) {
    this(
        Objects.requireNonNull(model, "model must not be null"),
        model.lexicon(),
        model.charModel(),
        model.sentenceWeights(),
        beamWidth,
        circumflexMode);
  }

  NimbleDeasciifier(
      NimbleLexicon lexicon,
      NimbleCharModel charModel,
      NimbleWeightTable sentenceWeights,
      int beamWidth,
      CircumflexMode circumflexMode) {
    this(null, lexicon, charModel, sentenceWeights, beamWidth, circumflexMode);
  }

  private NimbleDeasciifier(
      NimbleModel model,
      NimbleLexicon lexicon,
      NimbleCharModel charModel,
      NimbleWeightTable sentenceWeights,
      int beamWidth,
      CircumflexMode circumflexMode) {
    this.model = model;
    this.lexicon = Objects.requireNonNull(lexicon, "lexicon must not be null");
    this.charModel = Objects.requireNonNull(charModel, "charModel must not be null");
    this.sentenceWeights = Objects.requireNonNull(sentenceWeights, "sentenceWeights must not be null");
    this.beamWidth = Math.max(1, beamWidth);
    this.circumflexMode = Objects.requireNonNull(circumflexMode, "circumflexMode must not be null");
  }

  public NimbleDeasciifier withCircumflexMode(CircumflexMode mode) {
    if (model != null) {
      return new NimbleDeasciifier(model, beamWidth, mode);
    }
    return new NimbleDeasciifier(lexicon, charModel, sentenceWeights, beamWidth, mode);
  }

  public CircumflexMode circumflexMode() {
    return circumflexMode;
  }

  public NimbleModel model() {
    return model;
  }

  public String deasciify(String text) {
    return deasciify(text, circumflexMode);
  }

  public String deasciify(String text, CircumflexMode mode) {
    if (text == null || text.isEmpty()) {
      return text;
    }
    CircumflexMode activeMode = Objects.requireNonNull(mode, "mode must not be null");
    List<Token> tokens = TurkishTextCodec.tokenize(text);
    if (tokens.isEmpty()) {
      return text;
    }

    boolean pinDotlessI =
        !(!TurkishTextCodec.containsNonDotlessTurkishDiacritic(text)
            && TurkishTextCodec.containsDotlessI(text)
            && shouldUnpinDotlessI(text, tokens));

    int n = tokens.size();
    long[] featBuf = new long[NimbleContextScorer.FEATURE_BUFFER_SIZE];
    TokenColumn[] columns = buildSentenceColumns(text, tokens, pinDotlessI);

    NimbleCandidate[] chosen;
    if (!hasAmbiguousColumn(columns)) {
      chosen = new NimbleCandidate[n];
      for (int i = 0; i < n; i++) {
        TokenColumn col = columns[i];
        if (col.isQuestionClitic) {
          int prevHarmony = i > 0 ? chosen[i - 1].lastVowelHarmony : 2;
          chosen[i] = resolveQuestionClitic(tokens.get(i), prevHarmony, pinDotlessI);
        } else {
          chosen[i] = col.candidates[0];
        }
      }
    } else {
      computeUnaryScores(tokens, columns, featBuf);
      chosen = decodeBeamPass1(tokens, columns, pinDotlessI, featBuf);
      refineWithWideContext(tokens, columns, chosen, pinDotlessI, featBuf);
    }

    StringBuilder sb = null;
    int cursor = 0;
    for (int i = 0; i < n; i++) {
      Token tok = tokens.get(i);
      Token activeTok = pinDotlessI ? tok : unwrapDotlessIPin(tok);
      String filteredSurface =
          applyCircumflexFilter(chosen[i].surface, activeTok.original(), activeMode);
      String restored =
          projectCasingAndPins(
              activeTok, filteredSurface, chosen[i].foreignStemLength, pinDotlessI);
      if (!restored.equals(tok.original())) {
        if (sb == null) {
          sb = new StringBuilder(text.length());
        }
        sb.append(text, cursor, tok.start());
        sb.append(restored);
        cursor = tok.end();
      }
    }
    if (sb == null) {
      return text;
    }
    sb.append(text, cursor, text.length());
    return sb.toString();
  }

  TokenColumn[] buildSentenceColumns(String sentence, List<Token> tokens, boolean pinDotlessI) {
    int n = tokens.size();
    TokenColumn[] columns = new TokenColumn[n];
    for (int i = 0; i < n; i++) {
      Token tok = tokens.get(i);
      Token activeTok = pinDotlessI ? tok : unwrapDotlessIPin(tok);
      boolean isQuestion =
          !activeTok.immutable()
              && !activeTok.hasApostrophe()
              && QUESTION_CLITICS.contains(activeTok.asciiLower());

      NimbleCandidate[] cands;
      if (isQuestion) {
        cands =
            new NimbleCandidate[] {
              NimbleCandidate.create(
                  activeTok.asciiLower(), activeTok.asciiLower(), 0, 0.0f, 0.0f, false, false)
            };
      } else if (!activeTok.immutable()
          && !activeTok.hasPinnedTurkish()
          && activeTok.original().length() == 1) {
        cands = resolveSingleCharOrthography(activeTok, tokens, i, pinDotlessI);
      } else if (pinDotlessI
          && !activeTok.immutable()
          && !activeTok.hasPinnedTurkish()
          && !activeTok.numeralApostrophe()
          && !activeTok.hasApostrophe()
          && activeTok.casing() == Casing.LOWER) {
        cands = plainTokenCache.get(activeTok.original());
        if (cands == null) {
          cands = buildCandidatesForToken(activeTok, true);
          if (plainTokenCache.size() < MAX_PLAIN_TOKEN_CACHE) {
            plainTokenCache.putIfAbsent(activeTok.original(), cands);
          }
        }
      } else {
        cands = buildCandidatesForToken(activeTok, pinDotlessI);
      }
      columns[i] = new TokenColumn(cands, isQuestion, null);
    }
    return columns;
  }

  static boolean hasAmbiguousColumn(TokenColumn[] columns) {
    for (TokenColumn col : columns) {
      if (col.isAmbiguous()) {
        return true;
      }
    }
    return false;
  }

  void computeUnaryScores(List<Token> tokens, TokenColumn[] columns, long[] featBuf) {
    int n = tokens.size();
    char sentenceEndPunct = tokens.get(n - 1).rightPunct();
    boolean sentenceCapitalized = isSentenceMajorityCapitalized(tokens);
    for (int i = 0; i < n; i++) {
      TokenColumn col = columns[i];
      if (!col.isAmbiguous()) {
        columns[i] = new TokenColumn(col.candidates, col.isQuestionClitic, SINGLE_ZERO_PRIOR);
        continue;
      }
      int k = col.candidates.length;
      float[] unary = new float[k];
      for (int ci = 0; ci < k; ci++) {
        unary[ci] =
            NimbleContextScorer.scoreUnary(
                i,
                tokens,
                sentenceEndPunct,
                sentenceCapitalized,
                col.candidates[ci],
                lexicon,
                sentenceWeights,
                featBuf);
      }
      columns[i] = new TokenColumn(col.candidates, col.isQuestionClitic, unary);
    }
  }

  private NimbleCandidate[] buildCandidatesForToken(Token token, boolean pinDotlessI) {
    if (token.immutable()) {
      return singleCandidate(token.original(), token.asciiLower(), 0, false);
    }
    if (token.numeralApostrophe()) {
      return singleCandidate(
          TurkishTextCodec.restoreNumeralApostrophe(token), token.asciiLower(), 0, false);
    }
    if (!TurkishTextCodec.hasAmbiguousAscii(token.asciiLower())) {
      return singleCandidate(
          TurkishTextCodec.toTurkishLower(token.original()), token.asciiLower(), 0, false);
    }

    String ascii = token.asciiLower();
    if (!token.hasApostrophe()
        && token.casing() == Casing.UPPER
        && !token.hasPinnedTurkish()) {
      String acr = lexicon.getAllCapsTurkishAcronym(ascii);
      if (acr != null) {
        return singleCandidate(acr, ascii, 0, false);
      }
      if (lexicon.isForeignAsciiKeep(ascii, Casing.UPPER)) {
        return singleCandidate(ascii, ascii, token.original().length(), false);
      }
    }

    if (token.hasApostrophe()) {
      return buildApostropheCandidates(token, pinDotlessI);
    }

    if (TurkishTextCodec.hasLetterElongation(ascii)) {
      return buildElongatedCandidates(token, pinDotlessI);
    }

    if (!token.hasPinnedTurkish() && lexicon.isForeignAsciiKeep(ascii, token.casing())) {
      return singleCandidate(ascii, ascii, token.original().length(), false);
    }

    List<String> surfaces = new ArrayList<>(4);
    List<Float> logProbs = new ArrayList<>(4);
    List<Boolean> properFlags = new ArrayList<>(4);

    ConfusionSet proper =
        (!token.sentenceStart() && token.casing() == Casing.TITLE)
            ? lexicon.getProperConfusion(ascii)
            : null;
    ConfusionSet lower = lexicon.getLowerConfusion(ascii);

    if (proper != null) {
      addConfusionEntries(token, proper, pinDotlessI, true, surfaces, logProbs, properFlags);
    }
    if (lower != null) {
      addConfusionEntries(token, lower, pinDotlessI, false, surfaces, logProbs, properFlags);
    } else if (surfaces.isEmpty()) {
      String residual = lexicon.getSingleResidual(ascii);
      if (residual != null
          && TurkishTextCodec.isCompatibleWithInput(token.original(), residual, pinDotlessI)) {
        surfaces.add(residual);
        logProbs.add(0.0f);
        properFlags.add(false);
      }
    }

    boolean isOov = false;
    if (surfaces.isEmpty() && token.sentenceStart() && token.casing() == Casing.TITLE) {
      ConfusionSet startProper = lexicon.getProperConfusion(ascii);
      if (startProper != null) {
        addConfusionEntries(token, startProper, pinDotlessI, true, surfaces, logProbs, properFlags);
      }
    }
    if (surfaces.isEmpty()) {
      surfaces.add(charModel.decodeTop1(token, pinDotlessI));
      logProbs.add(0.0f);
      properFlags.add(!token.sentenceStart() && token.casing() == Casing.TITLE);
      isOov = true;
    }

    int count = surfaces.size();
    String[] surfArr = surfaces.toArray(new String[0]);
    float[] charRel =
        count >= 2
            ? charModel.scoreCandidateDifferences(token, surfArr, count)
            : SINGLE_ZERO_PRIOR;

    if (token.casing() == Casing.LOWER
        && lower != null
        && count >= 2
        && charRel[0] <= -2.0f
        && surfArr[0].equals(lower.surfaces()[0])) {
      ConfusionSet allProper = lexicon.getProperConfusion(ascii);
      if (allProper != null && allProper.surfaces().length >= 2) {
        String topLower = lower.surfaces()[0];
        String topProper = allProper.surfaces()[0];
        if (!TurkishTextCodec.stripCircumflex(topProper)
                .equals(TurkishTextCodec.stripCircumflex(topLower))
            && properGap(allProper, topLower) >= 3.0f) {
          for (int i = 0; i < count; i++) {
            charRel[i] = 0.0f;
            if (surfArr[i].equals(topProper)) {
              properFlags.set(i, true);
            }
          }
        }
      }
    }

    NimbleCandidate[] result = new NimbleCandidate[count];
    for (int i = 0; i < count; i++) {
      result[i] =
          NimbleCandidate.create(
              surfArr[i],
              ascii,
              0,
              logProbs.get(i),
              charRel[i],
              properFlags.get(i),
              isOov);
    }
    return result;
  }

  private static float properGap(ConfusionSet allProper, String topLower) {
    String[] surfaces = allProper.surfaces();
    float[] logProbs = allProper.logProbs();
    float lowerLogProb = -5.0f;
    for (int i = 0; i < surfaces.length; i++) {
      if (surfaces[i].equals(topLower)) {
        lowerLogProb = logProbs[i];
        break;
      }
    }
    return logProbs[0] - lowerLogProb;
  }

  private NimbleCandidate[] resolveSingleCharOrthography(
      Token activeTok, List<Token> tokens, int index, boolean pinDotlessI) {
    String orig = activeTok.original();
    if ("S".equals(orig)
        && index > 0
        && "A".equals(tokens.get(index - 1).original())
        && tokens.get(index - 1).rightPunct() == '.') {
      return singleCandidate("ş", "s", 0, false);
    }
    if ("O".equals(orig)
        && index > 0
        && "M".equals(tokens.get(index - 1).original())
        && tokens.get(index - 1).rightPunct() == '.') {
      return singleCandidate("ö", "o", 0, false);
    }
    if ("I".equals(orig)
        && activeTok.rightPunct() == '.'
        && (index == 0
            || !(tokens.get(index - 1).original().length() == 1
                && tokens.get(index - 1).rightPunct() == '.'))
        && index + 1 < tokens.size()
        && (tokens.get(index + 1).casing() == Casing.TITLE
            || tokens.get(index + 1).casing() == Casing.UPPER)) {
      return singleCandidate("ı", "i", 0, false);
    }
    return buildCandidatesForToken(activeTok, pinDotlessI);
  }

  private NimbleCandidate[] buildApostropheCandidates(Token token, boolean pinDotlessI) {
    int apos = token.apostropheIndex();
    String orig = token.original();
    char aposChar = orig.charAt(apos);
    String stemOrig = orig.substring(0, apos);
    String stemAscii = token.asciiLower().substring(0, apos);
    String suffixAscii = token.asciiLower().substring(apos + 1);
    Casing stemCasing = TurkishTextCodec.detectCasing(stemOrig, stemOrig.length());

    if (endsWithDigit(stemAscii)) {
      int lastDigitEnd = stemAscii.length();
      int digitStart = lastDigitEnd - 1;
      while (digitStart > 0 && Character.isDigit(stemAscii.charAt(digitStart - 1))) {
        digitStart--;
      }
      String numVowelStem =
          TurkishTextCodec.spokenTurkishNumberStem(stemAscii.substring(digitStart, lastDigitEnd));
      String surface =
          stemAscii + aposChar + harmonizeSuffix(numVowelStem, suffixAscii, orig, apos + 1, pinDotlessI, false);
      return singleCandidate(surface, token.asciiLower(), stemOrig.length(), false);
    }

    if (stemCasing == Casing.UPPER) {
      String trAcr = lexicon.getAllCapsTurkishAcronym(stemAscii);
      if (trAcr != null) {
        String pronStem = acronymPronunciationStem(trAcr);
        String surface =
            trAcr + aposChar + harmonizeSuffix(pronStem, suffixAscii, orig, apos + 1, pinDotlessI, false);
        if (TurkishTextCodec.isCompatibleWithInput(orig, surface, pinDotlessI)) {
          return singleCandidate(surface, token.asciiLower(), 0, false);
        }
      }
      if (lexicon.isForeignAsciiKeep(stemAscii, Casing.UPPER)) {
        String surface =
            stemAscii + aposChar + harmonizeSuffix(stemAscii, suffixAscii, orig, apos + 1, pinDotlessI, true);
        if (TurkishTextCodec.isCompatibleWithInput(orig, surface, pinDotlessI)) {
          return singleCandidate(surface, token.asciiLower(), stemOrig.length(), false);
        }
      }
      if (!token.hasPinnedTurkish() && isLikelyAllCapsInitialism(stemOrig, stemAscii)) {
        String stemLower = TurkishTextCodec.toTurkishLower(stemOrig);
        String pronStem = acronymPronunciationStem(stemLower);
        String surface =
            stemLower + aposChar + harmonizeSuffix(pronStem, suffixAscii, orig, apos + 1, pinDotlessI, false);
        if (TurkishTextCodec.isCompatibleWithInput(orig, surface, pinDotlessI)) {
          return singleCandidate(surface, token.asciiLower(), stemOrig.length(), false);
        }
      }
    }

    if (TurkishTextCodec.isCamelCaseIdentifier(stemOrig)
        || lexicon.isForeignAsciiKeep(stemAscii, stemCasing)) {
      String surface =
          stemAscii + aposChar + harmonizeSuffix(stemAscii, suffixAscii, orig, apos + 1, pinDotlessI, true);
      if (TurkishTextCodec.isCompatibleWithInput(orig, surface, pinDotlessI)) {
        return singleCandidate(surface, token.asciiLower(), stemOrig.length(), false);
      }
    }

    List<String> stems = new ArrayList<>(3);
    List<Float> priors = new ArrayList<>(3);
    List<Boolean> properFlags = new ArrayList<>(3);

    ConfusionSet proper =
        (!token.sentenceStart() && token.casing() == Casing.TITLE)
            ? lexicon.getProperConfusion(stemAscii)
            : null;
    ConfusionSet lower = lexicon.getLowerConfusion(stemAscii);
    if (proper != null) {
      collectCompatibleStems(proper, stemOrig, pinDotlessI, true, stems, priors, properFlags);
    }
    if (lower != null) {
      collectCompatibleStems(lower, stemOrig, pinDotlessI, false, stems, priors, properFlags);
    } else if (stems.isEmpty()) {
      String res = lexicon.getSingleResidual(stemAscii);
      if (res != null && TurkishTextCodec.isCompatibleWithInput(stemOrig, res, pinDotlessI)) {
        stems.add(res);
        priors.add(0.0f);
        properFlags.add(false);
      }
    }
    if (stems.isEmpty() && token.sentenceStart() && stemCasing == Casing.TITLE) {
      ConfusionSet startProper = lexicon.getProperConfusion(stemAscii);
      if (startProper != null) {
        collectCompatibleStems(startProper, stemOrig, pinDotlessI, true, stems, priors, properFlags);
      }
    }

    if (stems.isEmpty()) {
      stems.add(charModel.decodeSliceTop1(token, 0, apos, pinDotlessI));
      priors.add(0.0f);
      properFlags.add(stemCasing == Casing.TITLE && !token.sentenceStart());
    }

    List<NimbleCandidate> out = new ArrayList<>(stems.size());
    for (int i = 0; i < stems.size(); i++) {
      String stemLower = stems.get(i);
      String full =
          stemLower + aposChar + harmonizeSuffix(stemLower, suffixAscii, orig, apos + 1, pinDotlessI, false);
      if (TurkishTextCodec.isCompatibleWithInput(orig, full, pinDotlessI)) {
        boolean dup = false;
        for (NimbleCandidate existing : out) {
          if (existing.surface.equals(full)) {
            dup = true;
            break;
          }
        }
        if (!dup) {
          out.add(
              NimbleCandidate.create(
                  full, token.asciiLower(), 0, priors.get(i), 0.0f, properFlags.get(i), false));
        }
      }
    }
    if (out.isEmpty()) {
      String fallback = charModel.decodeTop1(token, pinDotlessI);
      return singleCandidate(fallback, token.asciiLower(), 0, true);
    }
    return out.toArray(new NimbleCandidate[0]);
  }

  private NimbleCandidate[] buildElongatedCandidates(Token token, boolean pinDotlessI) {
    SqueezedWord squeezed1 = TurkishTextCodec.squeezeElongations(token.asciiLower(), 1);
    SqueezedWord active = squeezed1;
    if (!lexicon.hasKnownAsciiKey(squeezed1.squeezedAscii())) {
      SqueezedWord squeezed2 = TurkishTextCodec.squeezeElongations(token.asciiLower(), 2);
      if (lexicon.hasKnownAsciiKey(squeezed2.squeezedAscii())) {
        active = squeezed2;
      }
    }

    String sqAscii = active.squeezedAscii();
    String sqOrig = buildSqueezedOrig(token.original(), active);
    Token sqTok =
        new Token(
            token.start(),
            token.start() + sqAscii.length(),
            sqOrig,
            sqAscii,
            token.casing(),
            sqOrig.indexOf('\''),
            false,
            false,
            token.sentenceStart(),
            token.rightPunct(),
            TurkishTextCodec.containsPinnedTurkish(sqOrig));

    String decodedSq;
    ConfusionSet lower = lexicon.getLowerConfusion(sqAscii);
    if (lower != null) {
      decodedSq = lower.surfaces()[0];
    } else {
      String res = lexicon.getSingleResidual(sqAscii);
      decodedSq = res != null ? res : charModel.decodeTop1(sqTok, pinDotlessI);
    }
    String projected = active.projectToOriginalLength(decodedSq);
    if (!TurkishTextCodec.isCompatibleWithInput(token.original(), projected, pinDotlessI)) {
      projected = charModel.decodeTop1(token, pinDotlessI);
    }
    return singleCandidate(projected, token.asciiLower(), 0, false);
  }

  private static String buildSqueezedOrig(String orig, SqueezedWord sw) {
    int[] starts = sw.runStarts();
    int[] ends = sw.runEnds();
    char[] out = new char[starts.length];
    for (int i = 0; i < starts.length; i++) {
      char chosen = orig.charAt(starts[i]);
      for (int p = starts[i]; p < ends[i]; p++) {
        char c = orig.charAt(p);
        if (TurkishTextCodec.isPinnedTurkishDiacritic(c)) {
          chosen = c;
          break;
        }
      }
      out[i] = chosen;
    }
    return new String(out);
  }

  private static void addConfusionEntries(
      Token token,
      ConfusionSet set,
      boolean pinDotlessI,
      boolean isProper,
      List<String> surfaces,
      List<Float> logProbs,
      List<Boolean> properFlags) {
    for (int i = 0; i < set.surfaces().length; i++) {
      String candidate = set.surfaces()[i];
      if (!TurkishTextCodec.isCompatibleWithInput(token.original(), candidate, pinDotlessI)) {
        continue;
      }
      int existingIdx = surfaces.indexOf(candidate);
      if (existingIdx < 0) {
        surfaces.add(candidate);
        logProbs.add(set.logProbs()[i]);
        properFlags.add(isProper);
      } else if (set.logProbs()[i] > logProbs.get(existingIdx)) {
        logProbs.set(existingIdx, set.logProbs()[i]);
      }
    }
  }

  private static void collectCompatibleStems(
      ConfusionSet set,
      String stemOrig,
      boolean pinDotlessI,
      boolean isProper,
      List<String> stems,
      List<Float> priors,
      List<Boolean> properFlags) {
    for (int i = 0; i < set.surfaces().length; i++) {
      String s = set.surfaces()[i];
      if (TurkishTextCodec.isCompatibleWithInput(stemOrig, s, pinDotlessI) && !stems.contains(s)) {
        stems.add(s);
        priors.add(set.logProbs()[i]);
        properFlags.add(isProper);
      }
    }
  }

  NimbleCandidate[] decodeBeamPass1(
      List<Token> tokens, TokenColumn[] columns, boolean pinDotlessI, long[] featBuf) {
    int n = columns.length;
    List<BeamHyp> beam = new ArrayList<>(beamWidth);
    beam.add(new BeamHyp(NimbleCandidate.BOUNDARY, NimbleCandidate.BOUNDARY, 0.0f, null));

    for (int i = 0; i < n; i++) {
      TokenColumn col = columns[i];
      if (col.isQuestionClitic) {
        List<BeamHyp> nextBeam = new ArrayList<>(beam.size());
        Token qTok = tokens.get(i);
        for (BeamHyp hyp : beam) {
          NimbleCandidate qCand =
              resolveQuestionClitic(qTok, hyp.current.lastVowelHarmony, pinDotlessI);
          float trans =
              NimbleContextScorer.scoreTransition(
                  hyp.prev, hyp.current, qCand, sentenceWeights, featBuf);
          insertTopBeam(
              nextBeam, new BeamHyp(hyp.current, qCand, hyp.score + trans, hyp), beamWidth);
        }
        beam = nextBeam;
        continue;
      }

      if (!col.isAmbiguous() && beam.size() == 1) {
        BeamHyp hyp = beam.getFirst();
        NimbleCandidate onlyCand = col.candidates[0];
        beam.set(0, new BeamHyp(hyp.current, onlyCand, 0.0f, hyp));
        continue;
      }

      List<BeamHyp> nextBeam = new ArrayList<>(beamWidth);
      for (BeamHyp hyp : beam) {
        for (int ci = 0; ci < col.candidates.length; ci++) {
          NimbleCandidate cur = col.candidates[ci];
          float trans =
              NimbleContextScorer.scoreTransition(
                  hyp.prev, hyp.current, cur, sentenceWeights, featBuf);
          insertTopBeam(
              nextBeam,
              new BeamHyp(hyp.current, cur, hyp.score + col.unaryScores[ci] + trans, hyp),
              beamWidth);
        }
      }
      if (nextBeam.isEmpty()) {
        nextBeam.add(new BeamHyp(NimbleCandidate.BOUNDARY, col.candidates[0], 0.0f, beam.getFirst()));
      }
      beam = nextBeam;
    }

    NimbleCandidate[] chosen = new NimbleCandidate[n];
    BeamHyp curHyp = beam.getFirst();
    for (int i = n - 1; i >= 0 && curHyp != null; i--) {
      chosen[i] = curHyp.current;
      curHyp = curHyp.back;
    }
    return chosen;
  }

  private void refineWithWideContext(
      List<Token> tokens,
      TokenColumn[] columns,
      NimbleCandidate[] chosen,
      boolean pinDotlessI,
      long[] featBuf) {
    int n = columns.length;
    for (int sweep = 0; sweep < 2; sweep++) {
      boolean changed = false;
      int start = (sweep == 0) ? n - 1 : 0;
      int step = (sweep == 0) ? -1 : 1;

      for (int i = start; i >= 0 && i < n; i += step) {
        TokenColumn col = columns[i];
        if (col.isQuestionClitic) {
          NimbleCandidate expected =
              resolveQuestionClitic(
                  tokens.get(i), i > 0 ? chosen[i - 1].lastVowelHarmony : 2, pinDotlessI);
          if (!expected.surface.equals(chosen[i].surface)) {
            chosen[i] = expected;
            changed = true;
          }
          continue;
        }
        if (!col.isAmbiguous()) {
          continue;
        }
        NimbleCandidate bestCand = chosen[i];
        float bestScore = Float.NEGATIVE_INFINITY;
        for (int ci = 0; ci < col.candidates.length; ci++) {
          NimbleCandidate cand = col.candidates[ci];
          float s =
              NimbleContextScorer.scoreCandidateInSequence(
                  i, chosen, n, cand, col.unaryScores[ci], sentenceWeights, featBuf);
          if (s > bestScore) {
            bestScore = s;
            bestCand = cand;
          }
        }
        if (bestCand != chosen[i]) {
          chosen[i] = bestCand;
          changed = true;
          if (i + 1 < n && columns[i + 1].isQuestionClitic) {
            chosen[i + 1] =
                resolveQuestionClitic(tokens.get(i + 1), bestCand.lastVowelHarmony, pinDotlessI);
          }
        }
      }
      if (!changed) {
        break;
      }
    }
  }

  private static void insertTopBeam(List<BeamHyp> beam, BeamHyp cand, int maxBeam) {
    if (beam.size() == maxBeam && cand.score <= beam.get(maxBeam - 1).score) {
      return;
    }
    int pos = beam.size();
    while (pos > 0 && cand.score > beam.get(pos - 1).score) {
      pos--;
    }
    beam.add(pos, cand);
    if (beam.size() > maxBeam) {
      beam.remove(maxBeam);
    }
  }

  private static NimbleCandidate resolveQuestionClitic(
      Token token, int prevHarmony, boolean pinDotlessI) {
    char targetVowel =
        switch (prevHarmony) {
          case 1 -> 'ı';
          case 3 -> 'u';
          case 4 -> 'ü';
          default -> 'i';
        };
    String ascii = token.asciiLower();
    char[] chars = ascii.toCharArray();
    for (int i = 0; i < chars.length; i++) {
      char raw = token.original().charAt(i);
      boolean pinned =
          token.hasPinnedTurkish()
              && (pinDotlessI
                  ? TurkishTextCodec.isPinnedTurkishDiacritic(raw)
                  : TurkishTextCodec.isNonDotlessTurkishDiacritic(raw));
      if (pinned) {
        chars[i] = TurkishTextCodec.toTurkishLower(raw);
      } else if (chars[i] == 'i' || chars[i] == 'u') {
        chars[i] = targetVowel;
      } else if (ascii.charAt(i) == 's'
          && i >= 3
          && (ascii.charAt(i - 1) == 'i' || ascii.charAt(i - 1) == 'u')) {
        chars[i] = 'ş';
      }
    }
    String surf = new String(chars);
    return NimbleCandidate.create(surf, ascii, 0, 0.0f, 0.0f, false, false);
  }

  /**
   * Detects sentences that were mistakenly lowercased with {@code Turkish.LOCALE} from pure-ASCII
   * text (e.g. {@code "ıstanbul'da hava bugun cok guzel."}), causing ASCII {@code 'I'} to turn
   * into dotless {@code 'ı'} inside front-vowel words.
   */
  private boolean shouldUnpinDotlessI(String sentence, List<Token> tokens) {
    int corruptedVotes = 0;
    int validVotes = 0;

    for (Token tok : tokens) {
      if (tok.immutable() || tok.numeralApostrophe() || !TurkishTextCodec.containsDotlessI(tok.original())) {
        continue;
      }
      String stem = tok.hasApostrophe() ? tok.original().substring(0, tok.apostropheIndex()) : tok.original();
      String stemLower = TurkishTextCodec.toTurkishLower(stem);
      if (hasFrontDotlessIDisharmony(stemLower)) {
        corruptedVotes += 2;
        continue;
      }

      String asciiKey =
          tok.hasApostrophe() ? tok.asciiLower().substring(0, tok.apostropheIndex()) : tok.asciiLower();
      if (lexicon.hasKnownAsciiKey(asciiKey)) {
        if (lexicon.hasCompatibleKnownSurface(stem, asciiKey, true)) {
          validVotes++;
        } else {
          corruptedVotes += 2;
        }
        continue;
      }

      ScoredSurface pinnedTop = charModel.decodeTop1WithScore(tok, true);
      ScoredSurface unpinnedTop = charModel.decodeTop1WithScore(tok, false);
      if (pinnedTop.surface().equals(unpinnedTop.surface())) {
        validVotes++;
      } else if (unpinnedTop.score() - pinnedTop.score() >= 2.2f) {
        corruptedVotes++;
      }
    }
    return corruptedVotes > validVotes;
  }

  private static boolean hasFrontDotlessIDisharmony(String stemLower) {
    if (stemLower.indexOf('ı') < 0) {
      return false;
    }
    for (int i = 0; i < stemLower.length(); i++) {
      if (stemLower.charAt(i) == 'e') {
        return true;
      }
    }
    return false;
  }

  private static String applyCircumflexFilter(
      String candidateLower, String origToken, CircumflexMode mode) {
    if (mode == CircumflexMode.FULL) {
      return candidateLower;
    }
    String stripped = TurkishTextCodec.stripCircumflex(candidateLower);
    if (stripped.equals(candidateLower)) {
      return candidateLower;
    }
    boolean allowMinimalPair =
        mode == CircumflexMode.DISAMBIGUATION_ONLY && startsWithDisambiguatingPrefix(candidateLower);
    char[] out = candidateLower.toCharArray();
    for (int i = 0; i < out.length; i++) {
      char c = out[i];
      if (c == 'â' || c == 'î' || c == 'û') {
        char origCh = i < origToken.length() ? origToken.charAt(i) : 0;
        boolean pinnedCirc = origCh == 'â' || origCh == 'Â' || origCh == 'î' || origCh == 'Î' || origCh == 'û' || origCh == 'Û';
        if (!pinnedCirc && !allowMinimalPair) {
          out[i] = TurkishTextCodec.stripCircumflex(c);
        }
      }
    }
    return new String(out);
  }

  static boolean startsWithDisambiguatingPrefix(String surfaceLower) {
    if (surfaceLower.length() < 3) {
      return false;
    }
    for (String prefix : DISAMBIGUATING_CIRCUMFLEX_PREFIXES) {
      if (surfaceLower.startsWith(prefix)) {
        return true;
      }
    }
    return false;
  }

  private boolean isLikelyAllCapsInitialism(String stemOrig, String stemAscii) {
    if (stemOrig.length() < 2
        || stemOrig.length() > 6
        || lexicon.hasKnownAsciiKey(stemAscii)) {
      return false;
    }
    boolean hasVowel = false;
    for (int i = 0; i < stemAscii.length(); i++) {
      if (TurkishTextCodec.isTurkishVowel(stemAscii.charAt(i))) {
        hasVowel = true;
        break;
      }
    }
    if (!hasVowel) {
      return true;
    }
    int n = stemAscii.length();
    return n <= 4
        && !TurkishTextCodec.isTurkishVowel(stemAscii.charAt(n - 1))
        && (n == 2 || !TurkishTextCodec.isTurkishVowel(stemAscii.charAt(n - 2)));
  }

  private static String acronymPronunciationStem(String restoredAcronymLower) {
    int n = restoredAcronymLower.length();
    if (n == 0) {
      return "";
    }
    char last = restoredAcronymLower.charAt(n - 1);
    if (TurkishTextCodec.isTurkishVowel(last)) {
      return String.valueOf(last);
    }
    if (last == 'k') {
      if (n >= 3 && TurkishTextCodec.isTurkishVowel(restoredAcronymLower.charAt(n - 2))) {
        return restoredAcronymLower;
      }
      return "ka";
    }
    if (n >= 3 && TurkishTextCodec.isTurkishVowel(restoredAcronymLower.charAt(n - 2))) {
      return restoredAcronymLower;
    }
    return "e";
  }

  private static String harmonizeSuffix(
      String pronStemLower,
      String suffixAscii,
      String fullOrig,
      int suffixOffset,
      boolean pinDotlessI,
      boolean foreignStem) {
    boolean front = true;
    boolean rounded = false;
    if (PALATAL_FRONT_ROUNDED_STEMS.contains(pronStemLower)) {
      front = true;
      rounded = true;
    } else if (PALATAL_FRONT_UNROUNDED_STEMS.contains(pronStemLower)
        || (pronStemLower.length() >= 5 && pronStemLower.endsWith("lal"))) {
      front = true;
      rounded = false;
    } else if (foreignStem && endsWithSilentEnglishE(pronStemLower)) {
      front = true;
      rounded = false;
    } else {
      for (int i = pronStemLower.length() - 1; i >= 0; i--) {
        char v = TurkishTextCodec.toTurkishLower(pronStemLower.charAt(i));
        if (TurkishTextCodec.isTurkishVowel(v)) {
          front = TurkishTextCodec.isFrontVowel(v);
          rounded = TurkishTextCodec.isRoundedVowel(v);
          if (v == 'â' || v == 'î' || v == 'û') {
            front = (v == 'î' || v == 'û');
            rounded = (v == 'û');
          }
          break;
        }
      }
    }

    int stemEndIdx = pronStemLower.length() - 1;
    boolean endsWithVoiceless =
        stemEndIdx >= 0 && isVoicelessConsonant(pronStemLower.charAt(stemEndIdx));
    boolean startsDistributive = suffixAscii.startsWith("sar") || suffixAscii.startsWith("ser");

    char[] out = new char[suffixAscii.length()];
    for (int i = 0; i < suffixAscii.length(); i++) {
      char raw = fullOrig.charAt(suffixOffset + i);
      boolean pinned =
          pinDotlessI
              ? TurkishTextCodec.isPinnedTurkishDiacritic(raw)
              : TurkishTextCodec.isNonDotlessTurkishDiacritic(raw);
      if (pinned) {
        char tl = TurkishTextCodec.toTurkishLower(raw);
        out[i] = tl;
        if (TurkishTextCodec.isTurkishVowel(tl)) {
          front = TurkishTextCodec.isFrontVowel(tl);
          rounded = TurkishTextCodec.isRoundedVowel(tl);
        }
        continue;
      }

      char c = suffixAscii.charAt(i);
      if (i == 0 && startsDistributive && c == 's') {
        out[i] = 'ş';
      } else if (i == 0 && endsWithVoiceless && c == 'c') {
        out[i] = 'ç';
      } else if (c == 'e') {
        front = true;
        rounded = false;
        out[i] = 'e';
      } else if (c == 'a') {
        front = false;
        rounded = false;
        out[i] = 'a';
      } else if ((c == 'i' || c == 'u') && isInvariantKiAt(suffixAscii, i)) {
        front = true;
        rounded = false;
        out[i] = 'i';
      } else if (c == 'i' || c == 'u') {
        char hv = TurkishTextCodec.highHarmonicVowel(front, rounded);
        out[i] = hv;
        rounded = TurkishTextCodec.isRoundedVowel(hv);
      } else if (c == 's' && i >= 2 && out[i - 1] != 's' && isEvidentialMisBefore(out, i)) {
        out[i] = 'ş';
      } else {
        out[i] = c;
      }
    }
    return new String(out);
  }

  private static boolean isInvariantKiAt(String suffixAscii, int pos) {
    if (pos == 0 || suffixAscii.charAt(pos) != 'i' || suffixAscii.charAt(pos - 1) != 'k') {
      return false;
    }
    boolean precededByLocOrGen =
        (pos >= 3 && (suffixAscii.startsWith("da", pos - 3)
            || suffixAscii.startsWith("de", pos - 3)
            || suffixAscii.startsWith("ta", pos - 3)
            || suffixAscii.startsWith("te", pos - 3)
            || suffixAscii.startsWith("ni", pos - 3)
            || suffixAscii.startsWith("nu", pos - 3)))
            || (pos == 2 && (suffixAscii.startsWith("in") || suffixAscii.startsWith("un")));
    if (!precededByLocOrGen) {
      return false;
    }
    int rem = suffixAscii.length() - (pos + 1);
    return rem == 0
        || suffixAscii.startsWith("ler", pos + 1)
        || suffixAscii.startsWith("ni", pos + 1)
        || suffixAscii.startsWith("ne", pos + 1)
        || suffixAscii.startsWith("nde", pos + 1);
  }

  private static boolean isEvidentialMisBefore(char[] out, int pos) {
    if (pos < 2 || out[pos - 2] != 'm') {
      return false;
    }
    char v = out[pos - 1];
    return v == 'ı' || v == 'i' || v == 'u' || v == 'ü';
  }

  private static boolean isVoicelessConsonant(char c) {
    return c == 'f' || c == 's' || c == 't' || c == 'k' || c == 'ç' || c == 'ş' || c == 'h' || c == 'p';
  }

  private static boolean endsWithSilentEnglishE(String stemLower) {
    int n = stemLower.length();
    return n >= 4 && stemLower.charAt(n - 1) == 'e' && stemLower.charAt(n - 2) == 'l';
  }

  private static boolean endsWithDigit(String s) {
    return !s.isEmpty() && Character.isDigit(s.charAt(s.length() - 1));
  }

  static boolean isSentenceMajorityCapitalized(List<Token> tokens) {
    int total = 0;
    int caps = 0;
    for (Token t : tokens) {
      if (t.immutable()) {
        continue;
      }
      total++;
      if (t.casing() == Casing.TITLE || t.casing() == Casing.UPPER) {
        caps++;
      }
    }
    return total >= 3 && caps * 2 > total;
  }

  private static NimbleCandidate[] singleCandidate(
      String surface, String asciiLower, int foreignStemLen, boolean oovFallback) {
    return new NimbleCandidate[] {
      NimbleCandidate.create(surface, asciiLower, foreignStemLen, 0.0f, 0.0f, false, oovFallback)
    };
  }

  private static Token unwrapDotlessIPin(Token tok) {
    if (!tok.hasPinnedTurkish()) {
      return tok;
    }
    if (TurkishTextCodec.containsNonDotlessTurkishDiacritic(tok.original())) {
      return tok;
    }
    return new Token(
        tok.start(),
        tok.end(),
        tok.original(),
        tok.asciiLower(),
        tok.casing(),
        tok.apostropheIndex(),
        tok.numeralApostrophe(),
        tok.immutable(),
        tok.sentenceStart(),
        tok.rightPunct(),
        false);
  }

  static String projectCasingAndPins(
      Token token, String candLower, int foreignStemLength, boolean pinDotlessI) {
    String orig = token.original();
    if (token.casing() == Casing.LOWER && !token.hasPinnedTurkish() && foreignStemLength == 0) {
      return candLower;
    }
    if (orig.length() != candLower.length()) {
      return orig;
    }
    char[] out = new char[orig.length()];
    for (int i = 0; i < orig.length(); i++) {
      char o = orig.charAt(i);
      boolean pinned =
          token.hasPinnedTurkish()
              && (pinDotlessI
                  ? TurkishTextCodec.isPinnedTurkishDiacritic(o)
                  : TurkishTextCodec.isNonDotlessTurkishDiacritic(o));
      if (TurkishTextCodec.isApostrophe(o)
          || !TurkishTextCodec.isLetter(o)
          || pinned
          || i < foreignStemLength) {
        out[i] = o;
      } else {
        char cl = candLower.charAt(i);
        if (TurkishTextCodec.toAsciiLower(o) != TurkishTextCodec.toAsciiLower(cl)) {
          out[i] = o;
        } else {
          out[i] = Character.isUpperCase(o) ? TurkishTextCodec.toTurkishUpper(cl) : cl;
        }
      }
    }
    return new String(out);
  }
}
