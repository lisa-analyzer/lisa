package it.unive.lisa.analysis;

import it.unive.lisa.symbolic.Identifier;
import it.unive.lisa.symbolic.Skip;
import it.unive.lisa.symbolic.SymbolicExpression;

/**
 * The abstract analysis state at a given program point. An analysis state is
 * composed by an {@link AbstractState} modeling the abstract values of program
 * variables and heap locations, and a {@link SymbolicExpression} keeping trace
 * of what has been evaluated and is available for later computations, but is
 * not stored in memory (i.e. the stack).
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
	 * The last expression that has been computed, carrying over abstract values
	 */
	private final SymbolicExpression lastComputedExpression;

	/**
	 * Builds a new state.
	 * 
	 * @param state                  the {@link AbstractState} to embed in this
	 *                               analysis state
	 * @param lastComputedExpression the last expression that has been computed
	 */
	public AnalysisState(AbstractState<H, V> state, SymbolicExpression lastComputedExpression) {
		this.state = state;
		this.lastComputedExpression = lastComputedExpression;
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
	 * of expression that abstract domains are able to interpret.
	 * 
	 * @return the last computed expression
	 */
	public SymbolicExpression getLastComputedExpression() {
		return lastComputedExpression;
	}

	@Override
	public AnalysisState<H, V> assign(Identifier id, SymbolicExpression value) {
		return new AnalysisState<>(getState().assign(id, value), (SymbolicExpression) id);
	}

	@Override
	public AnalysisState<H, V> smallStepSemantics(SymbolicExpression expression) {
		// TODO should we return an expression that contains the value?
		// answer: yes, but how to do this
		return new AnalysisState<>(state.smallStepSemantics(expression), new Skip());
	}

	@Override
	public AnalysisState<H, V> assume(SymbolicExpression expression) {
		return new AnalysisState<>(state.assume(expression), new Skip());
	}

	@Override
	public Satisfiability satisfy(SymbolicExpression expression) {
		return state.satisfy(expression);
	}

	@Override
	public AnalysisState<H, V> lub(AnalysisState<H, V> other) {
		// TODO should we perform some check on the expression too?
		return new AnalysisState<>(state.lub(other.state), lastComputedExpression);
	}

	@Override
	public AnalysisState<H, V> widening(AnalysisState<H, V> other) {
		// TODO should we perform some check on the expression too?
		return new AnalysisState<>(state.widening(other.state), lastComputedExpression);
	}

	@Override
	public boolean lessOrEqual(AnalysisState<H, V> other) {
		// TODO should we perform some check on the expression too?
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
		return state.isTop() && lastComputedExpression instanceof Skip;
	}
	
	@Override
	public boolean isBottom() {
		return state.isBottom() && lastComputedExpression instanceof Skip;
	}
}
