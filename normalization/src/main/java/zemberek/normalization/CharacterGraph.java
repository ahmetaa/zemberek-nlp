package zemberek.normalization;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

class CharacterGraph {

    private static final AtomicInteger nodeIndexCounter = new AtomicInteger(0);
    private Node root = new Node(nodeIndexCounter.getAndIncrement(), (char) 0);

    boolean isRoot(Node node) {
        return node == root;
    }

    public Node getRoot() {
        return root;
    }

    public Node addWord(String word) {
        return add(root, 0, word, word);
    }

    private Node add(Node currentNode, int index, String word, String actual) {
        char c = word.charAt(index);
        Node child = currentNode.addChild(nodeIndexCounter.getAndIncrement(), c);
        if (index == word.length() - 1) {
            child.word = actual;
            return child;
        }
        index++;
        return add(child, index, word, actual);
    }

    public Set<Node> getAllNodes() {
        HashSet<Node> nodes = new HashSet<>();
        walk(root, nodes, node -> true);
        return nodes;
    }

    public Set<Node> getAllNodes(Predicate<Node> predicate) {
        HashSet<Node> nodes = new HashSet<>();
        walk(root, nodes, predicate);
        return nodes;
    }

    private void walk(Node current, Set<Node> nodes, Predicate<Node> predicate) {
        if (nodes.contains(current)) {
            return;
        }
        if (predicate.test(current)) {
            nodes.add(current);
        }
        for (Node node : current.getImmediateChildNodes()) {
            walk(node, nodes, predicate);
        }
    }

}
