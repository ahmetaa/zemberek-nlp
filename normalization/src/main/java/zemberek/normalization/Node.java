package zemberek.normalization;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

class Node {

  static final int TYPE_EMPTY = 0;
  static final int TYPE_WORD = 1;
  static final int TYPE_ENDING = 2;
  static final int TYPE_GRAPH_ROOT = 3;
  private static final Node[] EMPTY_NODES = new Node[0];

  char chr;
  String word;
  private int index;
  private Node[] children = EMPTY_NODES;
  private Node[] epsilonNodes = null;
  private byte type;

  Node(int index, char chr, int type) {
    this(index, chr, type, null);
  }

  Node(int index, char chr, int type, String word) {
    this.index = index;
    this.chr = chr;
    this.type = (byte) type;
    this.word = word;
  }

  public int getType() {
    return type;
  }

  Iterable<Node> getImmediateChildNodeIterable() {
    return getImmediateChildNodes();
  }

  List<Node> getImmediateChildNodes() {
    return children.length == 0 ? Collections.emptyList() : Arrays.asList(children);
  }

  List<Node> getAllChildNodes() {
    if (epsilonNodes == null) {
      return getImmediateChildNodes();
    }
    int total = children.length;
    for (Node emptyNode : epsilonNodes) {
      total += emptyNode.children.length;
    }
    List<Node> nodeList = new ArrayList<>(total);
    Collections.addAll(nodeList, children);
    for (Node emptyNode : epsilonNodes) {
      Collections.addAll(nodeList, emptyNode.children);
    }
    return nodeList;
  }

  boolean hasEpsilonConnection() {
    return epsilonNodes != null;
  }

  boolean hasImmediateChild(char c) {
    return getImmediateChild(c) != null;
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
    int pos = binarySearch(children, c);
    return pos >= 0 ? children[pos] : null;
  }

  private static int binarySearch(Node[] arr, char c) {
    int low = 0;
    int high = arr.length - 1;
    while (low <= high) {
      int mid = (low + high) >>> 1;
      char midVal = arr[mid].chr;
      if (midVal < c) {
        low = mid + 1;
      } else if (midVal > c) {
        high = mid - 1;
      } else {
        return mid;
      }
    }
    return -(low + 1);
  }

  private void addIfChildExists(char c, List<Node> nodeList) {
    Node child = getImmediateChild(c);
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
    return getOrInsertChild(index, c, type, null, false);
  }

  Node addChild(int index, char c, int type, String word) {
    return getOrInsertChild(index, c, type, word, true);
  }

  private Node getOrInsertChild(int index, char c, int type, String word, boolean updateWord) {
    int pos = binarySearch(children, c);
    if (pos >= 0) {
      Node existing = children[pos];
      if (updateWord) {
        existing.word = word;
        existing.type = (byte) type;
      }
      return existing;
    }
    int insertPos = -(pos + 1);
    Node child = new Node(index, c, type, word);
    Node[] next = new Node[children.length + 1];
    System.arraycopy(children, 0, next, 0, insertPos);
    next[insertPos] = child;
    System.arraycopy(children, insertPos, next, insertPos + 1, children.length - insertPos);
    children = next;
    return child;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
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
    if (children.length > 0) {
      char[] characters = new char[children.length];
      for (int i = 0; i < children.length; i++) {
        characters[i] = children[i].chr;
      }
      sb.append(" children=").append(Arrays.toString(characters));
    }
    if (word != null) {
      sb.append(" word=").append(word);
    }
    sb.append("]");
    return sb.toString();
  }
}
