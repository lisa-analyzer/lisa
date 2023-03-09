package it.unive.lisa.analysis;

import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.value.TypeDomain;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.conf.LiSAConfiguration;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.logging.TimerLogger;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.edge.Edge;
import it.unive.lisa.program.cfg.fixpoints.AscendingFixpoint;
import it.unive.lisa.program.cfg.fixpoints.CFGFixpoint.CompoundState;
import it.unive.lisa.program.cfg.fixpoints.OptimizedFixpoint;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.util.collections.workset.FIFOWorkingSet;
import it.unive.lisa.util.datastructures.graph.algorithms.Fixpoint;
import it.unive.lisa.util.datastructures.graph.algorithms.FixpointException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * An {@link AnalyzedCFG} that has been built using an
 * {@link OptimizedFixpoint}. This means that this graph will only contain
 * results for widening points (that is, {@link Statement}s that part of
 * {@link #getCycleEntries()}), exit statements (that is, {@link Statement}s
 * such that {@link Statement#stopsExecution()} holds), and hotspots (that is,
 * {@link Statement}s such that {@link LiSAConfiguration#hotspots} holds).
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <A> the type of {@link AbstractState} contained into the analysis
 *                state
 * @param <H> the type of {@link HeapDomain} contained into the computed
 *                abstract state
 * @param <V> the type of {@link ValueDomain} contained into the computed
 *                abstract state
 * @param <T> the type of {@link TypeDomain} embedded into the computed abstract
 *                state
 */
public class OptimizedAnalyzedCFG<
		A extends AbstractState<A, H, V, T>,
		H extends HeapDomain<H>,
		V extends ValueDomain<V>,
		T extends TypeDomain<T>>
		extends AnalyzedCFG<A, H, V, T> {

	private static final Logger LOG = LogManager.getLogger(OptimizedAnalyzedCFG.class);

	private final InterproceduralAnalysis<A, H, V, T> interprocedural;

	private StatementStore<A, H, V, T> expanded;

	/**
	 * Builds the control flow graph, storing the given mapping between nodes
	 * and fixpoint computation results.
	 * 
	 * @param cfg             the original control flow graph
	 * @param singleton       an instance of the {@link AnalysisState}
	 *                            containing the abstract state of the analysis
	 *                            that was executed, used to retrieve top and
	 *                            bottom values
	 * @param interprocedural the analysis that have been used to produce this
	 *                            result, and that can be used to unwind the
	 *                            results
	 */
	public OptimizedAnalyzedCFG(CFG cfg,
			AnalysisState<A, H, V, T> singleton,
			InterproceduralAnalysis<A, H, V, T> interprocedural) {
		super(cfg, singleton);
		this.interprocedural = interprocedural;
	}

	/**
	 * Builds the control flow graph, storing the given mapping between nodes
	 * and fixpoint computation results.
	 * 
	 * @param cfg             the original control flow graph
	 * @param singleton       an instance of the {@link AnalysisState}
	 *                            containing the abstract state of the analysis
	 *                            that was executed, used to retrieve top and
	 *                            bottom values
	 * @param entryStates     the entry state for each entry point of the cfg
	 * @param results         the results of the fixpoint computation
	 * @param interprocedural the analysis that have been used to produce this
	 *                            result, and that can be used to unwind the
	 *                            results
	 */
	public OptimizedAnalyzedCFG(CFG cfg,
			AnalysisState<A, H, V, T> singleton,
			Map<Statement, AnalysisState<A, H, V, T>> entryStates,
			Map<Statement, AnalysisState<A, H, V, T>> results,
			InterproceduralAnalysis<A, H, V, T> interprocedural) {
		super(cfg, singleton, entryStates, results);
		this.interprocedural = interprocedural;
	}

	/**
	 * Builds the control flow graph, storing the given mapping between nodes
	 * and fixpoint computation results.
	 * 
	 * @param cfg             the original control flow graph
	 * @param entryStates     the entry state for each entry point of the cfg
	 * @param results         the results of the fixpoint computation
	 * @param interprocedural the analysis that have been used to produce this
	 *                            result, and that can be used to unwind the
	 *                            results
	 */
	public OptimizedAnalyzedCFG(CFG cfg,
			StatementStore<A, H, V, T> entryStates,
			StatementStore<A, H, V, T> results,
			InterproceduralAnalysis<A, H, V, T> interprocedural) {
		super(cfg, entryStates, results);
		this.interprocedural = interprocedural;
	}

	/**
	 * Yields the computed result at a given statement (exit state). If such a
	 * state is not available as it was discarded due to optimization, and
	 * fixpoint's results have not been unwinded yet, a fixpoint iteration is
	 * executed in-place.
	 *
	 * @param st the statement
	 *
	 * @return the result computed at the given statement
	 */
	public AnalysisState<A, H, V, T> getUnwindedAnalysisStateAfter(Statement st) {
		if (results.getKeys().contains(st))
			return results.getState(st);

		if (expanded != null)
			return expanded.getState(st);

		AnalysisState<A, H, V, T> bottom = results.lattice.bottom();
		Map<Statement, CompoundState<A, H, V, T>> starting = new HashMap<>();
		for (Entry<Statement, AnalysisState<A, H, V, T>> entry : entryStates)
			starting.put(entry.getKey(), CompoundState.of(entry.getValue(), new StatementStore<>(bottom)));

		Map<Statement, CompoundState<A, H, V, T>> existing = new HashMap<>();
		StatementStore<A, H, V, T> bot = new StatementStore<>(bottom);
		CompoundState<A, H, V, T> fallback = CompoundState.of(bottom, bot);
		for (Entry<Statement, AnalysisState<A, H, V, T>> entry : results) {
			Statement stmt = entry.getKey();
			AnalysisState<A, H, V, T> approx = entry.getValue();
			CompoundState<A, H, V, T> def = existing.getOrDefault(stmt, fallback);

			if (!(stmt instanceof Expression) || ((Expression) stmt).getRootStatement() == stmt)
				existing.put(stmt, CompoundState.of(approx, def.intermediateStates));
			else {
				Expression e = (Expression) stmt;
				CompoundState<A, H, V, T> val = existing.computeIfAbsent(e.getRootStatement(),
						ex -> CompoundState.of(bottom, bot.bottom()));
				val.intermediateStates.put(e, approx);
			}
		}

		AscendingFixpoint<A, H, V, T> asc = new AscendingFixpoint<>(this, 0, interprocedural);
		Fixpoint<CFG, Statement, Edge, CompoundState<A, H, V, T>> fix = new Fixpoint<>(this, true);
		TimerLogger.execAction(LOG, "Unwinding optimizied results of " + this, () -> {
			try {
				Map<Statement,
						CompoundState<A, H, V, T>> res = fix.fixpoint(starting, FIFOWorkingSet.mk(), asc, existing);
				expanded = new StatementStore<>(bottom);
				for (Entry<Statement, CompoundState<A, H, V, T>> e : res.entrySet()) {
					expanded.put(e.getKey(), e.getValue().postState);
					for (Entry<Statement, AnalysisState<A, H, V, T>> ee : e.getValue().intermediateStates)
						expanded.put(ee.getKey(), ee.getValue());
				}
			} catch (FixpointException e) {
				LOG.error("Unable to unwind optimized results of " + this, e);
			}
		});

		return expanded.getState(st);
	}
}
