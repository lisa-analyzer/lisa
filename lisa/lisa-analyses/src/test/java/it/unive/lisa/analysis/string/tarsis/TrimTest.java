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
		RegexAutomaton comp1 = RegexAutomaton.string("   ").concat(RegexAutomaton.string(" abc"));
		RegexAutomaton comp2 = RegexAutomaton.string(" a").concat(RegexAutomaton.string(" b "));

		// "abc".trim() = "abc"
		Tarsis a = new Tarsis(abc);
		assertTrue(a.trim().getAutomaton().isEqualTo(abc));

		// "   ".trim() = ""
		Tarsis b = new Tarsis(ws);
		assertTrue(b.trim().getAutomaton().isEqualTo(epsilon));

		// "   a b c   ".trim() = "a b c"
		Tarsis c = new Tarsis(abc_with_ws);
		assertTrue(c.trim().getAutomaton().isEqualTo(RegexAutomaton.string("a b c")));

		// ("   ")*.trim() = ""
		Tarsis d = new Tarsis(empty_star);
		assertTrue(d.trim().getAutomaton().isEqualTo(RegexAutomaton.emptyStr()));
		
		// "   " + " abc".trim() = "abc"
		Tarsis e = new Tarsis(comp1);
		assertTrue(e.trim().getAutomaton().isEqualTo(abc));
		
		// " a" + " b ".trim() = "a b "
		Tarsis f = new Tarsis(comp2);
		assertTrue(f.trim().getAutomaton().isEqualTo(RegexAutomaton.string("a b ")));
	}
}
