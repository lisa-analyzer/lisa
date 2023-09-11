package it.unive.lisa;

import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;

public class TestValueDomain extends TestDomain<TestValueDomain, ValueExpression>
		implements ValueDomain<TestValueDomain> {

	@Override
	public StructuredRepresentation representation() {
		return new StringRepresentation("value");
	}
}
