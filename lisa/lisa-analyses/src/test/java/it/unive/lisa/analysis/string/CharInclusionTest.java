package it.unive.lisa.analysis.string;

import static org.junit.Assert.*;

import it.unive.lisa.analysis.SemanticException;
import java.util.HashSet;
import java.util.Set;
import org.junit.Test;

public class CharInclusionTest {

	@Test
	public void representationTest() {
		Set<Character> certainlyContained = new HashSet<>();
		Set<Character> maybeContained = new HashSet<>();

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
		Set<Character> certainlyContained = new HashSet<>();
		Set<Character> maybeContained = new HashSet<>();

		Set<Character> otherCertainlyContained = new HashSet<>();
		Set<Character> otherMaybeContained = new HashSet<>();

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

		HashSet<Character> certainlyContainedResult = new HashSet<>();
		HashSet<Character> maybeContainedResult = new HashSet<>();

		certainlyContainedResult.add('a');

		maybeContainedResult.add('d');
		maybeContainedResult.add('e');
		maybeContainedResult.add('f');
		maybeContainedResult.add('z');

		assertEquals(
				new CharInclusion(certainlyContained, maybeContained)
						.lubAux(new CharInclusion(otherCertainlyContained, otherMaybeContained)),
				new CharInclusion(certainlyContainedResult, maybeContainedResult));
	}

	@Test
	public void testLessOrEqualAux() throws SemanticException {
		Set<Character> certainlyContained = new HashSet<>();
		Set<Character> maybeContained = new HashSet<>();

		Set<Character> otherCertainlyContained = new HashSet<>();
		Set<Character> otherMaybeContained = new HashSet<>();

		certainlyContained.add('a');
		certainlyContained.add('b');

		otherCertainlyContained.add('a');
		otherCertainlyContained.add('b');
		otherCertainlyContained.add('c');
		otherCertainlyContained.add('d');

		maybeContained.add('f');
		maybeContained.add('g');
		maybeContained.add('h');

		otherMaybeContained.add('h');

		assertTrue(new CharInclusion(certainlyContained, maybeContained)
				.lessOrEqualAux(new CharInclusion(otherCertainlyContained, otherMaybeContained)));
	}

	@Test
	public void testLessOrEqualAux1() throws SemanticException {
		Set<Character> certainlyContained = new HashSet<>();
		Set<Character> maybeContained = new HashSet<>();

		Set<Character> otherCertainlyContained = new HashSet<>();
		Set<Character> otherMaybeContained = new HashSet<>();

		certainlyContained.add('a');
		certainlyContained.add('b');
		certainlyContained.add('c');
		certainlyContained.add('d');
		certainlyContained.add('e');

		otherCertainlyContained.add('a');
		otherCertainlyContained.add('b');
		otherCertainlyContained.add('c');
		otherCertainlyContained.add('d');

		maybeContained.add('f');
		maybeContained.add('g');
		maybeContained.add('h');

		otherMaybeContained.add('h');

		assertFalse(new CharInclusion(certainlyContained, maybeContained)
				.lessOrEqualAux(new CharInclusion(otherCertainlyContained, otherMaybeContained)));
	}

	@Test
	public void testLessOrEqualAux2() throws SemanticException {
		Set<Character> certainlyContained = new HashSet<>();
		Set<Character> maybeContained = new HashSet<>();

		Set<Character> otherCertainlyContained = new HashSet<>();
		Set<Character> otherMaybeContained = new HashSet<>();

		certainlyContained.add('a');
		certainlyContained.add('b');
		certainlyContained.add('c');

		otherCertainlyContained.add('a');
		otherCertainlyContained.add('b');
		otherCertainlyContained.add('c');
		otherCertainlyContained.add('d');

		maybeContained.add('d');
		maybeContained.add('e');
		maybeContained.add('f');

		otherMaybeContained.add('h');

		assertFalse(new CharInclusion(certainlyContained, maybeContained)
				.lessOrEqualAux(new CharInclusion(otherCertainlyContained, otherMaybeContained)));
	}

	@Test
	public void testLessOrEqualAux3() throws SemanticException {
		Set<Character> certainlyContained = new HashSet<>();
		Set<Character> maybeContained = new HashSet<>();

		Set<Character> otherCertainlyContained = new HashSet<>();
		Set<Character> otherMaybeContained = new HashSet<>();

		certainlyContained.add('a');
		certainlyContained.add('b');
		certainlyContained.add('c');

		otherCertainlyContained.add('a');
		otherCertainlyContained.add('b');
		otherCertainlyContained.add('c');
		otherCertainlyContained.add('d');

		maybeContained.add('d');
		maybeContained.add('e');
		maybeContained.add('f');

		otherMaybeContained.add('d');
		otherMaybeContained.add('e');
		otherMaybeContained.add('f');
		otherMaybeContained.add('g');

		assertFalse(new CharInclusion(certainlyContained, maybeContained)
				.lessOrEqualAux(new CharInclusion(otherCertainlyContained, otherMaybeContained)));
	}

	@Test
	public void testLessOrEqualAux4() throws SemanticException {
		Set<Character> certainlyContained = new HashSet<>();
		Set<Character> maybeContained = new HashSet<>();

		Set<Character> otherCertainlyContained = new HashSet<>();
		Set<Character> otherMaybeContained = new HashSet<>();

		certainlyContained.add('a');
		certainlyContained.add('b');
		certainlyContained.add('c');
		certainlyContained.add('d');

		otherCertainlyContained.add('a');
		otherCertainlyContained.add('b');
		otherCertainlyContained.add('c');
		otherCertainlyContained.add('d');

		maybeContained.add('d');
		maybeContained.add('e');
		maybeContained.add('f');

		otherMaybeContained.add('d');
		otherMaybeContained.add('e');
		otherMaybeContained.add('f');

		assertTrue(new CharInclusion(certainlyContained, maybeContained)
				.lessOrEqualAux(new CharInclusion(otherCertainlyContained, otherMaybeContained)));
	}
}
