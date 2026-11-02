package it.unive.lisa.triage.model;

import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.AnalysisState.Error;
import it.unive.lisa.analysis.AnalyzedCFG;
import it.unive.lisa.analysis.ProgramState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.checks.semantic.SemanticTool;
import it.unive.lisa.lattices.GenericMapLattice;
import it.unive.lisa.lattices.GenericSetLattice;
import it.unive.lisa.program.cfg.edge.Edge;
import it.unive.lisa.program.cfg.fixpoints.forward.ForwardCFGFixpoint;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.triage.reachability.Reachability;
import it.unive.lisa.type.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Extracts the symbolic transitions performed by the nodes of an analysis
 * result (an {@link AnalyzedCFG}, i.e., a cfg in a given context), by replaying
 * their semantics through a {@link ReplayDomain} (see {@link NodeTransitions}).
 * Each node is replayed starting from its state before the node, as computed by
 * the analysis (see
 * {@link Reachability#preStateOf(AnalyzedCFG, Statement, Analysis)}), paired
 * with the identity transition.
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 *
 * @param <A> the type of the abstract states of the analysis
 * @param <D> the type of the domain of the analysis
 */
public class TransitionExtractor<A extends AbstractLattice<A>, D extends AbstractDomain<A>> {

	private final AnalyzedCFG<A> result;

	private final Analysis<A, D> analysis;

	private final ReplayDomain<A, D> domain;

	private final ReplayInterprocedural<A, D> replay;

	/**
	 * Builds the extractor.
	 *
	 * @param tool     the tool containing the results of the analysis
	 * @param result   the result whose nodes are replayed
	 * @param factory  the factory of the fresh values used in the transitions
	 * @param maxCases the maximum number of cases of each transition
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public TransitionExtractor(
			SemanticTool<A, D> tool,
			AnalyzedCFG<A> result,
			FreshValueFactory factory,
			int maxCases) {
		this.result = result;
		// we use an analysis without events, as the replay is not part of
		// the fixpoint computation
		this.analysis = new Analysis(tool.getConfiguration().analysis, tool.getConfiguration().shouldSmashError);
		this.domain = new ReplayDomain<>(analysis.domain, factory, maxCases);
		this.replay = new ReplayInterprocedural<>(tool, result, domain);
	}

	/**
	 * Yields the analysis (with no events) used to compute the states before
	 * the nodes.
	 *
	 * @return the analysis
	 */
	public Analysis<A, D> getAnalysis() {
		return analysis;
	}

	/**
	 * Yields the result whose nodes are replayed.
	 *
	 * @return the result
	 */
	public AnalyzedCFG<A> getResult() {
		return result;
	}

	/**
	 * Yields the state before the given node, as computed by the analysis.
	 *
	 * @param node the node
	 *
	 * @return the state before the node
	 *
	 * @throws SemanticException if something goes wrong during the computation
	 */
	public AnalysisState<A> preStateOf(
			Statement node)
			throws SemanticException {
		return Reachability.preStateOf(result, node, analysis);
	}

	/**
	 * Extracts the symbolic transitions starting at the given node.
	 *
	 * @param node the node, that must be a root statement of the cfg
	 *
	 * @return the transitions
	 *
	 * @throws SemanticException if something goes wrong while replaying the
	 *                               node
	 */
	public NodeTransitions extract(
			Statement node)
			throws SemanticException {
		AnalysisState<ReplayState<A>> pre = ReplayStates.lift(preStateOf(node), domain);
		StatementStore<ReplayState<A>> expressions = new StatementStore<>(pre).bottom();
		AnalysisState<ReplayState<A>> post = node.forwardSemantics(pre, replay, expressions);

		Map<Statement, SymbolicStore> partial = new HashMap<>();
		for (Statement sub : expressions.getKeys()) {
			AnalysisState<ReplayState<A>> state = expressions.getState(sub);
			if (!state.getExecution().isBottom())
				partial.put(sub, state.getExecutionState().getStore());
		}
		if (!post.getExecution().isBottom())
			partial.put(node, post.getExecutionState().getStore());

		List<EdgeTransition> outgoing = new ArrayList<>();
		for (Edge edge : result.getOutgoingEdges(node)) {
			AnalysisState<ReplayState<A>> traversed = ForwardCFGFixpoint.traverseEdge(
					edge,
					post,
					result,
					replay.getAnalysis(),
					replay.getCurrentContext(),
					null);
			if (!traversed.getExecution().isBottom())
				outgoing.add(new EdgeTransition(edge, traversed.getExecutionState().getStore()));
		}

		SymbolicStore exit = null;
		if (result.getAllExitpoints().contains(node) && !post.getExecution().isBottom())
			exit = post.getExecutionState().getStore();

		return new NodeTransitions(node, outgoing, exit, raisedErrors(node, post), partial);
	}

	/**
	 * Collects the errors in the given state that have been raised by the given
	 * node or by its sub-expressions.
	 */
	private Map<Error, SymbolicStore> raisedErrors(
			Statement node,
			AnalysisState<ReplayState<A>> post) {
		Map<Error, SymbolicStore> errors = new HashMap<>();
		GenericMapLattice<Error, ProgramState<ReplayState<A>>> map = post.getErrors();
		if (!map.isBottom() && !map.isTop())
			for (Error error : map.getKeys()) {
				ProgramState<ReplayState<A>> state = map.getState(error);
				if (!state.isBottom() && rootOf(error.getThrower()) == node)
					errors.put(error, state.getState().getStore());
			}

		GenericMapLattice<Type, GenericSetLattice<Statement>> smashed = post.getSmashedErrors();
		ProgramState<ReplayState<A>> smashedState = post.getSmashedErrorsState();
		if (!smashed.isBottom() && !smashed.isTop() && !smashedState.isBottom())
			for (Type type : smashed.getKeys())
				for (Statement thrower : smashed.getState(type))
					if (rootOf(thrower) == node)
						errors.put(new Error(type, thrower), smashedState.getState().getStore());
		return errors;
	}

	private static Statement rootOf(
			Statement st) {
		return st instanceof Expression ? ((Expression) st).getRootStatement() : st;
	}
}
