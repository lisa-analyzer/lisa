package it.unive.lisa;

import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.type.TypeDomain;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;
import java.util.Collections;
import java.util.Set;

public class TestTypeDomain extends TestDomain<TestTypeDomain, ValueExpression> implements TypeDomain<TestTypeDomain> {

	@Override
	public StructuredRepresentation representation() {
		return new StringRepresentation("type");
	}

	@Override
	public Set<Type> getRuntimeTypesOf(
			SymbolicExpression e,
			ProgramPoint pp,
			SemanticOracle oracle) {
		return Collections.emptySet();
	}

	@Override
	public Type getDynamicTypeOf(
			SymbolicExpression e,
			ProgramPoint pp,
			SemanticOracle oracle) {
		return Untyped.INSTANCE;
	}
}
