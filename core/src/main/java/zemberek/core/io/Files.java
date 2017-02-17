package zemberek.core.io;

import java.io.*;
import java.text.Collator;
import java.util.*;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Helper methods for File operations.
 * File operations throws RuntimeException instead of IOException.
 */
public class Files {

    private static class FileModificationTimeComparatorDesc implements Comparator<File> {
        public int compare(File f1, File f2) {
            if (f1.lastModified() > f2.lastModified())
                return 1;
            else return f1.lastModified() == f2.lastModified() ? 0 : -1;
        }
    }

    private static class FileModificationTimeComparatorAsc implements Comparator<File> {
        public int compare(File f1, File f2) {
            if (f1.lastModified() < f2.lastModified())
                return 1;
            else return f1.lastModified() == f2.lastModified() ? 0 : -1;
        }
    }

    public static final Comparator<File> FILE_MODIFICATION_TIME_COMPARATOR_ASC = new FileModificationTimeComparatorAsc();
    public static final Comparator<File> FILE_MODIFICATION_TIME_COMPARATOR_DESC = new FileModificationTimeComparatorDesc();

    public static Comparator<File> getNameSortingComparator(final Locale locale) {
        return new Comparator<File>() {
            public int compare(File file, File file1) {
                Collator coll = Collator.getInstance(locale);
                return coll.compare(file.getName(), file1.getName());
            }
        };
    }

    public static Comparator<File> getAbsolutePathSortingComparator(final Locale locale) {
        return new Comparator<File>() {
            public int compare(File file, File file1) {
                Collator coll = Collator.getInstance(locale);
                return coll.compare(file.getAbsolutePath(), file1.getAbsolutePath());
            }
        };
    }

    public static Comparator<File> getNameSortingComparator() {
        return new Comparator<File>() {
            public int compare(File file, File file1) {
                return file.getName().compareToIgnoreCase(file1.getName());
            }
        };
    }

    public static Comparator<File> getAbsolutePathSortingComparator() {
        return new Comparator<File>() {
            public int compare(File file, File file1) {
                return file.getAbsolutePath().compareToIgnoreCase(file1.getAbsolutePath());
            }
        };
    }

    /**
     * This is a file filter using regular expressions. if file path/name matches with regexp
     * it will accept.
     */
    public static class RegexpFilter implements FileFilter {
        final Pattern regexp;

        public RegexpFilter(String regExp) {
            checkNotNull(regExp, "regexp String cannot be null.");
            checkArgument(!Strings.isNullOrEmpty(regExp), "regexp String cannot be empty");
            this.regexp = Pattern.compile(regExp);
        }

        public RegexpFilter(Pattern regExp) {
            checkNotNull(regExp, "regexp Pattern cannot be null.");
            this.regexp = regExp;
        }

        public boolean accept(File pathname) {
            return regexp.matcher(pathname.getPath()).find();
        }
    }

    public static class ExtensionFilter implements FileFilter {

        private final String[] extensions;

        public ExtensionFilter(String... extensions) {
            this.extensions = extensions;
        }

        public boolean accept(File pathname) {
            if (extensions == null || extensions.length == 0)
                return true;
            for (String extension : extensions) {
                if (pathname.getName().endsWith(extension)) return true;
            }
            return false;
        }
    }

    public static FileFilter extensionFilter(String... extensions) {
        if(extensions.length==0) {
            return new AcceptAllFilter();
        }
        return new ExtensionFilter(extensions);
    }

    public static class AcceptAllFilter implements FileFilter {
        public boolean accept(File pathname) {
            return true;
        }
    }

    private Files() {
    }

    /**
     * This deletes files in a directory. it does not go into sub directories, and
     * it does not delete directories.
     *
     * @param files : zero or more files.
     */
    public static void deleteFiles(File... files) {
        for (File s : files) {
            if (s.exists() && !s.isDirectory())
                s.delete();
        }
    }

    /**
     * This deletes files and directories and all child directories and files.
     *
     * @param files : zero or more file or dorectory.
     */
    public static void deleteFilesAndDirs(File... files) {
        for (File file : files) {
            if (file.exists()) {
                if (file.isDirectory())
                    deleteFilesAndDirs(file.listFiles());
                else
                    file.delete();
            }
        }
    }

    /**
     * Crawls into a directory and retrieves all the files in it and its sub directories.
     *
     * @param dir a File representing a directory
     * @return all the files in the
     */
    public static List<File> crawlDirectory(File dir) {
        return crawlDirectory(dir, new AcceptAllFilter());
    }

    /**
     * Crawls into a directory and retrieves all the files in it and its sub directories.
     *
     * @param dir        a File representing a directory
     * @param comparator comparator to apply.
     * @return all the files in the
     */
    public static List<File> getFilesSorted(File dir, Comparator<File> comparator) {
        checkExistingDirectory(dir);
        List<File> files = Arrays.asList(dir.listFiles());
        Collections.sort(files, comparator);
        return files;
    }

    /**
     * Crawls into a directory and retrieves all the files in it and its sub directories.
     *
     * @param dir        a File representing a directory
     * @param comparator comparator to apply.
     * @param filters    filters to apply.
     * @return all the files in the
     */
    public static List<File> getFilesSorted(File dir, Comparator<File> comparator, FileFilter... filters) {
        checkExistingDirectory(dir);
        List<File> files = new ArrayList<File>();
        for (File file : dir.listFiles()) {
            if (filters.length == 0)
                files.add(file);
            else {
                for (FileFilter filter : filters) {
                    if (filter.accept(file)) {
                        files.add(file);
                        break;
                    }
                }
            }
        }
        Collections.sort(files, comparator);
        return files;
    }

    /**
     * Crawls into a directory and retrieves all the files in it and its sub directories.
     * Only the files matching to the filter will be included.
     *
     * @param dir     a File representing a directory
     * @param filters filter
     * @return all the files in the
     */
    public static List<File> crawlDirectory(File dir, FileFilter... filters) {
        return crawlDirectory(dir, true, filters);
    }

    /**
     * Crawls into a directory and retrieves all the files in it and its sub directories.
     * Only the files matching to the filter will be included.
     *
     * @param dir            a File representing a directory
     * @param recurseSubDirs determines if it will recurse to the sub directories.
     * @param filters        filter
     * @return all the files in the
     */
    public static List<File> crawlDirectory(File dir, boolean recurseSubDirs, FileFilter... filters) {
        checkNotNull(dir, "File is null!");
        checkExistingDirectory(dir);
        List<File> files = new ArrayList<File>();
        for (File file : dir.listFiles()) {
            if (file.isDirectory() && recurseSubDirs)
                files.addAll(crawlDirectory(file, true, filters));
            else if (!file.isDirectory()) {
                if (filters.length == 0)
                    files.add(file);
                else {
                    for (FileFilter filter : filters) {
                        if (filter.accept(file)) {
                            files.add(file);
                            break;
                        }
                    }
                }
            }
        }
        return files;
    }

    private static void checkExistingDirectory(File dir) {
        checkNotNull(dir, "Dir is null!");
        checkArgument(dir.exists(), "Directory does not exist! : " + dir);
        checkArgument(dir.isDirectory(), "i was expecting a directory : " + dir);
    }

    /**
     * get all directories under root dir.
     *
     * @param rootDir        root dir to scan
     * @param recurseSubDirs if true, sub directories are also scanned.
     * @return List of directories.
     */
    public static List<File> getDirectories(File rootDir, boolean recurseSubDirs) {
        checkNotNull(rootDir, "File is null!");
        checkArgument(rootDir.isDirectory(), "i was expecting a directory..");
        checkArgument(rootDir.exists(), "Directory does not exist!.");
        List<File> dirs = new ArrayList<>();
        for (File dir : rootDir.listFiles()) {
            if (dir.isDirectory()) {
                if (recurseSubDirs) {
                    dirs.addAll(getDirectories(dir, true));
                }
                dirs.add(dir);
            }
        }
        return dirs;
    }


    /**
     * calculates MD5 of a file.
     *
     * @param file file name
     * @return MD5 result as a byte array.
     * @throws java.io.IOException if an IO error occurs.
     */
    public static byte[] calculateMD5(File file) throws IOException {
        checkIfFileExist(file);
        return IOs.calculateMD5(new FileInputStream(file));
    }

    private static void checkIfFileExist(File file) {
        checkNotNull(file, "file is null!");
        checkArgument(file.exists(), "File does not exist");
        checkArgument(!file.isDirectory(), "This is a directory, not a file..");
    }

    /**
     * Copies all files under srcDir to dstDir.
     * If dstDir does not exist, it will be created.
     *
     * @param srcDir Source directory
     * @param dstDir destination directory
     * @throws java.io.IOException if an exception occurs during copy operation.
     */
    public static void copyDirectory(File srcDir, File dstDir) throws IOException {
        if (srcDir.isDirectory()) {
            java.nio.file.Files.createDirectories(dstDir.toPath());
            String[] children = srcDir.list();
            for (String aChildren : children) {
                copyDirectory(
                        new File(srcDir, aChildren),
                        new File(dstDir, aChildren));
            }
        } else {
            copy(srcDir, dstDir);
        }
    }

    /**
     * copies one file to another.
     *
     * @param src source file
     * @param dst destination file
     * @throws java.io.IOException if there is an error occurs during the read or write of the files
     */
    public static void copy(File src, File dst) throws IOException {
        IOs.copy(new FileInputStream(src), new FileOutputStream(dst));
    }

    /**
     * compares two input file contents.
     *
     * @param file1 first file name
     * @param file2 second file name
     * @return true if contents of two file contents are equal.
     * @throws NullPointerException if one of the filename is null
     * @throws java.io.IOException          if an IO exception occurs while reading streams.
     */
    public static boolean contentEquals(File file1, File file2) throws IOException {
        checkNotNull(file1, "file1 cannot be null.");
        checkNotNull(file2, "file2 cannot be null.");
        checkIfFileExist(file1);
        checkIfFileExist(file2);
        return file1.length() == file2.length() &&
                IOs.contentEquals(new FileInputStream(file1), new FileInputStream(file2));
    }

    /**
     * checks if input file contains UTF-8 BOM information. note that it is not required that UTF-8 files carries this
     * information.
     *
     * @param file file to be checked.
     * @return true if file contains UTF-8 bom data
     * @throws java.io.IOException if there is an error during file access.
     */
    public static boolean containsUTF8Bom(File file) throws IOException {
        return IOs.containsUTF8Bom(new FileInputStream(file));
    }

    /**
     * merges files into a single file. if the target file already exist, it appends it. if target does not
     * exist, it creates it.
     *
     * @param target        target file to be appended.
     * @param filesToAppend filesToMerge
     * @throws java.io.IOException if there is an error during appending files.
     */
    public static void appendFiles(File target, File... filesToAppend) throws IOException {
        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(target, true));
        try {
            for (File file : filesToAppend) {
                IOs.copy(new BufferedInputStream(new FileInputStream(file)), bos, true);
            }
        } finally {
            IOs.closeSilently(bos);
        }
    }

    /**
     * Appends files to an output stream. Output stream is not closed after operation is finished.
     *
     * @param os            output stream to be appended.
     * @param filesToAppend filesToMerge
     * @throws java.io.IOException if there is an error during appending files.
     */
    public static void appendFiles(OutputStream os, File... filesToAppend) throws IOException {
        for (File file : filesToAppend) {
            IOs.copy(new BufferedInputStream(new FileInputStream(file)), os, true);
        }
    }


    /**
     * This method dumps the contents of a file in hex format to console.
     *
     * @param f      file to hex dump.
     * @param amount amount of bytes to write. if this value is -1, then it dupms all the content.
     * @throws java.io.IOException if there is a problem accessing the file.
     */
    public static void hexDump(File f, long amount) throws IOException {
        Dumper.hexDump(new FileInputStream(f), System.out, 20, amount);
    }

    /**
     * This method dumps the contents of a file in hex format to another file.
     *
     * @param f      file to hex dump.
     * @param out    the output file hex values will be written.
     * @param amount amount of bytes to write. if this value is -1, then it dupms all the content.
     * @throws java.io.IOException if there is a problem accessing the file.
     */
    public static void hexDump(File f, File out, long amount) throws IOException {
        Dumper.hexDump(new FileInputStream(f), new FileOutputStream(out), 20, amount);
    }
}

