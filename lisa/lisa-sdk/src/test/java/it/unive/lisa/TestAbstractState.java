package it.unive.lisa;

import java.util.Collections;
import java.util.Set;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.analysis.representation.DomainRepresentation;
import it.unive.lisa.analysis.representation.StringRepresentation;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;

public class TestAbstractState extends TestDomain<TestAbstractState, SymbolicExpression>
		implements AbstractState<TestAbstractState, TestHeapDomain, TestValueDomain, TestTypeDomain> {

	@Override
	public DomainRepresentation representation() {
		return new StringRepresentation("state");
	}

	@Override
	public ExpressionSet<SymbolicExpression> rewrite(SymbolicExpression expression, ProgramPoint pp)
			throws SemanticException {
		return new ExpressionSet<>(expression);
	}

	@Override
	public ExpressionSet<SymbolicExpression> rewrite(ExpressionSet<SymbolicExpression> expressions, ProgramPoint pp)
			throws SemanticException {
		return expressions;
	}

	@Override
	public Set<Type> getRuntimeTypesOf(SymbolicExpression e, ProgramPoint pp) throws SemanticException {
		return Collections.singleton(Untyped.INSTANCE);
	}

	@Override
	public Type getDynamicTypeOf(SymbolicExpression e, ProgramPoint pp) throws SemanticException {
		return Untyped.INSTANCE;
	}
}
