/*
 *
 * Copyright (c) 2008, All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Portions of the code may be copied from Google Collections
 * or Apache Commons projects.
 */

package zemberek.core.io;

import java.io.*;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * this class has IO operations
 */
public class IOs {

    private static final int BYTE_BUFFER_SIZE = 1 << 20;

    public static final String LINE_SEPARATOR;
    public static final int CHAR_BUFFER_SIZE = 1 << 20;

    static {
        // avoid security issues
        StringWriter buf = new StringWriter(4);
        PrintWriter out = new PrintWriter(buf);
        out.println();
        LINE_SEPARATOR = buf.toString();
    }

    private IOs() {
    }

    /**
     * Reads a buffered reader as a single string. If there are multi lines, it appends a LINE SEPARATOR.
     *
     * @param reader a reader
     * @return simple string representation of the entire reader. careful with the memory usage.
     * @throws NullPointerException: if reader is null.
     * @throws java.io.IOException   if an IO error occurs.
     */
    public static String readAsString(BufferedReader reader) throws IOException {
        try {
            checkNotNull(reader, "reader cannot be null");
            StringBuilder sb = new StringBuilder();
            String s;
            while ((s = reader.readLine()) != null) {
                sb.append(s).append(LINE_SEPARATOR);
            }
            if (sb.length() >= LINE_SEPARATOR.length())
                sb.delete(sb.length() - LINE_SEPARATOR.length(), sb.length());
            return sb.toString();
        } finally {
            closeSilently(reader);
        }
    }

    /**
     * Reads a reader as a list of strings. each item represents one line in the reader.
     *
     * @param reader a reader
     * @return List of strings.
     * @throws NullPointerException: if reader is null.
     * @throws java.io.IOException   if an IO error occurs.
     */
    public static List<String> readAsStringList(BufferedReader reader)
            throws IOException {
        try {
            checkNotNull(reader, "reader cannot be null");
            String s;
            List<String> res = new ArrayList<>();
            while ((s = reader.readLine()) != null)
                res.add(s);
            return res;
        } finally {
            closeSilently(reader);
        }
    }


    /**
     * Reads a reader as a list of strings. each item represents one line in the reader which passes the Filters.
     *
     * @param reader  a reader
     * @param trim    trims the lines if set
     * @param filters zero or more StringFilter. if there are more than one all filters needs to pass the string.
     * @return List of strings.
     * @throws NullPointerException: if reader is null.
     * @throws java.io.IOException   if an IO error occurs.
     */
    public static List<String> readAsStringList(BufferedReader reader, boolean trim, Filter... filters)
            throws IOException {
        try {
            checkNotNull(reader, "reader cannot be null");
            String s;
            List<String> res = new ArrayList<>();
            while ((s = reader.readLine()) != null) {
                if (trim)
                    s = s.trim();
                if (filters.length == 0 || StringFilters.canPassAll(s, filters)) {
                    res.add(s);
                }
            }
            return res;
        } finally {
            closeSilently(reader);
        }
    }

    /**
     * closes the <code>closeables</code> silently, meaning that if the Closeable is null,
     * or if it throws an exception during close() operation it only creates a system error output,
     * does not throw an exception.
     * this is especially useful when you need to close one or more resources in finally blocks.
     * This method should only be called in finalize{} blocks or wherever it really makes sense.
     *
     * @param closeables zero or more closeable.
     */
    public static void closeSilently(Closeable... closeables) {
        // if closeables is null, return silently.
        if (closeables == null) return;

        for (Closeable closeable : closeables) {
            try {
                if (closeable != null)
                    closeable.close();
            } catch (IOException e) {
                System.err.println("IO Exception during closing stream (" + closeable + ")." + e);
            }
        }
    }

    /**
     * Returns a BufferedReader for the input stream.
     *
     * @param is input stream
     * @return a bufferedReader for the input stream.
     */
    public static BufferedReader getReader(InputStream is) {
        return new BufferedReader(new InputStreamReader(is), CHAR_BUFFER_SIZE);
    }

    /**
     * Returns a Buffered reader for the given input stream and charset. if charset is UTF-8
     * it explicitly checks for UTF-8 BOM information.
     *
     * @param is      input stream for the reader.
     * @param charset charset string, if null,empty or has only whitespace, system uses default encoding.
     * @return BufferedReader
     * @throws java.io.IOException: if the given encoding is not supported, or an error occurs
     *                      if given charset is utf-8 and an IO error occurs during utf-8 BOM detection operation.
     */
    public static BufferedReader getReader(InputStream is, String charset) throws IOException {
        checkNotNull(is, "input stream cannot be null");
        if (!Strings.hasText(charset))
            return getReader(is);
        if (charset.trim().equalsIgnoreCase("utf-8"))
            return new BufferedReader(new InputStreamReader(forceUTF8(is), "utf-8"), CHAR_BUFFER_SIZE);
        else
            return new BufferedReader(new InputStreamReader(is, charset), CHAR_BUFFER_SIZE);

    }

    /**
     * returns a BufferedWriter for the output stream.
     *
     * @param os output stream
     * @return a bufferedReader for the output stream.
     */
    public static BufferedWriter getBufferedWriter(OutputStream os, String encoding) {
        try {
            return new BufferedWriter(new OutputStreamWriter(os, encoding), CHAR_BUFFER_SIZE);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * returns a PrintWriter for the output stream.
     *
     * @param os output stream
     * @return a bufferedReader for the output stream.
     */
    public static PrintWriter getPrintWriter(OutputStream os) {
        return new PrintWriter(new BufferedWriter(new OutputStreamWriter(os), CHAR_BUFFER_SIZE));
    }

    /**
     * returns a BufferedWriter for the output stream.
     *
     * @param os      output stream
     * @param charset encoding string.
     * @return a bufferedReader for the output stream.
     * @throws java.io.UnsupportedEncodingException
     *          if encoding is not supported.
     */
    public static BufferedWriter getWriter(OutputStream os, String charset)
            throws UnsupportedEncodingException {
        return new BufferedWriter(new OutputStreamWriter(os, charset), CHAR_BUFFER_SIZE);

    }

    /**
     * returns a PrintWriter for the output stream.
     *
     * @param os      output stream
     * @param charset encoding string.
     * @return a PrintWriter for the output stream.
     * @throws java.io.UnsupportedEncodingException
     *          if encoding is not supported.
     */
    public static PrintWriter getPrintWriter(OutputStream os, String charset)
            throws UnsupportedEncodingException {
        return new PrintWriter(new BufferedWriter(new OutputStreamWriter(os, charset), CHAR_BUFFER_SIZE));
    }

    /**
     * returns an IterableLineReader backed by a LineIterator. Can be used directly in enhanced for loops.
     * Please note that if not all the lines are read, reader will not be closed. So, it is suggested
     * to close the IterableLineReader in a finally block using {@link #closeSilently(java.io.Closeable...)}
     *
     * @param is an input stream.
     * @return an IterableLineReader that can be iterated for lines.
     */
    public static IterableLineReader getIterableReader(InputStream is) {
        return new IterableLineReader(getReader(is));
    }

    /**
     * returns a LineIterator. it is suggested to close th eiterator in a finally block.
     *
     * @param is input stream to read.
     * @return an IterableLineReader that can be iterated for lines.
     */
    public LineIterator getLineIterator(InputStream is) {
        return new LineIterator(getReader(is));
    }

    /**
     * returns an IterableLineReader backed by a LineIterator. Can be used directly in enhanced for loops.
     * Please note that if not all the lines are read, reader will not be closed. So, it is suggested
     * to close the IterableLineReader in a finally block using {@link #closeSilently(java.io.Closeable...)}
     *
     * @param is      input stream
     * @param charset the charset.
     * @return an IterableLineReader that can be iterated for lines.
     * @throws java.io.IOException if charset is not available, or there is an error ocurred during utf-8 test.
     */
    public static IterableLineReader getIterableReader(InputStream is, String charset) throws IOException {
        return new IterableLineReader(getReader(is, charset));
    }


    /**
     * returns a LineIterator. it is suggested to close th eiterator in a finally block.
     *
     * @param is      input stream to read.
     * @param charset charset
     * @return an IterableLineReader that can be iterated for lines.
     * @throws java.io.IOException if charset is not available, or there is an error ocurred during utf-8 test.
     */
    public LineIterator getLineIterator(InputStream is, String charset) throws IOException {
        return new LineIterator(getReader(is, charset));
    }

    /**
     * Copies oan input stream content to an output stream.
     * Once the copy is finished streams will be closed.
     *
     * @param is input stream
     * @param os output stream
     * @return copied byte count.
     * @throws java.io.IOException if an IO error occurs.
     */
    public static long copy(InputStream is, OutputStream os) throws IOException {
        return copy(is, os, false);
    }

    /**
     * Copies oan input stream content to an output stream.
     * Once the copy is finished only the input strean is closed by default. Closing of the
     * output stream depends on the boolean parameter..
     *
     * @param is             input stream
     * @param os             output stream
     * @param keepOutputOpen if true, output stream will not be closed.
     * @return copied byte count.
     * @throws java.io.IOException if an IO error occurs.
     */
    static long copy(InputStream is, OutputStream os, boolean keepOutputOpen) throws IOException {
        long total = 0;
        try {
            checkNotNull(is, "Input stream cannot be null.");
            checkNotNull(os, "Output stream cannot be null.");
            byte[] buf = new byte[BYTE_BUFFER_SIZE];
            int i;
            while ((i = is.read(buf)) != -1) {
                os.write(buf, 0, i);
                total += i;
            }
        } finally {
            closeSilently(is);
            if (!keepOutputOpen)
                closeSilently(os);
        }
        return total;
    }

    /**
     * compares two input stream contents. Streams will be closed after the operation is ended or interrupted.
     * <p/>
     * copied and modified from Apache Commons-io
     *
     * @param is1 first input stream
     * @param is2 second input stream.
     * @return true if contents of two streams are equal.
     * @throws NullPointerException if one of the stream is null
     * @throws java.io.IOException          if an IO exception occurs while reading streams.
     */
    public static boolean contentEquals(InputStream is1, InputStream is2) throws IOException {
        try {
            checkNotNull(is1, "Input stream 1 cannot be null.");
            checkNotNull(is2, "Input stream 2 cannot be null.");
            if (!(is1 instanceof BufferedInputStream)) {
                is1 = new BufferedInputStream(is1, CHAR_BUFFER_SIZE);
            }
            if (!(is2 instanceof BufferedInputStream)) {
                is2 = new BufferedInputStream(is2, CHAR_BUFFER_SIZE);
            }

            int ch = is1.read();
            while (-1 != ch) {
                int ch2 = is2.read();
                if (ch != ch2) {
                    return false;
                }
                ch = is1.read();
            }
            int ch2 = is2.read();
            return (ch2 == -1);
        } finally {
            closeSilently(is1, is2);
        }
    }

    /**
     * Calculates the MD5 of a stream.
     *
     * @param is a non null stream
     * @return MD5 of the stream as byte array.
     * @throws java.io.IOException          if an error occurs during read of the stream.
     * @throws NullPointerException if input stream is null
     */
    public static byte[] calculateMD5(InputStream is) throws IOException {
        try {
            checkNotNull(is, "input stream cannot be null.");
            MessageDigest digest;
            digest = MessageDigest.getInstance("MD5");

            byte[] buffer = new byte[BYTE_BUFFER_SIZE];
            int read;

            while ((read = is.read(buffer)) > 0)
                digest.update(buffer, 0, read);
            return digest.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 is not available." + e);
        } finally {
            closeSilently(is);
        }
    }

    private static final byte[] bomBytes = new byte[]{(byte) 0xef, (byte) 0xbb, (byte) 0xbf};

    /**
     * UTF encoded text has a a special information in the beginning of the file called BOM.
     * BOM information is mandatory for all UTF encodings, except UTF-8.
     * Unfortunately, Java assumes UTF-8 files does not have the BOM information. But windows systems usually
     * put the UTF-8 BOM data in the begining of the file. This causes errors in Java applications. This method is a workaround
     * for reading UTF-8 encoded files.
     * Checks if the stream has UTF-8 BOM information in the beginning of a stream. if it has the information,
     * it returns a PushbackStream backed by the input stream but three bytes already read. if BOM does not exist,
     * it returns a PushbackStream backed by the input stream.
     *
     * @param is input stream
     * @return if it is an UT8, returns the input stream with three characters already read.
     * @throws java.io.IOException          if there is an error during reading of first three bytes
     * @throws NullPointerException if input stream is null
     */
    static InputStream forceUTF8(InputStream is) throws IOException {
        checkNotNull(is, "input stream cannot be null.");
        PushbackInputStream pis = new PushbackInputStream(is, bomBytes.length);
        byte[] bomRead = new byte[bomBytes.length];
        if (pis.read(bomRead, 0, bomBytes.length) == -1) {
            return is;
        }
        if (!Arrays.equals(bomRead, bomBytes)) {
            pis.unread(bomRead);
        }
        return pis;
    }

    /**
     * checks if a stream contains UTF8 BOM information at the begininning.
     *
     * @param is inputstream
     * @return if input stream contains UTF-8 bom bytes, return true.
     * @throws java.io.IOException if there is a problem during reading bytes from stream.
     */
    static boolean containsUTF8Bom(InputStream is) throws IOException {
        try {
            checkNotNull(is, "input stream cannot be null.");
            byte[] bomRead = new byte[bomBytes.length];
            return is.read(bomRead, 0, bomBytes.length) != -1 && Arrays.equals(bomRead, bomBytes);
        } finally {
            closeSilently(is);
        }
    }

    /**
     * converts an input stream data to byte array. careful with memory usage here. if aim is to transfer bytes from
     * one stream to another, use {@link #copy(java.io.InputStream, java.io.OutputStream)} instead.
     *
     * @param is , an input stream
     * @return a byte array representing the stream data.
     * @throws java.io.IOException          if an error occurs during the read or write of the streams.
     * @throws NullPointerException if input stream is null
     */
    public static byte[] readAsByteArray(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            checkNotNull(is, "input stream cannot be null.");
            int b;
            byte[] buffer = new byte[BYTE_BUFFER_SIZE];
            while ((b = is.read(buffer, 0, buffer.length)) != -1) {
                baos.write(buffer, 0, b);
            }
            return baos.toByteArray();
        } finally {
            closeSilently(is, baos);
        }
    }

    /**
     * Writes the value of each item in a collection to
     * an <code>OutputStream</code> line by line, using the default character
     * encoding of the platform. A Line separator will be added to each line.
     * If there is a null value, an empty line will be added.
     * The output stream will Not be closed once the operation is finished.
     * <p/>
     * copied and modified from Apache Commons-io
     *
     * @param lines  the lines to write, null entries produce blank lines
     * @param output the <code>OutputStream</code> to write to, not null, not closed
     * @throws NullPointerException if the output is null
     * @throws java.io.IOException          if an I/O error occurs
     */
    public static void writeLines(Collection<String> lines, OutputStream output) throws IOException {
        if (lines == null)
            return;

        for (String line : lines) {
            if (line != null)
                output.write(line.getBytes());
            output.write(LINE_SEPARATOR.getBytes());
        }
    }

    /**
     * Writes the <code>toString()</code> value of each item in a collection to
     * an <code>OutputStream</code> line by line, using the default character
     * encoding of the platform. if an element is null, nothing is written for it.
     * The stream will Not be closed once the operation is finished.
     * <p/>
     * copied and modified from commons-io
     *
     * @param lines  the lines to write, null entries produce blank lines
     * @param output the <code>BufferedWriter</code> to write to, not null, not closed
     * @throws NullPointerException if the output is null
     * @throws java.io.IOException          if an I/O error occurs
     */
    public static void writeLines(Collection<String> lines,
                                  BufferedWriter output) throws IOException {
        writeToStringLines(lines, output);
    }

    /**
     * Writes the string to <code>OutputStream</code>
     * The stream will Not be closed once the operation is finished.
     * <p/>
     *
     * @param s      String to write.
     * @param output the <code>OutputStream</code> to write to, not null, not closed
     * @throws NullPointerException if the output is null
     * @throws java.io.IOException          if an I/O error occurs
     */
    public static void writeString(String s, OutputStream output) throws IOException {
        writeString(s, output, null);
    }

    /**
     * Writes the string to <code>OutputStream</code>
     * The stream will Not be closed once the operation is finished.
     * <p/>
     *
     * @param s        String to write.
     * @param output   the <code>OutputStream</code> to write to, not null, not closed
     * @param encoding character encoding.
     * @throws NullPointerException if the output is null
     * @throws java.io.IOException          if an I/O error occurs
     */
    public static void writeString(String s,
                                   OutputStream output,
                                   String encoding) throws IOException {
        if (Strings.isNullOrEmpty(s))
            return;
        if (encoding == null)
            encoding = Charset.defaultCharset().name();
        output.write(s.getBytes(encoding));
    }

    /**
     * retrieves a classpath resource as stream.
     *
     * @param resource resource name. may or may not contain a / symbol.
     * @return an InputStrean obtained from the resource
     */
    public static InputStream getClassPathResourceAsStream(String resource) {
        if (!resource.startsWith("/"))
            resource = "/" + resource;
        return IOs.class.getResourceAsStream(resource);
    }


    /**
     * Writes the <code>toString()</code> value of each item in a collection to
     * an <code>OutputStream</code> line by line, using the default character
     * encoding of the platform. if an element is null, nothing is written for it.
     * The stream will Not be closed once the operation is finished.
     * <p/>
     * copied and modified from commons-io
     *
     * @param lines the lines to write, null entries produce blank lines
     * @throws NullPointerException if the output is null
     * @throws java.io.IOException          if an I/O error occurs
     */
    public static void writeToStringLines(
            Collection<?> lines,
            BufferedWriter writer) throws IOException {

        if (lines == null)
            return;

        long i = 0;
        for (Object line : lines) {
            String l = "";
            if (line != null)
                l = line.toString();

            if (!Strings.isNullOrEmpty(l))
                writer.write(l);
            else {
                writer.write(LINE_SEPARATOR);
                continue;
            }
            if (++i < lines.size())
                writer.write(LINE_SEPARATOR);
        }
    }


}