package zemberek.core.hash;

public interface HashKeyProvider {

    /**
     * reads the byte array representation of the key with index.
     *
     * @param index id of the key.
     * @return byte array representation of the key.
     */
    byte[] getKeyAsBytes(int index);

    /**
     * Total amount of the keys.
     *
     * @return amount of keys.
     */
    int keyAmount();
}
