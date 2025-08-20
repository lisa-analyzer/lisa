package it.unive.lisa;

import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.analysis.lattices.Satisfiability;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;
import java.util.Collections;
import java.util.Set;

public class TestAbstractDomain
		implements
		AbstractDomain<TestAbstractState> {

	@Override
	public TestAbstractState assign(
			TestAbstractState state,
			Identifier id,
			SymbolicExpression expression,
			ProgramPoint pp)
			throws SemanticException {
		return state;
	}

	@Override
	public TestAbstractState smallStepSemantics(
			TestAbstractState state,
			SymbolicExpression expression,
			ProgramPoint pp)
			throws SemanticException {
		return state;
	}

	@Override
	public TestAbstractState assume(
			TestAbstractState state,
			SymbolicExpression expression,
			ProgramPoint src,
			ProgramPoint dest)
			throws SemanticException {
		return state;
	}

	@Override
	public SemanticOracle makeOracle(
			TestAbstractState state) {
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

	}

}
