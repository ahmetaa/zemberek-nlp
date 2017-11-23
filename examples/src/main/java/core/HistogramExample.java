package core;

import java.util.Arrays;
import zemberek.core.collections.Histogram;

public class HistogramExample {

  public static void counts() {
    String[] fruits = {"apple", "pear", "grape", "apple", "apple", "apricot", "grape"};

    System.out.println("Adding elements to histogram:" + Arrays.toString(fruits));
    Histogram<String> histogram = new Histogram<>();
    histogram.add(fruits);

    System.out.println("\nPrint with no order");
    for (String s : histogram) {
      System.out.println(s + " count: " + histogram.getCount(s));
    }

    System.out.println("\nPrint with count order");
    for (String s : histogram.getSortedList()) {
      System.out.println(s + " count: " + histogram.getCount(s));
    }

    histogram.removeSmaller(2);
    System.out.println("\nAfter removing elements with counts less than 2");
    for (String s : histogram.getSortedList()) {
      System.out.println(s + " count: " + histogram.getCount(s));
    }
  }

  public static void main(String[] args) {
    counts();
  }

}
