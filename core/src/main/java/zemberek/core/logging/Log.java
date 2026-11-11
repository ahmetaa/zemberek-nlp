package zemberek.core.logging;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.IllegalFormatException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import zemberek.core.io.Strings;

/**
 * A convenient Log class.
 */
public final class Log {


  static final CUSTOM_FORMAT formatter = new CUSTOM_FORMAT();
  private static final Logger logger = Logger.getLogger("zemberek-logger");
  private static final Map<File, FileHandler> fileHandlers = new ConcurrentHashMap<>();
  private static final Map<String, LogLevel> classLevelMap = new ConcurrentHashMap<>();
  private static final String LOG_CLASS_NAME = Log.class.getName();
  private static Thread shutdownHook;
  public static OutputStream fileStream;
  static volatile Level currentLevel = Level.INFO;
  static SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss.SSS", Locale.ENGLISH);
  static Map<Level, String> levelShortStringMap = new HashMap<>();

  static {
    levelShortStringMap.put(Level.FINEST, "-");
    levelShortStringMap.put(Level.FINER, "T");
    levelShortStringMap.put(Level.FINE, "D");
    levelShortStringMap.put(Level.CONFIG, "-");
    levelShortStringMap.put(Level.OFF, "-");
    levelShortStringMap.put(Level.ALL, "-");
    levelShortStringMap.put(Level.WARNING, "W");
    levelShortStringMap.put(Level.INFO, "I");
    levelShortStringMap.put(Level.SEVERE, "E");
    reset();
  }

  public static synchronized void reset() {
    closeAllFileHandlers();
    unregisterShutdownHook();
    for (Handler handler : logger.getHandlers()) {
      logger.removeHandler(handler);
    }
    classLevelMap.clear();
    currentLevel = Level.INFO;
    logger.setUseParentHandlers(false);
    logger.setLevel(currentLevel);
    ConsoleHandler ch = new ConsoleHandler();
    ch.setFormatter(formatter);
    ch.setLevel(currentLevel);
    logger.addHandler(ch);
  }

  /**
   * Closes all file handlers, removes all logger handlers (including console), clears per-class
   * overrides, and unregisters any JVM shutdown hook registered by this class.
   */
  public static synchronized void shutdown() {
    closeAllFileHandlers();
    unregisterShutdownHook();
    for (Handler handler : logger.getHandlers()) {
      logger.removeHandler(handler);
    }
    classLevelMap.clear();
    currentLevel = Level.OFF;
    logger.setLevel(Level.OFF);
  }

  private static void ensureShutdownHookRegistered() {
    if (shutdownHook == null) {
      shutdownHook = new Thread(Log::closeAllFileHandlers, "zemberek-log-closer");
      Runtime.getRuntime().addShutdownHook(shutdownHook);
    }
  }

  private static void unregisterShutdownHook() {
    if (shutdownHook != null) {
      try {
        Runtime.getRuntime().removeShutdownHook(shutdownHook);
      } catch (IllegalStateException ignored) {
        // JVM shutdown already in progress.
      }
      shutdownHook = null;
    }
  }

  private static void closeAllFileHandlers() {
    for (FileHandler handler : fileHandlers.values()) {
      logger.removeHandler(handler);
      handler.close();
    }
    fileHandlers.clear();
  }

  private Log() {
  }

  public static void addClassLevel(Class clazz, LogLevel level) {
    classLevelMap.put(clazz.getName(), level);
  }

  public static void removeClassLevel(Class clazz, LogLevel level) {
    classLevelMap.remove(clazz.getName(), level);
  }

  private static void setLevel(Level level) {
    logger.setLevel(level);
    for (Handler handler : logger.getHandlers()) {
      handler.setLevel(level);
    }
    currentLevel = level;
  }

  /**
   * @return true if Log is in debug level.
   */
  public static boolean isDebug() {
    return checkLevel(Level.FINE);
  }

  /**
   * @return true if Log is in trace level.
   */
  public static boolean isTrace() {
    return checkLevel(Level.FINER);
  }

  /**
   * @return true if Log is in info level.
   */
  public static boolean isInfo() {
    return checkLevel(Level.INFO);
  }

  /**
   * @return true if Log is in warn level.
   */
  public static boolean isWarn() {
    return checkLevel(Level.WARNING);
  }

  /**
   * @return true if Log is in trace level.
   */
  public static boolean isError() {
    return checkLevel(Level.SEVERE);
  }

  private static boolean checkLevel(Level level) {
    return currentLevel.intValue() <= level.intValue();
  }

  /**
   * Trace level logging. This can be used for cases finer grained than debug level.
   *
   * @param message message to log. it can be in String.format(message) format.
   * @param params if message is formatted according to String.format() params should contain
   * necessary arguments. If last parameter is a throwable, it is handled specially.
   */
  public static void trace(String message, Object... params) {
    log(Level.FINER, message, params);
  }

  public static void trace(boolean condition, String message, Object... params) {
    if (condition) {
      log(Level.FINER, message, params);
    }
  }

  public static void trace(Object object) {
    if (!classLevelMap.isEmpty() || checkLevel(Level.FINER)) {
      log(Level.FINER, String.valueOf(object));
    }
  }

  public static void debug(String message, Object... params) {
    log(Level.FINE, message, params);
  }

  public static void debug(boolean condition, String message, Object... params) {
    if (condition) {
      log(Level.FINE, message, params);
    }
  }

  public static void debug(Object object) {
    if (!classLevelMap.isEmpty() || checkLevel(Level.FINE)) {
      log(Level.FINE, String.valueOf(object));
    }
  }

  public static void info() {
    log(Level.INFO, "");
  }

  public static void info(String message, Object... params) {
    log(Level.INFO, message, params);
  }

  public static void info(boolean condition, String message, Object... params) {
    if (condition) {
      log(Level.INFO, message, params);
    }
  }

  public static void info(Object object) {
    if (!classLevelMap.isEmpty() || checkLevel(Level.INFO)) {
      log(Level.INFO, String.valueOf(object));
    }
  }

  public static void warn(String message, Object... params) {
    log(Level.WARNING, message, params);
  }

  public static void exception(Throwable t) {
    logger.log(Level.SEVERE, "Exception occurred.", t);
  }

  public static void warn(boolean condition, String message, Object... params) {
    if (condition) {
      log(Level.WARNING, message, params);
    }
  }

  public static void warn(Object object) {
    if (!classLevelMap.isEmpty() || checkLevel(Level.WARNING)) {
      log(Level.WARNING, String.valueOf(object));
    }
  }

  public static void error(String message, Object... params) {
    log(Level.SEVERE, message, params);
  }

  public static void error(boolean condition, String message, Object... params) {
    if (condition) {
      log(Level.SEVERE, message, params);
    }
  }

  public static void error(Object object) {
    if (!classLevelMap.isEmpty() || checkLevel(Level.SEVERE)) {
      log(Level.SEVERE, String.valueOf(object));
    }
  }

  public static void setError() {
    setLevel(Level.SEVERE);
  }

  public static void setWarn() {
    setLevel(Level.WARNING);
  }

  public static void setInfo() {
    setLevel(Level.INFO);
  }

  public static void setDebug() {
    setLevel(Level.FINE);
  }

  public static void setTrace() {
    setLevel(Level.FINER);
  }

  public static synchronized void addFileHandler(Path path) throws IOException {
    File file = path.toFile();
    if (fileHandlers.containsKey(file)) {
      Log.info("Log File %s already exist. Appending.", file);
      return;
    }

    final FileHandler handler = new FileHandler(file.getAbsolutePath(), true);
    handler.setFormatter(formatter);
    handler.setLevel(currentLevel);
    logger.addHandler(handler);
    fileHandlers.put(file, handler);
    ensureShutdownHookRegistered();
  }

  public static void flushFileHandlers() {
    fileHandlers.values().stream().filter(Objects::nonNull).forEach(Handler::flush);
  }

  public static synchronized void removeFileHandler(File file) {
    FileHandler handler = fileHandlers.remove(file);
    if (handler != null) {
      logger.removeHandler(handler);
      handler.close();
      if (fileHandlers.isEmpty()) {
        unregisterShutdownHook();
      }
    }
  }

  public static void removeFileHandler(Path path) {
    removeFileHandler(path.toFile());
  }

  public static List<Path> getCurrentLogFiles() {
    return fileHandlers.keySet().stream().map(File::toPath).collect(Collectors.toList());
  }

  public static void log(Level level, String message, Object... params) {
    if (classLevelMap.isEmpty() && level.intValue() < currentLevel.intValue()) {
      return;
    }

    StackTraceElement caller = findCaller();
    String className = caller != null ? caller.getClassName() : LOG_CLASS_NAME;
    String methodName = caller != null ? caller.getMethodName() : "log";

    LogLevel classLevel = classLevelMap.get(className);
    Level effectiveLevel = classLevel != null ? classLevel.level : currentLevel;
    if (level.intValue() < effectiveLevel.intValue()) {
      return;
    }

    Throwable thrown = null;
    if (params != null && params.length > 0) {
      Object last = params[params.length - 1];
      if (last instanceof Throwable) {
        if (params.length > 1) {
          Object[] subParams = new Object[params.length - 1];
          System.arraycopy(params, 0, subParams, 0, subParams.length);
          params = subParams;
        } else {
          params = new Object[0];
        }
        thrown = (Throwable) last;
      }
    }
    LogRecord record = new LogRecord(level, message);
    record.setLoggerName(logger.getName());
    record.setSourceClassName(className);
    record.setSourceMethodName(methodName);
    record.setThrown(thrown);
    record.setParameters(params);

    synchronized (Log.class) {
      Level savedLevel = currentLevel;
      boolean adjustLevel = effectiveLevel.intValue() < savedLevel.intValue();
      if (adjustLevel) {
        setLevel(effectiveLevel);
      }
      try {
        logger.log(record);
      } finally {
        if (adjustLevel) {
          setLevel(savedLevel);
        }
      }
    }
  }

  private static StackTraceElement findCaller() {
    StackTraceElement[] frames = Thread.currentThread().getStackTrace();
    boolean seenLogClass = false;
    for (int i = 1; i < frames.length; i++) {
      boolean isLogClass = LOG_CLASS_NAME.equals(frames[i].getClassName());
      if (isLogClass) {
        seenLogClass = true;
      } else if (seenLogClass) {
        return frames[i];
      }
    }
    return frames.length > 2 ? frames[2] : null;
  }

  private static String shortenName(String name, int length) {
    if (name.length() < length) {
      return Strings.rightPad(name, length);
    } else {
      return name.substring(0, length - 1) + "~";
    }
  }

  private static String padIfNecessary(String name, int length) {
    if (name.length() < length) {
      return Strings.rightPad(name, length);
    }
    return name;
  }

  public enum LogLevel {
    TRACE(Level.FINER),
    DEBUG(Level.FINE),
    INFO(Level.INFO),
    WARNING(Level.WARNING),
    ERROR(Level.SEVERE);

    Level level;

    LogLevel(Level level) {
      this.level = level;
    }
  }

  private static class CUSTOM_FORMAT extends Formatter {

    @Override
    public String format(LogRecord record) {
      synchronized (this) {
        StringBuilder sb = new StringBuilder(levelShortStringMap.get(record.getLevel()));
        sb.append("|").append(format.format(new Date())).append("|");
        Object parameters[] = record.getParameters();

        boolean multiLine = false;
        if (record.getMessage().indexOf('\n') > 0) {
          multiLine = true;
        }

        // generate Exception String if available
        String throwStr = "";
        if (record.getThrown() != null) {
          Throwable t = record.getThrown();
          StringWriter sw = new StringWriter();
          try (PrintWriter pw = new PrintWriter(sw)) {
            t.printStackTrace(pw);
          }
          throwStr = sw.toString();
        }

        if (!multiLine) {
          if (parameters == null || parameters.length == 0) {
            sb.append(padIfNecessary(record.getMessage(), 100));
          } else {
            try {
              sb.append(
                  padIfNecessary(String.format(Locale.ENGLISH, record.getMessage(), parameters),
                      100));
            } catch (IllegalFormatException e) {
              sb.append("Log Format Error: ")
                  .append(record.getMessage())
                  .append(" With Parameters: ")
                  .append(Joiner.on(",").join(parameters));
            }
          }
          sb.append("| ")
              .append(Strings.subStringAfterLast(record.getSourceClassName(), "."))
              .append("#");
          sb.append(record.getSourceMethodName());
          sb.append("\n");
        } else {
          sb.append(" \u2193 |")
              .append(Strings.subStringAfterLast(record.getSourceClassName(), "."))
              .append("#");
          sb.append(record.getSourceMethodName());
          sb.append("\n");
          for (String s : Splitter.on("\n").split(record.getMessage())) {
            sb.append("    ").append(s).append("\n");
          }
        }
        if (throwStr.length() > 0) {
          sb.append("\n");
          for (String s : Splitter.on("\n").split(throwStr)) {
            sb.append(s).append("\n");
          }
        }
        return sb.toString();
      }
    }
  }

}
