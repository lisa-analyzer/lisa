package it.unive.lisa;

import it.unive.lisa.analysis.representation.DomainRepresentation;
import it.unive.lisa.analysis.representation.StringRepresentation;
import it.unive.lisa.analysis.value.TypeDomain;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;
import it.unive.lisa.util.collections.externalSet.ExternalSetCache;
import java.util.Set;

public class TestTypeDomain extends TestDomain<TestTypeDomain, ValueExpression> implements TypeDomain<TestTypeDomain> {

	@Override
	public DomainRepresentation representation() {
		return new StringRepresentation("type");
	}

	@Override
	public Set<Type> getInferredRuntimeTypes() {
		return new ExternalSetCache<Type>().mkEmptySet();
	}

	@Override
	public Type getInferredDynamicType() {
		return Untyped.INSTANCE;
	}
}
