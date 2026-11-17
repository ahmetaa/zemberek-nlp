package zemberek.normalization.v2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import zemberek.core.turkish.TurkishAlphabet;
import zemberek.normalization.v2.NormV2Types.SpanToken;
import zemberek.normalization.v2.NormalizationKnowledge.CasingCounts;
import zemberek.normalization.v2.SpanCandidateGenerator.SlotDraftBuilder;

/**
 * Expands conversational chat shorthands ({@code slm -> selam}, {@code tsk -> teşekkür}),
 * compositional shorthand+suffix forms ({@code arkdslr -> arkadaşlar}), and dropped-vowel words
 * ({@code >=5} consonants) via consonant-skeleton lookup.
 */
final class ShorthandExpander {

  private static final Map<String, String> CURATED_CHAT_SHORTHANDS = buildCuratedChatShorthands();

  /** Insertion-ordered so longer prefixes ({@code "tskr"}) match before shorter ones ({@code "tsk"}). */
  private static final Map<String, String[]> COMPOSITIONAL_SHORTHAND_STEMS =
      buildCompositionalShorthandStems();

  private static final Map<String, String[]> VOWELLESS_SUFFIX_EXPANSIONS =
      buildVowellessSuffixExpansions();

  private static final Set<String> VOWELED_SHORTHAND_SUFFIXES =
      Set.of(
          "ler", "lar", "lerde", "larda", "lerden", "lardan", "leri", "ları", "lerin", "ların",
          "lerim", "larım", "lerimiz", "larımız", "im", "ım", "um", "üm", "imiz", "ımız", "umuz",
          "ümüz", "in", "ın", "un", "ün", "iniz", "ınız", "unuz", "ünüz", "dir", "dır", "dur",
          "dür", "tir", "tır", "tur", "tür", "da", "de", "ta", "te", "dan", "den", "tan", "ten",
          "a", "e", "ya", "ye", "ı", "i", "u", "ü", "yı", "yi", "yu", "yü", "la", "le", "yla",
          "yle", "sın", "sin", "sun", "sün");

  private final NormalizationKnowledge knowledge;
  private final LatticeMorphologySupport morph;
  private final Map<String, String[]> consonantSkeletonIndex;

  ShorthandExpander(NormalizationKnowledge knowledge, LatticeMorphologySupport morph) {
    this.knowledge = knowledge;
    this.morph = morph;
    this.consonantSkeletonIndex = buildConsonantSkeletonIndex(knowledge, morph);
  }

  static boolean isCuratedShorthand(String lower) {
    return CURATED_CHAT_SHORTHANDS.containsKey(lower);
  }

  void addCandidates(SlotDraftBuilder slot) {
    SpanToken tok = slot.tok;
    String low = tok.lower();
    if (low.length() < 2 || low.length() > 16 || low.indexOf('\'') >= 0) {
      return;
    }

    String sqTrail = SingleTokenRewriteGenerator.squeezeTrailingRun(low);
    String sqAll = SingleTokenRewriteGenerator.squeezeRuns(low, 1);
    String[] lookupKeys = distinctLookupKeys(low, sqTrail, sqAll);
    String baseQuery = sqTrail.length() >= 3 ? sqTrail : low;

    addExactShorthands(slot, low, lookupKeys);
    addCompositionalShorthands(slot, baseQuery);
    addConsonantSkeletonMatches(slot, low, baseQuery);
  }

  private static String[] distinctLookupKeys(String low, String sqTrail, String sqAll) {
    if (sqTrail.equals(low)) {
      return new String[] {low};
    }
    return sqAll.equals(sqTrail)
        ? new String[] {low, sqTrail}
        : new String[] {low, sqTrail, sqAll};
  }

  /** 1. Exact curated conversational shorthands & common chat vowel-drops. */
  private static void addExactShorthands(SlotDraftBuilder slot, String low, String[] lookupKeys) {
    for (String key : lookupKeys) {
      String exactTarget = CURATED_CHAT_SHORTHANDS.get(key);
      if (exactTarget != null && !exactTarget.equals(low)) {
        int morphOrSplitFlag =
            exactTarget.indexOf(' ') >= 0
                ? NormV2Types.SRC_CLITIC_SPLIT
                : NormV2Types.SRC_FORMAL_MORPH;
        int elongFlag = !key.equals(low) ? NormV2Types.SRC_ELONGATION : 0;
        emitShorthand(
            slot, exactTarget, NormV2Types.SRC_SHORTHAND | morphOrSplitFlag | elongFlag, 0.94f);
      }
    }
  }

  /** 2. Compositional {@code [Shorthand Stem + Productive Morphotactic Suffix]}. */
  private void addCompositionalShorthands(SlotDraftBuilder slot, String baseQuery) {
    for (Map.Entry<String, String[]> entry : COMPOSITIONAL_SHORTHAND_STEMS.entrySet()) {
      String shortStem = entry.getKey();
      if (baseQuery.length() <= shortStem.length() || !baseQuery.startsWith(shortStem)) {
        continue;
      }
      String rawTail = baseQuery.substring(shortStem.length());
      String[] candTails = resolveSuffixExpansions(rawTail);
      if (candTails == null) {
        continue;
      }
      for (String expandedStem : entry.getValue()) {
        emitExpandedStemWithTails(slot, shortStem, expandedStem, candTails);
      }
    }
  }

  private static String[] resolveSuffixExpansions(String rawTail) {
    if (rawTail.length() > 10) {
      return null;
    }
    boolean productiveVoweledChain =
        rawTail.length() >= 2
            && isPureTurkishAlpha(rawTail)
            && asciiConsonantSkeleton(rawTail, false).length() < rawTail.length();
    if (VOWELED_SHORTHAND_SUFFIXES.contains(rawTail) || productiveVoweledChain) {
      return new String[] {rawTail};
    }
    return VOWELLESS_SUFFIX_EXPANSIONS.get(rawTail);
  }

  private void emitExpandedStemWithTails(
      SlotDraftBuilder slot, String shortStem, String expandedStem, String[] candTails) {
    String stemPrefix =
        expandedStem.length() > 2
            ? expandedStem.substring(0, expandedStem.length() - 1)
            : expandedStem;
    for (String vTail : candTails) {
      String realized =
          "merhaba".equals(expandedStem) && ("lar".equals(vTail) || "ler".equals(vTail))
              ? "merhabalar"
              : morph.realizeValidWordOrNull(expandedStem + vTail);
      if (realized != null
          && realized.startsWith(stemPrefix)
          && ("merhabalar".equals(realized) || morph.hasRegularStrictAnalysis(realized))
          && !LatticeMorphologySupport.stripsExplicitStemDiacritic(shortStem, realized)) {
        emitShorthand(
            slot, realized, NormV2Types.SRC_SHORTHAND | NormV2Types.SRC_FORMAL_MORPH, 0.90f);
      }
    }
  }

  /** 3. Dropped-vowel consonant skeleton lookup ({@code >=5} consonants). */
  private void addConsonantSkeletonMatches(SlotDraftBuilder slot, String low, String baseQuery) {
    if (consonantSkeletonIndex.isEmpty()
        || !isPureTurkishAlpha(baseQuery)
        || baseQuery.length() < 5
        || baseQuery.length() > 14
        || knowledge.isKnownAcronymStem(low)
        || morph.isKnownStrictAbbreviation(low)
        || !isEligibleForSkeletonLookup(baseQuery)) {
      return;
    }

    String qSkel = asciiConsonantSkeleton(baseQuery, false);
    if (qSkel.length() < 4) {
      return;
    }
    String qSkelCollapsed = SingleTokenRewriteGenerator.squeezeRuns(qSkel, 1);
    String[] skelKeys =
        qSkelCollapsed.equals(qSkel) || qSkelCollapsed.length() < 4
            ? new String[] {qSkel}
            : new String[] {qSkel, qSkelCollapsed};

    String qAscii = NormV2Types.toAscii(baseQuery);
    char qFirstAscii = qAscii.charAt(0);
    int emitted = 0;
    for (String sk : skelKeys) {
      String[] cands = consonantSkeletonIndex.get(sk);
      if (cands == null) {
        continue;
      }
      for (String cand : cands) {
        if (isValidSkeletonCandidate(low, baseQuery, qAscii, qFirstAscii, cand)) {
          emitShorthand(
              slot, cand, NormV2Types.SRC_SHORTHAND | NormV2Types.SRC_FORMAL_MORPH, 0.80f);
          if (++emitted >= 3) {
            return;
          }
        }
      }
    }
  }

  private static boolean isEligibleForSkeletonLookup(String word) {
    int vowels = 0;
    int consonants = 0;
    int maxConsCluster = 0;
    int curConsCluster = 0;
    for (int i = 0; i < word.length(); i++) {
      if (TurkishAlphabet.INSTANCE.isVowel(word.charAt(i))) {
        vowels++;
        curConsCluster = 0;
      } else {
        consonants++;
        curConsCluster++;
        if (curConsCluster > maxConsCluster) {
          maxConsCluster = curConsCluster;
        }
      }
    }
    boolean startsWithVowel = TurkishAlphabet.INSTANCE.isVowel(word.charAt(0));
    return (vowels == 0 && consonants >= 5)
        || (startsWithVowel && vowels == 1 && consonants >= 4)
        || (vowels <= 2 && consonants >= 5 && maxConsCluster >= 4)
        || (!startsWithVowel && vowels == 1 && consonants >= 5 && maxConsCluster >= 3);
  }

  private static boolean isValidSkeletonCandidate(
      String rawLow, String baseQuery, String qAscii, char qFirstAscii, String cand) {
    if (cand.equals(rawLow)
        || cand.length() < baseQuery.length() + 1
        || cand.length() > baseQuery.length() + 6) {
      return false;
    }
    String cAscii = NormV2Types.toAscii(cand);
    return cAscii.charAt(0) == qFirstAscii
        && isOrderedSubsequence(qAscii, cAscii)
        && !contradictsExplicitConsonantDiacritic(baseQuery, cand);
  }

  private static void emitShorthand(
      SlotDraftBuilder slot, String targetLowerOrCased, int srcMask, float precision) {
    String rawLow = slot.tok.lower();
    String normLow = NormV2Types.trLower(targetLowerOrCased);
    String surf =
        targetLowerOrCased.indexOf(' ') >= 0
            ? formatMultiWordShorthand(slot, targetLowerOrCased)
            : slot.truecase(normLow);
    slot.addRule(
        surf,
        normLow,
        srcMask,
        precision,
        SpanCandidateGenerator.computeWeightedEditCost(rawLow, normLow),
        SpanCandidateGenerator.countDiacriticDiffs(rawLow, normLow));
  }

  private static String formatMultiWordShorthand(SlotDraftBuilder slot, String target) {
    if (slot.sentenceAllCaps) {
      return NormV2Types.trUpper(target);
    }
    if (Character.isUpperCase(target.charAt(0))) {
      return target;
    }
    return slot.capHere ? NormV2Types.trCapitalize(target) : target;
  }

  private static Map<String, String[]> buildConsonantSkeletonIndex(
      NormalizationKnowledge knowledge, LatticeMorphologySupport morph) {
    if (knowledge == null || knowledge.cleanLm == null) {
      return Map.of();
    }
    List<String> topWords =
        !knowledge.shorthandWords.isEmpty()
            ? knowledge.shorthandWords
            : rankShorthandWordsFromLexicon(knowledge, morph);
    if (topWords.isEmpty()) {
      return Map.of();
    }

    Map<String, List<String>> tmp = new HashMap<>(16_384);
    for (String w : topWords) {
      String skExact = asciiConsonantSkeleton(w, false);
      String skNoSoftG = asciiConsonantSkeleton(w, true);
      String skCollapsed = SingleTokenRewriteGenerator.squeezeRuns(skExact, 1);
      for (String key : new String[] {skExact, skNoSoftG, skCollapsed}) {
        if (key.length() >= 4) {
          List<String> bucket = tmp.computeIfAbsent(key, k -> new ArrayList<>(4));
          if (bucket.size() < 4 && !bucket.contains(w)) {
            bucket.add(w);
          }
        }
      }
    }
    Map<String, String[]> out = new HashMap<>(tmp.size() * 4 / 3 + 16);
    for (Map.Entry<String, List<String>> e : tmp.entrySet()) {
      out.put(e.getKey(), e.getValue().toArray(new String[0]));
    }
    return out;
  }

  private static List<String> rankShorthandWordsFromLexicon(
      NormalizationKnowledge knowledge, LatticeMorphologySupport morph) {
    if (knowledge.casingLexicon == null || knowledge.casingLexicon.isEmpty()) {
      return List.of();
    }
    record RankedWord(String word, float logProb) {}
    List<RankedWord> pool = new ArrayList<>(24_000);
    for (Map.Entry<String, CasingCounts> e : knowledge.casingLexicon.entrySet()) {
      String w = e.getKey();
      if (w.length() < 5 || w.length() > 16 || !isPureTurkishAlpha(w)) {
        continue;
      }
      CasingCounts cc = e.getValue();
      if (cc.lower < 5 || cc.lower <= (cc.title + cc.upper + cc.mixed) * 3) {
        continue;
      }
      if (knowledge.cleanLm.unigramCount(w) < 4
          || knowledge.isKnownProperOrAcronymStem(w)
          || morph.isKnownStrictAbbreviation(w)
          || !morph.hasRegularStrictAnalysis(w)
          || morph.isStrictProperOrAbbrOnly(w)) {
        continue;
      }
      pool.add(new RankedWord(w, knowledge.cleanLm.unigramLogProb(w)));
    }
    pool.sort((a, b) -> Float.compare(b.logProb(), a.logProb()));
    int limit = Math.min(12_000, pool.size());
    List<String> out = new ArrayList<>(limit);
    for (int i = 0; i < limit; i++) {
      out.add(pool.get(i).word());
    }
    return out;
  }

  private static boolean isPureTurkishAlpha(String s) {
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      if (!((c >= 'a' && c <= 'z' && c != 'q' && c != 'w' && c != 'x')
          || c == 'ç'
          || c == 'ğ'
          || c == 'ı'
          || c == 'ö'
          || c == 'ş'
          || c == 'ü')) {
        return false;
      }
    }
    return !s.isEmpty();
  }

  private static String asciiConsonantSkeleton(String lower, boolean dropSoftG) {
    StringBuilder sb = new StringBuilder(lower.length());
    for (int i = 0; i < lower.length(); i++) {
      char c = lower.charAt(i);
      if (TurkishAlphabet.INSTANCE.isVowel(c) || (dropSoftG && c == 'ğ')) {
        continue;
      }
      char asc = TurkishAlphabet.INSTANCE.getAsciiEqual(c);
      if (asc >= 'a' && asc <= 'z') {
        sb.append(asc);
      }
    }
    return sb.toString();
  }

  private static boolean isOrderedSubsequence(String sub, String full) {
    int n = sub.length();
    int m = full.length();
    if (n > m) {
      return false;
    }
    int i = 0;
    for (int j = 0; j < m && i < n; j++) {
      if (sub.charAt(i) == full.charAt(j)) {
        i++;
      }
    }
    return i == n;
  }

  private static boolean contradictsExplicitConsonantDiacritic(String rawLow, String candLow) {
    for (int i = 0; i < rawLow.length(); i++) {
      char c = rawLow.charAt(i);
      if ((c == 'ç' || c == 'ğ' || c == 'ş') && candLow.indexOf(c) < 0) {
        return true;
      }
    }
    return false;
  }

  private static Map<String, String> buildCuratedChatShorthands() {
    Map<String, String> m = new LinkedHashMap<>(128);
    // Greetings & farewells
    m.put("slm", "selam");
    m.put("slmlar", "selamlar");
    m.put("slmlr", "selamlar");
    m.put("mrb", "merhaba");
    m.put("mrhb", "merhaba");
    m.put("mrblar", "merhabalar");
    m.put("mrblr", "merhabalar");
    m.put("mrhblr", "merhabalar");
    m.put("nbr", "ne haber");
    m.put("hg", "hoş geldin");
    m.put("hb", "hoş bulduk");
    m.put("kib", "kendine iyi bak");
    m.put("aeo", "Allah'a emanet ol");
    m.put("grsrz", "görüşürüz");
    m.put("grşrz", "görüşürüz");
    m.put("grşz", "görüşürüz");
    m.put("grsz", "görüşürüz");
    m.put("görşrz", "görüşürüz");

    // Thanks, politeness & wishes
    m.put("tsk", "teşekkür");
    m.put("tşk", "teşekkür");
    m.put("tskr", "teşekkür");
    m.put("tşkr", "teşekkür");
    m.put("tskler", "teşekkürler");
    m.put("tşkler", "teşekkürler");
    m.put("tsklr", "teşekkürler");
    m.put("tşklr", "teşekkürler");
    m.put("tskrler", "teşekkürler");
    m.put("tşkrler", "teşekkürler");
    m.put("tskrlr", "teşekkürler");
    m.put("tşkrlr", "teşekkürler");
    m.put("ltfn", "lütfen");
    m.put("ltn", "lütfen");
    m.put("lütfn", "lütfen");
    m.put("lutfn", "lütfen");
    m.put("saol", "sağ ol");
    m.put("sagol", "sağ ol");
    m.put("saolun", "sağ olun");
    m.put("sagolun", "sağ olun");
    m.put("saolasın", "sağ olasın");
    m.put("sagolasin", "sağ olasın");
    m.put("eyw", "eyvallah");
    m.put("eyv", "eyvallah");
    m.put("eyvlh", "eyvallah");
    m.put("eywllh", "eyvallah");
    m.put("inş", "inşallah");
    m.put("ins", "inşallah");
    m.put("inşllh", "inşallah");
    m.put("insllh", "inşallah");
    m.put("mşllh", "maşallah");
    m.put("msllh", "maşallah");
    m.put("tbrk", "tebrik");
    m.put("tbrkler", "tebrikler");
    m.put("tbrklr", "tebrikler");
    m.put("syglr", "saygılar");
    m.put("svglr", "sevgiler");
    m.put("svgler", "sevgiler");

    // Conversational questions & responses
    m.put("nsl", "nasıl");
    m.put("nslsn", "nasılsın");
    m.put("nslsın", "nasılsın");
    m.put("naslsn", "nasılsın");
    m.put("nslsnz", "nasılsınız");
    m.put("naslsnz", "nasılsınız");
    m.put("tmm", "tamam");
    m.put("tmmdır", "tamamdır");
    m.put("tmmdir", "tamamdır");
    m.put("tmmdr", "tamamdır");
    m.put("evt", "evet");
    m.put("hyr", "hayır");
    m.put("hyrls", "hayırlısı");

    // Address terms & nouns
    m.put("cnm", "canım");
    m.put("cnmsın", "canımsın");
    m.put("cnmsin", "canımsın");
    m.put("knk", "kanka");
    m.put("knka", "kanka");
    m.put("knkm", "kankam");
    m.put("krdş", "kardeş");
    m.put("krds", "kardeş");
    m.put("krdşm", "kardeşim");
    m.put("krdsm", "kardeşim");
    m.put("krdşler", "kardeşler");
    m.put("krdslr", "kardeşler");
    m.put("arkdş", "arkadaş");
    m.put("arkds", "arkadaş");
    m.put("arkdşm", "arkadaşım");
    m.put("arkdsm", "arkadaşım");
    m.put("arkdslr", "arkadaşlar");
    m.put("arkdşlr", "arkadaşlar");
    m.put("arkadaslr", "arkadaşlar");
    m.put("arkadaşlr", "arkadaşlar");
    m.put("msj", "mesaj");
    m.put("msjlar", "mesajlar");
    m.put("msjlr", "mesajlar");
    m.put("cvp", "cevap");
    m.put("cvplar", "cevaplar");
    m.put("cvplr", "cevaplar");
    m.put("tlf", "telefon");
    m.put("tlfn", "telefon");
    m.put("yrdm", "yardım");

    // High-frequency conversational verbs, adverbs & pronouns
    m.put("blmyrm", "bilmiyorum");
    m.put("bilmyrm", "bilmiyorum");
    m.put("blmyrsn", "bilmiyorsun");
    m.put("svyrm", "seviyorum");
    m.put("sevyrm", "seviyorum");
    m.put("svmyrm", "sevmiyorum");
    m.put("istyrm", "istiyorum");
    m.put("istym", "istiyorum");
    m.put("istmyrm", "istemiyorum");
    m.put("glyrm", "geliyorum");
    m.put("gdyrm", "gidiyorum");
    m.put("bklm", "bakalım");
    m.put("bakalm", "bakalım");
    m.put("glcm", "geleceğim");
    m.put("glcem", "geleceğim");
    m.put("glmycm", "gelmeyeceğim");
    m.put("knşlm", "konuşalım");
    m.put("knslm", "konuşalım");
    m.put("ozldm", "özledim");
    m.put("özldm", "özledim");
    m.put("anlmdm", "anlamadım");
    m.put("anlmyrm", "anlamıyorum");
    m.put("unttm", "unuttum");
    m.put("dşnyrm", "düşünüyorum");
    m.put("dsnyrm", "düşünüyorum");
    m.put("yrn", "yarın");
    m.put("zmn", "zaman");
    m.put("gzl", "güzel");
    m.put("kdr", "kadar");
    m.put("ztn", "zaten");
    m.put("glb", "galiba");
    m.put("hrlde", "herhalde");
    m.put("heralde", "herhalde");
    m.put("fln", "falan");
    m.put("felan", "falan");
    m.put("deil", "değil");
    m.put("deıl", "değil");
    m.put("dgl", "değil");
    m.put("dğl", "değil");
    m.put("dglm", "değilim");
    m.put("dğlm", "değilim");
    m.put("deilim", "değilim");
    m.put("ii", "iyi");
    m.put("çk", "çok");
    m.put("bi", "bir");
    m.put("bı", "bir");
    m.put("bn", "ben");
    m.put("bnm", "benim");
    m.put("snn", "senin");
    m.put("bna", "bana");
    m.put("sna", "sana");
    m.put("bni", "beni");
    m.put("sni", "seni");
    m.put("bnde", "ben de");
    m.put("snde", "sen de");
    return Map.copyOf(m);
  }

  private static Map<String, String[]> buildCompositionalShorthandStems() {
    Map<String, String[]> m = new LinkedHashMap<>(32);
    m.put("tskr", new String[] {"teşekkür"});
    m.put("tşkr", new String[] {"teşekkür"});
    m.put("tsk", new String[] {"teşekkür"});
    m.put("tşk", new String[] {"teşekkür"});
    m.put("slm", new String[] {"selam"});
    m.put("mrhb", new String[] {"merhaba"});
    m.put("mrb", new String[] {"merhaba"});
    m.put("msj", new String[] {"mesaj"});
    m.put("cvp", new String[] {"cevap", "cevab"});
    m.put("tmm", new String[] {"tamam"});
    m.put("knka", new String[] {"kanka"});
    m.put("knk", new String[] {"kanka"});
    m.put("cnm", new String[] {"canım"});
    m.put("krdş", new String[] {"kardeş"});
    m.put("krds", new String[] {"kardeş"});
    m.put("arkdş", new String[] {"arkadaş"});
    m.put("arkds", new String[] {"arkadaş"});
    m.put("tlfn", new String[] {"telefon"});
    m.put("tlf", new String[] {"telefon"});
    m.put("tbrk", new String[] {"tebrik"});
    m.put("prblm", new String[] {"problem"});
    m.put("prgrm", new String[] {"program"});
    m.put("kmpny", new String[] {"kampanya"});
    m.put("blm", new String[] {"bölüm"});
    m.put("fyt", new String[] {"fiyat"});
    m.put("rsm", new String[] {"resim", "resm"});
    m.put("yrdm", new String[] {"yardım"});
    m.put("zmn", new String[] {"zaman"});
    m.put("insn", new String[] {"insan"});
    return Collections.unmodifiableMap(m);
  }

  private static Map<String, String[]> buildVowellessSuffixExpansions() {
    Map<String, String[]> m = new LinkedHashMap<>(24);
    m.put("lr", new String[] {"ler", "lar"});
    m.put("lrd", new String[] {"lerde", "larda"});
    m.put("lrdn", new String[] {"lerden", "lardan"});
    m.put("lrm", new String[] {"lerim", "larım"});
    m.put("lrmz", new String[] {"lerimiz", "larımız"});
    m.put("lrn", new String[] {"lerin", "ların", "lerini", "larını"});
    m.put("m", new String[] {"im", "ım", "um", "üm", "m"});
    m.put("mz", new String[] {"imiz", "ımız", "umuz", "ümüz", "miz", "mız"});
    m.put("n", new String[] {"in", "ın", "un", "ün", "n"});
    m.put("nz", new String[] {"iniz", "ınız", "unuz", "ünüz", "niz", "nız"});
    m.put("dr", new String[] {"dir", "dır", "dur", "dür", "tir", "tır", "tur", "tür"});
    m.put("dn", new String[] {"den", "dan", "ten", "tan"});
    m.put("d", new String[] {"de", "da", "te", "ta"});
    m.put("sn", new String[] {"sin", "sın", "sun", "sün"});
    return Collections.unmodifiableMap(m);
  }
}
