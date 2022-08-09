package it.unive.lisa.analysis.string;

import org.junit.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AcceptsEmptyLanguageTest {

    @Test
    public void test01() {
        Set<State> states = new HashSet<>();
        Set<Transition> delta = new HashSet<>();
        State[] st = new State[2];
        st[0] = new State(true, false);
        st[1] = new State(false, false);
        Collections.addAll(states, st);

        delta.add(new Transition(st[0], st[1], "a"));

        Automaton a = new Automaton(states, delta);

        assertTrue(a.acceptsEmptyLanguage());
    }

    @Test
    public void test02() {
        Set<State> states = new HashSet<>();
        Set<Transition> delta = new HashSet<>();
        State[] st = new State[2];
        st[0] = new State(true, false);
        st[1] = new State(false, true);
        Collections.addAll(states, st);

        delta.add(new Transition(st[0], st[0], "a"));
        delta.add(new Transition(st[0], st[1], "b"));
        delta.add(new Transition(st[1], st[1], "b"));

        // ab+
        Automaton a = new Automaton(states, delta);

        assertFalse(a.acceptsEmptyLanguage());
    }
}
