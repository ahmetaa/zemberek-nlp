package zemberek.core.logging;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import zemberek.core.logging.Log.LogLevel;

public class LoggerTest {

  @After
  public void tearDown() {
    Log.reset();
  }

  @Test
  public void testLevel() {
    Log.reset();
    Assert.assertFalse(Log.isDebug());
    Log.setDebug();
    Assert.assertTrue(Log.isDebug());
    Log.setTrace();
    Assert.assertTrue(Log.isDebug());
    Assert.assertTrue(Log.isTrace());
    Assert.assertTrue(Log.isInfo());
  }

  @Test
  public void testResetDoesNotDuplicateHandlers() {
    Logger julLogger = Logger.getLogger("zemberek-logger");
    Log.reset();
    Log.reset();
    Log.reset();
    Assert.assertEquals(1, julLogger.getHandlers().length);
  }

  @Test
  public void testShutdownRemovesHandlersAndRestoresOnReset() throws IOException {
    Logger julLogger = Logger.getLogger("zemberek-logger");
    Path logFile = Files.createTempFile("zemberek-log-shutdown", ".log");
    try {
      Log.addFileHandler(logFile);
      Log.info("Before shutdown");
      Assert.assertEquals(2, julLogger.getHandlers().length);

      Log.shutdown();
      Assert.assertEquals(0, julLogger.getHandlers().length);
      Assert.assertTrue(Log.getCurrentLogFiles().isEmpty());
      Assert.assertFalse(Log.isInfo());

      Log.reset();
      Assert.assertEquals(1, julLogger.getHandlers().length);
      Assert.assertTrue(Log.isInfo());
    } finally {
      Files.deleteIfExists(logFile);
    }
  }

  @Test
  public void testClassLevelOverrideDoesNotCorruptGlobalLevel() throws IOException {
    Path logFile = Files.createTempFile("zemberek-log-classlevel", ".log");
    try {
      Log.reset();
      Log.addFileHandler(logFile);
      Assert.assertTrue(Log.isInfo());
      Assert.assertFalse(Log.isDebug());

      // Suppress INFO for LoggerTest; filtered call must not mutate global INFO level.
      Log.addClassLevel(LoggerTest.class, LogLevel.WARNING);
      Log.info("Suppressed info message");
      Assert.assertTrue("Global INFO level must remain intact", Log.isInfo());

      // Enable DEBUG only for LoggerTest while global level stays INFO.
      Log.addClassLevel(LoggerTest.class, LogLevel.DEBUG);
      Log.debug("Per-class debug message %d", 42);
      Assert.assertFalse("Global DEBUG must remain false after per-class debug log", Log.isDebug());
      Assert.assertTrue(Log.isInfo());

      Log.flushFileHandlers();
      String content = Files.readString(logFile, StandardCharsets.UTF_8);
      Assert.assertFalse(content.contains("Suppressed info message"));
      Assert.assertTrue(content.contains("Per-class debug message 42"));
    } finally {
      Log.removeFileHandler(logFile);
      Files.deleteIfExists(logFile);
    }
  }

  @Test
  public void testDisabledLogSkipsObjectToStringAndHandlesNull() {
    Log.reset();
    AtomicBoolean toStringCalled = new AtomicBoolean(false);
    Object expensiveObject = new Object() {
      @Override
      public String toString() {
        toStringCalled.set(true);
        return "expensive";
      }
    };
    Log.debug(expensiveObject);
    Log.trace(expensiveObject);
    Assert.assertFalse(toStringCalled.get());

    // Null object should not throw NullPointerException even when enabled.
    Log.info((Object) null);
  }
}
