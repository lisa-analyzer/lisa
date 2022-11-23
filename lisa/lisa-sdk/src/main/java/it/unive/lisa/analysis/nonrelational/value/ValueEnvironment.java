package it.unive.lisa.analysis.nonrelational.value;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.lattices.FunctionalLattice;
import it.unive.lisa.analysis.nonrelational.Environment;
import it.unive.lisa.analysis.representation.DomainRepresentation;
import it.unive.lisa.analysis.representation.ObjectRepresentation;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.ValueExpression;
import java.util.Map;
import org.apache.commons.lang3.tuple.Pair;

/**
 * An environment for a {@link NonRelationalValueDomain}, that maps
 * {@link Identifier}s to instances of such domain. This is a
 * {@link FunctionalLattice}, that is, it implements a function mapping keys
 * (identifiers) to values (instances of the domain), and lattice operations are
 * automatically lifted for individual elements of the environment if they are
 * mapped to the same key. The abstract value computed for the last processed
 * expression is exposed through {@link #getValueOnStack()}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <T> the concrete instance of the {@link NonRelationalValueDomain}
 *                whose instances are mapped in this environment
 */
public class ValueEnvironment<T extends NonRelationalValueDomain<T>>
		extends Environment<ValueEnvironment<T>, ValueExpression, T, T>
		implements ValueDomain<ValueEnvironment<T>> {

	private final T stack;

	/**
	 * Builds an empty environment.
	 * 
	 * @param domain a singleton instance to be used during semantic operations
	 *                   to retrieve top and bottom values
	 */
	public ValueEnvironment(T domain) {
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
	public ValueEnvironment(T domain, Map<Identifier, T> function, T stack) {
		super(domain, function);
		this.stack = stack;
	}

	/**
	 * Yields the computed value of the last {@link SymbolicExpression} handled
	 * by this domain, either through
	 * {@link #assign(Identifier, SymbolicExpression, ProgramPoint)} or
	 * {@link #smallStepSemantics(ValueExpression, ProgramPoint)}.
	 * 
	 * @return the value computed for the last expression
	 */
	public T getValueOnStack() {
		return stack;
	}

	@Override
	public ValueEnvironment<T> mk(T lattice, Map<Identifier, T> function) {
		return new ValueEnvironment<>(lattice, function, stack);
	}

	@Override
	public Pair<T, T> eval(ValueExpression expression, ProgramPoint pp) throws SemanticException {
		T eval = lattice.eval(expression, this, pp);
		return Pair.of(eval, eval);
	}

	@Override
	public ValueEnvironment<T> assignAux(Identifier id, ValueExpression expression, Map<Identifier, T> function,
			T value, T eval, ProgramPoint pp) {
		return new ValueEnvironment<>(lattice, function, value);
	}

	@Override
	public ValueEnvironment<T> smallStepSemantics(ValueExpression expression, ProgramPoint pp)
			throws SemanticException {
		if (isBottom())
			return this;
		return new ValueEnvironment<>(lattice, function, lattice.eval(expression, this, pp));
	}

	@Override
	public ValueEnvironment<T> assumeSatisfied(T eval) {
		return this;
	}

	@Override
	public ValueEnvironment<T> lubAux(ValueEnvironment<T> other)
			throws SemanticException {
		ValueEnvironment<T> newEnv = functionalLift(other, this::lubKeys, (o1, o2) -> o1 == null ? o2 : o1.lub(o2));
		return new ValueEnvironment<>(newEnv.lattice, newEnv.function, stack.lub(other.stack));
	}

	@Override
	public ValueEnvironment<T> wideningAux(ValueEnvironment<T> other) throws SemanticException {
		ValueEnvironment<
				T> newEnv = functionalLift(other, this::lubKeys, (o1, o2) -> o1 == null ? o2 : o1.widening(o2));
		return new ValueEnvironment<>(newEnv.lattice, newEnv.function, stack.widening(other.stack));
	}

	@Override
	public ValueEnvironment<T> glbAux(ValueEnvironment<T> other)
			throws SemanticException {
		ValueEnvironment<T> newEnv = functionalLift(other, this::glbKeys, (o1, o2) -> o1 == null ? o2 : o1.glb(o2));
		return new ValueEnvironment<>(newEnv.lattice, newEnv.function, stack.glb(other.stack));
	}

	@Override
	public ValueEnvironment<T> narrowingAux(ValueEnvironment<T> other) throws SemanticException {
		ValueEnvironment<
				T> newEnv = functionalLift(other, this::glbKeys, (o1, o2) -> o1 == null ? o2 : o1.narrowing(o2));
		return new ValueEnvironment<>(newEnv.lattice, newEnv.function, stack.narrowing(other.stack));
	}

	@Override
	public ValueEnvironment<T> top() {
		return isTop() ? this : new ValueEnvironment<>(lattice.top(), null, lattice.top());
	}

	@Override
	public ValueEnvironment<T> bottom() {
		return isBottom() ? this : new ValueEnvironment<>(lattice.bottom(), null, lattice.bottom());
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
		if (!(obj instanceof ValueEnvironment))
			return false;
		ValueEnvironment<?> other = (ValueEnvironment<?>) obj;
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
}
