package it.unive.lisa;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;
import java.util.Collections;
import java.util.Set;

public class TestAbstractState extends TestDomain<TestAbstractState, SymbolicExpression>
		implements AbstractState<TestAbstractState> {

	@Override
	public StructuredRepresentation representation() {
		return new StringRepresentation("state");
	}

	@Override
	public ExpressionSet rewrite(SymbolicExpression expression, ProgramPoint pp)
			throws SemanticException {
		return new ExpressionSet(expression);
	}

	@Override
	public ExpressionSet rewrite(ExpressionSet expressions, ProgramPoint pp)
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
