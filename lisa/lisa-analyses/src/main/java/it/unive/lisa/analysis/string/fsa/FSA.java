package it.unive.lisa.analysis.string.fsa;

import it.unive.lisa.analysis.SemanticDomain;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.nonrelational.value.BaseNonRelationalValueDomain;
import it.unive.lisa.analysis.representation.DomainRepresentation;
import it.unive.lisa.analysis.representation.StringRepresentation;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.symbolic.value.operator.binary.StringConcat;
import it.unive.lisa.symbolic.value.operator.binary.StringContains;
import it.unive.lisa.symbolic.value.operator.ternary.TernaryOperator;
import it.unive.lisa.symbolic.value.operator.unary.UnaryOperator;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * A class that represent the Finite State Automaton domain for strings.
 *
 * @author <a href="mailto:simone.leoni2@studenti.unipr.it">Simone Leoni</a>
 * @author <a href="mailto:vincenzo.arceri@unipr.it">Vincenzo Arceri</a>
 */
public class FSA extends BaseNonRelationalValueDomain<FSA> {
	/**
	 * Used to store the string representation
	 */
	private final Automaton a;
	/**
	 * Top element of the domain
	 */
	private static final FSA TOP = new FSA();
	/**
	 * The paramater used for the widening operator.
	 */
	public static final int WIDENING_TH = 3;

	/**
	 * Creates a new FSA object representing the TOP element.
	 */
	public FSA() {
		// top
		this.a = new Automaton(null, null);
	}

	/**
	 * Creates a new FSA object using an {@link Automaton}.
	 * 
	 * @param a the {@link Automaton} used for object construction.
	 */
	FSA(Automaton a) {
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
		states.add(new State(true, false));
		return new FSA(new Automaton(states, new TreeSet<>()));
	}

	@Override
	public DomainRepresentation representation() {
		if (isBottom())
			return new StringRepresentation("no string");
		else if (isTop())
			return new StringRepresentation("any string");

		return new StringRepresentation(this.a.toRegex());
	}

	@Override
	public FSA evalNonNullConstant(Constant constant, ProgramPoint pp) throws SemanticException {
		if (constant.getValue() instanceof String) {
			return new FSA(new Automaton((String) constant.getValue()));
		}
		return top();
	}

	@Override
	public FSA evalUnaryExpression(UnaryOperator operator, FSA arg, ProgramPoint pp) throws SemanticException {
		// TODO
		return super.evalUnaryExpression(operator, arg, pp);
	}

	@Override
	public FSA evalBinaryExpression(BinaryOperator operator, FSA left, FSA right, ProgramPoint pp)
			throws SemanticException {
		if (operator == StringConcat.INSTANCE)
			return new FSA(left.a.concat(right.a));
		return top();
	}

	@Override
	public FSA evalTernaryExpression(TernaryOperator operator, FSA left, FSA middle, FSA right, ProgramPoint pp)
			throws SemanticException {
		// TODO
		return super.evalTernaryExpression(operator, left, middle, right, pp);
	}

	@Override
	public SemanticDomain.Satisfiability satisfiesBinaryExpression(BinaryOperator operator, FSA left, FSA right,
			ProgramPoint pp) throws SemanticException {
		if (operator == StringContains.INSTANCE) {
			try {
				Set<String> rightLang = right.a.getLanguage();
				Set<String> leftLang = left.a.getLanguage();
				// right accepts only the empty string
				if (rightLang.size() == 1 && rightLang.contains(""))
					return SemanticDomain.Satisfiability.SATISFIED;

				boolean noneContained = true;
				for (String sub : rightLang) {
					for (String s : leftLang) {
						if (s.contains(sub)) {
							noneContained = false;
							break;
						}
						if (!noneContained)
							break;
					}
				}
				if (noneContained)
					return SemanticDomain.Satisfiability.NOT_SATISFIED;

				// all the strings accepted by right are substring of at least
				// one string accpeted by left
				for (String sub : rightLang) {
					boolean isContained = false;
					for (String s : leftLang) {
						if (s.contains(sub)) {
							isContained = true;
							break;
						}
					}
					if (!isContained)
						return SemanticDomain.Satisfiability.UNKNOWN;
				}
				return SemanticDomain.Satisfiability.SATISFIED;
			} catch (CyclicAutomatonException e) {
				return SemanticDomain.Satisfiability.UNKNOWN;
			}
		}
		return SemanticDomain.Satisfiability.UNKNOWN;
	}
}
