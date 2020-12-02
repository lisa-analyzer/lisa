package it.unive.lisa.analysis.impl.types;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import it.unive.lisa.analysis.FunctionalLattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.ValueDomain;
import it.unive.lisa.caches.Caches;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.ValueExpression;

public final class TypeEnvironment extends FunctionalLattice<TypeEnvironment, Identifier, InferredTypes>
		implements ValueDomain<TypeEnvironment> {

	private final InferredTypes lastComputedTypes;

	public TypeEnvironment() {
		super(new InferredTypes());
		this.lastComputedTypes = new InferredTypes(Caches.types().mkEmptySet());
	}

	private TypeEnvironment(InferredTypes domain, Map<Identifier, InferredTypes> function,
			InferredTypes lastComputedTypes) {
		super(domain, function);
		this.lastComputedTypes = lastComputedTypes;
	}

	public InferredTypes getLastComputedTypes() {
		return lastComputedTypes;
	}

	@Override
	public TypeEnvironment assign(Identifier id, ValueExpression value) {
		Map<Identifier, InferredTypes> func;
		if (function == null)
			func = new HashMap<>();
		else
			func = new HashMap<>(function);
		InferredTypes inferred = new InferredTypes(value.getTypes());
		func.put(id, inferred);
		return new TypeEnvironment(lattice, func, inferred);
	}

	@Override
	public TypeEnvironment smallStepSemantics(ValueExpression expression) {
		// environment should not change without an assignment
		return new TypeEnvironment(lattice, function, new InferredTypes(expression.getTypes()));
	}

	@Override
	public TypeEnvironment assume(ValueExpression expression) throws SemanticException {
		// TODO: to be refined
		return new TypeEnvironment(lattice, function, new InferredTypes(expression.getTypes()));
	}

	@Override
	public Satisfiability satisfies(ValueExpression currentExpression) {
		// TODO: to be refined
		return Satisfiability.UNKNOWN;
	}

	@Override
	public TypeEnvironment lubAux(TypeEnvironment other) throws SemanticException {
		TypeEnvironment lub = super.lubAux(other);
		return new TypeEnvironment(lub.lattice, lub.function, lastComputedTypes.lub(other.lastComputedTypes));
	}

	@Override
	public TypeEnvironment wideningAux(TypeEnvironment other) throws SemanticException {
		TypeEnvironment widen = super.wideningAux(other);
		return new TypeEnvironment(widen.lattice, widen.function, lastComputedTypes.lub(other.lastComputedTypes));
	}

	@Override
	public boolean lessOrEqualAux(TypeEnvironment other) throws SemanticException {
		if (!super.lessOrEqualAux(other))
			return false;

		return lastComputedTypes.lessOrEqual(other.lastComputedTypes);
	}

	@Override
	public TypeEnvironment top() {
		return new TypeEnvironment(lattice.top(), null, new InferredTypes());
	}

	@Override
	public TypeEnvironment bottom() {
		return new TypeEnvironment(lattice.bottom(), null, new InferredTypes());
	}

	@Override
	public boolean isTop() {
		return lattice.isTop() && function == null;
	}

	@Override
	public boolean isBottom() {
		return lattice.isBottom() && function == null;
	}

	@Override
	public TypeEnvironment forgetIdentifier(Identifier id) throws SemanticException {
		if (function == null)
			return new TypeEnvironment(lattice, null, new InferredTypes());

		TypeEnvironment result = new TypeEnvironment(lattice, new HashMap<>(function), lastComputedTypes);
		if (result.function.containsKey(id))
			result.function.remove(id);

		return result;
	}

	@Override
	public int hashCode() {
		return super.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		return true;
	}

	@Override
	public String toString() {
		return representation();
	}

	@Override
	public String representation() {
		if (isTop())
			return "TOP";

		if (isBottom())
			return "BOTTOM";

		StringBuilder builder = new StringBuilder();
		for (Entry<Identifier, InferredTypes> entry : function.entrySet())
			builder.append(entry.getKey()).append(": ").append(entry.getValue().toString()).append("\n");

		return builder.toString().trim();
	}
}