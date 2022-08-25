package it.unive.lisa.analysis.string;

import org.junit.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;

public class ToRegexTest {

    @Test
    public void test01() {
        Set<State> states = new HashSet<>();
        State[] st = new State[1];
        st[0] = new State(true, true);
        Collections.addAll(states, st);

        Set<Transition> delta = new HashSet<>();
        delta.add(new Transition(st[0], st[0], "a"));

        Automaton a = new Automaton(states, delta);

        assertEquals("a*", a.toRegex());
    }
}
