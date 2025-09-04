package it.unive.lisa.analysis;

import it.unive.lisa.analysis.continuations.Continuation;
import it.unive.lisa.analysis.continuations.Exceptions;
import it.unive.lisa.analysis.continuations.Execution;
import it.unive.lisa.analysis.continuations.Halt;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.analysis.lattices.FunctionalLattice;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.util.representation.StructuredRepresentation;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Predicate;

/**
 * The abstract analysis state at a given program point. An analysis state is a
 * function from {@link Continuation}s to {@link ProgramState}s, separating the
 * state corresponding to normal execution ({@link Execution}), the state
 * corresponding to the program's halting ({@link Halt}), the states
 * corresponding to exceptions ({@link Exception} and {@link Exceptions}), and
 * the states traversing finally blocks ({@link Finally}).
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <A> the type of {@link AbstractLattice} embedded in the states
 */
public class AnalysisState<A extends AbstractLattice<A>>
		extends
		FunctionalLattice<AnalysisState<A>, Continuation, ProgramState<A>>
		implements
		DomainLattice<AnalysisState<A>, AnalysisState<A>> {

	/**
	 * Builds a new analysis state.
	 * 
	 * @param lattice the program state to embed in this analysis state
	 */
	public AnalysisState(
			ProgramState<A> lattice) {
		super(lattice);
	}

	/**
	 * Builds a new analysis state.
	 * 
	 * @param lattice  the program state to embed in this analysis state
	 * @param function the mapping from continuations to program states
	 */
	public AnalysisState(
			ProgramState<A> lattice,
			Map<Continuation, ProgramState<A>> function) {
		super(lattice, function);
	}

	@Override
	public ProgramState<A> getState(
			Continuation key) {
		if (key == Execution.INSTANCE && isTop())
			return lattice.top();
		if (key == Execution.INSTANCE && isBottom())
			return lattice.bottom();
		return super.getState(key);
	}

	/**
	 * Stores the given state as the approximation for the normal execution.
	 * 
	 * @param state the state to store
	 * 
	 * @return a new instance with the updated mapping
	 */
	public AnalysisState<A> withExecutionState(
			ProgramState<A> state) {
		return putState(Execution.INSTANCE, state);
	}

	/**
	 * Yields the {@link AbstractLattice} embedded into the program state of the
	 * normal execution, containing abstract values for program variables and
	 * memory locations.
	 * 
	 * @return the abstract state
	 */
	public A getExecutionState() {
		return getState(Execution.INSTANCE).getState();
	}

	/**
	 * Yields the last computed expression(s) in the normal execution. This is a
	 * {@link SymbolicExpression} that corresponds to the last computed
	 * value(s). The set returned by this method usually contains one
	 * expression, but instances created through lattice operations (e.g., lub)
	 * might contain more.
	 * 
	 * @return the last computed expression
	 */
	public ExpressionSet getExecutionExpressions() {
		return getState(Execution.INSTANCE).getComputedExpressions();
	}

	/**
	 * Yields a shallow copy of this state, but with the normal execution's
	 * state having the given computed expression.
	 * 
	 * @param computedExpression the expression
	 * 
	 * @return the shallow copy with the new expression
	 */
	public AnalysisState<A> withExecutionExpression(
			SymbolicExpression computedExpression) {
		return withExecutionExpressions(new ExpressionSet(computedExpression));
	}

	/**
	 * Yields a shallow copy of this state, but with the normal execution's
	 * state having the given computed expressions.
	 * 
	 * @param computedExpressions the expressions
	 * 
	 * @return the shallow copy with the new expressions
	 */
	public AnalysisState<A> withExecutionExpressions(
			ExpressionSet computedExpressions) {
		return withExecutionState(getState(Execution.INSTANCE).withComputedExpressions(computedExpressions));
	}

	/**
	 * Yields the additional information accumulated during the fixpoint
	 * computation in the normal execution. This is a generic key-value mapping
	 * that users of the library can use for ad-hoc purposes.
	 * 
	 * @return the additional information
	 */
	public FixpointInfo getExecutionInformation() {
		return getState(Execution.INSTANCE).getFixpointInformation();
	}

	/**
	 * Yields the additional information associated to the given key, as defined
	 * in this instance's {@link #getExecutionInformation()}. Note that the info
	 * is retrieved from the program state corresponding to the normal
	 * execution.
	 * 
	 * @param key the key
	 * 
	 * @return the mapped information
	 */
	public Lattice<?> getExecutionInfo(
			String key) {
		return getState(Execution.INSTANCE).getInfo(key);
	}

	/**
	 * Yields the additional information associated to the given key, casted to
	 * the given type, as defined in this instance's
	 * {@link #getExecutionInformation()}. Note that the info is retrieved from
	 * the program state corresponding to the normal execution.
	 * 
	 * @param <T>  the type to cast the return value of this method to
	 * @param key  the key
	 * @param type the type to cast the retrieved information to
	 * 
	 * @return the mapped information
	 */
	public <T> T getExecutionInfo(
			String key,
			Class<T> type) {
		return getState(Execution.INSTANCE).getInfo(key, type);
	}

	/**
	 * Yields a copy of this state where the additional fixpoint information
	 * ({@link #getExecutionInformation()}) of the normal execution has been
	 * updated by mapping the given key to {@code info}. This is a strong
	 * update, meaning that the information previously mapped to the same key,
	 * if any, is lost. For a weak update, use
	 * {@link #weakStoreExecutionInfo(String, Lattice)}.
	 * 
	 * @param key  the key
	 * @param info the information to store
	 * 
	 * @return a new instance with the updated mapping
	 */
	public AnalysisState<A> storeExecutionInfo(
			String key,
			Lattice<?> info) {
		ProgramState<A> state = getState(Execution.INSTANCE);
		return putState(Execution.INSTANCE, state.storeInfo(key, info));
	}

	/**
	 * Yields a copy of this state where the additional fixpoint information
	 * ({@link #getExecutionInformation()}) of the normal execution has been
	 * updated by mapping the given key to {@code info}. This is a weak update,
	 * meaning that the information previously mapped to the same key, if any,
	 * is lubbed together with the given one, and the result is stored inside
	 * the mapping instead. For a strong update, use
	 * {@link #storeExecutionInfo(String, Lattice)}.
	 * 
	 * @param key  the key
	 * @param info the information to store
	 * 
	 * @return a new instance with the updated mapping
	 * 
	 * @throws SemanticException if something goes wrong during the lub
	 */
	public AnalysisState<A> weakStoreExecutionInfo(
			String key,
			Lattice<?> info)
			throws SemanticException {
		ProgramState<A> state = getState(Execution.INSTANCE);
		return putState(Execution.INSTANCE, state.weakStoreInfo(key, info));
	}

	@Override
	public AnalysisState<A> pushScope(
			ScopeToken scope,
			ProgramPoint pp)
			throws SemanticException {
		Map<Continuation, ProgramState<A>> function = mkNewFunction(null, false);
		for (Entry<Continuation, ProgramState<A>> entry : this)
			function.put(entry.getKey(), entry.getValue().pushScope(scope, pp));
		return new AnalysisState<>(lattice, function);
	}

	@Override
	public AnalysisState<A> popScope(
			ScopeToken scope,
			ProgramPoint pp)
			throws SemanticException {
		Map<Continuation, ProgramState<A>> function = mkNewFunction(null, false);
		for (Entry<Continuation, ProgramState<A>> entry : this)
			function.put(entry.getKey(), entry.getValue().popScope(scope, pp));
		return new AnalysisState<>(lattice, function);
	}

	@Override
	public AnalysisState<A> top() {
		return isTop() ? this : new AnalysisState<>(lattice.top(), null);
	}

	@Override
	public boolean isTop() {
		return super.isTop()
				|| (function != null
						&& function.size() == 1
						&& function.keySet().iterator().next() == Execution.INSTANCE
						&& function.values().iterator().next().isTop());
	}

	@Override
	public AnalysisState<A> bottom() {
		return isBottom() ? this : new AnalysisState<>(lattice.bottom(), null);
	}

	@Override
	public boolean isBottom() {
		return super.isBottom()
				|| (function != null
						&& function.size() == 1
						&& function.keySet().iterator().next() == Execution.INSTANCE
						&& function.values().iterator().next().isBottom());
	}

	/**
	 * Forgets an {@link Identifier} from the normal execution's state. This
	 * means that all information regarding the given {@code id} will be lost.
	 * 
	 * @param id the identifier to forget
	 * @param pp the program point that where this operation is being evaluated
	 * 
	 * @return the state without information about the given id in the normal
	 *             execution
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	@Override
	public AnalysisState<A> forgetIdentifier(
			Identifier id,
			ProgramPoint pp)
			throws SemanticException {
		return putState(Execution.INSTANCE, getState(Execution.INSTANCE).forgetIdentifier(id, pp));
	}

	/**
	 * Forgets all {@link Identifier}s that match the given predicate from the
	 * normal execution's state. This means that all information regarding those
	 * identifiers will be lost.
	 * 
	 * @param test the test to identify the targets of the removal
	 * @param pp   the program point that where this operation is being
	 *                 evaluated
	 * 
	 * @return the state without information about the ids in the normal
	 *             execution
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	@Override
	public AnalysisState<A> forgetIdentifiersIf(
			Predicate<Identifier> test,
			ProgramPoint pp)
			throws SemanticException {
		return putState(Execution.INSTANCE, getState(Execution.INSTANCE).forgetIdentifiersIf(test, pp));
	}

	/**
	 * Forgets all the given {@link Identifier}s from the normal execution's
	 * state. This means that all information regarding all elements of
	 * {@code ids} will be lost.
	 * 
	 * @param ids the collection of identifiers to forget
	 * @param pp  the program point that where this operation is being evaluated
	 * 
	 * @return the state without information about the given ids in the normal
	 *             execution
	 * 
	 * @throws SemanticException if an error occurs during the computation
	 */
	@Override
	public AnalysisState<A> forgetIdentifiers(
			Iterable<Identifier> ids,
			ProgramPoint pp)
			throws SemanticException {
		if (ids == null || !ids.iterator().hasNext())
			return this;
		return putState(Execution.INSTANCE, getState(Execution.INSTANCE).forgetIdentifiers(ids, pp));
	}

	@Override
	public StructuredRepresentation representation() {
		if (isBottom())
			return Lattice.bottomRepresentation();
		if (isTop())
			return Lattice.topRepresentation();

		if (function != null && function.size() == 1 && function.keySet().iterator().next() == Execution.INSTANCE)
			return function.values().iterator().next().representation();

		return super.representation();
	}

	@Override
	public String toString() {
		return representation().toString();
	}

	/**
	 * Yields a copy of this state, but with the normal execution's
	 * {@link AbstractLattice}'s inner memory abstraction set to top. This is
	 * useful to represent effects of unknown calls that arbitrarily manipulate
	 * the memory.
	 * 
	 * @return the copy with top memory
	 */
	public AnalysisState<A> withTopMemory() {
		return putState(Execution.INSTANCE, getState(Execution.INSTANCE).withTopMemory());
	}

	/**
	 * Yields a copy of this state, but with the normal execution's
	 * {@link AbstractLattice}'s inner value abstraction set to top. This is
	 * useful to represent effects of unknown calls that arbitrarily manipulate
	 * the values of variables.
	 * 
	 * @return the copy with top value
	 */
	public AnalysisState<A> withTopValues() {
		return putState(Execution.INSTANCE, getState(Execution.INSTANCE).withTopValues());
	}

	/**
	 * Yields a copy of this state, but with the normal execution's
	 * {@link AbstractLattice}'s inner type abstraction set to top. This is
	 * useful to represent effects of unknown calls that arbitrarily manipulate
	 * the values of variables (and their type accordingly).
	 * 
	 * @return the copy with top type
	 */
	public AnalysisState<A> withTopTypes() {
		return putState(Execution.INSTANCE, getState(Execution.INSTANCE).withTopTypes());
	}

	/**
	 * Yields {@code true} if the state of the normal execution is currently
	 * tracking abstract information for the given identifier.
	 * 
	 * @param id the identifier
	 * 
	 * @return whether or not the normal execution knows about {@code id}
	 */
	@Override
	public boolean knowsIdentifier(
			Identifier id) {
		return getState(Execution.INSTANCE).knowsIdentifier(id);
	}

	/**
	 * Yields all of the instances of a specific lattice, of class
	 * {@code lattice}, contained inside the normal execution state, also
	 * recursively querying inner lattices (enabling retrieval through Cartesian
	 * products or other types of combinations).<br>
	 * <br>
	 * 
	 * @param <D>    the type of lattice to retrieve
	 * @param domain the class of the lattice instance to retrieve
	 * 
	 * @return the instances of that lattice
	 */
	@Override
	public <D extends Lattice<D>> Collection<D> getAllLatticeInstances(
			Class<D> domain) {
		return getState(Execution.INSTANCE).getAllLatticeInstances(domain);
	}

	@Override
	public ProgramState<A> stateOfUnknown(
			Continuation key) {
		return lattice.bottom();
	}

	@Override
	public AnalysisState<A> mk(
			ProgramState<A> lattice,
			Map<Continuation, ProgramState<A>> function) {
		return new AnalysisState<>(lattice, function);
	}

}
