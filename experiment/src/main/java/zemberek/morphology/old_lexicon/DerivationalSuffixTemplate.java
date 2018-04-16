package zemberek.morphology.old_lexicon;

import zemberek.morphology.old_lexicon.graph.TerminationType;

public class DerivationalSuffixTemplate extends SuffixFormTemplate {

  public DerivationalSuffixTemplate(int index, String id, Suffix suffix, TerminationType type) {
    super(index, id, suffix, type);
  }

  public DerivationalSuffixTemplate(int index, String id, Suffix suffix) {
    super(index, id, suffix);
  }
}
