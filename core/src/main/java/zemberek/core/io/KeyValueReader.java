package zemberek.core.io;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class KeyValueReader {

    private final String separator;
    private final String ignorePrefix;

    public KeyValueReader(String seperator) {
        if (Strings.isNullOrEmpty(seperator))
            throw new IllegalArgumentException("Separator is null or empty!");
        this.separator = seperator;
        this.ignorePrefix = "#";
    }

    public KeyValueReader(String seperator, String ignorePrefix) {
        if (Strings.isNullOrEmpty(seperator))
            throw new IllegalArgumentException("Separator is null or empty!");
        this.separator = seperator;
        this.ignorePrefix = ignorePrefix;
    }

    public Map<String, String> loadFromFile(File file) throws IOException {
        return loadFromFile(new SimpleTextReader.
                Builder(file)
                .trim()
                .ignoreIfStartsWith(ignorePrefix)
                .ignoreWhiteSpaceLines()
                .build());
    }

    public Map<String, String> loadFromFile(File file, String encoding) throws IOException {
        return loadFromFile(new SimpleTextReader.
                Builder(file)
                .trim()
                .ignoreIfStartsWith(ignorePrefix)
                .ignoreWhiteSpaceLines()
                .encoding(encoding)
                .build());
    }

    public Map<String, String> loadFromStream(InputStream is) throws IOException {
        return loadFromFile(new SimpleTextReader.
                Builder(is)
                .trim()
                .ignoreIfStartsWith(ignorePrefix)
                .ignoreWhiteSpaceLines()
                .build());
    }

    public Map<String, String> loadFromStream(InputStream is, String encoding) throws IOException {
        return loadFromFile(new SimpleTextReader.
                Builder(is)
                .trim()
                .ignoreIfStartsWith(ignorePrefix)
                .ignoreWhiteSpaceLines()
                .encoding(encoding)
                .build());
    }

    public Map<String, String> loadFromFile(SimpleTextReader sfr) throws IOException {
        List<String> lines = sfr.asStringList();

        if (lines.size() == 0)
            return Collections.emptyMap();
        Map<String, String> result = new LinkedHashMap<String, String>();
        for (String line : lines) {
            if (!line.contains(separator))
                throw new IllegalArgumentException("line: [" + line + "] has no separator:" + separator);
            String key = Strings.subStringUntilFirst(line, separator).trim();
            String value = Strings.subStringAfterFirst(line, separator).trim();
            result.put(key, value);
        }
        return result;
    }
}
