package it.unive.lisa.analysis.string;

import org.junit.Test;
import java.util.Set;
import java.util.HashSet;
import static org.junit.Assert.assertEquals;

public class GetLanguageAtMostTest {

    @Test
    public void test1() {
	String[] expected = {"", "a", "aa", "aaa", "aaaa", "aaaaa", "aaaaaa", "aaaaaaa", "aaaaaaaa"};

	State q0 = new State(0, true, true);
	Set<State> sts = new HashSet<>();
	sts.add(q0);

	Transition t = new Transition(q0, q0, "a");
	Set<Transition> delta = new HashSet<>();
	delta.add(t);

	Automaton a = new Automaton(sts, delta);
	assertEquals(expected, a.getLanguageAtMost(9));
    }
	
}
