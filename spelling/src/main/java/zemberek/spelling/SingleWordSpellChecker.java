package zemberek.spelling;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.base.Stopwatch;
import zemberek.core.DoubleValueSet;
import zemberek.core.logging.Log;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class SingleWordSpellChecker {

    Node root = new Node((char) 0);

    final double maxPenalty;
    final boolean checkNearKeySubstitution;

    static final double INSERTION_PENALTY = 1.0;
    static final double DELETION_PENALTY = 1.0;
    static final double SUBSTITUTION_PENALTY = 1.0;
    static final double NEAR_KEY_SUBSTITUTION_PENALTY = 0.5;
    static final double TRANSPOSITION_PENALTY = 1.0;

    static Map<Character, String> nearKeys = new HashMap<>();

    static {
        nearKeys.put('a', "eüs");
        nearKeys.put('b', "svn");
        nearKeys.put('c', "vçx");
        nearKeys.put('ç', "czö");
        nearKeys.put('d', "orsf");
        nearKeys.put('e', "iawr");
        nearKeys.put('f', "gd");
        nearKeys.put('g', "fğh");
        nearKeys.put('ğ', "gıpü");
        nearKeys.put('h', "npgj");
        nearKeys.put('ğ', "gıpü");
        nearKeys.put('ı', "ğou");
        nearKeys.put('i', "ueş");
        nearKeys.put('j', "öhk");
        nearKeys.put('k', "tmjl");
        nearKeys.put('l', "mykş");
        nearKeys.put('m', "klnö");
        nearKeys.put('n', "rhbm");
        nearKeys.put('o', "ıdp");
        nearKeys.put('ö', "jvmç");
        nearKeys.put('p', "hqoğ");
        nearKeys.put('r', "dnet");
        nearKeys.put('s', "zbad");
        nearKeys.put('ş', "yl");
        nearKeys.put('t', "ükry");
        nearKeys.put('u', "iyı");
        nearKeys.put('ü', "atğ");
        nearKeys.put('v', "öcb");
        nearKeys.put('y', "lştu");
        nearKeys.put('z', "çsx");
        nearKeys.put('x', "wzc");
        nearKeys.put('q', "pqw");
        nearKeys.put('w', "qxe");
    }

    public SingleWordSpellChecker(double maxPenalty) {
        this.maxPenalty = maxPenalty;
        this.checkNearKeySubstitution = false;
    }

    public SingleWordSpellChecker() {
        this.maxPenalty = 1.0;
        this.checkNearKeySubstitution = false;
    }

    public SingleWordSpellChecker(double maxPenalty, boolean checkNearKeySubstitution) {
        this.maxPenalty = maxPenalty;
        this.checkNearKeySubstitution = checkNearKeySubstitution;
    }

    public static class Node {
        char chr;
        Map<Character, Node> nodes = new HashMap<>(2);
        String word;

        public Node(char chr) {
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
                node = new Node(c);
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

            if (chr != node.chr) return false;
            if (word != null ? !word.equals(node.word) : node.word != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = (int) chr;
            result = 31 * result + (word != null ? word.hashCode() : 0);
            return result;
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
                    else
                        newHypotheses.add(hyp);
                } else
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
                        String nearCharactersString = nearKeys.get(childNode.chr);
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


    void addHypothesis(DoubleValueSet<String> result, Hypothesis hypothesis) {
        String hypWord = hypothesis.node.word;
        if (hypWord == null) {
            if (Log.isDebug())
                Log.info("No word in node:%s", hypothesis.toString());
            return;
        }
        if (!result.contains(hypWord)) {
            result.set(hypWord, hypothesis.penalty);
            if (Log.isDebug())
                Log.info("%s hypotesis added first time:%s", hypWord, hypothesis.toString());
        } else if (result.get(hypWord) > hypothesis.penalty) {
            result.set(hypWord, hypothesis.penalty);
            if (Log.isDebug())
                Log.info("%s hypotesis updated:%s", hypWord, hypothesis.toString());
        }
    }

    DoubleValueSet<String> decode(String input) {
        Hypothesis hyp = new Hypothesis(null, root, 0, Operation.N_A);
        DoubleValueSet<String> hypotheses = new DoubleValueSet<>();
        Set<Hypothesis> next = expand(hyp, input, hypotheses);
        while (true) {
            HashSet<Hypothesis> newHyps = new HashSet<>();
            if (Log.isDebug())
                Log.info("-----------");
            for (Hypothesis hypothesis : next) {
                if (Log.isDebug())
                    Log.info("%s", hypothesis);
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


    public static void main(String[] args) throws IOException {
        SingleWordSpellChecker dt = new SingleWordSpellChecker(1.4, true);
        System.out.println("Loading vocabulary");
        List<String> list = Files.readAllLines(new File("kelimeler").toPath(), Charsets.UTF_8);
        System.out.println("Building tree");
        dt.buildDictionary(list);
        System.out.println("Tree is ready");

        Random rnd = new Random(0xbeefcafe);
        List<String> testSet = new ArrayList<>();

        for (int i = 0; i < 1000; i++) {
            testSet.add(list.get(rnd.nextInt(list.size())));
        }

        testSet.add("hipermaretleri");
        Stopwatch sw = new Stopwatch().start();
        int i = 0;
        System.out.println(testSet.size());
        for (String s : testSet) {
            //System.out.println(s);
            DoubleValueSet<String> res = dt.decode(dt.process(s));
/*            for (String re : res) {
                System.out.println(re + " " + res.getChild(re));
            }*/
            i = i + res.size();
        }
        System.out.println("elapsed " + sw.elapsed(TimeUnit.MILLISECONDS));

        System.out.println(i);
    }


}
