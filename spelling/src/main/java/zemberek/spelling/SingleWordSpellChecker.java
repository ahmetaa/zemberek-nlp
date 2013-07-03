package zemberek.spelling;

import com.google.common.base.Joiner;
import zemberek.core.DoubleValueSet;
import zemberek.core.logging.Log;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class SingleWordSpellChecker {

    private static final AtomicInteger nodeIndexCounter = new AtomicInteger(0);

    private Node root = new Node(nodeIndexCounter.getAndIncrement(), (char) 0);

    public final double maxPenalty;
    public final boolean checkNearKeySubstitution;

    static final double INSERTION_PENALTY = 1.0;
    static final double DELETION_PENALTY = 1.0;
    static final double SUBSTITUTION_PENALTY = 1.0;
    static final double NEAR_KEY_SUBSTITUTION_PENALTY = 0.5;
    static final double TRANSPOSITION_PENALTY = 1.0;

    public Map<Character, String> nearKeyMap = new HashMap<>();
    public static final Map<Character, String> TURKISH_FQ_NEAR_KEY_MAP = new HashMap<>();
    public static final Map<Character, String> TURKISH_Q_NEAR_KEY_MAP = new HashMap<>();

    static {
        TURKISH_FQ_NEAR_KEY_MAP.put('a', "eüs");
        TURKISH_FQ_NEAR_KEY_MAP.put('b', "svn");
        TURKISH_FQ_NEAR_KEY_MAP.put('c', "vçx");
        TURKISH_FQ_NEAR_KEY_MAP.put('ç', "czö");
        TURKISH_FQ_NEAR_KEY_MAP.put('d', "orsf");
        TURKISH_FQ_NEAR_KEY_MAP.put('e', "iawr");
        TURKISH_FQ_NEAR_KEY_MAP.put('f', "gd");
        TURKISH_FQ_NEAR_KEY_MAP.put('g', "fğh");
        TURKISH_FQ_NEAR_KEY_MAP.put('ğ', "gıpü");
        TURKISH_FQ_NEAR_KEY_MAP.put('h', "npgj");
        TURKISH_FQ_NEAR_KEY_MAP.put('ğ', "gıpü");
        TURKISH_FQ_NEAR_KEY_MAP.put('ı', "ğou");
        TURKISH_FQ_NEAR_KEY_MAP.put('i', "ueş");
        TURKISH_FQ_NEAR_KEY_MAP.put('j', "öhk");
        TURKISH_FQ_NEAR_KEY_MAP.put('k', "tmjl");
        TURKISH_FQ_NEAR_KEY_MAP.put('l', "mykş");
        TURKISH_FQ_NEAR_KEY_MAP.put('m', "klnö");
        TURKISH_FQ_NEAR_KEY_MAP.put('n', "rhbm");
        TURKISH_FQ_NEAR_KEY_MAP.put('o', "ıdp");
        TURKISH_FQ_NEAR_KEY_MAP.put('ö', "jvmç");
        TURKISH_FQ_NEAR_KEY_MAP.put('p', "hqoğ");
        TURKISH_FQ_NEAR_KEY_MAP.put('r', "dnet");
        TURKISH_FQ_NEAR_KEY_MAP.put('s', "zbad");
        TURKISH_FQ_NEAR_KEY_MAP.put('ş', "yli");
        TURKISH_FQ_NEAR_KEY_MAP.put('t', "ükry");
        TURKISH_FQ_NEAR_KEY_MAP.put('u', "iyı");
        TURKISH_FQ_NEAR_KEY_MAP.put('ü', "atğ");
        TURKISH_FQ_NEAR_KEY_MAP.put('v', "öcb");
        TURKISH_FQ_NEAR_KEY_MAP.put('y', "lştu");
        TURKISH_FQ_NEAR_KEY_MAP.put('z', "çsx");
        TURKISH_FQ_NEAR_KEY_MAP.put('x', "wzc");
        TURKISH_FQ_NEAR_KEY_MAP.put('q', "pqw");
        TURKISH_FQ_NEAR_KEY_MAP.put('w', "qxe");
    }

    static {
        TURKISH_Q_NEAR_KEY_MAP.put('a', "s");
        TURKISH_Q_NEAR_KEY_MAP.put('b', "vn");
        TURKISH_Q_NEAR_KEY_MAP.put('c', "vx");
        TURKISH_Q_NEAR_KEY_MAP.put('ç', "ö");
        TURKISH_Q_NEAR_KEY_MAP.put('d', "sf");
        TURKISH_Q_NEAR_KEY_MAP.put('e', "wr");
        TURKISH_Q_NEAR_KEY_MAP.put('f', "gd");
        TURKISH_Q_NEAR_KEY_MAP.put('g', "fh");
        TURKISH_Q_NEAR_KEY_MAP.put('ğ', "pü");
        TURKISH_Q_NEAR_KEY_MAP.put('h', "gj");
        TURKISH_Q_NEAR_KEY_MAP.put('ğ', "pü");
        TURKISH_Q_NEAR_KEY_MAP.put('ı', "ou");
        TURKISH_Q_NEAR_KEY_MAP.put('i', "ş");
        TURKISH_Q_NEAR_KEY_MAP.put('j', "hk");
        TURKISH_Q_NEAR_KEY_MAP.put('k', "jl");
        TURKISH_Q_NEAR_KEY_MAP.put('l', "kş");
        TURKISH_Q_NEAR_KEY_MAP.put('m', "nö");
        TURKISH_Q_NEAR_KEY_MAP.put('n', "bm");
        TURKISH_Q_NEAR_KEY_MAP.put('o', "ıp");
        TURKISH_Q_NEAR_KEY_MAP.put('ö', "mç");
        TURKISH_Q_NEAR_KEY_MAP.put('p', "oğ");
        TURKISH_Q_NEAR_KEY_MAP.put('r', "et");
        TURKISH_Q_NEAR_KEY_MAP.put('s', "ad");
        TURKISH_Q_NEAR_KEY_MAP.put('ş', "li");
        TURKISH_Q_NEAR_KEY_MAP.put('t', "ry");
        TURKISH_Q_NEAR_KEY_MAP.put('u', "yı");
        TURKISH_Q_NEAR_KEY_MAP.put('ü', "ğ");
        TURKISH_Q_NEAR_KEY_MAP.put('v', "cb");
        TURKISH_Q_NEAR_KEY_MAP.put('y', "tu");
        TURKISH_Q_NEAR_KEY_MAP.put('z', "x");
        TURKISH_Q_NEAR_KEY_MAP.put('x', "zc");
        TURKISH_Q_NEAR_KEY_MAP.put('q', "w");
        TURKISH_Q_NEAR_KEY_MAP.put('w', "qe");
    }

    public SingleWordSpellChecker(double maxPenalty) {
        this.maxPenalty = maxPenalty;
        this.checkNearKeySubstitution = false;
    }

    public SingleWordSpellChecker() {
        this.maxPenalty = 1.0;
        this.checkNearKeySubstitution = false;
    }

    public SingleWordSpellChecker(double maxPenalty, Map<Character, String> nearKeyMap) {
        this.maxPenalty = maxPenalty;
        this.nearKeyMap = Collections.unmodifiableMap(nearKeyMap);
        this.checkNearKeySubstitution = true;
    }

    public static class Node {
        int index;
        char chr;
        Map<Character, Node> nodes = new HashMap<>(2);
        String word;

        public Node(int index, char chr) {
            this.index = index;
            this.chr = chr;
        }

        public Iterable<Node> getChildNodes() {
            return nodes.values();
        }

        public boolean hasChild(char c) {
            return nodes.containsKey(c);
        }

        public Node getChild(char c) {
            return nodes.get(c);
        }

        public Node addChild(char c) {
            Node node = nodes.get(c);
            if (node == null) {
                node = new Node(nodeIndexCounter.getAndIncrement(), c);
            }
            nodes.put(c, node);
            return node;
        }

        public void setWord(String word) {
            this.word = word;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Node node = (Node) o;
            return index == node.index;
        }

        @Override
        public int hashCode() {
            return index;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder("[" + chr);
            if (nodes.size() > 0)
                sb.append(" children=").append(Joiner.on(",").join(nodes.keySet()));
            if (word != null)
                sb.append(" word=").append(word);
            sb.append("]");
            return sb.toString();
        }
    }

    Locale tr = new Locale("tr");

    public String process(String str) {
        return str.toLowerCase(tr).replace("['.]", "");
    }

    public void addWord(String word) {
        String clean = process(word);
        addChar(root, 0, clean, word);
    }

    public void addWords(String... words) {
        for (String word : words) {
            addWord(word);
        }
    }

    public void buildDictionary(List<String> vocabulary) {
        for (String s : vocabulary) {
            addWord(s);
        }
    }

    private Node addChar(Node currentNode, int index, String word, String actual) {
        char c = word.charAt(index);
        Node child = currentNode.addChild(c);
        if (index == word.length() - 1) {
            child.word = actual;
            return child;
        }
        index++;
        return addChar(child, index, word, actual);
    }

    Set<Hypothesis> expand(Hypothesis hypothesis, String input, DoubleValueSet<String> finished) {

        Set<Hypothesis> newHypotheses = new HashSet<>();

        int nextIndex = hypothesis.index + 1;

        // no-error
        if (nextIndex < input.length()) {
            if (hypothesis.node.hasChild(input.charAt(nextIndex))) {
                Hypothesis hyp = hypothesis.getNewMoveForward(
                        hypothesis.node.getChild(input.charAt(nextIndex)),
                        0.0,
                        Operation.NE);
                if (nextIndex >= input.length() - 1) {
                    if (hyp.node.word != null)
                        addHypothesis(finished, hyp);
                } // TODO: below line may produce unnecessary hypotheses.
                newHypotheses.add(hyp);
            }
        } else if (hypothesis.node.word != null)
            addHypothesis(finished, hypothesis);

        // we don't need to explore further if we reached to max penalty
        if (hypothesis.penalty >= maxPenalty)
            return newHypotheses;

        // substitution
        if (nextIndex < input.length()) {
            for (Node childNode : hypothesis.node.getChildNodes()) {

                double penalty = 0;
                if (checkNearKeySubstitution) {
                    char nextChar = input.charAt(nextIndex);
                    if (childNode.chr != nextChar) {
                        String nearCharactersString = nearKeyMap.get(childNode.chr);
                        if (nearCharactersString != null && nearCharactersString.indexOf(nextChar) >= 0)
                            penalty = NEAR_KEY_SUBSTITUTION_PENALTY;
                        else penalty = SUBSTITUTION_PENALTY;
                    }
                } else penalty = SUBSTITUTION_PENALTY;

                if (penalty > 0 && hypothesis.penalty + penalty <= maxPenalty) {
                    Hypothesis hyp = hypothesis.getNewMoveForward(
                            childNode,
                            penalty,
                            Operation.SUB);
                    if (nextIndex == input.length() - 1) {
                        if (hyp.node.word != null)
                            addHypothesis(finished, hyp);
                    } else
                        newHypotheses.add(hyp);
                }
            }
        }

        if (hypothesis.penalty + DELETION_PENALTY > maxPenalty)
            return newHypotheses;

        // deletion
        newHypotheses.add(hypothesis.getNewMoveForward(hypothesis.node, DELETION_PENALTY, Operation.DEL));

        // insertion
        for (Node childNode : hypothesis.node.getChildNodes()) {
            newHypotheses.add(hypothesis.getNew(childNode, INSERTION_PENALTY, Operation.INS));
        }

        // transposition
        if (nextIndex < input.length() - 1) {
            char transpose = input.charAt(nextIndex + 1);
            Node nextNode = hypothesis.node.getChild(transpose);
            char nextChar = input.charAt(nextIndex);
            if (hypothesis.node.hasChild(transpose) && nextNode.hasChild(nextChar)) {
                Hypothesis hyp = hypothesis.getNew(
                        nextNode.getChild(nextChar),
                        TRANSPOSITION_PENALTY,
                        nextIndex + 1,
                        Operation.TR);
                if (nextIndex == input.length() - 1) {
                    if (hyp.node.word != null)
                        addHypothesis(finished, hyp);
                } else
                    newHypotheses.add(hyp);
            }
        }
        return newHypotheses;
    }


    private void addHypothesis(DoubleValueSet<String> result, Hypothesis hypothesis) {
        String hypWord = hypothesis.node.word;
        if (hypWord == null) {
            if (Log.isDebug())
                Log.debug("No word in node:%s", hypothesis.toString());
            return;
        }
        if (!result.contains(hypWord)) {
            result.set(hypWord, hypothesis.penalty);
            if (Log.isDebug())
                Log.debug("Hypotesis added:%s", hypothesis.toString());
        } else if (result.get(hypWord) > hypothesis.penalty) {
            result.set(hypWord, hypothesis.penalty);
            if (Log.isDebug())
                Log.debug("Hypotesis updated:%s", hypothesis.toString());
        }
    }

    public DoubleValueSet<String> decode(String input) {
        Hypothesis hyp = new Hypothesis(null, root, 0, Operation.N_A);
        DoubleValueSet<String> hypotheses = new DoubleValueSet<>();
        Set<Hypothesis> next = expand(hyp, input, hypotheses);
        while (true) {
            HashSet<Hypothesis> newHyps = new HashSet<>();
            if (Log.isDebug())
                Log.debug("-----------");
            for (Hypothesis hypothesis : next) {
                if (Log.isDebug())
                    Log.debug("%s", hypothesis);
                newHyps.addAll(expand(hypothesis, input, hypotheses));
            }
            if (newHyps.size() == 0)
                break;
            next = newHyps;
        }
        return hypotheses;
    }

    enum Operation {
        NE, INS, DEL, SUB, TR, N_A
    }

    static class Hypothesis implements Comparable<Hypothesis> {
        Operation operation = Operation.N_A;
        Hypothesis previous;
        Node node;
        double penalty;
        int index;

        Hypothesis(Hypothesis previous, Node node, double penalty, Operation operation) {
            this.previous = previous;
            this.node = node;
            this.penalty = penalty;
            this.index = -1;
            this.operation = operation;
        }

        String backTrack() {
            StringBuilder sb = new StringBuilder();
            Hypothesis p = previous;
            while (p.node.chr != 0) {
                if (p.node != p.previous.node)
                    sb.append(p.node.chr);
                p = p.previous;
            }
            return sb.reverse().toString();
        }

        Hypothesis(Hypothesis previous, Node node, double penalty, int index, Operation operation) {
            this.previous = previous;
            this.node = node;
            this.penalty = penalty;
            this.index = index;
            this.operation = operation;
        }

        Hypothesis getNew(Node node, double penaltyToAdd, Operation operation) {
            return new Hypothesis(this, node, this.penalty + penaltyToAdd, index, operation);
        }

        Hypothesis getNewMoveForward(Node node, double penaltyToAdd, Operation operation) {
            return new Hypothesis(this, node, this.penalty + penaltyToAdd, index + 1, operation);
        }

        Hypothesis getNew(Node node, double penaltyToAdd, int index, Operation operation) {
            return new Hypothesis(this, node, this.penalty + penaltyToAdd, index, operation);
        }

        Hypothesis getNew(double penaltyToAdd, Operation operation) {
            return new Hypothesis(this, this.node, this.penalty + penaltyToAdd, index, operation);
        }

        @Override
        public int compareTo(Hypothesis o) {
            return Double.compare(penalty, o.penalty);
        }

        @Override
        public String toString() {
            return "Hypothesis{" +
                    "previous=" + backTrack() + " " + previous.operation +
                    ", node=" + node +
                    ", penalty=" + penalty +
                    ", index=" + index +
                    ", OP=" + operation.name() +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Hypothesis that = (Hypothesis) o;

            if (index != that.index) return false;
            if (Double.compare(that.penalty, penalty) != 0) return false;
            if (!node.equals(that.node)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result;
            long temp;
            result = node.hashCode();
            temp = Double.doubleToLongBits(penalty);
            result = 31 * result + (int) (temp ^ (temp >>> 32));
            result = 31 * result + index;
            return result;
        }
    }
}
