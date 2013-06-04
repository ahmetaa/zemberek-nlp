package zemberek.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

/**
 * This is a specialized hash table suitable for short-length long-tail distributed keys.
 * It is designed for compactness. Values of the hash is counts of the keys.
 * <p/>Limitations:
 * <p/>-Key length cannot be longer than 255
 * <p/>-Same key can be added Integer.MAX_VALUE /2 times at max.
 * <p/>
 * <p/>Authors: mdakin, aaakin, hkaya
 */
public class CompactMultiSet implements Iterable<CompactMultiSet.Entry> {

    // Capacity must be power of two
    public static final int INITIAL_CAPACITY = 256;
    private static final int SHORT_COUNT_LIMIT = 127;
    private int bucketSize = INITIAL_CAPACITY;
    private int size = 0;
    private byte contents[][];

    public CompactMultiSet() {
        this(INITIAL_CAPACITY);
    }

    public CompactMultiSet(int initialSize) {
        bucketSize = initialSize;
        contents = new byte[initialSize][0];
    }

    public CompactMultiSet(int bucketSize, int estimatedKeyCount, int bytesPerKey) {
        this.bucketSize = bucketSize;
        long averageBytesRequired = estimatedKeyCount * (bytesPerKey + 2);
        contents = new byte[bucketSize][(int) (averageBytesRequired / bucketSize)];
    }

    int indexFor(int hash) {
        return hash & (bucketSize - 1);
    }

    int countEmpty() {
        int emptySlots = 0;
        for (byte[] content : contents) {
            if (content.length == 0) {
                emptySlots++;
            }
        }
        return emptySlots;
    }

    int getHash(byte input[]) {
        int result = 0x13;
        for (byte b : input) {
            result ^= ((result << 5) + b + (result >>> 2));
        }
        return result & 0x7fffffff;
    }

    /*
     * if input exists, it returns the index of the count data start.
     * Otherwise it returns the next empty slot index (index where next contents size will be written )
     */
    private int locate(byte[] slot, byte[] input) {
        int j = 0;
        while (j < slot.length) {
            // the slot, so we exit and return the last position.
            if (slot[j] == 0) {
                return -j;
            }
            int length = slot[j++] & 0xff;
            // If lengths of arrays are not equal, fast jump to next one
            boolean found = true;
            if (input.length != length) {
                found = false;
            } // If lengths are equal, make a char by char comparison.
            else {
                for (int i = 0; i < length; i++) {
                    if (input[i] != slot[j + i]) {
                        found = false;
                        break;
                    }
                }
            }
            // We found a match. return its payload pointer
            if (found) {
                return j + length;
            }
            // else jump to count data
            j += length;
            // move 1 or 4 bytes according to the count data.
            if ((slot[j] & 0x80) == 0) {
                j++;
            } else {
                j += 4;
            }
        }
        return -j;
    }

    public boolean add(byte[] input) {
        if (input == null || input.length == 0) {
            throw new IllegalArgumentException("Content cannot be null or empty.");
        }
        if (input.length > 255) {
            throw new IllegalArgumentException("Input length cannot be larger than 255.");
        }

        int i = indexFor(getHash(input));
        byte[] slot = this.contents[i];
        int slotlength = slot.length;
        int inputlength = input.length;

        int j = 0;
        if (slotlength != 0) {
            j = locate(slot, input);
            // The key exists, just update the counter
            if (j > 0) {
                int count = decodeCount(slot, j);
                count++;

                if ((Integer.MAX_VALUE >> 1) <= count) {
                    throw new IllegalStateException("Cannot put a count value higher than" + (Integer.MAX_VALUE >> 1));
                }

                // we reached to the 1 byte count limit. We need to expand the count area to 4 bytes.
                if (count == SHORT_COUNT_LIMIT) {
                    final int usedByteAmount = usedByteCount(slot);
                    if (j == usedByteAmount) { // we are the last key in the slot
                        if (j > slotlength - 3) // if there is not enough space in slot, grow it by 3
                        {
                            this.contents[i] = Arrays.copyOf(this.contents[i], slotlength + 3);
                        }
                    } else { // else we need to grow 3 bytes and shift remaining bytes.
                        byte[] updated = Arrays.copyOf(this.contents[i], slotlength + 3);
                        System.arraycopy(this.contents[i], j + 1, updated, j + 4, usedByteAmount - j - 1);
                        this.contents[i] = updated;
                    }
                    slot = this.contents[i];
                    slotlength += 3;
                }
                // encode count.
                if (count < SHORT_COUNT_LIMIT) {
                    slot[j] = (byte) (count & 0xff);
                } else {
                    slot[j] = (byte) (((count >> 24) | 0x80) & 0xff);
                    slot[j + 1] = (byte) ((count >> 16) & 0xff);
                    slot[j + 2] = (byte) ((count >> 8) & 0xff);
                    slot[j + 3] = (byte) (count & 0xff);
                }
                return false;
            } else {
                j = -j;
            }
        }

        // TODO(mdakin) Increase capacity if slots got too big. this requires re-locating all keys to the new buckets.

        // Grow the slot if available space is not enough for input key
        if (inputlength + j + 2 > slotlength) {
            this.contents[i] = Arrays.copyOf(this.contents[i], slotlength + inputlength + 2);
            slot = this.contents[i];
        }

        // Encode length
        slot[j] = (byte) (inputlength & 0xff);
        j += 1;

        // Copy the content
        System.arraycopy(input, 0, slot, j, inputlength);
        j += inputlength;

        //  count = 1.
        slot[j] = 1;
        size++;
        return true;
    }

    private int usedByteCount(byte[] slot) {
        if (slot.length == 0) {
            return 0;
        }
        int j = 0;
        while (j < slot.length) {
            if (slot[j] == 0) {
                return j;
            }
            j += (slot[j] & 0xff) + 1;
            // move 1 or 4 bytes according to the count data.
            if ((slot[j] & 0x80) == 0) {
                j++;
            } else {
                j += 4;
            }
        }
        return j;
    }

    private int unusedByteCount(byte[] slot) {
        return slot.length - usedByteCount(slot);
    }

    private int decodeCount(byte[] slot, int i) {
        if ((slot[i] & 0x80) == 0) {
            return slot[i] & 0xff;
        } else {
            return (slot[i] & 0x7f) << 24 | (slot[i + 1] & 0xff) << 16 | (slot[i + 2] & 0xff) << 8 | slot[i + 3] & 0xff;
        }
    }

    public long totalBytesAllocated() {
        long total = 0;
        for (byte[] content : contents) {
            total += content.length;
        }
        return total;
    }

    public int getSize() {
        return size;
    }

    /**
     * count value for input. -1 if input is not in the set
     *
     * @param input input key
     * @return count value or -1
     */
    public int get(byte[] input) {
        int i = indexFor(getHash(input));
        byte[] slot = this.contents[i];
        if (slot.length == 0) {
            return -1;
        }
        int j = locate(slot, input);
        if (j <= 0) {
            return -1;
        } else {
            return decodeCount(slot, j);
        }
    }

    public Iterator<Entry> iterator() {
        return new CompactMultiSetIterator();
    }

    public Iterable<Entry> sortedIterable() {
        return new Iterable<Entry>() {
            public Iterator<Entry> iterator() {
                return new CompactMultiSetSortedIterator();
            }
        };
    }

    /**
     * Returns an Iterator that gives entries with keys with their bucket  and slot order
     * Suppose hash is holding values as such:
     * <p/>[0]--k6
     * <p/>[1]--
     * <p/>[2]--k3, k1
     * <p/>[3]--k6, k2, k5
     * <p/>
     * <p/>This iterator will give it in this order:
     * <p/>[k6], [k1, k3], [k2, k5, k6]
     * <p/>here we assume that when keys are ordered according to their content it would be k1,k2,k3,k4,k5,k6
     */
    class CompactMultiSetIterator implements Iterator<Entry> {

        int slotIndex;
        byte[] slot;
        int ptr;

        CompactMultiSetIterator() {
            slotIndex = 0;
            ptr = 0;
            slot = contents[0];
        }

        public boolean hasNext() {
            // There is still stuff in slot.
            if (ptr < slot.length && (slot[ptr] & 0xff) != 0) {
                return true;
            }
            while (slotIndex < bucketSize - 1) {
                slotIndex++;
                slot = contents[slotIndex];
                if (slot.length > 0 && slot[0] > 0) {
                    ptr = 0;
                    return true;
                }
            }
            return false;
        }

        public Entry next() {
            int length = slot[ptr++] & 0xff;
            // Get encoded length and copy content.
            byte[] arr = new byte[length];
            System.arraycopy(slot, ptr, arr, 0, length);
            ptr += length;
            // decode count and increment pointer accordingly.
            final int count = decodeCount(slot, ptr);
            if (count < SHORT_COUNT_LIMIT) {
                ptr++;
            } else {
                ptr += 4;
            }
            return new Entry(arr, count);
        }

        public void remove() {
            throw new UnsupportedOperationException("Remove is not supported.");
        }
    }

    /**
     * Returns an Iterator that gives entries with  keys ordered by their content in a slot. This is useful when merging key values
     * Suppose hash is holding values as such:
     * <p/>[0]--k6
     * <p/>[1]--
     * <p/>[2]--k3, k1
     * <p/>[3]--k6, k2, k5
     * <p/>
     * <p/>This iterator will give it in this order:
     * <p/>[k6], [k1, k3], [k2, k5, k6]
     * <p/>here we assume that when keys are ordered according to their content it would be k1,k2,k3,k4,k5,k6
     *
     * @return an Entry iterator.
     */
    public Iterator<Entry> sortedIterator() {
        return new CompactMultiSetSortedIterator();
    }

    class CompactMultiSetSortedIterator implements Iterator<Entry> {

        int slotIndex;
        ArrayList<Entry> entries;
        int ptr;

        CompactMultiSetSortedIterator() {
            slotIndex = -1;
            entries = new ArrayList<Entry>();
            ptr = 0;
        }

        public boolean hasNext() {
            // There is still stuff in entries.
            if (ptr < entries.size()) {
                return true;
            }
            entries.clear();
            ptr = 0;
            while (++slotIndex < bucketSize) {
                readEntries();
                if (entries.size() > 0) {
                    return true;
                }
            }
            return false;
        }

        private void readEntries() {
            byte[] slot = contents[slotIndex];

            if (slot.length == 0 || slot[0] <= 0) {
                return;
            }

            int slotPtr = 0;
            int length;
            byte[] arr;
            int count;

            while (slotPtr < slot.length && (slot[slotPtr] & 0xff) != 0) {
                length = slot[slotPtr++] & 0xff;
                arr = new byte[length];
                System.arraycopy(slot, slotPtr, arr, 0, length);
                slotPtr += length;
                count = decodeCount(slot, slotPtr);
                if (count < SHORT_COUNT_LIMIT) {
                    slotPtr++;
                } else {
                    slotPtr += 4;
                }
                Entry e = new Entry(arr, count);
                boolean added = false;
                for (int i = 0; i < entries.size(); i++) {
                    if (compareByteArrays(arr, entries.get(i).arr) < 0) {
                        entries.add(i, e);
                        added = true;
                        break;
                    }
                }
                if (!added) {
                    entries.add(e);
                }
            }
        }

        public Entry next() {
            return entries.get(ptr++);
        }

        public void remove() {
            throw new UnsupportedOperationException("Remove is not supported.");
        }
    }

    public static class Entry implements Comparable<Entry> {

        public final byte[] arr;
        public final int count;

        Entry(byte[] arr, int count) {
            this.arr = arr;
            this.count = count;
        }

        public int compareTo(Entry o) {
            return this.count - o.count;
        }
    }

    /**
     * Compares two byte array according to their bucket index and their content.
     *
     * @param b1 first key
     * @param b2 second Key
     * @return comparison result.
     */
    public int compareByteArrays(byte[] b1, byte[] b2) {
        int h1 = getHash(b1) & (bucketSize - 1);
        int h2 = getHash(b2) & (bucketSize - 1);

        if (h1 < h2) {
            return -1;
        } else if (h1 > h2) {
            return 1;
        } else {
            if (b1.length < b2.length) {
                return -1;
            } else if (b1.length > b2.length) {
                return 1;
            } else {
                for (int i = 0; i < b1.length; i++) {
                    if (b1[i] < b2[i]) {
                        return -1;
                    } else if (b1[i] > b2[i]) {
                        return 1;
                    }
                }
                return 0;
            }
        }
    }
}
