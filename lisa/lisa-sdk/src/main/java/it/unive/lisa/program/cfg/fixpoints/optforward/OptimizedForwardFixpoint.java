package it.unive.lisa.program.cfg.fixpoints.optforward;

import static java.lang.String.format;

import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.edge.Edge;
import it.unive.lisa.program.cfg.fixpoints.CompoundState;
import it.unive.lisa.program.cfg.fixpoints.forward.ForwardCFGFixpoint;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.util.collections.workset.WorkingSet;
import it.unive.lisa.util.datastructures.graph.Graph;
import it.unive.lisa.util.datastructures.graph.algorithms.FixpointException;
import it.unive.lisa.util.datastructures.graph.algorithms.ForwardFixpoint;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Predicate;
import org.apache.commons.lang3.StringUtils;

/**
 * An optimized {@link ForwardFixpoint} for {@link CFGs}. This fixpoint
 * algorithm is optimized: it works exploiting the basic blocks of the target
 * graph, and only yields approximations of widening points, stopping statements
 * and user-defined hotspots.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <A> the type of {@link AbstractDomain} contained into the analysis
 *                state
 * @param <D> the kind of {@link AbstractDomain} to run during the analysis
 */
public abstract class OptimizedForwardFixpoint<
		A extends AbstractLattice<A>,
		D extends AbstractDomain<A>>
		extends
		ForwardCFGFixpoint<A, D> {

	/**
	 * The predicate to identify additional statements whose approximation must
	 * be preserved in the results.
	 */
	protected final Predicate<Statement> hotspots;

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
	 *                                statements whose approximation must be
	 *                                preserved in the results
	 */
	public OptimizedForwardFixpoint(
			CFG graph,
			boolean forceFullEvaluation,
			InterproceduralAnalysis<A, D> interprocedural,
			Predicate<Statement> hotspots) {
		super(graph, forceFullEvaluation, interprocedural);
		this.hotspots = hotspots;
	}

	@Override
	public Map<Statement, CompoundState<A>> fixpoint(
			Map<Statement, CompoundState<A>> startingPoints,
			WorkingSet<Statement> ws,
			Map<Statement, CompoundState<A>> initialResult)
			throws FixpointException {
		Map<Statement, CompoundState<A>> result = initialResult == null ? new HashMap<>(graph.getNodesCount())
				: new HashMap<>(initialResult);

		Map<Statement, Statement[]> bbs = graph.getBasicBlocks();
		startingPoints.keySet().forEach(ws::push);

		Set<Statement> toProcess = null;
		if (forceFullEvaluation)
			toProcess = new HashSet<>(bbs.keySet());

		CompoundState<A> newApprox;
		while (!ws.isEmpty()) {
			Statement current = ws.pop();

			if (current == null)
				throw new FixpointException("null node encountered during fixpoint in '" + graph + "'");
			if (!graph.containsNode(current))
				throw new FixpointException("'" + current + "' is not part of '" + graph + "'");

			Statement[] bb = bbs.get(current);
			if (bb == null)
				throw new FixpointException("'" + current + "' is not the leader of a basic block of '" + graph + "'");

			CompoundState<
					A> entrystate = getEntryState(graph, current, startingPoints.get(current), result);
			if (entrystate == null)
				throw new FixpointException("'" + current + "' does not have an entry state");

			newApprox = analyze(result, entrystate, bb);

			Statement closing = bb[bb.length - 1];
			CompoundState<A> oldApprox = result.get(closing);
			if (oldApprox != null)
				try {
					newApprox = join(closing, newApprox, oldApprox);
				} catch (Exception e) {
					throw new FixpointException(format(ERROR, "joining states", closing, graph), e);
				}

			try {
				// we go on if we were asked to analyze all nodes at least once
				if ((forceFullEvaluation && toProcess.remove(current))
						// or if this is the first time we analyze this node
						|| oldApprox == null
						// or if we got a result that should not be considered
						// equal
						|| !leq(closing, newApprox, oldApprox)) {
					result.put(closing, newApprox);
					for (Statement instr : graph.followersOf(closing))
						ws.push(instr);
				}
			} catch (Exception e) {
				throw new FixpointException(format(ERROR, "updating result", closing, graph), e);
			}
		}

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

		return result;
	}

	private CompoundState<A> analyze(
			Map<Statement, CompoundState<A>> result,
			CompoundState<A> entrystate,
			Statement[] bb)
			throws FixpointException {
		StatementStore<A> emptyIntermediate = entrystate.intermediateStates.bottom();
		CompoundState<A> newApprox = CompoundState.of(entrystate.postState.bottom(), emptyIntermediate);
		CompoundState<A> entry = entrystate;
		for (int i = 0; i < bb.length; i++) {
			Statement cursor = bb[i];
			try {
				newApprox = semantics(cursor, entry);

				// storing approximations into result is a trick: it won't ever
				// be used in fixpoint comparisons, but it will still make
				// it out as part of the final result
				for (Entry<Statement, AnalysisState<A>> intermediate : newApprox.intermediateStates)
					if (intermediate.getKey().stopsExecution()
							|| (hotspots != null && hotspots.test(intermediate.getKey())))
						result.put(intermediate.getKey(), CompoundState.of(intermediate.getValue(), emptyIntermediate));
				if (cursor != bb[bb.length - 1]
						&& (cursor.stopsExecution() || (hotspots != null && hotspots.test(cursor))))
					result.put(cursor, CompoundState.of(newApprox.postState, emptyIntermediate));

				if (i < bb.length - 1) {
					Collection<Edge> edges = graph.getEdgesConnecting(cursor, bb[i + 1]);
					if (edges.size() != 1)
						throw new FixpointException("More than one edge connecting " + cursor + " and " + bb[i + 1]
								+ " in the basic block: " + StringUtils.join(edges, ", "));
					// we still invoke traverse as there is variable scoping
					// handling happening there
					entry = traverse(edges.iterator().next(), newApprox);
				}
			} catch (Exception e) {
				throw new FixpointException(format(ERROR, "computing semantics", cursor, graph), e);
			}
		}

		return CompoundState.of(newApprox.postState, emptyIntermediate);
	}

}
