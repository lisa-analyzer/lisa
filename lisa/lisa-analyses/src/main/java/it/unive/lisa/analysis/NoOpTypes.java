package it.unive.lisa.analysis;

import it.unive.lisa.analysis.lattices.SingleTypeLattice;
import it.unive.lisa.analysis.type.TypeDomain;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;
import java.util.Set;

/**
 * A no-op type domain that uses {@link SingleTypeLattice} as lattice structure.
 * This is useful in analyses where type information is not relevant or when a
 * placeholder is needed. Note that this domain cannot produce typing
 * information:
 * {@link #getRuntimeTypesOf(SingleTypeLattice, SymbolicExpression, ProgramPoint, SemanticOracle)}
 * always returns all possible types, and
 * {@link #getDynamicTypeOf(SingleTypeLattice, SymbolicExpression, ProgramPoint, SemanticOracle)}
 * always returns {@link Untyped#INSTANCE}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class NoOpTypes
		implements
		TypeDomain<SingleTypeLattice> {

	@Override
	public SingleTypeLattice makeLattice() {
		return SingleTypeLattice.SINGLETON;
	}

	@Override
	public SingleTypeLattice assign(
			SingleTypeLattice state,
			Identifier id,
			ValueExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return state;
	}

	@Override
	public SingleTypeLattice smallStepSemantics(
			SingleTypeLattice state,
			ValueExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return state;
	}

	@Override
	public SingleTypeLattice assume(
			SingleTypeLattice state,
			ValueExpression expression,
			ProgramPoint src,
			ProgramPoint dest,
			SemanticOracle oracle)
			throws SemanticException {
		return state;
	}

	@Override
	public Set<Type> getRuntimeTypesOf(
			SingleTypeLattice state,
			SymbolicExpression e,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return pp.getProgram().getTypes().getTypes();
	}

	@Override
	public Type getDynamicTypeOf(
			SingleTypeLattice state,
			SymbolicExpression e,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return Untyped.INSTANCE;
	}

}
