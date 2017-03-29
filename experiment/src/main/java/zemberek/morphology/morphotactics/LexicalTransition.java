package zemberek.morphology.morphotactics;

import com.google.common.base.Preconditions;
import zemberek.core.logging.Log;
import zemberek.core.turkish.TurkishAlphabet;

import java.util.*;

public class LexicalTransition {

    LexicalState from;
    LexicalState to;
    // this string represents the possible surface forms for this transition.
    String surfaceTemplate;
    Set<Rule> rules = new HashSet<>(2);

    private LexicalTransition(Builder builder) {
        Preconditions.checkNotNull(builder.from);
        Preconditions.checkNotNull(builder.to);
        this.from = builder.from;
        this.to = builder.to;
        this.surfaceTemplate = builder.surfaceTemplate == null ? "" : builder.surfaceTemplate;
        this.rules = builder.rules;
        this.rules.addAll(rulesFromTemplate(this.surfaceTemplate));
        from.addOutgoing(this);
        to.addIncoming(this);
    }

    private List<Rule> rulesFromTemplate(String template) {
        if (template == null || template.length() == 0) {
            return Collections.emptyList();
        }
        List<Rule> rules = new ArrayList<>(1);
        if (template.startsWith(">") || !TurkishAlphabet.INSTANCE.isVowel(template.charAt(0))) {
            rules.add(Rules.rejectAny(RuleNames.WovelExpecting));
        }
        return rules;
    }

    public Builder builder() {
        return new Builder();
    }

    public static class Builder {
        LexicalState from;
        LexicalState to;
        String surfaceTemplate;
        Set<Rule> rules = new HashSet<>();

        public Builder from(LexicalState from) {
            checkIfDefined(this.from, "from");
            this.from = from;
            return this;
        }

        private void checkIfDefined(Object o, String name) {
            Preconditions.checkArgument(
                    o == null,
                    "[%s = %s] is already defined.", name, o);
        }

        public Builder to(LexicalState to) {
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

        LexicalTransition build() {
            return new LexicalTransition(this);
        }
    }

    public String toString() {
        return "[" + from.id + "->" + to.id + "|" + surfaceTemplate + "]";
    }

}
