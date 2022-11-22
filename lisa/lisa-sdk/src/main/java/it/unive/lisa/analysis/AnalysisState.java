package it.unive.lisa.analysis;

import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.analysis.representation.DomainRepresentation;
import it.unive.lisa.analysis.representation.ObjectRepresentation;
import it.unive.lisa.analysis.symbols.Symbol;
import it.unive.lisa.analysis.symbols.SymbolAliasing;
import it.unive.lisa.analysis.value.TypeDomain;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.ValueExpression;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

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
 * @param <T> the type of {@link TypeDomain} embedded in the abstract state
 */
public class AnalysisState<A extends AbstractState<A, H, V, T>,
		H extends HeapDomain<H>,
		V extends ValueDomain<V>,
		T extends TypeDomain<T>>
		extends BaseLattice<AnalysisState<A, H, V, T>> implements
		SemanticDomain<AnalysisState<A, H, V, T>, SymbolicExpression, Identifier> {

	/**
	 * The abstract state of program variables and memory locations
	 */
	private final A state;

	/**
	 * The lattice that handles symbol aliasing
	 */
	private final SymbolAliasing aliasing;

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
	 * @param aliasing           the symbol aliasing information
	 */
	public AnalysisState(A state, SymbolicExpression computedExpression, SymbolAliasing aliasing) {
		this(state, new ExpressionSet<>(computedExpression), aliasing);
	}

	/**
	 * Builds a new state.
	 * 
	 * @param state               the {@link AbstractState} to embed in this
	 *                                analysis state
	 * @param computedExpressions the expressions that have been computed
	 * @param aliasing            the symbol aliasing information
	 */
	public AnalysisState(A state, ExpressionSet<SymbolicExpression> computedExpressions, SymbolAliasing aliasing) {
		this.state = state;
		this.computedExpressions = computedExpressions;
		this.aliasing = aliasing;
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
	 * Yields the symbol aliasing information, that can be used to resolve
	 * targets of calls when the names used in the call are different from the
	 * ones in the target's signature.
	 * 
	 * @return the aliasing information
	 */
	public SymbolAliasing getAliasing() {
		return aliasing;
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

	/**
	 * Registers an alias for the given symbol. Any previous aliases will be
	 * deleted.
	 * 
	 * @param toAlias the symbol being aliased
	 * @param alias   the alias for {@code toAlias}
	 * 
	 * @return a copy of this analysis state, with the new alias
	 */
	public AnalysisState<A, H, V, T> alias(Symbol toAlias, Symbol alias) {
		SymbolAliasing aliasing = this.aliasing.putState(toAlias, alias);
		return new AnalysisState<>(state, computedExpressions, aliasing);
	}

	@Override
	public AnalysisState<A, H, V, T> assign(Identifier id, SymbolicExpression value, ProgramPoint pp)
			throws SemanticException {
		A s = state.assign(id, value, pp);
		return new AnalysisState<>(s, new ExpressionSet<>(id), aliasing);
	}

	/**
	 * Yields a copy of this analysis state, where the symbolic expression
	 * {@code id} has been assigned to {@code value}: if {@code id} is not an
	 * {@code Identifier}, then it is rewritten before performing the
	 * assignment.
	 * 
	 * @param id         the symbolic expression to be assigned
	 * @param expression the expression to assign
	 * @param pp         the program point that where this operation is being
	 *                       evaluated
	 * 
	 * @return a copy of this analysis state, modified by the assignment
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	public AnalysisState<A, H, V, T> assign(SymbolicExpression id, SymbolicExpression expression, ProgramPoint pp)
			throws SemanticException {

		if (id instanceof Identifier)
			return assign((Identifier) id, expression, pp);

		A s = state.bottom();
		ExpressionSet<SymbolicExpression> rewritten = rewrite(id, pp);
		for (SymbolicExpression i : rewritten)
			s = s.lub(state.assign((Identifier) i, expression, pp));
		return new AnalysisState<>(s, rewritten, aliasing);
	}

	@Override
	public AnalysisState<A, H, V, T> smallStepSemantics(SymbolicExpression expression, ProgramPoint pp)
			throws SemanticException {
		A s = state.smallStepSemantics(expression, pp);
		return new AnalysisState<>(s, new ExpressionSet<>(expression), aliasing);
	}

	private ExpressionSet<SymbolicExpression> rewrite(SymbolicExpression expression, ProgramPoint pp)
			throws SemanticException {
		Set<SymbolicExpression> rewritten = new HashSet<>();
		@SuppressWarnings("unchecked")
		ExpressionSet<ValueExpression> tmp = getState().getDomainInstance(HeapDomain.class).rewrite(expression, pp);
		tmp.elements()
				.stream()
				.map(SymbolicExpression.class::cast)
				.forEach(rewritten::add);
		return new ExpressionSet<>(rewritten);
	}

	@Override
	public AnalysisState<A, H, V, T> assume(SymbolicExpression expression, ProgramPoint pp) throws SemanticException {
		return new AnalysisState<>(state.assume(expression, pp), computedExpressions, aliasing);
	}

	@Override
	public Satisfiability satisfies(SymbolicExpression expression, ProgramPoint pp) throws SemanticException {
		return state.satisfies(expression, pp);
	}

	@Override
	public AnalysisState<A, H, V, T> pushScope(ScopeToken scope) throws SemanticException {
		return new AnalysisState<>(
				state.pushScope(scope),
				onAllExpressions(this.computedExpressions, scope, true),
				aliasing);
	}

	private static ExpressionSet<SymbolicExpression> onAllExpressions(
			ExpressionSet<SymbolicExpression> computedExpressions,
			ScopeToken scope, boolean push) throws SemanticException {
		Set<SymbolicExpression> result = new HashSet<>();
		for (SymbolicExpression exp : computedExpressions)
			result.add(push ? exp.pushScope(scope) : exp.popScope(scope));
		return new ExpressionSet<>(result);
	}

	@Override
	public AnalysisState<A, H, V, T> popScope(ScopeToken scope) throws SemanticException {
		return new AnalysisState<>(
				state.popScope(scope),
				onAllExpressions(this.computedExpressions, scope, false),
				aliasing);
	}

	@Override
	public AnalysisState<A, H, V, T> lubAux(AnalysisState<A, H, V, T> other) throws SemanticException {
		return new AnalysisState<>(
				state.lub(other.state),
				computedExpressions.lub(other.computedExpressions),
				aliasing.lub(other.aliasing));
	}

	@Override
	public AnalysisState<A, H, V, T> glbAux(AnalysisState<A, H, V, T> other) throws SemanticException {
		return new AnalysisState<>(
				state.glb(other.state),
				computedExpressions.glb(other.computedExpressions),
				aliasing.glb(other.aliasing));
	}

	@Override
	public AnalysisState<A, H, V, T> wideningAux(AnalysisState<A, H, V, T> other) throws SemanticException {
		return new AnalysisState<>(
				state.widening(other.state),
				computedExpressions.lub(other.computedExpressions),
				aliasing.widening(other.aliasing));
	}

	@Override
	public AnalysisState<A, H, V, T> narrowingAux(AnalysisState<A, H, V, T> other) throws SemanticException {
		return new AnalysisState<>(
				state.narrowing(other.state),
				computedExpressions.glb(other.computedExpressions),
				aliasing.narrowing(other.aliasing));
	}

	@Override
	public boolean lessOrEqualAux(AnalysisState<A, H, V, T> other) throws SemanticException {
		return state.lessOrEqual(other.state)
				&& computedExpressions.lessOrEqual(other.computedExpressions)
				&& aliasing.lessOrEqual(other.aliasing);
	}

	@Override
	public AnalysisState<A, H, V, T> top() {
		return new AnalysisState<>(state.top(), computedExpressions.top(), aliasing.top());
	}

	@Override
	public AnalysisState<A, H, V, T> bottom() {
		return new AnalysisState<>(state.bottom(), computedExpressions.bottom(), aliasing.bottom());
	}

	@Override
	public boolean isTop() {
		return state.isTop() && computedExpressions.isTop() && aliasing.isTop();
	}

	@Override
	public boolean isBottom() {
		return state.isBottom() && computedExpressions.isBottom() && aliasing.isBottom();
	}

	@Override
	public AnalysisState<A, H, V, T> forgetIdentifier(Identifier id) throws SemanticException {
		return new AnalysisState<>(state.forgetIdentifier(id), computedExpressions, aliasing);
	}

	@Override
	public AnalysisState<A, H, V, T> forgetIdentifiersIf(Predicate<Identifier> test) throws SemanticException {
		return new AnalysisState<>(state.forgetIdentifiersIf(test), computedExpressions, aliasing);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((aliasing == null) ? 0 : aliasing.hashCode());
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
		AnalysisState<?, ?, ?, ?> other = (AnalysisState<?, ?, ?, ?>) obj;
		if (aliasing == null) {
			if (other.aliasing != null)
				return false;
		} else if (!aliasing.equals(other.aliasing))
			return false;
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
		DomainRepresentation stateRepr = state.representation();
		DomainRepresentation exprRepr = computedExpressions.representation();
		return new ObjectRepresentation(Map.of("state", stateRepr, "expressions", exprRepr));
	}

	@Override
	public String toString() {
		return representation().toString();
	}

	@Override
	@SuppressWarnings("unchecked")
	public <D> D getDomainInstance(Class<D> domain) {
		if (domain.isAssignableFrom(getClass()))
			return (D) this;

		return state.getDomainInstance(domain);
	}

	/**
	 * Yields a copy of this state, but with the {@link AbstractState}'s inner
	 * {@link HeapDomain} set to top.
	 * 
	 * @return the copy with top heap
	 */
	public AnalysisState<A, H, V, T> withTopHeap() {
		return new AnalysisState<>(state.withTopHeap(), computedExpressions, aliasing);
	}

	/**
	 * Yields a copy of this state, but with the {@link AbstractState}'s inner
	 * {@link ValueDomain} set to top.
	 * 
	 * @return the copy with top value
	 */
	public AnalysisState<A, H, V, T> withTopValue() {
		return new AnalysisState<>(state.withTopValue(), computedExpressions, aliasing);
	}

	/**
	 * Yields a copy of this state, but with the {@link AbstractState}'s inner
	 * {@link TypeDomain} set to top.
	 * 
	 * @return the copy with top type
	 */
	public AnalysisState<A, H, V, T> withTopType() {
		return new AnalysisState<>(state.withTopType(), computedExpressions, aliasing);
	}
}
