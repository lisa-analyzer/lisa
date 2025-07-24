package it.unive.lisa.analysis.string.tarsis;

import static org.junit.Assert.assertTrue;

import it.unive.lisa.lattices.string.tarsis.RegexAutomaton;
import it.unive.lisa.util.datastructures.regex.Atom;
import it.unive.lisa.util.datastructures.regex.Or;
import it.unive.lisa.util.numeric.IntInterval;
import it.unive.lisa.util.numeric.MathNumber;
import it.unive.lisa.util.numeric.MathNumberConversionException;
import org.junit.Test;

public class RepeatTest {

	@Test
	public void repeatSingleString()
			throws MathNumberConversionException {
		RegexAutomaton abc = RegexAutomaton.string("abc");
		RegexAutomaton abcabc = RegexAutomaton.string("abcabc");
		RegexAutomaton abc_star = RegexAutomaton.string("abc").star();

		Tarsis domain = new Tarsis();

		// "abc".repeat(1) = "abc"
		assertTrue(domain.repeat(abc, new IntInterval(1, 1)).isEqualTo(abc));

		// "abc".repeat(2) = "abcabc"
		assertTrue(domain.repeat(abc, new IntInterval(2, 2)).isEqualTo(abcabc));

		// "abc".repeat(0) = ""
		assertTrue(domain.repeat(abc, new IntInterval(0, 0)).isEqualTo(RegexAutomaton.emptyStr()));

		// "abc".repeat([1,2]) = {"abc", "abcabc"}
		assertTrue(domain.repeat(abc, new IntInterval(1, 2)).isEqualTo(abc.union(abcabc)));

		// "abc".repeat([0,+infty]) = (abc)*
		assertTrue(domain.repeat(abc, new IntInterval(MathNumber.ZERO, MathNumber.PLUS_INFINITY)).isEqualTo(abc_star));

		// "abc".repeat([1,+infty]) = abc(abc)*
		assertTrue(
				domain
						.repeat(abc, new IntInterval(MathNumber.ONE, MathNumber.PLUS_INFINITY))
						.isEqualTo(abc.concat(abc_star)));
	}

	@Test
	public void repeatTwoStrings()
			throws MathNumberConversionException {

		RegexAutomaton ab_or_cd = new Or(new Atom("ab"), new Atom("cd")).toAutomaton(RegexAutomaton.emptyLang());
		RegexAutomaton abab_or_cdcd = new Or(new Atom("abab"), new Atom("cdcd"))
				.toAutomaton(RegexAutomaton.emptyLang());

		Tarsis domain = new Tarsis();

		// {"ab", "cd"}.repeat(1) = {"ab", "cd"}
		assertTrue(domain.repeat(ab_or_cd, new IntInterval(1, 1)).isEqualTo(ab_or_cd));

		// {"ab", "cd"}.repeat(2) = {"abab", "cdcd"}
		assertTrue(domain.repeat(ab_or_cd, new IntInterval(2, 2)).isEqualTo(abab_or_cdcd));

		// {"ab", "cd"}.repeat(0) = {""}
		assertTrue(domain.repeat(ab_or_cd, new IntInterval(0, 0)).isEqualTo(RegexAutomaton.emptyStr()));

		// {"ab", "cd"}.repeat([1,2]) = {"ab", "cd", "abab", "cdcd"}
		assertTrue(domain.repeat(ab_or_cd, new IntInterval(1, 2)).isEqualTo(ab_or_cd.union(abab_or_cdcd)));

		// {"ab", "cd"}.repeat([0,+infty]) = (ab|cd)*
		assertTrue(
				domain
						.repeat(ab_or_cd, new IntInterval(MathNumber.ZERO, MathNumber.PLUS_INFINITY))
						.isEqualTo(ab_or_cd.star()));

		// {"ab", "cd"}.repeat([1,+infty]) = (ab|cd)(ab|cd)*
		assertTrue(
				domain
						.repeat(ab_or_cd, new IntInterval(MathNumber.ONE, MathNumber.PLUS_INFINITY))
						.isEqualTo(ab_or_cd.concat(ab_or_cd.star())));
	}

}
