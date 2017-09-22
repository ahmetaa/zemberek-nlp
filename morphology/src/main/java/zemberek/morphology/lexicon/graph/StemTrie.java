package zemberek.morphology.lexicon.graph;

import zemberek.core.collections.IntMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

    public String toString() {
        return root != null ? root.dump() : "";
    }

    public List<StemNode> getMatchingStems(String input) {
        Node node = root;
        int index = 0;
        String s = "";
        List<StemNode> objects = new ArrayList<>();
        while (index < input.length()) {
            node = node.getChildNode(input.charAt(index));
            if (node == null) break;
            String nodeString = node.getFragment();
            s += nodeString;
            if (input.startsWith(s) && node.hasObject()) {
                objects.addAll(node.stems);
            }
            index += nodeString.length();
        }
        return objects;
    }

    public static class Node {
        private char[] fragment;
        private ArrayList<StemNode> stems;
        private IntMap<Node> children_;

        public Node() {
        }

        public Node(StemNode s, char[] fragment) {
            addStem(s);
            setFragment(fragment);
        }

        public void trimLeft(int i) {
            setFragment(getSuffix(fragment, i));
        }

        public void setFragment(char[] fragment) {
            this.fragment = fragment;
        }

        public void addStem(StemNode s) {
            if (stems == null) {
                stems = new ArrayList<>(1);
            }
            if (!stems.contains(s)) {
                stems.add(s);
            }
        }

        public void addChild(Node node) {
            if (children_ == null) {
                children_ = new IntMap<>(2);
            }
            children_.put(node.getChar(), node);
        }

        public String getFragment() {
            return fragment == null ? "#" : new String(fragment);
        }

        public Node getChildNode(char c) {
            if (children_ == null) return null;
            return children_.get(c);
        }

        @Override
        public String toString() {
            String s = getFragment() + " : ";
            if (children_ != null) {
                s += "( ";
                for (Node node : children_.getValues()) {
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
            if (children_ != null) {
                for (Node subNode : this.children_.getValues()) {
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

        public boolean hasObject() {
            return (stems != null && stems.size() > 0);
        }

    }
}