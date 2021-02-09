package it.unive.lisa.analysis.nonrelational.inference;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.ValueDomain;
import it.unive.lisa.analysis.nonrelational.Environment;
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
	public String representation() {
		if (isBottom() || isTop())
			return super.representation();

		return super.representation() + "\n[inferred: " + inferredValue + ", state: " + inferredValue.executionState()
				+ "]";
	}
}