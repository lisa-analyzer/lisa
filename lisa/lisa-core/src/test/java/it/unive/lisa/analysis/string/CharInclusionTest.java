package it.unive.lisa.analysis.string;

import it.unive.lisa.analysis.SemanticException;
import org.junit.Test;

import java.util.HashSet;

import static org.junit.Assert.assertEquals;


public class CharInclusionTest {

    @Test
    public void representationTest(){
        HashSet<Character> certainlyContained = new HashSet<>();
        HashSet<Character> maybeContained = new HashSet<>();

        certainlyContained.add('a');
        certainlyContained.add('b');
        certainlyContained.add('c');

        maybeContained.add('d');
        maybeContained.add('e');
        maybeContained.add('f');

        assertEquals(new CharInclusion(certainlyContained, maybeContained).representation().toString(),
                "CertainlyContained: {a, b, c}, MaybeContained: {d, e, f}");
    }

    @Test
    public void lubAuxTest() throws SemanticException {
        HashSet<Character> certainlyContained = new HashSet<>();
        HashSet<Character> maybeContained = new HashSet<>();

        HashSet<Character> otherCertainlyContained = new HashSet<>();
        HashSet<Character> otherMaybeContained = new HashSet<>();

        certainlyContained.add('a');
        certainlyContained.add('b');
        certainlyContained.add('c');
        maybeContained.add('d');
        maybeContained.add('e');
        maybeContained.add('f');

        otherCertainlyContained.add('a');
        otherCertainlyContained.add('f');
        otherCertainlyContained.add('g');
        otherMaybeContained.add('d');
        otherMaybeContained.add('e');
        otherMaybeContained.add('z');

        assertEquals(new CharInclusion(certainlyContained,maybeContained).
                        lubAux(new CharInclusion(otherCertainlyContained, otherMaybeContained)).
                                representation().toString(), "CertainlyContained: {a}, MaybeContained: {d, e, f, z}");
    }
}
