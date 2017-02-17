package zemberek.core.io;

import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * SimpleTextReader can be used reading text sources with ease.
 */
public final class SimpleTextReader implements AutoCloseable {

    private final InputStream is;
    private final String encoding;
    private Filter filters[] = new Filter[0];
    private boolean trim = false;
    private Template template;

    public static class Builder {
        private InputStream _is;
        private Template _template = new Template();

        public Builder(String fileName) throws IOException {
            checkNotNull(fileName, "File name cannot be null..");
            this._is = new BufferedInputStream(new FileInputStream(fileName));
        }

        public Builder(InputStream is) {
            checkNotNull(is, "File name cannot be null..");
            this._is = is;
        }

        public Builder(File file) throws IOException {
            checkNotNull(file, "File name cannot be null..");
            this._is = new BufferedInputStream(new FileInputStream(file));
        }

        public Builder encoding(String encoding) {
            if (encoding != null)
                this._template._encoding = encoding;
            return this;
        }

        public Builder ignoreWhiteSpaceLines() {
            this._template._ignoreWhiteSpaceLines = true;
            return this;
        }

        public Builder ignoreIfStartsWith(String... prefix) {
            this._template._ignorePrefix = prefix;
            return this;
        }

        public Builder allowMatchingRegexp(String regexp) {
            this._template._regexp = regexp;
            return this;
        }

        public SimpleTextReader build() {
            return new SimpleTextReader(_is, _template);
        }

        public Builder trim() {
            this._template._trim = true;
            return this;
        }
    }

    static class Template {
        private String _encoding;
        private boolean _trim = false;
        private boolean _ignoreWhiteSpaceLines = false;
        private String _regexp;
        private String[] _ignorePrefix;

        public Template() {
            _encoding = Charset.defaultCharset().name();
        }

        private Template(String encoding) {
            this._encoding = encoding;
        }

        public Template encoding(String encoding) {
            if (encoding == null)
                this._encoding = Charset.defaultCharset().name();
            this._encoding = encoding;
            return this;
        }

        public Template ignoreWhiteSpaceLines() {
            this._ignoreWhiteSpaceLines = true;
            return this;
        }

        public Template ignoreIfStartsWith(String... prefix) {
            this._ignorePrefix = prefix;
            return this;
        }

        public Template allowMatchingRegexp(String regexp) {
            this._regexp = regexp;
            return this;
        }

        public SimpleTextReader generateReader(InputStream is) throws IOException {
            return new SimpleTextReader(is, this);
        }

        public SimpleTextReader generateReader(String fileName) throws IOException {
            return new SimpleTextReader(new BufferedInputStream(new FileInputStream(fileName)), this);
        }

        public SimpleTextReader generateReader(File file) throws IOException {
            return new SimpleTextReader(new BufferedInputStream(new FileInputStream(file)), this);
        }

        public Template trim() {
            this._trim = true;
            return this;
        }
    }

    SimpleTextReader(InputStream is, Template template) {
        this.template = template;
        this.is = is;
        this.encoding = template._encoding;

        List<Filter> filterz = new ArrayList<>();
        if (template._ignoreWhiteSpaceLines) {
            filterz.add(StringFilters.PASS_ONLY_TEXT);
        }
        if (template._regexp != null) {
            filterz.add(StringFilters.newRegexpFilter(template._regexp));
        }
        if (template._ignorePrefix != null) {
            filterz.add(new IgnorePrefixFilter(template._ignorePrefix));
        }

        this.filters = filterz.toArray(new Filter[filterz.size()]);

        this.trim = template._trim;
    }

    private class IgnorePrefixFilter implements Filter<String> {
        String[] tokens;

        private IgnorePrefixFilter(String... token) {
            checkNotNull(token, "Cannot initialize Filter with null string.");
            this.tokens = token;
        }

        public boolean canPass(String s) {
            for (String token : tokens) {
                if (s.startsWith(token))
                    return false;
            }
            return true;
        }
    }

    /**
     * Creates a FileReader using the File.
     *
     * @param file a file.
     * @throws NullPointerException if file is null.
     * @throws java.io.IOException  if File does not exist
     */
    public SimpleTextReader(File file) throws IOException {
        checkNotNull(file, "File name cannot be null..");
        this.is = new BufferedInputStream(new FileInputStream(file));
        encoding = Charset.defaultCharset().name();
        this.template = new Template(encoding);
    }

    public SimpleTextReader(File file, String encoding) throws IOException {
        checkNotNull(file, "File name cannot be null..");
        this.is = new BufferedInputStream(new FileInputStream(file));
        if (encoding == null)
            this.encoding = Charset.defaultCharset().name();
        else
            this.encoding = encoding;
        this.template = new Template(encoding);
    }

    public SimpleTextReader(String fileName) throws IOException {
        checkNotNull(fileName, "File name cannot be null..");
        this.is = new BufferedInputStream(new FileInputStream(fileName));
        encoding = Charset.defaultCharset().name();
        this.template = new Template(encoding);
    }

    public SimpleTextReader(String fileName, String encoding) throws IOException {
        checkNotNull(fileName, "File name cannot be null..");
        this.is = new BufferedInputStream(new FileInputStream(fileName));
        if (encoding == null)
            this.encoding = Charset.defaultCharset().name();
        else
            this.encoding = encoding;
        this.template = new Template(encoding);
    }

    public SimpleTextReader(InputStream is) {
        checkNotNull(is, "Input Stream cannot be null..");
        this.is = is;
        encoding = Charset.defaultCharset().name();
        this.template = new Template(encoding);
    }

    public SimpleTextReader(InputStream is, String encoding) {
        checkNotNull(is, "Input Stream cannot be null..");
        this.is = is;
        if (encoding == null)
            this.encoding = Charset.defaultCharset().name();
        else
            this.encoding = encoding;
        this.template = new Template(encoding);
    }

    /**
     * Returns a new SimpleTextReader that skips the whitespace lines and trims lines.
     *
     * @param file file
     * @return a new SimpleTextReader
     * @throws java.io.IOException if a porblem occurs while accessing file.
     */
    public static SimpleTextReader trimmingUTF8Reader(File file) throws IOException {
        return new Builder(file).encoding("utf-8").trim().ignoreWhiteSpaceLines().build();
    }

    /**
     * Returns a new UTF-8 LineIterator that skips the whitespace lines and trims lines.
     *
     * @param file file
     * @return a new LineIterator
     * @throws java.io.IOException if a porblem occurs while accessing file.
     */
    public static LineIterator trimmingUTF8LineIterator(File file) throws IOException {
        return new Builder(file).encoding("utf-8").trim().ignoreWhiteSpaceLines().build().getLineIterator();
    }

    /**
     * Returns a new UTF-8 IterableLineReader that skips the whitespace lines and trims lines.
     *
     * @param file file
     * @return a new IterableLineReader
     * @throws java.io.IOException if a porblem occurs while accessing file.
     */
    public static IterableLineReader trimmingUTF8IterableLineReader(File file) throws IOException {
        return new Builder(file).trim().ignoreWhiteSpaceLines().build().getIterableReader();
    }

    /**
     * Returns a new SimpleTextReader that skips the whitespace lines and trims lines.
     *
     * @param is       input stream to read.
     * @param encoding character encoding. if null, default encoding is used.
     * @return a new SimpleTextReader
     * @throws java.io.IOException if a porblem occurs while accessing file.
     */
    public static SimpleTextReader trimmingReader(InputStream is, String encoding) throws IOException {
        return new Builder(is).encoding(encoding).trim().ignoreWhiteSpaceLines().build();
    }

    /**
     * Returns a new LineIterator that skips the whitespace lines and trims lines.
     *
     * @param is       input stream to read.
     * @param encoding character encoding. if null, default encoding is used.
     * @return a new LineIterator
     * @throws java.io.IOException if a porblem occurs while accessing file.
     */
    public static LineIterator trimmingLineIterator(InputStream is, String encoding) throws IOException {
        return new Builder(is).encoding(encoding).trim().ignoreWhiteSpaceLines().build().getLineIterator();
    }

    /**
     * Returns a new IterableLineReader that skips the whitespace lines and trims lines.
     *
     * @param is       input stream to read.
     * @param encoding character encoding.
     * @return a new IterableLineReader
     * @throws java.io.IOException if a porblem occurs while accessing file.
     */
    public static IterableLineReader trimmingIterableLineReader(InputStream is, String encoding) throws IOException {
        return new Builder(is).encoding(encoding).trim().ignoreWhiteSpaceLines().build().getIterableReader();
    }

    /**
     * returns the current encoding.
     *
     * @return current encoding.
     */
    public String getEncoding() {
        return encoding;
    }

    /**
     * it generates a new SimpleTextReader using this.
     *
     * @param is an input stream for reader.
     * @return a new SimpleTextReader having the same attribues of this one.
     * @throws java.io.IOException if there is an error while accessing the input stream.
     */
    public SimpleTextReader cloneForStream(InputStream is) throws IOException {
        return this.template.generateReader(is);
    }

    /**
     * it generates a new SimpleTextReader using this.
     *
     * @param file File for the new Reader.
     * @return a new SimpleTextReader having the same attribues of this one.
     * @throws java.io.IOException if there is an error while accessing the file.
     */
    public SimpleTextReader cloneForFile(File file) throws IOException {
        return this.template.generateReader(file);
    }

    /**
     * converts an input stream data to byte array. careful with memory usage here.
     *
     * @return a byte array representing the stream data.
     * @throws java.io.IOException  if an error occurs during the read or write of the streams.
     * @throws NullPointerException if filename is null
     */
    public byte[] asByteArray() throws IOException {
        return IOs.readAsByteArray(is);
    }

    /**
     * Reads the entire file as a single string. Use with caution for big files.
     *
     * @return simgle string representation.
     * @throws java.io.IOException if an IO error occurs
     */
    public String asString() throws IOException {
        String res = IOs.readAsString(getReader());
        if (trim)
            return res.trim();
        else return res;
    }

    /**
     * Reads a reader as a list of strings. each item represents one line in the reader.
     *
     * @return a list of Strings from the reader.
     * @throws java.io.IOException if an io error occurs
     */
    public List<String> asStringList() throws IOException {
        return IOs.readAsStringList(getReader(), trim, filters);
    }

    /**
     * return a buffered reader for the file
     *
     * @return buffered reader
     * @throws RuntimeException    if file does not exist
     * @throws java.io.IOException if file does not exist or encoding is not available
     */
    BufferedReader getReader() throws IOException {
        return IOs.getReader(is, encoding);

    }

    /**
     * returns an IterableLineReader. This is expecially useful to use in enhanced for loops.
     * if all the elements are consumed, the resources will be closed automatically.
     *
     * @return a new IterableLineReader instance.
     * @throws java.io.IOException if file does not exist, or encoding is not supported.
     */
    public IterableLineReader getIterableReader() throws IOException {
        return new IterableLineReader(getReader(), trim, filters);
    }

    /**
     * returns a LineIterator. it is suggested to close th iterator in a finally block.
     *
     * @return an IterableLineReader that can be iterated for lines.
     * @throws java.io.IOException if file does not exist, or encoding is not supported.
     */
    public LineIterator getLineIterator() throws IOException {
        return new LineIterator(getReader(), trim, filters);
    }

    /**
     * counts the lines. if there are constraints while creating the reader (eg: not reading empty lines),
     * it counts ONLY the lines that are allowed to be read.
     *
     * @return line count.
     * @throws java.io.IOException if there is a problem while accesing the file.
     */
    public long countLines() throws IOException {
        long i;
        try (LineIterator li = getLineIterator()) {
            i = 0;
            while (li.hasNext()) {
                i++;
                li.next();
            }
            return i;
        }
    }

    /**
     * Closes the stream silently.
     *
     * @throws java.io.IOException
     */
    public void close() throws IOException {
        IOs.closeSilently(is);
    }
}
