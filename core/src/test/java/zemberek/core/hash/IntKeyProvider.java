package zemberek.core.hash;

import com.google.common.primitives.Ints;

class IntKeyProvider implements HashKeyProvider {

    final int[] arr;

    IntKeyProvider(int[] arr) {
        this.arr = arr;
    }

    public byte[] getKeyAsBytes(int index) {
        return Ints.toByteArray(arr[index]);
    }

    public int keyAmount() {
        return arr.length;
    }
}
