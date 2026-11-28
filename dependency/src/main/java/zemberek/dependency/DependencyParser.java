package zemberek.dependency;

import zemberek.morphology.analysis.SentenceAnalysis;
import zemberek.morphology.analysis.format.ConlluSentence;

/**
 * Contract for Turkish Universal Dependencies v2 syntactic dependency parsers.
 */
public interface DependencyParser {

  /** Tokenizes, analyzes, disambiguates, and parses a raw Turkish sentence. */
  DependencyTree parse(String sentence);

  /** Projects a pre-disambiguated {@link SentenceAnalysis} to UD v2 and parses its dependencies. */
  DependencyTree parse(SentenceAnalysis analysis);

  /**
   * Parses syntactic dependencies directly over an already-projected {@link ConlluSentence}
   * (useful for gold-tokenization evaluation or custom CoNLL-U pipelines).
   */
  DependencyTree parse(ConlluSentence projected);
}
