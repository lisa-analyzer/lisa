package it.unive.lisa.analysis;

import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.analysis.lattices.Satisfiability;
import it.unive.lisa.analysis.lattices.SingleHeapLattice;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Identifier;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;

/**
 * A no-op heap domain that uses {@link SingleHeapLattice} as lattice structure.
 * This is useful in analyses where heap information is not relevant or when a
 * placeholder is needed. Note that this domain never produces substitutions,
 * and rewrite operations will always return the input expression wrapped in an
 * {@link ExpressionSet}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class NoOpHeap
		implements
		HeapDomain<SingleHeapLattice> {

	@Override
	public SingleHeapLattice makeLattice() {
		return SingleHeapLattice.SINGLETON;
	}

	@Override
	public Pair<SingleHeapLattice, List<HeapReplacement>> assign(
			SingleHeapLattice state,
			Identifier id,
			SymbolicExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return Pair.of(state, Collections.emptyList());
	}

	@Override
	public Pair<SingleHeapLattice, List<HeapReplacement>> smallStepSemantics(
			SingleHeapLattice state,
			SymbolicExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return Pair.of(state, Collections.emptyList());
	}

	@Override
	public Pair<SingleHeapLattice, List<HeapReplacement>> assume(
			SingleHeapLattice state,
			SymbolicExpression expression,
			ProgramPoint src,
			ProgramPoint dest,
			SemanticOracle oracle)
			throws SemanticException {
		return Pair.of(state, Collections.emptyList());
	}

	@Override
	public Satisfiability alias(
			SingleHeapLattice state,
			SymbolicExpression x,
			SymbolicExpression y,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return Satisfiability.UNKNOWN;
	}

	@Override
	public Satisfiability isReachableFrom(
			SingleHeapLattice state,
			SymbolicExpression x,
			SymbolicExpression y,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return Satisfiability.UNKNOWN;
	}

	@Override
	public ExpressionSet rewrite(
			SingleHeapLattice state,
			SymbolicExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return new ExpressionSet(expression);
	}

}
