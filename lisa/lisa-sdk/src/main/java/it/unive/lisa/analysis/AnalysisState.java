package it.unive.lisa.analysis;

import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.analysis.lattices.GenericMapLattice;
import it.unive.lisa.analysis.lattices.GenericSetLattice;
import it.unive.lisa.conf.LiSAConfiguration;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.type.Type;
import it.unive.lisa.util.representation.MapRepresentation;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredObject;
import it.unive.lisa.util.representation.StructuredRepresentation;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * The abstract analysis state at a given program point. An analysis state can
 * be seen as a function from continuations (i.e., tokens that distinguish the
 * normal execution from e.g. a halting one, or an exceptional one) to
 * {@link ProgramState}s, separating the state corresponding to the normal
 * non-erroneous execution, the state corresponding to the program's halting,
 * the states corresponding to errors, and the state corresponding to smashed
 * errors (see {@link LiSAConfiguration#shouldSmashError}).
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <A> the type of {@link AbstractLattice} embedded in the states
 */
public class AnalysisState<A extends AbstractLattice<A>>
		implements
		BaseLattice<AnalysisState<A>>,
		DomainLattice<AnalysisState<A>, AnalysisState<A>> {

	private final ProgramState<A> execution;
	private final ProgramState<A> halt;
	private final GenericMapLattice<Type, GenericSetLattice<Statement>> smashedErrors;
	private final ProgramState<A> smashedErrorsState;
	private final GenericMapLattice<Error, ProgramState<A>> errors;

	/**
	 * Builds a new analysis state.
	 * 
	 * @param lattice the program state to embed in this analysis state
	 */
	public AnalysisState(
			ProgramState<A> lattice) {
		ProgramState<A> top = lattice.top();
		this.execution = top;
		this.halt = top;
		this.smashedErrorsState = top;
		this.smashedErrors = new GenericMapLattice<>(new GenericSetLattice<>());
		this.errors = new GenericMapLattice<>(top);
	}

	private AnalysisState(
			ProgramState<A> execution,
			ProgramState<A> halt,
			GenericMapLattice<Type, GenericSetLattice<Statement>> smashedErrors,
			ProgramState<A> smashedErrorsState,
			GenericMapLattice<Error, ProgramState<A>> errors) {
		this.execution = execution;
		this.halt = halt;
		this.smashedErrors = smashedErrors;
		this.smashedErrorsState = smashedErrorsState;
		this.errors = errors;
	}

	/**
	 * Stores the given state as the approximation for the normal execution.
	 * 
	 * @param state the state to store
	 * 
	 * @return a new instance with the updated mapping
	 */
	public AnalysisState<A> withExecution(
			ProgramState<A> state) {
		return new AnalysisState<>(state, halt, smashedErrors, smashedErrorsState, errors);
	}

	/**
	 * Yields the {@link AbstractLattice} embedded into the program state of the
	 * normal execution, containing abstract values for program variables and
	 * memory locations.
	 * 
	 * @return the abstract state
	 */
	public A getExecutionState() {
		return execution.getState();
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
		return execution.getComputedExpressions();
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
		return withExecution(execution.withComputedExpressions(computedExpressions));
	}

	/**
	 * Yields the additional information accumulated during the fixpoint
	 * computation in the normal execution. This is a generic key-value mapping
	 * that users of the library can use for ad-hoc purposes.
	 * 
	 * @return the additional information
	 */
	public FixpointInfo getExecutionInformation() {
		return execution.getFixpointInformation();
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
		return execution.getInfo(key);
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
		return execution.getInfo(key, type);
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
		return new AnalysisState<>(execution.storeInfo(key, info), halt, smashedErrors, smashedErrorsState, errors);
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
		return new AnalysisState<>(execution.weakStoreInfo(key, info), halt, smashedErrors, smashedErrorsState, errors);
	}

	/**
	 * Yields the program state corresponding to the normal execution. This is
	 * the state that represents the program's normal behavior during its
	 * execution, and it is the only one that evolves through assignments,
	 * updates, and other state-changing operations.
	 * 
	 * @return the normal execution state
	 */
	public ProgramState<A> getExecution() {
		return execution;
	}

	/**
	 * Yields the program state corresponding to the halting of the program.
	 * This is used to track the state of the program after an instruction that
	 * causes the program to halt (e.g., a call to an exit function).
	 * 
	 * @return the halting state
	 */
	public ProgramState<A> getHalt() {
		return halt;
	}

	/**
	 * Yields the non-smashed errors currently tracked by this state. Each entry
	 * in the returned lattice represents an error (with a type and a throwing
	 * statement) mapped to the program state that was active when the error was
	 * thrown.
	 * 
	 * @return the non-smashed errors
	 */
	public GenericMapLattice<Error, ProgramState<A>> getErrors() {
		return errors;
	}

	/**
	 * Yields the smashed errors currently tracked by this state. Each entry in
	 * the returned lattice represents an error type mapped to the set of
	 * statements where the error occurred. See
	 * {@link LiSAConfiguration#shouldSmashError} for more details.
	 * 
	 * @return the smashed errors
	 */
	public GenericMapLattice<Type, GenericSetLattice<Statement>> getSmashedErrors() {
		return smashedErrors;
	}

	/**
	 * Yields the summary state corresponding to all smashed errors. See
	 * {@link LiSAConfiguration#shouldSmashError} for more details.
	 * 
	 * @return the summary state
	 */
	public ProgramState<A> getSmashedErrorsState() {
		return smashedErrorsState;
	}

	/**
	 * Yields a new state with all errors removed, containing only the normal
	 * and halting state.
	 * 
	 * @param halting if {@code true}, the halting state is also removed (i.e.,
	 *                    set to bottom)
	 * 
	 * @return the cleaned state
	 */
	public AnalysisState<A> removeAllErrors(
			boolean halting) {
		if (isBottom() || isTop())
			return this;

		if (smashedErrors.isBottom()
				&& smashedErrorsState.isBottom()
				&& errors.isBottom()
				&& (!halting || halt.isBottom()))
			return this;

		return new AnalysisState<>(
				execution,
				halting ? halt.bottom() : halt,
				smashedErrors.bottom(),
				smashedErrorsState.bottom(),
				errors.bottom());
	}

	/**
	 * Yields a new state where {@code error} is added to this state. If
	 * {@code error}'s type is already in this state, its state is lubbed with
	 * {@code state}. Otherwise, the pair is added to the state as-is.
	 *
	 * @param error the error to add
	 * @param state the state to map to the error
	 * 
	 * @return the updated state
	 * 
	 * @throws SemanticException if something goes wrong during the computation
	 */
	public AnalysisState<A> addError(
			Error error,
			ProgramState<A> state)
			throws SemanticException {
		if (state.isBottom())
			return this;
		// we don't check if this state is bottom here, since even if
		// the whole state is bottom we still want to track the new error
		ProgramState<A> exc = errors.getState(error);
		return new AnalysisState<>(
				execution,
				halt,
				smashedErrors,
				smashedErrorsState,
				errors.putState(error, state.lub(exc)));
	}

	/**
	 * Yields a new state with all errors in the given map added to this state.
	 * If an error in {@code errors} is not present in this state, it is added
	 * with the respective program state. If it is already present, its state is
	 * lubbed with the one already present.
	 * 
	 * @param errors the errors to add
	 * 
	 * @return the updated state
	 * 
	 * @throws SemanticException if something goes wrong during the computation
	 */
	public AnalysisState<A> addErrors(
			Map<Error, ProgramState<A>> errors)
			throws SemanticException {
		if (errors.isEmpty())
			return this;
		// we don't check if this state is bottom here, since even if
		// the whole state is bottom we still want to track the new error
		Map<Error, ProgramState<A>> func = this.errors.mkNewFunction(this.errors.function, false);
		for (Entry<Error, ProgramState<A>> e : errors.entrySet()) {
			ProgramState<A> exc = func.get(e.getKey());
			func.put(e.getKey(), e.getValue().lub(exc));
		}
		return new AnalysisState<>(
				execution,
				halt,
				smashedErrors,
				smashedErrorsState,
				new GenericMapLattice<>(this.errors.lattice, func));
	}

	/**
	 * Removes all the given errors from this state. If an error in
	 * {@code caught} is not present in this state, it is simply ignored. Note
	 * that this method only operates on the non-smashed errors.
	 * 
	 * @param caught the errors to remove
	 * 
	 * @return the updated state
	 */
	public AnalysisState<A> removeErrors(
			Collection<Error> caught) {
		if (caught.isEmpty() || isBottom() || isTop() || errors.function == null || errors.function.isEmpty())
			return this;

		Map<Error, ProgramState<A>> filtered = errors.mkNewFunction(errors.function, false);
		for (Entry<Error, ProgramState<A>> e : errors)
			if (caught.contains(e.getKey()))
				filtered.remove(e.getKey());

		return new AnalysisState<>(
				execution,
				halt,
				smashedErrors,
				smashedErrorsState,
				new GenericMapLattice<>(errors.lattice, filtered));
	}

	/**
	 * Yields a new state where {@code error} is added to the smashed errors. If
	 * {@code error}'s type is already in the smashed errors, its thrower is
	 * added to the ones already present instead. The smashed errors state is
	 * also lubbed with {@code state}.
	 * 
	 * @param error the error to add
	 * @param state the state to lub with the smashed errors state
	 * 
	 * @return the updated state
	 * 
	 * @throws SemanticException if something goes wrong during the computation
	 */
	public AnalysisState<A> addSmashedError(
			Error error,
			ProgramState<A> state)
			throws SemanticException {
		if (state.isBottom())
			return this;
		// we don't check if this state is bottom here, since even if
		// the whole state is bottom we still want to track the new error
		GenericSetLattice<Statement> throwers = smashedErrors.getState(error.getType());
		GenericSetLattice<Statement> newThrowers = throwers.add(error.getThrower());
		return new AnalysisState<>(
				execution,
				halt,
				smashedErrors.putState(error.getType(), newThrowers),
				smashedErrorsState.lub(state),
				errors);
	}

	/**
	 * Yields a new state where all types in {@code errors} are added to the
	 * smashed errors, with the throwers specified in the image of the type. If
	 * a type is already in the smashed errors, all throwers in the image are
	 * added to the ones already present instead. The smashed errors state is
	 * also lubbed with {@code state}.
	 * 
	 * @param errors the map from error types to the statements that threw them
	 * @param state  the state to lub with the smashed errors state
	 * 
	 * @return the updated state
	 * 
	 * @throws SemanticException if something goes wrong during the computation
	 */
	public AnalysisState<A> addSmashedErrors(
			Map<Type, Set<Statement>> errors,
			ProgramState<A> state)
			throws SemanticException {
		if (errors.isEmpty())
			return this;
		// we don't check if this state is bottom here, since even if
		// the whole state is bottom we still want to track the new error
		Map<Type, GenericSetLattice<Statement>> func = smashedErrors.mkNewFunction(smashedErrors.function, false);
		for (Entry<Type, Set<Statement>> e : errors.entrySet()) {
			GenericSetLattice<Statement> throwers = func.get(e.getKey());
			if (throwers == null)
				func.put(e.getKey(), new GenericSetLattice<>(e.getValue()));
			else
				func.put(e.getKey(), throwers.addAll(e.getValue()));
		}
		return new AnalysisState<>(
				execution,
				halt,
				new GenericMapLattice<>(smashedErrors.lattice, func),
				smashedErrorsState.lub(state),
				this.errors);
	}

	/**
	 * Yields a copy of this state where, for each error type {@code t} in the
	 * keys of {@code caught}, all statements in {@code caught.get(t)} have been
	 * removed from the set of throwers of {@code t} in the smashed errors. If
	 * that set becomes empty, then {@code t} is removed from the smashed
	 * errors. If all smashed errors are removed, then both the smashed errors
	 * and the smashed errors state are set to bottom.
	 * 
	 * @param caught the map from caught errors to the throwers that are caught
	 * 
	 * @return the updated state
	 */
	public AnalysisState<A> removeSmashedErrors(
			Map<Type, Set<Statement>> caught) {
		if (caught.isEmpty() || isBottom() || isTop() || smashedErrors.function == null
				|| smashedErrors.function.isEmpty())
			return this;

		Map<Type, GenericSetLattice<Statement>> filtered = smashedErrors.mkNewFunction(smashedErrors.function, false);
		for (Entry<Type, GenericSetLattice<Statement>> e : smashedErrors) {
			Set<Statement> throwers = caught.get(e.getKey());
			if (throwers != null) {
				GenericSetLattice<Statement> set = filtered.get(e.getKey());
				set = set.removeAll(throwers);
				if (set.elements.isEmpty())
					filtered.remove(e.getKey());
				else
					filtered.put(e.getKey(), set);
			}
		}

		if (filtered.isEmpty())
			return new AnalysisState<>(
					execution,
					halt,
					smashedErrors.bottom(),
					smashedErrorsState.bottom(),
					errors);
		else
			return new AnalysisState<>(
					execution,
					halt,
					new GenericMapLattice<>(smashedErrors.lattice, filtered),
					smashedErrorsState,
					errors);
	}

	@Override
	public AnalysisState<A> pushScope(
			ScopeToken scope,
			ProgramPoint pp)
			throws SemanticException {
		return new AnalysisState<>(
				execution.pushScope(scope, pp),
				halt.pushScope(scope, pp),
				smashedErrors,
				smashedErrorsState.pushScope(scope, pp),
				errors.transform(k -> k, state -> state.pushScope(scope, pp), (
						s1,
						s2) -> s1.lub(s2)));
	}

	@Override
	public AnalysisState<A> popScope(
			ScopeToken scope,
			ProgramPoint pp)
			throws SemanticException {
		return new AnalysisState<>(
				execution.popScope(scope, pp),
				halt.popScope(scope, pp),
				smashedErrors,
				smashedErrorsState.popScope(scope, pp),
				errors.transform(k -> k, state -> state.popScope(scope, pp), (
						s1,
						s2) -> s1.lub(s2)));
	}

	@Override
	public boolean lessOrEqualAux(
			AnalysisState<A> other)
			throws SemanticException {
		return execution.lessOrEqual(other.execution)
				&& halt.lessOrEqual(other.halt)
				&& smashedErrors.lessOrEqual(other.smashedErrors)
				&& smashedErrorsState.lessOrEqual(other.smashedErrorsState)
				&& errors.lessOrEqual(other.errors);
	}

	@Override
	public AnalysisState<A> lubAux(
			AnalysisState<A> other)
			throws SemanticException {
		return new AnalysisState<>(
				execution.lub(other.execution),
				halt.lub(other.halt),
				smashedErrors.lub(other.smashedErrors),
				smashedErrorsState.lub(other.smashedErrorsState),
				errors.lub(other.errors));
	}

	@Override
	public AnalysisState<A> glbAux(
			AnalysisState<A> other)
			throws SemanticException {
		return new AnalysisState<>(
				execution.glb(other.execution),
				halt.glb(other.halt),
				smashedErrors.glb(other.smashedErrors),
				smashedErrorsState.glb(other.smashedErrorsState),
				errors.glb(other.errors));
	}

	@Override
	public AnalysisState<A> wideningAux(
			AnalysisState<A> other)
			throws SemanticException {
		return new AnalysisState<>(
				execution.widening(other.execution),
				halt.widening(other.halt),
				smashedErrors.widening(other.smashedErrors),
				smashedErrorsState.widening(other.smashedErrorsState),
				errors.widening(other.errors));
	}

	@Override
	public AnalysisState<A> narrowingAux(
			AnalysisState<A> other)
			throws SemanticException {
		return new AnalysisState<>(
				execution.narrowing(other.execution),
				halt.narrowing(other.halt),
				smashedErrors.narrowing(other.smashedErrors),
				smashedErrorsState.narrowing(other.smashedErrorsState),
				errors.narrowing(other.errors));
	}

	@Override
	public AnalysisState<A> top() {
		return isTop() ? this
				: new AnalysisState<>(
						execution.top(),
						halt.top(),
						smashedErrors.top(),
						smashedErrorsState.top(),
						errors.top());
	}

	/**
	 * Yields a copy of this state where the inner {@link ProgramState}
	 * corresponding to the normal execution has been set to top.
	 *
	 * @return this state but with the top execution
	 */
	public AnalysisState<A> topExecution() {
		return execution.isTop() ? this : withExecution(execution.top());
	}

	@Override
	public boolean isTop() {
		return execution.isTop()
				&& halt.isTop()
				&& smashedErrors.isTop()
				&& smashedErrorsState.isTop()
				&& errors.isTop();
	}

	@Override
	public AnalysisState<A> bottom() {
		return isBottom() ? this
				: new AnalysisState<>(
						execution.bottom(),
						halt.bottom(),
						smashedErrors.bottom(),
						smashedErrorsState.bottom(),
						errors.bottom());
	}

	/**
	 * Yields a copy of this state where the inner {@link ProgramState}
	 * corresponding to the normal execution has been set to bottom.
	 *
	 * @return this state but with the bottom execution
	 */
	public AnalysisState<A> bottomExecution() {
		return execution.isBottom() ? this : withExecution(execution.bottom());
	}

	@Override
	public boolean isBottom() {
		return execution.isBottom()
				&& halt.isBottom()
				&& smashedErrors.isBottom()
				&& smashedErrorsState.isBottom()
				&& errors.isBottom();
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
		if (execution.isTop() || execution.isBottom())
			return this;
		return withExecution(execution.forgetIdentifier(id, pp));
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
		if (execution.isTop() || execution.isBottom())
			return this;
		return withExecution(execution.forgetIdentifiersIf(test, pp));
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
		if (execution.isTop() || execution.isBottom())
			return this;
		if (ids == null || !ids.iterator().hasNext())
			return this;
		return withExecution(execution.forgetIdentifiers(ids, pp));
	}

	@Override
	public StructuredRepresentation representation() {
		if (isBottom())
			return Lattice.bottomRepresentation();
		if (isTop())
			return Lattice.topRepresentation();

		Map<StructuredRepresentation, StructuredRepresentation> repr = new TreeMap<>();
		repr.put(new StringRepresentation("normal"), execution.representation());
		if (!halt.isBottom())
			repr.put(new StringRepresentation("halt"), execution.representation());
		for (Entry<Error, ProgramState<A>> e : errors)
			repr.put(e.getKey().representation(), e.getValue().representation());
		if (!smashedErrorsState.isBottom() && smashedErrors.function != null && !smashedErrors.function.isEmpty()) {
			StringBuilder s = new StringBuilder("smashed exceptions: ");
			smashedErrors.function.entrySet()
					.stream()
					.sorted((
							e1,
							e2) -> e1.getKey().toString().compareTo(e2.getKey().toString()))
					.forEachOrdered(t -> s.append(t.getKey())
							.append(" from ")
							.append(t.getValue().elements.stream().map(Object::toString).sorted()
									.collect(Collectors.joining(", ")))
							.append("; "));
			s.delete(s.length() - 2, s.length());
			repr.put(new StringRepresentation(s.toString()), smashedErrorsState.representation());
		}

		if (repr.size() == 1 && repr.keySet().iterator().next().toString().equals("normal"))
			// if we have only the execution, we dump that directly
			return repr.values().iterator().next();
		else
			// otherwise, represent the full state
			return new MapRepresentation(repr);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((execution == null) ? 0 : execution.hashCode());
		result = prime * result + ((halt == null) ? 0 : halt.hashCode());
		result = prime * result + ((smashedErrors == null) ? 0 : smashedErrors.hashCode());
		result = prime * result + ((smashedErrorsState == null) ? 0 : smashedErrorsState.hashCode());
		result = prime * result + ((errors == null) ? 0 : errors.hashCode());
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
		if (execution == null) {
			if (other.execution != null)
				return false;
		} else if (!execution.equals(other.execution))
			return false;
		if (halt == null) {
			if (other.halt != null)
				return false;
		} else if (!halt.equals(other.halt))
			return false;
		if (smashedErrors == null) {
			if (other.smashedErrors != null)
				return false;
		} else if (!smashedErrors.equals(other.smashedErrors))
			return false;
		if (smashedErrorsState == null) {
			if (other.smashedErrorsState != null)
				return false;
		} else if (!smashedErrorsState.equals(other.smashedErrorsState))
			return false;
		if (errors == null) {
			if (other.errors != null)
				return false;
		} else if (!errors.equals(other.errors))
			return false;
		return true;
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
		if (execution.isTop())
			return this;
		return withExecution(execution.withTopMemory());
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
		if (execution.isTop())
			return this;
		return withExecution(execution.withTopValues());
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
		if (execution.isTop())
			return this;
		return withExecution(execution.withTopTypes());
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
		return execution.knowsIdentifier(id);
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
		return execution.getAllLatticeInstances(domain);
	}

	/**
	 * A token that represents one or more errors of a specific type being
	 * raised.
	 *
	 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
	 */
	public static class Error
			implements
			StructuredObject {

		private final Type type;

		private final Statement thrower;

		/**
		 * Builds a new continuation for the given error type.
		 * 
		 * @param type    the error type
		 * @param thrower the statement that threw the error
		 */
		public Error(
				Type type,
				Statement thrower) {
			this.type = type;
			this.thrower = thrower;
		}

		/**
		 * Yields the error type associated with this token.
		 * 
		 * @return the error type
		 */
		public Type getType() {
			return type;
		}

		/**
		 * Yields the statement that threw the error.
		 * 
		 * @return the statement that threw the error
		 */
		public Statement getThrower() {
			return thrower;
		}

		/**
		 * Yields a new instance of this class with the same error type and the
		 * given thrower.
		 * 
		 * @param thrower the statement that threw the error
		 * 
		 * @return the new instance
		 */
		public Error withThrower(
				Statement thrower) {
			return new Error(type, thrower);
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((type == null) ? 0 : type.hashCode());
			result = prime * result + ((thrower == null) ? 0 : thrower.hashCode());
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
			Error other = (Error) obj;
			if (type == null) {
				if (other.type != null)
					return false;
			} else if (!type.equals(other.type))
				return false;
			if (thrower == null) {
				if (other.thrower != null)
					return false;
			} else if (!thrower.equals(other.thrower))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return representation().toString();
		}

		@Override
		public StructuredRepresentation representation() {
			return new StringRepresentation(type + " from " + thrower);
		}
	}

}
