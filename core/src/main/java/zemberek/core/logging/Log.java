package zemberek.core.logging;

import com.google.common.base.Joiner;
import zemberek.core.io.Strings;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.IllegalFormatException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.*;

public final class Log {

    public static final Thread.UncaughtExceptionHandler EXCEPTION_HANDLER = new ExceptionLoggerHandler();

    private static final ConcurrentMap<String, Logger> loggers = new ConcurrentHashMap<>();

    static Level currentLevel = Level.INFO;

    static {
        Logger global = Logger.getLogger("");
        Handler[] handlers = global.getHandlers();
        for (Handler handler : handlers) {
            global.removeHandler(handler);
        }
        ConsoleHandler ch = new ConsoleHandler();
        ch.setFormatter(new CUSTOM_FORMAT());
        global.addHandler(ch);
        Thread.setDefaultUncaughtExceptionHandler(EXCEPTION_HANDLER);
    }

    private Log() {
    }

    private static class ExceptionLoggerHandler implements Thread.UncaughtExceptionHandler {
        @Override
        public void uncaughtException(Thread t, Throwable e) {
            Log.error("Exception occured in thread :" + t.getName());
            Log.error(e.toString());
            for (StackTraceElement stackTraceElement : e.getStackTrace()) {
                Log.error("  " + stackTraceElement.toString());
            }
            e.printStackTrace();
        }
    }

    public static void setLevel(Level level) {
        synchronized (loggers) {
            for (Logger logger : loggers.values()) {
                logger.setLevel(level);
            }
            Logger.getLogger("").setLevel(level);
        }
        currentLevel = level;
    }

    public static boolean isDebug() {
        return currentLevel == Level.FINE;
    }

    public static boolean isInfo() {
        return currentLevel == Level.INFO;
    }

    public static boolean isWarning() {
        return currentLevel == Level.WARNING;
    }

    public static void debug(String message, Object... params) {
        log(Level.FINE, message, params);
    }

    public static void info(String message, Object... params) {
        log(Level.INFO, message, params);
    }

    public static void warn(String message, Object... params) {
        log(Level.WARNING, message, params);
    }

    public static void error(String message, Object... params) {
        log(Level.SEVERE, message, params);
    }

    public static void setInfo() {
       setLevel(Level.INFO);
    }

    public static void setDebug() {
        setLevel(Level.FINE);
    }


    static final CUSTOM_FORMAT formatter = new CUSTOM_FORMAT();

    public static void addFileHandler(Path path) throws IOException {
        final StreamHandler handler = new StreamHandler(Files.newOutputStream(path), formatter);
        Logger.getLogger("").addHandler(handler);
    }

    public static void log(Level level, String message, Object... params) {
        final int stackPositionOfCaller = 2;
        StackTraceElement caller = new Throwable().getStackTrace()[stackPositionOfCaller];
        String className = caller.getClassName();
        Logger logger;

        logger = loggers.get(className);
        if (logger == null) {
            logger = Logger.getLogger(className);
            loggers.putIfAbsent(className, logger);
            logger.setLevel(currentLevel);
        }

        if (logger.isLoggable(level)) {
            String formattedMessage;
            Throwable thrown = null;
            if (params.length == 0) {
                formattedMessage = message;
            } else {
                Object last = params[params.length - 1];
                if (last instanceof Throwable) {
                    Object[] subParams = new Object[params.length - 1];
                    System.arraycopy(params, 0, subParams, 0, subParams.length);
                    formattedMessage = String.format(message, subParams);
                    thrown = (Throwable) last;
                } else {
                    formattedMessage = String.format(message, params);
                }
            }
            LogRecord record = new LogRecord(level, formattedMessage);
            record.setLoggerName(logger.getName());
            record.setSourceClassName(className);
            record.setSourceMethodName(caller.getMethodName());
            record.setThrown(thrown);
            record.setParameters(params);
            logger.log(record);
        }
    }

    static SimpleDateFormat format = new SimpleDateFormat("HH:MM:ss.SSS");

    static Map<Level, String> levelShortStringMap = new HashMap<>();

    static {
        levelShortStringMap.put(Level.FINEST, "D");
        levelShortStringMap.put(Level.FINER, "D");
        levelShortStringMap.put(Level.FINE, "D");
        levelShortStringMap.put(Level.CONFIG, "-");
        levelShortStringMap.put(Level.OFF, "-");
        levelShortStringMap.put(Level.ALL, "-");
        levelShortStringMap.put(Level.WARNING, "W");
        levelShortStringMap.put(Level.INFO, "I");
        levelShortStringMap.put(Level.SEVERE, "E");
    }

    private static String shortenName(String name, int length) {
        if (name.length() < length) {
            return Strings.rightPad(name, length);
        } else {
            return name.substring(0, length - 1) + "~";
        }
    }

    private static class CUSTOM_FORMAT extends Formatter {

        @Override
        public String format(LogRecord record) {
            synchronized (this) {
                StringBuilder sb = new StringBuilder(levelShortStringMap.get(record.getLevel()));
                sb.append("|").append(format.format(new Date())).append("|");
                sb.append(shortenName(Strings.subStringAfterLast(record.getSourceClassName(), "."), 20)).append("|");
                sb.append(shortenName(Strings.subStringAfterLast(record.getSourceMethodName(), "."), 20)).append("| ");
                Object parameters[] = record.getParameters();
                if (parameters == null || parameters.length == 0) {
                    sb.append(record.getMessage());
                } else {
                    try {
                        sb.append(String.format(record.getMessage(), parameters));
                    } catch (IllegalFormatException e) {
                        sb.append("Log Format Error: ")
                                .append(record.getMessage())
                                .append(" With Parameters: ")
                                .append(Joiner.on(",").join(parameters));
                    }
                }
                sb.append("\n");
                return sb.toString();
            }
        }
    }
}