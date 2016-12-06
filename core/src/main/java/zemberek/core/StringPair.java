package zemberek.core;

public class StringPair {
    public final String first;
    public final String second;

    public StringPair(String first, String second) {
        this.first = first;
        this.second = second;
    }

    public static StringPair fromStringLastDelimiter(String str, char delimiter) {
        int index = str.lastIndexOf(delimiter);
        return fromString(str, index);
    }

    public static StringPair fromString(String str, char delimiter) {
        int index = str.indexOf(delimiter);
        return fromString(str, index);
    }

    private static StringPair fromString(String str, int delimiterPos) {
        if (delimiterPos == -1) {
            throw new IllegalArgumentException("Cannot extract two string from : [" + str + "]");
        }
        String first = str.substring(0, delimiterPos).trim();
        String second = str.substring(delimiterPos).trim();
        return new StringPair(first, second);
    }

    public static StringPair fromString(String str) {
        return fromString(str, ' ');
    }

}
