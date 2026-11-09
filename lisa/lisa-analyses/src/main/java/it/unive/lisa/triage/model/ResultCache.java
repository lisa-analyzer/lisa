package it.unive.lisa.triage.model;

import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.AnalyzedCFG;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.interprocedural.ScopeId;
import it.unive.lisa.interprocedural.callgraph.CallGraph;
import it.unive.lisa.program.cfg.CodeMember;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.triage.reachability.Reachability;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;


/**
 * A cache of the information computed from the results of an analysis while
 * building alarm models, shared by all the models built over the same results
 * (e.g., the ones of the rounds expanding more calls, of the chains of callers,
 * and of different warnings): the states before the nodes of the results (see
 * {@link Reachability#preStateOf(AnalyzedCFG, Statement, Analysis)}), and the
 * results obtained by merging the ones of a callee (see
 * {@link ReplayInterprocedural}). Results are identified by identity, since
 * their states are immutable.
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 *
 * @param <A> the type of the abstract states of the analysis
 */
public class ResultCache<A extends AbstractLattice<A>> {

	private final Map<AnalyzedCFG<A>, Map<Statement, AnalysisState<A>>> preStates = new IdentityHashMap<>();

	private final Map<Collection<AnalyzedCFG<A>>, Map<ScopeId<A>, AnalyzedCFG<A>>> merged = new IdentityHashMap<>();

	/**
	 * The global variables written by the code of each member, as the frontend
	 * knows them.
	 */
	private GlobalWrites globalWrites = GlobalWrites.UNKNOWN;

	/**
	 * For each member, the global variables written by its code and by the
	 * code of the members it calls, transitively ({@code null} if unknown).
	 */
	private final Map<CodeMember, Optional<Set<String>>> transitiveWrites = new HashMap<>();

	/**
	 * Sets the global variables written by the code of each member of the
	 * program, as the frontend knows them (by default, none is known).
	 *
	 * @param globalWrites the global writes
	 */
	public void setGlobalWrites(
			GlobalWrites globalWrites) {
		this.globalWrites = globalWrites;
		transitiveWrites.clear();
	}

	/**
	 * Yields the names of the global variables that the given members might
	 * write, also through the members they call, transitively, as the given
	 * call graph tells them, or {@code null} if they are not known (see
	 * {@link GlobalWrites}).
	 *
	 * @param members   the members
	 * @param callgraph the call graph
	 *
	 * @return the names, or {@code null}
	 */
	public Set<String> globalsWrittenBy(
			Collection<? extends CodeMember> members,
			CallGraph callgraph) {
		Set<String> result = new HashSet<>();
		for (CodeMember member : members) {
			Optional<Set<String>> written = transitiveWrites.get(member);
			if (written == null) {
				written = Optional.ofNullable(transitively(member, callgraph));
				transitiveWrites.put(member, written);
			}
			if (written.isEmpty())
				return null;
			result.addAll(written.get());
		}
		return result;
	}

	private Set<String> transitively(
			CodeMember member,
			CallGraph callgraph) {
		Set<String> result = new HashSet<>();
		Set<CodeMember> seen = new HashSet<>();
		LinkedList<CodeMember> ws = new LinkedList<>();
		ws.add(member);
		while (!ws.isEmpty()) {
			CodeMember current = ws.removeFirst();
			if (!seen.add(current))
				continue;
			Set<String> written = globalWrites.writtenBy(current);
			if (written == null)
				return null;
			result.addAll(written);
			if (callgraph == null)
				return null;
			for (CodeMember callee : callgraph.getCallees(current))
				// initializers do nothing once the variables they initialize
				// have been written (see GlobalWrites)
				if (!globalWrites.isInitializer(callee))
					ws.add(callee);
		}
		return result;
	}

	/**
	 * Yields the state before the given node of the given result, as computed
	 * by {@link Reachability#preStateOf(AnalyzedCFG, Statement, Analysis)}. The
	 * analysis is only used if the state is not cached: any analysis running
	 * the domain of the one that computed the results can be used.
	 *
	 * @param <D>      the type of the domain of the analysis
	 * @param result   the result
	 * @param node     the node
	 * @param analysis the analysis used to compute the state
	 *
	 * @return the state before the node
	 *
	 * @throws SemanticException if something goes wrong during the computation
	 */
	public <D extends AbstractDomain<A>> AnalysisState<A> preStateOf(
			AnalyzedCFG<A> result,
			Statement node,
			Analysis<A, D> analysis)
			throws SemanticException {
		Map<Statement, AnalysisState<A>> states = preStates.computeIfAbsent(result, r -> new HashMap<>());
		AnalysisState<A> state = states.get(node);
		if (state == null) {
			state = Reachability.preStateOf(result, node, analysis);
			states.put(node, state);
		}
		return state;
	}

	/**
	 * Yields the result obtained by merging the given results with the given
	 * context, computing it through the given supplier if it is not cached. The
	 * supplier might yield {@code null}, if the results cannot be merged. The
	 * collection of results is identified by identity (e.g., the one of all the
	 * results of a cfg, as stored by the tool holding them).
	 *
	 * @param results the results to merge
	 * @param id      the context of the merged result
	 * @param merge   the supplier computing the merged result
	 *
	 * @return the merged result, or {@code null} if the results cannot be
	 *             merged
	 */
	public AnalyzedCFG<A> merged(
			Collection<AnalyzedCFG<A>> results,
			ScopeId<A> id,
			Supplier<AnalyzedCFG<A>> merge) {
		Map<ScopeId<A>, AnalyzedCFG<A>> byId = merged.computeIfAbsent(results, r -> new HashMap<>());
		if (byId.containsKey(id))
			return byId.get(id);
		AnalyzedCFG<A> res = merge.get();
		byId.put(id, res);
		return res;
	}
}
