package it.unive.lisa.analysis;

import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.lattices.SymbolicExpressionSet;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.Skip;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The abstract analysis state at a given program point. An analysis state is
 * composed by an {@link AbstractState} modeling the abstract values of program
 * variables and heap locations, and a collection of {@link SymbolicExpression}s
 * keeping trace of what has been evaluated and is available for later
 * computations, but is not stored in memory (i.e. the stack).
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <A> the type of {@link AbstractState} embedded in this state
 * @param <H> the type of {@link HeapDomain} embedded in the abstract state
 * @param <V> the type of {@link ValueDomain} embedded in the abstract state
 */
public class AnalysisState<A extends AbstractState<A, H, V>, H extends HeapDomain<H>, V extends ValueDomain<V>>
		implements Lattice<AnalysisState<A, H, V>>,
		SemanticDomain<AnalysisState<A, H, V>, SymbolicExpression, Identifier> {

	/**
	 * The abstract state of program variables and memory locations
	 */
	private final A state;

	/**
	 * The last expressions that have been computed, representing side-effect
	 * free expressions that are pending evaluation
	 */
	private final SymbolicExpressionSet computedExpressions;

	/**
	 * Builds a new state.
	 * 
	 * @param state              the {@link AbstractState} to embed in this
	 *                               analysis state
	 * @param computedExpression the expression that has been computed
	 */
	public AnalysisState(A state, SymbolicExpression computedExpression) {
		this(state, new SymbolicExpressionSet(computedExpression));
	}

	/**
	 * Builds a new state.
	 * 
	 * @param state               the {@link AbstractState} to embed in this
	 *                                analysis state
	 * @param computedExpressions the expressions that have been computed
	 */
	public AnalysisState(A state, SymbolicExpressionSet computedExpressions) {
		this.state = state;
		this.computedExpressions = computedExpressions;
	}

	/**
	 * Yields the {@link AbstractState} embedded into this analysis state,
	 * containing abstract values for program variables and memory locations.
	 * 
	 * @return the abstract state
	 */
	public A getState() {
		return state;
	}

	/**
	 * Yields the last computed expression. This is an instance of
	 * {@link SymbolicExpression} that will contain markers for all abstract
	 * values that would be present on the stack, as well as variable
	 * identifiers for values that should be read from the state. These are tied
	 * together in a form of expression that abstract domains are able to
	 * interpret. The collection returned by this method usually contains one
	 * expression, but instances created through lattice operations (e.g., lub)
	 * might contain more.
	 * 
	 * @return the last computed expression
	 */
	public SymbolicExpressionSet getComputedExpressions() {
		return computedExpressions;
	}

	@Override
	public AnalysisState<A, H, V> assign(Identifier id, SymbolicExpression value, ProgramPoint pp)
			throws SemanticException {
		A s = state.assign(id, value, pp);
		SymbolicExpressionSet exprs = new SymbolicExpressionSet(
				s.getHeapState().smallStepSemantics(id, pp).getRewrittenExpressions()
						.stream()
						.map(e -> (SymbolicExpression) e).collect(Collectors.toSet()));
		return new AnalysisState<A, H, V>(s, exprs);
	}

	@Override
	public AnalysisState<A, H, V> smallStepSemantics(SymbolicExpression expression, ProgramPoint pp)
			throws SemanticException {
		A s = state.smallStepSemantics(expression, pp);
		SymbolicExpressionSet exprs = new SymbolicExpressionSet(s.getHeapState().getRewrittenExpressions().stream()
				.map(e -> (SymbolicExpression) e).collect(Collectors.toSet()));
		return new AnalysisState<>(s, exprs);
	}

	@Override
	public AnalysisState<A, H, V> assume(SymbolicExpression expression, ProgramPoint pp) throws SemanticException {
		return new AnalysisState<>(state.assume(expression, pp), computedExpressions);
	}

	@Override
	public Satisfiability satisfies(SymbolicExpression expression, ProgramPoint pp) throws SemanticException {
		return state.satisfies(expression, pp);
	}

	@Override
	public AnalysisState<A, H, V> lub(AnalysisState<A, H, V> other) throws SemanticException {
		return new AnalysisState<>(state.lub(other.state),
				lubRewrittenExpressions(computedExpressions, other.computedExpressions));
	}

	@Override
	public AnalysisState<A, H, V> widening(AnalysisState<A, H, V> other) throws SemanticException {
		return new AnalysisState<>(state.widening(other.state),
				lubRewrittenExpressions(computedExpressions, other.computedExpressions));
	}

	@Override
	public boolean lessOrEqual(AnalysisState<A, H, V> other) throws SemanticException {
		return state.lessOrEqual(other.state);
	}

	@Override
	public AnalysisState<A, H, V> top() {
		return new AnalysisState<>(state.top(), new Skip());
	}

	@Override
	public AnalysisState<A, H, V> bottom() {
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
	public AnalysisState<A, H, V> forgetIdentifier(Identifier id) throws SemanticException {
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
		AnalysisState<?, ?, ?> other = (AnalysisState<?, ?, ?>) obj;
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

	private SymbolicExpressionSet lubRewrittenExpressions(SymbolicExpressionSet r1,
			SymbolicExpressionSet r2) throws SemanticException {
		Set<SymbolicExpression> rewritten = new HashSet<>();
		rewritten.addAll(r1.elements().stream().filter(e1 -> !(e1 instanceof Identifier)).collect(Collectors.toSet()));
		rewritten.addAll(r2.elements().stream().filter(e2 -> !(e2 instanceof Identifier)).collect(Collectors.toSet()));

		for (Identifier id1 : r1.elements().stream().filter(t -> t instanceof Identifier).map(Identifier.class::cast)
				.collect(Collectors.toSet()))
			for (Identifier id2 : r2.elements().stream().filter(t -> t instanceof Identifier)
					.map(Identifier.class::cast)
					.collect(Collectors.toSet()))
				if (id1.equals(id2))
					rewritten.add(id1.lub(id2));
				else if (!r1.contains(id2))
					rewritten.add(id2);

		return new SymbolicExpressionSet(rewritten);
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
