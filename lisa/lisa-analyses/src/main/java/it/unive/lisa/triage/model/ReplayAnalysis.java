package it.unive.lisa.triage.model;

import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.analysis.AnalysisState.Error;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.CombinationSemantics;
import it.unive.lisa.analysis.ProgramState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.interprocedural.ScopeId;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.heap.HeapExpression;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.PushAny;
import it.unive.lisa.type.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

/**
 * The {@link Analysis} used to replay statements over a {@link ReplayDomain}.
 * Before the target of an assignment that is a heap location (e.g., a field of
 * an object) is rewritten into the locations of the abstraction of the heap of
 * the analysis, the write is recorded in the stores of the replayed state, as a
 * write of the heap model (see {@link HeapModel}). The wrapped domain still
 * performs the assignment as during the analysis, so that it can answer the
 * queries of the replay. Assignments of locations of the abstraction of the
 * heap of the analysis that do not come from such a write make the heap
 * unconstrained. The semantics of a statement on one of the combinations of the
 * expressions computed by its operands is evaluated only in the cases computing
 * that combination (see
 * {@link #ofCombination(AnalysisState, ScopeId, ProgramPoint, ProgramPoint[], SymbolicExpression[], CombinationSemantics)}).
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 *
 * @param <A> the kind of {@link AbstractLattice} produced by the wrapped domain
 * @param <D> the kind of the wrapped {@link AbstractDomain}
 */
public class ReplayAnalysis<A extends AbstractLattice<A>, D extends AbstractDomain<A>>
		extends
		Analysis<ReplayState<A>, ReplayDomain<A, D>> {

	/**
	 * The heap locations read at each program point during the replay of the
	 * current node, for each state they are read from.
	 */
	private final Map<ProgramPoint,
			Map<AnalysisState<ReplayState<A>>, Set<SymbolicExpression>>> accessed = new HashMap<>();

	/**
	 * The program points where the accesses to the heap are ambiguous (see
	 * {@link #startNode(Set)}).
	 */
	private Set<ProgramPoint> ambiguous = Collections.emptySet();

	/**
	 * Builds the analysis.
	 *
	 * @param domain           the replay domain
	 * @param shouldSmashError the predicate deciding which errors are smashed
	 *                             (can be {@code null})
	 */
	public ReplayAnalysis(
			ReplayDomain<A, D> domain,
			Predicate<Type> shouldSmashError) {
		super(domain, shouldSmashError);
	}

	/**
	 * {@inheritDoc}<br>
	 * <br>
	 * The semantics is evaluated in the cases of the given state where the
	 * operands computed the given expressions (see
	 * {@link #restrictTo(AnalysisState, ProgramPoint[], SymbolicExpression[])}),
	 * so that, e.g., the value of a branch of a conditional expression is not
	 * paired with the cases of the other one. If these cases are not known
	 * exactly (see
	 * {@link #restrictsExactly(AnalysisState, ProgramPoint[], SymbolicExpression[])}),
	 * the errors raised by the semantics might be raised only in some of them
	 * (e.g., an error raised in all of them might not be raised in the cases
	 * computing another combination): they are raised under an unknown
	 * condition (see {@link #uncertainErrors}).
	 */
	@Override
	public AnalysisState<ReplayState<A>> ofCombination(
			AnalysisState<ReplayState<A>> state,
			ScopeId<?> context,
			ProgramPoint pp,
			ProgramPoint[] operands,
			SymbolicExpression[] computed,
			CombinationSemantics<ReplayState<A>> semantics)
			throws SemanticException {
		boolean exact = restrictsExactly(state, operands, computed);
		AnalysisState<ReplayState<A>> restricted = restrictTo(state, operands, computed);
		AnalysisState<ReplayState<A>> result = semantics.compute(restricted);
		return exact ? result : uncertainErrors(restricted, result, context, pp);
	}

	/**
	 * {@inheritDoc}<br>
	 * <br>
	 * If not all the executions of the given state raise the error, the cases
	 * moved to the error happen under an unknown condition (by assuming an
	 * unknown boolean value, see {@link PushAny}), so that the verification
	 * never takes the error for granted.
	 */
	@Override
	public AnalysisState<ReplayState<A>> moveExecutionToError(
			AnalysisState<ReplayState<A>> state,
			Error exception,
			boolean allExecutions,
			ScopeId<?> context,
			ProgramPoint pp)
			throws SemanticException {
		return super.moveExecutionToError(allExecutions ? state : maybe(state, context, pp), exception, true,
				context, pp);
	}

	/**
	 * {@inheritDoc}<br>
	 * <br>
	 * If not all the executions of {@code erroneous} raise the error, the cases
	 * moved to the error happen under an unknown condition (see
	 * {@link #moveExecutionToError(AnalysisState, Error, boolean, ScopeId, ProgramPoint)}).
	 */
	@Override
	public AnalysisState<ReplayState<A>> moveExecutionToError(
			AnalysisState<ReplayState<A>> normal,
			AnalysisState<ReplayState<A>> erroneous,
			Error exception,
			boolean allExecutions,
			ScopeId<?> context,
			ProgramPoint pp)
			throws SemanticException {
		return super.moveExecutionToError(normal, allExecutions ? erroneous : maybe(erroneous, context, pp),
				exception, true, context, pp);
	}

	// the cases of the given state where an unknown boolean value holds
	private AnalysisState<ReplayState<A>> maybe(
			AnalysisState<ReplayState<A>> state,
			ScopeId<?> context,
			ProgramPoint pp)
			throws SemanticException {
		if (state.getExecution().isBottom())
			return state;
		return assume(state, new PushAny(pp.getProgram().getTypes().getBooleanType(), pp.getLocation()), context, pp,
				pp);
	}

	/**
	 * Yields the cases of the given state where the given operands computed the
	 * given expressions, the {@code i}-th operand computing the {@code i}-th
	 * expression (see
	 * {@link ReplayDomain#restrictTo(ReplayState, ProgramPoint, SymbolicExpression)}).
	 * Expressions beyond the operands, and {@code null} ones, do not restrict
	 * the state.
	 *
	 * @param state    the state
	 * @param operands the operands
	 * @param computed the expressions computed by the operands, in order
	 *
	 * @return the cases of the state where the operands computed the
	 *             expressions
	 */
	public AnalysisState<ReplayState<A>> restrictTo(
			AnalysisState<ReplayState<A>> state,
			ProgramPoint[] operands,
			SymbolicExpression[] computed) {
		AnalysisState<ReplayState<A>> result = state;
		for (int i = 0; i < computed.length && i < operands.length; i++)
			if (computed[i] != null)
				result = restrictTo(result, operands[i], computed[i]);
		return result;
	}

	/**
	 * Yields whether
	 * {@link #restrictTo(AnalysisState, ProgramPoint[], SymbolicExpression[])}
	 * yields exactly the cases of the given state where the given operands
	 * computed the given expressions (see
	 * {@link ReplayDomain#restrictsExactly(ReplayState, ProgramPoint, SymbolicExpression)}):
	 * only then, what happens in all the cases of the restricted state (e.g.,
	 * an error raised in all of them) happens in all the ones computing the
	 * expressions.
	 *
	 * @param state    the state
	 * @param operands the operands
	 * @param computed the expressions computed by the operands, in order
	 *
	 * @return whether the restriction is exact
	 */
	public boolean restrictsExactly(
			AnalysisState<ReplayState<A>> state,
			ProgramPoint[] operands,
			SymbolicExpression[] computed) {
		AnalysisState<ReplayState<A>> current = state;
		for (int i = 0; i < computed.length && i < operands.length; i++)
			if (computed[i] != null) {
				if (!current.getExecution().isBottom()
						&& !domain.restrictsExactly(current.getExecutionState(), operands[i], computed[i]))
					return false;
				current = restrictTo(current, operands[i], computed[i]);
			}
		return true;
	}

	// the cases of the given state where the given program point computed the
	// given expression
	private AnalysisState<ReplayState<A>> restrictTo(
			AnalysisState<ReplayState<A>> state,
			ProgramPoint pp,
			SymbolicExpression computed) {
		if (state.getExecution().isBottom())
			return state;
		ReplayState<A> restricted = domain.restrictTo(state.getExecutionState(), pp, computed);
		if (restricted == state.getExecutionState())
			return state;
		return state.withExecution(new ProgramState<>(
				restricted,
				state.getExecutionExpressions(),
				state.getExecutionInformation()));
	}

	/**
	 * Yields the given result of a semantics evaluated on the given entry
	 * state, where the errors it raised (the ones that are not in the entry
	 * state) and the halting it caused (if the halting state differs from the
	 * entry one) happen under an unknown condition, that is, by assuming an
	 * unknown boolean value (see {@link PushAny}) in their states. The cases of
	 * an error or of the halting state that were already in the entry state are
	 * kept as they are (see
	 * {@link ReplayDomain#assumeOnAdded(ReplayState, ReplayState, SymbolicExpression, ScopeId, ProgramPoint)}).
	 *
	 * @param entry   the entry state of the semantics
	 * @param result  the result of the semantics
	 * @param context the context where the semantics is evaluated
	 * @param pp      the program point raising the errors
	 *
	 * @return the result, with the raised errors and the halting under an
	 *             unknown condition
	 *
	 * @throws SemanticException if an error occurs during the computation
	 */
	public AnalysisState<ReplayState<A>> uncertainErrors(
			AnalysisState<ReplayState<A>> entry,
			AnalysisState<ReplayState<A>> result,
			ScopeId<?> context,
			ProgramPoint pp)
			throws SemanticException {
		if (result.isBottom() || result.isTop())
			return result;
		PushAny unknown = new PushAny(pp.getProgram().getTypes().getBooleanType(), pp.getLocation());
		AnalysisState<ReplayState<A>> weakened = result;
		if (result.getErrors().function != null) {
			Map<Error, ProgramState<ReplayState<A>>> raised = new HashMap<>();
			for (Map.Entry<Error, ProgramState<ReplayState<A>>> e : result.getErrors()) {
				ProgramState<ReplayState<A>> before = entry.getErrors().function == null ? null
						: entry.getErrors().function.get(e.getKey());
				if (!e.getValue().equals(before))
					raised.put(e.getKey(), weaken(before, e.getValue(), unknown, context, pp));
			}
			if (!raised.isEmpty())
				weakened = weakened.removeErrors(raised.keySet()).addErrors(raised);
		}
		ProgramState<ReplayState<A>> smashed = result.getSmashedErrorsState();
		ProgramState<ReplayState<A>> smashedBefore = entry.getSmashedErrorsState();
		if (!smashed.isBottom() && !smashed.equals(smashedBefore))
			weakened = weakened.withSmashedErrorsState(weaken(smashedBefore, smashed, unknown, context, pp));
		// the same holds for halting: a halt reached by the semantics (e.g.,
		// a callee calling an exit function) happens only in some of the
		// cases it was evaluated on
		ProgramState<ReplayState<A>> halt = result.getHalt();
		ProgramState<ReplayState<A>> haltBefore = entry.getHalt();
		if (!halt.isBottom() && !halt.equals(haltBefore))
			weakened = weakened.withHalt(weaken(haltBefore, halt, unknown, context, pp));
		return weakened;
	}

	// the given state after the semantics, where only the cases added by the
	// semantics happen under the given unknown condition: the ones that were
	// already in the state before it are kept as they are
	private ProgramState<ReplayState<A>> weaken(
			ProgramState<ReplayState<A>> before,
			ProgramState<ReplayState<A>> after,
			PushAny unknown,
			ScopeId<?> context,
			ProgramPoint pp)
			throws SemanticException {
		if (after.isBottom())
			return after;
		ReplayState<A> assumed = before == null || before.isBottom()
				? domain.assume(after.getState(), unknown, context, pp, pp)
				: domain.assumeOnAdded(before.getState(), after.getState(), unknown, context, pp);
		return new ProgramState<>(assumed, after.getComputedExpressions(), after.getFixpointInformation());
	}

	/**
	 * Starts the replay of a node. A program point where the semantics reads
	 * more than one heap location from the same state (e.g., one per cell that
	 * an array access might read) yields alternatives that over-approximate the
	 * executions (while locations read from different states, e.g., when
	 * initializing the cells of an array one after the other, are not): the
	 * replay joins them, losing which location goes with which execution (e.g.,
	 * the cell with the value of the index). The node is then replayed again,
	 * passing those points: each access there happens under an unknown
	 * condition, so that the executions through them are never precise.
	 *
	 * @param ambiguous the program points where the accesses to the heap are
	 *                      ambiguous
	 */
	public void startNode(
			Set<ProgramPoint> ambiguous) {
		accessed.clear();
		this.ambiguous = ambiguous;
	}

	/**
	 * Yields the program points where the replay of the current node read more
	 * than one heap location from the same state (see {@link #startNode(Set)}).
	 *
	 * @return the program points
	 */
	public Set<ProgramPoint> getAmbiguous() {
		Set<ProgramPoint> result = new HashSet<>();
		for (Map.Entry<ProgramPoint, Map<AnalysisState<ReplayState<A>>, Set<SymbolicExpression>>> e : accessed
				.entrySet())
			for (Set<SymbolicExpression> read : e.getValue().values())
				if (read.size() > 1)
					result.add(e.getKey());
		return result;
	}

	@Override
	public AnalysisState<ReplayState<A>> smallStepSemantics(
			AnalysisState<ReplayState<A>> state,
			SymbolicExpression expression,
			ScopeId<?> context,
			ProgramPoint pp)
			throws SemanticException {
		if (expression instanceof AccessChild) {
			accessed.computeIfAbsent(pp, k -> new IdentityHashMap<>())
					.computeIfAbsent(state, k -> new HashSet<>())
					.add(expression);
			if (ambiguous.contains(pp)) {
				SummaryMarker choice = new SummaryMarker(
						pp.getProgram().getTypes().getBooleanType(),
						"choice among the heap locations accessed by " + pp, pp.getLocation(),
						SummaryValue.Kind.HEAP);
				state = mapStore(state, s -> s.assume(choice, pp));
			}
		}
		return super.smallStepSemantics(state, expression, context, pp);
	}

	@Override
	public AnalysisState<ReplayState<A>> assign(
			AnalysisState<ReplayState<A>> state,
			Identifier id,
			SymbolicExpression expression,
			ScopeId<?> context,
			ProgramPoint pp)
			throws SemanticException {
		if (SymbolicCase.isHeap(id))
			// a location of the abstraction of the heap of the analysis,
			// assigned without a write of the program that the heap model
			// can represent
			state = mapStore(state, SymbolicStore::heapHavoc);
		return super.assign(state, id, expression, context, pp);
	}

	@Override
	public AnalysisState<ReplayState<A>> assign(
			AnalysisState<ReplayState<A>> state,
			SymbolicExpression id,
			SymbolicExpression expression,
			ScopeId<?> context,
			ProgramPoint pp)
			throws SemanticException {
		if (id instanceof AccessChild)
			state = mapStore(state, s -> s.heapWrite((AccessChild) id, expression, pp));
		else if (!(id instanceof Identifier) && id instanceof HeapExpression)
			// a write that the heap model does not represent
			state = mapStore(state, SymbolicStore::heapHavoc);
		return super.assign(state, id, expression, context, pp);
	}

	private static <A extends AbstractLattice<A>> AnalysisState<ReplayState<A>> mapStore(
			AnalysisState<ReplayState<A>> state,
			UnaryOperator<SymbolicStore> f) {
		ProgramState<ReplayState<A>> exec = state.getExecution();
		if (exec.isBottom())
			return state;
		ReplayState<A> replay = exec.getState();
		return state.withExecution(new ProgramState<>(
				replay.withStore(f.apply(replay.getStore())),
				exec.getComputedExpressions(),
				exec.getFixpointInformation()));
	}
}
