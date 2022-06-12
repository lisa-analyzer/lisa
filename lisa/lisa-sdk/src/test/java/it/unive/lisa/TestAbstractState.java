package it.unive.lisa;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.BaseLattice;
import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.representation.DomainRepresentation;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Identifier;

public class TestAbstractState extends BaseLattice<TestAbstractState>
		implements AbstractState<TestAbstractState, TestHeapDomain, TestValueDomain, TestTypeDomain> {

	public TestAbstractState(TestHeapDomain heap, TestValueDomain value) {
	}

	@Override
	public TestHeapDomain getHeapState() {
		return null;
	}

	@Override
	public TestValueDomain getValueState() {
		return null;
	}

	@Override
	public TestAbstractState assign(Identifier id, SymbolicExpression expression, ProgramPoint pp)
			throws SemanticException {
		return null;
	}

	@Override
	public TestAbstractState smallStepSemantics(SymbolicExpression expression, ProgramPoint pp)
			throws SemanticException {
		return null;
	}

	@Override
	public TestAbstractState assume(SymbolicExpression expression, ProgramPoint pp) throws SemanticException {
		return null;
	}

	@Override
	public Satisfiability satisfies(SymbolicExpression expression, ProgramPoint pp) throws SemanticException {
		return null;
	}

	@Override
	public TestAbstractState pushScope(ScopeToken scope) throws SemanticException {
		return null;
	}

	@Override
	public TestAbstractState popScope(ScopeToken scope) throws SemanticException {
		return null;
	}

	@Override
	public TestAbstractState lubAux(TestAbstractState other) throws SemanticException {
		return null;
	}

	@Override
	public TestAbstractState wideningAux(TestAbstractState other) throws SemanticException {
		return null;
	}

	@Override
	public boolean lessOrEqualAux(TestAbstractState other) throws SemanticException {
		return false;
	}

	@Override
	public TestAbstractState top() {
		return null;
	}

	@Override
	public TestAbstractState bottom() {
		return null;
	}

	@Override
	public boolean isTop() {
		return false;
	}

	@Override
	public boolean isBottom() {
		return false;
	}

	@Override
	public TestAbstractState forgetIdentifier(Identifier id) throws SemanticException {
		return null;
	}

	@Override
	public int hashCode() {
		return 0;
	}

	@Override
	public boolean equals(Object obj) {
		return this == obj;
	}

	@Override
	public DomainRepresentation representation() {
		return null;
	}

	@Override
	public String toString() {
		return null;
	}

	@Override
	public TestTypeDomain getTypeState() {
		return null;
	}

	@Override
	public DomainRepresentation typeRepresentation() {
		return null;
	}
}
