package zemberek.normalization;

import zemberek.core.collections.FloatValueMap;
import zemberek.core.collections.UIntMap;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class SingleWordSpellChecker {

    public static final Map<Character, String> TURKISH_FQ_NEAR_KEY_MAP = new HashMap<>();
    public static final Map<Character, String> TURKISH_Q_NEAR_KEY_MAP = new HashMap<>();
    static final float INSERTION_PENALTY = 1;
    static final float DELETION_PENALTY = 1;
    static final float SUBSTITUTION_PENALTY = 1;
    static final float NEAR_KEY_SUBSTITUTION_PENALTY = 0.5f;
    static final float TRANSPOSITION_PENALTY = 1;
    private static final AtomicInteger nodeIndexCounter = new AtomicInteger(0);
    private static final Locale tr = new Locale("tr");

    static {
        Map<Character, String> map =TURKISH_FQ_NEAR_KEY_MAP; 
        map.put('a', "eüs");
        map.put('b', "svn");
        map.put('c', "vçx");
        map.put('ç', "czö");
        map.put('d', "orsf");
        map.put('e', "iawr");
        map.put('f', "gd");
        map.put('g', "fğh");
        map.put('ğ', "gıpü");
        map.put('h', "npgj");
        map.put('ğ', "gıpü");
        map.put('ı', "ğou");
        map.put('i', "ueş");
        map.put('j', "öhk");
        map.put('k', "tmjl");
        map.put('l', "mykş");
        map.put('m', "klnö");
        map.put('n', "rhbm");
        map.put('o', "ıdp");
        map.put('ö', "jvmç");
        map.put('p', "hqoğ");
        map.put('r', "dnet");
        map.put('s', "zbad");
        map.put('ş', "yli");
        map.put('t', "ükry");
        map.put('u', "iyı");
        map.put('ü', "atğ");
        map.put('v', "öcb");
        map.put('y', "lştu");
        map.put('z', "çsx");
        map.put('x', "wzc");
        map.put('q', "pqw");
        map.put('w', "qxe");
    }

    static {
        Map<Character, String> map =TURKISH_Q_NEAR_KEY_MAP;

        map.put('a', "s");
        map.put('b', "vn");
        map.put('c', "vx");
        map.put('ç', "ö");
        map.put('d', "sf");
        map.put('e', "wr");
        map.put('f', "gd");
        map.put('g', "fh");
        map.put('ğ', "pü");
        map.put('h', "gj");
        map.put('ğ', "pü");
        map.put('ı', "ou");
        map.put('i', "ş");
        map.put('j', "hk");
        map.put('k', "jl");
        map.put('l', "kş");
        map.put('m', "nö");
        map.put('n', "bm");
        map.put('o', "ıp");
        map.put('ö', "mç");
        map.put('p', "oğ");
        map.put('r', "et");
        map.put('s', "ad");
        map.put('ş', "li");
        map.put('t', "ry");
        map.put('u', "yı");
        map.put('ü', "ğ");
        map.put('v', "cb");
        map.put('y', "tu");
        map.put('z', "x");
        map.put('x', "zc");
        map.put('q', "w");
        map.put('w', "qe");
    }

    public final float maxPenalty;
    public final boolean checkNearKeySubstitution;
    public Map<Character, String> nearKeyMap = new HashMap<>();
    private Graph graph = new Graph();

    public SingleWordSpellChecker(float maxPenalty) {
        this.maxPenalty = maxPenalty;
        this.checkNearKeySubstitution = false;
    }

    public SingleWordSpellChecker() {
        this.maxPenalty = 1;
        this.checkNearKeySubstitution = false;
    }

    public SingleWordSpellChecker(float maxPenalty, Map<Character, String> nearKeyMap) {
        this.maxPenalty = maxPenalty;
        this.nearKeyMap = Collections.unmodifiableMap(nearKeyMap);
        this.checkNearKeySubstitution = true;
    }

    private String process(String str) {
        return str.toLowerCase(tr).replace("['.]", "");
    }


    public void addWord(String word) {
        graph.addWord(process(word));
    }


    public void addWords(String... words) {
        for (String word : words) {
            graph.addWord(process(word));
        }
    }

    public void buildDictionary(List<String> vocabulary) {
        for (String s : vocabulary) {
            graph.addWord(process(s));
        }
    }

    /**
     * Returns suggestions sorted by penalty.
     */
    public List<ScoredString> getSuggestionsWithScores(String input) {
        Decoder decoder = new Decoder();
        return getMatches(input, decoder);
    }

    private List<ScoredString> getMatches(String input, Decoder decoder) {
        FloatValueMap<String> results = decoder.decode(input);

        List<ScoredString> res = new ArrayList<>(results.size());
        for (String result : results) {
            res.add(new ScoredString(result, results.get(result)));
        }
        Collections.sort(res);
        return res;
    }

    public List<ScoredString> getSuggestionsWithScores(String input, CharMatcher matcher) {
        Decoder decoder = new Decoder(matcher);
        return getMatches(input, decoder);
    }

    public FloatValueMap<String> decode(String input) {
        return new Decoder().decode(input);
    }


    public List<String> getSuggestions(String input) {
        return new Decoder().decode(input).getKeyList();
    }

    public List<String> getSuggestionsSorted(String input) {
        List<ScoredString> s = getSuggestionsWithScores(input);
        List<String> result = new ArrayList<>(s.size());
        result.addAll(s.stream().map(s1 -> s1.s).collect(Collectors.toList()));
        return result;
    }

    enum Operation {
        NE, INS, DEL, SUB, TR, N_A
    }

    public static class Node {
        int index;
        char chr;
        UIntMap<Node> nodes = new UIntMap<>(2);
        String word;

        Node(int index, char chr) {
            this.index = index;
            this.chr = chr;
        }

        Iterable<Node> getChildNodes() {
            return nodes;
        }

        boolean hasChild(char c) {
            return nodes.containsKey(c);
        }

        Node getChild(char c) {
            return nodes.get(c);
        }

        Node addChild(char c) {
            Node node = nodes.get(c);
            if (node == null) {
                node = new Node(nodeIndexCounter.getAndIncrement(), c);
            }
            nodes.put(c, node);
            return node;
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
            char[] characters = new char[nodes.size()];
            int[] keys = nodes.getKeyArray();
            for (int i = 0; i < characters.length; i++) {
                characters[i] = (char) keys[i];
            }
            Arrays.sort(characters);
            if (nodes.size() > 0) {
                sb.append(" children=").append(Arrays.toString(characters));
            }
            if (word != null) {
                sb.append(" word=").append(word);
            }
            sb.append("]");
            return sb.toString();
        }
    }

    public static class ScoredString implements Comparable<ScoredString> {
        final String s;
        final float penalty;

        public ScoredString(String s, float penalty) {
            this.s = s;
            this.penalty = penalty;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ScoredString result = (ScoredString) o;

            if (Float.compare(result.penalty, penalty) != 0) return false;
            if (!s.equals(result.s)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result;
            long temp;
            result = s.hashCode();
            temp = Float.floatToIntBits(penalty);
            result = 31 * result + (int) (temp ^ (temp >>> 32));
            return result;
        }

        @Override
        public int compareTo(ScoredString o) {
            return Float.compare(penalty, o.penalty);
        }
    }

    static class Hypothesis implements Comparable<Hypothesis> {
        Operation operation = Operation.N_A;
        Hypothesis previous;
        Node node;
        float penalty;
        int index;

        Hypothesis(Hypothesis previous, Node node, float penalty, Operation operation) {
            this.previous = previous;
            this.node = node;
            this.penalty = penalty;
            this.index = -1;
            this.operation = operation;
        }

        Hypothesis(Hypothesis previous, Node node, float penalty, int index, Operation operation) {
            this.previous = previous;
            this.node = node;
            this.penalty = penalty;
            this.index = index;
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

        Hypothesis getNew(Node node, float penaltyToAdd, Operation operation) {
            return new Hypothesis(this, node, this.penalty + penaltyToAdd, index, operation);
        }

        Hypothesis getNewMoveForward(Node node, float penaltyToAdd, Operation operation) {
            return new Hypothesis(this, node, this.penalty + penaltyToAdd, index + 1, operation);
        }

        Hypothesis getNew(Node node, float penaltyToAdd, int index, Operation operation) {
            return new Hypothesis(this, node, this.penalty + penaltyToAdd, index, operation);
        }

        Hypothesis getNew(float penaltyToAdd, Operation operation) {
            return new Hypothesis(this, this.node, this.penalty + penaltyToAdd, index, operation);
        }

        @Override
        public int compareTo(Hypothesis o) {
            return Float.compare(penalty, o.penalty);
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

    private class Graph {
        Node root = new Node(nodeIndexCounter.getAndIncrement(), (char) 0);

        public void addWord(String word) {
            add(root, 0, word, word);
        }

        private Node add(Node currentNode, int index, String word, String actual) {
            char c = word.charAt(index);
            Node child = currentNode.addChild(c);
            if (index == word.length() - 1) {
                child.word = actual;
                return child;
            }
            index++;
            return add(child, index, word, actual);
        }
    }

    public interface CharMatcher {
        char[] matches(char c);
    }

    private static class DeasciifierMatcher implements CharMatcher {

        static final char[] C = {'c', 'ç'};
        static final char[] G = {'g', 'ğ'};
        static final char[] I = {'ı', 'i'};
        static final char[] O = {'o', 'ö'};
        static final char[] S = {'s', 'ş'};
        static final char[] U = {'u', 'ü'};

        @Override
        public char[] matches(char c) {
            switch (c) {
                case 'c':
                    return C;
                case 'g':
                    return G;
                case 'i':
                case 'ı':
                    return I;
                case 's':
                    return S;
                case 'o':
                    return O;
                case 'u':
                    return U;
                default:
                    return new char[]{c};
            }
        }
    }

    public  static class ExactMatcher implements CharMatcher {
        @Override
        public char[] matches(char c) {
            return new char[]{c};
        }
    }

    private static ExactMatcher EXACT_MATCHER = new ExactMatcher();
    public static DeasciifierMatcher ASCII_TOLERANT_MATCHER = new DeasciifierMatcher();

    private class Decoder {
        FloatValueMap<String> finished = new FloatValueMap<>(4);
        CharMatcher matcher;

        Decoder() {
            this(EXACT_MATCHER);
        }

        Decoder(CharMatcher matcher) {
            this.matcher = matcher;
        }

        FloatValueMap<String> decode(String input) {
            Hypothesis hyp = new Hypothesis(null, graph.root, 0, Operation.N_A);

            Set<Hypothesis> next = expand(hyp, input);
            while (true) {
                HashSet<Hypothesis> newHyps = new HashSet<>();
                for (Hypothesis hypothesis : next) {
                    newHyps.addAll(expand(hypothesis, input));
                }
                if (newHyps.size() == 0)
                    break;
                next = newHyps;
            }
            return finished;
        }

        private Set<Hypothesis> expand(Hypothesis hypothesis, String input) {

            Set<Hypothesis> newHypotheses = new HashSet<>();

            int nextIndex = hypothesis.index + 1;

            char nextChar = nextIndex < input.length() ? input.charAt(nextIndex) : 0;

            // no-error
            if (nextIndex < input.length()) {
                char[] cc = matcher.matches(nextChar);
                for (char c : cc) {
                    if (hypothesis.node.hasChild(c)) {
                        Hypothesis h = hypothesis.getNewMoveForward(
                                hypothesis.node.getChild(c),
                                0,
                                Operation.NE);
                        if (nextIndex >= input.length() - 1) {
                            if (h.node.word != null) {
                                addHypothesis(h);
                            }
                        }
                        newHypotheses.add(h);
                    }
                }
            } else if (hypothesis.node.word != null) {
                addHypothesis(hypothesis);
            }

            // we don't need to explore further if we reached to max penalty
            if (hypothesis.penalty >= maxPenalty) {
                return newHypotheses;
            }

            // substitution
            if (nextIndex < input.length()) {
                for (Node childNode : hypothesis.node.getChildNodes()) {

                    float penalty = 0;
                    if (checkNearKeySubstitution) {
                        if (childNode.chr != nextChar) {
                            String nearCharactersString = nearKeyMap.get(childNode.chr);
                            if (nearCharactersString != null && nearCharactersString.indexOf(nextChar) >= 0) {
                                penalty = NEAR_KEY_SUBSTITUTION_PENALTY;
                            } else {
                                penalty = SUBSTITUTION_PENALTY;
                            }
                        }
                    } else {
                        penalty = SUBSTITUTION_PENALTY;
                    }

                    if (penalty > 0 && hypothesis.penalty + penalty <= maxPenalty) {
                        Hypothesis hyp = hypothesis.getNewMoveForward(
                                childNode,
                                penalty,
                                Operation.SUB);
                        if (nextIndex == input.length() - 1) {
                            if (hyp.node.word != null) {
                                addHypothesis(hyp);
                            }
                        } else {
                            newHypotheses.add(hyp);
                        }
                    }
                }
            }

            if (hypothesis.penalty + DELETION_PENALTY > maxPenalty) {
                return newHypotheses;
            }

            // deletion
            newHypotheses.add(hypothesis.getNewMoveForward(hypothesis.node, DELETION_PENALTY, Operation.DEL));

            // insertion
            for (Node childNode : hypothesis.node.getChildNodes()) {
                newHypotheses.add(hypothesis.getNew(childNode, INSERTION_PENALTY, Operation.INS));
            }

            // transposition
            if (nextIndex < input.length() - 1) {
                char transpose = input.charAt(nextIndex + 1);
                char[] tt = matcher.matches(transpose);
                for (char t : tt) {
                    Node nextNode = hypothesis.node.getChild(t);
                    char[] cc = matcher.matches(nextChar);
                    for (char c : cc) {
                        if (hypothesis.node.hasChild(t) && nextNode.hasChild(c)) {
                            Hypothesis hyp = hypothesis.getNew(
                                    nextNode.getChild(c),
                                    TRANSPOSITION_PENALTY,
                                    nextIndex + 1,
                                    Operation.TR);
                            if (nextIndex == input.length() - 1) {
                                if (hyp.node.word != null) {
                                    addHypothesis(hyp);
                                }
                            } else {
                                newHypotheses.add(hyp);
                            }
                        }
                    }
                }
            }
            return newHypotheses;
        }

        private void addHypothesis(Hypothesis hypothesis) {
            String hypWord = hypothesis.node.word;
            if (hypWord == null) {
                return;
            }
            if (!finished.contains(hypWord)) {
                finished.set(hypWord, hypothesis.penalty);
            } else if (finished.get(hypWord) > hypothesis.penalty) {
                finished.set(hypWord, hypothesis.penalty);
            }
        }

    }
}
