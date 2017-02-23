package zemberek.embedding.fasttext;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Random;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

class Matrix {
    int m_;
    int n_;
    float[][] data_;
    private ReadWriteLock[] locks;

    private Matrix(int m_, int n_, float[] data_) {
        this.m_ = m_;
        this.n_ = n_;
        this.data_ = new float[m_][n_];
        for (int i = 0; i < m_; i++) {
            System.arraycopy(data_, i * n_, this.data_[i], 0, n_);
        }
    }

    Matrix(int m_, int n_) {
        this.m_ = m_;
        this.n_ = n_;
        this.data_ = new float[m_][n_];
        locks = new ReentrantReadWriteLock[m_];
        for (int i = 0; i < m_; i++) {
            this.data_[i] = new float[n_];
            locks[i] = new ReentrantReadWriteLock();
        }
    }

    final void writeLock(int i) {
        locks[i].writeLock().lock();
    }

    final void writeUnlock(int i) {
        locks[i].writeLock().unlock();
    }

    final void readLock(int i) {
        locks[i].readLock().lock();
    }

    final void readUnlock(int i) {
        locks[i].readLock().unlock();
    }

    void uniform(float a) {
        Random random = new Random(1);
        for (int i = 0; i < m_; i++) {
            for (int j = 0; j < n_; j++) {
                float v = (float) (random.nextDouble() * 2 * a - a);
                data_[i][j] = v;
            }
        }
    }

    void addRow(Vector vec, int i, float a) {
        writeLock(i);
        for (int j = 0; j < n_; j++) {
            data_[i][j] += a * vec.data_[j];
        }
        writeUnlock(i);
    }

    float dotRow(Vector vec, int i) {
        assert (i >= 0);
        assert (i < m_);
        assert (vec.m_ == n_);
        float d = 0.0f;
        readLock(i);
        for (int j = 0; j < n_; j++) {
            d += data_[i][j] * vec.data_[j];
        }
        readUnlock(i);
        return d;
    }

    void save(DataOutputStream dos) throws IOException {
        dos.writeInt(m_);
        dos.writeInt(n_);
        for (int i = 0; i < m_; i++) {
            for (int j = 0; j < n_; j++) {
                dos.writeFloat(data_[i][j]);
            }
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
