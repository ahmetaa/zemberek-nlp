package zemberek.morphology.morphotactics;

import com.google.common.base.Preconditions;
import zemberek.core.logging.Log;

import java.util.HashSet;
import java.util.Set;

public class Transition {

    MorphemeState from;
    MorphemeState to;
    // this string represents the possible surface forms for this transition.
    // there
    String surfaceTemplate;
    Set<Rule> rules;

    private Transition(Builder builder) {
        Preconditions.checkNotNull(builder.from);
        Preconditions.checkNotNull(builder.to);
        Preconditions.checkNotNull(builder.surfaceTemplate);
        this.from = builder.from;
        this.to = builder.to;
        this.surfaceTemplate = builder.surfaceTemplate;
        this.rules = builder.rules;
        from.addOutgoing(this);
        to.addIncoming(this);
    }

    public Builder builder() {
        return new Builder();
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

        Transition build() {
            return new Transition(this);
        }
    }

    public String toString() {
        return "[" + from.id + "->" + to.id + "|" + surfaceTemplate + "]";
    }

}
