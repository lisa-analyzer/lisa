package it.unive.lisa.util.datastructures.automaton;

import it.unive.lisa.util.collections.workset.FIFOWorkingSet;
import it.unive.lisa.util.collections.workset.WorkingSet;
import it.unive.lisa.util.datastructures.regex.Atom;
import it.unive.lisa.util.datastructures.regex.EmptySet;
import it.unive.lisa.util.datastructures.regex.RegularExpression;
import it.unive.lisa.util.numeric.IntInterval;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.Stack;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

/**
 * A class that describes a generic automaton(dfa, nfa, epsilon nfa).
 * Transitions recognize instances of {@link TransitionSymbol}.
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <A> the concrete type of this automaton
 * @param <T> the concrete type of {@link TransitionSymbol}s that instances of
 *                this class have on their transitions
 */
public abstract class Automaton<A extends Automaton<A, T>,
		T extends TransitionSymbol<T>> implements AutomataFactory<A, T> {

	/**
	 * The states of this automaton.
	 */
	protected final SortedSet<State> states;

	/**
	 * The transitions of this automaton.
	 */
	protected final SortedSet<Transition<T>> transitions;

	/**
	 * Flag that tracks if this automaton is deterministic. If
	 * {@link Optional#isPresent()} returns {@code false}, then it is unknown if
	 * this automaton is deterministic or not.
	 */
	protected Optional<Boolean> deterministic;

	/**
	 * Flag that tracks if this automaton has been minimized. If
	 * {@link Optional#isPresent()} returns {@code false}, then it is unknown if
	 * this automaton is minimum or not.
	 */
	protected Optional<Boolean> minimized;

	/**
	 * Builds an empty automaton.
	 */
	protected Automaton() {
		this.states = new TreeSet<>();
		this.transitions = new TreeSet<>();
		this.deterministic = Optional.empty();
		this.minimized = Optional.empty();
	}

	/**
	 * Builds a new automaton with given {@code states} and {@code transitions}.
	 *
	 * @param states      the set of states of the new automaton
	 * @param transitions the set of the transitions of the new automaton
	 * 
	 * @throws IllegalArgumentException if the set of states contains multiple
	 *                                      states with the same ids
	 */
	protected Automaton(
			SortedSet<State> states,
			SortedSet<Transition<T>> transitions) {
		if (states.size() != states.stream().map(State::getId).distinct().count())
			throw new IllegalArgumentException("The automaton being created contains multiple states with the same id");
		this.states = states;
		this.transitions = transitions;
		this.deterministic = Optional.empty();
		this.minimized = Optional.empty();
	}

	/**
	 * Yields the set of states of this automaton.
	 * 
	 * @return the set of states
	 */
	public SortedSet<State> getStates() {
		return states;
	}

	/**
	 * Yields the set of transitions contained in this automaton.
	 * 
	 * @return the set of transitions
	 */
	public SortedSet<Transition<T>> getTransitions() {
		return transitions;
	}

	/**
	 * Adds a new state to this automaton.
	 * 
	 * @param s the state to add
	 * 
	 * @throws IllegalArgumentException a state with the given id is already
	 *                                      part of this automaton
	 */
	public void addState(
			State s) {
		if (states.stream().filter(ss -> ss.getId() == s.getId()).findAny().isPresent())
			throw new IllegalArgumentException("A state with id " + s.getId() + " aready exists");
		states.add(s);
		this.deterministic = Optional.empty();
		this.minimized = Optional.empty();
	}

	/**
	 * Builds a new transition going from {@code from} to {@code to} and
	 * recognizing {@code input} and adds it to the set of transitions of this
	 * automaton.
	 * 
	 * @param from  the source node
	 * @param to    the destination node
	 * @param input the input to be recognized by the transition
	 */
	public void addTransition(
			State from,
			State to,
			T input) {
		addTransition(new Transition<>(from, to, input));
	}

	/**
	 * Adds the given transition to the set of transitions of this automaton.
	 * 
	 * @param t the transition to add
	 */
	public void addTransition(
			Transition<T> t) {
		transitions.add(t);
		this.deterministic = Optional.empty();
		this.minimized = Optional.empty();
	}

	/**
	 * Removes every transition in the given set from the ones of this
	 * automaton.
	 * 
	 * @param ts the set of transitions to remove
	 */
	public void removeTransitions(
			Set<Transition<T>> ts) {
		transitions.removeAll(ts);
		this.deterministic = Optional.empty();
		this.minimized = Optional.empty();
	}

	/**
	 * Removes every state in the given set from the ones of this automaton.
	 * 
	 * @param ts the set of states to remove
	 */
	public void removeStates(
			Set<State> ts) {
		states.removeAll(ts);
		this.deterministic = Optional.empty();
		this.minimized = Optional.empty();
	}

	/**
	 * Yields a minimal automaton equivalent to this one through Brzozowski's
	 * minimization algorithm. <br>
	 * <br>
	 * This automaton is never modified.
	 * 
	 * @return a minimal automaton equivalent to this one
	 */
	@SuppressWarnings("unchecked")
	public A minimize() {
		if (minimized.isPresent() && minimized.get())
			return (A) this;
		A a = (A) this;
		if (!isDeterministic())
			a = determinize();

		a = a.reverse().determinize();
		a = a.removeUnreachableStates();
		a = a.reverse().determinize();
		a = a.removeUnreachableStates();

		a.deterministic = Optional.of(true);
		a.minimized = Optional.of(true);
		return a;
	}

	/**
	 * Yields the set of all outgoing transitions from the given state.
	 * 
	 * @param s the state
	 * 
	 * @return the set of outgoing transitions
	 */
	public SortedSet<Transition<T>> getOutgoingTransitionsFrom(
			State s) {
		return new TreeSet<>(transitions.stream().filter(t -> t.getSource().equals(s)).collect(Collectors.toSet()));
	}

	/**
	 * Yields the set of all ingoing transitions to the given state.
	 * 
	 * @param s the state
	 * 
	 * @return the set of ingoing transitions
	 */
	public SortedSet<Transition<T>> getIngoingTransitionsFrom(
			State s) {
		return new TreeSet<>(
			transitions.stream().filter(t -> t.getDestination().equals(s)).collect(Collectors.toSet()));
	}

	/**
	 * Yields the set of final states of this automaton.
	 * 
	 * @return the set of final states
	 */
	public SortedSet<State> getFinalStates() {
		SortedSet<State> result = new TreeSet<>();

		for (State s : states)
			if (s.isFinal())
				result.add(s);

		return result;
	}

	/**
	 * Yields one of the initial states of this automaton. If this automaton
	 * does not have an initial state, this method returns {@code null}.
	 * 
	 * @return one of the initial states, or {@code null}
	 */
	public State getInitialState() {
		for (State s : states)
			if (s.isInitial())
				return s;

		return null;
	}

	/**
	 * Yields the set of initial states of this automaton.
	 * 
	 * @return the set of initial states
	 */
	public SortedSet<State> getInitialStates() {
		SortedSet<State> initialStates = new TreeSet<>();

		for (State s : states)
			if (s.isInitial())
				initialStates.add(s);

		return initialStates;
	}

	/**
	 * Remove all the unreachable states from the current automaton.
	 * 
	 * @return a copy of this automaton without unreachable states
	 */
	public A removeUnreachableStates() {
		SortedSet<State> newStates = new TreeSet<>(states);
		SortedSet<Transition<T>> newTransitions = new TreeSet<>(transitions);

		Set<State> reachableStates = new HashSet<>();
		Set<State> ws = new HashSet<>();
		Set<State> temp = new HashSet<>();

		reachableStates.addAll(getInitialStates());
		getInitialStates().forEach(ws::add);

		do {
			for (State s : ws)
				for (Transition<T> t : getOutgoingTransitionsFrom(s))
					temp.add(t.getDestination());

			temp.removeAll(reachableStates);
			ws.clear();
			ws.addAll(temp);
			reachableStates.addAll(ws);
		} while (!ws.isEmpty());

		newStates.removeIf(s -> !reachableStates.contains(s));
		newTransitions
			.removeIf(t -> !reachableStates.contains(t.getSource()) || !reachableStates.contains(t.getDestination()));
		return from(newStates, newTransitions);
	}

	/**
	 * Creates an automaton that accept the reverse language. The returned
	 * automaton might have more than one initial state.
	 *
	 * @return a newly created automaton that accepts the reverse language of
	 *             {@code this}.
	 */
	public A reverse() {
		SortedSet<Transition<T>> tr = new TreeSet<>();
		SortedSet<State> st = new TreeSet<>();
		Map<State, State> revStates = new HashMap<>();

		for (State s : states) {
			boolean fin = false, init = false;
			if (s.isInitial())
				fin = true;
			if (s.isFinal())
				init = true;
			State q = new State(s.getId(), init, fin);
			st.add(q);
			revStates.put(s, q);
		}

		// create transitions using the new states of the reverse automaton
		for (Transition<T> t : transitions)
			tr.add(
				new Transition<
						T>(revStates.get(t.getDestination()), revStates.get(t.getSource()), t.getSymbol().reverse()));

		return from(st, tr);// .toSingleInitalState();
	}

	/**
	 * Computes the epsilon closure of this automaton starting from {@code s},
	 * namely the set of states that are reachable from {@code s} just with
	 * epsilon transitions.
	 *
	 * @param s the state from which the method starts to compute the epsilon
	 *              closure
	 * 
	 * @return the set of states that are reachable from {@code s} just with
	 *             epsilon transitions
	 */
	public SortedSet<State> epsilonClosure(
			State s) {
		SortedSet<State> paths = new TreeSet<>();
		SortedSet<State> previous = new TreeSet<>();
		SortedSet<State> partial;
		paths.add(s);

		while (!paths.equals(previous)) {
			previous = new TreeSet<>(paths);
			partial = new TreeSet<>();

			for (State reached : paths)
				for (Transition<T> t : getOutgoingTransitionsFrom(reached))
					if (t.isEpsilonTransition())
						partial.add(t.getDestination());

			paths.addAll(partial);
		}

		return paths;
	}

	/**
	 * Computes the epsilon closure of this automaton starting from {@code set},
	 * namely the set of states that are reachable from each state in
	 * {@code set} just with epsilon transitions.
	 *
	 * @param set the set of states from which the epsilon closure is computed.
	 * 
	 * @return the set of states that are reachable from {@code set} just with
	 *             epsilon transitions
	 */
	public SortedSet<State> epsilonClosure(
			Set<State> set) {
		SortedSet<State> solution = new TreeSet<>();

		for (State s : set)
			solution.addAll(epsilonClosure(s));

		return solution;
	}

	/**
	 * Yields that states that can be reached from at least one state in
	 * {@code set} by reading symbol {@code sym}.
	 * 
	 * @param set the set of states to start from
	 * @param sym the symbol to read
	 * 
	 * @return the set of states that can be reached
	 */
	public SortedSet<State> nextStatesNFA(
			Set<State> set,
			T sym) {
		SortedSet<State> solution = new TreeSet<>();
		for (State s : set)
			for (Transition<T> t : getOutgoingTransitionsFrom(s))
				if (t.getSymbol().equals(sym))
					solution.add(t.getDestination());

		return solution;
	}

	private static boolean containsInitialState(
			Set<State> states) {
		for (State s : states)
			if (s.isInitial())
				return true;

		return false;
	}

	private static boolean containsFinalState(
			Set<State> states) {
		for (State s : states)
			if (s.isFinal())
				return true;

		return false;
	}

	/**
	 * Yields the set of symbols that can be read (accepted) by the given set of
	 * states.
	 * 
	 * @param states the set of states
	 * 
	 * @return the set of symbols that can be read
	 */
	public SortedSet<T> getReadableSymbolsFromStates(
			Set<State> states) {
		SortedSet<T> result = new TreeSet<>();

		for (State s : states)
			for (Transition<T> t : getOutgoingTransitionsFrom(s))
				if (!t.getSymbol().isEpsilon())
					result.add(t.getSymbol());

		return result;
	}

	/**
	 * Yields the set of symbols that can be read (accepted) by the given state,
	 * excluding epsilon.
	 * 
	 * @param state the state
	 * 
	 * @return the set of symbols that can be read
	 */
	public SortedSet<T> getReadableSymbolsFromState(
			State state) {
		SortedSet<T> result = new TreeSet<>();

		for (Transition<T> t : getOutgoingTransitionsFrom(state))
			if (!t.getSymbol().isEpsilon())
				result.add(t.getSymbol());

		return result;
	}

	/**
	 * Yields true if and only if this automaton is deterministic, that is, if
	 * the transition relation is a function. This is computed by detecting if
	 * none of the states of this automaton has two outgoing transitions
	 * recognizing the same symbol but going to different states.
	 * 
	 * @return {@code true} if and only if that condition holds
	 */
	public boolean isDeterministic() {
		if (deterministic.isPresent())
			return deterministic.get();

		if (states.stream().filter(s -> s.isInitial()).collect(Collectors.toSet()).size() > 1) {
			deterministic = Optional.of(false);
			return false;
		}

		deterministic = Optional.of(false);
		for (State s : states) {
			Set<Transition<T>> outgoingTranisitions = getOutgoingTransitionsFrom(s);
			for (Transition<T> t : outgoingTranisitions)
				if (t.getSymbol().isEpsilon())
					return false;
				else
					for (Transition<T> t2 : outgoingTranisitions)
						if (t2.getSymbol().isEpsilon())
							return false;
						else if (!t.getDestination().equals(t2.getDestination())
								&& t.getSymbol().equals(t2.getSymbol()))
							return false;
		}

		deterministic = Optional.of(true);
		return true;
	}

	/**
	 * Yields a deterministic automaton equivalent to this one. It this
	 * automaton is already deterministic, it is immediately returned instead.
	 * <br>
	 * <br>
	 * This automaton is never modified by this method.
	 * 
	 * @return a deterministic automaton equivalent to this one.
	 */
	@SuppressWarnings("unchecked")
	public A determinize() {
		if (isDeterministic())
			return (A) this;

		SortedSet<State> newStates = new TreeSet<>();
		SortedSet<Transition<T>> newDelta = new TreeSet<>();

		Map<Set<State>, Boolean> marked = new HashMap<>();
		Map<Set<State>, State> statesName = new HashMap<>();
		WorkingSet<Set<State>> unmarked = FIFOWorkingSet.mk();

		int num = 0;
		Set<State> temp = epsilonClosure(getInitialStates());
		statesName.put(temp, new State(num++, true, containsFinalState(temp)));

		newStates.add(statesName.get(temp));
		marked.put(temp, false);
		unmarked.push(temp);

		while (!unmarked.isEmpty()) {
			Set<State> T = unmarked.pop();
			newStates.add(statesName.get(T));
			marked.put(T, true);

			for (T alphabet : getReadableSymbolsFromStates(T)) {
				temp = epsilonClosure(nextStatesNFA(T, alphabet));

				if (!statesName.containsKey(temp))
					statesName.put(temp, new State(num++, false, containsFinalState(temp)));

				newStates.add(statesName.get(temp));

				if (!marked.containsKey(temp)) {
					marked.put(temp, false);
					unmarked.push(temp);
				}

				newDelta.add(new Transition<>(statesName.get(T), statesName.get(temp), alphabet));
			}
		}

		A det = from(newStates, newDelta);
		det.deterministic = Optional.of(true);
		det.minimized = Optional.of(false);
		return det;
	}

	/**
	 * Yields the automaton recognizing the language that is the union of the
	 * languages recognized by {@code this} and {@code other}.
	 *
	 * @param other the other automaton
	 * 
	 * @return Yields the automaton recognizing the language that is the union
	 *             of the languages recognized by {@code this} and {@code other}
	 */
	@SuppressWarnings("unchecked")
	public A union(
			A other) {
		if (this == other)
			return (A) this;

		SortedSet<State> sts = new TreeSet<>();
		SortedSet<Transition<T>> ts = new TreeSet<>();

		SortedSet<State> initStates = new TreeSet<>();

		Map<State, State> thisMapping = new HashMap<>();
		Map<State, State> otherMapping = new HashMap<>();

		int c = 1;
		for (State s : states) {
			State st = new State(c++, false, s.isFinal());
			thisMapping.put(s, st);
			sts.add(st);
			if (s.isInitial())
				initStates.add(st);
		}

		for (State s : other.states) {
			State st = new State(c++, false, s.isFinal());
			otherMapping.put(s, st);
			sts.add(st);
			if (s.isInitial())
				initStates.add(st);
		}

		State q0 = new State(0, true, false);
		sts.add(q0);

		for (State s : initStates)
			ts.add(new Transition<>(q0, s, epsilon()));

		for (Transition<T> t : transitions)
			ts.add(
				new Transition<>(thisMapping.get(t.getSource()), thisMapping.get(t.getDestination()), t.getSymbol()));

		for (Transition<T> t : other.transitions)
			ts.add(
				new Transition<>(otherMapping.get(t.getSource()), otherMapping.get(t.getDestination()), t.getSymbol()));

		return from(sts, ts).minimize();
	}

	/**
	 * Checks if the Automaton {@code this} has any cycle.
	 *
	 * @return a boolean value that tells if {@code this} has any cycle.
	 */
	public boolean hasCycle() {
		// BFS: move one step each time starting from the initial states
		Set<State> currentStates = getInitialStates();
		Set<State> visited = new TreeSet<>();
		while (!visited.containsAll(states)) {
			Set<State> temp = new TreeSet<>();
			for (State s : currentStates)
				for (State follower : getNextStates(s))
					if (s == follower || visited.contains(follower))
						return true;
					else
						temp.add(follower);

			visited.addAll(currentStates);
			currentStates = temp;
		}

		return false;
	}

	/**
	 * Yields the set of possible successors of the given node.
	 * 
	 * @param node the node
	 * 
	 * @return the set of possible successors
	 */
	public SortedSet<State> getNextStates(
			State node) {
		SortedSet<State> neighbors = new TreeSet<>();
		for (Transition<T> edge : getOutgoingTransitionsFrom(node))
			neighbors.add(edge.getDestination());

		return neighbors;
	}

	/**
	 * Returns the concretized language accepted by {@code this}.
	 * {@link Transition}s' symbols representing unknown characters or strings
	 * will be encoded using {@link TransitionSymbol#UNKNOWN_SYMBOL}, while
	 * empty strings will be concretized (that is, they will not appear as
	 * {@link TransitionSymbol#EPSILON}).
	 * 
	 * @return a set representing the language accepted by {@code this}.
	 * 
	 * @throws CyclicAutomatonException thrown if the automaton is cyclic.
	 */
	public SortedSet<String> getLanguage()
			throws CyclicAutomatonException {
		SortedSet<String> lang = new TreeSet<>();
		if (hasCycle())
			throw new CyclicAutomatonException();

		// is the minimum automaton recognizing empty string
		if (states.size() == 1
				&& states.iterator().next().isFinal()
				&& states.iterator().next().isInitial()
				&& transitions.isEmpty()) {
			lang.add("");
			return lang;
		}

		WorkingSet<Pair<String, Transition<T>>> ws = FIFOWorkingSet.mk();
		for (State q : getInitialStates())
			for (Transition<T> t : getOutgoingTransitionsFrom(q))
				ws.push(Pair.of("", t));

		while (!ws.isEmpty()) {
			Pair<String, Transition<T>> top = ws.pop();
			String currentString = top.getKey();
			Transition<T> tr = top.getValue();

			T symbol = tr.getSymbol();
			String sym = symbol.isEpsilon() ? "" : symbol.toString();
			if (tr.getDestination().isFinal())
				lang.add(currentString + sym);

			for (Transition<T> t : getOutgoingTransitionsFrom(tr.getDestination()))
				ws.push(Pair.of(currentString + sym, t));
		}

		return lang;
	}

	/**
	 * Performs the totalization of this automaton w.r.t the given alphabet.
	 * 
	 * @param sigma the alphabet to use during totalization
	 * 
	 * @return the totalized automaton
	 */
	public A totalize(
			Set<T> sigma) {
		SortedSet<State> newStates = new TreeSet<>(states);
		SortedSet<Transition<T>> newTransitions = new TreeSet<>(transitions);

		int code = 1 + states.stream().map(State::getId).max(Integer::compare).orElseGet(() -> -1);

		// add a new "garbage" state
		State garbage = new State(code, false, false);
		newStates.add(garbage);

		// add additional transitions towards the garbage state
		for (State s : states)
			for (T c : sigma)
				if (!getReadableSymbolsFromStates(Collections.singleton(s)).contains(c))
					newTransitions.add(new Transition<>(s, garbage, c));

		// self loops over garbage state
		for (T c : sigma)
			newTransitions.add(new Transition<>(garbage, garbage, c));

		return from(newStates, newTransitions);
	}

	/**
	 * Return a new Automaton that accept a language that is the complementary
	 * language of {@code this}.
	 * 
	 * @param sigma the alphabet used for automaton completion.
	 * 
	 * @return the complement Automaton of {@code this}.
	 */
	public A complement(
			Set<T> sigma) {
		SortedSet<State> sts = new TreeSet<>();
		SortedSet<Transition<T>> delta = new TreeSet<>();
		Map<State, State> oldToNew = new HashMap<>();
		A r = determinize().totalize(sigma != null ? sigma : getAlphabet());

		for (State s : r.states) {
			State q = new State(s.getId(), s.isInitial(), !s.isFinal());
			sts.add(q);
			oldToNew.put(s, q);
		}
		// creates all the new transitions
		for (Transition<T> t : r.transitions)
			delta.add(new Transition<>(oldToNew.get(t.getSource()), oldToNew.get(t.getDestination()), t.getSymbol()));

		return from(sts, delta).minimize();
	}

	/**
	 * Returns the Automaton that accepts the language that is the intersection
	 * between the language of {@code this} and another Automaton.
	 *
	 * @param other the Automaton used for intersection with this.
	 * 
	 * @return a new Automaton accepting the language which is the intersection
	 *             between the language of {@code this} and {@code other}'s
	 *             langauge.
	 */
	@SuppressWarnings("unchecked")
	public A intersection(
			A other) {
		if (this == other)
			return (A) this;

		int code = 0;
		Map<State, Pair<State, State>> stateMapping = new HashMap<>();
		SortedSet<State> newStates = new TreeSet<>();
		SortedSet<Transition<T>> newDelta = new TreeSet<Transition<T>>();

		for (State s1 : states)
			for (State s2 : other.states) {
				State s = new State(code++, s1.isInitial() && s2.isInitial(), s1.isFinal() && s2.isFinal());
				stateMapping.put(s, Pair.of(s1, s2));
				newStates.add(s);
			}

		for (Transition<T> t1 : transitions) {
			for (Transition<T> t2 : other.transitions) {
				if (t1.getSymbol().equals(t2.getSymbol())) {
					State from = getStateFromPair(stateMapping, Pair.of(t1.getSource(), t2.getSource()));
					State to = getStateFromPair(stateMapping, Pair.of(t1.getDestination(), t2.getDestination()));
					newDelta.add(new Transition<T>(from, to, t1.getSymbol()));
				}
			}
		}

		return from(newStates, newDelta).minimize();
	}

	/**
	 * Given a pair of states, yields the state associated to the pair in the
	 * given state mapping.
	 * 
	 * @param mapping the mapping
	 * @param pair    the pair of states
	 * 
	 * @return the state associated to the pair in the given state mapping
	 */
	protected State getStateFromPair(
			Map<State, Pair<State, State>> mapping,
			Pair<State, State> pair) {
		for (Entry<State, Pair<State, State>> entry : mapping.entrySet())
			if (entry.getValue().getLeft().equals(pair.getLeft())
					&& entry.getValue().getRight().equals(pair.getRight()))
				return entry.getKey();

		return null;
	}

	/**
	 * Checks if the Automaton {@code this} accepts the empty language.
	 * 
	 * @return a boolean value that points out if {@code this} accepts the empty
	 *             language
	 */
	public boolean acceptsEmptyLanguage() {
		// if there's no final state this automaton accepts the empty language
		return states.stream().noneMatch(State::isFinal);
	}

	/**
	 * Yields {@code true} if and only if {@code this} is contained into
	 * {@code other}, that is, if the language recognized by the intersection
	 * between {@code this} and the complement of {@code other} is empty.
	 * 
	 * @param other the other automaton
	 * 
	 * @return {@code true} if that condition holds
	 */
	public boolean isContained(
			A other) {
		SortedSet<T> commonAlphabet = commonAlphabet(other);
		A complement = other.complement(commonAlphabet);
		A intersection = intersection(complement);
		A minimal = intersection.minimize();
		return minimal.acceptsEmptyLanguage();
	}

	/**
	 * Checks if the automaton {@code this} accepts the same language as
	 * {@code other}, implemented as:<br>
	 * {@code language(A).equals(language(B))} if both automata are loop free,
	 * {@code A.contains(B) && B.contains(A)} otherwise.<br>
	 * 
	 * @param other the other automaton
	 * 
	 * @return a boolean value that points out if the automata are equivalent
	 */
	public boolean isEqualTo(
			A other) {
		A o = (A) other;
		if (!hasCycle() && !o.hasCycle())
			try {
				return getLanguage().equals(o.getLanguage());
			} catch (CyclicAutomatonException e) {
				// safe to ignore
			}

		A a = minimize();
		A b = o.minimize();

		if (a.hasCycle() && !b.hasCycle() || !a.hasCycle() && b.hasCycle())
			return false;

		if (!a.hasCycle() && !b.hasCycle())
			try {
				return a.getLanguage().equals(b.getLanguage());
			} catch (CyclicAutomatonException e) {
				// safe to ignore
			}

		if (!a.isContained(b))
			return false;

		if (!b.isContained(a))
			return false;

		return true;
	}

	/**
	 * Deep-copies this automaton to a new one.
	 * 
	 * @return the copied automaton.
	 */
	public A copy() {
		SortedSet<State> newStates = new TreeSet<>();
		SortedSet<Transition<T>> newTransitions = new TreeSet<>();
		HashMap<String, State> nameToStates = new HashMap<>();

		for (State s : states) {
			State newState = new State(s.getId(), s.isInitial(), s.isFinal());
			newStates.add(newState);
			nameToStates.put(newState.getState(), newState);
		}

		for (Transition<T> t : transitions)
			newTransitions.add(
				new Transition<>(
					nameToStates.get(t.getSource().getState()),
					nameToStates.get(t.getDestination().getState()),
					t.getSymbol()));

		return from(newStates, newTransitions);
	}

	/**
	 * Yields the common alphabet of the automata {@code this} and
	 * {@code other}.
	 * 
	 * @param other the other automaton
	 * 
	 * @return a set of strings representing the common alphabet.
	 */
	public SortedSet<T> commonAlphabet(
			A other) {
		SortedSet<T> fa = getAlphabet();
		SortedSet<T> sa = other.getAlphabet();

		if (fa.containsAll(sa))
			return fa;

		if (sa.containsAll(fa))
			return sa;

		fa.addAll(sa);
		return fa;
	}

	/**
	 * Yields the set of symbols that represents the alphabet of the symbols
	 * recognized by this automaton (that is, as subset of the alphabet over
	 * which the automata is defined).
	 * 
	 * @return the alphabet recognized by this automaton
	 */
	public SortedSet<T> getAlphabet() {
		SortedSet<T> alphabet = new TreeSet<>();

		for (Transition<T> t : transitions)
			alphabet.add(t.getSymbol());

		return alphabet;
	}

	/**
	 * Yields the set of symbols of length at most {@code n} that can be
	 * recognized starting from the given state. In this context, the length of
	 * a symbol is the number of sub-symbols it is composed of, regardless of
	 * the length of the concrete strings it represents. <br>
	 * <br>
	 * For instance, if symbols are regular expressions, given {@code r1 = aab}
	 * and {@code r2 = c*}, {@code r = r1r2} has length 2.
	 * 
	 * @param s the state where to start the inspection
	 * @param n the maximum length of the symbols
	 * 
	 * @return the computed set of symbols
	 */
	public SortedSet<T> getNextSymbols(
			State s,
			int n) {
		SortedSet<T> result = new TreeSet<>();
		if (n == 0)
			return result;

		SortedSet<T> lang = new TreeSet<>();
		WorkingSet<Triple<T, State, Integer>> stack = FIFOWorkingSet.mk();
		stack.push(Triple.of(epsilon(), s, n));

		while (!stack.isEmpty()) {
			Triple<T, State, Integer> top = stack.pop();
			T currentString = top.getLeft();
			Set<T> newChars = getReadableSymbolsFromState(top.getMiddle());
			for (T c : newChars) {
				T newString = concat(currentString, c);
				lang.add(newString);

				if (top.getRight() - 1 > 0) {
					for (State q : getOutgoingTransitionsFrom(top.getMiddle()).stream()
						.filter(t -> t.getSymbol().equals(c))
						.map(Transition::getDestination)
						.collect(Collectors.toSet()))
						stack.push(Triple.of(newString, q, top.getRight() - 1));
				}
			}
		}

		return lang;
	}

	/**
	 * Creates a new automaton that represent the widening operator applied on
	 * the automaton {@code this}.
	 * 
	 * @param n the parameter of the widening operator.
	 * 
	 * @return a newly created automaton representing the widening automaton.
	 */
	public A widening(
			int n) {
		Map<SortedSet<T>, SortedSet<State>> powerStates = new HashMap<>();
		Map<State, SortedSet<T>> languages = new HashMap<>();
		SortedSet<State> newStates = new TreeSet<>();
		Map<SortedSet<State>, State> mapping = new HashMap<>();

		// we partition the states wrt their n-bounded language
		for (State s : states) {
			SortedSet<T> lang = getNextSymbols(s, n);
			languages.put(s, lang);
			powerStates.computeIfAbsent(lang, k -> new TreeSet<>()).add(s);
		}

		int i = 0;
		for (SortedSet<State> ps : powerStates.values()) {
			State ns = new State(i++, containsInitialState(ps), containsFinalState(ps));
			newStates.add(ns);
			mapping.put(ps, ns);
		}

		SortedSet<Transition<T>> newTransitions = new TreeSet<>();
		for (Transition<T> t : transitions) {
			Set<State> fromPartition = powerStates.get(languages.get(t.getSource()));
			Set<State> toPartition = powerStates.get(languages.get(t.getDestination()));
			newTransitions.add(new Transition<>(mapping.get(fromPartition), mapping.get(toPartition), t.getSymbol()));
		}

		A automaton = from(newStates, newTransitions);
		return automaton.minimize();
	}

	@Override
	public String toString() {
		return toRegex().simplify().toString();
	}

	/**
	 * Yields a textual representation of the adjacency matrix of this
	 * automaton.
	 * 
	 * @return a string containing a textual representation of the automaton
	 */
	public String prettyPrint() {
		StringBuilder result = new StringBuilder();

		for (State st : states) {
			SortedSet<Transition<T>> transitions = getOutgoingTransitionsFrom(st);
			if (!transitions.isEmpty() || st.isFinal() || st.isInitial()) {
				result.append(st.getState()).append(" ");
				if (st.isFinal())
					result.append("[accept]");
				else
					result.append("[reject]");

				if (st.isInitial())
					result.append("[initial]");

				result.append("\n");

				for (Transition<T> t : transitions)
					result.append("\t")
						.append(st)
						.append(" [")
						.append(t.getSymbol())
						.append("] -> ")
						.append(t.getDestination())
						.append("\n");
			}
		}

		return result.toString();
	}

	/**
	 * Create a new automaton representing the concatenation of {@code this} and
	 * {@code other}.
	 * 
	 * @param other the other automaton.
	 * 
	 * @return a newly created automaton representing the concatenation of the
	 *             given automata.
	 */
	public A concat(
			A other) {
		SortedSet<State> newStates = new TreeSet<>();
		SortedSet<Transition<T>> newTransitions = new TreeSet<>();

		Map<State, State> thisMapping = new HashMap<>();
		Map<State, State> otherMapping = new HashMap<>();

		SortedSet<State> thisFinalStates = new TreeSet<>();
		SortedSet<State> otherInitialStates = new TreeSet<>();

		int c = 0;
		for (State s : states) {
			State st = new State(c++, s.isInitial(), false);
			thisMapping.put(s, st);
			newStates.add(st);
			if (s.isFinal())
				thisFinalStates.add(st);
		}
		for (State s : other.states) {
			State st = new State(c++, false, s.isFinal());
			otherMapping.put(s, st);
			newStates.add(st);
			if (s.isInitial())
				otherInitialStates.add(st);
		}

		for (Transition<T> t : transitions)
			newTransitions.add(
				new Transition<>(thisMapping.get(t.getSource()), thisMapping.get(t.getDestination()), t.getSymbol()));
		for (Transition<T> t : other.transitions)
			newTransitions.add(
				new Transition<>(otherMapping.get(t.getSource()), otherMapping.get(t.getDestination()), t.getSymbol()));

		for (State f : thisFinalStates)
			for (State i : otherInitialStates)
				newTransitions.add(new Transition<>(f, i, epsilon()));

		return from(newStates, newTransitions).minimize();
	}

	/**
	 * If this automaton has a single initial state, this method returns
	 * {@code this}. Otherwise, it yields a new one where a new unique initial
	 * state has been introduced, connected to the original initial states
	 * through epsilon-transitions.
	 * 
	 * @return an automaton with a single initial state
	 */
	@SuppressWarnings("unchecked")
	public A toSingleInitalState() {
		SortedSet<State> initialStates = getInitialStates();
		if (initialStates.size() < 2)
			return (A) this;

		SortedSet<State> newStates = new TreeSet<>();
		Map<Integer, State> nameToStates = new HashMap<Integer, State>();
		SortedSet<Transition<T>> newDelta = new TreeSet<>();

		int max = -1;
		for (State s : states) {
			State mock = new State(s.getId(), false, s.isFinal());
			newStates.add(mock);
			nameToStates.put(s.getId(), mock);
			max = Math.max(max, s.getId());
		}

		for (Transition<T> t : transitions)
			newDelta.add(
				new Transition<>(
					nameToStates.get(t.getSource().getId()),
					nameToStates.get(t.getDestination().getId()),
					t.getSymbol()));

		State newInit = new State(max + 1, true, false);
		newStates.add(newInit);
		for (State init : initialStates)
			newDelta.add(new Transition<>(newInit, nameToStates.get(init.getId()), epsilon()));

		return from(newStates, newDelta);
	}

	/**
	 * Creates and return the regex that represent the accepted language by the
	 * automaton {@code this}, using Brzozowski algebraic method (see <a href=
	 * "https://cs.stackexchange.com/questions/2016/how-to-convert-finite-automata-to-regular-expressions">here</a>).
	 * Note that the returned regular expression is not simplified.
	 * 
	 * @return a String representing that is the regex that represent the
	 *             accepted language.
	 */
	public RegularExpression toRegex() {
		// this algorithm works only with deterministic automata
		A a = minimize();

		if (a.getInitialStates().isEmpty())
			return EmptySet.INSTANCE;

		// automaton that accepts only the empty string
		if (a.states.size() == 1 && a.transitions.size() == 0)
			return Atom.EPSILON;

		// automaton with one state -> cyclic automaton
		if (a.states.size() == 1) {
			RegularExpression regex = null;
			for (Transition<T> t : a.transitions)
				if (regex == null)
					regex = symbolToRegex(t.getSymbol());
				else
					regex = regex.or(symbolToRegex(t.getSymbol()));
			return regex.star();
		}

		// automaton with only one transition returns the symbol of that
		// transition
		if (a.transitions.size() == 1)
			return symbolToRegex(a.transitions.stream().findFirst().get().getSymbol());

		a = a.toSingleInitalState();
		int m = a.states.size();

		// in both A and B, the initial state is at index 0
		RegularExpression[][] A = new RegularExpression[m][m];
		RegularExpression[] B = new RegularExpression[m];

		Map<Integer, State> mapping = new HashMap<>();
		mapping.put(0, a.states.stream().filter(State::isInitial).findFirst().get());

		// NOTE: algorithm's indexing is 1-based, java is 0-based

		/*
		 * for i = 1 to m: if final(i): B[i] := ε else: B[i] := ∅ for i = 1 to
		 * m: for j = 1 to m: for a in Σ: if trans(i, a, j): A[i,j] := a else:
		 * A[i,j] := ∅
		 */
		Iterator<State> it = a.states.iterator();
		for (int i = 0; i < m; i++) {
			State source = mapping.get(i);
			if (source == null) {
				source = nextNonInitialState(it);
				mapping.put(i, source);
			}

			if (source.isFinal())
				B[i] = Atom.EPSILON;
			else
				B[i] = EmptySet.INSTANCE;

			for (int j = 0; j < m; j++) {
				State dest = mapping.get(j);
				if (dest == null) {
					dest = nextNonInitialState(it);
					mapping.put(j, dest);
				}

				Iterator<Transition<T>> tt = a.getAllTransitionsConnecting(source, dest).iterator();
				if (!tt.hasNext())
					A[i][j] = EmptySet.INSTANCE;
				else {
					A[i][j] = symbolToRegex(tt.next().getSymbol());
					while (tt.hasNext())
						A[i][j] = A[i][j].or(symbolToRegex(tt.next().getSymbol()));
				}
			}
		}

		/*
		 * for n = m decreasing to 1: B[n] := star(A[n,n]) . B[n] for j = 1 to
		 * n: A[n,j] := star(A[n,n]) . A[n,j]; for i = 1 to n: B[i] += A[i,n] .
		 * B[n] for j = 1 to n: A[i,j] += A[i,n] . A[n,j]
		 */
		for (int n = m - 1; n >= 0; n--) {
			RegularExpression star_nn = A[n][n].star();
			B[n] = star_nn.comp(B[n]);

			for (int j = 0; j < n; j++)
				A[n][j] = star_nn.comp(A[n][j]);

			for (int i = 0; i < n; i++) {
				B[i] = B[i].or(A[i][n].comp(B[n]));

				for (int j = 0; j < n; j++)
					A[i][j] = A[i][j].or(A[i][n].comp(A[n][j]));
			}
		}

		return B[0];
	}

	private static State nextNonInitialState(
			Iterator<State> it) {
		while (it.hasNext()) {
			State cursor = it.next();
			if (!cursor.isInitial())
				return cursor;
		}

		return null;
	}

	/**
	 * Yields the set of transitions going from {@code s1} to {@code s2}.
	 * 
	 * @param s1 the source state
	 * @param s2 the destination state
	 * 
	 * @return the set of transitions connecting the two states
	 */
	public SortedSet<Transition<T>> getAllTransitionsConnecting(
			State s1,
			State s2) {
		SortedSet<Transition<T>> result = new TreeSet<>();

		for (Transition<T> t : this.transitions)
			if (t.getSource().equals(s1) && t.getDestination().equals(s2))
				result.add(t);

		return result;
	}

	/**
	 * Creates a new automaton composed of a loop over this one.
	 * 
	 * @return the star automaton
	 */
	public A star() {
		SortedSet<Transition<T>> tr = new TreeSet<>();
		SortedSet<State> st = new TreeSet<>();
		Map<State, State> mapping = new HashMap<>();

		for (State s : states) {
			// initial states become also final
			State q = new State(s.getId(), s.isInitial(), s.isInitial() || s.isFinal());
			st.add(q);
			mapping.put(s, q);
		}

		// create transitions using the new states of the reverse automaton
		for (Transition<T> t : transitions)
			tr.add(new Transition<T>(mapping.get(t.getSource()), mapping.get(t.getDestination()), t.getSymbol()));

		for (State f : getFinalStates())
			for (State i : getInitialStates())
				tr.add(new Transition<>(mapping.get(f), mapping.get(i), epsilon()));

		return from(st, tr);
	}

	/**
	 * Yields {@code true} if and only if the two given states are mutually
	 * reachable, that is, if there exist a path going from {@code s1} to
	 * {@code s2} and one going from {@code s2} to {@code s1}. Paths are
	 * searched using the Dijkstra algorithm.
	 * 
	 * @param s1 the first state
	 * @param s2 the second state
	 * 
	 * @return {@code true} if and only if {@code s1} and {@code s2} are
	 *             mutually reachable
	 */
	public boolean areMutuallyReachable(
			State s1,
			State s2) {
		return !minimumPath(s1, s2).isEmpty() && !minimumPath(s2, s1).isEmpty();
	}

	/**
	 * Yields all possible paths going from an initial state to a final state in
	 * the target automaton. Note that each node of an SCC of the automaton will
	 * appear at most once in each path.
	 * 
	 * @return the set of all possible paths
	 */
	public Set<List<State>> getAllPaths() {
		Set<List<State>> result = new HashSet<>();
		for (State initial : getInitialStates()) {
			Set<Transition<T>[]> paths = depthFirst(initial);

			for (Transition<T>[] tt : paths) {
				List<State> path = new ArrayList<>(tt.length + 1);

				for (Transition<T> t : tt) {
					path.add(t.getSource());

					if (t.getDestination().isFinal())
						path.add(t.getDestination());
				}

				simplify(path);
				result.add(path);
			}
		}

		return result;
	}

	@SuppressWarnings("unchecked")
	private Set<Transition<T>[]> depthFirst(
			State src) {
		Set<Transition<T>[]> paths = new HashSet<>();
		Stack<Triple<State, Transition<T>[], int[]>> ws = new Stack<>();
		ws.push(Triple.of(src, new Transition[0], new int[0]));

		do {
			Triple<State, Transition<T>[], int[]> current = ws.pop();
			State node = current.getLeft();
			Transition<T>[] visited = current.getMiddle();
			int[] hashes = current.getRight();
			int len = visited.length;

			Set<Transition<T>> tr = getOutgoingTransitionsFrom(node);

			transitions: for (Transition<T> t : tr) {
				int thash = t.hashCode();

				int count = 0;
				for (int i = 0; i < len; i++) {
					// we look at the element's hash before invoking the
					// actual comparison for fast failure
					if (visited[i] == t || (hashes[i] == thash && t.equals(visited[i])))
						count++;

					if (count > 1)
						continue transitions;
				}

				Transition<T>[] copy = Arrays.copyOf(visited, len + 1);
				int[] hashesCopy = Arrays.copyOf(hashes, len + 1);
				copy[len] = t;
				hashesCopy[len] = thash;

				if (t.getDestination().isFinal())
					paths.add(copy);
				ws.push(Triple.of(t.getDestination(), copy, hashesCopy));
			}
		} while (!ws.isEmpty());

		return paths;
	}

	private static void simplify(
			List<State> path) {
		ListIterator<State> it = path.listIterator();
		while (it.hasNext()) {
			if (!it.hasPrevious()) {
				it.next();
				continue;
			}

			// we move back one to get the two elements to compare
			it.previous();

			State previous = it.next();
			State current = it.next();
			if (previous.equals(current))
				it.remove();
		}
	}

	/**
	 * Finds the minimum path between the two given states using the Dijkstra
	 * algorithm.
	 * 
	 * @param src    the source node
	 * @param target the destination node
	 * 
	 * @return the minimum path
	 */
	public List<State> minimumPath(
			State src,
			State target) {
		Set<State> unSettledNodes = new HashSet<>();
		Map<State, Integer> distance = new HashMap<>();
		Map<State, State> predecessors = new HashMap<>();

		distance.put(src, 0);
		unSettledNodes.add(src);

		while (unSettledNodes.size() > 0) {
			State node = getMinimum(unSettledNodes, distance);
			unSettledNodes.remove(node);
			findMinimalDistances(node, distance, predecessors, unSettledNodes);
		}

		return getPath(target, predecessors);
	}

	private void findMinimalDistances(
			State node,
			Map<State, Integer> distance,
			Map<State, State> predecessors,
			Set<State> unSettledNodes) {
		Set<State> adjacentNodes = getNextStates(node);
		int shortest = getShortestDistance(node, distance);
		for (State target : adjacentNodes) {
			int dist = getDistance(node, target);
			if (getShortestDistance(target, distance) > shortest + dist) {
				distance.put(target, shortest + dist);
				predecessors.put(target, node);
				unSettledNodes.add(target);
			}
		}
	}

	private int getDistance(
			State node,
			State target) {
		if (!getAllTransitionsConnecting(node, target).isEmpty())
			return 1;
		// should never happen
		return -1;
	}

	private static State getMinimum(
			Set<State> vertexes,
			Map<State, Integer> distance) {
		State minimum = null;
		for (State vertex : vertexes)
			if (minimum == null)
				minimum = vertex;
			else if (getShortestDistance(vertex, distance) < getShortestDistance(minimum, distance))
				minimum = vertex;

		return minimum;
	}

	private static int getShortestDistance(
			State destination,
			Map<State, Integer> distance) {
		Integer d = distance.get(destination);
		if (d == null)
			return Integer.MAX_VALUE;
		else
			return d;
	}

	private static List<State> getPath(
			State target,
			Map<State, State> predecessors) {
		List<State> path = new LinkedList<>();
		State step = target;

		// check if a path exists
		if (predecessors.get(step) == null) {
			path.add(target);
			return path;
		}

		path.add(step);
		while (predecessors.get(step) != null) {
			step = predecessors.get(step);
			path.add(step);
		}

		// Put it into the correct order
		Collections.reverse(path);
		return path;
	}

	/**
	 * Finds the maximum path between the two given states using the Dijkstra
	 * algorithm.
	 * 
	 * @param src    the source node
	 * @param target the destination node
	 * 
	 * @return the maximum path
	 */
	public List<State> maximumPath(
			State src,
			State target) {
		Set<State> unSettledNodes = new HashSet<>();
		Map<State, Integer> distance = new HashMap<>();
		Map<State, State> predecessors = new HashMap<>();

		distance.put(src, 0);
		unSettledNodes.add(src);

		while (unSettledNodes.size() > 0) {
			State node = getMaximum(unSettledNodes, distance);
			unSettledNodes.remove(node);
			findMaximumDistances(node, distance, predecessors, unSettledNodes);
		}

		return getPath(target, predecessors);
	}

	private void findMaximumDistances(
			State node,
			Map<State, Integer> distance,
			Map<State, State> predecessors,
			Set<State> unSettledNodes) {
		Set<State> adjacentNodes = getNextStates(node);
		int longest = getLongestDistance(node, distance);
		for (State target : adjacentNodes) {
			int dist = getDistance(node, target);
			if (getLongestDistance(target, distance) < longest + dist) {
				distance.put(target, longest + dist);
				predecessors.put(target, node);
				unSettledNodes.add(target);
			}
		}
	}

	private static State getMaximum(
			Set<State> vertexes,
			Map<State, Integer> distance) {
		State maximum = null;
		for (State vertex : vertexes)
			if (maximum == null)
				maximum = vertex;
			else if (getLongestDistance(vertex, distance) > getLongestDistance(maximum, distance))
				maximum = vertex;

		return maximum;
	}

	private static int getLongestDistance(
			State destination,
			Map<State, Integer> distance) {
		Integer d = distance.get(destination);
		if (d == null)
			return Integer.MIN_VALUE;
		else
			return d;
	}

	/**
	 * Yields {@code true} if and only if this automaton has only one path. This
	 * means that, after minimization, if the automaton has only one path then
	 * the each state is the starting point of a transition at most once, and it
	 * is the destination node of a transition at most once.
	 * 
	 * @return {@code true} if that condition holds
	 * 
	 * @throws CyclicAutomatonException if the minimized automaton contain loops
	 */
	public boolean hasOnlyOnePath()
			throws CyclicAutomatonException {
		A a = minimize();
		if (a.hasCycle())
			throw new CyclicAutomatonException();

		Set<State> transFrom = new HashSet<>();
		Set<State> transTo = new HashSet<>();
		State from = null;
		State to = null;

		for (Transition<T> t : a.transitions) {
			from = t.getSource();
			if (transFrom.contains(from))
				return false;

			transFrom.add(from);

			to = t.getDestination();
			if (transTo.contains(to))
				return false;

			transTo.add(to);
		}

		return true;
	}

	/**
	 * Yields the sub-automaton contained in this one that recognizes only the
	 * longest string in the language of {@code this}. Note that this method
	 * assumes that the given automaton is loop-free and that it is a
	 * single-path automaton, that is, that all the strings of the language it
	 * recognizes are prefixes of the longest one.
	 * 
	 * @return the sub-automaton
	 */
	public A extractLongestString() {
		State lastFinalState = null;

		for (State finalState : getFinalStates()) {
			Set<Transition<T>> outgoingTransaction = getOutgoingTransitionsFrom(finalState);
			if (outgoingTransaction.isEmpty())
				lastFinalState = finalState;
		}

		// might wannt to throw an exception if lastFinalState is still null
		// or if more than one init state exists

		State nextState = getInitialState();
		List<T> symbols = new LinkedList<>();
		while (!nextState.equals(lastFinalState))
			for (Transition<T> t : transitions)
				if (t.getSource().equals(nextState)) {
					nextState = t.getDestination();
					symbols.add(t.getSymbol());
				}

		SortedSet<State> newStates = new TreeSet<>();
		SortedSet<Transition<T>> newTransitions = new TreeSet<>();
		int counter = 0;
		State last = new State(counter++, true, symbols.isEmpty());
		newStates.add(last);

		for (T symbol : symbols) {
			State tail = new State(counter++, false, counter - 1 == symbols.size());
			newStates.add(tail);
			newTransitions.add(new Transition<>(last, tail, symbol));
			last = tail;
		}
		return from(newStates, newTransitions);
	}

	/**
	 * Yields an acyclic version of this automaton, where all SCCs have been
	 * removed.
	 * 
	 * @return the acyclic automaton
	 */
	public A makeAcyclic() {
		Set<List<State>> paths = getAllPaths();
		paths = paths.stream()
			.filter(p -> p.stream().distinct().collect(Collectors.toList()).equals(p))
			.collect(Collectors.toSet());

		SortedSet<State> states = new TreeSet<>();
		SortedSet<Transition<T>> delta = new TreeSet<>();
		for (List<State> p : paths)
			for (State s : p) {
				states.add(s);
				for (Transition<T> t : getOutgoingTransitionsFrom(s))
					if (p.contains(t.getDestination()) && !states.contains(t.getDestination()))
						// we consider only forward transitions
						// this condition eliminates also self loops
						delta.add(t);
			}

		return from(states, delta);
	}

	/**
	 * Yields the automaton that recognizes all possible substrings of the
	 * strings recognized by this automaton.
	 * 
	 * @return the factors automaton
	 */
	public A factors() {
		SortedSet<State> newStates = new TreeSet<>();
		Map<Integer, State> nameToStates = new HashMap<Integer, State>();
		SortedSet<Transition<T>> newDelta = new TreeSet<>();

		for (State s : states) {
			State mock = new State(s.getId(), true, true);
			newStates.add(mock);
			nameToStates.put(s.getId(), mock);
		}

		for (Transition<T> t : transitions)
			newDelta.add(
				new Transition<>(
					nameToStates.get(t.getSource().getId()),
					nameToStates.get(t.getDestination().getId()),
					t.getSymbol()));

		return from(newStates, newDelta).minimize();
	}

	/**
	 * Yields the automaton that recognizes all possible prefixes of the strings
	 * recognized by this automaton.
	 * 
	 * @return the prefix automaton
	 */
	public A prefix() {
		SortedSet<State> newStates = new TreeSet<>();
		Map<Integer, State> nameToStates = new HashMap<Integer, State>();
		SortedSet<Transition<T>> newDelta = new TreeSet<>();

		for (State s : states) {
			State mock = new State(s.getId(), s.isInitial(), true);
			newStates.add(mock);
			nameToStates.put(s.getId(), mock);
		}

		for (Transition<T> t : transitions)
			newDelta.add(
				new Transition<>(
					nameToStates.get(t.getSource().getId()),
					nameToStates.get(t.getDestination().getId()),
					t.getSymbol()));

		return from(newStates, newDelta).minimize();
	}

	/**
	 * Yields the automaton that recognizes all possible suffixes of the strings
	 * recognized by this automaton.
	 * 
	 * @return the suffix automaton
	 */
	public A suffix() {
		SortedSet<State> newStates = new TreeSet<>();
		Map<Integer, State> nameToStates = new HashMap<Integer, State>();
		SortedSet<Transition<T>> newDelta = new TreeSet<>();

		for (State s : states) {
			State mock = new State(s.getId(), true, s.isFinal());
			newStates.add(mock);
			nameToStates.put(s.getId(), mock);
		}

		for (Transition<T> t : transitions)
			newDelta.add(
				new Transition<>(
					nameToStates.get(t.getSource().getId()),
					nameToStates.get(t.getDestination().getId()),
					t.getSymbol()));

		return from(newStates, newDelta).minimize();
	}

	/**
	 * Yields the length of the longest string recognized by this automaton,
	 * with {@link Integer#MAX_VALUE} representing infinity.
	 * 
	 * @return the length of the longest string
	 */
	public int lengthOfLongestString() {
		Set<List<State>> paths = getAllPaths();
		if (paths.size() == 0)
			return 0;

		int max = Integer.MIN_VALUE, tmp;
		for (List<State> v : paths) {
			tmp = maxStringLengthTraversing(v);
			if (tmp == Integer.MAX_VALUE)
				return tmp;
			else if (tmp > max)
				max = tmp;
		}

		return max;
	}

	private int maxStringLengthTraversing(
			List<State> path) {
		if (path.size() == 0)
			return 0;

		if (path.size() == 1)
			// length of self loops
			return maxStringLength(path.get(0), path.get(0));

		int len = 0;
		for (int i = 0; i < path.size() - 1; i++) {
			int l = maxStringLength(path.get(i), path.get(i + 1));
			if (l == Integer.MAX_VALUE)
				return l;
			len += l;
		}

		return len;
	}

	private int maxStringLength(
			State from,
			State to) {
		Set<Transition<T>> transitions = getAllTransitionsConnecting(from, to);
		if (transitions.isEmpty())
			return 0;

		if (transitions.size() == 1)
			return transitions.iterator().next().getSymbol().maxLength();

		int len = -1;
		for (Transition<T> t : transitions)
			if (len == -1)
				len = t.getSymbol().maxLength();
			else
				len = Math.max(len, t.getSymbol().maxLength());

		return len;
	}

	/**
	 * Yields {@code true} if this automaton recognizes exactly one string, that
	 * is, if it has no loops and all states have at most one outgoing
	 * transition.
	 * 
	 * @return {@code true} if that condition holds
	 */
	public boolean recognizesExactlyOneString() {
		if (hasCycle())
			return false;

		for (State s : getStates())
			if (getOutgoingTransitionsFrom(s).size() > 1)
				return false;

		return true;
	}

	/**
	 * Yields a copy of this automaton, but where all states are final and only
	 * the given state is initial.
	 * 
	 * @param s the state to make the initial state
	 * 
	 * @return the modified copy of this automaton
	 */
	public A factorsChangingInitialState(
			State s) {
		SortedSet<State> newStates = new TreeSet<>();
		Map<Integer, State> nameToStates = new HashMap<Integer, State>();
		SortedSet<Transition<T>> newDelta = new TreeSet<>();

		for (State q : states) {
			State mock = new State(q.getId(), q == s ? true : false, true);
			newStates.add(mock);
			nameToStates.put(q.getId(), mock);
		}

		for (Transition<T> t : transitions)
			newDelta.add(
				new Transition<>(
					nameToStates.get(t.getSource().getId()),
					nameToStates.get(t.getDestination().getId()),
					t.getSymbol()));
		return from(newStates, newDelta).minimize();
	}

	@Override
	public int hashCode() {
		return Objects.hash(states, transitions);
	}

	@Override
	@SuppressWarnings("unchecked")
	public boolean equals(
			Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		A other = (A) obj;
		return Objects.equals(states, other.states) && Objects.equals(transitions, other.transitions);
	}

	/**
	 * Yields the symbol representing the empty string.
	 * 
	 * @return the symbol for epsilon
	 */
	public abstract T epsilon();

	/**
	 * Yields a {@link TransitionSymbol} that is the concatenation of the two
	 * given ones.
	 * 
	 * @param first  the first symbol to concatenate
	 * @param second the second symbol to concatenate
	 * 
	 * @return the result of the concatenation
	 */
	public abstract T concat(
			T first,
			T second);

	/**
	 * Yields a {@link RegularExpression} representing the given symbol.
	 * 
	 * @param symbol the symbol to represent
	 * 
	 * @return the equivalent regular expression
	 */
	public abstract RegularExpression symbolToRegex(
			T symbol);

	/**
	 * Yields the longest common prefix of the strings recognized by this
	 * automaton. If the automaton is not deterministic, it is first
	 * determinized.
	 * 
	 * @return the longest common prefix of the strings recognized by this
	 *             automaton, or the empty string if no such prefix exists
	 */
	public String longestCommonPrefix() {
		if (!isDeterministic())
			return determinize().longestCommonPrefix();

		State current = getInitialState();
		String lcp = "";

		while (current != null) {
			if (current.isFinal())
				return lcp;

			Set<Transition<T>> outgoing = getOutgoingTransitionsFrom(current);
			if (outgoing.size() != 1)
				break;

			Transition<T> dest = outgoing.iterator().next();
			lcp += dest.getSymbol().toString();
			current = dest.getDestination();
		}

		return lcp;
	}

	/**
	 * Yields the interval having as bounds the minimum and maximum length of
	 * the strings recognized by this automaton.
	 * 
	 * @return the length of the strings
	 */
	public IntInterval length() {
		int max = lengthOfLongestString();
		int min = toRegex().minLength();
		return new IntInterval(Integer.valueOf(min), max == Integer.MAX_VALUE ? null : max);
	}

}
