package zemberek.core.process;

import com.google.common.base.Joiner;

import java.io.*;

public class ProcessRunner {

    File processRoot;

    public ProcessRunner(File processRoot) {
        this.processRoot = processRoot;
    }

    public void execute(ProcessBuilder pb) throws IOException, InterruptedException {
        System.out.println(Joiner.on(" ").join(pb.command()));
        Process process = pb.redirectErrorStream(true).directory(processRoot).start();
        new AsyncPipe(process.getErrorStream(), System.err).start();
        new AsyncPipe(process.getInputStream(), System.out).start();
        process.waitFor();
    }

    public void pipe(InputStream is, OutputStream os, ProcessBuilder... builders) throws IOException, InterruptedException {
        int i = 0;
        File tempin;
        File tempout = File.createTempFile("pipe", "txt");
        OutputStream tos;
        for (ProcessBuilder builder : builders) {
            if (i == 0) {
            } else {
                tempin = tempout;
                is = new FileInputStream(tempin);
            }
            if (i == builders.length - 1) {
                if (os == null) {
                    tos = System.out;
                } else tos = os;
            } else {
                tempout = File.createTempFile("pipe", "txt");
                tos = new FileOutputStream(tempout);
            }
            Process process = builder.redirectErrorStream(true).directory(processRoot).start();
            new AsyncPipe(process.getErrorStream(), System.err, false).start();
            new AsyncPipe(process.getInputStream(), tos).start();
            if (is != null)
                new AsyncPipe(is, process.getOutputStream()).start();
            i++;
            process.waitFor();
        }
    }

    public void execute(ProcessBuilder pb, InputStream is, OutputStream os) throws IOException, InterruptedException {
        System.out.println(Joiner.on(" ").join(pb.command()));
        execute(pb.directory(processRoot).start(), is, os);
    }

    public void execute(ProcessBuilder pb, OutputStream os) throws IOException, InterruptedException {
        System.out.println(Joiner.on(" ").join(pb.command()));
        execute(pb.directory(processRoot).start(), os);
    }

    public void execute(Process process) throws IOException, InterruptedException {
        new AsyncPipe(process.getErrorStream(), System.err).start();
        new AsyncPipe(process.getInputStream(), System.out).start();
        process.waitFor();
    }

    public void execute(Process process, InputStream is, OutputStream os) throws IOException, InterruptedException {
        new AsyncPipe(process.getInputStream(), os).start();
        new AsyncPipe(is, process.getOutputStream()).start();
        new AsyncPipe(process.getErrorStream(), System.err, false).start();
        process.waitFor();
    }

    public void execute(Process process, OutputStream os) throws IOException, InterruptedException {
        new AsyncPipe(process.getInputStream(), os).start();
        new AsyncPipe(process.getErrorStream(), System.err, false).start();
        process.waitFor();
    }

    public void execute(ProcessBuilder pb, File inFile, File outFile) throws IOException, InterruptedException {
        execute(pb, new FileInputStream(inFile), new FileOutputStream(outFile));
    }

    public void execute(ProcessBuilder pb, File outFile) throws IOException, InterruptedException {
        execute(pb, new FileOutputStream(outFile));
    }

    /**
     * A thread copies an input stream to an output stream.
     */
    class AsyncPipe extends Thread {
        InputStream is;
        OutputStream os;
        boolean closeStreams;

        AsyncPipe(InputStream is, OutputStream os) {
            this.is = is;
            this.os = os;
            closeStreams = true;
        }

        AsyncPipe(InputStream is, OutputStream os, boolean closeStreams) {
            this.is = is;
            this.os = os;
            this.closeStreams = closeStreams;
        }

        @Override
        public void run() {
            try {
                synchronized (this) {
                    byte[] buf = new byte[4096];
                    int i;
                    while ((i = is.read(buf)) != -1) {
                        os.write(buf, 0, i);
                    }
                    if (closeStreams) {
                        os.close();
                        is.close();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * A thread copies an input stream to an output stream.
     */
    class SyncPipe  {
        InputStream is;
        OutputStream os;
        boolean closeStreams;

        SyncPipe(InputStream is, OutputStream os) {
            this.is = is;
            this.os = os;
            closeStreams = true;
        }

        SyncPipe(InputStream is, OutputStream os, boolean closeStreams) {
            this.is = is;
            this.os = os;
            this.closeStreams = closeStreams;
        }

        public void pipe() {
            try {
                synchronized (this) {
                    byte[] buf = new byte[4096];
                    int i;
                    while ((i = is.read(buf)) != -1) {
                        os.write(buf, 0, i);
                    }
                    if (closeStreams) {
                        os.close();
                        is.close();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
