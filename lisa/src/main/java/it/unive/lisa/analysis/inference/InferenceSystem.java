package it.unive.lisa.analysis.inference;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.nonrelational.Environment;
import it.unive.lisa.analysis.representation.DomainRepresentation;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.ValueExpression;
import java.util.Map;

/**
 * An inference system that model standard derivation systems (e.g., types
 * systems, small step semantics, big step semantics, ...). An inference system
 * is an {@link Environment} that work on {@link InferredValue}s, and that
 * exposes the last inferred value ({@link #getInferredValue()}) and the
 * execution state ({@link #getExecutionState()}).
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <T> the type of {@link InferredValue} in this inference system
 */
public class InferenceSystem<T extends InferredValue<T>> extends Environment<InferenceSystem<T>, ValueExpression, T>
		implements ValueDomain<InferenceSystem<T>> {

	private final T inferredValue;

	/**
	 * Builds an empty inference system.
	 * 
	 * @param domain a singleton instance to be used during semantic operations
	 *                   to retrieve top and bottom values
	 */
	public InferenceSystem(T domain) {
		super(domain);
		inferredValue = domain.bottom();
	}

	private InferenceSystem(T domain, Map<Identifier, T> function) {
		this(domain, function, domain.bottom());
	}

	private InferenceSystem(T domain, Map<Identifier, T> function, T inferredValue) {
		super(domain, function);
		this.inferredValue = inferredValue;
	}

	/**
	 * Yields the execution state (also called program counter), that might
	 * change when evaluating an expression.
	 * 
	 * @return the execution state
	 */
	public T getExecutionState() {
		return inferredValue.executionState();
	}

	/**
	 * Yields the inferred value of the last {@link SymbolicExpression} handled
	 * by this domain, either through
	 * {@link #assign(Identifier, SymbolicExpression, ProgramPoint)} or
	 * {@link #smallStepSemantics(ValueExpression, ProgramPoint)}.
	 * 
	 * @return the value inferred for the last expression
	 */
	public T getInferredValue() {
		return inferredValue;
	}

	@Override
	protected InferenceSystem<T> copy() {
		return new InferenceSystem<>(lattice, mkNewFunction(function), inferredValue);
	}

	@Override
	protected InferenceSystem<T> assignAux(Identifier id, ValueExpression value, Map<Identifier, T> function, T eval,
			ProgramPoint pp) {
		T v = lattice.variable(id, pp);
		if (!v.isBottom())
			function.put(id, v);
		return new InferenceSystem<>(lattice, function, eval);
	}

	@Override
	public InferenceSystem<T> smallStepSemantics(ValueExpression expression, ProgramPoint pp) throws SemanticException {
		// we update the inferred value
		return new InferenceSystem<>(lattice, function, lattice.eval(expression, this, pp));
	}

	@Override
	public InferenceSystem<T> top() {
		return isTop() ? this : new InferenceSystem<T>(lattice.top(), null);
	}

	@Override
	public InferenceSystem<T> bottom() {
		return isBottom() ? this : new InferenceSystem<T>(lattice.bottom(), null);
	}

	@Override
	public InferenceSystem<T> lubAux(InferenceSystem<T> other) throws SemanticException {
		InferenceSystem<T> lub = super.lubAux(other);
		if (lub.isTop() || lub.isBottom())
			return lub;
		return new InferenceSystem<>(lub.lattice, lub.function, inferredValue.lub(other.inferredValue));
	}

	@Override
	public InferenceSystem<T> wideningAux(InferenceSystem<T> other) throws SemanticException {
		InferenceSystem<T> widen = super.wideningAux(other);
		if (widen.isTop() || widen.isBottom())
			return widen;
		return new InferenceSystem<>(widen.lattice, widen.function, inferredValue.widening(other.inferredValue));
	}

	@Override
	public boolean lessOrEqualAux(InferenceSystem<T> other) throws SemanticException {
		if (!super.lessOrEqualAux(other))
			return false;

		return inferredValue.lessOrEqual(other.inferredValue);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((inferredValue == null) ? 0 : inferredValue.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		@SuppressWarnings("unchecked")
		InferenceSystem<T> other = (InferenceSystem<T>) obj;
		if (inferredValue == null) {
			if (other.inferredValue != null)
				return false;
		} else if (!inferredValue.equals(other.inferredValue))
			return false;
		return true;
	}

	@Override
	public DomainRepresentation representation() {
		if (isBottom() || isTop())
			return super.representation();

		return new SystemRepresentation(super.representation(), inferredValue.representation(),
				inferredValue.executionState().representation());
	}

	private static class SystemRepresentation extends DomainRepresentation {

		private final DomainRepresentation map;
		private final DomainRepresentation inferred;
		private final DomainRepresentation state;

		public SystemRepresentation(DomainRepresentation map, DomainRepresentation inferred,
				DomainRepresentation state) {
			this.map = map;
			this.inferred = inferred;
			this.state = state;
		}

		@Override
		public String toString() {
			return map + "\n[inferred: " + inferred + ", state: " + state + "]";
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((inferred == null) ? 0 : inferred.hashCode());
			result = prime * result + ((map == null) ? 0 : map.hashCode());
			result = prime * result + ((state == null) ? 0 : state.hashCode());
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
			SystemRepresentation other = (SystemRepresentation) obj;
			if (inferred == null) {
				if (other.inferred != null)
					return false;
			} else if (!inferred.equals(other.inferred))
				return false;
			if (map == null) {
				if (other.map != null)
					return false;
			} else if (!map.equals(other.map))
				return false;
			if (state == null) {
				if (other.state != null)
					return false;
			} else if (!state.equals(other.state))
				return false;
			return true;
		}
	}
}