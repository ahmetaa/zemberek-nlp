package zemberek.embedding.fasttext;

import zemberek.core.collections.IntVector;


import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Random;

public class ProductQuantizer {

    //TODO: original is 8 bits but because byte is signed, for now make it 7
    int nbits_ = 7;
    int ksub_ = 1 << nbits_;
    int max_points_per_cluster_ = 128;
    int max_points_ = max_points_per_cluster_ * ksub_;
    int seed_ = 1234;
    int niter_ = 25;
    float eps_ = 1e-7f;

    int dim_;
    int nsubq_;
    int dsub_;
    int lastdsub_;

    FArray centroids_;

    Random rng;

    public ProductQuantizer() {
    }

    // used for mimicking pointer arithmetic.
    ProductQuantizer(int dim, int dsub) {
        this.dim_ = dim;
        this.nsubq_ = dim / dsub;
        dsub_ = dsub;
        centroids_ = new FArray(new float[dim * ksub_]);
        rng = new Random(seed_);
        lastdsub_ = dim_ % dsub;
        if (lastdsub_ == 0) {
            lastdsub_ = dsub_;
        } else {
            nsubq_++;
        }
    }

    static class FArray {
        int pointer;

        float[] data;

        FArray(float[] data) {
            this.data = data;
            this.pointer = 0;
        }

        FArray(int pointer, float[] data) {
            this.pointer = pointer;
            this.data = data;
        }

        void fill(int from, int to, float val) {
            Arrays.fill(data, pointer + from, pointer + to, val);
        }

        float get(int i) {
            return data[pointer + i];
        }

        void arrayCopy(int srcPos, FArray dest, int destPos, int amount) {
            System.arraycopy(data, pointer + srcPos, dest.data, dest.pointer + destPos, amount);
        }

        void set(int i, float value) {
            data[pointer + i] = value;
        }

        void add(int i, float value) {
            data[pointer + i] += value;
        }

        void divide(int i, float value) {
            data[pointer + i] /= value;
        }
        FArray ref(int offset) {
            return new FArray(pointer + offset, this.data);
        }

    }
    static class BArray {
        int pointer;

        byte[] data;

        BArray(byte[] data) {
            this.data = data;
            this.pointer = 0;
        }

        BArray(int pointer, byte[] data) {
            this.pointer = pointer;
            this.data = data;
        }

        byte get(int i) {
            return data[pointer + i];
        }

        void set(int i, byte value) {
            data[pointer + i] = value;
        }
        BArray ref(int offset) {
            return new BArray(pointer + offset, this.data);
        }
    }



    float distL2(FArray x, FArray y, int d) {
        float dist = 0;
        for (int i = 0; i < d; i++) {
            float tmp = x.get(i) - y.get(i);
            dist += tmp * tmp;
        }
        return dist;
    }

    // TODO: Original code has two metohds for this, one const other not.
    FArray get_centroids(int m, byte i) {
        if (m == nsubq_ - 1) {
            return centroids_.ref(m * ksub_ * dsub_ + i * lastdsub_);
        }
        return centroids_.ref((m * ksub_ + i) * dsub_);
    }

    float assign_centroid(FArray x,
                          FArray c0,
                          BArray code,
                          int d) {

        FArray c = c0.ref(0);
        float dis = distL2(x, c, d);
        code.set(0, (byte) 0);
        for (int j = 1; j < ksub_; j++) {
            c = c.ref(d);
            float disij = distL2(x, c, d);
            if (disij < dis) {
                code.set(0, (byte) j);
                dis = disij;
            }
        }
        return dis;
    }

    void Estep(
            FArray x,
            FArray centroids,
            BArray codes,
            int d,
            int n) {
        for (int i = 0; i < n; i++) {
            assign_centroid(
                    x.ref(i * d), centroids, codes.ref(i), d);
        }
    }

    void MStep(FArray x0,
               FArray centroids,
               BArray codes,
               int d,
               int n) {
        int[] nelts = new int[ksub_];
        centroids.fill(0, d * ksub_, 0);

        FArray x = x0.ref(0);

        for (int i = 0; i < n; i++) {
            int k = codes.get(i);
            FArray c = centroids.ref(k * d);
            for (int j = 0; j < d; j++) {
                c.add(j, x.get(j));
            }
            nelts[k]++;
            x = x.ref(d);
        }

        FArray c = centroids.ref(0);
        for (int k = 0; k < ksub_; k++) {
            float z = (float) nelts[k];
            if (z != 0) {
                for (int j = 0; j < d; j++) {
                    c.divide(j, z);
                }
            }
            c = c.ref(d);
        }

        for (int k = 0; k < ksub_; k++) {
            if (nelts[k] == 0) {
                int m = 0;
                while (rng.nextFloat() * (n - ksub_) >= nelts[m] - 1) {
                    m = (m + 1) % ksub_;
                }

                centroids.arrayCopy(m * d, centroids, k * d, d);
                for (int j = 0; j < d; j++) {
                    int sign = (j % 2) * 2 - 1;
                    centroids.add(k * d + j, sign * eps_);
                    centroids.add(m * d + j, -sign * eps_);
                }
                nelts[k] = nelts[m] / 2;
                nelts[m] -= nelts[k];
            }
        }
    }

    void kmeans(FArray x, FArray c, int n, int d) {
        IntVector iv = new IntVector(n);
        for (int i = 0; i < iv.size(); i++) {
            iv.set(i, i);
        }
        iv.shuffle(rng);
        for (int i = 0; i < ksub_; i++) {
            x.arrayCopy(iv.get(i) * d, c, i * d, d);
        }
        BArray codes = new BArray(new byte[n]);
        for (int i = 0; i < niter_; i++) {
            Estep(x, c, codes, d, n);
            MStep(x, c, codes, d, n);
        }
    }

    void train(int n, float[] x) {
        if (n < ksub_) {
            throw new IllegalArgumentException(
                    "Matrix too small for quantization, must have > 256 rows. But it is " + n);
        }
        IntVector perm = new IntVector(n);
        for (int i = 0; i < n; i++) {
            perm.set(i, i);
        }
        int d = dsub_;
        int np = Math.min(n, max_points_);

        float[] xslice = new float[np * dsub_];
        for (int m = 0; m < nsubq_; m++) {
            if (m == nsubq_ - 1) {
                d = lastdsub_;
            }
            if (np != n) {
                perm.shuffle(rng);
            }
            for (int j = 0; j < np; j++) {
                System.arraycopy(x, perm.get(j) * dim_ + m * dsub_, xslice, j * d, d);
            }
            kmeans(new FArray(xslice),
                    get_centroids(m, (byte) 0),
                    np,
                    d);
        }
    }

    float mulcode(Vector x, BArray codes, int t, float alpha) {
        float res = 0;
        int d = dsub_;
        BArray code = codes.ref(nsubq_ * t);
        for (int m = 0; m < nsubq_; m++) {
            FArray c = get_centroids(m, code.get(m));
            if (m == nsubq_ - 1) {
                d = lastdsub_;
            }
            for (int n = 0; n < d; n++) {
                res += x.data_[m * dsub_ + n] * c.get(n);
            }
        }
        return res * alpha;
    }

    void addcode(Vector x, BArray codes, int t, float alpha) {
        int d = dsub_;
        BArray code = codes.ref(nsubq_ * t);
        for (int m = 0; m < nsubq_; m++) {
            FArray c = get_centroids(m, code.get(m));
            if (m == nsubq_ - 1) {
                d = lastdsub_;
            }
            for (int n = 0; n < d; n++) {
                x.data_[m * dsub_ + n] += alpha * c.get(n);
            }
        }
    }

    void compute_code(FArray x, BArray code) {
        int d = dsub_;
        for (int m = 0; m < nsubq_; m++) {
            if (m == nsubq_ - 1) {
                d = lastdsub_;
            }
            assign_centroid(
                    x.ref(m * dsub_),
                    get_centroids(m, (byte) 0),
                    code.ref(m),
                    d);
        }
    }

    void compute_codes(FArray x, BArray codes, int n) {
        for (int i = 0; i < n; i++) {
            compute_code(
                    x.ref(i * dim_),
                    codes.ref(i * nsubq_));
        }
    }

    void save(DataOutputStream dos) throws IOException {
        dos.writeInt(dim_);
        dos.writeInt(nsubq_);
        dos.writeInt(dsub_);
        dos.writeInt(lastdsub_);
        for (int i = 0; i < centroids_.data.length; i++) {
            dos.writeFloat(centroids_.data[i]);
        }
    }

    void load(DataInputStream dis) throws IOException {
        dim_ = dis.readInt();
        nsubq_ = dis.readInt();
        dsub_ = dis.readInt();
        lastdsub_ = dis.readInt();
        float[] centroidData = new float[dim_ * ksub_];
        for (int i = 0; i < centroidData.length; i++) {
            centroidData[i] = dis.readFloat();
        }
        centroids_ = new FArray(centroidData);
    }
}
