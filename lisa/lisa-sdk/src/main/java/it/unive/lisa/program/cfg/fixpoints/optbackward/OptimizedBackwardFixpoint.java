package it.unive.lisa.program.cfg.fixpoints.optbackward;

import static java.lang.String.format;

import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.checks.semantic.SemanticCheck;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.fixpoints.CompoundState;
import it.unive.lisa.program.cfg.fixpoints.backward.BackwardCFGFixpoint;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.util.datastructures.graph.Graph;
import it.unive.lisa.util.datastructures.graph.algorithms.BackwardFixpoint;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Predicate;
import org.apache.commons.lang3.tuple.Pair;

/**
 * An optimized {@link BackwardFixpoint} for {@link CFG}s. This fixpoint
 * algorithm is optimized: it works exploiting the basic blocks of the target
 * graph, and only yields approximations of widening points, stopping statements
 * and user-defined hotspots. These are identified through a predicate over
 * statements (also considering intermediate ones) for which the fixpoint
 * results must be kept. This is useful for avoiding result unwinding due to
 * {@link SemanticCheck}s querying for the post-state of statements. Note that
 * statements for which {@link Statement#stopsExecution()} is {@code true} are
 * always considered hotspots.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <A> the type of {@link AbstractDomain} contained into the analysis
 *                state
 * @param <D> the kind of {@link AbstractDomain} to run during the analysis
 */
public abstract class OptimizedBackwardFixpoint<
		A extends AbstractLattice<A>,
		D extends AbstractDomain<A>>
		extends
		BackwardCFGFixpoint<A, D> {

	/**
	 * The predicate to identify additional statements (also considering
	 * intermediate ones) for which the fixpoint results must be kept. This is
	 * useful for avoiding result unwinding due to {@link SemanticCheck}s
	 * querying for the post-state of statements. Note that statements for which
	 * {@link Statement#stopsExecution()} is {@code true} are always considered
	 * hotspots.
	 */
	protected final Predicate<Statement> hotspots;

	private final Map<Statement, Statement[]> bbs;

	/**
	 * Builds an optimized fixpoint for the given {@link Graph}.
	 * 
	 * @param graph               the source graph
	 * @param forceFullEvaluation whether or not the fixpoint should evaluate
	 *                                all nodes independently of the fixpoint
	 *                                implementation
	 * @param interprocedural     the {@link InterproceduralAnalysis} to use for
	 *                                semantics invocation
	 * @param hotspots            the predicate to identify additional
	 *                                statements (also considering intermediate
	 *                                ones) for which the fixpoint results must
	 *                                be kept. This is useful for avoiding
	 *                                result unwinding due to
	 *                                {@link SemanticCheck}s querying for the
	 *                                post-state of statements. Note that
	 *                                statements for which
	 *                                {@link Statement#stopsExecution()} is
	 *                                {@code true} are always considered
	 *                                hotspots
	 */
	public OptimizedBackwardFixpoint(
			CFG graph,
			boolean forceFullEvaluation,
			InterproceduralAnalysis<A, D> interprocedural,
			Predicate<Statement> hotspots) {
		super(graph, forceFullEvaluation, interprocedural);
		this.hotspots = hotspots;
		// graph might be null if this is just used as a factory
		if (graph == null)
			this.bbs = null;
		else {
			this.bbs = new HashMap<>();
			for (Entry<Statement, Statement[]> bb : graph.getBasicBlocks().entrySet()) {
				// we store the basic blocks as <closing statement, block>
				Statement[] block = bb.getValue();
				bbs.put(block[block.length - 1], block);
			}
		}
	}

	@Override
	public Pair<CompoundState<A>, Statement> semantics(
			Statement node,
			CompoundState<A> exitstate,
			Map<Statement, CompoundState<A>> result)
			throws SemanticException {
		Statement[] bb = bbs.get(node);
		if (bb == null)
			throw new SemanticException("'" + node + "' is not the leader of a basic block of '" + graph + "'");
		StatementStore<A> emptyIntermediate = exitstate.intermediateStates.bottom();
		CompoundState<A> newApprox = CompoundState.of(exitstate.postState.bottom(), emptyIntermediate);
		CompoundState<A> exit = exitstate;
		for (int i = bb.length - 1; i >= 0; i--)
			try {
				Statement cursor = bb[i];
				Pair<CompoundState<A>, Statement> r = super.semantics(cursor, exit, result);
				newApprox = r.getLeft();

				// storing approximations into result is a trick: it won't ever
				// be used in fixpoint comparisons, but it will still make
				// it out as part of the final result
				for (Entry<Statement, AnalysisState<A>> intermediate : newApprox.intermediateStates)
					if (intermediate.getKey().stopsExecution()
							|| (hotspots != null && hotspots.test(intermediate.getKey())))
						result.put(intermediate.getKey(), CompoundState.of(intermediate.getValue(), emptyIntermediate));
				if (cursor != bb[0] && (cursor.stopsExecution() || (hotspots != null && hotspots.test(cursor))))
					result.put(cursor, CompoundState.of(newApprox.postState, emptyIntermediate));

				exit = newApprox;
			} catch (Exception e) {
				throw new SemanticException(format(ERROR, "computing semantics", bb[i], graph), e);
			}

		return Pair.of(CompoundState.of(newApprox.postState, emptyIntermediate), bb[bb.length - 1]);
	}

	@Override
	protected void cleanup(
			Map<Statement, CompoundState<A>> result) {
		// cleanup: theoretically, we can reconstruct the full results by
		// storing only the pre-states of the entrypoints and the post-states of
		// the widening-points. we additionally store the post-states of
		// stopping statements as those will be frequently queried by
		// interprocedural analyses during the fixpoint, so that we
		// can delay unwinding. we also store hotspots
		Collection<Statement> cleanup = new HashSet<>();
		Collection<Statement> wideningPoints = graph.getCycleEntries();
		for (Statement st : result.keySet())
			if (!wideningPoints.contains(st) && !st.stopsExecution() && (hotspots == null || !hotspots.test(st)))
				cleanup.add(st);
		cleanup.forEach(result::remove);
	}

	@Override
	public boolean isOptimized() {
		return true;
	}

	@Override
	public BackwardCFGFixpoint<A, D> asOptimized() {
		return this;
	}

	@Override
	public BackwardCFGFixpoint<A, D> asBackward() {
		return this;
	}
}
