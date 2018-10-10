package zemberek.core.collections;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A simple compact trie.
 *
 * @author mdakin@gmail.com
 */
public class Trie<T> {

  private Node<T> root = new Node<>();
  private int size = 0;

  public void add(String s, T item) {
    if (item == null) {
      throw new NullPointerException("Input key can not be null");
    }
    char[] chars = s.toCharArray();
    Node<T> node = root;
    Node<T> previousNode;
    // i holds the char index for input
    int i = 0;
    // fragmentSplitIndex is the index of the last fragment
    int fragmentSplitIndex;
    // While we still have chars left on the input, or no child marked with s[i]
    // is found in sub-nodes
    while (node != null) {
      previousNode = node;
      node = node.getChildNode(chars[i]);
      // Cases:
      // root <- foo ==> root-foo*
      // or
      // root-foo* <- bar ==> root-foo*
      //                         \-bar*
      // or
      // root-foo* <- foobar ==> root-foo*-bar*
      if (node == null) {
        previousNode.addChild(new Node<>(item, getSuffix(chars, i)));
        size++;
        return;
      } else {
        fragmentSplitIndex = getSplitPoint(chars, i, node.fragment);
        i += fragmentSplitIndex;
        // Case:
        // root-foobar* <- foo ==> root-foo*-bar*
        // or
        // root-fo-obar* <-foo ==> root-fo-o*-bar*
        //        \-x*                    \-x*
        // or Homonym:
        // root-foo* <-- foo ==> root-foo**
        if (i == chars.length) {
          // Homonym
          if (fragmentSplitIndex == node.fragment.length) {
            if (node.addItem(item)) {
              size++;
            }
            break;
          }
          Node<T> newNode = new Node<>(item, Arrays.copyOf(node.fragment, fragmentSplitIndex));
          size++;
          node.trimLeft(fragmentSplitIndex);
          newNode.addChild(node);
          previousNode.addChild(newNode);
          break;
        }
        // Case:
        // root-foobar* <- foxes ==> root-fo-obar*
        //                                  \-xes*
        if (i < chars.length && fragmentSplitIndex < node.fragment.length) {
          Node<T> node1 = new Node<>();
          node1.setFragment(Arrays.copyOf(node.fragment, fragmentSplitIndex)); // fo
          previousNode.addChild(node1);
          node.trimLeft(fragmentSplitIndex); // obar
          node1.addChild(node);
          Node<T> node2 = new Node<>(item, getSuffix(chars, i)); //xes
          size++;
          node1.addChild(node2);
          break;
        }
      }
    }
  }

  // Remove does not apply compaction, just removes the item from node.
  public void remove(String s, T item) {
    Node node = walkToNode(s);
    if (node != null && node.hasItem()) {
      node.items.remove(item);
      size--;
    }
  }

  public int size() {
    return size;
  }

  public boolean containsItem(String s, T item) {
    Node<T> node = walkToNode(s);
    return (node != null && node.items.contains(item));
  }

  public List<T> getItems(String s) {
    Node<T> node = walkToNode(s);
    return node == null ? new ArrayList<>(0) : new ArrayList<>(node.items);
  }

  public List<T> getAll() {
    List<T> items = new ArrayList<>(size);
    List<Node<T>> toWalk = Lists.newArrayList(root);
    while (toWalk.size() > 0) {
      List<Node<T>> n = new ArrayList<>();
      for (Node<T> tNode : toWalk) {
        if (tNode.hasItem()) {
          items.addAll(tNode.items);
        }
        if (tNode.children != null && tNode.children.size() > 0) {
          n.addAll(tNode.children.getValues());
        }
      }
      toWalk = n;
    }
    return items;
  }

  public List<T> getPrefixMatchingItems(String input) {
    List<T> items = new ArrayList<>(2);
    Node<T> node = root;
    char[] chars = input.toCharArray();
    int i = 0;
    mainLoop:
    while (i < chars.length) {
      node = node.getChildNode(chars[i]);
      // if there are no child node with input char, break
      if (node == null) {
        break;
      }
      char[] fragment = node.fragment;
      // Compare fragment and input.
      int j;
      for (j = 0; j < fragment.length && i < chars.length; j++, i++) {
        if (fragment[j] != chars[i]) {
          break mainLoop;
        }
      }
      if (j == fragment.length) {
        if (node.hasItem()) {
          items.addAll(node.items);
        }
      } else {
        // no need to go further
        break;
      }
    }
    return items;
  }

  public String toString() {
    return root != null ? root.dump() : "";
  }

  private Node<T> walkToNode(String input) {
    Node<T> node = root;
    int i = 0;
    while (i < input.length()) {
      node = node.getChildNode(input.charAt(i));
      // if there are no child node with input char, break
      if (node == null) {
        break;
      }
      char[] fragment = node.fragment;
      // Compare fragment and input.
      int j;
      //TODO: code below may be simplified
      for (j = 0; j < fragment.length && i < input.length(); j++, i++) {
        if (fragment[j] != input.charAt(i)) {
          break;
        }
      }
    }
    return node;
  }

  /**
   * Finds the last position of common chars for 2 char arrays relative to a given index.
   *
   * @param input input char array to look in the fragment
   * @param start start index where method starts looking the input in the fragment
   * @param fragment the char array to look input array.
   * @return <pre>
   * for input: "foo" fragment = "foobar" index = 0, returns 3
   * for input: "fool" fragment = "foobar" index = 0, returns 3
   * for input: "fool" fragment = "foobar" index = 1, returns 2
   * for input: "foo" fragment = "obar" index = 1, returns 2
   * for input: "xyzfoo" fragment = "foo" index = 3, returns 2
   * for input: "xyzfoo" fragment = "xyz" index = 3, returns 0
   * for input: "xyz" fragment = "abc" index = 0, returns 0
   * </pre>
   */
  private static int getSplitPoint(char[] input, int start, char[] fragment) {
    int fragmentIndex = 0;
    while (start < input.length && fragmentIndex < fragment.length
        && input[start++] == fragment[fragmentIndex]) {
      fragmentIndex++;
    }
    return fragmentIndex;
  }

  private static char[] getSuffix(char[] arr, int index) {
    char[] res = new char[arr.length - index];
    System.arraycopy(arr, index, res, 0, arr.length - index);
    return res;
  }

  public static class Node<T> {

    private char[] fragment;
    private List<T> items;
    private IntMap<Node<T>> children;

    Node() {
    }

    Node(T s, char[] fragment) {
      addItem(s);
      setFragment(fragment);
    }

    void trimLeft(int i) {
      setFragment(getSuffix(fragment, i));
    }

    void setFragment(char[] fragment) {
      this.fragment = fragment;
    }

    boolean addItem(T item) {
      if (items == null) {
        items = new ArrayList<>(1);
      }
      if (!items.contains(item)) {
        items.add(item);
        return true;
      } else {
        return false;
      }

    }

    void addChild(Node<T> node) {
      if (children == null) {
        children = new IntMap<>(4);
      }
      children.put(node.getChar(), node);
    }

    Node<T> getChildNode(char c) {
      if (children == null) {
        return null;
      }
      return children.get(c);
    }

    @Override
    public String toString() {
      StringBuilder s = new StringBuilder(fragment == null ? "#" : new String(fragment));
      if (children != null) {
        s.append("( ");
        for (Node node : children.getValues()) {
          if (node != null) {
            s.append(node.getChar()).append(" ");
          }
        }
        s.append(")");
      }
      if (items != null) {
        for (T item : items) {
          s.append(" [").append(item).append("]");
        }
      }
      return s.toString();
    }

    private char getChar() {
      if (fragment == null) {
        return '#';
      }
      return fragment[0];
    }

    /**
     * Returns string representation of node and all child nodes until leafs.
     *
     * @param b string buffer to append.
     * @param level level of the operation
     */
    private void toDeepString(StringBuffer b, int level) {
      char[] indentChars = new char[level * 2];
      for (int i = 0; i < indentChars.length; i++) {
        indentChars[i] = ' ';
      }
      b.append(indentChars).append(this.toString());
      b.append("\n");
      if (children != null) {
        for (Node subNode : this.children.getValues()) {
          if (subNode != null) {
            subNode.toDeepString(b, level + 1);
          }
        }
      }
    }

    /**
     * Returns string representation of Node (and subnodes) for testing.
     *
     * @return String representation of trie.
     */
    public final String dump() {
      StringBuffer b = new StringBuffer();
      toDeepString(b, 0);
      return b.toString();
    }

    boolean hasItem() {
      return (items != null && items.size() > 0);
    }

  }
}