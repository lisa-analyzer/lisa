package it.unive.lisa.analysis.string.tarsis;

import static org.junit.Assert.assertTrue;

import it.unive.lisa.util.numeric.MathNumberConversionException;
import org.junit.Test;

public class TrimTest {

	@Test
	public void trimSingleString() throws MathNumberConversionException {
		RegexAutomaton abc = RegexAutomaton.string("abc");
		RegexAutomaton ws = RegexAutomaton.string("   ");
		RegexAutomaton epsilon = RegexAutomaton.string("");
		RegexAutomaton abc_with_ws = RegexAutomaton.string("   a b c   ");
		RegexAutomaton empty_star = RegexAutomaton.string("   ").star();
		RegexAutomaton comp1 = RegexAutomaton.string("   ").concat(RegexAutomaton.string(" abc"));
		RegexAutomaton comp2 = RegexAutomaton.string(" a").concat(RegexAutomaton.string(" b "));
		RegexAutomaton comp3 = RegexAutomaton.string(" abc ").concat(RegexAutomaton.string("   "));

		// "abc".trim() = "abc"
		Tarsis a = new Tarsis(abc);
		assertTrue(a.trim().getAutomaton().isEqualTo(abc));

		// " ".trim() = ""
		Tarsis b = new Tarsis(ws);
		assertTrue(b.trim().getAutomaton().isEqualTo(epsilon));

		// " a b c ".trim() = "a b c"
		Tarsis c = new Tarsis(abc_with_ws);
		assertTrue(c.trim().getAutomaton().isEqualTo(RegexAutomaton.string("a b c")));

		// (" ")*.trim() = ""
		Tarsis d = new Tarsis(empty_star);
		assertTrue(d.trim().getAutomaton().isEqualTo(RegexAutomaton.emptyStr()));

		// " " + " abc".trim() = "abc"
		Tarsis e = new Tarsis(comp1);
		assertTrue(e.trim().getAutomaton().isEqualTo(abc));

		// " a" + " b ".trim() = "a b"
		Tarsis f = new Tarsis(comp2);
		assertTrue(f.trim().getAutomaton().isEqualTo(RegexAutomaton.string("a b")));

		// " a b c "*.trim() = "a b c " + (" a b c ")* + " a b c"
		Tarsis g = new Tarsis(abc_with_ws.star());
		assertTrue(g.trim().getAutomaton().isEqualTo(RegexAutomaton.string("a b c   ").concat(abc_with_ws.star())
				.concat(RegexAutomaton.string("   a b c")).star()));

		// " abc " + " ".trim() = "abc"
		Tarsis h = new Tarsis(comp3);
		assertTrue(h.trim().getAutomaton().isEqualTo(RegexAutomaton.string("abc")));
	}
}
