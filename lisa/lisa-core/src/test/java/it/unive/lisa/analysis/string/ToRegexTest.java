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

        assertEquals("(a)*", a.toRegex());
    }

    @Test
    public void test02() {
        Set<State> states = new HashSet<>();
        State[] st = new State[2];
        st[0] = new State(true, false);
        st[1] = new State(false, true);
        Collections.addAll(states, st);

        Set<Transition> delta = new HashSet<>();
        delta.add(new Transition(st[0], st[0], "c"));
        delta.add(new Transition(st[0], st[1], "a"));
        delta.add(new Transition(st[1], st[0], "b"));
        delta.add(new Transition(st[1], st[1], "d"));

        Automaton a = new Automaton(states, delta);

        assertEquals("((c)*(a))(d|(b)(c)*(a))*", a.toRegex());
    }

    @Test
    public void test03() {
        Set<State> states = new HashSet<>();
        State[] st = new State[3];

        st[0] = new State(true, false);
        st[1] = new State(false, true);
        st[2] = new State(false, true);
        Collections.addAll(states, st);

        Set<Transition> delta = new HashSet<>();
        delta.add(new Transition(st[0], st[1], "a"));
        delta.add(new Transition(st[0], st[2], "c"));

        // ab | cc
        Automaton a = new Automaton(states, delta);
        assertEquals("((c)|(a))", a.toRegex());
    }

    @Test
    public void test04() {
        Set<State> states = new HashSet<>();
        State[] st = new State[3];
        st[0] = new State(true, false);
        st[1] = new State(false, false);
        st[2] = new State(false, true);
        Collections.addAll(states, st);

        Set<Transition> delta = new HashSet<>();
        delta.add(new Transition(st[0], st[1], "a"));
        delta.add(new Transition(st[1], st[2], "b"));

        Automaton a = new Automaton(states, delta);
        assertEquals("(a)(b)", a.toRegex());
    }

    @Test
    public void test05() {
        Set<State> states = new HashSet<>();
        State[] st = new State[5];
        Set<Transition> delta = new HashSet<>();

        st[0] = new State(true, false);
        st[1] = new State(false, false);
        st[2] = new State(false, false);
        st[3] = new State(false, true);
        st[4] = new State(false, true);
        Collections.addAll(states, st);

        delta.add(new Transition(st[0], st[1], "a"));
        delta.add(new Transition(st[0], st[2], "c"));
        delta.add(new Transition(st[1], st[3], "b"));
        delta.add(new Transition(st[2], st[4], "c"));

        // ab | cc
        Automaton a = new Automaton(states, delta);

        assertEquals("ab|cc", a.toRegex());
    }
}
