package zemberek.core.io;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class MultiFileLineIterator implements Iterator<String> {

    private int fileCursor = 0;
    private LineIterator currentIterator;
    private final List<File> files;
    private SimpleTextReader.Template template = new SimpleTextReader.Template();

    public MultiFileLineIterator(File... files) throws IOException {
        this.files = new ArrayList<File>(Arrays.asList(files));
        currentIterator = template.generateReader(files[0]).getLineIterator();
    }

    public MultiFileLineIterator(SimpleTextReader.Template template, File... files) throws IOException {
        this.files = new ArrayList<File>(Arrays.asList(files));
        currentIterator = template.generateReader(files[0]).getLineIterator();
        this.template = template;
    }

    public MultiFileLineIterator(List<File> files) throws IOException {
        this.files = files;
        currentIterator = template.generateReader(files.get(0)).getLineIterator();
    }

    public MultiFileLineIterator(SimpleTextReader.Template template, List<File> files) throws IOException {
        this.files = new ArrayList<File>(files);
        this.template = template;
        currentIterator = template.generateReader(files.get(0)).getLineIterator();
    }

    public boolean hasNext() {
        while (!currentIterator.hasNext()) {
            fileCursor++;
            currentIterator.close();
            if (fileCursor < files.size())
                try {
                    currentIterator = template.generateReader(files.get(fileCursor)).getLineIterator();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            else return false;
        }
        return true;
    }

    public String next() {
        return currentIterator.next();
    }

    public void remove() {
        throw new UnsupportedOperationException("remove ise not supported here.");
    }
}