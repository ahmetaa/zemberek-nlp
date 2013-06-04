package zemberek.core.hash;

public interface IntHashKeyProvider {

    /**
     * reads the byte array representation of the key with index.
     *
     * @param index id of the key.
     * @return byte array representation of the key.
     */
    int[] getKey(int index);

    /**
     * Total amount of the keys.
     *
     * @return amount of keys.
     */
    int keyAmount();
}
