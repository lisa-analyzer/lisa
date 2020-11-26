package it.unive.lisa.analysis;

import java.util.Collection;
import java.util.Collections;

import org.apache.commons.collections.CollectionUtils;

import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.Skip;

/**
 * The abstract analysis state at a given program point. An analysis state is
 * composed by an {@link AbstractState} modeling the abstract values of program
 * variables and heap locations, and a collection of {@link SymbolicExpression}s
 * keeping trace of what has been evaluated and is available for later
 * computations, but is not stored in memory (i.e. the stack).
 * 
 * @param <H> the type of heap analysis embedded in the abstract state
 * @param <V> the type of value analysis embedded in the abstract state
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class AnalysisState<H extends HeapDomain<H>, V extends ValueDomain<V>>
		implements Lattice<AnalysisState<H, V>>, SemanticDomain<AnalysisState<H, V>, SymbolicExpression, Identifier> {

	/**
	 * The abstract state of program variables and memory locations
	 */
	private final AbstractState<H, V> state;

	/**
	 * The last expressions that have been computed, representing side-effect free
	 * expressions that are pending evaluation
	 */
	private final Collection<SymbolicExpression> computedExpressions;

	/**
	 * Builds a new state.
	 * 
	 * @param state              the {@link AbstractState} to embed in this analysis
	 *                           state
	 * @param computedExpression the expression that has been computed
	 */
	public AnalysisState(AbstractState<H, V> state, SymbolicExpression computedExpression) {
		this(state, Collections.singleton(computedExpression));
	}

	/**
	 * Builds a new state.
	 * 
	 * @param state               the {@link AbstractState} to embed in this
	 *                            analysis state
	 * @param computedExpressions the expressions that have been computed
	 */
	public AnalysisState(AbstractState<H, V> state, Collection<SymbolicExpression> computedExpressions) {
		this.state = state;
		this.computedExpressions = computedExpressions;
	}

	/**
	 * Yields the {@link AbstractState} embedded into this analysis state,
	 * containing abstract values for program variables and memory locations.
	 * 
	 * @return the abstract state
	 */
	public AbstractState<H, V> getState() {
		return state;
	}

	/**
	 * Yields the last computed expression. This is an instance of
	 * {@link SymbolicExpression} that will contain markers for all abstract values
	 * that would be present on the stack, as well as variable identifiers for
	 * values that should be read from the state. These are tied together in a form
	 * of expression that abstract domains are able to interpret. The collection
	 * returned by this method usually contains one expression, but instances
	 * created through lattice operations (e.g., lub) might contain more.
	 * 
	 * @return the last computed expression
	 */
	public Collection<SymbolicExpression> getComputedExpressions() {
		return computedExpressions;
	}

	@Override
	public AnalysisState<H, V> assign(Identifier id, SymbolicExpression value) throws SemanticException {
		return new AnalysisState<>(getState().assign(id, value), id);
	}

	@Override
	public AnalysisState<H, V> smallStepSemantics(SymbolicExpression expression) throws SemanticException {
		AbstractState<H, V> s = state.smallStepSemantics(expression);
		@SuppressWarnings("unchecked")
		Collection<SymbolicExpression> exprs = CollectionUtils.collect(s.getHeapState().getRewrittenExpressions(),
				e -> (SymbolicExpression) e);
		return new AnalysisState<>(s, exprs);
	}

	@Override
	public AnalysisState<H, V> assume(SymbolicExpression expression) throws SemanticException {
		return new AnalysisState<>(state.assume(expression), computedExpressions);
	}

	@Override
	public Satisfiability satisfies(SymbolicExpression expression) throws SemanticException {
		return state.satisfies(expression);
	}

	@Override
	@SuppressWarnings("unchecked")
	public AnalysisState<H, V> lub(AnalysisState<H, V> other) throws SemanticException {
		return new AnalysisState<>(state.lub(other.state),
				CollectionUtils.union(computedExpressions, other.computedExpressions));
	}

	@Override
	@SuppressWarnings("unchecked")
	public AnalysisState<H, V> widening(AnalysisState<H, V> other) throws SemanticException {
		return new AnalysisState<>(state.widening(other.state),
				CollectionUtils.union(computedExpressions, other.computedExpressions));
	}

	@Override
	public boolean lessOrEqual(AnalysisState<H, V> other) throws SemanticException {
		return state.lessOrEqual(other.state);
	}

	@Override
	public AnalysisState<H, V> top() {
		return new AnalysisState<>(state.top(), new Skip());
	}

	@Override
	public AnalysisState<H, V> bottom() {
		return new AnalysisState<>(state.bottom(), new Skip());
	}

	@Override
	public boolean isTop() {
		return state.isTop() && computedExpressions.size() == 1
				&& computedExpressions.iterator().next() instanceof Skip;
	}

	@Override
	public boolean isBottom() {
		return state.isBottom() && computedExpressions.size() == 1
				&& computedExpressions.iterator().next() instanceof Skip;
	}

	@Override
	public AnalysisState<H, V> forgetIdentifier(Identifier id) throws SemanticException {
		return new AnalysisState<>(state.forgetIdentifier(id), computedExpressions);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((computedExpressions == null) ? 0 : computedExpressions.hashCode());
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
		AnalysisState<?, ?> other = (AnalysisState<?, ?>) obj;
		if (computedExpressions == null) {
			if (other.computedExpressions != null)
				return false;
		} else if (!computedExpressions.equals(other.computedExpressions))
			return false;
		if (state == null) {
			if (other.state != null)
				return false;
		} else if (!state.equals(other.state))
			return false;
		return true;
	}

	@Override
	public String representation() {
		return "{{\n" + state + "\n}} -> " + computedExpressions;
	}

	@Override
	public String toString() {
		return representation();
	}
}
