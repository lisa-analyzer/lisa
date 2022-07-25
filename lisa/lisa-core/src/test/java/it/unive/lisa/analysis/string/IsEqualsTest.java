package it.unive.lisa.analysis.string;

import org.junit.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertTrue;

public class IsEqualsTest {

    @Test
    public void test01() {
        Set<State> states = new HashSet<>();
        State[] st = new State[4];
        Set<State> states2 = new HashSet<>();
        State[] st2 = new State[3];
        Set<Transition> delta = new HashSet<>();
        Set<Transition> delta2 = new HashSet<>();
        st[0] = new State(true, false);
        st[1] = new State(false, false);
        st[2] = new State(false, false);
        st[3] = new State(false, true);
        Collections.addAll(states, st);

        st2[0] = new State(false, false);
        st2[1] = new State(false, false);
        st2[2] = new State(false, true);
        Collections.addAll(states2, st2);

        delta.add(new Transition(st[0], st[1], "a"));
        delta.add(new Transition(st[0], st[2], "b"));
        delta.add(new Transition(st[1], st[3], "c"));
        delta.add(new Transition(st[2], st[3], "c"));

        delta2.add(new Transition(st2[0], st2[1], "a"));
        delta2.add(new Transition(st2[0], st2[1], "b"));
        delta2.add(new Transition(st2[1], st2[2], "c"));

        Automaton a = new Automaton(states, delta);
        Automaton a2 = new Automaton(states, delta);

        assertTrue(a.isEqual(a2));
    }
}
