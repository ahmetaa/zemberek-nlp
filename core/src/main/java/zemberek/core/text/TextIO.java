package zemberek.core.text;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

public class TextIO {

    public static long charCount(Path path, Charset charset) throws IOException {
        BufferedReader reader = Files.newBufferedReader(path, charset);
        char[] buf = new char[4096];
        long count = 0;
        while (true) {
            int k = reader.read(buf);
            if (k == -1) {
                break;
            }
            count += k;
        }
        return count;
    }
}
