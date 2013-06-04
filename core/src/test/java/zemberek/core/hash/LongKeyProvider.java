package zemberek.core.hash;

import com.google.common.primitives.Longs;

class LongKeyProvider implements HashKeyProvider {

    final long[] arr;

    LongKeyProvider(long[] arr) {
        this.arr = arr;
    }

    public byte[] getKeyAsBytes(int index) {
        return Longs.toByteArray(arr[index]);
    }

    public int keyAmount() {
        return arr.length;
    }
}
