package it.unive.lisa.analysis.string;

import static org.junit.Assert.assertEquals;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

public class GetLanguageAtMostTest {

	@Test
	public void test1() {	
		Set<String> expected = new HashSet<>();
		expected.add("");
		expected.add("a");
		expected.add("aa");
		expected.add("aaa");

		State q0 = new State(0, true, true);
		Set<State> sts = new HashSet<>();
		sts.add(q0);

		Transition t = new Transition(q0, q0, "a");
		Set<Transition> delta = new HashSet<>();
		delta.add(t);

		Automaton a = new Automaton(sts, delta);
		assertEquals(expected, a.getLanguageAtMost(3));
	}

}
