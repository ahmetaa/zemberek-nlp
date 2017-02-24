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
    // for activating row level locking functionality, set this to true.
    private boolean enableLocks = false;

    private Matrix(int m_, int n_, float[] data_) {
        this(m_, n_, data_, false);
    }

    private Matrix(int m_, int n_, float[] data_, boolean enableLocks) {
        this.m_ = m_;
        this.n_ = n_;
        this.data_ = new float[m_][n_];
        for (int i = 0; i < m_; i++) {
            System.arraycopy(data_, i * n_, this.data_[i], 0, n_);
        }
        this.enableLocks = enableLocks;
    }


    /**
     * Generates a matrix that hs n_ columns and m_ rows.
     */
    Matrix(int m_, int n_, boolean enableLocks) {
        this.enableLocks = enableLocks;
        this.m_ = m_;
        this.n_ = n_;
        this.data_ = new float[m_][n_];
        if (enableLocks) {
            locks = new ReentrantReadWriteLock[m_];
        }
        for (int i = 0; i < m_; i++) {
            this.data_[i] = new float[n_];
            if (enableLocks) {
                locks[i] = new ReentrantReadWriteLock();
            }
        }
    }

    /**
     * Locks the [i]th row for writing.
     */
    final void writeLock(int i) {
        if (enableLocks) {
            locks[i].writeLock().lock();
        }
    }

    /**
     * Unlocks the [i]th row from write operations.
     */
    final void writeUnlock(int i) {
        if (enableLocks) {
            locks[i].writeLock().unlock();
        }
    }

    final void readLock(int i) {
        if (enableLocks) {
            locks[i].readLock().lock();
        }
    }

    final void readUnlock(int i) {
        if (enableLocks) {
            locks[i].readLock().unlock();
        }
    }

    /**
     * Fills the Matrix with uniform random numbers in [-a a] range.
     */
    void uniform(float a) {
        Random random = new Random(1);
        for (int i = 0; i < m_; i++) {
            for (int j = 0; j < n_; j++) {
                float v = (float) (random.nextDouble() * 2 * a - a);
                data_[i][j] = v;
            }
        }
    }

    /**
     * Sums the [a]*values of Vector [vec] to the [i].th row of the Matrix.
     * If locks are enabled, access to the row is thread safe.
     */
    void addRow(Vector vec, int i, float a) {
        writeLock(i);
        for (int j = 0; j < n_; j++) {
            data_[i][j] += a * vec.data_[j];
        }
        writeUnlock(i);
    }

    /**
     * Calculates dot product of Vector [vec] and [i]th row.
     * If locks are enabled, access to the row is thread safe.
     */
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

    /**
     * Saves values to binary stream [dos]
     */
    void save(DataOutputStream dos) throws IOException {
        dos.writeInt(m_);
        dos.writeInt(n_);
        for (int i = 0; i < m_; i++) {
            for (int j = 0; j < n_; j++) {
                dos.writeFloat(data_[i][j]);
            }
        }
    }

    void printRow(String s, int i, int amount) {
        int n = amount > n_ ? n_ : amount;
        System.out.print(s + "[" + i + "] = ");
        for (int k = 0; k < n; k++) {
            System.out.print(String.format("%.4f ", data_[i][k]));
        }
        System.out.println();
    }

    /**
     * loads the values from binary stream [dis] and instantiates the matrix.
     */
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
