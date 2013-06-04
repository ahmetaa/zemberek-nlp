package zemberek.core.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;

class Dumper {
    
    /**
     * dumps the contents of an input stream in hex format to an output stream. both stream are closed at the end of method call
     *
     * @param is     file
     * @param os     output stream to write hex values
     * @param column the column size of the hex numbers.
     * @param amount amount of bytes to write.
     * @throws java.io.IOException if there is an error while accesing the file or writing the hex values.
     */
    public static void hexDump(InputStream is, OutputStream os, int column, long amount) throws IOException {
        PrintStream ps = new PrintStream(os);
        try {
            byte[] bytes = new byte[column];
            int i;
            long total = 0;
            while ((i = is.read(bytes)) != -1) {
                for (int j = 0; j < i; j++) {
                    ps.print(Bytes.toHexWithZeros(bytes[j]) + " ");
                }
                for (int j = 0; j < i; j++) {
                    char c = (char) bytes[j];
                    if (!Character.isWhitespace(c))
                        ps.print((char) bytes[j]);
                    else
                        ps.print(" ");
                }
                ps.println();
                total += i;
                if (total >= amount && amount > -1)
                    break;
            }
        } finally {
            IOs.closeSilently(is, ps);
        }
    }    
}
