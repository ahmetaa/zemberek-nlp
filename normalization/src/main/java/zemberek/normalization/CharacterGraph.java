package zemberek.normalization;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

class CharacterGraph {

    private static final AtomicInteger nodeIndexCounter = new AtomicInteger(0);
    private Node root = new Node(nodeIndexCounter.getAndIncrement(), (char) 0, Node.TYPE_GRAPH_ROOT);

    boolean isRoot(Node node) {
        return node == root;
    }

    public Node getRoot() {
        return root;
    }

    public Node addWord(String word, int type) {
        return add(root, 0, word, type);
    }

    private Node add(Node currentNode, int index, String word, int type) {
        char c = word.charAt(index);
        if (index == word.length() - 1) {
            return currentNode.addChild(nodeIndexCounter.getAndIncrement(), c, type, word);
        }
        Node child = currentNode.addChild(nodeIndexCounter.getAndIncrement(), c, Node.TYPE_EMPTY);
        index++;
        return add(child, index, word, type);
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

    private void walk(Node current, Set<Node> nodes, Consumer<Node> consumer) {
        if (nodes.contains(current)) {
            return;
        }
        consumer.accept(current);
        nodes.add(current);

        for (Node node : current.getImmediateChildNodes()) {
            walk(node, nodes, consumer);
        }
    }


    boolean wordExists(String w, int type) {
        HashSet<Node> nodes = new HashSet<>();
        walk(root, nodes, node -> node.word != null && node.word.equals(w) && node.getType() == type);
        return nodes.size() > 0;
    }

}
