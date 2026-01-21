package it.unive.lisa.analysis.nonRedundantPowerset;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.analysis.value.ValueLattice;
import it.unive.lisa.lattices.Satisfiability;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.ValueExpression;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * A {@link ValueDomain} that computes {@link NonRedundantSetDomainLattice}
 * elements as the powerset of the elements of a given underlying lattice.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <S> the type of the concrete non redundant set lattice
 * @param <L> the type of the underlying lattice whose elements are contained in
 *                the non redundant set lattice
 */
public class NonRedundantPowerset<S extends NonRedundantSetDomainLattice<S, L> & ValueLattice<S>,
		L extends ValueLattice<L>>
		implements
		ValueDomain<S> {

	/**
	 * An instance of the underlying lattice from which top and bottom can be
	 * retrieved. It is necessary in certain basic lattice operation.
	 */
	private final ValueDomain<L> valueDomain;

	private final S singleton;

	/**
	 * Create an instance of non redundant set of elements of the type of
	 * valueDomain with the elements contained in elements.
	 * 
	 * @param valueDomain the underlying domain treating individual lattice
	 *                        instances
	 * @param singleton   a singleton instance of the non redundant set lattice
	 *                        that can be used to create new instances of the
	 *                        same type
	 */
	public NonRedundantPowerset(
			ValueDomain<L> valueDomain,
			S singleton) {
		this.valueDomain = valueDomain;
		this.singleton = singleton;
	}

	@Override
	public S assign(
			S state,
			Identifier id,
			ValueExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (state.isBottom())
			return state;
		Set<L> newElements = new HashSet<>();
		for (L elem : state.elements)
			newElements.add(valueDomain.assign(elem, id, expression, pp, oracle));
		return state.mk(newElements).removeRedundancy();
	}

	@Override
	public S smallStepSemantics(
			S state,
			ValueExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (state.isBottom())
			return state;
		Set<L> newElements = new HashSet<>();
		for (L elem : state.elements)
			newElements.add(valueDomain.smallStepSemantics(elem, expression, pp, oracle));
		return state.mk(newElements).removeRedundancy();
	}

	@Override
	public S assume(
			S state,
			ValueExpression expression,
			ProgramPoint src,
			ProgramPoint dest,
			SemanticOracle oracle)
			throws SemanticException {
		if (state.isBottom())
			return state;
		Set<L> newElements = new HashSet<>();
		for (L elem : state.elements)
			newElements.add(valueDomain.assume(elem, expression, src, dest, oracle));
		return state.mk(newElements).removeRedundancy();
	}

	@Override
	public Satisfiability satisfies(
			S state,
			ValueExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		Set<Satisfiability> setSatisf = new HashSet<Satisfiability>();

		for (L element : state.elements)
			setSatisf.add(valueDomain.satisfies(element, expression, pp, oracle));

		if ((setSatisf.contains(Satisfiability.SATISFIED) && setSatisf.contains(Satisfiability.NOT_SATISFIED))
				|| setSatisf.contains(Satisfiability.UNKNOWN))
			return Satisfiability.UNKNOWN;
		else if (setSatisf.contains(Satisfiability.SATISFIED))
			return Satisfiability.SATISFIED;
		else if (setSatisf.contains(Satisfiability.NOT_SATISFIED))
			return Satisfiability.NOT_SATISFIED;

		return Satisfiability.UNKNOWN;
	}

	@Override
	public S makeLattice() {
		return singleton.mk(Collections.singleton(valueDomain.makeLattice()));
	}

}
