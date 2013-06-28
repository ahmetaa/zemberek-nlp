package zemberek.core.bits;

import java.io.*;

/**
 * An implementation of a simple integer array compression algorithm.
 * Original algorithm concept details can be found in Kimmo Fredriksson and Fedor Nikitin's
 * Simple Compression Code Supporting Random Access and Fast String Matching paper.
 */
public class DenseIntegerSequence {

    private LongBitVector bitVector;
    private LongBitVector markerVector;
    private Selector selector;
    private int size;

    static final int[] integerBitMasks = new int[32];

    static {
        for (int i = 0; i < 32; i++)
            integerBitMasks[i] = 1 << i;
    }

    /**
     * used only for deserialization.
     *
     * @param bitVector    bitVector for code words.
     * @param markerVector for code start marks.
     * @param size         amount of code words
     */
    private DenseIntegerSequence(LongBitVector bitVector, LongBitVector markerVector, int size) {
        this.bitVector = bitVector;
        this.markerVector = markerVector;
        this.size = size;
        this.selector = new Selector(markerVector);
    }

    /**
     * Generates a compressed data structure for the given integer sequence.
     *
     * @param sequence integer array to compress.
     */
    public DenseIntegerSequence(int[] sequence) {
        this.size = sequence.length;
        bitVector = new LongBitVector(sequence.length * 2, 100);
        markerVector = new LongBitVector(sequence.length * 2, 100);
        for (int i : sequence) {
            bitVector.checkAndEnsureCapacity(32);
            markerVector.checkAndEnsureCapacity(32);
            if (i >= 0 && i < 2) {
                if (i == 0)
                    bitVector.addFast(false);
                else
                    bitVector.addFast(true);
                markerVector.addFast(true);
                continue;
            }
            int msbPos = 32 - Integer.numberOfLeadingZeros(i);
            final int maskToCheck = integerBitMasks[msbPos - 1];
            boolean markerStarted = false;
            while (msbPos-- > 0) {
                boolean k = (i & maskToCheck) != 0;
                bitVector.addFast(k);
                if (!markerStarted) {
                    markerVector.addFast(true);
                    markerStarted = true;
                } else
                    markerVector.addFast(false);
                i = i << 1;
            }
        }
        bitVector.compress();
        markerVector.compress();
        selector = new Selector(markerVector);
    }

    /**
     * retrieves the integer with given index.
     *
     * @param index sequence index.
     * @return integer with given index.
     * @throws IllegalArgumentException  if index is negative
     * @throws IndexOutOfBoundsException if index is larger than sequence size.
     */
    public int get(int index) {
        if (index < 0)
            throw new IllegalArgumentException("Index cannot be negative." + index);
        if (index >= size) {
            throw new IndexOutOfBoundsException("Index cannot be larger than size:" + this.size);
        }
        int selectResult = (int) selector.select1(index + 1);
        if (selectResult < 0)
            return -1;
        boolean msb = bitVector.get(selectResult);
        if (!msb)
            return 0;
        int result = 1;
        boolean b;
        selectResult++;
        while (selectResult < markerVector.size() && !markerVector.get(selectResult)) {
            b = bitVector.get(selectResult);
            if (b)
                result = result << 1 | 1;
            else
                result = result << 1;
            selectResult++;
        }
        return result;
    }

    public long averageMemory() {
        long bitVectorMemory = 2 * bitVector.getLongArray().length * 8;
        System.out.println("Memory for bit vectors containing code words and start markers: " + bitVectorMemory);
        System.out.println("Selector memory:" + selector.averageMemory());
        return bitVectorMemory + selector.averageMemory();
    }

    /**
     * A custom serializer.
     *
     * @param dos DataOutputStream to serialize
     * @throws java.io.IOException if an error occurs while writing.
     */
    public void serialize(DataOutputStream dos) throws IOException {
        dos.writeInt(size);
        serializeLongBitVector(dos, bitVector);
        serializeLongBitVector(dos, markerVector);
    }

    /**
     * A custom serializer.
     *
     * @param file file to serialize
     * @throws java.io.IOException if an error occurs while writing.
     */
    public void serialize(File file) throws IOException {
        try(DataOutputStream dos= new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)))) {
            serialize(dos);
        }
    }

    private void serializeLongBitVector(DataOutputStream dos, LongBitVector vector) throws IOException {
        dos.writeLong(vector.size());
        long[] longs = vector.getLongArray();
        dos.writeInt(longs.length);
        for (long l : longs) {
            dos.writeLong(l);
        }
    }

    private static LongBitVector deserializeLongBitVector(DataInputStream dis) throws IOException {
        long bitVectorSize = dis.readLong();
        int arraySize = dis.readInt();
        long[] arr = new long[arraySize];
        for (int i = 0; i < arr.length; i++) {
            arr[i] = dis.readLong();
        }
        return new LongBitVector(arr, bitVectorSize);
    }

    public static DenseIntegerSequence deserialize(DataInputStream dis) throws IOException {
        int size = dis.readInt();
        return new DenseIntegerSequence(
                deserializeLongBitVector(dis),
                deserializeLongBitVector(dis),
                size
        );
    }

    public static DenseIntegerSequence deserialize(File file) throws IOException {
        try (DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(file)))) {
            return deserialize(dis);
        }
    }

}
