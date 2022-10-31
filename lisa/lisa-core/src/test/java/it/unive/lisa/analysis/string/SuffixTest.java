package it.unive.lisa.analysis.string;

import it.unive.lisa.analysis.SemanticException;
import org.junit.Test;

import static org.junit.Assert.*;

public class SuffixTest {

    @Test
    public void testSuffixLubAux() throws SemanticException {
        Suffix result = new Suffix("Hello World!").lubAux(new Suffix("World!"));

        assertEquals(result.getSuffix(), "World!");
    }

    @Test
    public void testSuffixLubAux1() throws SemanticException {
        Suffix result = new Suffix("abcde").lubAux(new Suffix("cde"));

        assertEquals(result.getSuffix(), "cde");
    }

    @Test
    public void testSuffixLubAux2() throws SemanticException {
        Suffix result = new Suffix("Hello").lubAux(new Suffix("World"));

        assertTrue(result.isTop());
    }

    @Test
    public void testSuffixLessOrEqual() throws SemanticException {
        Suffix suffix = new Suffix("fghabc");

        assertTrue(suffix.lessOrEqualAux(new Suffix("abc")));
    }

    @Test
    public void testSuffixLessOrEqual1() throws SemanticException {
        Suffix suffix = new Suffix("fghabc");

        assertFalse(suffix.lessOrEqualAux(new Suffix("abd")));
    }
}
