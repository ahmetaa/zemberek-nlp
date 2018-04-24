package zemberek.examples.core;

import java.util.Arrays;
import zemberek.core.collections.Histogram;
import zemberek.core.logging.Log;

public class HistogramExample {

  public static void counts() {
    String[] fruits = {"apple", "pear", "grape", "apple", "apple", "apricot", "grape"};

    Log.info("Adding elements to histogram:" + Arrays.toString(fruits));
    Histogram<String> histogram = new Histogram<>();
    histogram.add(fruits);

    Log.info("\nPrint with no order");
    for (String s : histogram) {
      Log.info(s + " count: " + histogram.getCount(s));
    }

    Log.info("\nPrint with count order");
    for (String s : histogram.getSortedList()) {
      Log.info(s + " count: " + histogram.getCount(s));
    }

    histogram.removeSmaller(2);
    Log.info("\nAfter removing elements with counts less than 2");
    for (String s : histogram.getSortedList()) {
      Log.info(s + " count: " + histogram.getCount(s));
    }
  }

  public static void main(String[] args) {
    counts();
  }

}
