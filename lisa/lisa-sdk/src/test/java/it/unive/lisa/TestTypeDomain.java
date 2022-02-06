package it.unive.lisa;

import it.unive.lisa.analysis.BaseLattice;
import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.representation.DomainRepresentation;
import it.unive.lisa.analysis.value.TypeDomain;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.type.Type;
import it.unive.lisa.util.collections.externalSet.ExternalSet;

public class TestTypeDomain extends BaseLattice<TestTypeDomain> implements TypeDomain<TestTypeDomain> {

	@Override
	public TestTypeDomain assign(Identifier id, ValueExpression expression, ProgramPoint pp) throws SemanticException {
		return null;
	}

	@Override
	public TestTypeDomain smallStepSemantics(ValueExpression expression, ProgramPoint pp) throws SemanticException {
		return null;
	}

	@Override
	public TestTypeDomain assume(ValueExpression expression, ProgramPoint pp) throws SemanticException {
		return null;
	}

	@Override
	public TestTypeDomain forgetIdentifier(Identifier id) throws SemanticException {
		return null;
	}

	@Override
	public Satisfiability satisfies(ValueExpression expression, ProgramPoint pp) throws SemanticException {
		return null;
	}

	@Override
	public TestTypeDomain pushScope(ScopeToken token) throws SemanticException {
		return null;
	}

	@Override
	public TestTypeDomain popScope(ScopeToken token) throws SemanticException {
		return null;
	}

	@Override
	public DomainRepresentation representation() {
		return null;
	}

	@Override
	public TestTypeDomain top() {
		return null;
	}

	@Override
	public TestTypeDomain bottom() {
		return null;
	}

	@Override
	protected TestTypeDomain lubAux(TestTypeDomain other) throws SemanticException {
		return null;
	}

	@Override
	protected TestTypeDomain wideningAux(TestTypeDomain other) throws SemanticException {
		return null;
	}

	@Override
	protected boolean lessOrEqualAux(TestTypeDomain other) throws SemanticException {
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

	@Override
	public ExternalSet<Type> getInferredRuntimeTypes() {
		return null;
	}

	@Override
	public Type getInferredDynamicType() {
		return null;
	}
}
