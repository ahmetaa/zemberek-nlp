package zemberek.normalization;

import zemberek.core.collections.UIntMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class Node {

    private int index;
    char chr;
    private UIntMap<Node> nodes = new UIntMap<>(2);
    String word;
    private Node[] epsilonNodes = null;
    private int type;

    static final int TYPE_EMPTY = 0;
    static final int TYPE_WORD = 1;
    static final int TYPE_ENDING = 2;
    static final int TYPE_GRAPH_ROOT = 3;

    Node(int index, char chr, int type) {
        this.index = index;
        this.chr = chr;
        this.type = type;
    }

    Node(int index, char chr, int type, String word) {
        this.index = index;
        this.chr = chr;
        this.type = type;
        this.word = word;
    }

    public int getType() {
        return type;
    }

    Iterable<Node> getImmediateChildNodeIterable() {
        return nodes;
    }

    List<Node> getImmediateChildNodes() {
        return nodes.getValues();
    }

    List<Node> getAllChildNodes() {
        List<Node> nodeList = nodes.getValues();
        if (epsilonNodes == null) {
            return nodeList;
        }
        for (Node emptyNode : epsilonNodes) {
            for(Node n: emptyNode.nodes) {
                nodeList.add(n);
            }
        }
        return nodeList;
    }

    boolean hasEpsilonConnection() {
        return epsilonNodes != null;
    }

    boolean hasImmediateChild(char c) {
        return nodes.containsKey(c);
    }

    boolean hasChild(char c) {
        if (hasImmediateChild(c)) {
            return true;
        }
        if (epsilonNodes == null) {
            return false;
        }
        for (Node node : epsilonNodes) {
            if (node.hasImmediateChild(c)) {
                return true;
            }
        }
        return false;
    }

    Node getImmediateChild(char c) {
        return nodes.get(c);
    }

    private void addIfChildExists(char c, List<Node> nodeList) {
        Node child = this.nodes.get(c);
        if (child != null) {
            nodeList.add(child);
        }
    }

    List<Node> getChildList(char[] charArray) {
        List<Node> children = new ArrayList<>(charArray.length + 1);
        for (char c : charArray) {
            addIfChildExists(c, children);
            if (epsilonNodes != null) {
                for (Node emptyNode : epsilonNodes) {
                    emptyNode.addIfChildExists(c, children);
                }
            }
        }
        return children;
    }


    List<Node> getChildList(char c) {
        List<Node> children = new ArrayList<>(2);
        addIfChildExists(c, children);
        if (epsilonNodes != null) {
            for (Node emptyNode : epsilonNodes) {
                emptyNode.addIfChildExists(c, children);
            }
        }
        return children;
    }

    boolean connectEpsilon(Node node) {
        if (epsilonNodes == null) {
            epsilonNodes = new Node[1];
            epsilonNodes[0] = node;
        } else {
            for (Node n : epsilonNodes) {
                if (n.equals(node)) {
                    return false;
                }
            }
            epsilonNodes = Arrays.copyOf(epsilonNodes, epsilonNodes.length + 1);
            epsilonNodes[epsilonNodes.length - 1] = node;
        }
        return true;
    }

    public Node[] getEpsilonNodes() {
        return epsilonNodes;
    }

    Node addChild(int index, char c, int type) {
        Node node = nodes.get(c);
        if (node == null) {
            node = new Node(index, c, type);
            nodes.put(c, node);
        }
        return node;
    }

    Node addChild(int index, char c, int type, String word) {
        Node node = nodes.get(c);
        if (node == null) {
            node = new Node(index, c, type, word);
            nodes.put(c, node);
        } else {
            node.word = word;
            node.type = type;
        }
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
