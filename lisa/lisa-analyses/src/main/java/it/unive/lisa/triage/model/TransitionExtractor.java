package it.unive.lisa.triage.model;

import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.AnalysisState.Error;
import it.unive.lisa.analysis.AnalyzedCFG;
import it.unive.lisa.analysis.ProgramState;
import it.unive.lisa.analysis.ScopeToken;
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
import it.unive.lisa.program.cfg.statement.call.CFGCall;
import it.unive.lisa.program.cfg.statement.call.Call;
import it.unive.lisa.triage.reachability.Reachability;
import it.unive.lisa.type.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;

/**
 * Extracts the symbolic transitions performed by the nodes of an analysis
 * result (an {@link AnalyzedCFG}, i.e., a cfg in a given context), by replaying
 * their semantics through a {@link ReplayDomain} (see {@link NodeTransitions}).
 * Each node is replayed starting from its state before the node, as computed by
 * the analysis (see
 * {@link Reachability#preStateOf(AnalyzedCFG, Statement, Analysis)}), paired
 * with the identity transition.<br>
 * <br>
 * When callees are expanded (see {@link ReplayInterprocedural}), the
 * transitions of a node also include the ones entering the frames of its
 * expanded callees, and the ones of the exit points of the cfg of a callee
 * frame include the transition returning to the location where the caller
 * resumes, obtained by bringing the post-state of the exit point back to the
 * caller as the analysis does (assigning the returned value to the meta
 * variable of the call, and popping the scope of the call).
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

	private final TransitionExtractor<A, D> parent;

	private final Statement resumeNode;

	private final ResultCache<A> cache;

	/**
	 * Builds the extractor, summarizing all calls.
	 *
	 * @param tool     the tool containing the results of the analysis
	 * @param frame    the frame whose nodes are replayed
	 * @param factory  the factory of the fresh values used in the transitions
	 * @param maxCases the maximum number of cases of each transition
	 */
	public TransitionExtractor(
			SemanticTool<A, D> tool,
			Frame frame,
			FreshValueFactory factory,
			int maxCases) {
		this(tool, frame, factory, maxCases, Collections.emptySet(), () -> false, null);
	}

	/**
	 * Builds the extractor.
	 *
	 * @param tool       the tool containing the results of the analysis
	 * @param frame      the frame whose nodes are replayed
	 * @param factory    the factory of the fresh values used in the transitions
	 * @param maxCases   the maximum number of cases of each transition
	 * @param expanded   the frames of the callees to expand
	 * @param budget     tells whether or not new callees can be entered (see
	 *                       {@link ReplayInterprocedural})
	 * @param resumeNode the node of the parent frame containing the call that
	 *                       reaches the frame, where the caller resumes after
	 *                       the call returns ({@code null} for the root frame)
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public TransitionExtractor(
			SemanticTool<A, D> tool,
			Frame frame,
			FreshValueFactory factory,
			int maxCases,
			Set<Frame> expanded,
			BooleanSupplier budget,
			Statement resumeNode) {
		this(tool, frame, factory, maxCases, expanded, budget, resumeNode, null);
	}

	/**
	 * Builds the extractor.
	 *
	 * @param tool       the tool containing the results of the analysis
	 * @param frame      the frame whose nodes are replayed
	 * @param factory    the factory of the fresh values used in the transitions
	 * @param maxCases   the maximum number of cases of each transition
	 * @param expanded   the frames of the callees to expand
	 * @param budget     tells whether more calls can be expanded
	 * @param resumeNode the node of the caller containing the call that reaches
	 *                       the frame, where the caller resumes after the call
	 *                       returns ({@code null} for the root frame)
	 * @param parent     the extractor of the frame of the caller, that handles
	 *                       the errors leaving the frame ({@code null} for the
	 *                       root frame)
	 */
	public TransitionExtractor(
			SemanticTool<A, D> tool,
			Frame frame,
			FreshValueFactory factory,
			int maxCases,
			Set<Frame> expanded,
			BooleanSupplier budget,
			Statement resumeNode,
			TransitionExtractor<A, D> parent) {
		this(tool, frame, factory, maxCases, expanded, budget, resumeNode, parent, new ResultCache<>());
	}

	/**
	 * Builds the extractor.
	 *
	 * @param tool       the tool containing the results of the analysis
	 * @param frame      the frame whose nodes are replayed
	 * @param factory    the factory of the fresh values used in the transitions
	 * @param maxCases   the maximum number of cases of each transition
	 * @param expanded   the frames of the callees to expand
	 * @param budget     tells whether more calls can be expanded
	 * @param resumeNode the node of the caller containing the call that reaches
	 *                       the frame, where the caller resumes after the call
	 *                       returns ({@code null} for the root frame)
	 * @param parent     the extractor of the frame of the caller, that handles
	 *                       the errors leaving the frame ({@code null} for the
	 *                       root frame)
	 * @param cache      the cache of the information computed from the results
	 *                       of the analysis
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public TransitionExtractor(
			SemanticTool<A, D> tool,
			Frame frame,
			FreshValueFactory factory,
			int maxCases,
			Set<Frame> expanded,
			BooleanSupplier budget,
			Statement resumeNode,
			TransitionExtractor<A, D> parent,
			ResultCache<A> cache) {
		this.parent = parent;
		this.cache = cache;
		this.frame = frame;
		this.result = (AnalyzedCFG<A>) frame.getResult();
		// we use an analysis without events, as the replay is not part of
		// the fixpoint computation
		this.analysis = new Analysis(tool.getConfiguration().analysis, tool.getConfiguration().shouldSmashError);
		this.domain = new ReplayDomain<>(analysis.domain, factory, maxCases);
		this.replay = new ReplayInterprocedural<>(tool, frame, domain, expanded, budget, cache);
		this.resumeNode = resumeNode;
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
		return cache.preStateOf(result, node, analysis);
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
		return extract(new ModelLocation(frame, node), true);
	}

	/**
	 * Extracts the symbolic transitions starting at the given location of the
	 * frame of this extractor: the location before a node, or the one where the
	 * node resumes after an expanded call returned.
	 *
	 * @param location  the location
	 * @param expansion whether or not the calls of the node can be expanded
	 *
	 * @return the transitions
	 *
	 * @throws SemanticException if something goes wrong while replaying the
	 *                               node
	 */
	public NodeTransitions extract(
			ModelLocation location,
			boolean expansion)
			throws SemanticException {
		Statement node = location.getNode();
		replay.startRun(node, location.getResumed(), expansion);
		// errors and halts reaching the node come from previous steps: only
		// the executions entering the node are replayed
		AnalysisState<ReplayState<A>> pre = ReplayStates.liftExecution(preStateOf(node), domain);
		StatementStore<ReplayState<A>> expressions = new StatementStore<>(pre).bottom();
		AnalysisState<ReplayState<A>> post = node.forwardSemantics(pre, replay, expressions);
		// when the run stops at an expanded call, the semantics of the
		// expressions evaluated afterwards might turn the bottom execution
		// into one whose computed expressions are not bottom: such executions
		// are discarded
		Map<Frame, SymbolicStore> entered = replay.getEntered();
		if (replay.hasStopped())
			post = stopped(post);
		else if (entered.isEmpty() && post.getExecution().isBottom())
			post = lost(node, pre, post);

		Map<Statement, SymbolicStore> partial = new LinkedHashMap<>();
		for (Statement sub : expressions.getKeys()) {
			AnalysisState<ReplayState<A>> state = expressions.getState(sub);
			if (replay.hasStopped())
				state = stopped(state);
			if (!state.getExecution().isBottom())
				partial.put(sub, state.getExecutionState().getStore());
		}
		if (!post.getExecution().isBottom())
			partial.put(node, post.getExecutionState().getStore());

		List<EdgeTransition> outgoing = new ArrayList<>();
		// a node with several unconditional successors is an
		// over-approximation of the control flow of the program (e.g., a
		// finally block shared by the paths reaching it, that continues to
		// all their destinations): which successor is taken depends on an
		// unknown condition
		long unconditional = result.getOutgoingEdges(node).stream()
				.filter(e -> e.isUnconditional() && !(e instanceof ErrorEdge))
				.count();
		for (Edge edge : result.getOutgoingEdges(node)) {
			int from = outgoing.size();
			traverse(location, edge, post, outgoing);
			if (unconditional > 1 && edge.isUnconditional() && !(edge instanceof ErrorEdge))
				for (int i = from; i < outgoing.size(); i++) {
					EdgeTransition e = outgoing.get(i);
					SummaryMarker choice = new SummaryMarker(node.getProgram().getTypes().getBooleanType(),
							"choice among the successors of " + node, node.getLocation(),
							SummaryValue.Kind.CONTROL);
					outgoing.set(i, new EdgeTransition(e.getSource(), e.getDestination(), e.getEdge(),
							e.getStore().assume(choice, node)));
				}
		}
		// errors raised by the node might be caught by the handlers of the
		// nodes that follow it (frontends might attach handlers only to some
		// nodes of a protected block, e.g., its last one, letting errors flow
		// there): the node moves directly to those handlers
		if (raisesErrors(post))
			for (Edge handler : downstreamHandlers(node))
				traverse(location, handler.newInstance(node, handler.getDestination()), post, outgoing);

		// the callees entered by the node
		for (Map.Entry<Frame, SymbolicStore> callee : entered.entrySet())
			for (Statement entry : callee.getKey().getResult().getEntrypoints())
				outgoing.add(new EdgeTransition(
						location,
						new ModelLocation(callee.getKey(), entry),
						null,
						callee.getValue()));

		SymbolicStore exit = null;
		if (result.getAllExitpoints().contains(node) && !post.getExecution().isBottom()) {
			exit = post.getExecutionState().getStore();
			if (!frame.isRoot() && resumeNode != null) {
				// the return to the caller, that resumes after the call
				CFGCall call = frame.getCall();
				AnalysisState<ReplayState<A>> back = call.getProgram().getFeatures().getScopingStrategy().unscope(
						call,
						new ScopeToken(call),
						post,
						replay.getAnalysis(),
						frame.getParent().getResult().getId());
				if (!back.getExecution().isBottom())
					outgoing.add(new EdgeTransition(
							location,
							new ModelLocation(frame.getParent(), resumeNode, call),
							null,
							back.getExecutionState().getStore()));
			}
		}

		// errors raised in a callee frame that are not caught by the callee
		// leave it, and are handled by its callers
		List<NodeTransitions.EscapingError> escaping = new ArrayList<>();
		if (parent != null)
			for (Map.Entry<Error, ProgramState<ReplayState<A>>> e : raised(node, post).entrySet())
				if (!caught(node, e.getKey()))
					leave(location, e.getKey(), e.getValue(), outgoing, escaping, e.getKey().getThrower());

		return new NodeTransitions(location, outgoing, exit, raisedErrors(node, post), partial, replay.getCalls(),
				escaping);
	}

	/**
	 * Moves an error raised in the frame of this extractor, and not caught
	 * there, to the caller: the scope of the callee is popped, and the error is
	 * raised by the call, as the analysis does when an error leaves a callee.
	 */
	private void leave(
			ModelLocation source,
			Error error,
			ProgramState<ReplayState<A>> state,
			List<EdgeTransition> outgoing,
			List<NodeTransitions.EscapingError> escaping,
			Statement origin)
			throws SemanticException {
		CFGCall call = frame.getCall();
		ProgramState<ReplayState<A>> popped = state.popScope(new ScopeToken(call), call);
		Call thrower = call;
		while (thrower.getSource() != null)
			thrower = thrower.getSource();
		parent.raise(source, new Error(error.getType(), thrower), popped, outgoing, escaping, origin);
	}

	/**
	 * Handles an error raised by a node of the frame of this extractor through
	 * one of its calls: the error goes to the handlers of the node that catch
	 * it, or leaves the frame if none catches it (to the caller, or out of the
	 * model if this is the root frame).
	 */
	private void raise(
			ModelLocation source,
			Error error,
			ProgramState<ReplayState<A>> state,
			List<EdgeTransition> outgoing,
			List<NodeTransitions.EscapingError> escaping,
			Statement origin)
			throws SemanticException {
		Statement node = rootOf(error.getThrower());
		AnalysisState<ReplayState<A>> raised = new AnalysisState<>(state).bottom().addError(error, state);
		for (Edge handler : handlersOf(node))
			traverse(source, handler, raised, outgoing);
		if (caught(node, error))
			return;
		if (parent == null)
			escaping.add(new NodeTransitions.EscapingError(node, error, state.getState().getStore(), origin));
		else
			leave(source, error, state, outgoing, escaping, origin);
	}

	/**
	 * Yields the error-handling edges that might catch the errors raised by the
	 * given node: its own ones, and the ones of the nodes that follow it (see
	 * {@link #downstreamHandlers(Statement)}).
	 */
	private List<Edge> handlersOf(
			Statement node) {
		List<Edge> handlers = new ArrayList<>();
		for (Edge edge : result.getOutgoingEdges(node))
			if (edge.isErrorHandling())
				handlers.add(edge);
		for (Edge handler : downstreamHandlers(node))
			handlers.add(handler.newInstance(node, handler.getDestination()));
		return handlers;
	}

	/**
	 * Yields {@code true} if the given error, raised by the given node, is
	 * surely caught by one of the handlers of the node (see
	 * {@link #handlersOf(Statement)}): its type can be assigned to one of the
	 * types caught by the handler (and to none of the excluded ones), and its
	 * thrower is protected by the handler.
	 */
	private boolean caught(
			Statement node,
			Error error) {
		Type type = error.getType();
		for (Edge edge : handlersOf(node)) {
			if (!(edge instanceof ErrorEdge))
				continue;
			ErrorEdge handler = (ErrorEdge) edge;
			if (!handler.getProtectedBlock().getBody().contains(node))
				continue;
			boolean excluded = false;
			for (Type ex : handler.getExcludedTypes())
				excluded |= type.canBeAssignedTo(ex);
			if (excluded)
				continue;
			for (Type t : handler.getTypes())
				if (type.canBeAssignedTo(t))
					return true;
		}
		return false;
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

	/**
	 * Yields the post-state of a node whose replay yielded no execution. If the
	 * analysis computed executions after the node, the replay lost them (e.g.,
	 * the semantics of a native construct behaves differently when replayed):
	 * since excluding them would be unsound, the node continues from the
	 * post-state of the analysis, losing all values.
	 */
	private AnalysisState<ReplayState<A>> lost(
			Statement node,
			AnalysisState<ReplayState<A>> pre,
			AnalysisState<ReplayState<A>> post) {
		AnalysisState<A> after = result.getAnalysisStateAfter(node);
		if (after.getExecution().isBottom() || after.getExecutionState().isBottom()
				|| pre.getExecution().isBottom())
			return post;
		ProgramState<A> execution = after.getExecution();
		ReplayState<A> state = domain.lift(execution.getState())
				.withStore(pre.getExecutionState().getStore().havocAll());
		return post.withExecution(new ProgramState<>(state, execution.getComputedExpressions(),
				execution.getFixpointInformation()));
	}

	private static <A extends AbstractLattice<A>> AnalysisState<ReplayState<A>> stopped(
			AnalysisState<ReplayState<A>> state) {
		if (!state.getExecution().isBottom() && state.getExecutionState().isBottom())
			return state.withExecution(state.getExecution().bottom());
		return state;
	}

	private static boolean raisesErrors(
			AnalysisState<?> post) {
		return (!post.getErrors().isBottom() && !post.getErrors().getKeys().isEmpty())
				|| (!post.getSmashedErrors().isBottom() && !post.getSmashedErrors().getKeys().isEmpty());
	}

	/**
	 * Yields the error-handling edges of the nodes that can be reached from the
	 * given one through edges that are not error-handling ones (excluding the
	 * ones of the node itself), keeping one edge for each handler and protected
	 * block.
	 */
	private Collection<Edge> downstreamHandlers(
			Statement node) {
		Map<List<Object>, Edge> handlers = new LinkedHashMap<>();
		Set<Statement> seen = new LinkedHashSet<>();
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
		Map<Error, SymbolicStore> errors = new LinkedHashMap<>();
		for (Map.Entry<Error, ProgramState<ReplayState<A>>> e : raised(node, post).entrySet())
			errors.put(e.getKey(), e.getValue().getState().getStore());
		return errors;
	}

	/**
	 * Collects the errors in the given state that have been raised by the given
	 * node or by its sub-expressions, with their states.
	 */
	private Map<Error, ProgramState<ReplayState<A>>> raised(
			Statement node,
			AnalysisState<ReplayState<A>> post) {
		Map<Error, ProgramState<ReplayState<A>>> errors = new LinkedHashMap<>();
		GenericMapLattice<Error, ProgramState<ReplayState<A>>> map = post.getErrors();
		if (!map.isBottom() && !map.isTop())
			for (Error error : map.getKeys()) {
				ProgramState<ReplayState<A>> state = map.getState(error);
				if (!state.isBottom() && rootOf(error.getThrower()) == node)
					errors.put(error, state);
			}

		GenericMapLattice<Type, GenericSetLattice<Statement>> smashed = post.getSmashedErrors();
		ProgramState<ReplayState<A>> smashedState = post.getSmashedErrorsState();
		if (!smashed.isBottom() && !smashed.isTop() && !smashedState.isBottom())
			for (Type type : smashed.getKeys())
				for (Statement thrower : smashed.getState(type))
					if (rootOf(thrower) == node)
						errors.put(new Error(type, thrower), smashedState);
		return errors;
	}

	private static Statement rootOf(
			Statement st) {
		// expressions synthesized by the semantics of other statements might
		// have no root in the cfg
		Statement root = st instanceof Expression ? ((Expression) st).getRootStatement() : st;
		if (st.getCFG() == null || st.getCFG().containsNode(root))
			return root;
		Statement node = Reachability.nodeOf(st);
		return node == null ? root : node;
	}
}
