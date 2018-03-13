package zemberek.morphology._morphotactics;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import zemberek.core.collections.IntMap;
import zemberek.core.logging.Log;
import zemberek.core.turkish.PhoneticAttribute;
import zemberek.core.turkish.TurkishAlphabet;
import zemberek.morphology._analyzer.MorphemeSurfaceForm.SuffixTemplateToken;
import zemberek.morphology._analyzer.MorphemeSurfaceForm.SuffixTemplateTokenizer;
import zemberek.morphology._analyzer.SearchPath;
import zemberek.morphology.structure.Turkish;

public class SuffixTransition extends MorphemeTransition {

  // this string represents the possible surface forms for this transition.
  private final String surfaceTemplate;

  private List<SuffixTemplateToken> tokenList;

  private Map<EnumSet<PhoneticAttribute>, String> surfaceCache = new HashMap<>();

  private IntMap<String> surfaceCacheNew = new IntMap<>();

  public IntMap<String> getSurfaceCacheNew() {
    return  surfaceCacheNew;
  }

  public Map<EnumSet<PhoneticAttribute>, String> getSurfaceCache() {
    return surfaceCache;
  }

  private SuffixTransition(Builder builder) {
    Preconditions.checkNotNull(builder.from);
    Preconditions.checkNotNull(builder.to);
    this.from = builder.from;
    this.to = builder.to;
    this.surfaceTemplate = builder.surfaceTemplate == null ? "" : builder.surfaceTemplate;
    this.condition = builder.condition;
    conditionsFromTemplate(this.surfaceTemplate);
    this.tokenList = Lists
        .newArrayList(new SuffixTemplateTokenizer(this.surfaceTemplate));
    this.conditionCount = countConditions();
  }

  private int countConditions() {
    if (condition == null) {
      return 0;
    }
    if (condition instanceof CombinedCondition) {
      return ((CombinedCondition) condition).count();
    } else {
      return 1;
    }
  }

  private SuffixTransition(String surfaceTemplate) {
    this.surfaceTemplate = surfaceTemplate;
  }

  public SuffixTransition getCopy() {
    SuffixTransition st = new SuffixTransition(surfaceTemplate);
    st.from = from;
    st.to = to;
    st.condition = condition;
    st.tokenList = new ArrayList<>(tokenList);
    return st;
  }

  public boolean canPass(SearchPath path) {
    return condition == null || condition.accept(path);
  }

  private void connect() {
    from.addOutgoing(this);
    to.addIncoming(this);
  }

  // adds vowel-consonant expectation related conditions automatically.
  // TODO: consider moving this to morphotactics somehow.
  private void conditionsFromTemplate(String template) {
    if (template == null || template.length() == 0) {
      return;
    }
    String lower = template.toLowerCase(Turkish.LOCALE);
    Condition c = null;
    boolean firstCharVowel = TurkishAlphabet.INSTANCE.isVowel(lower.charAt(0));
    if (lower.startsWith(">") || !firstCharVowel) {
      c = Conditions.notHave(PhoneticAttribute.ExpectsVowel);
    }
    if ((lower.startsWith("+") && TurkishAlphabet.INSTANCE.isVowel(lower.charAt(2)))
        || firstCharVowel) {
      c = Conditions.notHave(PhoneticAttribute.ExpectsConsonant);
    }
    if (c != null) {
      if (condition == null) {
        condition = c;
      } else {
        condition = c.and(condition);
      }
    }
  }

  public Builder builder() {
    return new Builder();
  }

  public String toString() {
    return "[" + from.id + "→" + to.id +
        (surfaceTemplate.isEmpty() ? "" : (":" + surfaceTemplate))
        + "]";
  }

  public static class Builder {

    MorphemeState from;
    MorphemeState to;
    String surfaceTemplate;
    Condition condition;

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

    public Builder setCondition(Condition _condition) {
      if (condition != null) {
        Log.warn("Condition was already set.");
      }
      this.condition = _condition;
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
    public SuffixTransition build() {
      SuffixTransition transition = new SuffixTransition(this);
      transition.connect();
      return transition;
    }

    // generates a transition and connects it.
    public MorphemeState add() {
      SuffixTransition transition = new SuffixTransition(this);
      transition.connect();
      return transition.from;
    }
  }

  public List<SuffixTemplateToken> getTokenList() {
    return tokenList;
  }

  public boolean hasSurfaceForm() {
    return tokenList.size() > 0;
  }

  public SuffixTemplateToken getLastTemplateToken() {
    if (tokenList.size() == 0) {
      return null;
    } else {
      return tokenList.get(tokenList.size() - 1);
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
    SuffixTransition that = (SuffixTransition) o;
    return Objects.equals(surfaceTemplate, that.surfaceTemplate)
        && Objects.equals(from, that.from)
        && Objects.equals(to, that.to);
  }

  @Override
  public int hashCode() {
    return Objects.hash(surfaceTemplate, from, to);
  }
}
