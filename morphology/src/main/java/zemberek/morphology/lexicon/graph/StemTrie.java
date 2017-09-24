package zemberek.morphology.lexicon.graph;

import zemberek.core.collections.IntMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/**
 * StemTrie is a simple compact trie that holds stems
 *
 * @author mdakin@gmail.com
 */
public class StemTrie {

    private Node root = new Node();

    public void add(StemNode stem) {
        if (stem == null) {
            throw new NullPointerException("Input key can not be null");
        }
        char[] chars = stem.surfaceForm.toCharArray();
        Node node = root;
        Node previousNode;
        // i holds the char index for input
        int i = 0;
        // fragmentSplitIndex is the index of the last fragment
        int fragmentSplitIndex;
        // While we still have chars left on the input, or no child marked with s[i]
        // is found in subnodes
        while (node != null) {
            previousNode = node;
            node = node.getChildNode(chars[i]);
            // Cases:
            // root <- foo ==> root-foo*
            // or
            // root-foo* <- bar ==> root-foo*
            //                         \-bar*
            // or
            // root-foo* <- foobar ==> foor-foo*-bar*
            if (node == null) {
                previousNode.addChild(new Node(stem, getSuffix(chars, i)));
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
                        node.addStem(stem);
                        break;
                    }
                    Node newNode = new Node(stem, Arrays.copyOf(node.fragment, fragmentSplitIndex));
                    node.trimLeft(fragmentSplitIndex);
                    newNode.addChild(node);
                    previousNode.addChild(newNode);
                    break;
                }
                // Case:
                // root-foobar* <- foxes ==> root-fo-obar*
                //                                  \-xes*
                if (i < chars.length && fragmentSplitIndex < node.fragment.length) {
                    Node node1 = new Node();
                    node1.setFragment(Arrays.copyOf(node.fragment, fragmentSplitIndex)); // fo
                    previousNode.addChild(node1);
                    node.trimLeft(fragmentSplitIndex); // obar
                    node1.addChild(node);
                    Node node2 = new Node(stem, getSuffix(chars, i)); //xes
                    node1.addChild(node2);
                    break;
                }
            }
        }
    }

    public void remove(StemNode stem) {
        Node node = walkToNode(stem.surfaceForm, null);
        if (node != null && node.hasStem()) {
            node.stems.remove(stem);
        }
    }

    public boolean hasStem(StemNode stem) {
        Node node = walkToNode(stem.surfaceForm, null);
        return  (node != null && node.hasStem());
    }

    private Node walkToNode(String input, Consumer<Node> nodeCallback) {
        Node node = root;
        int i = 0;
        while(i < input.length()) {
            node = node.getChildNode(input.charAt(i));
            // if there are no child node with input char, break
            if (node == null) {
                break;
            }
            char[] fragment = node.fragment;
            int j = 0;
            // Compare fragment and input.
            while (j < fragment.length && i < input.length() && fragment[j++] == input.charAt(i++));
            if (nodeCallback != null) {
                if (j == fragment.length && i <= input.length() && node.hasStem()) {
                    nodeCallback.accept(node);
                }
            }
        }
        return node;
    }

    public List<StemNode> getMatchingStems(String input) {
        List<StemNode> stems = new ArrayList<>();
        walkToNode(input, (node) -> {
            if (node.hasStem()) {
                stems.addAll(node.stems);
            }});
        return stems;
    }

    public String toString() {
        return root != null ? root.dump() : "";
    }

    /**
     * Finds the last position of common chars for 2 char arrays relative to a given index.
     *
     * @param input    input char array to look in the fragment
     * @param start    start index where method starts looking the input in the fragment
     * @param fragment the char array to look input array.
     * @return for input: "foo" fragment = "foobar" index = 0, returns 3
     *         for input: "fool" fragment = "foobar" index = 0, returns 3
     *         for input: "fool" fragment = "foobar" index = 1, returns 2
     *         for input: "foo" fragment = "obar" index = 1, returns 2
     *         for input: "xyzfoo" fragment = "foo" index = 3, returns 2
     *         for input: "xyzfoo" fragment = "xyz" index = 3, returns 0
     *         for input: "xyz" fragment = "abc" index = 0, returns 0
     */
    static int getSplitPoint(char[] input, int start, char[] fragment) {
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

    public static class Node {
        private char[] fragment;
        private ArrayList<StemNode> stems;
        private IntMap<Node> children;

        Node() {}

        Node(StemNode s, char[] fragment) {
            addStem(s);
            setFragment(fragment);
        }

        void trimLeft(int i) {
            setFragment(getSuffix(fragment, i));
        }

        void setFragment(char[] fragment) {
            this.fragment = fragment;
        }

        void addStem(StemNode s) {
            if (stems == null) {
                stems = new ArrayList<>(1);
            }
            if (!stems.contains(s)) {
                stems.add(s);
            }
        }

        void addChild(Node node) {
            if (children == null) {
                children = new IntMap<>(2);
            }
            children.put(node.getChar(), node);
        }

        Node getChildNode(char c) {
            if (children == null) return null;
            return children.get(c);
        }

        @Override
        public String toString() {
            String s = fragment == null ? "#" : new String(fragment);
            if (children != null) {
                s += "( ";
                for (Node node : children.getValues()) {
                    if (node != null) {
                        s += node.getChar() + " ";
                    }
                }
                s += ")";
            } else {
                s += "";
            }
            if (stems != null) {
                for (StemNode stem : stems) {
                    s += " [" + stem.surfaceForm + "]";
                }
            }
            return s;
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
         * @param b     string buffer to append.
         * @param level level of the operation
         */
        private void toDeepString(StringBuffer b, int level) {
            char[] indentChars = new char[level * 2];
            for (int i = 0; i < indentChars.length; i++)
                indentChars[i] = ' ';
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

        boolean hasStem() {
            return (stems != null && stems.size() > 0);
        }

    }
}