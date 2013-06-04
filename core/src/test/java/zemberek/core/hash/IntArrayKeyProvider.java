package zemberek.core.hash;

import com.google.common.primitives.Ints;

public class IntArrayKeyProvider implements IntHashKeyProvider, HashKeyProvider {

    final int[][] arr;

    public IntArrayKeyProvider(int[][] arr) {
        this.arr = arr;
    }

    public int[] getKey(int index) {
        return arr[index];  //To change body of implemented methods use File | Settings | File Templates.
    }

    public byte[] getKeyAsBytes(int index) {
        int[] k = arr[index];
        byte[] bytes = new byte[k.length * 4];
        int j = 0;
        for (int r : k) {
            byte[] bytez = Ints.toByteArray(r);
            for (byte b : bytez) {
                bytes[j++] = b;
            }
        }
        return bytes;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public int keyAmount() {
        return arr.length;
    }
}
