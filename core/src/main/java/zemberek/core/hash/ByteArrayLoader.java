package zemberek.core.hash;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

public class ByteArrayLoader {
    byte[] data;

    public ByteArrayLoader(byte[] data) {
        this.data = data;
    }

    public DataInputStream getDataInputStream() {
        return new DataInputStream(new ByteArrayInputStream(data));
    }

    public double[] getAllDoubles(int amount) throws IOException {
        try (DataInputStream dis = getDataInputStream()) {
            double[] d = new double[amount];
            for (int i = 0; i < amount; i++) {
                d[i] = dis.readDouble();
            }
            return d;
        }
    }

    public int[] getAllInts(int amount) throws IOException {
        try (DataInputStream dis = getDataInputStream()) {
            int[] d = new int[amount];

            for (int i = 0; i < amount; i++) {
                d[i] = dis.readInt();
            }
            return d;
        }
    }

    public float[] getAllFloats(int amount) throws IOException {
        try (DataInputStream dis = getDataInputStream()) {
            float[] d = new float[amount];
            for (int i = 0; i < amount; i++) {
                d[i] = dis.readFloat();
            }
            return d;
        }
    }

    public float[] getAllFloatsFromDouble(int amount) throws IOException {
        try (DataInputStream dis = getDataInputStream()) {
            float[] d = new float[amount];
            for (int i = 0; i < amount; i++) {
                d[i] = (float) dis.readDouble();
            }
            return d;
        }
    }
}
