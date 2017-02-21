package zemberek.embedding.fasttext;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Random;

public class Matrix {
    int m_;
    int n_;
    float[] data_;

    public Matrix() {
        this.m_ = 0;
        this.n_ = 0;
        data_ = new float[0];
    }

    private Matrix(int m_, int n_, float[] data_) {
        this.m_ = m_;
        this.n_ = n_;
        this.data_ = data_;
    }

    public Matrix(int m_, int n_) {
        this.m_ = m_;
        this.n_ = n_;
        this.data_ = new float[m_ * n_];
    }

    public Matrix(Matrix other) {
        this.m_ = other.m_;
        this.n_ = other.n_;
        this.data_ = other.data_.clone();
    }

    public void uniform(float a) {
        Random random = new Random(1);
        for (int i = 0; i < data_.length; i++) {
            data_[i] = random.nextFloat() * 2 * a - a;
        }
    }

    void addRow(Vector vec, int i, float a) {
        assert (i >= 0);
        assert (i < m_);
        assert (vec.m_ == n_);
        for (int j = 0; j < n_; j++) {
            data_[i * n_ + j] += a * vec.data_[j];
        }
    }

    float dotRow(Vector vec, int i) {
        assert (i >= 0);
        assert (i < m_);
        assert (vec.m_ == n_);
        float d = 0.0f;
        for (int j = 0; j < n_; j++) {
            d += data_[i * n_ + j] * vec.data_[j];
        }
        return d;
    }

    void save(DataOutputStream dos) throws IOException {
        dos.writeInt(m_);
        dos.writeInt(n_);
        for (float v : data_) {
            dos.writeFloat(v);
        }
    }

    static Matrix load(DataInputStream dis) throws IOException {
        int m_ = dis.readInt();
        int n_ = dis.readInt();
        float[] data = new float[m_ * n_];
        for (int i = 0; i < data.length; i++) {
            data[i] = dis.readFloat();
        }
        return new Matrix(m_, n_, data);
    }

}
