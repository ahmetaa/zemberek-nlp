package zemberek.morphology.morphotactics;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import zemberek.core.logging.Log;

public class MorphemeState {

  public final Morpheme morpheme;
  public final String id;
  List<MorphemeTransition> outgoing = new ArrayList<>(2);
  List<MorphemeTransition> incoming = new ArrayList<>(2);
  public boolean terminal;

  public MorphemeState(String id, Morpheme morpheme, boolean terminal) {
    this.morpheme = morpheme;
    this.id = id;
    this.terminal = terminal;
  }

  public static MorphemeState terminal(String id, Morpheme morpheme) {
    return new MorphemeState(id, morpheme, true);
  }

  public static MorphemeState nonTerminal(String id, Morpheme morpheme) {
    return new MorphemeState(id, morpheme, false);
  }

  public MorphemeState addOutgoing(SuffixTransition... suffixTransitions) {
    for (SuffixTransition suffixTransition : suffixTransitions) {
      if (outgoing.contains(suffixTransition)) {
        Log.warn("Outgoing transition %s already exist in %s", suffixTransition, this);
      }
      outgoing.add(suffixTransition);
    }
    return this;
  }

  public MorphemeState addIncoming(SuffixTransition... suffixTransitions) {
    for (SuffixTransition suffixTransition : suffixTransitions) {
      if (incoming.contains(suffixTransition)) {
        Log.warn("Incoming transition %s already exist in %s", suffixTransition, this);
      }
      incoming.add(suffixTransition);
    }
    return this;
  }

  public SuffixTransition.Builder transition(MorphemeState to) {
    return new SuffixTransition.Builder().from(this).to(to);
  }


  public SuffixTransition.Builder transition(MorphemeState to, String template) {
    return new SuffixTransition.Builder().surfaceTemplate(template).from(this).to(to);
  }

  @Override
  public String toString() {
    return "[" + id + ":" + morpheme.id + "]";
  }

  public List<MorphemeTransition> getOutgoing() {
    return outgoing;
  }

  public List<MorphemeTransition> getIncoming() {
    return incoming;
  }
}
