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

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.Reader;
import java.util.Iterator;

/**
 * This class wraps a LineIterator. It is useful to use this in an enhanced for loop.
 */
public class IterableLineReader implements Iterable<String>, AutoCloseable {

    private final BufferedReader bufferedReader;
    private boolean trim;
    private Filter filters[] = new Filter[0];

    public IterableLineReader(Reader reader) {
        if (reader instanceof BufferedReader)
            this.bufferedReader = (BufferedReader) reader;
        else
            this.bufferedReader = new BufferedReader(reader);

    }

    public IterableLineReader(Reader reader, boolean trim, Filter[] filters) {
        if (reader instanceof BufferedReader)
            this.bufferedReader = (BufferedReader) reader;
        else
            this.bufferedReader = new BufferedReader(reader);
        this.filters = filters;
        this.trim = trim;

    }

    public void close() {
        IOs.closeSilently(bufferedReader);
    }

    public Iterator<String> iterator() {
        return new LineIterator(bufferedReader, trim, filters);
    }
}
