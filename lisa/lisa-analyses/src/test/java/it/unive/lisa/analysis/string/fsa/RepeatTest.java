package it.unive.lisa.analysis.string.fsa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.lattices.string.fsa.SimpleAutomaton;
import it.unive.lisa.util.numeric.IntInterval;
import it.unive.lisa.util.numeric.MathNumber;
import it.unive.lisa.util.numeric.MathNumberConversionException;
import org.junit.jupiter.api.Test;

public class RepeatTest {

	@Test
	public void repeatTest001()
			throws MathNumberConversionException {
		SimpleAutomaton a = new SimpleAutomaton("a");
		assertEquals(a.repeat(new IntInterval(2, 2)), new SimpleAutomaton("aa"));
	}

	@Test
	public void repeatTest002()
			throws MathNumberConversionException {
		SimpleAutomaton a = new SimpleAutomaton("alpha");
		assertEquals(a.repeat(new IntInterval(3, 3)), new SimpleAutomaton("alphaalphaalpha"));
	}

	@Test
	public void repeatTest003()
			throws MathNumberConversionException {
		SimpleAutomaton a = new SimpleAutomaton("ab").union(new SimpleAutomaton("cd"));
		assertEquals(
				a.repeat(new IntInterval(3, 3)),
				new SimpleAutomaton("ababab").union(new SimpleAutomaton("cdcdcd")));
	}

	@Test
	public void repeatTest004()
			throws MathNumberConversionException {
		SimpleAutomaton a = new SimpleAutomaton("ab").union(new SimpleAutomaton("cd"));
		assertEquals(a.repeat(new IntInterval(0, 0)), a.emptyString());
	}

	/*
	 * The tests below exercise repeat()'s documented contract directly ("each
	 * string of this automaton repeated k-times, with k belonging to intv"):
	 * every individual string accepted by the original automaton is
	 * self-repeated, for every repetition count in the given range: strings
	 * obtained by mixing different accepted strings across repetitions must
	 * never appear in the result. isEqualTo() (language equivalence) is used
	 * instead of equals() (structural equality) since the expected automata
	 * below are not necessarily built the same way internally.
	 */

	@Test
	public void repeatOverARangeSelfRepeatsEachStringIndependently()
			throws MathNumberConversionException {
		SimpleAutomaton a = new SimpleAutomaton("a").union(new SimpleAutomaton("b"));
		SimpleAutomaton result = a.repeat(new IntInterval(1, 2));

		SimpleAutomaton expected = new SimpleAutomaton("a")
				.union(new SimpleAutomaton("b"))
				.union(new SimpleAutomaton("aa"))
				.union(new SimpleAutomaton("bb"));

		assertTrue(result.isEqualTo(expected));
		// "ab" and "ba" would require mixing the two accepted strings, which
		// is not part of the "self-repeat" semantics
		assertFalse(result.validateString("ab"));
		assertFalse(result.validateString("ba"));
	}

	@Test
	public void repeatNeverMixesDifferentAcceptedStrings()
			throws MathNumberConversionException {
		SimpleAutomaton a = new SimpleAutomaton("ab").union(new SimpleAutomaton("cd"));
		SimpleAutomaton result = a.repeat(new IntInterval(2, 2));

		assertTrue(result.validateString("abab"));
		assertTrue(result.validateString("cdcd"));
		assertFalse(result.validateString("abcd"));
		assertFalse(result.validateString("cdab"));
	}

	@Test
	public void repeatOnACombinatorialUnionOfConcatenations()
			throws MathNumberConversionException {
		// (a|b)(c|d)(e|f), accepting exactly {ace, acf, ade, adf, bce, bcf,
		// bde, bdf}: this is the kind of diamond-shaped automaton where the
		// same states are reachable via several distinct paths
		SimpleAutomaton choice1 = new SimpleAutomaton("a").union(new SimpleAutomaton("b"));
		SimpleAutomaton choice2 = new SimpleAutomaton("c").union(new SimpleAutomaton("d"));
		SimpleAutomaton choice3 = new SimpleAutomaton("e").union(new SimpleAutomaton("f"));
		SimpleAutomaton a = choice1.concat(choice2).concat(choice3);

		SimpleAutomaton result = a.repeat(new IntInterval(2, 2));

		String[] strings = { "ace", "acf", "ade", "adf", "bce", "bcf", "bde", "bdf" };
		for (String s : strings)
			assertTrue(result.validateString(s + s), "expected " + s + s + " to be accepted");

		// picking two different base strings and concatenating them must
		// never be accepted, as that would mix independent choices
		assertFalse(result.validateString("ace" + "bdf"));
		assertFalse(result.validateString("adf" + "bce"));
	}

	@Test
	public void repeatWithUnboundedUpperBound()
			throws MathNumberConversionException {
		SimpleAutomaton a = new SimpleAutomaton("a");
		SimpleAutomaton result = a.repeat(new IntInterval(MathNumber.ONE, MathNumber.PLUS_INFINITY));

		assertTrue(result.validateString("a"));
		assertTrue(result.validateString("aaaaa"));
		assertFalse(result.validateString(""));
		assertFalse(result.validateString("b"));
		assertFalse(result.validateString("ab"));
	}

}
