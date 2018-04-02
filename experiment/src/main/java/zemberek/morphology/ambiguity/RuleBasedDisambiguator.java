package zemberek.morphology.ambiguity;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import zemberek.core.turkish.PrimaryPos;
import zemberek.morphology._analyzer._SentenceAnalysis;
import zemberek.morphology._analyzer._SingleAnalysis;
import zemberek.morphology._analyzer._TurkishMorphologicalAnalyzer;
import zemberek.morphology._analyzer._WordAnalysis;

// Used for collecting training data.

public class RuleBasedDisambiguator {

  _TurkishMorphologicalAnalyzer analyzer;

  public RuleBasedDisambiguator(_TurkishMorphologicalAnalyzer analyzer) {
    this.analyzer = analyzer;
  }

  public ResultSentence disambiguate(String sentence) {
    _SentenceAnalysis ambiguous = analyzer.analyzeSentence(sentence);
    ResultSentence s = new ResultSentence(sentence, ambiguous);
    s.makeDecisions();
    return s;
  }

  public ResultSentence noDisambiguation(String sentence) {
    _SentenceAnalysis ambiguous = analyzer.analyzeSentence(sentence);
    return new ResultSentence(sentence, ambiguous);
  }

  static class ResultSentence {

    String sentence;
    _SentenceAnalysis sentenceAnalysis;
    List<AmbiguityAnalysis> results;

    public ResultSentence(String sentence, _SentenceAnalysis sentenceAnalysis) {
      this.sentence = sentence;
      this.sentenceAnalysis = sentenceAnalysis;
      results = new ArrayList<>();
      for (_WordAnalysis analysis : sentenceAnalysis) {
        results.add(new AmbiguityAnalysis(analysis));
      }
    }

    public void makeDecisions() {
      for (int i = 0; i < results.size(); i++) {
        AmbiguityAnalysis a = results.get(i);

        if (a.choices.size() == 0) {
          continue;
        }

        AnalysisDecision first = a.choices.get(0);
        AnalysisDecision second = a.choices.size() > 1 ? a.choices.get(1) : null;

        if (a.choices.size() == 2) {
          ignoreOne(first, second, pairLexRules);
          oneProper(first, second, i == 0, a.input);
          bothProper(first, second, bothProperRules);
        }

        AmbiguityAnalysis begin = i == 0 ? null : results.get(i - 1);
        AmbiguityAnalysis next = i == results.size() - 1 ? null : results.get(i + 1);

        if (next == null
            || next.checkPos(PrimaryPos.Punctuation)
            || next.checkPos(PrimaryPos.Question)
            || next.checkPos(PrimaryPos.Conjunction)) {
          if (a.choices.size() == 2) {
            ignoreOne(first, second, endPairLexRules);
          }
        }

        if (begin == null) {
          if (a.choices.size() == 2) {
            ignoreOne(first, second, beginPairLexRules);
          }
        }

      }
    }
  }

  public static class AmbigiousWord {

    AmbiguityAnalysis previous;
    AmbiguityAnalysis current;
    AmbiguityAnalysis next;

    public AmbigiousWord(_WordAnalysis previous, _WordAnalysis current, _WordAnalysis next) {
      this.previous = new AmbiguityAnalysis(previous);
      this.current = new AmbiguityAnalysis(current);
      this.next = new AmbiguityAnalysis(next);
    }
  }

  public static class AmbiguityAnalysis {

    String input;
    List<AnalysisDecision> choices = new ArrayList<>();

    AmbiguityAnalysis(_WordAnalysis wordAnalysis) {
      this.input = wordAnalysis.getInput();
      for (_SingleAnalysis analysis : wordAnalysis) {
        choices.add(new AnalysisDecision(input, analysis, Decision.UNDECIDED));
      }
    }

    boolean checkPos(PrimaryPos pos) {
      return choices.size() == 1 && choices.get(0).analysis.getItem().primaryPos.equals(pos);
    }

  }

  enum Decision {
    IGNORE,
    SELECT,
    UNDECIDED
  }

  public static class AnalysisDecision {

    String input;
    _SingleAnalysis analysis;

    Decision decision;

    AnalysisDecision(String input, _SingleAnalysis analysis, Decision decision) {
      this.input = input;
      this.analysis = analysis;
      this.decision = decision;
    }
  }

  private static class PairRule {

    String toIgnore;
    String toLeave;

    public PairRule(String toIgnore, String toLeave) {
      this.toIgnore = toIgnore;
      this.toLeave = toLeave;
    }
  }

  static List<PairRule> pairLexRules = Lists.newArrayList(
      new PairRule(
          "Noun+A3pl+Pnon+Nom|Zero→Verb+Pres+A1sg",
          "Noun+A3pl+P1sg+Nom"),
      new PairRule(
          "Noun+A3pl+Pnon+Nom|Zero→Verb+Pres+A3sg",
          "Noun+A3pl+Pnon+Nom"),
      new PairRule(
          "Noun+A3pl+Pnon+Nom|Zero→Verb+Pres+A2sg",
          "Noun+A3pl+P2sg+Nom"),
      new PairRule(
          "Noun+A3sg+Pnon+Nom|Zero→Verb+Pres+A3pl",
          "Noun+A3pl+Pnon+Nom"),
      new PairRule(
          "[şûra:Noun]",
          "[şura:Noun]"),
      new PairRule(
          "[merhaba:Noun]",
          "[merhaba:Interj]"),
      new PairRule(
          "[ben:Noun]",
          "[ben:Pron,Pers]"),
      new PairRule(
          "[konmak:Verb]",
          "[konuşmak:Verb]"),
      new PairRule(
          "[Burada:Noun,Prop]",
          "[bura:Noun]"),
      new PairRule(
          "[ama:Conj]",
          "[am:Noun]"),
      new PairRule(
          "[Bacak:Noun,Prop]",
          "[bacak:Noun]"),
      new PairRule(
          "[görmek:Verb]",
          "[görüşmek:Verb]"),
      new PairRule(
          "[defa:Noun]",
          "[defalarca:Adv]"),
      new PairRule(
          "[ister:Noun]",
          "[istemek:Verb]"),
      new PairRule( // yapmaya ..
          "Neg+Opt",
          "Inf2→Noun"),
      new PairRule(
          "[iğrenç:Adv]",
          "[iğrenç:Adj]"),
      new PairRule(
          "[uymak:Verb]",
          "[uyanmak:Verb]"),
      new PairRule(
          "[bilemek:Verb]",
          "[bilmek:Verb]"),
      new PairRule(
          "[ölmek:Verb]",
          "[öldürmek:Verb]")

  );

  static List<PairRule> endPairLexRules = Lists.newArrayList(
      new PairRule(
          "FutPart→Noun",
          "Fut+"),
      new PairRule(
          "FutPart→Adj",
          "Fut+"),
      new PairRule(
          "AorPart→Noun",
          "Aor+"),
      new PairRule(
          "AorPart→Adj",
          "Aor+"),
      new PairRule(
          "NarrPart→Noun",
          "Narr+"),
      new PairRule(
          "NarrPart→Adj",
          "Narr+"),
      new PairRule(
          "Inf2→Noun",
          "Neg+"),
      new PairRule(
          "Inf2→Noun",
          "Neces+"),
      new PairRule(
          "[demek:Adv]",
          "[demek:Verb]"),
      //TODO: false with değil
      new PairRule(
          ":Noun",
          ":Verb"
      )
  );

  static List<PairRule> beginPairLexRules = Lists.newArrayList(
      new PairRule(
          "P2sg+Gen",
          "Pnon+Gen"),
      new PairRule(
          "Nom",
          "Acc")

  );


  static List<PairRule> bothProperRules = Lists.newArrayList(
      new PairRule(
          "Nom",
          "Acc")

  );

  static void ignoreOne(AnalysisDecision a1, AnalysisDecision a2, List<PairRule> rulez) {
    String lex1 = a1.analysis.formatLexical();
    String lex2 = a2.analysis.formatLexical();

    for (PairRule pairRule : rulez) {
      if (lex1.contains(pairRule.toIgnore) && lex2.contains(pairRule.toLeave)) {
        a1.decision = Decision.IGNORE;
      }
      if (lex2.contains(pairRule.toIgnore) && lex1.contains(pairRule.toLeave)) {
        a2.decision = Decision.IGNORE;
      }
    }
  }

  static void bothProper(AnalysisDecision a1, AnalysisDecision a2, List<PairRule> rulez) {

    String lex1 = a1.analysis.formatLexical();
    String lex2 = a2.analysis.formatLexical();

    if (!lex1.contains("Prop") || !lex2.contains("Prop")) {
      return;
    }

    for (PairRule pairRule : rulez) {
      if (lex1.contains(pairRule.toIgnore) && lex2.contains(pairRule.toLeave)) {
        a1.decision = Decision.IGNORE;
      }
      if (lex2.contains(pairRule.toIgnore) && lex1.contains(pairRule.toLeave)) {
        a2.decision = Decision.IGNORE;
      }
    }
  }

  static void oneProper(
      AnalysisDecision a1,
      AnalysisDecision a2,
      boolean first,
      String input) {
    String lex1 = a1.analysis.formatLexical();
    String lex2 = a2.analysis.formatLexical();

    if (lex1.contains("Prop]") && !lex2.contains("Prop]")) {
      if ((!first && Character.isUpperCase(input.charAt(0))) || input.contains("'")) {
        a2.decision = Decision.IGNORE;
      }
      if (Character.isLowerCase(input.charAt(0)) && !input.contains("'")) {
        a1.decision = Decision.IGNORE;
      }
      if (containsAny(lex1, possesion)) {
        a1.decision = Decision.IGNORE;
      }
    }

    if (lex2.contains("Prop]") && !lex1.contains("Prop]")) {
      if ((!first && Character.isUpperCase(input.charAt(0))) || input.contains("'")) {
        a1.decision = Decision.IGNORE;
      }

      if (Character.isLowerCase(input.charAt(0)) && !input.contains("'")) {
        a2.decision = Decision.IGNORE;
      }

      if (containsAny(lex2, possesion)) {
        a2.decision = Decision.IGNORE;
      }

    }

  }

  static Set<String> possesion = Sets.newHashSet("P1sg", "P2sg", "P1pl", "P2pl", "P3pl");

  static boolean containsAny(String in, Set<String> set) {
    for (String s : set) {
      if (in.contains(s)) {
        return true;
      }
    }
    return false;
  }

}


