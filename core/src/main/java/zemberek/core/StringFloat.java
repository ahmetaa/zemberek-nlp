package zemberek.core;

public class StringFloat implements Comparable<StringFloat> {
    public final String string;
    public final float value;

    public StringFloat(String string, float value) {
        this.string = string;
        this.value = value;
    }

    @Override
    public int compareTo(StringFloat o) {
        return Float.compare(o.value, value);
    }

    @Override
    public String toString() {
        return toString(4);
    }

    public String toString(int fractionDigits) {
        return string + ":" + String.format("%." + fractionDigits + "f", value);
    }
}
