package zemberek.dependency;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;

/**
 * Compact 2D weight matrix supporting both full-precision {@code FP32} ({@link #DTYPE_FP32}) and
 * symmetric per-row {@code INT8} quantization ({@link #DTYPE_INT8}: {@code w[r, c] = scales[r] *
 * qData[r * cols + c]}).
 *
 * <p>All hot-path operations are allocation-free and write into caller-provided {@code float[]}
 * scratch buffers.
 */
final class QuantizedMatrix {

  static final byte DTYPE_FP32 = 0;
  static final byte DTYPE_INT8 = 1;

  final int rows;
  final int cols;
  final byte dtype;
  final float[] fData;   // non-null iff dtype == DTYPE_FP32
  final byte[] qData;    // non-null iff dtype == DTYPE_INT8
  final float[] scales;  // non-null iff dtype == DTYPE_INT8 (length = rows)

  private QuantizedMatrix(int rows, int cols, byte dtype, float[] fData, byte[] qData, float[] scales) {
    this.rows = rows;
    this.cols = cols;
    this.dtype = dtype;
    this.fData = fData;
    this.qData = qData;
    this.scales = scales;
  }

  static QuantizedMatrix ofFloat(int rows, int cols, float[] data) {
    if (data.length != rows * cols) {
      throw new IllegalArgumentException(
          "Expected " + (rows * cols) + " floats, got " + data.length);
    }
    return new QuantizedMatrix(rows, cols, DTYPE_FP32, data, null, null);
  }

  static QuantizedMatrix quantizePerRowInt8(int rows, int cols, float[] data) {
    if (data.length != rows * cols) {
      throw new IllegalArgumentException(
          "Expected " + (rows * cols) + " floats, got " + data.length);
    }
    byte[] q = new byte[rows * cols];
    float[] sc = new float[rows];
    for (int r = 0; r < rows; r++) {
      int base = r * cols;
      float maxAbs = 0f;
      for (int c = 0; c < cols; c++) {
        float a = Math.abs(data[base + c]);
        if (a > maxAbs) {
          maxAbs = a;
        }
      }
      if (maxAbs == 0f) {
        sc[r] = 0f;
      } else {
        float scale = maxAbs / 127.0f;
        float invScale = 127.0f / maxAbs;
        sc[r] = scale;
        for (int c = 0; c < cols; c++) {
          int v = Math.round(data[base + c] * invScale);
          if (v > 127) {
            v = 127;
          } else if (v < -127) {
            v = -127;
          }
          q[base + c] = (byte) v;
        }
      }
    }
    return new QuantizedMatrix(rows, cols, DTYPE_INT8, null, q, sc);
  }

  /** Copies row {@code r} into {@code out[outOff .. outOff + cols)}. */
  void rowToFloat(int r, float[] out, int outOff) {
    int base = r * cols;
    if (dtype == DTYPE_FP32) {
      System.arraycopy(fData, base, out, outOff, cols);
    } else {
      float s = scales[r];
      byte[] q = qData;
      for (int c = 0; c < cols; c++) {
        out[outOff + c] = q[base + c] * s;
      }
    }
  }

  /** Adds row {@code r} element-wise into {@code out[outOff .. outOff + cols)}. */
  void addRowToFloat(int r, float[] out, int outOff) {
    int base = r * cols;
    if (dtype == DTYPE_FP32) {
      float[] f = fData;
      for (int c = 0; c < cols; c++) {
        out[outOff + c] += f[base + c];
      }
    } else {
      float s = scales[r];
      byte[] q = qData;
      for (int c = 0; c < cols; c++) {
        out[outOff + c] += q[base + c] * s;
      }
    }
  }

  /** Computes {@code out[outOff .. outOff + rows) = W * x[xOff .. xOff + cols)}. */
  void matVec(float[] x, int xOff, float[] out, int outOff) {
    int rCount = rows;
    int cCount = cols;
    if (dtype == DTYPE_FP32) {
      float[] f = fData;
      for (int r = 0; r < rCount; r++) {
        int base = r * cCount;
        float sum = 0f;
        for (int c = 0; c < cCount; c++) {
          sum += f[base + c] * x[xOff + c];
        }
        out[outOff + r] = sum;
      }
    } else {
      byte[] q = qData;
      float[] sc = scales;
      for (int r = 0; r < rCount; r++) {
        int base = r * cCount;
        float sum = 0f;
        for (int c = 0; c < cCount; c++) {
          sum += q[base + c] * x[xOff + c];
        }
        out[outOff + r] = sum * sc[r];
      }
    }
  }

  /** Computes {@code out[outOff .. outOff + rows) = W * x[xOff .. xOff + cols) + bias}. */
  void matVecAddBias(float[] x, int xOff, float[] bias, float[] out, int outOff) {
    int rCount = rows;
    int cCount = cols;
    if (dtype == DTYPE_FP32) {
      float[] f = fData;
      for (int r = 0; r < rCount; r++) {
        int base = r * cCount;
        float sum = bias[r];
        for (int c = 0; c < cCount; c++) {
          sum += f[base + c] * x[xOff + c];
        }
        out[outOff + r] = sum;
      }
    } else {
      byte[] q = qData;
      float[] sc = scales;
      for (int r = 0; r < rCount; r++) {
        int base = r * cCount;
        float sum = 0f;
        for (int c = 0; c < cCount; c++) {
          sum += q[base + c] * x[xOff + c];
        }
        out[outOff + r] = sum * sc[r] + bias[r];
      }
    }
  }

  /** Computes {@code out[outOff .. outOff + cols) = x[xOff .. xOff + rows)^T * W}. */
  void vecMat(float[] x, int xOff, float[] out, int outOff) {
    int rCount = rows;
    int cCount = cols;
    Arrays.fill(out, outOff, outOff + cCount, 0f);
    if (dtype == DTYPE_FP32) {
      float[] f = fData;
      for (int r = 0; r < rCount; r++) {
        float xr = x[xOff + r];
        if (xr == 0f) {
          continue;
        }
        int base = r * cCount;
        for (int c = 0; c < cCount; c++) {
          out[outOff + c] += xr * f[base + c];
        }
      }
    } else {
      byte[] q = qData;
      float[] sc = scales;
      for (int r = 0; r < rCount; r++) {
        float xr = x[xOff + r] * sc[r];
        if (xr == 0f) {
          continue;
        }
        int base = r * cCount;
        for (int c = 0; c < cCount; c++) {
          out[outOff + c] += xr * q[base + c];
        }
      }
    }
  }

  /** Computes scalar bilinear form {@code u^T * W * v} where {@code u.length == rows, v.length == cols}. */
  float bilinear(float[] u, float[] v) {
    int rCount = rows;
    int cCount = cols;
    float total = 0f;
    if (dtype == DTYPE_FP32) {
      float[] f = fData;
      for (int r = 0; r < rCount; r++) {
        int base = r * cCount;
        float rowDot = 0f;
        for (int c = 0; c < cCount; c++) {
          rowDot += f[base + c] * v[c];
        }
        total += u[r] * rowDot;
      }
    } else {
      byte[] q = qData;
      float[] sc = scales;
      for (int r = 0; r < rCount; r++) {
        int base = r * cCount;
        float rowDot = 0f;
        for (int c = 0; c < cCount; c++) {
          rowDot += q[base + c] * v[c];
        }
        total += u[r] * sc[r] * rowDot;
      }
    }
    return total;
  }

  void write(DataOutputStream out) throws IOException {
    out.writeByte(dtype);
    out.writeInt(rows);
    out.writeInt(cols);
    if (dtype == DTYPE_FP32) {
      for (float v : fData) {
        out.writeFloat(v);
      }
    } else {
      for (float s : scales) {
        out.writeFloat(s);
      }
      out.write(qData);
    }
  }

  static QuantizedMatrix read(DataInputStream in) throws IOException {
    byte dtype = in.readByte();
    int rows = in.readInt();
    int cols = in.readInt();
    if (dtype == DTYPE_FP32) {
      float[] f = new float[rows * cols];
      for (int i = 0; i < f.length; i++) {
        f[i] = in.readFloat();
      }
      return new QuantizedMatrix(rows, cols, DTYPE_FP32, f, null, null);
    } else if (dtype == DTYPE_INT8) {
      float[] sc = new float[rows];
      for (int i = 0; i < rows; i++) {
        sc[i] = in.readFloat();
      }
      byte[] q = new byte[rows * cols];
      in.readFully(q);
      return new QuantizedMatrix(rows, cols, DTYPE_INT8, null, q, sc);
    } else {
      throw new IOException("Unsupported QuantizedMatrix dtype: " + dtype);
    }
  }
}
