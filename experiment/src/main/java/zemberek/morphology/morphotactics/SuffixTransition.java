package zemberek.morphology.morphotactics;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import zemberek.core.logging.Log;
import zemberek.core.turkish.PhoneticExpectation;
import zemberek.core.turkish.TurkishAlphabet;
import zemberek.morphology.analyzer.MorphemeSurfaceForm.SuffixTemplateToken;
import zemberek.morphology.analyzer.MorphemeSurfaceForm.SuffixTemplateTokenizer;
import zemberek.morphology.analyzer.SearchPath;

public class SuffixTransition extends MorphemeTransition {

  // this string represents the possible surface forms for this transition.
  public final String surfaceTemplate;

  private List<SuffixTemplateToken> tokenList;

  // TODO: this can be a list.
  Set<Rule> rules;

  private SuffixTransition(Builder builder) {
    Preconditions.checkNotNull(builder.from);
    Preconditions.checkNotNull(builder.to);
    this.from = builder.from;
    this.to = builder.to;
    this.surfaceTemplate = builder.surfaceTemplate == null ? "" : builder.surfaceTemplate;
    this.rules = builder.rules;
    this.rules.addAll(rulesFromTemplate(this.surfaceTemplate));
    this.tokenList = Lists
        .newArrayList(new SuffixTemplateTokenizer(this.surfaceTemplate));

  }

  public boolean canPass(SearchPath path) {
    for (Rule rule : rules) {
      if (!rule.canPass(path)) {
        return false;
      }
    }
    return true;
  }

  private void connect() {
    from.addOutgoing(this);
    to.addIncoming(this);
  }

  private List<Rule> rulesFromTemplate(String template) {
    if (template == null || template.length() == 0) {
      return Collections.emptyList();
    }
    List<Rule> rules = new ArrayList<>(1);
    if (template.startsWith(">") || !TurkishAlphabet.INSTANCE.isVowel(template.charAt(0))) {
      rules.add(Rules.rejectIfContains(PhoneticExpectation.VowelStart));
    }
    return rules;
  }

  public Builder builder() {
    return new Builder();
  }

  public String toString() {
    return "[" + from.id + "->" + to.id + "|" + surfaceTemplate + "]";
  }

  public static class Builder {

    MorphemeState from;
    MorphemeState to;
    String surfaceTemplate;
    Set<Rule> rules = new HashSet<>();

    public Builder from(MorphemeState from) {
      checkIfDefined(this.from, "from");
      this.from = from;
      return this;
    }

    private void checkIfDefined(Object o, String name) {
      Preconditions.checkArgument(
          o == null,
          "[%s = %s] is already defined.", name, o);
    }

    public Builder to(MorphemeState to) {
      checkIfDefined(this.to, "to");
      this.to = to;
      return this;
    }

    public Builder addRule(Rule rule) {
      if (rules.contains(rule)) {
        Log.warn("Transition already contains rule: %s", rule);
      }
      rules.add(rule);
      return this;
    }

    public Builder addRules(Rule... rules) {
      for (Rule rule : rules) {
        addRule(rule);
      }
      return this;
    }

    public Builder empty() {
      return surfaceTemplate("");
    }

    public Builder surfaceTemplate(String template) {
      checkIfDefined(this.surfaceTemplate, "surfaceTemplate");
      this.surfaceTemplate = template;
      return this;
    }

    // generates a transition and connects it.
    public SuffixTransition add() {
      SuffixTransition transition = new SuffixTransition(this);
      transition.connect();
      return transition;
    }
  }

  public List<SuffixTemplateToken> getTokenList() {
    return tokenList;
  }

  public Set<Rule> getRules() {
    return rules;
  }
}
