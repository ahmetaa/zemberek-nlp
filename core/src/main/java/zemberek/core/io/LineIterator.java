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

import com.google.common.base.Preconditions;

import java.io.*;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * An Iterator over the lines in a <code>Reader</code>.
 * <p/>
 * <code>LineIterator</code> holds a reference to an open <code>Reader</code>.
 * if there hasNext() returns false, it automatically closes the reader. if somewhat
 * an early return is possible iterator or the reader should be closed by calling {@link #close()} method.
 * <p/>
 * The recommended usage pattern is:
 * <pre>
 * ;
 * try(LineIterator it = new LineIterator(Files.getReader("filename", "UTF-8"))) {
 *   while (it.hasNext()) {
 *     String line = it.next();
 *     /// do something with line
 *   }
 * }
 * </pre>
 * <p/>
 * This class uses code from Apache commons io LineIterator class. however, it's behavior is slightly different.
 */
public class LineIterator implements Iterator<String>, AutoCloseable {

    private final BufferedReader bufferedReader;
    /**
     * The current line.
     */
    private String cachedLine;
    /**
     * A flag indicating if the iterator has been fully read.
     */
    private boolean finished = false;

    private boolean trim = false;

    private Filter filters[] = new Filter[0];

    public LineIterator(InputStream is) {
        Preconditions.checkNotNull(is, "InputStream cannot be null!");
        this.bufferedReader = IOs.getReader(is);
    }

    public LineIterator(Reader reader) {
        Preconditions.checkNotNull(reader, "Reader cannot be null!");
        if (reader instanceof BufferedReader)
            this.bufferedReader = (BufferedReader) reader;
        else
            this.bufferedReader = new BufferedReader(reader);
    }

    public LineIterator(Reader reader, boolean trim, Filter... filters) {
        Preconditions.checkNotNull(reader, "Reader cannot be null!");
        if (reader instanceof BufferedReader) {
            this.bufferedReader = (BufferedReader) reader;
        } else
            this.bufferedReader = new BufferedReader(reader);
        if (filters != null && filters.length > 0)
            this.filters = filters;
        this.trim = trim;
    }

    public boolean hasNext() {
        if (cachedLine != null) {
            return true;
        } else if (finished) {
            close();
            return false;
        } else {
            try {
                String line;
                do {
                    line = bufferedReader.readLine();
                    if (line != null && trim) {
                        line = line.trim();
                    }
                }
                while (line != null && filters.length > 0 && !StringFilters.canPassAll(line, filters));

                if (line == null) {
                    finished = true;
                    close();
                    return false;
                } else {
                    cachedLine = line;
                    return true;
                }
            } catch (IOException ioe) {
                close();
                throw new IllegalStateException(ioe.toString());
            }
        }
    }

    public String next() {
        if (!hasNext()) {
            close();
            throw new NoSuchElementException("No more lines");
        }
        String currentLine = cachedLine;
        cachedLine = null;
        return currentLine;
    }

    public void remove() {
        throw new UnsupportedOperationException("remove() is not implemented in LineIterator class.");
    }

    public void close() {
        IOs.closeSilently(bufferedReader);
    }
}
