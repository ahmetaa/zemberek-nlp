package zemberek.core.collections;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TestUtils {

  public static List<int[]> createFuzzingLists() {
    List<int[]> fuzzLists = new ArrayList(5000);
    int maxListSize = 300;
    Random r = new Random(0xBEEFCAFE);
    // Random sized lists with values in [0..n] shuffled.
    for (int i = 0; i < 1000; i++) {
      int[] arr = new int[r.nextInt(maxListSize) + 1];
      for (int j = 0; j < arr.length; j++) {
        arr[j] = j;
      }
      shuffle(arr);
      fuzzLists.add(arr);
    }
    // Random sized lists with values in [-n..n] shuffled.
    for (int i = 0; i < 1000; i++) {
      int size = r.nextInt(maxListSize) + 1;
      int[] arr = new int[size * 2];
      int idx = 0;
      for (int j = 0; j< arr.length; j++) {
        arr[idx++] = j - size;
      }
      shuffle(arr);
      fuzzLists.add(arr);
    }
    // Random sized lists in [-m,m] shuffled. Possible duplicates.
    int m = 1 << 10;
    for (int i = 0; i < 2000; i++) {
      int size = r.nextInt(maxListSize) + 1;
      int[] arr = new int[size];
      for (int j = 0; j < arr.length; j++) {
        arr[j] = r.nextInt(2 * m) - m;
      }
      shuffle(arr);
      fuzzLists.add(arr);
    }
    return fuzzLists;
  }

  // Fisher yates shuffle
  public static void shuffle(int[] array) {
    int index, temp;
    Random random = new Random(0xCAFEBABE);
    for (int i = array.length - 1; i > 0; i--)
    {
      index = random.nextInt(i + 1);
      temp = array[index];
      array[index] = array[i];
      array[i] = temp;
    }
  }
}
