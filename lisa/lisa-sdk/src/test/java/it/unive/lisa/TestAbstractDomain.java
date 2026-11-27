package it.unive.lisa;

import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AnalysisState.Error;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.events.EventQueue;
import it.unive.lisa.interprocedural.ScopeId;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.lattices.Satisfiability;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.program.cfg.protection.ProtectedBlock;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;
import java.util.Collections;
import java.util.Set;
import org.apache.commons.lang3.tuple.Pair;

public class TestAbstractDomain
		implements
		AbstractDomain<TestAbstractState> {

	@Override
	public TestAbstractState assign(
			TestAbstractState state,
			Identifier id,
			SymbolicExpression expression,
			ScopeId<?> context,
			ProgramPoint pp)
			throws SemanticException {
		return state;
	}

	@Override
	public TestAbstractState smallStepSemantics(
			TestAbstractState state,
			SymbolicExpression expression,
			ScopeId<?> context,
			ProgramPoint pp)
			throws SemanticException {
		return state;
	}

	@Override
	public TestAbstractState assume(
			TestAbstractState state,
			SymbolicExpression expression,
			ScopeId<?> context,
			ProgramPoint src,
			ProgramPoint dest)
			throws SemanticException {
		return state;
	}

	@Override
	public SemanticOracle makeOracle(
			TestAbstractState state,
			ScopeId<?> context) {
		return new TestOracle();
	}

	@Override
	public TestAbstractState makeLattice() {
		return new TestAbstractState();
	}

	public class TestOracle
			implements
			SemanticOracle {

		@Override
		public EventQueue getEventQueue() {
			return null;
		}

		@Override
		public ScopeId<?> getContext() {
			return null;
		}

		@Override
		public Set<Type> getRuntimeTypesOf(
				SymbolicExpression e,
				ProgramPoint pp)
				throws SemanticException {
			return Collections.singleton(Untyped.INSTANCE);
		}

		@Override
		public Type getDynamicTypeOf(
				SymbolicExpression e,
				ProgramPoint pp)
				throws SemanticException {
			return Untyped.INSTANCE;
		}

		@Override
		public ExpressionSet rewrite(
				SymbolicExpression expression,
				ProgramPoint pp)
				throws SemanticException {
			return new ExpressionSet(expression);
		}

		@Override
		public ExpressionSet rewrite(
				ExpressionSet expressions,
				ProgramPoint pp)
				throws SemanticException {
			return expressions;
		}

		@Override
		public Satisfiability alias(
				SymbolicExpression x,
				SymbolicExpression y,
				ProgramPoint pp)
				throws SemanticException {
			return Satisfiability.UNKNOWN;
		}

		@Override
		public ExpressionSet reachableFrom(
				SymbolicExpression e,
				ProgramPoint pp)
				throws SemanticException {
			return new ExpressionSet(e);
		}

		@Override
		public Satisfiability isReachableFrom(
				SymbolicExpression x,
				SymbolicExpression y,
				ProgramPoint pp)
				throws SemanticException {
			return Satisfiability.UNKNOWN;
		}

		@Override
		public Satisfiability areMutuallyReachable(
				SymbolicExpression x,
				SymbolicExpression y,
				ProgramPoint pp)
				throws SemanticException {
			return Satisfiability.UNKNOWN;
		}

		@Override
		public boolean hasWholeValueAnlysis() {
			return false;
		}

		@Override
		public Set<BinaryExpression> constraints(
				ValueDomain<?> requesting,
				ValueExpression e,
				ProgramPoint pp)
				throws SemanticException {
			return Collections.emptySet();
		}

	}

	@Override
	public TestAbstractState onCallReturn(
			TestAbstractState entryState,
			TestAbstractState callres,
			ScopeId<?> context,
			ProgramPoint call)
			throws SemanticException {
		return callres;
	}

	@Override
	public Pair<TestAbstractState, TestAbstractState> onExecutionToError(
			TestAbstractState remaining,
			TestAbstractState moved,
			Error error,
			ScopeId<?> context,
			ProgramPoint pp)
			throws SemanticException {
		return Pair.of(remaining, moved);
	}

	@Override
	public TestAbstractState onErrorToExecution(
			TestAbstractState state,
			ProtectedBlock protectedBlock,
			ScopeId<?> context,
			ProgramPoint pp)
			throws SemanticException {
		return state;
	}

	@Override
	public Pair<TestAbstractState, TestAbstractState> onExecutionToHalting(
			TestAbstractState remaining,
			TestAbstractState moved,
			ScopeId<?> context,
			ProgramPoint pp)
			throws SemanticException {
		return Pair.of(remaining, moved);
	}

	@Override
	public void setEventQueue(
			EventQueue queue) {
	}

}
