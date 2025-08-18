package it.unive.lisa.analysis.nonRedundantPowerset;

import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.lattices.Satisfiability;
import it.unive.lisa.analysis.nonrelational.value.BaseNonRelationalValueDomain;
import it.unive.lisa.analysis.nonrelational.value.NonRelationalValueDomain;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.UnaryExpression;
import java.util.HashSet;
import java.util.Set;

/**
 * A {@link NonRelationalValueDomain} that computes
 * {@link NonRedundantSetLattice} elements as the powerset of the elements of a
 * given underlying lattice.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <S> the type of the concrete non redundant set lattice
 * @param <L> the type of the underlying lattice whose elements are contained in
 *                the non redundant set lattice
 */
public class NonRelationalNonRedundantPowerset<S extends NonRedundantSetLattice<S, L>,
		L extends Lattice<L>> implements BaseNonRelationalValueDomain<S> {

	/**
	 * The underlying {@link BaseNonRelationalValueDomain} by which it can be
	 * possible to retrieve top and bottom elements.
	 */
	private final BaseNonRelationalValueDomain<L> valueDomain;

	private final S singleton;

	/**
	 * Creates an instance with elementsSet as elements and valueDomain as
	 * element.
	 * 
	 * @param valueDomain the underlying domain treating individual lattice
	 *                        instances
	 * @param singleton   a singleton instance of the non redundant set lattice
	 *                        that can be used to create new instances of the
	 *                        same type
	 */
	public NonRelationalNonRedundantPowerset(
			BaseNonRelationalValueDomain<L> valueDomain,
			S singleton) {
		this.valueDomain = valueDomain;
		this.singleton = singleton;
	}

	@Override
	public S evalNonNullConstant(
			Constant constant,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		Set<L> newSet = new HashSet<>();
		newSet.add(valueDomain.evalNonNullConstant(constant, pp, oracle));
		return singleton.mk(newSet).removeRedundancy().removeOverlapping();
	}

	@Override
	public S evalUnaryExpression(
			UnaryExpression expression,
			S arg,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		Set<L> newSet = new HashSet<>();
		for (L s : arg.elements)
			newSet.add(valueDomain.evalUnaryExpression(expression, s, pp, oracle));
		return singleton.mk(newSet).removeRedundancy().removeOverlapping();
	}

	@Override
	public S evalBinaryExpression(
			BinaryExpression expression,
			S left,
			S right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		Set<L> newSet = new HashSet<>();
		for (L sLeft : left.elements)
			for (L sRight : right.elements)
				newSet.add(valueDomain.evalBinaryExpression(expression, sLeft, sRight, pp, oracle));
		return singleton.mk(newSet).removeRedundancy().removeOverlapping();
	}

	@Override
	public Satisfiability satisfiesBinaryExpression(
			BinaryExpression expression,
			S left,
			S right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (left.isTop() || right.isTop())
			return Satisfiability.UNKNOWN;

		Satisfiability sat = Satisfiability.BOTTOM;
		for (L sLeft : left.elements)
			for (L sRight : right.elements)
				sat = sat.lub(valueDomain.satisfiesBinaryExpression(expression, sLeft, sRight, pp, oracle));
		return sat;
	}

	@Override
	public S top() {
		return singleton.top();
	}

	@Override
	public S bottom() {
		return singleton.bottom();
	}

	@Override
	public ValueEnvironment<S> makeLattice() {
		return new ValueEnvironment<>(singleton.top());
	}

}
