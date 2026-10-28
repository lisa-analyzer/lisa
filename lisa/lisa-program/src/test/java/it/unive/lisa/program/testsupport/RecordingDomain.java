package it.unive.lisa.program.testsupport;

import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AnalysisState.Error;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.events.EventQueue;
import it.unive.lisa.interprocedural.ScopeId;
import it.unive.lisa.lattices.Satisfiability;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.program.cfg.protection.ProtectedBlock;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.tuple.Pair;

// a hand-rolled fake AbstractDomain: it does not compute anything, it just
// records every expression it is asked to compute the semantics of, and lets
// a test configure the runtime types it should report for specific
// expressions (by identity), so that a Statement's own type-checking logic
// (e.g. Addition rejecting non-numeric operands) can be exercised precisely
public class RecordingDomain
		implements
		AbstractDomain<UnitLattice> {

	public final List<SymbolicExpression> smallStepCalls = new ArrayList<>();

	public final List<SymbolicExpression> assignCalls = new ArrayList<>();

	private final Map<SymbolicExpression, Set<Type>> runtimeTypes = new HashMap<>();

	public void setRuntimeTypes(
			SymbolicExpression expression,
			Set<Type> types) {
		runtimeTypes.put(expression, types);
	}

	private Set<Type> runtimeTypesOf(
			SymbolicExpression expression) {
		return runtimeTypes.getOrDefault(expression, Collections.singleton(Untyped.INSTANCE));
	}

	@Override
	public UnitLattice assign(
			UnitLattice state,
			Identifier id,
			SymbolicExpression expression,
			ScopeId<?> context,
			ProgramPoint pp)
			throws SemanticException {
		assignCalls.add(expression);
		return state;
	}

	@Override
	public UnitLattice smallStepSemantics(
			UnitLattice state,
			SymbolicExpression expression,
			ScopeId<?> context,
			ProgramPoint pp)
			throws SemanticException {
		smallStepCalls.add(expression);
		return state;
	}

	@Override
	public UnitLattice assume(
			UnitLattice state,
			SymbolicExpression expression,
			ScopeId<?> context,
			ProgramPoint src,
			ProgramPoint dest)
			throws SemanticException {
		return state;
	}

	@Override
	public UnitLattice makeLattice() {
		return UnitLattice.INSTANCE;
	}

	@Override
	public UnitLattice onCallReturn(
			UnitLattice entryState,
			UnitLattice callres,
			ScopeId<?> context,
			ProgramPoint call)
			throws SemanticException {
		return entryState;
	}

	@Override
	public Pair<UnitLattice, UnitLattice> onExecutionToError(
			UnitLattice remaining,
			UnitLattice moved,
			Error error,
			ScopeId<?> context,
			ProgramPoint pp)
			throws SemanticException {
		return Pair.of(remaining, moved);
	}

	@Override
	public UnitLattice onErrorToExecution(
			UnitLattice state,
			ProtectedBlock protectedBlock,
			ScopeId<?> context,
			ProgramPoint pp)
			throws SemanticException {
		return state;
	}

	@Override
	public Pair<UnitLattice, UnitLattice> onExecutionToHalting(
			UnitLattice remaining,
			UnitLattice moved,
			ScopeId<?> context,
			ProgramPoint pp)
			throws SemanticException {
		return Pair.of(remaining, moved);
	}

	@Override
	public void setEventQueue(
			EventQueue queue) {
	}

	@Override
	public SemanticOracle makeOracle(
			UnitLattice state,
			ScopeId<?> context) {
		return new SemanticOracle() {

			@Override
			public EventQueue getEventQueue() {
				return null;
			}

			@Override
			public ScopeId<?> getContext() {
				return null;
			}

			@Override
			public boolean hasWholeValueAnlysis() {
				return false;
			}

			@Override
			public Set<it.unive.lisa.symbolic.value.BinaryExpression> constraints(
					it.unive.lisa.analysis.value.ValueDomain<?> requesting,
					it.unive.lisa.symbolic.value.ValueExpression e,
					ProgramPoint pp)
					throws SemanticException {
				throw new UnsupportedOperationException("not needed for this test");
			}

			@Override
			public Set<Type> getRuntimeTypesOf(
					SymbolicExpression e,
					ProgramPoint pp)
					throws SemanticException {
				return runtimeTypesOf(e);
			}

			@Override
			public Type getDynamicTypeOf(
					SymbolicExpression e,
					ProgramPoint pp)
					throws SemanticException {
				throw new UnsupportedOperationException("not needed for this test");
			}

			@Override
			public it.unive.lisa.lattices.ExpressionSet rewrite(
					SymbolicExpression expression,
					ProgramPoint pp)
					throws SemanticException {
				throw new UnsupportedOperationException("not needed for this test");
			}

			@Override
			public it.unive.lisa.lattices.ExpressionSet rewrite(
					it.unive.lisa.lattices.ExpressionSet expressions,
					ProgramPoint pp)
					throws SemanticException {
				throw new UnsupportedOperationException("not needed for this test");
			}

			@Override
			public Satisfiability alias(
					SymbolicExpression x,
					SymbolicExpression y,
					ProgramPoint pp)
					throws SemanticException {
				throw new UnsupportedOperationException("not needed for this test");
			}

			@Override
			public it.unive.lisa.lattices.ExpressionSet reachableFrom(
					SymbolicExpression e,
					ProgramPoint pp)
					throws SemanticException {
				throw new UnsupportedOperationException("not needed for this test");
			}

			@Override
			public Satisfiability isReachableFrom(
					SymbolicExpression x,
					SymbolicExpression y,
					ProgramPoint pp)
					throws SemanticException {
				throw new UnsupportedOperationException("not needed for this test");
			}

			@Override
			public Satisfiability areMutuallyReachable(
					SymbolicExpression x,
					SymbolicExpression y,
					ProgramPoint pp)
					throws SemanticException {
				throw new UnsupportedOperationException("not needed for this test");
			}

		};
	}

}
