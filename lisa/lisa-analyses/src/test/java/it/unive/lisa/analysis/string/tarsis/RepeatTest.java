package it.unive.lisa.analysis.string.tarsis;

import static org.junit.Assert.assertTrue;

import it.unive.lisa.analysis.numeric.Interval;
import it.unive.lisa.util.datastructures.regex.Atom;
import it.unive.lisa.util.datastructures.regex.Or;
import it.unive.lisa.util.numeric.MathNumber;
import it.unive.lisa.util.numeric.MathNumberConversionException;
import org.junit.Test;

public class RepeatTest {

	@Test
	public void repeatSingleString() throws MathNumberConversionException {
		RegexAutomaton abc = RegexAutomaton.string("abc");
		RegexAutomaton abcabc = RegexAutomaton.string("abcabc");
		RegexAutomaton abc_star = RegexAutomaton.string("abc").star();

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
		assertTrue(
				a.repeat(new Interval(MathNumber.ZERO, MathNumber.PLUS_INFINITY)).getAutomaton().isEqualTo(abc_star));

		// "abc".repeat([1,+infty]) = abc(abc)*
		assertTrue(a.repeat(new Interval(MathNumber.ONE, MathNumber.PLUS_INFINITY)).getAutomaton()
				.isEqualTo(abc.concat(abc_star)));
	}

	@Test
	public void repeatTwoStrings() throws MathNumberConversionException {

		RegexAutomaton ab_or_cd = new Or(new Atom("ab"), new Atom("cd")).toAutomaton(RegexAutomaton.emptyLang());
		RegexAutomaton abab_or_cdcd = new Or(new Atom("abab"), new Atom("cdcd"))
				.toAutomaton(RegexAutomaton.emptyLang());

		Tarsis a = new Tarsis(ab_or_cd);

		// {"ab", "cd"}.repeat(1) = {"ab", "cd"}
		assertTrue(a.repeat(new Interval(1, 1)).getAutomaton().isEqualTo(ab_or_cd));

		// {"ab", "cd"}.repeat(2) = {"abab", "cdcd"}
		assertTrue(a.repeat(new Interval(2, 2)).getAutomaton().isEqualTo(abab_or_cdcd));

		// {"ab", "cd"}.repeat(0) = {""}
		assertTrue(a.repeat(new Interval(0, 0)).getAutomaton().isEqualTo(RegexAutomaton.emptyStr()));

		// {"ab", "cd"}.repeat([1,2]) = {"ab", "cd", "abab", "cdcd"}
		assertTrue(a.repeat(new Interval(1, 2)).getAutomaton().isEqualTo(ab_or_cd.union(abab_or_cdcd)));

		// {"ab", "cd"}.repeat([0,+infty]) = (ab|cd)*
		assertTrue(a.repeat(new Interval(MathNumber.ZERO, MathNumber.PLUS_INFINITY)).getAutomaton()
				.isEqualTo(ab_or_cd.star()));

		// {"ab", "cd"}.repeat([1,+infty]) = (ab|cd)(ab|cd)*
		assertTrue(a.repeat(new Interval(MathNumber.ONE, MathNumber.PLUS_INFINITY)).getAutomaton()
				.isEqualTo(ab_or_cd.concat(ab_or_cd.star())));
	}
}
