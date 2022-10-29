package it.unive.lisa.analysis.string;

import it.unive.lisa.analysis.SemanticException;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
}
