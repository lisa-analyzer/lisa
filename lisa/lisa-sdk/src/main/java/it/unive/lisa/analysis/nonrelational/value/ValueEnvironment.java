package it.unive.lisa.analysis.nonrelational.value;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.lattices.FunctionalLattice;
import it.unive.lisa.analysis.nonrelational.Environment;
import it.unive.lisa.analysis.representation.DomainRepresentation;
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

	private ValueEnvironment(T domain, Map<Identifier, T> function, T stack) {
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
	protected ValueEnvironment<T> mk(T lattice, Map<Identifier, T> function) {
		return new ValueEnvironment<>(lattice, function, stack);
	}

	@Override
	protected ValueEnvironment<T> copy() {
		return new ValueEnvironment<>(lattice, mkNewFunction(function), stack);
	}

	@Override
	protected Pair<T, T> eval(ValueExpression expression, ProgramPoint pp) throws SemanticException {
		T eval = lattice.eval(expression, this, pp);
		return Pair.of(eval, eval);
	}

	@Override
	protected ValueEnvironment<T> assignAux(Identifier id, ValueExpression expression, Map<Identifier, T> function,
			T value, T eval, ProgramPoint pp) {
		return new ValueEnvironment<>(lattice, function, value);
	}

	@Override
	public ValueEnvironment<T> smallStepSemantics(ValueExpression expression, ProgramPoint pp)
			throws SemanticException {
		return new ValueEnvironment<>(lattice, function, lattice.eval(expression, this, pp));
	}

	@Override
	protected ValueEnvironment<T> assumeSatisfied(T eval) {
		return this;
	}

	@Override
	protected ValueEnvironment<T> glbAux(T lattice, Map<Identifier, T> function, ValueEnvironment<T> other)
			throws SemanticException {
		return new ValueEnvironment<>(lattice, function, stack.glb(other.stack));
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

		return new ValueRepresentation(super.representation(), stack.representation());
	}

	private static class ValueRepresentation extends DomainRepresentation {

		private final DomainRepresentation map;
		private final DomainRepresentation stack;

		public ValueRepresentation(DomainRepresentation map, DomainRepresentation stack) {
			this.map = map;
			this.stack = stack;
		}

		@Override
		public String toString() {
			return map + "\n[stack: " + stack + "]";
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((stack == null) ? 0 : stack.hashCode());
			result = prime * result + ((map == null) ? 0 : map.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			ValueRepresentation other = (ValueRepresentation) obj;
			if (stack == null) {
				if (other.stack != null)
					return false;
			} else if (!stack.equals(other.stack))
				return false;
			if (map == null) {
				if (other.map != null)
					return false;
			} else if (!map.equals(other.map))
				return false;
			return true;
		}
	}
}
