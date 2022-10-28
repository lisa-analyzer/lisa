package it.unive.lisa.analysis.string;

import it.unive.lisa.analysis.SemanticException;
import org.junit.Test;

import static org.junit.Assert.*;

public class PrefixTest {

    @Test
    public void testPrefixConstructor() {
        new Prefix();
    }

    @Test
    public void testPrefixConstructor1() {
        new Prefix("Hello World!");
    }

    @Test
    public void testPrefixLubAux() throws SemanticException {
        Prefix result = new Prefix("abc").lubAux(new Prefix("abcdef"));

        assertEquals(result.getPrefix(), "abc");
    }

    @Test
    public void testPrefixLubAux1() throws SemanticException {
        Prefix result = new Prefix("Hello World!").lubAux(new Prefix("Hello, World!"));

        assertEquals(result.getPrefix(), "Hello");
    }

    @Test
    public void testPrefixLubAux2() throws SemanticException {
        Prefix result = new Prefix("abc").lubAux(new Prefix("def"));

        assertTrue(result.isTop());
    }

    @Test
    public void testLessOrEqualsAux() throws SemanticException {
        Prefix result = new Prefix("abc");

        assertFalse(result.lessOrEqualAux(new Prefix("abcde")));
    }

    @Test
    public void testLessOrEqualsAux1() throws SemanticException {
        Prefix result = new Prefix("abcde");

        assertFalse(result.lessOrEqualAux(new Prefix("abd")));
    }

    @Test
    public void testLessOrEqualsAux2() throws SemanticException {
        Prefix result = new Prefix("abde");

        assertTrue(result.lessOrEqualAux(new Prefix("abd")));
    }
}
