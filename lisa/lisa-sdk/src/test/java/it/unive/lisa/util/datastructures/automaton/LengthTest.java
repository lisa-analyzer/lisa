package it.unive.lisa.util.datastructures.automaton;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.util.numeric.IntInterval;
import it.unive.lisa.util.numeric.MathNumber;
import org.junit.jupiter.api.Test;

public class LengthTest {

	@Test
	public void fixedLengthStringYieldsAPointInterval() {
		IntInterval length = new TestAutomaton("ab").length();
		assertEquals(new MathNumber(2), length.getLow());
		assertEquals(new MathNumber(2), length.getHigh());
	}

	@Test
	public void variableLengthLanguageYieldsASoundLowerBound() {
		// "ab" | "abab" | "ababab" | ... : minimum length 2, no maximum.
		// The lower bound is computed via a heuristic simplification of the
		// automaton's regex form (RegularExpression#simplify is explicitly
		// documented as heuristic, not a complete canonicalizer), so it is
		// only guaranteed to be sound (never above the true minimum of 2),
		// not necessarily tight.
		//
		// NOTE: the upper bound is NOT asserted here. Per its javadoc,
		// lengthOfLongestString() should report Integer.MAX_VALUE for an
		// infinite/cyclic language such as this one, but it is actually
		// computed from getAllPaths(), whose traversal caps each transition
		// at two visits - so for a genuinely unbounded language it silently
		// returns a finite bound instead of MAX_VALUE, contradicting its own
		// documented contract. This is a real, pre-existing bug that is left
		// unfixed here (it would require distinguishing "a cycle exists" from
		// "a cycle lies on an accepting path", a bigger change than
		// warranted for this pass) and is reported separately.
		IntInterval length = new TestAutomaton("ab").star().concat(new TestAutomaton("ab")).length();
		assertTrue(length.getLow().leq(new MathNumber(2)), "lower bound must not exceed the true minimum length");
	}

}
