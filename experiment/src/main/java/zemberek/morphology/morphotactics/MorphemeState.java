package zemberek.morphology.morphotactics;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import zemberek.core.logging.Log;

public class MorphemeState {

  public final String id;
  public final Morpheme morpheme;
  List<MorphemeTransition> outgoing = new ArrayList<>(2);
  List<MorphemeTransition> incoming = new ArrayList<>(2);
  public final boolean terminal;
  public final boolean derivative;
  public final boolean posRoot;

  public MorphemeState(
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

  public void addOutgoingTransitions(MorphemeState state) {
    this.outgoing.addAll(state.outgoing);
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
    return Objects.equals(id, that.id);
  }

  @Override
  public int hashCode() {

    return Objects.hash(id);
  }
}
