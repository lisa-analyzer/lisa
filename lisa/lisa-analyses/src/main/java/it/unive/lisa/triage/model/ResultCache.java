package it.unive.lisa.triage.model;

import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.AnalyzedCFG;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.interprocedural.ScopeId;
import it.unive.lisa.interprocedural.callgraph.CallGraph;
import it.unive.lisa.program.SyntheticLocation;
import it.unive.lisa.program.cfg.CodeMember;
import it.unive.lisa.program.cfg.Parameter;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.triage.reachability.Reachability;
import it.unive.lisa.type.Untyped;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
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

	private final Map<Collection<AnalyzedCFG<A>>,
			Map<String, List<AnalyzedCFG<A>>>> sameCalls = new IdentityHashMap<>();

	/**
	 * The global variables written by the code of each member, as the frontend
	 * knows them.
	 */
	private GlobalWrites globalWrites = GlobalWrites.UNKNOWN;

	/**
	 * For each member, the global variables written by its code and by the code
	 * of the members it calls, transitively ({@code null} if unknown).
	 */
	private final Map<CodeMember, Optional<Set<String>>> transitiveWrites = new HashMap<>();

	/**
	 * The identifier standing for the value returned by a member in its
	 * summaries (see {@link #summaryOf(CodeMember)}).
	 */
	public static final Variable RETURNED = new Variable(Untyped.INSTANCE, "$returned", SyntheticLocation.INSTANCE);

	/**
	 * The facts known to hold whenever each member returns, over its formal
	 * parameters and {@link #RETURNED}.
	 */
	private final Map<CodeMember, List<SymbolicExpression>> summaries = new HashMap<>();

	/**
	 * Whether the summaries have been computed.
	 */
	private boolean summarized;

	/**
	 * Yields the facts known to hold whenever the given member returns: boolean
	 * expressions over the formal parameters of the member (as variables named
	 * after them, that the member never assigns) and {@link #RETURNED}, the
	 * returned value. They are assumed on the results of the calls to the
	 * member that are not expanded because of recursion.
	 *
	 * @param member the member
	 *
	 * @return the facts (empty if none is known)
	 */
	public List<SymbolicExpression> summaryOf(
			CodeMember member) {
		return summaries.getOrDefault(member, Collections.emptyList());
	}

	/**
	 * Sets the facts known to hold whenever the given member returns (see
	 * {@link #summaryOf(CodeMember)}).
	 *
	 * @param member the member
	 * @param facts  the facts
	 */
	public void setSummary(
			CodeMember member,
			List<SymbolicExpression> facts) {
		if (facts.isEmpty())
			summaries.remove(member);
		else
			summaries.put(member, facts);
	}

	/**
	 * Yields the variable standing for the given formal parameter in the
	 * summaries of its member (see {@link #summaryOf(CodeMember)}): variables
	 * are identified by their names.
	 *
	 * @param formal the formal parameter
	 *
	 * @return the variable
	 */
	public static Variable formal(
			Parameter formal) {
		return new Variable(formal.getStaticType(), formal.getName(), formal.getLocation());
	}

	/**
	 * Whether the conditions of the models are decided by the analysis to
	 * strengthen their seeds.
	 */
	private boolean decidesConditions = true;

	/**
	 * Yields whether the conditions of the models built with this cache are
	 * decided by the analysis, so that the ones it decides become seeds of the
	 * models (default: {@code true}).
	 *
	 * @return whether the conditions are decided by the analysis
	 */
	public boolean decidesConditions() {
		return decidesConditions;
	}

	/**
	 * Sets whether the conditions of the models built with this cache are
	 * decided by the analysis (see {@link #decidesConditions()}): this only
	 * affects the precision of the seeds of the models, and deciding complex
	 * conditions might be expensive for some analyses.
	 *
	 * @param decidesConditions whether the conditions are decided by the
	 *                              analysis
	 */
	public void setDecidesConditions(
			boolean decidesConditions) {
		this.decidesConditions = decidesConditions;
	}

	/**
	 * Yields whether the summaries of the members have been computed.
	 *
	 * @return whether the summaries have been computed
	 */
	public boolean isSummarized() {
		return summarized;
	}

	/**
	 * Records that the summaries of the members have been computed.
	 */
	public void setSummarized() {
		summarized = true;
	}

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
	 * Yields the results, among the given ones, whose contexts are made of the
	 * same calls as the given one, whatever the states they record (compared
	 * through their textual representation, as
	 * {@link it.unive.lisa.triage.reachability.Reachability#callSitesOf(AnalyzedCFG, boolean)}
	 * does), or all the given results if none is. The same collection is
	 * yielded for the same results and calls, so that its merges are cached
	 * (see {@link #merged(Collection, ScopeId, Supplier)}).
	 *
	 * @param results the results, identified by identity
	 * @param id      the context
	 *
	 * @return the results with the same calls
	 */
	public Collection<AnalyzedCFG<A>> withSameCalls(
			Collection<AnalyzedCFG<A>> results,
			ScopeId<A> id) {
		return sameCalls.computeIfAbsent(results, r -> new HashMap<>()).computeIfAbsent(id.toString(), calls -> {
			List<AnalyzedCFG<A>> same = new ArrayList<>();
			for (AnalyzedCFG<A> result : results)
				if (result.getId() != null && calls.equals(result.getId().toString()))
					same.add(result);
			return same.isEmpty() ? new ArrayList<>(results) : same;
		});
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
		// a merge that failed (e.g., because of a deadline) is not cached
		if (res != null)
			byId.put(id, res);
		return res;
	}
}
