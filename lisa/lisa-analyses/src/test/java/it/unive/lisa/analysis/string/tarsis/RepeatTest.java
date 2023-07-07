package it.unive.lisa.analysis.string.tarsis;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import it.unive.lisa.analysis.numeric.Interval;
import it.unive.lisa.util.numeric.MathNumber;
import it.unive.lisa.util.numeric.MathNumberConversionException;

public class RepeatTest {

	@Test
	public void repeatSingleString() throws MathNumberConversionException {
		RegexAutomaton abc = RegexAutomaton.string("abc");
		RegexAutomaton abcabc = RegexAutomaton.string("abcabc");
		RegexAutomaton abc_star = RegexAutomaton.string("abc").kleene();

		Tarsis a = new Tarsis(abc);

		// "abc".repeat(1) = "abc"
		assertTrue(a.repeat(new Interval(1, 1)).getAutomaton().isEqualTo(abc));
		
		// "abc".repeat(2) = "abcabc"
		assertTrue(a.repeat(new Interval(2, 2)).getAutomaton().isEqualTo(abcabc));

		// "abc".repeat(0) = ""
		assertTrue(a.repeat(new Interval(0, 0)).getAutomaton().isEqualTo(RegexAutomaton.emptyStr()));


		// "abc".repeat([1,2]) = {"abc", "abcabc"}
		assertTrue(a.repeat(new Interval(1, 2)).getAutomaton().isEqualTo(abc.union(abcabc)));

		// "abc".repeat([0,+infty]) = (abc)*
		assertTrue(a.repeat(new Interval(MathNumber.ZERO, MathNumber.PLUS_INFINITY)).getAutomaton().isEqualTo(abc_star));

		// "abc".repeat([1,+infty]) = abc(abc)*
		assertTrue(a.repeat(new Interval(MathNumber.ONE, MathNumber.PLUS_INFINITY)).getAutomaton().isEqualTo(abc.concat(abc_star)));
	}
}
