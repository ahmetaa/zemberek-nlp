package zemberek.core.logging;

import org.junit.Assert;
import org.junit.Test;

public class LoggerTest {

    @Test
    public void testLevel() {
        Assert.assertFalse(Log.isDebug());
        Log.setDebug();
        Assert.assertTrue(Log.isDebug());
        Log.setTrace();
        Assert.assertTrue(Log.isDebug());
        Assert.assertTrue(Log.isTrace());
        Assert.assertTrue(Log.isInfo());
    }
}
