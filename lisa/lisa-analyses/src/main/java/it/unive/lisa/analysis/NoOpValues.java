package it.unive.lisa.analysis;

import it.unive.lisa.analysis.lattices.SingleValueLattice;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.ValueExpression;

/**
 * A no-op value domain that uses {@link SingleValueLattice} as lattice
 * structure. This is useful in analyses where value information is not relevant
 * or when a placeholder is needed.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class NoOpValues implements ValueDomain<SingleValueLattice> {

	@Override
	public SingleValueLattice makeLattice() {
		return SingleValueLattice.SINGLETON;
	}

	@Override
	public SingleValueLattice assign(
			SingleValueLattice state,
			Identifier id,
			ValueExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return state;
	}

	@Override
	public SingleValueLattice smallStepSemantics(
			SingleValueLattice state,
			ValueExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return state;
	}

	@Override
	public SingleValueLattice assume(
			SingleValueLattice state,
			ValueExpression expression,
			ProgramPoint src,
			ProgramPoint dest,
			SemanticOracle oracle)
			throws SemanticException {
		return state;
	}

}
