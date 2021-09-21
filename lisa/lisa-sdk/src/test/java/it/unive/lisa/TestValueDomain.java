package it.unive.lisa;

import it.unive.lisa.analysis.BaseLattice;
import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.representation.DomainRepresentation;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.ValueExpression;

public class TestValueDomain extends BaseLattice<TestValueDomain> implements ValueDomain<TestValueDomain> {

	@Override
	public TestValueDomain assign(Identifier id, ValueExpression expression, ProgramPoint pp) throws SemanticException {
		return null;
	}

	@Override
	public TestValueDomain smallStepSemantics(ValueExpression expression, ProgramPoint pp) throws SemanticException {
		return null;
	}

	@Override
	public TestValueDomain assume(ValueExpression expression, ProgramPoint pp) throws SemanticException {
		return null;
	}

	@Override
	public TestValueDomain forgetIdentifier(Identifier id) throws SemanticException {
		return null;
	}

	@Override
	public Satisfiability satisfies(ValueExpression expression, ProgramPoint pp) throws SemanticException {
		return null;
	}

	@Override
	public TestValueDomain pushScope(ScopeToken token) throws SemanticException {
		return null;
	}

	@Override
	public TestValueDomain popScope(ScopeToken token) throws SemanticException {
		return null;
	}

	@Override
	public DomainRepresentation representation() {
		return null;
	}

	@Override
	public TestValueDomain top() {
		return null;
	}

	@Override
	public TestValueDomain bottom() {
		return null;
	}

	@Override
	protected TestValueDomain lubAux(TestValueDomain other) throws SemanticException {
		return null;
	}

	@Override
	protected TestValueDomain wideningAux(TestValueDomain other) throws SemanticException {
		return null;
	}

	@Override
	protected boolean lessOrEqualAux(TestValueDomain other) throws SemanticException {
		return false;
	}

	@Override
	public boolean equals(Object obj) {
		return this == obj;
	}

	@Override
	public int hashCode() {
		return 0;
	}

	@Override
	public String toString() {
		return null;
	}
}
