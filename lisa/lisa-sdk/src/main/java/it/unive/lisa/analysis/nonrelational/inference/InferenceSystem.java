package it.unive.lisa.analysis.nonrelational.inference;

import java.util.Map;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.nonrelational.VariableLift;
import it.unive.lisa.analysis.nonrelational.inference.InferredValue.InferredPair;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.util.representation.ObjectRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;

/**
 * An inference system that model standard derivation systems. An inference
 * system is a {@link VariableLift} that works on {@link InferredValue}s, and
 * that exposes an execution state ({@link #getExecutionState()}).
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <T> the type of {@link InferredValue} in this inference system
 */
public class InferenceSystem<T extends InferredValue<T>>
		extends
		VariableLift<InferenceSystem<T>, ValueExpression, T>
		implements
		ValueDomain<InferenceSystem<T>> {

	private final T state;

	/**
	 * Builds an empty inference system.
	 * 
	 * @param domain a singleton instance to be used during semantic operations
	 *                   to retrieve top and bottom values
	 */
	public InferenceSystem(
			T domain) {
		super(domain);
		state = domain.bottom();
	}

	/**
	 * Builds an inference system identical to the given one, except for the
	 * execution state that will be set to the given one.
	 * 
	 * @param other the inference system to copy
	 * @param state the new execution state
	 */
	public InferenceSystem(
			InferenceSystem<T> other,
			T state) {
		this(other.lattice, other.function, state);
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
	 * @param state    the execution state after the last computed expression
	 */
	public InferenceSystem(
			T domain,
			Map<Identifier, T> function,
			T state) {
		super(domain, function);
		this.state = state;
	}

	@Override
	public InferenceSystem<T> mk(
			T lattice,
			Map<Identifier, T> function) {
		return new InferenceSystem<>(lattice, function, state);
	}

	@Override
	public InferenceSystem<T> assign(
			Identifier id,
			ValueExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (isBottom() || !lattice.canProcess(expression, pp, oracle) || !lattice.tracksIdentifiers(id, pp, oracle))
			return this;

		Map<Identifier, T> func = mkNewFunction(function, false);
		InferredPair<T> eval = lattice.eval(expression, this, pp, oracle);
		T value = eval.getInferred();
		T v = lattice.fixedVariable(id, pp, oracle);
		if (!v.isBottom())
			// some domains might provide fixed representations
			// for some variables
			value = v;
		if (id.isWeak() && function != null && function.containsKey(id))
			// if we have a weak identifier for which we already have
			// information, we we perform a weak assignment
			value = value.lub(getState(id));
		func.put(id, value);
		return new InferenceSystem<>(lattice, func, eval.getState());
	}

	/**
	 * Yields the execution state (also called program counter), that might
	 * change when evaluating an expression.
	 * 
	 * @return the execution state
	 */
	public T getExecutionState() {
		return state;
	}

	@Override
	public InferenceSystem<T> smallStepSemantics(
			ValueExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (isBottom())
			return this;
		return new InferenceSystem<>(lattice, function, lattice.eval(expression, this, pp, oracle).getState());
	}

	@Override
	public InferenceSystem<T> assume(
			ValueExpression expression,
			ProgramPoint src,
			ProgramPoint dest,
			SemanticOracle oracle)
			throws SemanticException {
		if (isBottom())
			return this;

		Satisfiability sat = lattice.satisfies(expression, this, src, oracle);
		if (sat == Satisfiability.NOT_SATISFIED)
			return bottom();

		if (sat == Satisfiability.SATISFIED)
			return new InferenceSystem<>(lattice, function, lattice.eval(expression, this, src, oracle).getState());

		return lattice.assume(this, expression, src, dest, oracle);
	}

	/**
	 * Evaluates the given expression to an abstract value.
	 * 
	 * @param expression the expression to evaluate
	 * @param pp         the program point where the evaluation happens
	 * 
	 * @return the abstract result of the evaluation
	 * 
	 * @throws SemanticException if an error happens during the evaluation
	 */
	public T eval(
			ValueExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return lattice.eval(expression, this, pp, oracle).getInferred();
	}

	@Override
	public InferenceSystem<T> top() {
		return new InferenceSystem<>(lattice.top(), null, state.top());
	}

	@Override
	public InferenceSystem<T> bottom() {
		return new InferenceSystem<>(lattice.bottom(), null, state.bottom());
	}

	@Override
	public boolean isTop() {
		return super.isTop() && state.isTop();
	}

	@Override
	public boolean isBottom() {
		return super.isBottom() && state.isBottom();
	}

	@Override
	public InferenceSystem<T> lubAux(
			InferenceSystem<T> other)
			throws SemanticException {
		InferenceSystem<T> newEnv = super.lubAux(other);
		return new InferenceSystem<>(newEnv.lattice, newEnv.function, state.lub(other.state));
	}

	@Override
	public InferenceSystem<T> wideningAux(
			InferenceSystem<T> other)
			throws SemanticException {
		InferenceSystem<T> newEnv = super.wideningAux(other);
		return new InferenceSystem<>(newEnv.lattice, newEnv.function, state.widening(other.state));
	}

	@Override
	public InferenceSystem<T> glbAux(
			InferenceSystem<T> other)
			throws SemanticException {
		InferenceSystem<T> newEnv = super.glbAux(other);
		return new InferenceSystem<>(newEnv.lattice, newEnv.function, state.glb(other.state));
	}

	@Override
	public InferenceSystem<T> narrowingAux(
			InferenceSystem<T> other)
			throws SemanticException {
		InferenceSystem<T> newEnv = super.narrowingAux(other);
		return new InferenceSystem<>(newEnv.lattice, newEnv.function, state.narrowing(other.state));
	}

	@Override
	public boolean lessOrEqualAux(
			InferenceSystem<T> other)
			throws SemanticException {
		if (!super.lessOrEqualAux(other))
			return false;

		return state.lessOrEqual(other.state);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((state == null) ? 0 : state.hashCode());
		return result;
	}

	@Override
	public boolean equals(
			Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		InferenceSystem<?> other = (InferenceSystem<?>) obj;
		if (state == null) {
			if (other.state != null)
				return false;
		} else if (!state.equals(other.state))
			return false;
		return true;
	}

	@Override
	public StructuredRepresentation representation() {
		if (isBottom() || isTop())
			return super.representation();

		return new ObjectRepresentation(Map.of("map", super.representation(), "state", state.representation()));
	}
}
