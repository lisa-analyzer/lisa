package it.unive.lisa.analysis.types;

import java.util.Map;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.instances.TypeAnalysis;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;
import it.unive.lisa.util.collections.externalSet.ExternalSet;

public class TypeInferenceDomain extends ValueEnvironment<InferredTypes> implements TypeAnalysis {

	public TypeInferenceDomain() {
		super(new InferredTypes());
	}

	private TypeInferenceDomain(InferredTypes domain, Map<Identifier, InferredTypes> function, InferredTypes stack) {
		super(domain, function, stack);
	}

	@Override
	protected ValueEnvironment<InferredTypes> mk(InferredTypes lattice, Map<Identifier, InferredTypes> function) {
		return new TypeInferenceDomain(lattice, function, stack);
	}

	@Override
	protected TypeInferenceDomain copy() {
		return new TypeInferenceDomain(lattice, mkNewFunction(function), stack);
	}

	@Override
	protected TypeInferenceDomain assignAux(Identifier id, ValueExpression expression,
			Map<Identifier, InferredTypes> function,
			InferredTypes value, InferredTypes eval, ProgramPoint pp) {
		return new TypeInferenceDomain(lattice, function, value);
	}

	@Override
	public TypeInferenceDomain smallStepSemantics(ValueExpression expression, ProgramPoint pp)
			throws SemanticException {
		return new TypeInferenceDomain(lattice, function, lattice.eval(expression, this, pp));
	}

	@Override
	protected TypeInferenceDomain assumeSatisfied(InferredTypes eval) {
		return this;
	}

	@Override
	protected TypeInferenceDomain glbAux(InferredTypes lattice, Map<Identifier, InferredTypes> function,
			ValueEnvironment<InferredTypes> other) throws SemanticException {
		return new TypeInferenceDomain(lattice, function, stack.glb(other.getValueOnStack()));
	}

	@Override
	public TypeInferenceDomain top() {
		return isTop() ? this : new TypeInferenceDomain(lattice.top(), null, lattice.top());
	}

	@Override
	public TypeInferenceDomain bottom() {
		return isBottom() ? this : new TypeInferenceDomain(lattice.bottom(), null, lattice.bottom());
	}

	@Override
	public ExternalSet<Type> getInferredRuntimeTypes() {
		return stack.getRuntimeTypes();
	}

	@Override
	public Type getInferredDynamicType() {
		ExternalSet<Type> types = stack.getRuntimeTypes();
		if (stack.isTop() || stack.isBottom() || types.isEmpty())
			return Untyped.INSTANCE;
		return types.reduce(types.first(), (result, t) -> result.commonSupertype(t));
	}
}
