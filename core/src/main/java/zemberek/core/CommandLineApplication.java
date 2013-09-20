package zemberek.core;

import com.google.common.base.Joiner;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import zemberek.core.io.Words;
import zemberek.core.logging.Log;


import java.io.File;

/**
 * Base class for command line applications. It provides convenience methods for failing and executing applications.
 */
public abstract class CommandLineApplication {

    @Option(name = "-verbosity",
            usage = "Verbosity level. 0-WARN 1-INFO 2-DEBUG 3-TRACE. Default level is 1")
    public int verbosity = 1;

    @Option(name = "-logFile",
            usage = "Log output file.")
    public File logFile;

    /**
     * This is called when application is failed.
     * It prints usage, help and exception stack trace.
     *
     * @param parser parser object
     * @param e      exception that caused failure
     * @param doc    Documentation about what this Application does.
     */
    protected void fail(CmdLineParser parser, Exception e, String doc) {
        parser.setUsageWidth(100);
        System.err.println("Error: " + e.getMessage());
        System.err.println();
        if (doc != null && doc.length() > 0)
            System.err.println(Words.wrap(doc, 100));
        System.err.print("Usage: java -cp \"[CLASS-PATH]\" " + this.getClass().getName());
        parser.printSingleLineUsage(System.err);
        System.err.println();
        System.err.println();
        parser.printUsage(System.err);
        System.err.println();
        System.err.println("Stack Trace:");
        e.printStackTrace(System.err);
        System.exit(-1);
    }

    protected void fail(CmdLineParser parser, Exception e) {
        fail(parser, e, "");
    }

    /**
     * This method allows Applications to be called directly by other classes.
     *
     * @param args program arguments
     */
    public void execute(String... args) {
        CmdLineParser parser = new CmdLineParser(this);
        Log.info("Application is running with arguments: " + Joiner.on(" ").join(args));
        try {
            parser.parseArgument(args);
            Log.info("Verbosity Level: %d" , verbosity);
            switch (verbosity) {
                case 0:
                    Log.setWarn();
                    break;
                case 1:
                    Log.info("Log level is set to INFO.");
                    Log.setInfo();
                    break;
                case 2:
                    Log.info("Log level is set to DEBUG.");
                    Log.setDebug();
                    break;
                case 3:
                    Log.info("Log level is set to TRACE.");
                    Log.setTrace();
                    break;
                default:
                    Log.warn("Undefined verbosity level: " + verbosity + ". INFO level will be used.");
            }
            if(logFile!=null) {
                Log.addFileHandler(logFile.toPath());
                Log.debug("Application log is being recorded to %s file.", logFile);
            }
            run();
        } catch (Exception e) {
            fail(parser, e, getDescription());
        }
    }

    /**
     * @return a string describing the class functionality
     */
    protected abstract String getDescription();

    /**
     * Implementation of the subclasses of CommandLineApplication will be done within this method
     *
     * @throws Exception
     */
    protected abstract void run() throws Exception;

}
