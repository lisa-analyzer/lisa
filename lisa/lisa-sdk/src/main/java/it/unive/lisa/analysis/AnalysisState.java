package it.unive.lisa.analysis;

import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.analysis.lattices.Satisfiability;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.util.representation.ObjectRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;
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
		implements
		BaseLattice<AnalysisState<A>>,
		ScopedObject<AnalysisState<A>> {

	/**
	 * The abstract state of program variables and memory locations
	 */
	private final A state;

	/**
	 * The additional information to be computed during fixpoint computations
	 */
	private final FixpointInfo info;

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
	 */
	public AnalysisState(
			A state,
			SymbolicExpression computedExpression) {
		this(state, new ExpressionSet(computedExpression), null);
	}

	/**
	 * Builds a new state.
	 * 
	 * @param state               the {@link AbstractState} to embed in this
	 *                                analysis state
	 * @param computedExpressions the expressions that have been computed
	 */
	public AnalysisState(
			A state,
			ExpressionSet computedExpressions) {
		this(state, computedExpressions, null);
	}

	/**
	 * Builds a new state.
	 * 
	 * @param state              the {@link AbstractState} to embed in this
	 *                               analysis state
	 * @param computedExpression the expression that has been computed
	 * @param info               the additional information to be computed
	 *                               during fixpoint computations
	 */
	public AnalysisState(
			A state,
			SymbolicExpression computedExpression,
			FixpointInfo info) {
		this(state, new ExpressionSet(computedExpression), info);
	}

	/**
	 * Builds a new state.
	 * 
	 * @param state               the {@link AbstractState} to embed in this
	 *                                analysis state
	 * @param computedExpressions the expressions that have been computed
	 * @param info                the additional information to be computed
	 *                                during fixpoint computations
	 */
	public AnalysisState(
			A state,
			ExpressionSet computedExpressions,
			FixpointInfo info) {
		this.state = state;
		this.computedExpressions = computedExpressions;
		this.info = info;
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
	 * Yields the additional information that must be computed during fixpoint
	 * computations. This is a generic key-value mapping that users of the
	 * library can use for ad-hoc purposes.
	 * 
	 * @return the additional information (can be {@code null})
	 */
	public FixpointInfo getFixpointInformation() {
		return info;
	}

	/**
	 * Yields the Additional information associated to the given key, as defined
	 * in this instance's {@link #getFixpointInformation()}.
	 * 
	 * @param key the key
	 * 
	 * @return the mapped information
	 */
	public Lattice<?> getInfo(
			String key) {
		return info == null ? null : info.get(key);
	}

	/**
	 * Yields the additional information associated to the given key, casted to
	 * the given type, as defined in this instance's
	 * {@link #getFixpointInformation()}.
	 * 
	 * @param <T>  the type to cast the return value of this method to
	 * @param key  the key
	 * @param type the type to cast the retrieved information to
	 * 
	 * @return the mapped information
	 */
	public <T> T getInfo(
			String key,
			Class<T> type) {
		return info == null ? null : info.get(key, type);
	}

	/**
	 * Yields a copy of this state where the additional fixpoint information
	 * ({@link #getFixpointInformation()}) has been updated by mapping the given
	 * key to {@code info}. This is a strong update, meaning that the
	 * information previously mapped to the same key, if any, is lost. For a
	 * weak update, use {@link #weakStoreInfo(String, Lattice)}.
	 * 
	 * @param key  the key
	 * @param info the information to store
	 * 
	 * @return a new instance with the updated mapping
	 */
	public AnalysisState<A> storeInfo(
			String key,
			Lattice<?> info) {
		FixpointInfo fixinfo = this.info == null ? new FixpointInfo() : this.info;
		fixinfo = fixinfo.put(key, info);
		return new AnalysisState<>(state, computedExpressions, fixinfo);
	}

	/**
	 * Yields a copy of this state where the additional fixpoint information
	 * ({@link #getFixpointInformation()}) has been updated by mapping the given
	 * key to {@code info}. This is a weak update, meaning that the information
	 * previously mapped to the same key, if any, is lubbed together with the
	 * given one, and the result is stored inside the mapping instead. For a
	 * strong update, use {@link #storeInfo(String, Lattice)}.
	 * 
	 * @param key  the key
	 * @param info the information to store
	 * 
	 * @return a new instance with the updated mapping
	 * 
	 * @throws SemanticException if something goes wrong during the lub
	 */
	public AnalysisState<A> weakStoreInfo(
			String key,
			Lattice<?> info)
			throws SemanticException {
		FixpointInfo fixinfo = this.info == null ? new FixpointInfo() : this.info;
		fixinfo = fixinfo.putWeak(key, info);
		return new AnalysisState<>(state, computedExpressions, fixinfo);
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
	 * Yields a copy of this state, where {@code id} has been assigned to
	 * {@code value}.
	 * 
	 * @param id    the identifier to assign the value to
	 * @param value the expression to assign
	 * @param pp    the program point that where this operation is being
	 *                  evaluated
	 * 
	 * @return a copy of this domain, modified by the assignment
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	public AnalysisState<A> assign(
			Identifier id,
			SymbolicExpression value,
			ProgramPoint pp)
			throws SemanticException {
		A s = state.assign(id, value, pp, state);
		return new AnalysisState<>(s, new ExpressionSet(id), info);
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
	public AnalysisState<A> assign(
			SymbolicExpression id,
			SymbolicExpression expression,
			ProgramPoint pp)
			throws SemanticException {
		if (id instanceof Identifier)
			return assign((Identifier) id, expression, pp);

		A s = state.bottom();
		AnalysisState<A> sem = smallStepSemantics(id, pp);
		ExpressionSet rewritten = sem.state.rewrite(id, pp, state);
		for (SymbolicExpression i : rewritten)
			if (!(i instanceof Identifier))
				throw new SemanticException("Rewriting '" + id + "' did not produce an identifier: " + i);
			else
				s = s.lub(sem.state.assign((Identifier) i, expression, pp, state));
		return new AnalysisState<>(s, rewritten, info);
	}

	/**
	 * Yields a copy of this state, that has been modified accordingly to the
	 * semantics of the given {@code expression}.
	 * 
	 * @param expression the expression whose semantics need to be computed
	 * @param pp         the program point that where this operation is being
	 *                       evaluated
	 * 
	 * @return a copy of this domain, modified accordingly to the semantics of
	 *             {@code expression}
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	public AnalysisState<A> smallStepSemantics(
			SymbolicExpression expression,
			ProgramPoint pp)
			throws SemanticException {
		A s = state.smallStepSemantics(expression, pp, state);
		return new AnalysisState<>(s, new ExpressionSet(expression), info);
	}

	/**
	 * Yields a copy of this state, modified by assuming that the given
	 * expression holds. It is required that the returned domain is in relation
	 * with this one. A safe (but imprecise) implementation of this method can
	 * always return {@code this}.
	 * 
	 * @param expression the expression to assume to hold.
	 * @param src        the program point that where this operation is being
	 *                       evaluated, corresponding to the one that generated
	 *                       the given expression
	 * @param dest       the program point where the execution will move after
	 *                       the expression has been assumed
	 * 
	 * @return the (optionally) modified copy of this domain
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	public AnalysisState<A> assume(
			SymbolicExpression expression,
			ProgramPoint src,
			ProgramPoint dest)
			throws SemanticException {
		A assume = state.assume(expression, src, dest, state);
		if (assume.isBottom())
			return bottom();
		return new AnalysisState<>(assume, computedExpressions, info);
	}

	/**
	 * Checks if the given expression is satisfied by the abstract values of
	 * this state, returning an instance of {@link Satisfiability}.
	 * 
	 * @param expression the expression whose satisfiability is to be evaluated
	 * @param pp         the program point that where this operation is being
	 *                       evaluated
	 * 
	 * @return {@link Satisfiability#SATISFIED} is the expression is satisfied
	 *             by the values of this domain,
	 *             {@link Satisfiability#NOT_SATISFIED} if it is not satisfied,
	 *             or {@link Satisfiability#UNKNOWN} if it is either impossible
	 *             to determine if it satisfied, or if it is satisfied by some
	 *             values and not by some others (this is equivalent to a TOP
	 *             boolean value)
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	public Satisfiability satisfies(
			SymbolicExpression expression,
			ProgramPoint pp)
			throws SemanticException {
		return state.satisfies(expression, pp, state);
	}

	@Override
	public AnalysisState<A> pushScope(
			ScopeToken scope)
			throws SemanticException {
		return new AnalysisState<>(
				state.pushScope(scope),
				onAllExpressions(this.computedExpressions, scope, true),
				info);
	}

	private static ExpressionSet onAllExpressions(
			ExpressionSet computedExpressions,
			ScopeToken scope,
			boolean push)
			throws SemanticException {
		Set<SymbolicExpression> result = new HashSet<>();
		for (SymbolicExpression exp : computedExpressions)
			result.add(push ? exp.pushScope(scope) : exp.popScope(scope));
		return new ExpressionSet(result);
	}

	@Override
	public AnalysisState<A> popScope(
			ScopeToken scope)
			throws SemanticException {
		return new AnalysisState<>(
				state.popScope(scope),
				onAllExpressions(this.computedExpressions, scope, false),
				info);
	}

	@Override
	public AnalysisState<A> lubAux(
			AnalysisState<A> other)
			throws SemanticException {
		return new AnalysisState<>(
				state.lub(other.state),
				computedExpressions.lub(other.computedExpressions),
				info == null ? other.info : info.lub(other.info));
	}

	@Override
	public AnalysisState<A> glbAux(
			AnalysisState<A> other)
			throws SemanticException {
		return new AnalysisState<>(
				state.glb(other.state),
				computedExpressions.glb(other.computedExpressions),
				info == null ? null : info.glb(other.info));
	}

	@Override
	public AnalysisState<A> wideningAux(
			AnalysisState<A> other)
			throws SemanticException {
		return new AnalysisState<>(
				state.widening(other.state),
				computedExpressions.lub(other.computedExpressions),
				info == null ? other.info : info.widening(other.info));
	}

	@Override
	public AnalysisState<A> narrowingAux(
			AnalysisState<A> other)
			throws SemanticException {
		return new AnalysisState<>(
				state.narrowing(other.state),
				computedExpressions.glb(other.computedExpressions),
				info == null ? null : info.narrowing(other.info));
	}

	@Override
	public boolean lessOrEqualAux(
			AnalysisState<A> other)
			throws SemanticException {
		return state.lessOrEqual(other.state)
				&& computedExpressions.lessOrEqual(other.computedExpressions)
				&& (info == null ? true : info.lessOrEqual(other.info));
	}

	@Override
	public AnalysisState<A> top() {
		return new AnalysisState<>(state.top(), computedExpressions.top(), null);
	}

	@Override
	public AnalysisState<A> bottom() {
		return new AnalysisState<>(state.bottom(), computedExpressions.bottom(), FixpointInfo.BOTTOM);
	}

	@Override
	public boolean isTop() {
		return state.isTop() && computedExpressions.isTop() && info == null;
	}

	@Override
	public boolean isBottom() {
		return state.isBottom() && computedExpressions.isBottom() && (info != null && info.isBottom());
	}

	/**
	 * Forgets an {@link Identifier}. This means that all information regarding
	 * the given {@code id} will be lost. This method should be invoked whenever
	 * an identifier gets out of scope.
	 * 
	 * @param id the identifier to forget
	 * 
	 * @return the analysis state without information about the given id
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	public AnalysisState<A> forgetIdentifier(
			Identifier id)
			throws SemanticException {
		return new AnalysisState<>(state.forgetIdentifier(id), computedExpressions, info);
	}

	/**
	 * Forgets all {@link Identifier}s that match the given predicate. This
	 * means that all information regarding the those identifiers will be lost.
	 * This method should be invoked whenever an identifier gets out of scope.
	 * 
	 * @param test the test to identify the targets of the removal
	 * 
	 * @return the analysis state in without information about the ids
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	public AnalysisState<A> forgetIdentifiersIf(
			Predicate<Identifier> test)
			throws SemanticException {
		return new AnalysisState<>(state.forgetIdentifiersIf(test), computedExpressions, info);
	}

	/**
	 * Forgets all the given {@link Identifier}s by invoking
	 * {@link #forgetIdentifier(Identifier)} on each given identifier.
	 * 
	 * @param ids the collection of identifiers to forget
	 * 
	 * @return the analysis state without information about the given ids
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	public AnalysisState<A> forgetIdentifiers(
			Iterable<Identifier> ids)
			throws SemanticException {
		AnalysisState<A> result = this;
		for (Identifier id : ids)
			result = result.forgetIdentifier(id);
		return result;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((info == null) ? 0 : info.hashCode());
		result = prime * result + ((computedExpressions == null) ? 0 : computedExpressions.hashCode());
		result = prime * result + ((state == null) ? 0 : state.hashCode());
		return result;
	}

	@Override
	public boolean equals(
			Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AnalysisState<?> other = (AnalysisState<?>) obj;
		if (info == null) {
			if (other.info != null)
				return false;
		} else if (!info.equals(other.info))
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

	/**
	 * Variant of {@link #representation()} that also includes
	 * {@link #getFixpointInformation()}.
	 * 
	 * @return the enriched representation
	 */
	public StructuredRepresentation representationWithInfo() {
		if (isBottom())
			return Lattice.bottomRepresentation();
		if (isTop())
			return Lattice.topRepresentation();
		if (info == null || info.isBottom())
			return representation();

		StructuredRepresentation stateRepr = state.representation();
		StructuredRepresentation exprRepr = computedExpressions.representation();
		StructuredRepresentation infoRepr = info.representation();
		return new ObjectRepresentation(Map.of("state", stateRepr, "expressions", exprRepr, "info", infoRepr));
	}

	@Override
	public String toString() {
		return representation().toString();
	}

	/**
	 * Yields a copy of this state, but with the {@link AbstractState}'s inner
	 * memory abstraction set to top. This is useful to represent effects of
	 * unknown calls that arbitrarily manipulate the memory.
	 * 
	 * @return the copy with top memory
	 */
	public AnalysisState<A> withTopMemory() {
		return new AnalysisState<>(state.withTopMemory(), computedExpressions, info);
	}

	/**
	 * Yields a copy of this state, but with the {@link AbstractState}'s inner
	 * value abstraction set to top. This is useful to represent effects of
	 * unknown calls that arbitrarily manipulate the values of variables.
	 * 
	 * @return the copy with top value
	 */
	public AnalysisState<A> withTopValues() {
		return new AnalysisState<>(state.withTopValues(), computedExpressions, info);
	}

	/**
	 * Yields a copy of this state, but with the {@link AbstractState}'s inner
	 * type abstraction set to top. This is useful to represent effects of
	 * unknown calls that arbitrarily manipulate the values of variables (and
	 * their type accordingly).
	 * 
	 * @return the copy with top type
	 */
	public AnalysisState<A> withTopTypes() {
		return new AnalysisState<>(state.withTopTypes(), computedExpressions, info);
	}
}
