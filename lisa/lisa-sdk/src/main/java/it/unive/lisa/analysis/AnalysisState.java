package it.unive.lisa.analysis;

import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.analysis.symbols.Symbol;
import it.unive.lisa.analysis.symbols.SymbolAliasing;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.util.representation.ObjectRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;
import java.util.Collection;
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
 */
public class AnalysisState<A extends AbstractState<A>>
		implements BaseLattice<AnalysisState<A>>,
		SemanticDomain<AnalysisState<A>, SymbolicExpression, Identifier> {

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
	private final ExpressionSet computedExpressions;

	/**
	 * Builds a new state.
	 * 
	 * @param state              the {@link AbstractState} to embed in this
	 *                               analysis state
	 * @param computedExpression the expression that has been computed
	 * @param aliasing           the symbol aliasing information
	 */
	public AnalysisState(A state, SymbolicExpression computedExpression, SymbolAliasing aliasing) {
		this(state, new ExpressionSet(computedExpression), aliasing);
	}

	/**
	 * Builds a new state.
	 * 
	 * @param state               the {@link AbstractState} to embed in this
	 *                                analysis state
	 * @param computedExpressions the expressions that have been computed
	 * @param aliasing            the symbol aliasing information
	 */
	public AnalysisState(A state, ExpressionSet computedExpressions, SymbolAliasing aliasing) {
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
	public ExpressionSet getComputedExpressions() {
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
	public AnalysisState<A> alias(Symbol toAlias, Symbol alias) {
		SymbolAliasing aliasing = this.aliasing.putState(toAlias, alias);
		return new AnalysisState<>(state, computedExpressions, aliasing);
	}

	@Override
	public AnalysisState<A> assign(Identifier id, SymbolicExpression value, ProgramPoint pp)
			throws SemanticException {
		A s = state.assign(id, value, pp);
		return new AnalysisState<>(s, new ExpressionSet(id), aliasing);
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
	public AnalysisState<A> assign(SymbolicExpression id, SymbolicExpression expression, ProgramPoint pp)
			throws SemanticException {

		if (id instanceof Identifier)
			return assign((Identifier) id, expression, pp);

		A s = state.bottom();
		ExpressionSet rewritten = state.rewrite(id, pp);
		for (SymbolicExpression i : rewritten)
			if (!(i instanceof Identifier))
				throw new SemanticException("Rewriting '" + id + "' did not produce an identifier: " + i);
			else
				s = s.lub(state.assign((Identifier) i, expression, pp));
		return new AnalysisState<>(s, rewritten, aliasing);
	}

	@Override
	public AnalysisState<A> smallStepSemantics(SymbolicExpression expression, ProgramPoint pp)
			throws SemanticException {
		A s = state.smallStepSemantics(expression, pp);
		return new AnalysisState<>(s, new ExpressionSet(expression), aliasing);
	}

	@Override
	public AnalysisState<A> assume(SymbolicExpression expression, ProgramPoint src, ProgramPoint dest)
			throws SemanticException {
		A assume = state.assume(expression, src, dest);
		if (assume.isBottom())
			return bottom();
		return new AnalysisState<>(assume, computedExpressions, aliasing);
	}

	@Override
	public Satisfiability satisfies(SymbolicExpression expression, ProgramPoint pp) throws SemanticException {
		return state.satisfies(expression, pp);
	}

	@Override
	public AnalysisState<A> pushScope(ScopeToken scope) throws SemanticException {
		return new AnalysisState<>(
				state.pushScope(scope),
				onAllExpressions(this.computedExpressions, scope, true),
				aliasing);
	}

	private static ExpressionSet onAllExpressions(
			ExpressionSet computedExpressions,
			ScopeToken scope, boolean push) throws SemanticException {
		Set<SymbolicExpression> result = new HashSet<>();
		for (SymbolicExpression exp : computedExpressions)
			result.add(push ? exp.pushScope(scope) : exp.popScope(scope));
		return new ExpressionSet(result);
	}

	@Override
	public AnalysisState<A> popScope(ScopeToken scope) throws SemanticException {
		return new AnalysisState<>(
				state.popScope(scope),
				onAllExpressions(this.computedExpressions, scope, false),
				aliasing);
	}

	@Override
	public AnalysisState<A> lubAux(AnalysisState<A> other) throws SemanticException {
		return new AnalysisState<>(
				state.lub(other.state),
				computedExpressions.lub(other.computedExpressions),
				aliasing.lub(other.aliasing));
	}

	@Override
	public AnalysisState<A> glbAux(AnalysisState<A> other) throws SemanticException {
		return new AnalysisState<>(
				state.glb(other.state),
				computedExpressions.glb(other.computedExpressions),
				aliasing.glb(other.aliasing));
	}

	@Override
	public AnalysisState<A> wideningAux(AnalysisState<A> other) throws SemanticException {
		return new AnalysisState<>(
				state.widening(other.state),
				computedExpressions.lub(other.computedExpressions),
				aliasing.widening(other.aliasing));
	}

	@Override
	public AnalysisState<A> narrowingAux(AnalysisState<A> other) throws SemanticException {
		return new AnalysisState<>(
				state.narrowing(other.state),
				computedExpressions.glb(other.computedExpressions),
				aliasing.narrowing(other.aliasing));
	}

	@Override
	public boolean lessOrEqualAux(AnalysisState<A> other) throws SemanticException {
		return state.lessOrEqual(other.state)
				&& computedExpressions.lessOrEqual(other.computedExpressions)
				&& aliasing.lessOrEqual(other.aliasing);
	}

	@Override
	public AnalysisState<A> top() {
		return new AnalysisState<>(state.top(), computedExpressions.top(), aliasing.top());
	}

	@Override
	public AnalysisState<A> bottom() {
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
	public AnalysisState<A> forgetIdentifier(Identifier id) throws SemanticException {
		return new AnalysisState<>(state.forgetIdentifier(id), computedExpressions, aliasing);
	}

	@Override
	public AnalysisState<A> forgetIdentifiersIf(Predicate<Identifier> test) throws SemanticException {
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
		AnalysisState<?> other = (AnalysisState<?>) obj;
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
	public StructuredRepresentation representation() {
		if (isBottom())
			return Lattice.bottomRepresentation();
		if (isTop())
			return Lattice.topRepresentation();

		StructuredRepresentation stateRepr = state.representation();
		StructuredRepresentation exprRepr = computedExpressions.representation();
		return new ObjectRepresentation(Map.of("state", stateRepr, "expressions", exprRepr));
	}

	@Override
	public String toString() {
		return representation().toString();
	}

	@Override
	public <D extends SemanticDomain<?, ?, ?>> Collection<D> getAllDomainInstances(Class<D> domain) {
		Collection<D> result = SemanticDomain.super.getAllDomainInstances(domain);
		result.addAll(state.getAllDomainInstances(domain));
		return result;
	}
}
