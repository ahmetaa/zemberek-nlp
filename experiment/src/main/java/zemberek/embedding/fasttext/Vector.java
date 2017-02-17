package zemberek.embedding.fasttext;

import zemberek.core.math.FloatArrays;

import java.util.Arrays;

public class Vector {
    float[] data_;
    int m_;

    public Vector(int m_) {
        this.data_ = new float[m_];
        this.m_ = m_;
    }

    public int size() {
        return data_.length;
    }

    public void zero() {
        Arrays.fill(data_, 0);
    }

    public void mul(float a) {
        FloatArrays.scale(data_, a);
    }


    void addRow(Matrix A, int i) {
        assert(i >= 0);
        assert(i < A.m_);
        assert(m_ == A.n_);
        for (int j = 0; j < A.n_; j++) {
            data_[j] += A.data_[i * A.n_ + j];
        }
    }

    void addRow(Matrix A, int i, float a) {
        assert(i >= 0);
        assert(i < A.m_);
        assert(m_ == A.n_);
        for (int j = 0; j < A.n_; j++) {
            data_[j] += a * A.data_[i * A.n_ + j];
        }
    }


    public void mul(Matrix A, Vector vec) {
        for (int i = 0; i < m_; i++) {
            data_[i] = 0.0f;
            for (int j = 0; j < A.n_; j++) {
                data_[i] += A.data_[i * A.n_ + j] * vec.data_[j];
            }
        }
    }

    public int argmax() {
        float max = data_[0];
        int argmax = 0;
        for (int i = 1; i < m_; i++) {
            if (data_[i] > max) {
                max = data_[i];
                argmax = i;
            }
        }
        return argmax;
    }

}
