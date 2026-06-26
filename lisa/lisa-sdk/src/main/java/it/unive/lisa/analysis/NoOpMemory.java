package it.unive.lisa.analysis;

import it.unive.lisa.analysis.memory.MemoryDomain;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.lattices.Satisfiability;
import it.unive.lisa.lattices.SingleMemoryLattice;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Identifier;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;

/**
 * A no-op memory domain that uses {@link SingleMemoryLattice} as lattice
 * structure. This is useful in analyses where memory information is not
 * relevant or when a placeholder is needed. Note that this domain never
 * produces substitutions, and rewrite operations will always return the input
 * expression wrapped in an {@link ExpressionSet}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class NoOpMemory
		implements
		MemoryDomain<SingleMemoryLattice> {

	@Override
	public SingleMemoryLattice makeLattice() {
		return SingleMemoryLattice.SINGLETON;
	}

	@Override
	public Pair<SingleMemoryLattice, List<MemoryReplacement>> assign(
			SingleMemoryLattice state,
			Identifier id,
			SymbolicExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return Pair.of(state, Collections.emptyList());
	}

	@Override
	public Pair<SingleMemoryLattice, List<MemoryReplacement>> smallStepSemantics(
			SingleMemoryLattice state,
			SymbolicExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return Pair.of(state, Collections.emptyList());
	}

	@Override
	public Pair<SingleMemoryLattice, List<MemoryReplacement>> assume(
			SingleMemoryLattice state,
			SymbolicExpression expression,
			ProgramPoint src,
			ProgramPoint dest,
			SemanticOracle oracle)
			throws SemanticException {
		return Pair.of(state, Collections.emptyList());
	}

	@Override
	public Satisfiability alias(
			SingleMemoryLattice state,
			SymbolicExpression x,
			SymbolicExpression y,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return Satisfiability.UNKNOWN;
	}

	@Override
	public Satisfiability isReachableFrom(
			SingleMemoryLattice state,
			SymbolicExpression x,
			SymbolicExpression y,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return Satisfiability.UNKNOWN;
	}

	@Override
	public ExpressionSet rewrite(
			SingleMemoryLattice state,
			SymbolicExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return new ExpressionSet(expression);
	}

}
