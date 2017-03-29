package zemberek.tokenization;

import org.junit.Assert;
import org.junit.Test;

public class SpanTest {

    @Test
    public void shouldNotThrowException() {
        try {
            new Span(0, 0);
            new Span(1, 1);
            new Span(1, 5);
            new Span(Integer.MAX_VALUE, Integer.MAX_VALUE);
        } catch (Exception e) {
            Assert.fail();
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void badInitialization1() {
        new Span(-1, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void badInitialization2() {
        new Span(0, -1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void badInitialization3() {
        new Span(1, 0);
    }

    @Test
    public void substringTest() {
        Assert.assertEquals("", new Span(0, 0).getSubstring("hello"));
        Assert.assertEquals("h", new Span(0, 1).getSubstring("hello"));
        Assert.assertEquals("ello", new Span(1, 5).getSubstring("hello"));
    }

    @Test
    public void lengthTest() {
        Assert.assertEquals(0, new Span(0, 0).length());
        Assert.assertEquals(1, new Span(0, 1).length());
        Assert.assertEquals(4, new Span(1, 5).length());
    }


}
