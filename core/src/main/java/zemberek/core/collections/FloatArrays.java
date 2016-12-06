package zemberek.core.collections;

import com.google.common.base.Splitter;

import java.util.List;

public class FloatArrays {

    public static float sum(float[] input) {
        float total = 0;
        for (float v : input) {
            total += v;
        }
        return total;
    }

    public static void addInPlace(float[] input, float value) {
        for (int i = 0; i < input.length; i++) {
            input[i] += value;
        }
    }

    public static void subtractInPlace(float[] input, float value) {
        for (int i = 0; i < input.length; i++) {
            input[i] -= value;
        }
    }

    public static float mean(float[] input) {
        if (input.length == 0) {
            throw new IllegalArgumentException("Cannot calculate mean of empty array.");
        }
        return sum(input) / input.length;
    }


    public static void multiplyToFirst(float[] first, float[] second) {
        checkArrays(first, second);
        for (int i = 0; i < first.length; i++) {
            first[i] = first[i] * second[i];
        }
    }

    public static void subtractFromFirst(float[] first, float[] second) {
        checkArrays(first, second);
        for (int i = 0; i < first.length; i++) {
            first[i] = first[i] - second[i];
        }
    }


    public static void scaleInPlace(float[] first, float value) {
        for (int i = 0; i < first.length; i++) {
            first[i] = first[i] * value;
        }
    }

    public static void addToFirstScaled(float[] first, float[] second, float scale) {
        checkArrays(first, second);
        for (int i = 0; i < first.length; i++) {
            first[i] = first[i] + second[i] * scale;
        }
    }


    public static float dotProduct(float[] first, float[] second) {
        checkArrays(first, second);
        float sum = 0;
        for (int i = 0; i < first.length; i++) {
            sum += first[i] * second[i];
        }
        return sum;
    }

    public static float[] fromDelimitedString(String input, String delimiter) {
        List<String> tokens = Splitter.on(delimiter).omitEmptyStrings().trimResults().splitToList(input);
        float[] result = new float[tokens.size()];
        for (int i = 0; i < tokens.size(); i++) {
            result[i] = Float.parseFloat(tokens.get(i));
        }
        return result;
    }


    public static void checkArrays(float[] first, float[] second) {
        if (first == null) {
            throw new IllegalArgumentException("First array is null.");
        }
        if (second == null) {
            throw new IllegalArgumentException("Second array is null.");
        }
        if (first.length != second.length) {
            throw new IllegalArgumentException("Array lengths are not equal. First is "
                    + first.length + " second is " + second.length);
        }
    }

}
