package it.unive.lisa.analysis;

import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.analysis.representation.DomainRepresentation;
import it.unive.lisa.analysis.representation.SetRepresentation;
import it.unive.lisa.analysis.representation.StringRepresentation;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Identifier;
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
		extends BaseLattice<AnalysisState<A, H, V>> implements
		SemanticDomain<AnalysisState<A, H, V>, SymbolicExpression, Identifier> {

	/**
	 * The abstract state of program variables and memory locations
	 */
	private final A state;

	/**
	 * The last expressions that have been computed, representing side-effect
	 * free expressions that are pending evaluation
	 */
	private final ExpressionSet<SymbolicExpression> computedExpressions;

	/**
	 * Builds a new state.
	 * 
	 * @param state              the {@link AbstractState} to embed in this
	 *                               analysis state
	 * @param computedExpression the expression that has been computed
	 */
	public AnalysisState(A state, SymbolicExpression computedExpression) {
		this(state, new ExpressionSet<>(computedExpression));
	}

	/**
	 * Builds a new state.
	 * 
	 * @param state               the {@link AbstractState} to embed in this
	 *                                analysis state
	 * @param computedExpressions the expressions that have been computed
	 */
	public AnalysisState(A state, ExpressionSet<SymbolicExpression> computedExpressions) {
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
	public ExpressionSet<SymbolicExpression> getComputedExpressions() {
		return computedExpressions;
	}

	@Override
	public AnalysisState<A, H, V> assign(Identifier id, SymbolicExpression value, ProgramPoint pp)
			throws SemanticException {
		A s = state.assign(id, value, pp);
		return new AnalysisState<A, H, V>(s, new ExpressionSet<>(id));
	}

	public AnalysisState<A, H, V> assign(SymbolicExpression id, SymbolicExpression value, ProgramPoint pp)
			throws SemanticException {

		if (id instanceof Identifier)
			return assign((Identifier) id, value, pp);

		A s = (A) state.bottom();
		ExpressionSet<SymbolicExpression> rewritten = rewrite(id, pp);
		for (SymbolicExpression i : rewritten)
			s = s.lub(state.assign((Identifier) i, value, pp));
		return new AnalysisState<A, H, V>(s, rewritten);
	}

	@Override
	public AnalysisState<A, H, V> smallStepSemantics(SymbolicExpression expression, ProgramPoint pp)
			throws SemanticException {
		A s = state.smallStepSemantics(expression, pp);
		return new AnalysisState<>(s, new ExpressionSet<>(expression));
	}

	private ExpressionSet<SymbolicExpression> rewrite(SymbolicExpression expression, ProgramPoint pp)
			throws SemanticException {
		return new ExpressionSet<>(
				getState().getHeapState().rewrite(expression, pp).elements()
						.stream()
						.map(e -> (SymbolicExpression) e).collect(Collectors.toSet()));
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
	public AnalysisState<A, H, V> pushScope(ScopeToken scope) throws SemanticException {
		return new AnalysisState<A, H, V>(state.pushScope(scope),
				onAllExpressions(this.computedExpressions, scope, true));
	}

	private ExpressionSet<SymbolicExpression> onAllExpressions(ExpressionSet<SymbolicExpression> computedExpressions,
			ScopeToken scope, boolean push) throws SemanticException {
		Set<SymbolicExpression> result = new HashSet<>();
		for (SymbolicExpression exp : computedExpressions)
			result.add(push ? exp.pushScope(scope) : exp.popScope(scope));
		return new ExpressionSet<>(result);
	}

	@Override
	public AnalysisState<A, H, V> popScope(ScopeToken scope) throws SemanticException {
		return new AnalysisState<A, H, V>(state.popScope(scope),
				onAllExpressions(this.computedExpressions, scope, false));
	}

	@Override
	public AnalysisState<A, H, V> lubAux(AnalysisState<A, H, V> other) throws SemanticException {
		return new AnalysisState<>(state.lub(other.state), computedExpressions.lub(other.computedExpressions));
	}

	@Override
	public AnalysisState<A, H, V> wideningAux(AnalysisState<A, H, V> other) throws SemanticException {
		return new AnalysisState<>(state.widening(other.state), computedExpressions.lub(other.computedExpressions));
	}

	@Override
	public boolean lessOrEqualAux(AnalysisState<A, H, V> other) throws SemanticException {
		return state.lessOrEqual(other.state);
	}

	@Override
	public AnalysisState<A, H, V> top() {
		return new AnalysisState<>(state.top(), new ExpressionSet<>());
	}

	@Override
	public AnalysisState<A, H, V> bottom() {
		return new AnalysisState<>(state.bottom(), new ExpressionSet<>());
	}

	@Override
	public boolean isTop() {
		// we do not check the computed expressions since we still have to
		// track what is on the stack even if it's the top state
		return state.isTop();
	}

	@Override
	public boolean isBottom() {
		// we do not check the computed expressions since we still have to
		// track what is on the stack even if it's the bottom state
		return state.isBottom();
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

	@Override
	public DomainRepresentation representation() {
		return new AnalysisStateRepresentation(state.representation(),
				new SetRepresentation(computedExpressions.elements(), StringRepresentation::new));
	}

	private static class AnalysisStateRepresentation extends DomainRepresentation {
		private final DomainRepresentation state;
		private final DomainRepresentation expressions;

		private AnalysisStateRepresentation(DomainRepresentation state, DomainRepresentation expressions) {
			this.state = state;
			this.expressions = expressions;
		}

		@Override
		public String toString() {
			return "{{\n" + state + "\n}} -> " + expressions;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((expressions == null) ? 0 : expressions.hashCode());
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
			AnalysisStateRepresentation other = (AnalysisStateRepresentation) obj;
			if (expressions == null) {
				if (other.expressions != null)
					return false;
			} else if (!expressions.equals(other.expressions))
				return false;
			if (state == null) {
				if (other.state != null)
					return false;
			} else if (!state.equals(other.state))
				return false;
			return true;
		}
	}

	@Override
	public String toString() {
		return representation().toString();
	}
}
