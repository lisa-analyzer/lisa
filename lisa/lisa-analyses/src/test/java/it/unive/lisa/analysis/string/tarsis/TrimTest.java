package it.unive.lisa.analysis.string.tarsis;

import static org.junit.Assert.assertTrue;

import it.unive.lisa.lattices.string.tarsis.RegexAutomaton;
import it.unive.lisa.util.numeric.MathNumberConversionException;
import org.junit.Test;

public class TrimTest {

	@Test
	public void trimSingleString()
			throws MathNumberConversionException {
		RegexAutomaton abc = RegexAutomaton.string("abc");
		RegexAutomaton ws = RegexAutomaton.string("   ");
		RegexAutomaton epsilon = RegexAutomaton.string("");
		RegexAutomaton abc_with_ws = RegexAutomaton.string("   a b c   ");
		RegexAutomaton empty_star = RegexAutomaton.string("   ").star();
		RegexAutomaton comp1 = RegexAutomaton.string("   ").concat(RegexAutomaton.string(" abc"));
		RegexAutomaton comp2 = RegexAutomaton.string(" a").concat(RegexAutomaton.string(" b "));
		RegexAutomaton comp3 = RegexAutomaton.string(" abc ").concat(RegexAutomaton.string("   "));

		// "abc".trim() = "abc"
		assertTrue(abc.trim().isEqualTo(abc));

		// " ".trim() = ""
		assertTrue(ws.trim().isEqualTo(epsilon));

		// " a b c ".trim() = "a b c"
		assertTrue(abc_with_ws.trim().isEqualTo(RegexAutomaton.string("a b c")));

		// (" ")*.trim() = ""
		assertTrue(empty_star.trim().isEqualTo(RegexAutomaton.emptyStr()));

		// " " + " abc".trim() = "abc"
		assertTrue(comp1.trim().isEqualTo(abc));

		// " a" + " b ".trim() = "a b"
		assertTrue(comp2.trim().isEqualTo(RegexAutomaton.string("a b")));

		// " abc " + " ".trim() = "abc"
		assertTrue(comp3.trim().isEqualTo(RegexAutomaton.string("abc")));
	}

}
