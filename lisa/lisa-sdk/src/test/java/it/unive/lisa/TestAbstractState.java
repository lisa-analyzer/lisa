package it.unive.lisa;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.representation.DomainRepresentation;
import it.unive.lisa.analysis.representation.StringRepresentation;
import it.unive.lisa.symbolic.SymbolicExpression;

public class TestAbstractState extends TestDomain<TestAbstractState, SymbolicExpression>
		implements AbstractState<TestAbstractState, TestHeapDomain, TestValueDomain, TestTypeDomain> {

	@Override
	public DomainRepresentation representation() {
		return new StringRepresentation("state");
	}

	@Override
	public TestHeapDomain getHeapState() {
		return new TestHeapDomain();
	}

	@Override
	public TestValueDomain getValueState() {
		return new TestValueDomain();
	}

	@Override
	public TestTypeDomain getTypeState() {
		return new TestTypeDomain();
	}

}
