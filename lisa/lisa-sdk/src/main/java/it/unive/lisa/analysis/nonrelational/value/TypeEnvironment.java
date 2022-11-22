package it.unive.lisa.analysis.nonrelational.value;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.lattices.FunctionalLattice;
import it.unive.lisa.analysis.nonrelational.Environment;
import it.unive.lisa.analysis.representation.DomainRepresentation;
import it.unive.lisa.analysis.representation.ObjectRepresentation;
import it.unive.lisa.analysis.value.TypeDomain;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.tuple.Pair;

/**
 * An environment for a {@link NonRelationalTypeDomain}, that maps
 * {@link Identifier}s to instances of such domain. This is a
 * {@link FunctionalLattice}, that is, it implements a function mapping keys
 * (identifiers) to values (instances of the domain), and lattice operations are
 * automatically lifted for individual elements of the environment if they are
 * mapped to the same key. The runtime types computed for the last processed
 * expression are exposed through {@link #getInferredRuntimeTypes()} (and
 * {@link #getInferredDynamicType()} yields the lub of such types).
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <T> the concrete instance of the {@link NonRelationalTypeDomain} whose
 *                instances are mapped in this environment
 */
public class TypeEnvironment<T extends NonRelationalTypeDomain<T>>
		extends Environment<TypeEnvironment<T>, ValueExpression, T, T>
		implements TypeDomain<TypeEnvironment<T>> {

	private final T stack;

	/**
	 * Builds an empty environment.
	 * 
	 * @param domain a singleton instance to be used during semantic operations
	 *                   to retrieve top and bottom values
	 */
	public TypeEnvironment(T domain) {
		super(domain);
		this.stack = domain.bottom();
	}

	/**
	 * Builds an environment containing the given mapping. If function is
	 * {@code null}, the new environment is the top environment if
	 * {@code lattice.isTop()} holds, and it is the bottom environment if
	 * {@code lattice.isBottom()} holds.
	 * 
	 * @param domain   a singleton instance to be used during semantic
	 *                     operations to retrieve top and bottom values
	 * @param function the function representing the mapping contained in the
	 *                     new environment; can be {@code null}
	 * @param stack    the abstract value for the last computed expression, that
	 *                     is left on the top of the stack
	 */
	public TypeEnvironment(T domain, Map<Identifier, T> function, T stack) {
		super(domain, function);
		this.stack = stack;
	}

	@Override
	public TypeEnvironment<T> mk(T lattice, Map<Identifier, T> function) {
		return new TypeEnvironment<>(lattice, function, stack);
	}

	@Override
	public Pair<T, T> eval(ValueExpression expression, ProgramPoint pp) throws SemanticException {
		T eval = lattice.eval(expression, this, pp);
		return Pair.of(eval, eval);
	}

	@Override
	public TypeEnvironment<T> assignAux(Identifier id, ValueExpression expression, Map<Identifier, T> function,
			T value, T eval, ProgramPoint pp) {
		return new TypeEnvironment<>(lattice, function, value);
	}

	@Override
	public TypeEnvironment<T> smallStepSemantics(ValueExpression expression, ProgramPoint pp)
			throws SemanticException {
		if (isBottom())
			return this;
		return new TypeEnvironment<>(lattice, function, lattice.eval(expression, this, pp));
	}

	@Override
	public TypeEnvironment<T> assumeSatisfied(T eval) {
		return this;
	}

	@Override
	public TypeEnvironment<T> lubAux(TypeEnvironment<T> other)
			throws SemanticException {
		TypeEnvironment<T> newEnv = functionalLift(other, this::lubKeys, (o1, o2) -> o1 == null ? o2 : o1.lub(o2));
		return new TypeEnvironment<>(newEnv.lattice, newEnv.function, stack.lub(other.stack));
	}

	@Override
	public TypeEnvironment<T> wideningAux(TypeEnvironment<T> other) throws SemanticException {
		TypeEnvironment<
				T> newEnv = functionalLift(other, this::lubKeys, (o1, o2) -> o1 == null ? o2 : o1.widening(o2));
		return new TypeEnvironment<>(newEnv.lattice, newEnv.function, stack.widening(other.stack));
	}

	@Override
	public TypeEnvironment<T> glbAux(TypeEnvironment<T> other)
			throws SemanticException {
		TypeEnvironment<T> newEnv = functionalLift(other, this::glbKeys, (o1, o2) -> o1 == null ? o2 : o1.glb(o2));
		return new TypeEnvironment<>(newEnv.lattice, newEnv.function, stack.glb(other.stack));
	}

	@Override
	public TypeEnvironment<T> narrowingAux(TypeEnvironment<T> other) throws SemanticException {
		TypeEnvironment<
				T> newEnv = functionalLift(other, this::glbKeys, (o1, o2) -> o1 == null ? o2 : o1.narrowing(o2));
		return new TypeEnvironment<>(newEnv.lattice, newEnv.function, stack.narrowing(other.stack));
	}

	@Override
	public TypeEnvironment<T> top() {
		return isTop() ? this : new TypeEnvironment<>(lattice.top(), null, lattice.top());
	}

	@Override
	public TypeEnvironment<T> bottom() {
		return isBottom() ? this : new TypeEnvironment<>(lattice.bottom(), null, lattice.bottom());
	}

	@Override
	public boolean isTop() {
		return super.isTop() && stack.isTop();
	}

	@Override
	public boolean isBottom() {
		return super.isBottom() && stack.isBottom();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((stack == null) ? 0 : stack.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (!(obj instanceof TypeEnvironment))
			return false;
		TypeEnvironment<?> other = (TypeEnvironment<?>) obj;
		if (stack == null) {
			if (other.stack != null)
				return false;
		} else if (!stack.equals(other.stack))
			return false;
		return true;
	}

	@Override
	public DomainRepresentation representation() {
		if (isBottom() || isTop())
			return super.representation();

		return new ObjectRepresentation(Map.of("map", super.representation(), "stack", stack.representation()));
	}

	@Override
	public Set<Type> getInferredRuntimeTypes() {
		return stack.getRuntimeTypes();
	}

	@Override
	public Type getInferredDynamicType() {
		Set<Type> types = stack.getRuntimeTypes();
		if (stack.isTop() || stack.isBottom() || types.isEmpty())
			return Untyped.INSTANCE;
		return Type.commonSupertype(types, Untyped.INSTANCE);
	}
}
