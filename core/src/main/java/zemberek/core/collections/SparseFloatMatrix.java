package zemberek.core.collections;

public class SparseFloatMatrix {

    private UIntFloatMap[] rows;
    public final int m; // rows
    public final int n; // columns

    public SparseFloatMatrix(int m, int n) {
        this(m, n, 2);
    }

    public SparseFloatMatrix(int m, int n, int initialColumnSize) {
        this.m = m;
        this.n = n;
        this.rows = new UIntFloatMap[m];
        for (int i = 0; i < m; i++) {
            rows[i] = new UIntFloatMap(initialColumnSize, 0);
        }
    }

    float get(int row, int col) {
        return rows[row].get(col);
    }

    void set(int row, int col, float value) {
        rows[row].put(col, value);
    }
}
