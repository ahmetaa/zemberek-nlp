package zemberek.core.math;

import com.google.common.base.Splitter;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static java.lang.Math.abs;
import static java.lang.Math.sqrt;

public class DoubleArrays {

    public static final double[] ZERO_LENGTH_ARRAY = new double[0];

    // do not allow instantiation
    private DoubleArrays() {
    }

    /**
     * @return true if difference is smaller or equal to range
     */
    public static boolean inRange(double d1, double d2, double range) {
        return Math.abs(d1 - d2) <= range;
    }

    /**
     * @param input double array
     * @return reverse of the double array
     */
    public static double[] reverse(double[] input) {
        double[] result = new double[input.length];
        for (int i = 0; i < input.length; i++) {
            result[input.length - i - 1] = input[i];
        }
        return result;
    }

    /**
     * @param input int array
     * @return double array converted from input int array
     */
    public static double[] convert(int[] input) {
        double[] data = new double[input.length];
        int k = 0;
        for (int i : input) {
            data[k++] = i;
        }
        return data;
    }

    /**
     * @param input 2d int array
     * @return 2d double array converted from 2d int array
     */
    public static double[][] convert(int[][] input) {
        double[][] data = new double[input.length][];
        int k = 0;
        for (int[] i : input) {
            data[k] = new double[i.length];
            int j = 0;
            for (int ii : i) {
                data[k][j++] = ii;
            }
            k++;
        }
        return data;
    }

    /**
     * @param input float array
     * @return double array converted from input float array
     */
    public static double[] convert(float[] input) {
        double[] data = new double[input.length];
        int k = 0;
        for (float i : input) {
            data[k++] = i;
        }
        return data;
    }

    /**
     * @param d1    input double array
     * @param d2    input double array
     * @param range double input
     * @return true if the difference between elements of d1 and d2 is smaller than or equal to given range
     */
    public static boolean arrayEqualsInRange(double[] d1, double[] d2, double range) {
        validateArrays(d1, d2);
        for (int i = 0; i < d1.length; i++) {
            if (Math.abs(d1[i] - d2[i]) > range) {
                return false;
            }
        }
        return true;
    }

    /**
     * @return true if two input double arrays are equal
     */
    public static boolean arrayEquals(double[] d1, double[] d2) {
        validateArrays(d1, d2);
        return Arrays.equals(d1, d2);
    }

    /**
     * @return the double array after appending zeros to its end with the given amount
     * @throws IllegalArgumentException when amount input is negative
     */
    public static double[] appendZeros(double[] darray, int zeroAmountToAppend) {
        if (zeroAmountToAppend < 0) {
            throw new IllegalArgumentException("Cannot append negative amount of zeros. Amount:" + zeroAmountToAppend);
        }
        return Arrays.copyOf(darray, darray.length + zeroAmountToAppend);
    }

    public static double[] normalize16bitLittleEndian(byte[] bytez) {
        return normalize16bitLittleEndian(bytez, bytez.length);
    }

    /**
     * @param bytez  input byte array
     * @param amount input, size of the byte array
     * @return double array inncluding the normalized double value of each byte elements as Little-Endian representation
     * For 0xABCD:
     * Big-Endian Rep.-->0xABCD
     * Little-Endian Rep-->0xCDBA
     */
    public static double[] normalize16bitLittleEndian(byte[] bytez, int amount) {
        if ((amount & 1) != 0) {
            throw new IllegalArgumentException("Amount of bytes must be an order of 2. But it is: " + amount);
        }
        double[] result = new double[amount / 2];
        for (int i = 0; i < amount; i += 2) {
            final int val = (short) (bytez[i + 1] << 8) | (bytez[i] & 0xff);
            if (val >= 0) {
                result[i >>> 1] = (double) val / Short.MAX_VALUE;
            } else {
                result[i >>> 1] = -(double) val / Short.MIN_VALUE;
            }
        }
        return result;
    }

    /**
     * @param input input double array
     * @return byte array including the de-normalized 16-bit Big-Endian representations of double values in double array
     */
    public static byte[] denormalize16BitLittleEndian(double[] input) {
        byte[] result = new byte[input.length * 2];
        for (int i = 0; i < input.length; i++) {
            int denorm;
            if (input[i] < 0) {
                denorm = (int) (-input[i] * Short.MIN_VALUE);
            } else {
                denorm = (int) (input[i] * Short.MAX_VALUE);
            }
            result[i * 2] = (byte) (denorm & 0xff);
            result[i * 2 + 1] = (byte) (denorm >>> 8);
        }
        return result;
    }

    /**
     * @param input         input double array
     * @param bitsPerSample input as bit number
     * @return byte array including the de-normalized n-bit Big-Endian representations of double values in double array where n is bitsPerSample
     */
    static byte[] denormalizeLittleEndian(double[] input, int bitsPerSample) {
        int bytesPerSample = bitsPerSample % 8 == 0 ? bitsPerSample / 8 : bitsPerSample / 8 + 1;
        int maxVal = 1 << bitsPerSample - 1;
        byte[] result = new byte[input.length * bytesPerSample];
        for (int i = 0; i < input.length; i++) {
            int denorm;
            if (input[i] < 0) {
                denorm = (int) (-input[i] * maxVal);
            } else {
                denorm = (int) (input[i] * maxVal);
            }
            for (int j = 0; j < bytesPerSample; j++) {
                result[i * bytesPerSample + j] = (byte) ((denorm >>> j * 8) & 0xff);
            }
        }
        return result;
    }

    /**
     * gets a double array with values between -1.0 and 1.0 and converts it to
     * an integer in the range of [0,max]
     *
     * @param input double array
     * @param max   max integer value.
     * @return an integer array/
     */
    public static int[] toUnsignedInteger(double[] input, int max) {
        if (max < 1) {
            throw new IllegalArgumentException("Maximum int value must be positive. But it is:" + max);
        }
        int[] iarr = new int[input.length];
        double divider = (double) max / 2.0;
        for (int i = 0; i < input.length; i++) {
            double d = input[i];
            if (d < -1.0 || d > 1.0) {
                throw new IllegalArgumentException("Array value should be between -1.0 and 1.0. But it is: " + d);
            }
            iarr[i] = (int) (input[i] * divider);
        }
        return iarr;
    }

    /**
     * finds the maximum value of an array.
     *
     * @param input input array
     * @return maximum value.
     * @throws IllegalArgumentException if array is empty or null.
     */
    public static double max(double... input) {
        validateArray(input);
        double max = input[0];
        for (int i = 1; i < input.length; i++) {
            if (input[i] > max) {
                max = input[i];
            }
        }
        return max;
    }

    /**
     * finds the minimum value of an array.
     *
     * @param input input array
     * @return minimum value.
     * @throws IllegalArgumentException if array is empty or null.
     */
    public static double min(double... input) {
        validateArray(input);
        double min = input[0];
        for (int i = 1; i < input.length; i++) {
            if (input[i] < min) {
                min = input[i];
            }
        }
        return min;
    }

    /**
     * checks whether the input array is null or empty
     *
     * @param input input double array
     */
    public static void validateArray(double... input) {
        if (input == null) {
            throw new IllegalArgumentException("array is null!");
        } else if (input.length == 0) {
            throw new IllegalArgumentException("array is empty!");
        }
    }

    /**
     * @param input input array
     * @return index at which the maximum value of input is, minimum index is returned when multiple maximums
     */
    public static int maxIndex(double... input) {
        validateArray(input);
        double max = input[0];
        int index = 0;
        for (int i = 1; i < input.length; i++) {
            if (input[i] > max) {
                max = input[i];
                index = i;
            }
        }
        return index;
    }

    /**
     * @param input input array
     * @return index at which the minimum value element of input is, minimum index is returned when multiple minimums
     */
    public static int minIndex(double... input) {
        validateArray(input);
        double min = input[0];
        int minIndex = 0;
        for (int i = 1; i < input.length; i++) {
            if (input[i] < min) {
                min = input[i];
                minIndex = i;
            }
        }
        return minIndex;
    }

    /**
     * @param input input
     * @return sum of all elements in array
     */
    public static double sum(double... input) {
        double sum = 0;
        for (double v : input) {
            sum += v;
        }
        return sum;
    }

    /**
     * @param a1 input
     * @param a2 input
     * @return double array of which elements are the sum of 2 input arrays' elements
     */
    public static double[] sum(double[] a1, double[] a2) {
        validateArrays(a1, a2);
        double[] sum = new double[a1.length];
        for (int i = 0; i < a1.length; i++) {
            sum[i] = a1[i] + a2[i];
        }
        return sum;
    }

    /**
     * sums two double vector. result is written to first vector.
     *
     * @param first  first vector.
     * @param second second vector
     */
    public static void addToFirst(double[] first, double[] second) {
        validateArrays(first, second);
        for (int i = 0; i < first.length; i++) {
            first[i] = first[i] + second[i];
        }
    }

    /**
     * Adds a value to all elements of the [data] array.
     */
    public static void addToAll(double[] data, double valueToAdd) {
        validateArray(data);
        for (int i = 0; i < data.length; i++) {
            data[i] += valueToAdd;
        }
    }


    /**
     * sums two double vectors (second vector is scaled by scale factor).
     * result is written to first vector.
     *
     * @param first  first vector.
     * @param second second vector
     * @param scale  scale factor for second
     */
    public static void addToFirstScaled(double[] first, double[] second, double scale) {
        validateArrays(first, second);
        for (int i = 0; i < first.length; i++) {
            first[i] = first[i] + second[i] * scale;
        }
    }

    /**
     * @param input input double array
     * @return an array containing square-values of the input array's elements
     */
    public static double[] square(double... input) {
        double[] res = new double[input.length];
        for (int i = 0; i < input.length; i++) {
            res[i] = input[i] * input[i];
        }
        return res;
    }

    public static void squareInPlace(double... input) {
        for (int i = 0; i < input.length; i++) {
            input[i] = input[i] * input[i];
        }
    }

    /**
     * substracts two double vector.
     *
     * @param a1 first vector.
     * @param a2 second vector
     * @return substraction result
     */
    public static double[] subtract(double[] a1, double[] a2) {
        validateArrays(a1, a2);
        double[] diff = new double[a1.length];
        for (int i = 0; i < a1.length; i++) {
            diff[i] = a1[i] - a2[i];
        }
        return diff;
    }

    /**
     * substracts two double vector. result is written to first vector.
     *
     * @param first  first vector.
     * @param second second vector
     */
    public static void subtractFromFirst(double[] first, double[] second) {
        validateArrays(first, second);
        for (int i = 0; i < first.length; i++) {
            first[i] = first[i] - second[i];
        }
    }

    /**
     * @param a1 input double array
     * @param a2 input double array
     * @return the array produced after multiplying the elements of input arrays
     */
    public static double[] multiply(double[] a1, double[] a2) {
        validateArrays(a1, a2);
        double[] mul = new double[a1.length];
        for (int i = 0; i < a1.length; i++) {
            mul[i] = a1[i] * a2[i];
        }
        return mul;
    }

    /**
     * @param a1 input double array
     * @param a2 input double array
     * @return the dot product value of elements in input arrays
     */

    public static double dotProduct(double[] a1, double[] a2) {
        return sum(multiply(a1, a2));
    }

    /**
     * multiplies two double vectors and result is written to the first vector.
     *
     * @param first  first vector
     * @param second second vector.
     */
    public static void multiplyToFirst(double[] first, double[] second) {
        validateArrays(first, second);
        for (int i = 0; i < first.length; i++) {
            first[i] = first[i] * second[i];
        }
    }

    /**
     * Multiplies all elements of a vector with a double number and returns a
     * new vector
     *
     * @param a1 vector
     * @param b  scale factor
     * @return new scaled vector
     */
    public static double[] scale(double[] a1, double b) {
        validateArray(a1);
        double[] mul = new double[a1.length];
        for (int i = 0; i < a1.length; i++) {
            mul[i] = a1[i] * b;
        }
        return mul;
    }

    /**
     * Multiplies all elements of a vector with a double number
     *
     * @param a1 vector
     * @param b  scale factor
     */
    public static void scaleInPlace(double[] a1, double b) {
        validateArray(a1);
        for (int i = 0; i < a1.length; i++) {
            a1[i] = a1[i] * b;
        }
    }

    /**
     * Calculates mean of a vector.
     *
     * @param input double array
     * @return mean
     */
    public static double mean(double... input) {
        validateArray(input);
        return sum(input) / input.length;
    }

    /**
     * for A=[a0, a1, ...,an]
     * for B=[b0, b1, ...,bn]
     * returns C=|a0-b0|+|a1-b1|+...+|an-bn|
     *
     * @param a input array a
     * @param b input array b
     * @return squared sum of array elements.
     */
    public static double absoluteSumOfDifferences(double[] a, double[] b) {
        return sum(absoluteDifference(a, b));
    }

    /**
     * for A=[a0, a1, ...,an]
     * for B=[b0, b1, ...,bn]
     * returns C=[|a0-b0|,|a1-b1|,...,|an-bn|]
     *
     * @param a input array a
     * @param b input array b
     * @return squared sum of array elements.
     */
    public static double[] absoluteDifference(double[] a, double[] b) {
        validateArrays(a, b);
        double[] diff = new double[a.length];
        for (int i = 0; i < a.length; i++) {
            diff[i] += abs(a[i] - b[i]);
        }
        return diff;
    }


    /**
     * checks whether one of the input arrays are null or not, and whether their length is equal or not
     *
     * @param a1 input double array
     * @param a2 input double array
     */
    public static void validateArrays(double[] a1, double[] a2) {
        if (a1 == null) {
            throw new NullPointerException("first array is null!");
        }
        if (a2 == null) {
            throw new NullPointerException("second array is null!");
        }
        if (a1.length != a2.length) {
            throw new IllegalArgumentException("Array sizes must be equal. But, first:"
                    + a1.length + ", and second:" + a2.length);
        }
    }

    /**
     * for A=[a0, a1, ...,an] returns a0*a0+a1*a1+....+an*an
     *
     * @param array input array
     * @return squared sum of array elements.
     */
    public static double squaredSum(double[] array) {
        double result = 0;
        validateArray(array);
        for (double a : array) {
            result += a * a;
        }
        return result;
    }

    public static double squaredSumOfDifferences(double[] a, double[] b) {
        return (squaredSum(subtract(a, b)));
    }

    /**
     * @param input input double array
     * @return variance value of the elements in the input array
     */
    public static double variance(double[] input) {
        double sigmaSquare = 0;
        double mean = mean(input);
        for (double a : input) {
            final double meanDiff = a - mean;
            sigmaSquare += meanDiff * meanDiff;
        }
        return sigmaSquare / (input.length - 1);
    }

    /**
     * @param a input double array
     * @return standard deviation value of the elements in the input array
     */
    public static double standardDeviation(double[] a) {
        return sqrt(variance(a));
    }

    /**
     * @param a input double array
     * @return true if array includes at least one Not-a-Number (NaN) value, false otherwise
     */
    public static boolean containsNaN(double[] a) {
        for (double v : a) {
            if (Double.isNaN(v))
                return true;
        }
        return false;
    }

    /**
     * replaces the elements smaller than minValue with the minValue
     *
     * @param var      input double array
     * @param minValue double
     */
    public static void floorInPlace(double[] var, double minValue) {
        for (int k = 0; k < var.length; k++) {
            if (var[k] < minValue)
                var[k] = minValue;
        }
    }

    /**
     * If a data point is non-zero and below 'floor' make it equal to floor
     *
     * @param data  the data to floor
     * @param floor the floored value
     */
    public static void nonZeroFloorInPlace(double[] data, double floor) {
        for (int i = 0; i < data.length; i++) {
            if (data[i] != 0.0 && data[i] < floor) {
                data[i] = floor;
            }
        }
    }

    /**
     * Normalize the given data.
     *
     * @param data the data to normalize
     */
    public static void normalizeInPlace(double[] data) {
        double sum = sum(data);
        scaleInPlace(data, 1d / sum);
    }

    public static void serialize(DataOutputStream dos, double[] data) throws IOException {
        dos.writeInt(data.length);
        for (double v : data) {
            dos.writeDouble(v);
        }
    }

    public static void serializeRaw(DataOutputStream dos, double[] data) throws IOException {
        for (double v : data) {
            dos.writeDouble(v);
        }
    }


    public static void serialize(DataOutputStream dos, double[][] data) throws IOException {
        dos.writeInt(data.length);
        for (double[] doubles : data) {
            serialize(dos, doubles);
        }
    }

    public static void serializeRaw(DataOutputStream dos, double[][] data) throws IOException {
        for (double[] doubles : data) {
            serializeRaw(dos, doubles);
        }
    }


    public static double[] deserialize(DataInputStream dis) throws IOException {
        int amount = dis.readInt();
        double[] result = new double[amount];
        for (int i = 0; i < amount; i++) {
            result[i] = dis.readDouble();
        }
        return result;
    }

    public static double[] deserializeRaw(DataInputStream dis, int amount) throws IOException {
        double[] result = new double[amount];
        for (int i = 0; i < amount; i++) {
            result[i] = dis.readDouble();
        }
        return result;
    }

    public static void deserializeRaw(DataInputStream dis, double[] result) throws IOException {
        for (int i = 0; i < result.length; i++) {
            result[i] = dis.readDouble();
        }
    }

    public static double[][] deserialize2d(DataInputStream dis) throws IOException {
        int amount = dis.readInt();
        double[][] result = new double[amount][];
        for (int i = 0; i < amount; i++) {
            result[i] = deserialize(dis);
        }
        return result;
    }

    public static void deserialize2DRaw(DataInputStream dis, double[][] result) throws IOException {
        for (double[] row : result) {
            deserializeRaw(dis, row);
        }
    }

    public static double[][] clone2D(double[][] result) throws IOException {
        double[][] arr = new double[result.length][];
        for (int i = 0; i < arr.length; i++) {
            arr[i] = result[i].clone();
        }
        return arr;
    }


    public static double[] fromString(String str, String delimiter) {
        List<String> tokens = Splitter.on(delimiter).trimResults().omitEmptyStrings().splitToList(str);
        double[] result = new double[tokens.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = Float.parseFloat(tokens.get(i));
        }
        return result;
    }

    /**
     * Formats a float array as string using English Locale.
     */
    public static String format(double... input) {
        return format(10, 3, " ", input);
    }

    /**
     * Formats a float array as string using English Locale.
     */
    public static String format(int fractionDigits, String delimiter, double... input) {
        StringBuilder sb = new StringBuilder();
        String formatStr = "%." + fractionDigits + "f";
        int i = 0;
        for (double v : input) {
            sb.append(String.format(Locale.ENGLISH, formatStr, v));
            if (i++ < input.length - 1) sb.append(delimiter);
        }
        return sb.toString();
    }

    /**
     * Formats a float array as string using English Locale.
     */
    public static String format(int rightPad, int fractionDigits, String delimiter, double... input) {
        StringBuilder sb = new StringBuilder();
        String formatStr = "%." + fractionDigits + "f";
        int i = 0;
        for (double v : input) {
            String num = String.format(formatStr, v);
            sb.append(String.format(Locale.ENGLISH, "%-" + rightPad + "s", num));
            if (i++ < input.length - 1) sb.append(delimiter);
        }
        return sb.toString().trim();
    }


    public static double[] reduceFractionDigits(double[] arr, int digitCount) {
        if (digitCount < 1 || digitCount > 10) {
            throw new IllegalArgumentException("Digit count cannot be less than 1 or more than 10");
        }
        double[] newArr = new double[arr.length];
        int powerOfTen = (int) Math.pow(10, digitCount);
        for (int i = 0; i < arr.length; i++) {
            double val = arr[i] * powerOfTen;
            val = Math.round(val);
            val = val / powerOfTen;
            newArr[i] = val;
        }
        return newArr;
    }

}
