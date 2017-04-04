package zemberek.core.io;


import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

public class IOUtil {
    public static DataInputStream getDataInputStream(Path path) throws IOException {
        return new DataInputStream(new BufferedInputStream(Files.newInputStream(path)));
    }

    public static DataInputStream getDataInputStream(InputStream is) throws IOException {
        return new DataInputStream(new BufferedInputStream(is));
    }

    public static DataOutputStream getDataOutputStream(Path path) throws IOException {
        return new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(path)));
    }

    public static BufferedOutputStream geBufferedOutputStream(Path path) throws IOException {
        return new BufferedOutputStream(Files.newOutputStream(path));
    }

    public static DataInputStream getDataInputStream(Path path, int bufferSize) throws IOException {
        if (bufferSize <= 0)
            throw new IllegalArgumentException("Buffer size must be positive. But it is :" + bufferSize);
        return new DataInputStream(new BufferedInputStream(Files.newInputStream(path), bufferSize));
    }

    public static DataOutputStream getDataOutputStream(Path path, int bufferSize) throws IOException {
        if (bufferSize <= 0)
            throw new IllegalArgumentException("Buffer size must be positive. But it is :" + bufferSize);
        return new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(path), bufferSize));
    }

    public static int readIntLe(DataInputStream dis) throws IOException {
        return Integer.reverseBytes(dis.readInt());
    }

    public static short readShortLe(DataInputStream dis) throws IOException {
        return Short.reverseBytes(dis.readShort());
    }

    public static void writeShortLe(DataOutputStream dos, short value) throws IOException {
        dos.writeShort(Short.reverseBytes(value));
    }

    public static void writeIntLe(DataOutputStream dos, int value) throws IOException {
        dos.writeInt(Integer.reverseBytes(value));
    }

    public static void checkFileArgument(Path path) {
        File f = path.toFile();
        if (!f.exists()) {
            throw new IllegalArgumentException("File does not exist = " + f.getAbsolutePath());
        }
        if (f.isDirectory()) {
            throw new IllegalArgumentException("A file is expected. But path is a directory = " + f.getAbsolutePath());
        }
    }

    public static void checkDirectoryArgument(Path path) {
        File f = path.toFile();
        if (!f.exists()) {
            throw new IllegalArgumentException("Directory does not exist = " + f.getAbsolutePath());
        }
        if (!f.isDirectory()) {
            throw new IllegalArgumentException("A directory is expected. But path is a file = " + f.getAbsolutePath());
        }
    }

}
