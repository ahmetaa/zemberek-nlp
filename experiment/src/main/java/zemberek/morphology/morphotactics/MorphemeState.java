package zemberek.morphology.morphotactics;

import java.util.ArrayList;
import java.util.List;
import zemberek.core.logging.Log;

public class MorphemeState {

  public final String id;
  public final Morpheme morpheme;
  List<MorphemeTransition> outgoing = new ArrayList<>(2);
  List<MorphemeTransition> incoming = new ArrayList<>(2);
  public boolean terminal = false;
  public boolean derivative = false;

  public MorphemeState(String id, Morpheme morpheme, boolean terminal, boolean derivative) {
    this.morpheme = morpheme;
    this.id = id;
    this.terminal = terminal;
    this.derivative = derivative;
  }

  public static MorphemeState terminal(String id, Morpheme morpheme) {
    return new MorphemeState(id, morpheme, true, false);
  }

  public static MorphemeState nonTerminal(String id, Morpheme morpheme) {
    return new MorphemeState(id, morpheme, false, false);
  }

  public static MorphemeState terminalDerivative(String id, Morpheme morpheme) {
    return new MorphemeState(id, morpheme, true, true);
  }

  public static MorphemeState nonTerminalDerivative(String id, Morpheme morpheme) {
    return new MorphemeState(id, morpheme, false, true);
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
