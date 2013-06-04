package zemberek.core;

public class SparseIntMatrix {

    final SparseIntVector[] rows;
    final SparseIntVector[] columns;

    public SparseIntMatrix(int rowCount, int columnCount) {
        rows = new SparseIntVector[rowCount];
        columns = new SparseIntVector[columnCount];
    }

    public int get(int row, int col) {
        return rows[row].get(col);
    }

    public void set(int row, int col, int val) {
        if (rows[row] == null)
            rows[row] = new SparseIntVector();
        rows[row].set(col, val);
        if (columns[col] == null)
            columns[col] = new SparseIntVector();
        columns[col].set(row, val);
    }

    public void remove(int row, int col) {
        rows[row].remove(col);
        columns[col].remove(row);
    }

    public void mul(SparseIntMatrix sm) {

    }

}
