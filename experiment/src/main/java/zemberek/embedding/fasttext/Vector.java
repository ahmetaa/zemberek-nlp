package zemberek.embedding.fasttext;

import zemberek.core.math.FloatArrays;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class Vector {
    float[] data_;
    int m_;

    Vector(int m_) {
        this.data_ = new float[m_];
        this.m_ = m_;
    }

    public int size() {
        return data_.length;
    }

    void zero() {
        Arrays.fill(data_, 0);
    }

    void mul(float a) {
        FloatArrays.scaleInPlace(data_, a);
    }


    void addRow(Matrix_ A, int i) {
        assert (i >= 0);
        assert (i < A.m_);
        assert (m_ == A.n_);
        for (int j = 0; j < A.n_; j++) {
            data_[j] += A.at(i, j);
        }
    }

    void addRow(Matrix_ A, int i, float a) {
        assert (i >= 0);
        assert (i < A.m_);
        assert (m_ == A.n_);
        for (int j = 0; j < A.n_; j++) {
            data_[j] += a * A.at(i, j);
        }
    }

    void addRow(QMatrix A, int i) {
        assert(i >= 0);
        A.addToVector(this, i);
    }

    void mul(Matrix_ A, Vector vec) {
        for (int i = 0; i < m_; i++) {
            data_[i] = 0.0f;
            for (int j = 0; j < A.n_; j++) {
                data_[i] += A.at(i,j) * vec.data_[j];
            }
        }
    }

    void mul(QMatrix A, Vector vec) {
        assert(A.getM() == m_);
        assert(A.getN() == vec.m_);
        for (int i = 0; i < m_; i++) {
            data_[i] = A.dotRow(vec, i);
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

    String asString() {
        List<String> values = new ArrayList<>(data_.length);
        for (float v : data_) {
            values.add(String.format("%.6f", v));
        }
        return String.join(" ", values);
    }

}
