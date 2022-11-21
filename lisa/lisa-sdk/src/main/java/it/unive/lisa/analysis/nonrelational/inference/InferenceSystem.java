package it.unive.lisa.analysis.nonrelational.inference;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.nonrelational.Environment;
import it.unive.lisa.analysis.nonrelational.inference.InferredValue.InferredPair;
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
public class InferenceSystem<T extends InferredValue<T>>
		extends Environment<InferenceSystem<T>, ValueExpression, T, InferredPair<T>>
		implements ValueDomain<InferenceSystem<T>> {

	private final InferredPair<T> inferred;

	/**
	 * Builds an empty inference system.
	 * 
	 * @param domain a singleton instance to be used during semantic operations
	 *                   to retrieve top and bottom values
	 */
	public InferenceSystem(T domain) {
		super(domain);
		inferred = new InferredPair<>(domain.bottom(), domain.bottom(), domain.bottom());
	}

	/**
	 * Builds an inference system identical to the given one, except for the
	 * execution state that will be set to the given one.
	 * 
	 * @param other the inference system to copy
	 * @param state the new execution state
	 */
	public InferenceSystem(InferenceSystem<T> other, T state) {
		this(other.lattice, other.function, new InferredPair<>(other.lattice, other.inferred.getInferred(), state));
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
	 * @param inferred the inferred pair for the last computed expression, that
	 *                     is left on the top of the stack
	 */
	public InferenceSystem(T domain, Map<Identifier, T> function, InferredPair<T> inferred) {
		super(domain, function);
		this.inferred = inferred;
	}

	@Override
	public InferenceSystem<T> mk(T lattice, Map<Identifier, T> function) {
		return new InferenceSystem<>(lattice, function, inferred);
	}

	/**
	 * Yields the execution state (also called program counter), that might
	 * change when evaluating an expression.
	 * 
	 * @return the execution state
	 */
	public T getExecutionState() {
		return inferred.getState();
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
		return inferred.getInferred();
	}

	@Override
	public Pair<T, InferredPair<T>> eval(ValueExpression expression, ProgramPoint pp) throws SemanticException {
		InferredPair<T> eval = lattice.eval(expression, this, pp);
		return Pair.of(eval.getInferred(), eval);
	}

	@Override
	public InferenceSystem<T> assignAux(Identifier id, ValueExpression expression, Map<Identifier, T> function,
			T value, InferredPair<T> eval, ProgramPoint pp) {
		return new InferenceSystem<>(lattice, function, new InferredPair<>(lattice, value, eval.getState()));
	}

	@Override
	public InferenceSystem<T> smallStepSemantics(ValueExpression expression, ProgramPoint pp) throws SemanticException {
		if (isBottom())
			return this;
		return new InferenceSystem<>(lattice, function, lattice.eval(expression, this, pp));
	}

	@Override
	public InferenceSystem<T> top() {
		return new InferenceSystem<>(lattice.top(), null, inferred.top());
	}

	@Override
	public InferenceSystem<T> bottom() {
		return new InferenceSystem<>(lattice.bottom(), null, inferred.bottom());
	}

	@Override
	public boolean isTop() {
		return super.isTop() && inferred.isTop();
	}

	@Override
	public boolean isBottom() {
		return super.isBottom() && inferred.isBottom();
	}

	@Override
	public InferenceSystem<T> lubAux(InferenceSystem<T> other)
			throws SemanticException {
		InferenceSystem<T> newEnv = functionalLift(other, this::lubKeys, (o1, o2) -> o1 == null ? o2 : o1.lub(o2));
		return new InferenceSystem<>(newEnv.lattice, newEnv.function, inferred.lub(other.inferred));
	}

	@Override
	public InferenceSystem<T> wideningAux(InferenceSystem<T> other) throws SemanticException {
		InferenceSystem<
				T> newEnv = functionalLift(other, this::lubKeys, (o1, o2) -> o1 == null ? o2 : o1.widening(o2));
		return new InferenceSystem<>(newEnv.lattice, newEnv.function, inferred.widening(other.inferred));
	}

	@Override
	public InferenceSystem<T> glbAux(InferenceSystem<T> other) throws SemanticException {
		InferenceSystem<T> newEnv = functionalLift(other, this::glbKeys, (o1, o2) -> o1 == null ? o2 : o1.glb(o2));
		return new InferenceSystem<>(newEnv.lattice, newEnv.function, inferred.glb(other.inferred));
	}

	@Override
	public InferenceSystem<T> narrowingAux(InferenceSystem<T> other) throws SemanticException {
		InferenceSystem<
				T> newEnv = functionalLift(other, this::glbKeys, (o1, o2) -> o1 == null ? o2 : o1.narrowing(o2));
		return new InferenceSystem<>(newEnv.lattice, newEnv.function, inferred.narrowing(other.inferred));
	}

	@Override
	public boolean lessOrEqualAux(InferenceSystem<T> other) throws SemanticException {
		if (!super.lessOrEqualAux(other))
			return false;

		return inferred.lessOrEqual(other.inferred);
	}

	@Override
	public InferenceSystem<T> assumeSatisfied(InferredPair<T> eval) {
		return new InferenceSystem<>(lattice, function,
				new InferredPair<>(lattice, eval.getInferred(), eval.getState()));
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((inferred == null) ? 0 : inferred.hashCode());
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
		InferenceSystem<?> other = (InferenceSystem<?>) obj;
		if (inferred == null) {
			if (other.inferred != null)
				return false;
		} else if (!inferred.equals(other.inferred))
			return false;
		return true;
	}

	@Override
	public DomainRepresentation representation() {
		if (isBottom() || isTop())
			return super.representation();

		return new ObjectRepresentation(Map.of("map", super.representation(), "inferred", inferred.representation()));
	}
}
