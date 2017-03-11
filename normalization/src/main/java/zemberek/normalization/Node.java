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
    Node[] emptyNodes = null;

    Node(int index, char chr) {
        this.index = index;
        this.chr = chr;
    }

    Iterable<Node> getChildNodeIterable() {
        return nodes;
    }

    List<Node> getImmediateChildNodes() {
        return nodes.getValues();
    }

    List<Node> getAllChildNodes() {
        List<Node> nodeList = nodes.getValues();
        if (emptyNodes == null) {
            return nodeList;
        }
        for (Node emptyNode : emptyNodes) {
            nodeList.addAll(emptyNode.getImmediateChildNodes());
        }
        return nodeList;
    }


    boolean hasImmediateChild(char c) {
        return nodes.containsKey(c);
    }

    boolean hasChild(char c) {
        if(emptyNodes==null) {
            return nodes.containsKey(c);
        }
        for (Node node : emptyNodes) {
            if(node.hasImmediateChild(c)) {
                return true;
            }
        }
        return false;
    }


    Node getImmediateChild(char c) {
        return nodes.get(c);
    }

    void addIfChildExists(char c, List<Node> nodeList) {
        Node child = this.nodes.get(c);
        if (child != null) {
            nodeList.add(child);
        }
    }

    List<Node> getChildList(char[] charArray) {
        List<Node> children = new ArrayList<>(1);
        for (char c : charArray) {
            addIfChildExists(c, children);
            if (emptyNodes != null) {
                for (Node emptyNode : emptyNodes) {
                    emptyNode.addIfChildExists(c, children);
                }
            }
        }
        return children;
    }


    List<Node> getChildList(char c) {
        List<Node> children = new ArrayList<>(1);
        addIfChildExists(c, children);
        if (emptyNodes != null) {
            for (Node emptyNode : emptyNodes) {
                emptyNode.addIfChildExists(c, children);
            }
        }
        return children;
    }

    boolean connect(Node node) {
        if (!nodes.containsKey(node.chr)) {
            nodes.put(node.chr, node);
            return true;
        }
        return false;
    }

    boolean connectEmpty(Node node) {
        if (emptyNodes == null) {
            emptyNodes = new Node[1];
            emptyNodes[0] = node;
        } else {
            for (Node n : emptyNodes) {
                if (n.equals(node)) {
                    return false;
                }
            }
            emptyNodes = Arrays.copyOf(emptyNodes, emptyNodes.length + 1);
            emptyNodes[emptyNodes.length - 1] = node;
        }
        return true;
    }

    public Node[] getEmptyNodes() {
        return emptyNodes;
    }

    Node addChild(int index, char c) {
        Node node = nodes.get(c);
        if (node == null) {
            node = new Node(index, c);
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
