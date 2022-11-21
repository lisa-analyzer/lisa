package it.unive.lisa.analysis.string.fsa;

import it.unive.lisa.analysis.string.fsa.regex.Atom;
import it.unive.lisa.analysis.string.fsa.regex.Comp;
import it.unive.lisa.analysis.string.fsa.regex.EmptySet;
import it.unive.lisa.analysis.string.fsa.regex.Or;
import it.unive.lisa.analysis.string.fsa.regex.RegularExpression;
import it.unive.lisa.analysis.string.fsa.regex.Star;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * A class that describes an generic automaton(dfa, nfa, epsilon nfa).
 *
 * @author <a href="mailto:simone.leoni2@studenti.unipr.it">Simone Leoni</a>
 * @author <a href="mailto:vincenzo.arceri@unipr.it">Vincenzo Arceri</a>
 */
public final class Automaton {

	/**
	 * The states of the automaton
	 */
	private final SortedSet<State> states;

	/**
	 * The transitions of the automaton
	 */
	private final SortedSet<Transition> transitions;

	/**
	 * Set to {@code true} if and only if the automaton is determinized, i.e.,
	 * the method {@link Automaton#determinize} has been called on {@code this}.
	 * This field is not used inside {@link Automaton#equals}.
	 */
	private boolean IS_DETERMINIZED;

	/**
	 * Set to {@code true} if and only if the automaton is minimum, i.e., the
	 * method {@link Automaton#minimize} has been called on {@code this}. This
	 * field is not used inside {@link Automaton#equals}.
	 */
	private boolean IS_MINIMIZED;

	@Override
	public int hashCode() {
		return Objects.hash(states, transitions);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Automaton other = (Automaton) obj;
		return Objects.equals(states, other.states) && Objects.equals(transitions, other.transitions);
	}

	/**
	 * Builds a new automaton with given {@code states} and {@code transitions}.
	 *
	 * @param states      the set of states of the new automaton
	 * @param transitions the set of the transitions of the new automaton
	 */
	public Automaton(SortedSet<State> states, SortedSet<Transition> transitions) {
		this.states = states;
		this.transitions = transitions;
		this.IS_DETERMINIZED = false;
		this.IS_MINIMIZED = false;
	}

	/**
	 * Builds a new automaton that accepts a given string.
	 * 
	 * @param s the only string accepted by the automaton.
	 */
	public Automaton(String s) {
		if (s.isEmpty()) {
			states = new TreeSet<>();
			transitions = new TreeSet<>();
			State last = new State(true, true);
			states.add(last);
		} else {
			states = new TreeSet<>();
			transitions = new TreeSet<>();
			State last = new State(true, false);
			State next;
			states.add(last);
			for (int i = 0; i < s.length(); ++i) {
				if (i != s.length() - 1)
					next = new State(false, false);
				else
					next = new State(false, true);

				transitions.add(new Transition(last, next, "" + s.charAt(i)));
				last = next;
				states.add(last);
			}
		}
	}

	/**
	 * Computes all the automaton transitions to validate a given string
	 * {@code str}.
	 *
	 * @param str String that has to be checked.
	 * 
	 * @return a boolean value that indicates either if {@code str} has been
	 *             accepted or not.
	 */
	public boolean validateString(String str) {
		// stores all the possible states reached by the automaton after each
		// input char
		Set<State> currentStates = epsClosure();

		for (int i = 0; i < str.length(); ++i) {
			String c = "" + str.charAt(i);

			// stores temporally the new currentStates
			Set<State> newCurr = new TreeSet<>();
			for (State s : currentStates) {

				// stores all the states reached after char computation
				Set<State> dest = transitions.stream().filter(t -> t.getSource().equals(s) && t.getSymbol().equals(c))
						.map(Transition::getDestination).collect(Collectors.toSet());
				if (!dest.isEmpty()) {
					dest = epsClosure(dest);
					newCurr.addAll(dest);
				}
			}
			currentStates = newCurr;
		}

		// checks if there is at least one final state in the set of possible
		// reached states at the end of the validation process
		return currentStates.stream().anyMatch(State::isFinal);
	}

	/**
	 * Brzozowski minimization algorithm.
	 *
	 * @return the minimum automaton that accepts the same language as
	 *             {@code this}.
	 */
	public Automaton minimize() {
		if (IS_MINIMIZED)
			return this;
		Automaton min = reverse().determinize().reach().reverse().determinize().reach();
		min.IS_MINIMIZED = true;
		min.IS_DETERMINIZED = true;
		return min;
	}

	/**
	 * Remove all the unreachable states from the current automaton.
	 *
	 * @return a newly created automaton without the unreachable states of
	 *             {@code this}.
	 */
	Automaton reach() {
		// used to store temporarily the states reached from the states in NS
		SortedSet<State> T;
		SortedSet<State> initialStates = epsClosure();
		// stores the reached states of the automaton
		SortedSet<State> RS = new TreeSet<>(initialStates);
		// states that will be checked in the following iteration
		SortedSet<State> NS = new TreeSet<>(initialStates);

		do {
			T = new TreeSet<>();

			for (State q : NS) {
				T.addAll(transitions.stream().filter(t -> t.getSource().equals(q) && !RS.contains(t.getDestination()))
						.map(Transition::getDestination).collect(Collectors.toSet()));
			}
			NS = T;
			RS.addAll(T);
		} while (!NS.isEmpty());
		// add to the new automaton only the transitions between the states of
		// the new Automaton
		SortedSet<Transition> tr = transitions;
		tr.removeIf(t -> !RS.contains(t.getSource()) || !RS.contains(t.getDestination()));

		return new Automaton(RS, tr);
	}

	/**
	 * Creates an automaton that accept the reverse language.
	 *
	 * @return a newly created automaton that accepts the reverse language of
	 *             {@code this}.
	 */
	Automaton reverse() {
		SortedSet<Transition> tr = new TreeSet<>();
		SortedSet<State> st = new TreeSet<>();
		// used to associate states of the Automaton this to the reverse one
		Map<State, State> revStates = new HashMap<>();

		for (State s : states) {
			boolean fin = false, init = false;
			if (s.isInitial())
				fin = true;
			if (s.isFinal())
				init = true;
			State q = new State(init, fin);
			st.add(q);
			// add association between the newly created state and the state of
			// the automaton this
			revStates.put(s, q);
		}

		// create transitions using the new states of the reverse automaton
		for (Transition t : transitions)
			tr.add(new Transition(revStates.get(t.getDestination()), revStates.get(t.getSource()), t.getSymbol()));

		return new Automaton(st, tr);
	}

	/**
	 * Creates a deterministic automaton starting from {@code this}.
	 *
	 * @return a newly deterministic automaton that accepts the same language as
	 *             {@code this}.
	 */
	Automaton determinize() {
		if (IS_DETERMINIZED)
			return this;

		// transitions of the new deterministic automaton
		SortedSet<Transition> delta = new TreeSet<>();
		// states of the new deterministic automaton
		SortedSet<State> sts = new TreeSet<>();
		// store the macrostates of the new Automaton
		List<SortedSet<State>> detStates = new ArrayList<>();
		// used to map the macrostate with the corresponding state of the new
		// automaton
		Map<State, SortedSet<State>> stateToMacro = new HashMap<>();
		Map<SortedSet<State>, State> macroToState = new HashMap<>();
		// stores the already controlled states
		Set<State> marked = new TreeSet<>();
		// automaton alphabet
		Set<String> alphabet = transitions.stream().map(Transition::getSymbol).filter(s -> !s.equals(""))
				.collect(Collectors.toSet());

		// the first macrostate is the one associated with the epsilon closure
		// of the initial states
		SortedSet<State> initialStates = epsClosure();
		detStates.add(initialStates);
		State q = null;
		for (State s : initialStates)
			if (s.isFinal()) {
				q = new State(true, true);
				break;
			}
		if (q == null)
			q = new State(true, false);
		sts.add(q);
		stateToMacro.put(q, initialStates);
		macroToState.put(initialStates, q);
		// used to keep track of current state
		State current = q;

		// iterate until all the states have been checked
		while (!marked.equals(sts)) {
			for (State s : sts) {
				// get the first state that has not been checked yet
				if (!marked.contains(s)) {
					marked.add(s);
					current = s;
					break;
				}
			}
			// macrostate corresponding to the current state
			Set<State> currStates = stateToMacro.get(current);
			// find all the destination states of any non epsilon transaction
			// starting from a current state
			for (String c : alphabet) {
				Set<State> R = epsClosure(transitions.stream().filter(
						t -> currStates.contains(t.getSource()) && t.getSymbol().equals(c) && !t.getSymbol().equals(""))
						.map(Transition::getDestination).collect(Collectors.toSet()));
				// add R to detStates only if it is a new macrostate
				if (!detStates.contains(R) && !R.isEmpty()) {
					TreeSet<State> currentStates = (TreeSet<State>) R;
					detStates.add(currentStates);
					State nq = null;
					// make nq final if any of the state in the correspondent
					// macrostate is final
					for (State s : R)
						if (s.isFinal()) {
							nq = new State(false, true);
							break;
						}
					if (nq == null)
						nq = new State(false, false);
					sts.add(nq);
					stateToMacro.put(nq, currentStates);
					macroToState.put(currentStates, nq);
				}
				// add transition from currStates macrostate to R that is
				// destination macrostate
				if (!R.isEmpty())
					delta.add(new Transition(macroToState.get(currStates), macroToState.get(R), c));
			}
		}

		Automaton det = new Automaton(sts, delta);
		det.IS_DETERMINIZED = true;
		return det;
	}

	/**
	 * Computes the epsilon closure of this automaton starting from its initial
	 * states, namely the set of states that are reachable from all the initial
	 * states just with epsilon transitions.
	 *
	 * @return the set of states that are reachable from all the initial states
	 *             just with epsilon transitions.
	 */
	SortedSet<State> epsClosure() {
		return epsClosure(states.stream().filter(State::isInitial).collect(Collectors.toSet()));
	}

	/**
	 * Computes the epsilon closure of this automaton starting from
	 * {@code state}, namely the set of states that are reachable from
	 * {@code state} just with epsilon transitions.
	 *
	 * @param state the state from which the method starts to compute the
	 *                  epsilon closure
	 * 
	 * @return the set of states that are reachable from {@code state} just with
	 *             epsilon transitions.
	 */
	Set<State> epsClosure(State state) {
		Set<State> eps = new TreeSet<>();
		eps.add(state);
		// used to make sure that a state isn't checked twice
		Set<State> checked = new TreeSet<>();

		// add current state
		do {
			// used to collect new states that have to be added to eps inside
			// for
			// loop
			Set<State> temp = new TreeSet<>();
			for (State s : eps) {
				if (!checked.contains(s)) {
					checked.add(s);

					// collect all the possible destination from the current
					// state
					Set<State> dest = transitions.stream()
							.filter(t -> t.getSource().equals(s) && t.getSymbol().equals(""))
							.map(Transition::getDestination).collect(Collectors.toSet());

					temp.addAll(dest);
				}
			}

			eps.addAll(temp);

		} while (!checked.containsAll(eps));

		return eps;
	}

	/**
	 * Computes the epsilon closure of this automaton starting from
	 * {@code state}, namely the set of states that are reachable from
	 * {@code st} just with epsilon transitions.
	 *
	 * @param st the set of states from which the epsilon closure is computed.
	 * 
	 * @return the set of states that are reachable from {@code state} just with
	 *             epsilon transitions.
	 */
	private SortedSet<State> epsClosure(Set<State> st) {
		SortedSet<State> eps = new TreeSet<>();

		for (State s : st)
			eps.addAll(epsClosure(s));

		return eps;
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

	public Automaton union(Automaton other) {
		if (this == other)
			return this;

		SortedSet<State> sts = new TreeSet<>();
		SortedSet<Transition> ts = new TreeSet<>();

		Map<State, State> thisInitMapping = new HashMap<>();
		Map<State, State> otherInitMapping = new HashMap<>();

		Map<State, State> thisMapping = new HashMap<>();
		Map<State, State> otherMapping = new HashMap<>();

		for (State s : states) {
			State st = new State(false, s.isFinal());
			thisMapping.put(s, st);
			if (s.isInitial()) {
				sts.add(st);
				thisInitMapping.put(s, st);
			} else
				sts.add(st);
		}

		for (State s : other.states) {
			State st = new State(false, s.isFinal());
			otherMapping.put(s, st);
			if (s.isInitial()) {
				sts.add(st);
				otherInitMapping.put(s, st);
			} else {
				sts.add(st);
			}
		}

		State q0 = new State(true, false);
		sts.add(q0);

		for (State s : sts)
			if (thisInitMapping.containsValue(s) || otherInitMapping.containsValue(s))
				ts.add(new Transition(q0, s, ""));

		for (Transition t : transitions)
			ts.add(new Transition(thisMapping.get(t.getSource()), thisMapping.get(t.getDestination()), t.getSymbol()));

		for (Transition t : other.transitions)
			ts.add(new Transition(otherMapping.get(t.getSource()), otherMapping.get(t.getDestination()),
					t.getSymbol()));

		Automaton result = new Automaton(sts, ts);
		result.IS_DETERMINIZED = false;
		result.IS_MINIMIZED = false;
		return result;
	}

	/**
	 * Returns a set of string containing all the strings accepted by
	 * {@code this} of length from 1 to {@code length}.
	 *
	 * @param length the maximum length of the strings to be returned
	 * 
	 * @return a set containing the subset of strings accepted by {@code this}
	 */
	public Set<String> getLanguageAtMost(int length) {
		Set<String> lang = new TreeSet<>();
		Set<State> initialStates = epsClosure();

		for (State s : initialStates)
			lang.addAll(getLanguageAtMost(s, length));

		return lang;
	}

	/**
	 * Returns a set of string containing all the string accepted by
	 * {@code this} of length from 1 to {@code length} from a given state.
	 *
	 * @param q      state from which the strings are computed
	 * @param length maximum length of the computed strings
	 * 
	 * @return a set containing a subset of strings accepted by {@code this}
	 *             starting from the state {@code q} of maximum length
	 *             {@code length}.
	 */
	public SortedSet<String> getLanguageAtMost(State q, int length) {

		if (length == 0)
			return new TreeSet<>();

		// the set representing the accepted language
		SortedSet<String> lang = new TreeSet<>();
		lang.add("");
		// used to keep track of every single possible path
		LinkedList<AbstractMap.SimpleImmutableEntry<String, State>> stack = new LinkedList<>();
		// add all initialStates to the stack
		stack.addFirst(new AbstractMap.SimpleImmutableEntry<>("", q));

		while (!stack.isEmpty()) {
			AbstractMap.SimpleImmutableEntry<String, State> top = stack.removeFirst();
			String currentString = top.getKey();
			Set<String> newChars = transitions.stream().filter(t -> t.getSource().equals(top.getValue()))
					.map(Transition::getSymbol).collect(Collectors.toSet());
			for (String c : newChars) {
				String newString = currentString + c;
				lang.add(newString);

				if (newString.length() < length) {
					for (State s : getOutgoingTranstionsFrom(top.getValue()).stream()
							.filter(t -> t.getSymbol().equals(c)).map(Transition::getDestination)
							.collect(Collectors.toSet()))
						stack.add(new AbstractMap.SimpleImmutableEntry<>(newString, s));
				}
			}
		}

		return lang;

	}

	/**
	 * Returns all the outgoing transitions from a given state.
	 *
	 * @param q the source of the transitions.
	 * 
	 * @return a Set containing all the outgoing transitions from the state q.
	 */
	private Set<Transition> getOutgoingTranstionsFrom(State q) {
		return transitions.stream().filter(t -> t.getSource().equals(q)).collect(Collectors.toSet());
	}

	/**
	 * Checks if the Automaton {@code this} has any cycle.
	 *
	 * @return a boolean value that tells if {@code this} has any cycle.
	 */
	boolean hasCycle() {
		if (states.size() == 1) {
			for (Transition t : transitions) {
				if (t.getSource() == t.getDestination())
					return true;
			}
		}
		// visit the automaton to check if there is any cycle
		Set<State> currentStates = states.stream().filter(State::isInitial).collect(Collectors.toSet());
		Set<State> visited = new TreeSet<>();
		while (!visited.containsAll(states)) {
			Set<State> temp = new TreeSet<>();
			for (State s : currentStates) {
				temp.addAll(transitions.stream().filter(t -> t.getSource().equals(s)).map(Transition::getDestination)
						.collect(Collectors.toSet()));
			}
			for (State s : temp)
				if (visited.contains(s))
					return true;
			visited.addAll(currentStates);
			currentStates = temp;
		}

		return false;
	}

	/**
	 * Returns the language accepted by {@code this}.
	 * 
	 * @return a set representing the language accepted by the Automaton
	 *             {@code this}.
	 * 
	 * @throws CyclicAutomatonException thrown if the automaton is cyclic.
	 */
	public Set<String> getLanguage() throws CyclicAutomatonException {
		Set<String> lang = new TreeSet<>();
		if (hasCycle())
			throw new CyclicAutomatonException();

		// stack used to keep track of transitions that will be "visited"
		// each element is a pair to keep track of old String and next
		// Transition
		LinkedList<AbstractMap.SimpleImmutableEntry<String, Transition>> stack = new LinkedList<>();
		Set<State> initialStates = states.stream().filter(State::isInitial).collect(Collectors.toSet());
		// add initial states transitions to stack
		for (State q : initialStates) {
			for (Transition t : getOutgoingTranstionsFrom(q)) {
				stack.addFirst(new AbstractMap.SimpleImmutableEntry<>("", t));
			}
		}
		// generate all the strings and add them to lang
		while (!stack.isEmpty()) {
			AbstractMap.SimpleImmutableEntry<String, Transition> top = stack.removeFirst();
			String currentString = top.getKey();
			Transition tr = top.getValue();
			// when it finds a final state it adds the generated string to the
			// language
			if (tr.getDestination().isFinal())
				lang.add(currentString + tr.getSymbol());
			// adds all the possible path from current transition destination to
			// the stack
			for (Transition t : getOutgoingTranstionsFrom(tr.getDestination()))
				stack.addFirst(new AbstractMap.SimpleImmutableEntry<>(currentString + tr.getSymbol(), t));
		}

		return lang;
	}

	/**
	 * Creates a new {@code Automaton} that is the same as {@code this} but is
	 * complete.
	 */
	private Automaton complete(Set<String> sigma) {
		SortedSet<State> newStates = new TreeSet<>(states);
		SortedSet<Transition> newTransitions = new TreeSet<>(transitions);
		// add a new "garbage" state
		State garbage = new State(false, false);
		newStates.add(garbage);

		// adds all the transitions to the garbage state
		for (State s : newStates)
			for (String c : sigma)
				if (newTransitions.stream().filter(t -> t.getSymbol().equals(c) && t.getSource().equals(s))
						.collect(Collectors.toSet()).isEmpty())
					newTransitions.add(new Transition(s, garbage, c));
		return new Automaton(newStates, newTransitions);
	}

	/**
	 * Return a new Automaton that accept a language that is the complementary
	 * language of {@code this}.
	 * 
	 * @param sigma the alphabet used for automaton completion.
	 * 
	 * @return the complement Automaton of {@code this}.
	 */
	public Automaton complement(Set<String> sigma) {
		// states and transitions for the newly created automaton
		SortedSet<State> sts = new TreeSet<>();
		SortedSet<Transition> delta = new TreeSet<>();
		// keep track of the corresponding newly created states
		Map<State, State> oldToNew = new HashMap<>();
		Automaton r = this.determinize().complete(sigma);

		// creates all the new states
		for (State s : r.states) {
			// make final as non final and non final as final
			State q = new State(s.isInitial(), !s.isFinal());
			sts.add(q);
			oldToNew.put(s, q);
		}
		// creates all the new transitions
		for (Transition t : r.transitions)
			delta.add(new Transition(oldToNew.get(t.getSource()), oldToNew.get(t.getDestination()), t.getSymbol()));

		return new Automaton(sts, delta).minimize();
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
	public Automaton intersection(Automaton other) {
		if (this == other)
			return this;
		Set<String> sigma = commonAlphabet(other);
		// De Morgan's rule A && B = ¬(¬A || ¬B)
		return complement(sigma).union(other.complement(sigma)).minimize().complement(sigma);
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
	 * Checks if the automaton {@code this} contains the automaton
	 * {@code other}.
	 * 
	 * @param other the other automaton
	 * 
	 * @return a boolean value that points out if the automaton is contained or
	 *             not
	 */
	public boolean isContained(Automaton other) {
		Automaton intersection = intersection(other.complement(commonAlphabet(other))).minimize();
		return intersection.acceptsEmptyLanguage();
	}

	/**
	 * Checks if the automaton {@code this} accepts the same language as
	 * {@code other}.
	 * 
	 * @param other the other automaton
	 * 
	 * @return a boolean value that points out if the automata are equivalent
	 */
	public boolean isEqual(Automaton other) {
		if (!isContained(other))
			return false;
		return other.isContained(this);
	}

	/**
	 * Yields the common alphabet of the automata {@code this} and
	 * {@code other}.
	 * 
	 * @param other the other automaton
	 * 
	 * @return a set of strings representing the common alphabet.
	 */
	Set<String> commonAlphabet(Automaton other) {
		Set<String> result = new TreeSet<>();
		result.addAll(transitions.stream().map(Transition::getSymbol).collect(Collectors.toSet()));
		result.addAll(other.transitions.stream().map(Transition::getSymbol).collect(Collectors.toSet()));

		// remove the empty string
		result.remove("");
		return result;
	}

	/**
	 * Creates a new automaton that represent the widening operator applied on
	 * the automaton {@code this}.
	 * 
	 * @param n the parameter of the widening operator.
	 * 
	 * @return a newly created automaton representing the widening automaton.
	 */
	public Automaton widening(int n) {
		SortedSet<State> newStates = new TreeSet<>();
		// stores all the powerstates - note that this does not need to be
		// sorted: this is used only to iterate and produce a transition
		// that is added to sorted set, and thus the order in which those are
		// created does not matter
		Set<SortedSet<State>> powerStates = new HashSet<>();
		// used to store a mapping between the powerstate and the new state
		Map<SortedSet<State>, State> powerToNew = new HashMap<>();
		// used to store languages to improve performance
		Map<State, SortedSet<String>> languages = new HashMap<>();
		// generate all the languages for the states
		for (State s : states)
			languages.put(s, getLanguageAtMost(s, n));

		// create the new states for the new automaton
		for (State s : states) {
			SortedSet<State> ps = new TreeSet<>();
			ps.add(s);
			for (State q : states)
				if (!q.equals(s) && languages.get(s).equals(languages.get(q)))
					ps.add(q);

			boolean isInitial = false, isFinal = false;
			for (State q : ps) {
				if (q.isInitial())
					isInitial = true;
				if (q.isFinal())
					isFinal = true;
			}

			State ns = new State(isInitial, isFinal);
			powerStates.add(ps);
			powerToNew.put(ps, ns);
			newStates.add(ns);
		}

		// add transitions between the new states
		SortedSet<Transition> newTransitions = new TreeSet<>();
		for (Transition t : transitions)
			for (SortedSet<State> ps : powerStates)
				if (ps.contains(t.getSource()))
					for (SortedSet<State> psd : powerStates)
						if (psd.contains(t.getDestination()))
							newTransitions.add(new Transition(powerToNew.get(ps), powerToNew.get(psd), t.getSymbol()));

		Automaton automaton = new Automaton(newStates, newTransitions);
		return automaton.minimize();
	}

	@Override
	public String toString() {
		return toRegex();
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
	public Automaton concat(Automaton other) {
		SortedSet<State> newStates = new TreeSet<>();
		SortedSet<Transition> newTransitions = new TreeSet<>();
		Map<State, State> oldToNew = new HashMap<>();

		Set<State> thisFinalStates = states.stream().filter(State::isFinal).collect(Collectors.toSet());

		Set<State> otherInitialStates = other.states.stream().filter(State::isInitial).collect(Collectors.toSet());

		for (State s : states) {
			if (!s.isFinal())
				newStates.add(s);
			else {
				State q = new State(s.isInitial(), false);
				oldToNew.put(s, q);
				newStates.add(q);
			}
		}
		for (State s : other.states) {
			if (!s.isInitial())
				newStates.add(s);
			else {
				State q = new State(false, s.isFinal());
				oldToNew.put(s, q);
				newStates.add(q);
			}
		}

		for (Transition t : transitions) {
			State source = t.getSource();
			State dest = t.getDestination();
			if (thisFinalStates.contains(t.getSource()))
				source = oldToNew.get(t.getSource());
			if (thisFinalStates.contains(t.getDestination()))
				dest = oldToNew.get(t.getDestination());
			newTransitions.add(new Transition(source, dest, t.getSymbol()));
		}
		for (Transition t : other.transitions) {
			State source = t.getSource();
			State dest = t.getDestination();
			if (otherInitialStates.contains(t.getSource()))
				source = oldToNew.get(t.getSource());
			if (otherInitialStates.contains(t.getDestination()))
				dest = oldToNew.get(t.getDestination());
			newTransitions.add(new Transition(source, dest, t.getSymbol()));
		}

		for (State f : thisFinalStates)
			for (State i : otherInitialStates)
				newTransitions.add(new Transition(oldToNew.get(f), oldToNew.get(i), ""));

		return new Automaton(newStates, newTransitions).minimize();
	}

	/**
	 * If this automaton has a single initial state, this method returns
	 * {@code this}. Otherwise,it yields a new one where a new unique initial
	 * state has been introduced, connected to the original initial states
	 * through epsilon-transitions.
	 * 
	 * @return an automaton with a single initial state
	 */
	public Automaton toSingleInitalState() {
		SortedSet<State> initialStates = new TreeSet<>();
		for (State s : states)
			if (s.isInitial())
				initialStates.add(s);

		if (initialStates.size() < 2)
			return this;

		SortedSet<State> newStates = new TreeSet<>();
		Map<Integer, State> nameToStates = new HashMap<Integer, State>();
		SortedSet<Transition> newDelta = new TreeSet<>();

		for (State s : states) {
			State mock = new State(false, s.isFinal());
			newStates.add(mock);
			nameToStates.put(s.getId(), mock);
		}

		for (Transition t : transitions)
			newDelta.add(new Transition(nameToStates.get(t.getSource().getId()),
					nameToStates.get(t.getDestination().getId()), t.getSymbol()));

		State newInit = new State(true, false);
		for (State init : initialStates)
			newDelta.add(new Transition(newInit, nameToStates.get(init.getId()), ""));

		return new Automaton(newStates, newDelta);
	}

	/**
	 * Creates and return the regex that represent the accepted language by the
	 * automaton {@code this}, using Brzozowski algebraic method (see <a href=
	 * "https://cs.stackexchange.com/questions/2016/how-to-convert-finite-automata-to-regular-expressions">here</a>).
	 * 
	 * @return a String representing that is the regex that represent the
	 *             accepted language.
	 */
	public String toRegex() {
		// this algorithm works only with deterministic automata
		Automaton a = this.minimize();

		// automaton that accepts only the empty string
		if (a.states.size() == 1 && a.transitions.size() == 0)
			return "";

		// automaton with one state -> cyclic automaton
		if (a.states.size() == 1) {
			RegularExpression regex = null;
			for (Transition t : a.transitions)
				if (regex == null)
					regex = new Atom(t.getSymbol());
				else
					regex = new Or(regex, new Atom(t.getSymbol()));
			return new Star(regex).toString();
		}

		// automaton with only one transition returns the symbol of that
		// transition
		if (a.transitions.size() == 1)
			return a.transitions.stream().findFirst().get().getSymbol();

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

				Iterator<Transition> tt = a.getAllTransitionsConnecting(source, dest).iterator();
				if (!tt.hasNext())
					A[i][j] = EmptySet.INSTANCE;
				else {
					A[i][j] = new Atom(tt.next().getSymbol());
					while (tt.hasNext())
						A[i][j] = new Or(A[i][j], new Atom(tt.next().getSymbol()));
				}
			}
		}

		/*
		 * for n = m decreasing to 1: B[n] := star(A[n,n]) . B[n] for j = 1 to
		 * n: A[n,j] := star(A[n,n]) . A[n,j]; for i = 1 to n: B[i] += A[i,n] .
		 * B[n] for j = 1 to n: A[i,j] += A[i,n] . A[n,j]
		 */
		for (int n = m - 1; n >= 0; n--) {
			Star star_nn = new Star(A[n][n]);
			B[n] = new Comp(star_nn, B[n]);

			for (int j = 0; j < n; j++)
				A[n][j] = new Comp(star_nn, A[n][j]);

			for (int i = 0; i < n; i++) {
				B[i] = new Or(B[i], new Comp(A[i][n], B[n]));

				for (int j = 0; j < n; j++)
					A[i][j] = new Or(A[i][j], new Comp(A[i][n], A[n][j]));
			}
		}

		return B[0].simplify().toString();
	}

	private static State nextNonInitialState(Iterator<State> it) {
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
	public Set<Transition> getAllTransitionsConnecting(State s1, State s2) {
		Set<Transition> result = new TreeSet<>();

		for (Transition t : this.transitions)
			if (t.getSource().equals(s1) && t.getDestination().equals(s2))
				result.add(t);

		return result;
	}
}