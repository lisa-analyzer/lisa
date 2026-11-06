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
