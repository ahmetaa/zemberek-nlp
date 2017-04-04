package zemberek.core.collections;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Sets;
import org.junit.Assert;
import org.junit.Test;
import zemberek.core.io.TestUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class ApproximateStringSetTest {
    @Test
    public void testSmall() {
        Set<String> set = Sets.newHashSet("elma", "armut", "erik", "turp", "brokoli", "muz");
        ApproximateStringSet ass = ApproximateStringSet.generate(set);
        for (String s : set) {
            Assert.assertTrue(ass.contains(s));
        }
        Set<String> uniqueStrings = TestUtil.uniqueStrings(100_000, 7, 1);
        for (String s : uniqueStrings) {
            Assert.assertFalse(ass.contains(s));
        }
    }

    @Test
    public void testLarge() {
        Set<String> uniqueStrings = TestUtil.uniqueStrings(1_000_000, 10, 1);
        Stopwatch sw = Stopwatch.createStarted();
        System.out.println("Generating set.");
        ApproximateStringSet ass = ApproximateStringSet.generate(uniqueStrings);
        System.out.println("1M elements set generation elapsed = " + sw.elapsed(TimeUnit.MILLISECONDS));
        sw.reset().start();
        for (String s : uniqueStrings) {
            ass.contains(s);
        }
        System.out.println("Query elapsed = " + sw.elapsed(TimeUnit.MILLISECONDS));
        for (String s : uniqueStrings) {
            Assert.assertTrue(ass.contains(s));
        }
        //Test false keys.
        Set<String> otherStrings = TestUtil.uniqueStrings(1_000_000, 7, 1);
        otherStrings.removeAll(uniqueStrings);
        for (String s : otherStrings) {
            Assert.assertFalse(ass.contains(s));
        }
    }

    @Test
    public void testSerialization() throws IOException {
        Set<String> uniqueStrings = TestUtil.uniqueStrings(10_000, 30, 1);
        ApproximateStringSet ass = ApproximateStringSet.generate(uniqueStrings);
        Path tmp = Files.createTempFile("foo", "bar");
        tmp.toFile().deleteOnExit();
        ass.serialize(tmp);
        // Test true keys
        ApproximateStringSet deserialized = ApproximateStringSet.deserialize(tmp);
        for (String s : uniqueStrings) {
            Assert.assertTrue(deserialized.contains(s));
        }
        //Test false keys.
        Set<String> otherStrings = TestUtil.uniqueStrings(100_000, 5, 1);
        for (String s : otherStrings) {
            Assert.assertFalse(deserialized.contains(s));
        }
    }
}
