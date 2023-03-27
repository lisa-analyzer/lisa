package it.unive.lisa;

import it.unive.lisa.analysis.representation.DomainRepresentation;
import it.unive.lisa.analysis.representation.StringRepresentation;
import it.unive.lisa.analysis.value.TypeDomain;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;
import java.util.Collections;
import java.util.Set;

public class TestTypeDomain extends TestDomain<TestTypeDomain, ValueExpression> implements TypeDomain<TestTypeDomain> {

	@Override
	public DomainRepresentation representation() {
		return new StringRepresentation("type");
	}

	@Override
	public Set<Type> getRuntimeTypesOf(ValueExpression e, ProgramPoint pp) {
		return Collections.emptySet();
	}

	@Override
	public Type getDynamicTypeOf(ValueExpression e, ProgramPoint pp) {
		return Untyped.INSTANCE;
	}
}
