package zemberek.core.embeddings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import zemberek.core.math.FloatArrays;

class Vector {

  float[] data_;

  Vector(int m_) {
    this.data_ = new float[m_];
  }

  public float[] getData() {
    return data_;
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

  // Sums matrix[i] row values to this vector values.
  void addRow(Matrix A, int i) {
    assert (i >= 0);
    assert (i < A.m_);
    assert (size() == A.n_);
    for (int j = 0; j < A.n_; j++) {
      data_[j] += A.at(i, j);
    }
  }

  void addVector(Vector source) {
    assert (size() == source.size());
    FloatArrays.addToFirst(data_, source.data_);
  }

  void addRow(Matrix A, int i, float a) {
    assert (i >= 0);
    assert (i < A.m_);
    assert (size() == A.n_);
    for (int j = 0; j < A.n_; j++) {
      data_[j] += a * A.at(i, j);
    }
  }

  void addRow(QMatrix A, int i) {
    assert (i >= 0);
    A.addToVector(this, i);
  }

  void mul(Matrix A, Vector vec) {
    for (int i = 0; i < size(); i++) {
      data_[i] = 0.0f;
      for (int j = 0; j < A.n_; j++) {
        data_[i] += A.at(i, j) * vec.data_[j];
      }
    }
  }

  float norm() {
    float sum = 0f;
    for (float v : data_) {
      sum += v * v;
    }
    return (float) Math.sqrt(sum);

  }

  void mul(QMatrix A, Vector vec) {
    assert (A.getM() == size());
    assert (A.getN() == vec.size());
    for (int i = 0; i < size(); i++) {
      data_[i] = A.dotRow(vec, i);
    }
  }

  public int argmax() {
    float max = data_[0];
    int argmax = 0;
    for (int i = 1; i < size(); i++) {
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
      values.add(String.format(Locale.ENGLISH, "%.6f", v));
    }
    return String.join(" ", values);
  }

}
