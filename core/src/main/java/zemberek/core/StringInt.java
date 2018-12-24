package zemberek.core;

public class StringInt implements Comparable<StringInt> {

  public final String string;
  public final int value;

  public StringInt(String string, int value) {
    this.string = string;
    this.value = value;
  }

  public static StringInt fromString(String input, char delimiter) {
    int index = input.indexOf(delimiter);
    if (index < 0) {
      throw new IllegalArgumentException(
          String.format("Cannot parse line [%s] with delimiter [%s]. There is no delimiter.",
              input, delimiter));
    }
    String first = input.substring(0, index).trim();
    String second = input.substring(index).trim();
    try {
      return new StringInt(first, Integer.parseInt(second));
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException(
          String.format("Cannot parse line [%s] with delimiter [%s]. Integer parse error.",
              input, delimiter));
    }
  }

  @Override
  public int compareTo(StringInt o) {
    return Integer.compare(o.value, value);
  }

  @Override
  public String toString() {
    return string + ":" + value;
  }
}
