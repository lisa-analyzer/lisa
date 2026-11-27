package it.unive.lisa.analysis.string.tarsis;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.lattices.string.tarsis.RegexAutomaton;
import it.unive.lisa.util.datastructures.automaton.CyclicAutomatonException;
import org.junit.jupiter.api.Test;

/**
 * Tests for the intersection (and thus the greatest lower bound) of
 * {@link RegexAutomaton}s.
 */
public class IntersectionTest {

	private static RegexAutomaton str(
			String s) {
		return RegexAutomaton.string(s);
	}

	private static RegexAutomaton top() {
		return RegexAutomaton.topString();
	}

	private static boolean disjoint(
			RegexAutomaton a,
			RegexAutomaton b)
			throws SemanticException {
		return a.glb(b).isBottom() && b.glb(a).isBottom();
	}

	@Test
	public void differentAtoms() throws SemanticException, CyclicAutomatonException {
		// "ab" followed by "c" is the same language as "abc", even if the
		// strings are split differently in the transitions
		RegexAutomaton abc = str("ab").concat(str("c"));
		assertFalse(disjoint(abc, str("abc")));
		assertTrue(abc.glb(str("abc")).getLanguage().contains("abc"));
		assertTrue(disjoint(abc, str("abd")));
	}

	@Test
	public void topMatchesManyCharacters() throws SemanticException, CyclicAutomatonException {
		RegexAutomaton pre = str("pre").concat(top());
		assertFalse(disjoint(pre, str("prefix")));
		assertTrue(pre.glb(str("prefix")).getLanguage().contains("prefix"));
		assertFalse(disjoint(str("x").concat(top()).concat(str("y")), str("xabcy")));
		assertTrue(disjoint(pre, str("prx")));
	}

	@Test
	public void topMatchesTheEmptyString() throws SemanticException {
		assertFalse(disjoint(str("ab").concat(top()), str("ab")));
		assertFalse(disjoint(top().concat(str("ab")), str("ab")));
		assertFalse(disjoint(str("a").concat(top()).concat(str("b")), str("ab")));
	}

	@Test
	public void topAgainstTop() throws SemanticException {
		assertFalse(disjoint(str("a").concat(top()), top().concat(str("b"))));
	}
}
