package zemberek.normalization;

import zemberek.core.collections.UIntMap;

import java.util.Arrays;
import java.util.List;

class Node {

    private int index;
    char chr;
    private UIntMap<Node> nodes = new UIntMap<>(2);
    String word;

    Node(int index, char chr) {
        this.index = index;
        this.chr = chr;
    }

    Iterable<Node> getChildNodeIterable() {
        return nodes;
    }

    List<Node> getChildNodes() {
        return nodes.getValues();
    }


    boolean hasChild(char c) {
        return nodes.containsKey(c);
    }

    Node getChild(char c) {
        return nodes.get(c);
    }

    boolean connect(Node node) {
        if (!nodes.containsKey(node.chr)) {
            nodes.put(node.chr, node);
            return true;
        }
        return false;
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
