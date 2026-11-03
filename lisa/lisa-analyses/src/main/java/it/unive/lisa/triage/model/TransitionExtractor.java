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
import it.unive.lisa.program.cfg.edge.ErrorEdge;
import it.unive.lisa.program.cfg.fixpoints.forward.ForwardCFGFixpoint;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.triage.reachability.Reachability;
import it.unive.lisa.type.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

	private final Frame frame;

	private final AnalyzedCFG<A> result;

	private final Analysis<A, D> analysis;

	private final ReplayDomain<A, D> domain;

	private final ReplayInterprocedural<A, D> replay;

	/**
	 * Builds the extractor.
	 *
	 * @param tool     the tool containing the results of the analysis
	 * @param frame    the frame whose nodes are replayed
	 * @param factory  the factory of the fresh values used in the transitions
	 * @param maxCases the maximum number of cases of each transition
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public TransitionExtractor(
			SemanticTool<A, D> tool,
			Frame frame,
			FreshValueFactory factory,
			int maxCases) {
		this.frame = frame;
		this.result = (AnalyzedCFG<A>) frame.getResult();
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
	 * Yields the frame whose nodes are replayed.
	 *
	 * @return the frame
	 */
	public Frame getFrame() {
		return frame;
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
		// errors and halts reaching the node come from previous steps: only
		// the executions entering the node are replayed
		AnalysisState<ReplayState<A>> pre = ReplayStates.liftExecution(preStateOf(node), domain);
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

		ModelLocation location = new ModelLocation(frame, node);
		List<EdgeTransition> outgoing = new ArrayList<>();
		for (Edge edge : result.getOutgoingEdges(node))
			traverse(location, edge, post, outgoing);
		// errors raised by the node might be caught by the handlers of the
		// nodes that follow it (frontends might attach handlers only to some
		// nodes of a protected block, e.g., its last one, letting errors flow
		// there): the node moves directly to those handlers
		if (raisesErrors(post))
			for (Edge handler : downstreamHandlers(node))
				traverse(location, handler.newInstance(node, handler.getDestination()), post, outgoing);

		SymbolicStore exit = null;
		if (result.getAllExitpoints().contains(node) && !post.getExecution().isBottom())
			exit = post.getExecutionState().getStore();

		return new NodeTransitions(location, outgoing, exit, raisedErrors(node, post), partial);
	}

	private void traverse(
			ModelLocation source,
			Edge edge,
			AnalysisState<ReplayState<A>> post,
			List<EdgeTransition> outgoing)
			throws SemanticException {
		AnalysisState<ReplayState<A>> traversed = ForwardCFGFixpoint.traverseEdge(
				edge,
				post,
				result,
				replay.getAnalysis(),
				replay.getCurrentContext(),
				null);
		if (!traversed.getExecution().isBottom())
			outgoing.add(new EdgeTransition(
					source,
					new ModelLocation(frame, edge.getDestination()),
					edge,
					traversed.getExecutionState().getStore()));
	}

	private static boolean raisesErrors(
			AnalysisState<?> post) {
		return (!post.getErrors().isBottom() && !post.getErrors().getKeys().isEmpty())
				|| (!post.getSmashedErrors().isBottom() && !post.getSmashedErrors().getKeys().isEmpty());
	}

	/**
	 * Yields the error-handling edges of the nodes that can be reached from
	 * the given one through edges that are not error-handling ones (excluding
	 * the ones of the node itself), keeping one edge for each handler and
	 * protected block.
	 */
	private Collection<Edge> downstreamHandlers(
			Statement node) {
		Map<List<Object>, Edge> handlers = new LinkedHashMap<>();
		Set<Statement> seen = new HashSet<>();
		LinkedList<Statement> ws = new LinkedList<>();
		ws.add(node);
		while (!ws.isEmpty()) {
			Statement current = ws.removeFirst();
			if (!seen.add(current))
				continue;
			for (Edge edge : result.getOutgoingEdges(current))
				if (!edge.isErrorHandling())
					ws.add(edge.getDestination());
				else if (current != node && edge instanceof ErrorEdge)
					handlers.putIfAbsent(
							List.of(edge.getDestination(), ((ErrorEdge) edge).getProtectedBlock()),
							edge);
		}
		return handlers.values();
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
