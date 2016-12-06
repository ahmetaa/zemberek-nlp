package zemberek.core.quantization;

import zemberek.core.hash.ByteArrayLoader;

import java.io.*;
import java.util.Arrays;

public class FloatLookup {

    private float data[];
    final int range;

    public FloatLookup(float[] data) {
        this.data = data;
        this.range = data.length;
    }

    public int getRange() {
        return range;
    }

    public int getIndex(float value) {
        int index = Arrays.binarySearch(data, value);
        if (index < 0)
            throw new IllegalArgumentException("value cannot be found in lookup:" + value);
        else return index;
    }

    public int getClosestIndex(float value) {
        int index = Arrays.binarySearch(data, value);
        if (index < 0)
            return -index;
        else return index;
    }

    public void save(File file) throws IOException {
        DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file), 1000000));
        dos.writeInt(range);
        for (float v : data) {
            dos.writeFloat(v);
        }
        dos.close();
    }

    public void save(DataOutputStream dos) throws IOException {
        dos.writeInt(range);
        for (float v : data) {
            dos.writeFloat(v);
        }
    }

    public static FloatLookup getLookup(DataInputStream dis) throws IOException {
        int range = dis.readInt();
        byte[] data = new byte[range * 4];
        dis.readFully(data);
        return new FloatLookup(new ByteArrayLoader(data).getAllFloats(range));
    }

    public static FloatLookup getLookupFromDouble(DataInputStream dis) throws IOException {
        int range = dis.readInt();
        byte[] data = new byte[range * 8];
        dis.readFully(data);
        return new FloatLookup(new ByteArrayLoader(data).getAllFloatsFromDouble(range));
    }


    public static FloatLookup getLookup(File file) throws IOException {
        try (DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(file), 1000000))) {
            return getLookup(dis);
        }
    }

    public static FloatLookup getLookupFromDouble(File file) throws IOException {
        try (DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(file), 1000000))) {
            return getLookupFromDouble(dis);
        }
    }

    public static void changeBase(float[] data, double source, double target) {
        float multiplier = (float) (Math.log(source) / Math.log(target));
        for (int i = 0; i < data.length; i++) {
            data[i] = data[i] * multiplier;
        }
    }

    public void changeBase(double source, double target) {
        changeBase(data, source, target);
    }

    /**
     * Returns dequantized value of the given integer index.
     *
     * @param n value to deQuantize
     * @return dequanztized value.
     */
    public float get(int n) {
        if (n < 0 || n >= range)
            throw new IllegalArgumentException("Cannot dequantize value. Value is out of range:" + n);
        return data[n];
    }    
}
