package it.unive.lisa.triage.model;

import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.ProgramState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.interprocedural.ScopeId;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.heap.HeapExpression;
import it.unive.lisa.symbolic.value.Identifier;
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
 * unconstrained.
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
	private final Map<ProgramPoint, Map<AnalysisState<ReplayState<A>>, Set<SymbolicExpression>>> accessed = new HashMap<>();

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
	 * Starts the replay of a node. A program point where the semantics reads
	 * more than one heap location from the same state (e.g., one per cell that
	 * an array access might read) yields alternatives that over-approximate
	 * the executions (while locations read from different states, e.g., when
	 * initializing the cells of an array one after the other, are not):
	 * the replay joins them, losing which location goes with which execution
	 * (e.g., the cell with the value of the index). The node is then replayed
	 * again, passing those points: each access there happens under an unknown
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
	 * than one heap location from the same state (see
	 * {@link #startNode(Set)}).
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
