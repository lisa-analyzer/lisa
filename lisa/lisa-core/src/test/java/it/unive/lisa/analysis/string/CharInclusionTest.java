package it.unive.lisa.analysis.string;

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
                "CertainlyContained: {a, b, c} MaybeContained: {d, e, f}");
    }
}
