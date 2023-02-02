package it.unive.lisa.analysis.string.fsa;

import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticDomain.Satisfiability;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.nonrelational.value.BaseNonRelationalValueDomain;
import it.unive.lisa.analysis.representation.DomainRepresentation;
import it.unive.lisa.analysis.representation.StringRepresentation;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.symbolic.value.operator.binary.StringConcat;
import it.unive.lisa.symbolic.value.operator.binary.StringContains;
import it.unive.lisa.util.datastructures.automaton.CyclicAutomatonException;
import it.unive.lisa.util.datastructures.automaton.State;
import it.unive.lisa.util.numeric.IntInterval;
import it.unive.lisa.util.numeric.MathNumber;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * A class that represent the Finite State Automaton domain for strings,
 * exploiting a {@link SimpleAutomaton}.
 *
 * @author <a href="mailto:simone.leoni2@studenti.unipr.it">Simone Leoni</a>
 * @author <a href="mailto:vincenzo.arceri@unipr.it">Vincenzo Arceri</a>
 */
public class FSA implements BaseNonRelationalValueDomain<FSA> {

	/**
	 * Top element of the domain
	 */
	private static final FSA TOP = new FSA(new SimpleAutomaton("").unknownString());

	/**
	 * The parameter used for the widening operator.
	 */
	public static final int WIDENING_TH = 3;

	/**
	 * Used to store the string representation
	 */
	private final SimpleAutomaton a;

	/**
	 * Creates a new FSA object representing the TOP element.
	 */
	public FSA() {
		// we use the empty language as it is memory-efficient
		this.a = new SimpleAutomaton("").emptyLanguage();
	}

	/**
	 * Creates a new FSA object using a {@link SimpleAutomaton}.
	 * 
	 * @param a the {@link SimpleAutomaton} used for object construction.
	 */
	FSA(SimpleAutomaton a) {
		this.a = a;
	}

	@Override
	public FSA lubAux(FSA other) throws SemanticException {
		return new FSA(this.a.union(other.a).minimize());
	}

	@Override
	public FSA wideningAux(FSA other) throws SemanticException {
		return new FSA(this.a.union(other.a).widening(WIDENING_TH));
	}

	@Override
	public boolean lessOrEqualAux(FSA other) throws SemanticException {
		return this.a.isContained(other.a);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		FSA fsa = (FSA) o;
		return Objects.equals(a, fsa.a);
	}

	@Override
	public int hashCode() {
		return Objects.hash(a);
	}

	@Override
	public FSA top() {
		return TOP;
	}

	@Override
	public boolean isBottom() {
		return !isTop() && this.a.acceptsEmptyLanguage();
	}

	@Override
	public FSA bottom() {
		SortedSet<State> states = new TreeSet<>();
		states.add(new State(0, true, false));
		return new FSA(new SimpleAutomaton(states, new TreeSet<>()));
	}

	@Override
	public DomainRepresentation representation() {
		if (isBottom())
			return Lattice.bottomRepresentation();
		else if (isTop())
			return Lattice.topRepresentation();

		return new StringRepresentation(this.a.toRegex());
	}

	@Override
	public FSA evalNonNullConstant(Constant constant, ProgramPoint pp) throws SemanticException {
		if (constant.getValue() instanceof String) {
			return new FSA(new SimpleAutomaton((String) constant.getValue()));
		}
		return top();
	}

	// TODO unary and ternary and all other binary
	@Override
	public FSA evalBinaryExpression(BinaryOperator operator, FSA left, FSA right, ProgramPoint pp)
			throws SemanticException {
		if (operator == StringConcat.INSTANCE)
			return new FSA(left.a.concat(right.a));
		return top();
	}

	@Override
	public Satisfiability satisfiesBinaryExpression(BinaryOperator operator, FSA left, FSA right,
			ProgramPoint pp) throws SemanticException {
		if (operator == StringContains.INSTANCE) {
			try {
				Set<String> rightLang = right.a.getLanguage();
				Set<String> leftLang = left.a.getLanguage();
				// right accepts only the empty string
				if (rightLang.size() == 1 && rightLang.contains(""))
					return Satisfiability.SATISFIED;

				// we can compare languages
				boolean atLeastOne = false, all = true;
				for (String a : leftLang)
					for (String b : rightLang) {
						boolean cont = a.contains(b);
						atLeastOne = atLeastOne || cont;
						all = all && cont;
					}

				if (all)
					return Satisfiability.SATISFIED;
				if (atLeastOne)
					return Satisfiability.UNKNOWN;
				return Satisfiability.NOT_SATISFIED;
			} catch (CyclicAutomatonException e) {
				return Satisfiability.UNKNOWN;
			}
		}
		return Satisfiability.UNKNOWN;
	}

	/**
	 * Yields the FSA automaton corresponding to the substring of this FSA
	 * automaton abstract value between two indexes.
	 * 
	 * @param begin where the substring starts
	 * @param end   where the substring ends
	 * 
	 * @return the FSA automaton corresponding to the substring of this FSA
	 *             automaton between two indexes
	 * 
	 * @throws CyclicAutomatonException when the automaton is cyclic and its
	 *                                      language is accessed
	 */
	public FSA substring(long begin, long end) throws CyclicAutomatonException {
		if (isTop() || isBottom())
			return this;

		if (!a.hasCycle()) {
			SimpleAutomaton result = this.a.emptyLanguage();
			for (String s : a.getLanguage()) {
				result = result.union(new SimpleAutomaton(s.substring((int) begin, (int) end)));

				return new FSA(result);
			}
		}

		SimpleAutomaton[] array = this.a.toRegex().substring((int) begin, (int) end)
				.parallelStream()
				.map(s -> new SimpleAutomaton(s.toString())).toArray(SimpleAutomaton[]::new);

		SimpleAutomaton result = this.a.emptyLanguage();

		for (int i = 0; i < array.length; i++)
			result = result.union(array[i]);
		return new FSA(result);
	}

	/**
	 * Yields the {@link IntInterval} containing the minimum and maximum length
	 * of this abstract value.
	 * 
	 * @return the minimum and maximum length of this abstract value
	 */
	public IntInterval length() {
		return new IntInterval(a.toRegex().minLength(), a.lenghtOfLongestString());
	}

	/**
	 * Yields the {@link IntInterval} containing the minimum and maximum index
	 * of {@code s} in {@code this}.
	 *
	 * @param s the string to be searched
	 * 
	 * @return the minimum and maximum index of {@code s} in {@code this}
	 * 
	 * @throws CyclicAutomatonException when the automaton is cyclic and its
	 *                                      language is accessed
	 */
	public IntInterval indexOf(FSA s) throws CyclicAutomatonException {
		if (a.hasCycle())
			return mkInterval(-1, null);

		if (!a.hasCycle() && !s.a.hasCycle()) {
			Set<String> first = a.getLanguage();
			Set<String> second = s.a.getLanguage();

			IntInterval result = null;
			for (String f1 : first) {
				for (String f2 : second) {
					IntInterval partial;

					if (f1.contains(f2)) {
						int i = f1.indexOf(f2);
						partial = mkInterval(i, i);
					} else {
						partial = mkInterval(-1, -1);
					}
					result = result == null ? partial : mkInterval(partial, result);
				}
			}

			return result;
		}

		HashSet<Integer> indexesOf = new HashSet<>();
		for (State q : a.getStates()) {
			SimpleAutomaton build = a.factorsChangingInitialState(q);
			if (!build.intersection(s.a).acceptsEmptyLanguage())
				indexesOf.add(a.maximumPath(q, q).size() - 1);
		}

		// No state in the automaton can read search
		if (indexesOf.isEmpty())
			return mkInterval(-1, -1);
		else if (s.a.recognizesExactlyOneString() && a.recognizesExactlyOneString())
			return mkInterval(indexesOf.stream().mapToInt(i -> i).min().getAsInt(),
					indexesOf.stream().mapToInt(i -> i).max().getAsInt());
		else
			return mkInterval(-1, indexesOf.stream().mapToInt(i -> i).max().getAsInt());
	}

	private IntInterval mkInterval(Integer min, Integer max) {
		return new IntInterval(min, max);
	}

	private IntInterval mkInterval(IntInterval first, IntInterval second) {
		MathNumber newLow = first.getLow().min(second.getLow());
		MathNumber newHigh = first.getHigh().max(second.getHigh());
		return new IntInterval(newLow, newHigh);
	}
}
