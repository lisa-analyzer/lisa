package it.unive.lisa.analysis.string.tarsis;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import it.unive.lisa.util.numeric.MathNumberConversionException;

public class TrimTest {

	@Test
	public void repeatSingleString() throws MathNumberConversionException {
		RegexAutomaton abc = RegexAutomaton.string("abc");
		RegexAutomaton ws = RegexAutomaton.string("   ");
		RegexAutomaton epsilon = RegexAutomaton.string("");
		RegexAutomaton abc_with_ws = RegexAutomaton.string("   a b c   ");
		RegexAutomaton empty_star = RegexAutomaton.string("   ").star();

		Tarsis a = new Tarsis(abc);
		Tarsis b = new Tarsis(ws);
		Tarsis c = new Tarsis(abc_with_ws);
		Tarsis d = new Tarsis(empty_star);

		// "abc".trim() = "abc"
		assertTrue(a.trim().getAutomaton().isEqualTo(abc));

		// "   ".trim() = ""
		assertTrue(b.trim().getAutomaton().isEqualTo(epsilon));

		// "   a b c   ".trim() = "a b c"
		assertTrue(c.trim().getAutomaton().isEqualTo(RegexAutomaton.string("a b c")));

		// ("   ")*.trim() = ""
		assertTrue(d.trim().getAutomaton().isEqualTo(RegexAutomaton.emptyStr()));
	}
}
