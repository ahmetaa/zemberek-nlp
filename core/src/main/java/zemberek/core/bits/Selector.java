package zemberek.core.bits;

/**
 * An implementation of select1 algorithm. It finds the n.th 1 bit index in a bit sequence.
 * This algorithm uses two lookup tables.
 */
public class Selector {

    private long oneCount;
    private long[] bigSegments;
    private int[][] smallSegments;
    private LongBitVector bitVector;
    private int bigSegmentSize;
    private int smallSegmentSize;

    long getOneCount() {
        return oneCount;
    }

    public Selector(LongBitVector bitVector) {

        this.bitVector = bitVector;
        oneCount = bitVector.numberOfOnes();

        // calculating segment sizes.
        if (bitVector.size() <= 64) {
            bigSegmentSize = 64;
            smallSegmentSize = 1;
        } else {
            int log2n = (int) log2(oneCount);
            bigSegmentSize = log2n * log2n;
            smallSegmentSize = log2n;
        }

        //adjust bigSegmentSize to align with smal segment .
        for (int k = bigSegmentSize; k >= smallSegmentSize; k--) {
            if (k % smallSegmentSize == 0) {
                bigSegmentSize = k;
                break;
            }
        }

        int bigSegmentAmount = (int) (oneCount / bigSegmentSize) + 1;
        if (oneCount % bigSegmentSize == 0)
            bigSegmentAmount--;

        bigSegments = new long[bigSegmentAmount];
        smallSegments = new int[bigSegmentAmount][bigSegmentSize / smallSegmentSize];

        int smallSegmentCounter = 0, bigSegmentCounter = 0, relativeIndex = 0;
        long bitIndex = 0, oneCounter = 0;
        while (oneCounter < oneCount) {
            // if bit is zero, just increment and continue.
            if (!bitVector.get(bitIndex)) {
                relativeIndex++;
                bitIndex++;
                continue;
            }
            oneCounter++;

            if (oneCounter % smallSegmentSize == 0 || oneCounter == oneCount) {
                smallSegments[bigSegmentCounter][smallSegmentCounter] = relativeIndex;
                smallSegmentCounter++;
            }

            if (oneCounter % bigSegmentSize == 0) {
                if (bigSegmentCounter == 0)
                    bigSegments[bigSegmentCounter] = relativeIndex;
                else
                    bigSegments[bigSegmentCounter] = bigSegments[bigSegmentCounter - 1] + relativeIndex + 1;
                relativeIndex = 0;
                bigSegmentCounter++;
                smallSegmentCounter = 0;
            } else
                relativeIndex++;

            bitIndex++;
        }
        if (oneCount % bigSegmentSize != 0) {
            bigSegments[bigSegmentCounter] = bitVector.getLastBitIndex(true);
        }
    }

    public long select1(long l) {
        if (l > oneCount || l <= 0)
            return -1;
        final int k = (int) (l / bigSegmentSize);
        final int smallLoc = (int) (l % bigSegmentSize);
        final int s = smallLoc / smallSegmentSize;
        final int remainingOnes = smallLoc % smallSegmentSize;
        long bigStartInd = 0, smalStartInd = 0;
        if (k > 0)
            bigStartInd = bigSegments[k - 1] + 1;
        if (s > 0)
            smalStartInd = smallSegments[k][s - 1] + 1;
        long start = bigStartInd + smalStartInd;
        int oneCount = 0;
        while (oneCount < remainingOnes) {
            if (bitVector.get(start++))
                oneCount++;
        }
        return start - 1;
    }

    private static double log2(double n) {
        return Math.log(n) / Math.log(2);
    }

    long averageMemory() {
        return bigSegments.length * 8 + smallSegments.length * smallSegments[0].length * 4;
    }
}
