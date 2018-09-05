package zemberek.morphology.morphotactics;

import java.util.ArrayList;
import java.util.List;
import zemberek.core.logging.Log;

/**
 * Represents a state in morphotactics graph.
 */
public class MorphemeState {

  public final String id;
  public final Morpheme morpheme;
  private List<MorphemeTransition> outgoing = new ArrayList<>(2);
  private List<MorphemeTransition> incoming = new ArrayList<>(2);
  public final boolean terminal;
  public final boolean derivative;
  public final boolean posRoot;

  MorphemeState(
      String id,
      Morpheme morpheme,
      boolean terminal,
      boolean derivative,
      boolean posRoot) {
    this.morpheme = morpheme;
    this.id = id;
    this.terminal = terminal;
    this.derivative = derivative;
    this.posRoot = posRoot;
  }

  public static Builder builder(String id, Morpheme morpheme) {
    return new Builder(id, morpheme);
  }

  static class Builder {

    final String _id;
    final Morpheme _morpheme;
    private boolean _terminal = false;
    private boolean _derivative = false;
    private boolean _posRoot = false;

    public Builder(String _id, Morpheme _morpheme) {
      this._id = _id;
      this._morpheme = _morpheme;
    }

    public Builder terminal() {
      this._terminal = true;
      return this;
    }

    public Builder derivative() {
      this._derivative = true;
      return this;
    }

    public Builder posRoot() {
      this._posRoot = true;
      return this;
    }

    public MorphemeState build() {
      return new MorphemeState(_id, _morpheme, _terminal, _derivative, _posRoot);
    }
  }

  public static MorphemeState terminal(String id, Morpheme morpheme) {
    return builder(id, morpheme).terminal().build();
  }


  public static MorphemeState nonTerminal(String id, Morpheme morpheme) {
    return builder(id, morpheme).build();
  }


  public static MorphemeState terminalDerivative(String id, Morpheme morpheme) {
    return builder(id, morpheme).terminal().derivative().build();
  }

  public static MorphemeState nonTerminalDerivative(String id, Morpheme morpheme) {
    return builder(id, morpheme).derivative().build();
  }


  public MorphemeState addOutgoing(MorphemeTransition... suffixTransitions) {
    for (MorphemeTransition suffixTransition : suffixTransitions) {
      if (outgoing.contains(suffixTransition)) {
        Log.warn("Outgoing transition %s already exist in %s", suffixTransition, this);
      }
      outgoing.add(suffixTransition);
    }
    return this;
  }

  public MorphemeState addIncoming(MorphemeTransition... suffixTransitions) {
    for (MorphemeTransition suffixTransition : suffixTransitions) {
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

  public MorphemeState add(MorphemeState to, String template, Condition condition) {
    new SuffixTransition.Builder().surfaceTemplate(template)
        .setCondition(condition)
        .from(this).to(to).build();
    return this;
  }

  public MorphemeState addEmpty(MorphemeState to, Condition condition) {
    new SuffixTransition.Builder().setCondition(condition)
        .from(this).to(to).build();
    return this;
  }

  public MorphemeState add(MorphemeState to, String template) {
    new SuffixTransition.Builder().surfaceTemplate(template)
        .from(this).to(to).build();
    return this;
  }

  public MorphemeState addEmpty(MorphemeState to) {
    new SuffixTransition.Builder().from(this).to(to).build();
    return this;
  }

  public void dumpTransitions() {
    for (MorphemeTransition transition : outgoing) {
      System.out.println(transition.condition);
    }
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

  public void copyOutgoingTransitionsFrom(MorphemeState state) {
    for (MorphemeTransition transition : state.outgoing) {
      MorphemeTransition copy = transition.getCopy();
      copy.from = this;
      this.addOutgoing(transition);
    }
  }

  public void removeTransitionsTo(MorphemeState state) {
    List<MorphemeTransition> transitions = new ArrayList<>(2);
    for (MorphemeTransition transition : outgoing) {
      if (transition.to.equals(state)) {
        transitions.add(transition);
      }
    }
    outgoing.removeAll(transitions);
  }

  public void removeTransitionsTo(Morpheme morpheme) {
    List<MorphemeTransition> transitions = new ArrayList<>(2);
    for (MorphemeTransition transition : outgoing) {
      if (transition.to.morpheme.equals(morpheme)) {
        transitions.add(transition);
      }
    }
    outgoing.removeAll(transitions);
  }

  public void removeTransitionsTo(MorphemeState... state) {
    for (MorphemeState morphemeState : state) {
      removeTransitionsTo(morphemeState);
    }
  }

  public void removeTransitionsTo(Morpheme... morphemes) {
    for (Morpheme morpheme : morphemes) {
      removeTransitionsTo(morpheme);
    }
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    MorphemeState that = (MorphemeState) o;
    return id.equals(that.id);
  }

  @Override
  public int hashCode() {
    return id.hashCode();
  }
}
