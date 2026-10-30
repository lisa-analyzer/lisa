package it.unive.lisa.analysis.reachability;

import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.AnalysisState.Error;
import it.unive.lisa.analysis.AnalyzedCFG;
import it.unive.lisa.analysis.ProgramState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.interprocedural.ScopeId;
import it.unive.lisa.interprocedural.callgraph.CallGraph;
import it.unive.lisa.interprocedural.callgraph.CallResolutionException;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.lattices.GenericSetLattice;
import it.unive.lisa.lattices.Satisfiability;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.controlFlow.ControlFlowStructure;
import it.unive.lisa.program.cfg.controlFlow.Loop;
import it.unive.lisa.program.cfg.edge.Edge;
import it.unive.lisa.program.cfg.edge.ErrorEdge;
import it.unive.lisa.program.cfg.edge.FalseEdge;
import it.unive.lisa.program.cfg.edge.TrueEdge;
import it.unive.lisa.program.cfg.fixpoints.CompoundState;
import it.unive.lisa.program.cfg.fixpoints.forward.ForwardCFGFixpoint;
import it.unive.lisa.program.cfg.protection.ProtectedBlock;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.call.CFGCall;
import it.unive.lisa.program.cfg.statement.call.Call;
import it.unive.lisa.program.cfg.statement.call.MultiCall;
import it.unive.lisa.program.cfg.statement.call.TruncatedParamsCall;
import it.unive.lisa.program.cfg.statement.call.UnresolvedCall;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.type.Type;
import it.unive.lisa.util.collections.workset.LIFOWorkingSet;
import it.unive.lisa.util.collections.workset.VisitOnceFIFOWorkingSet;
import it.unive.lisa.util.collections.workset.VisitOnceWorkingSet;
import it.unive.lisa.util.collections.workset.WorkingSet;
import it.unive.lisa.util.datastructures.graph.GraphVisitor;
import it.unive.lisa.util.datastructures.graph.algorithms.Dominators;
import it.unive.lisa.util.datastructures.graph.algorithms.SCCs;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.apache.commons.lang3.tuple.Pair;

/**
 * An algorithm computing the reachability of program points from the results of
 * a fixpoint computation. A program point is
 * {@link ReachabilityStatus#REACHABLE} if every execution of the program
 * reaches it, {@link ReachabilityStatus#UNREACHABLE} if no execution reaches
 * it, and {@link ReachabilityStatus#POSSIBLY_REACHABLE} otherwise. Results are
 * computed lazily, when queried, and are cached.<br>
 * <br>
 * The reachability of a statement within a single analysis result (i.e., a cfg
 * analyzed in a given context) is computed on a graph whose nodes are the
 * statements of the cfg, plus a synthetic entry node and a synthetic exit node.
 * The graph only contains the edges that the executions might follow, that is:
 * <ul>
 * <li>the edges of the cfg leaving statements whose normal execution is not
 * bottom, excluding conditional edges whose condition is never satisfied
 * according to {@link Analysis#satisfies};</li>
 * <li>an edge from each statement raising an error (i.e., whose post-state
 * contains an error, possibly smashed, thrown by the statement itself or by a
 * call it contains) to the first statement of each catch block that can catch
 * it, or to the exit node if no catch block catches it;</li>
 * <li>an edge from each statement that might halt the execution to the exit
 * node;</li>
 * <li>an edge to the exit node from each statement that has no other followers
 * (e.g., returns), or that is part of a cycle that cannot reach the exit node
 * (e.g., statements in infinite loops).</li>
 * </ul>
 * Each execution follows a path from the entry node to the exit node: a
 * statement is reached by every execution reaching the entry of the cfg if it
 * dominates the exit node, by no execution if it is not reachable from the
 * entry node, and by some executions otherwise. Note that this assumes that
 * loops eventually terminate, as is common for reachability: the code following
 * a loop is considered reached by all executions reaching the loop. Since the
 * fixpoint results only describe the lub of all iterations of a loop, the first
 * evaluation of the guard of each loop is represented by a separate node, whose
 * conditional edges are pruned using the state that reaches the guard from
 * outside of the loop: this allows to determine that the first iteration of a
 * loop is always executed.<br>
 * <br>
 * The reachability of the entry of a result is
 * {@link ReachabilityStatus#REACHABLE} for results of entrypoints analyzed in
 * their starting scope (see {@link ScopeId#isStartingId()}). For other results,
 * the call sites that certainly lead to that result are identified as the ones
 * that, once pushed to the scope of the caller's result (see
 * {@link ScopeId#push(CFGCall, CompoundState)}), yield the scope of the result:
 * the entry of the result is reached by all executions if at least one of them
 * is, and it is considered {@link ReachabilityStatus#POSSIBLY_REACHABLE} if no
 * such call site exists. The reachability of a statement in a result is then
 * given by the composition of the reachability of the entry and the one
 * computed within the result (see {@link ReachabilityStatus#compose}).<br>
 * <br>
 * Note that statements are the finest granularity of this algorithm: the
 * reachability of an expression is the one of its root statement.
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 *
 * @param <A> the kind of {@link AbstractLattice} produced by the domain
 *                {@code D}
 * @param <D> the kind of {@link AbstractDomain} ran during the analysis
 */
public class Reachability<A extends AbstractLattice<A>, D extends AbstractDomain<A>> {

	private final Map<CFG, Collection<AnalyzedCFG<A>>> results;

	private final CallGraph callgraph;

	private final Analysis<A, D> analysis;

	private final Map<AnalyzedCFG<A>, CFG> owners;

	private final Map<AnalyzedCFG<A>, Map<Statement, ReachabilityStatus>> locals;

	private final Map<AnalyzedCFG<A>, ReachabilityStatus> entries;

	private final Map<CFG, Boolean> halting;

	/**
	 * Builds the algorithm. The given analysis is used to query the
	 * satisfiability of conditions and to resolve calls: it should not post
	 * events, as the queries are not part of the fixpoint computation.
	 *
	 * @param results   the results of the fixpoint computation, for each cfg
	 * @param callgraph the callgraph that has been built during the analysis
	 * @param analysis  the analysis that has been executed to produce the
	 *                      results
	 */
	public Reachability(
			Map<CFG, Collection<AnalyzedCFG<A>>> results,
			CallGraph callgraph,
			Analysis<A, D> analysis) {
		this.results = results;
		this.callgraph = callgraph;
		this.analysis = analysis;
		this.owners = new IdentityHashMap<>();
		for (Entry<CFG, Collection<AnalyzedCFG<A>>> entry : results.entrySet())
			if (entry.getValue() != null)
				for (AnalyzedCFG<A> result : entry.getValue())
					owners.put(result, entry.getKey());
		this.locals = new IdentityHashMap<>();
		this.entries = new IdentityHashMap<>();
		this.halting = new IdentityHashMap<>();
	}

	/**
	 * Yields the reachability of the given statement within the given result,
	 * that is, considering only the executions analyzed in the context of the
	 * result. If {@code st} is an expression, the reachability of its root
	 * statement is returned.
	 *
	 * @param result the result containing the statement
	 * @param st     the statement
	 *
	 * @return the reachability of the statement
	 *
	 * @throws SemanticException if something goes wrong during the computation
	 */
	public synchronized ReachabilityStatus getReachability(
			AnalyzedCFG<A> result,
			Statement st)
			throws SemanticException {
		Statement node = nodeOf(st);
		ReachabilityStatus local = node == null ? null : local(result).get(node);
		if (local == null)
			return ReachabilityStatus.UNREACHABLE;
		return local.compose(getEntryReachability(result));
	}

	/**
	 * Yields the reachability of the given statement, considering all the
	 * results of its cfg. A statement is reached by every execution if this
	 * happens in at least one of the results (see
	 * {@link #getReachability(AnalyzedCFG, Statement)}). If {@code st} is an
	 * expression, the reachability of its root statement is returned.
	 *
	 * @param st the statement
	 *
	 * @return the reachability of the statement
	 *
	 * @throws SemanticException if something goes wrong during the computation
	 */
	public synchronized ReachabilityStatus getReachability(
			Statement st)
			throws SemanticException {
		Collection<AnalyzedCFG<A>> res = results.get(st.getCFG());
		ReachabilityStatus status = ReachabilityStatus.UNREACHABLE;
		if (res != null)
			for (AnalyzedCFG<A> result : res)
				status = status.best(getReachability(result, st));
		return status;
	}

	/**
	 * Yields the reachability of the entry of the given result, that is,
	 * whether every execution of the program executes the cfg of the result in
	 * the context of the result.
	 *
	 * @param result the result
	 *
	 * @return the reachability of the entry of the result
	 *
	 * @throws SemanticException if something goes wrong during the computation
	 */
	public synchronized ReachabilityStatus getEntryReachability(
			AnalyzedCFG<A> result)
			throws SemanticException {
		ReachabilityStatus status = entries.get(result);
		if (status != null)
			return status;

		// we collect the results whose entry reachability influences the one
		// of the given result, transitively through call sites
		Map<AnalyzedCFG<A>, List<Pair<AnalyzedCFG<A>, Statement>>> links = new IdentityHashMap<>();
		Map<AnalyzedCFG<A>, List<AnalyzedCFG<A>>> callees = new IdentityHashMap<>();
		WorkingSet<AnalyzedCFG<A>> ws = new LIFOWorkingSet<>();
		ws.push(result);
		while (!ws.isEmpty()) {
			AnalyzedCFG<A> current = ws.pop();
			if (links.containsKey(current) || entries.containsKey(current))
				continue;
			List<Pair<AnalyzedCFG<A>, Statement>> callers = callersOf(current);
			links.put(current, callers);
			for (Pair<AnalyzedCFG<A>, Statement> caller : callers) {
				callees.computeIfAbsent(caller.getLeft(), r -> new ArrayList<>()).add(current);
				ws.push(caller.getLeft());
			}
		}

		// least fixpoint, where entries are reached by every execution if at
		// least one of their call sites is
		Map<AnalyzedCFG<A>, ReachabilityStatus> values = new IdentityHashMap<>();
		for (Entry<AnalyzedCFG<A>, List<Pair<AnalyzedCFG<A>, Statement>>> entry : links.entrySet())
			if (isStartingEntrypoint(entry.getKey()))
				values.put(entry.getKey(), ReachabilityStatus.REACHABLE);
			else if (entry.getValue().isEmpty())
				// we cannot identify the call sites leading to this result
				values.put(entry.getKey(), ReachabilityStatus.POSSIBLY_REACHABLE);
			else {
				values.put(entry.getKey(), ReachabilityStatus.UNREACHABLE);
				ws.push(entry.getKey());
			}

		while (!ws.isEmpty()) {
			AnalyzedCFG<A> current = ws.pop();
			ReachabilityStatus best = ReachabilityStatus.UNREACHABLE;
			for (Pair<AnalyzedCFG<A>, Statement> caller : links.get(current)) {
				ReachabilityStatus callerEntry = entries.get(caller.getLeft());
				if (callerEntry == null)
					callerEntry = values.get(caller.getLeft());
				ReachabilityStatus site = local(caller.getLeft()).get(caller.getRight());
				if (site != null)
					best = best.best(site.compose(callerEntry));
			}
			if (best != values.get(current)) {
				values.put(current, best);
				for (AnalyzedCFG<A> callee : callees.getOrDefault(current, Collections.emptyList()))
					if (!isStartingEntrypoint(callee) && !links.get(callee).isEmpty())
						ws.push(callee);
			}
		}

		entries.putAll(values);
		return entries.get(result);
	}

	private boolean isStartingEntrypoint(
			AnalyzedCFG<A> result) {
		CFG cfg = owners.get(result);
		return cfg != null
				&& (result.getId() == null || result.getId().isStartingId())
				&& cfg.getProgram().getEntryPoints().contains(cfg);
	}

	private List<Pair<AnalyzedCFG<A>, Statement>> callersOf(
			AnalyzedCFG<A> result)
			throws SemanticException {
		List<Pair<AnalyzedCFG<A>, Statement>> callers = new ArrayList<>();
		CFG cfg = owners.get(result);
		if (cfg == null || callgraph == null || result.getId() == null)
			return callers;

		for (Call site : callgraph.getCallSites(cfg)) {
			Collection<AnalyzedCFG<A>> callerResults = results.get(site.getCFG());
			Statement node = nodeOf(site);
			if (callerResults == null || node == null)
				continue;
			for (AnalyzedCFG<A> caller : callerResults) {
				if (caller.getId() == null)
					continue;
				Call resolved = site instanceof UnresolvedCall ? resolve((UnresolvedCall) site, caller) : site;
				if (resolved == null)
					continue;
				Set<CFGCall> calls = new HashSet<>();
				cfgCallsOf(resolved, calls);
				CompoundState<A> state = null;
				for (CFGCall call : calls) {
					if (!call.getTargetedCFGs().contains(cfg))
						continue;
					if (state == null)
						state = callState(caller, site, node);
					ScopeId<A> pushed = caller.getId().push(call, state);
					if (result.getId().equals(pushed)) {
						callers.add(Pair.of(caller, node));
						break;
					}
				}
			}
		}

		return callers;
	}

	private CompoundState<A> callState(
			AnalyzedCFG<A> caller,
			Call site,
			Statement node)
			throws SemanticException {
		StatementStore<A> store = new StatementStore<>(caller.getEntryState()).bottom();
		if (rootOf(site) != node)
			// the call has been synthesized during the analysis, and the
			// states of its expressions are not part of the results: we use
			// the state before the node containing it
			return CompoundState.of(preStateOf(caller, node).withExecutionExpressions(new ExpressionSet()), store);

		// the state used by the fixpoint to compute the scope of the targets
		// is the one after the evaluation of the parameters, without
		// execution expressions (see CallWithResult#forwardSemanticsAux)
		Expression[] params = site.getParameters();
		for (Expression param : params)
			store.put(param, caller.getAnalysisStateAfter(param));
		AnalysisState<A> state;
		if (params.length == 0) {
			Statement pred = site.getEvaluationPredecessor();
			state = pred == null ? preStateOf(caller, node) : caller.getAnalysisStateAfter(pred);
		} else
			state = caller.getAnalysisStateAfter(params[site.getOrder().last(params.length)]);
		return CompoundState.of(state.withExecutionExpressions(new ExpressionSet()), store);
	}

	private static Statement nodeOf(
			Statement st) {
		Statement root = rootOf(st);
		CFG cfg = st.getCFG();
		if (cfg.containsNode(root))
			return root;

		// expressions that are synthesized during the analysis (e.g., calls
		// to constructors) are not part of the cfg: we look for the node
		// containing an expression at the same location
		for (Statement node : cfg.getNodes()) {
			List<Statement> matching = new ArrayList<>(1);
			node.accept(new GraphVisitor<CFG, Statement, Edge, Collection<Statement>>() {

				@Override
				public boolean visit(
						Collection<Statement> tool,
						CFG graph,
						Statement expr) {
					if (expr.getLocation().equals(st.getLocation()))
						tool.add(expr);
					return tool.isEmpty();
				}
			}, matching);
			if (!matching.isEmpty())
				return node;
		}
		return null;
	}

	private static void cfgCallsOf(
			Call call,
			Collection<CFGCall> calls) {
		if (call instanceof CFGCall)
			calls.add((CFGCall) call);
		else if (call instanceof MultiCall)
			for (Call inner : ((MultiCall) call).getCalls())
				cfgCallsOf(inner, calls);
		else if (call instanceof TruncatedParamsCall)
			cfgCallsOf(((TruncatedParamsCall) call).getInnerCall(), calls);
	}

	private Call resolve(
			UnresolvedCall call,
			AnalyzedCFG<A> result)
			throws SemanticException {
		StatementStore<A> store = new StatementStore<>(result.getEntryState()).bottom();
		for (Expression e : call.getParameters())
			store.put(e, result.getAnalysisStateAfter(e));
		try {
			return callgraph.resolve(call, call.parameterTypes(store, analysis, result.getId()), null);
		} catch (CallResolutionException e) {
			return null;
		}
	}

	private static Statement rootOf(
			Statement st) {
		if (st instanceof Call) {
			Call original = (Call) st;
			while (original.getSource() != null)
				original = original.getSource();
			st = original;
		}
		if (st instanceof Expression)
			return ((Expression) st).getRootStatement();
		return st;
	}

	private Map<Statement, ReachabilityStatus> local(
			AnalyzedCFG<A> result)
			throws SemanticException {
		Map<Statement, ReachabilityStatus> local = locals.get(result);
		if (local == null) {
			local = computeLocal(result);
			locals.put(result, local);
		}
		return local;
	}

	private Map<Statement, ReachabilityStatus> computeLocal(
			AnalyzedCFG<A> result)
			throws SemanticException {
		ReachabilityGraph graph = new ReachabilityGraph();
		ReachabilityGraph.Node exit = graph.getExit();
		Map<Statement, ReachabilityGraph.Node> nodes = new IdentityHashMap<>();
		Map<Statement, AnalysisState<A>> post = new IdentityHashMap<>();
		for (Statement st : result.getNodes()) {
			nodes.put(st, graph.addNode(st, false));
			post.put(st, result.getAnalysisStateAfter(st));
		}

		// the first evaluation of each loop guard is a separate node
		Map<Statement, Loop> loops = new IdentityHashMap<>();
		Map<Statement, ReachabilityGraph.Node> firsts = new IdentityHashMap<>();
		for (ControlFlowStructure cfs : result.getDescriptor().getControlFlowStructures())
			if (cfs instanceof Loop && nodes.containsKey(cfs.getCondition())) {
				loops.put(cfs.getCondition(), (Loop) cfs);
				firsts.put(cfs.getCondition(), graph.addNode(cfs.getCondition(), true));
			}

		// error edges, with the types that each of them does not catch
		Map<ErrorEdge, Collection<Type>> handlers = new IdentityHashMap<>();
		for (Edge edge : result.getEdges())
			if (edge instanceof ErrorEdge)
				handlers.put((ErrorEdge) edge, ((ErrorEdge) edge).getExcludedTypes());

		for (Statement ep : result.getEntrypoints())
			if (!post.get(ep).isBottom())
				graph.connect(graph.getEntry(), target(ep, null, nodes, firsts, loops));

		for (Statement st : result.getNodes()) {
			AnalysisState<A> state = post.get(st);
			if (state.isBottom())
				continue;

			ReachabilityGraph.Node node = nodes.get(st);
			ReachabilityGraph.Node first = firsts.get(st);
			AnalysisState<A> firstState = first == null ? null : firstEvaluation(result, st, loops.get(st), post);
			Collection<ReachabilityGraph.Node> escapes = new LinkedHashSet<>();

			// normal executions
			if (!state.getExecution().isBottom() && !state.getExecutionState().isBottom()) {
				Collection<SymbolicExpression> conditions = state.getExecutionExpressions().elements();
				for (Edge edge : result.getOutgoingEdges(st)) {
					if (edge.isErrorHandling())
						continue;
					Statement dest = edge.getDestination();
					if (post.get(dest).isBottom() || !isFeasible(edge, state, conditions, result.getId()))
						continue;
					ReachabilityGraph.Node t = target(dest, st, nodes, firsts, loops);
					graph.connect(node, t);
					if (first != null && isFeasible(edge, firstState, conditions, result.getId()))
						graph.connect(first, t);
				}
			}

			// errors raised by st
			Set<Type> raised = new HashSet<>();
			for (Entry<Error, ProgramState<A>> error : state.getErrors())
				if (rootOf(error.getKey().getThrower()) == st)
					raised.add(error.getKey().getType());
			for (Entry<Type, GenericSetLattice<Statement>> error : state.getSmashedErrors())
				for (Statement thrower : error.getValue())
					if (rootOf(thrower) == st) {
						raised.add(error.getKey());
						break;
					}
			for (Type type : raised)
				escapes.addAll(handlersOf(st, type, handlers, post, nodes, firsts, loops, exit));

			// halting executions
			if (mayHalt(result, st, state))
				escapes.add(exit);

			for (ReachabilityGraph.Node escape : escapes) {
				graph.connect(node, escape);
				if (first != null)
					graph.connect(first, escape);
			}
		}

		// nodes without followers end the execution
		for (ReachabilityGraph.Node node : new ArrayList<>(graph.getNodes()))
			if (node != exit && graph.followersOf(node).isEmpty())
				graph.connect(node, exit);

		// executions that never leave a cycle (e.g., infinite loops) never
		// reach the exit: we connect the nodes of such cycles to it, so that
		// these executions are accounted for
		VisitOnceWorkingSet<ReachabilityGraph.Node> ws = new VisitOnceFIFOWorkingSet<>();
		ws.push(exit);
		while (!ws.isEmpty())
			graph.predecessorsOf(ws.pop()).forEach(ws::push);
		for (Collection<ReachabilityGraph.Node> scc : new SCCs<ReachabilityGraph,
				ReachabilityGraph.Node,
				ReachabilityGraph.Edge>().buildNonTrivial(graph))
			// either all nodes of the scc reach the exit, or none does
			if (!ws.getSeen().contains(scc.iterator().next()))
				for (ReachabilityGraph.Node node : scc)
					graph.connect(node, exit);

		// nodes that do not have dominators are not reachable from the entry
		Map<ReachabilityGraph.Node, Set<ReachabilityGraph.Node>> dominators = new Dominators<ReachabilityGraph,
				ReachabilityGraph.Node,
				ReachabilityGraph.Edge>().build(graph);
		Set<ReachabilityGraph.Node> mandatory = dominators.getOrDefault(exit, Collections.emptySet());

		Map<Statement, ReachabilityStatus> local = new IdentityHashMap<>();
		for (Statement st : result.getNodes()) {
			ReachabilityGraph.Node node = nodes.get(st);
			ReachabilityGraph.Node first = firsts.get(st);
			if (mandatory.contains(node) || (first != null && mandatory.contains(first)))
				local.put(st, ReachabilityStatus.REACHABLE);
			else if (dominators.containsKey(node) || (first != null && dominators.containsKey(first)))
				local.put(st, ReachabilityStatus.POSSIBLY_REACHABLE);
			else
				local.put(st, ReachabilityStatus.UNREACHABLE);
		}
		return local;
	}

	private static ReachabilityGraph.Node target(
			Statement dest,
			Statement source,
			Map<Statement, ReachabilityGraph.Node> nodes,
			Map<Statement, ReachabilityGraph.Node> firsts,
			Map<Statement, Loop> loops) {
		ReachabilityGraph.Node first = firsts.get(dest);
		if (first != null && (source == null || !loops.get(dest).contains(source)))
			// entering the loop from outside
			return first;
		return nodes.get(dest);
	}

	private AnalysisState<A> firstEvaluation(
			AnalyzedCFG<A> result,
			Statement guard,
			Loop loop,
			Map<Statement, AnalysisState<A>> post)
			throws SemanticException {
		// the state reaching the guard from outside of the loop
		AnalysisState<A> state = post.get(guard).bottom();
		if (result.getEntrypoints().contains(guard))
			state = state.lub(result.getAnalysisStateBefore(guard));
		for (Statement pred : result.predecessorsOf(guard))
			if (!loop.contains(pred))
				state = state.lub(ForwardCFGFixpoint.traverseEdge(result.getEdgeConnecting(pred, guard),
						post.get(pred), result, analysis, result.getId(), null));
		return state;
	}

	private AnalysisState<A> preStateOf(
			AnalyzedCFG<A> result,
			Statement node)
			throws SemanticException {
		// the entry state computed by forward fixpoints, that traverse the
		// edges reaching the node (see ForwardFixpoint#getEntryState), while
		// AnalyzedCFG#getAnalysisStateBefore only joins the post-states of
		// the predecessors
		AnalysisState<A> state = result.getAnalysisStateAfter(node).bottom();
		if (result.getEntrypoints().contains(node))
			state = state.lub(result.getAnalysisStateBefore(node));
		for (Statement pred : result.predecessorsOf(node))
			state = state.lub(ForwardCFGFixpoint.traverseEdge(result.getEdgeConnecting(pred, node),
					result.getAnalysisStateAfter(pred), result, analysis, result.getId(), null));
		return state;
	}

	private boolean isFeasible(
			Edge edge,
			AnalysisState<A> state,
			Collection<SymbolicExpression> conditions,
			ScopeId<A> context)
			throws SemanticException {
		boolean trueEdge = edge instanceof TrueEdge;
		if (!trueEdge && !(edge instanceof FalseEdge))
			return true;
		if (state.getExecution().isBottom() || state.getExecutionState().isBottom())
			return false;
		if (conditions.isEmpty())
			return true;

		for (SymbolicExpression condition : conditions) {
			Satisfiability sat = analysis.satisfies(state, condition, context, edge.getSource());
			if (sat == Satisfiability.BOTTOM)
				continue;
			if (trueEdge ? sat != Satisfiability.NOT_SATISFIED : sat != Satisfiability.SATISFIED)
				return true;
		}
		return false;
	}

	private static Collection<ReachabilityGraph.Node> handlersOf(
			Statement st,
			Type type,
			Map<ErrorEdge, Collection<Type>> handlers,
			Map<Statement, ? extends AnalysisState<?>> post,
			Map<Statement, ReachabilityGraph.Node> nodes,
			Map<Statement, ReachabilityGraph.Node> firsts,
			Map<Statement, Loop> loops,
			ReachabilityGraph.Node exit) {
		// the edges catching the error, according to the same rule of
		// Analysis#moveErrorsToExecution
		List<ErrorEdge> catching = new ArrayList<>();
		for (Entry<ErrorEdge, Collection<Type>> handler : handlers.entrySet())
			if (handler.getKey().getProtectedBlock().getBody().contains(st)
					&& catches(Arrays.asList(handler.getKey().getTypes()), type)
					&& !catches(handler.getValue(), type))
				catching.add(handler.getKey());

		// errors caught by a protected block are removed from the state
		// before reaching the end of the enclosing ones: only the edges of
		// the innermost protected block catch the error
		ProtectedBlock innermost = null;
		for (ErrorEdge edge : catching)
			if (innermost == null || edge.getProtectedBlock().getBody().size() < innermost.getBody().size())
				innermost = edge.getProtectedBlock();

		Collection<ReachabilityGraph.Node> targets = new LinkedHashSet<>();
		for (ErrorEdge edge : catching)
			if (edge.getProtectedBlock() == innermost && !post.get(edge.getDestination()).isBottom())
				targets.add(target(edge.getDestination(), st, nodes, firsts, loops));
		if (targets.isEmpty())
			// the error is not caught, or the analysis determined that no
			// execution reaches the catch block
			targets.add(exit);
		return targets;
	}

	private static boolean catches(
			Collection<Type> caught,
			Type type) {
		for (Type t : caught)
			if (type.canBeAssignedTo(t))
				return true;
		return false;
	}

	private boolean mayHalt(
			AnalyzedCFG<A> result,
			Statement st,
			AnalysisState<A> post)
			throws SemanticException {
		ProgramState<A> halt = post.getHalt();
		if (halt.isBottom())
			return false;
		if (!halt.lessOrEqual(result.getAnalysisStateBefore(st).getHalt()))
			return true;

		// the halting executions produced by st might be indistinguishable
		// from the ones that reach it (e.g., in loops): we inspect the targets
		// of the calls it contains
		List<Call> calls = new ArrayList<>();
		st.accept(new GraphVisitor<CFG, Statement, Edge, Collection<Call>>() {

			@Override
			public boolean visit(
					Collection<Call> tool,
					CFG graph,
					Statement node) {
				if (node instanceof Call)
					tool.add((Call) node);
				return true;
			}
		}, calls);

		for (Call call : calls) {
			Call resolved = call instanceof UnresolvedCall ? resolve((UnresolvedCall) call, result) : call;
			if (resolved == null || mayHalt(resolved))
				return true;
		}
		return false;
	}

	private boolean mayHalt(
			Call call) {
		if (call instanceof MultiCall) {
			for (Call inner : ((MultiCall) call).getCalls())
				if (mayHalt(inner))
					return true;
			return false;
		}
		if (call instanceof TruncatedParamsCall)
			return mayHalt(((TruncatedParamsCall) call).getInnerCall());
		if (!(call instanceof CFGCall))
			// native or open calls: we cannot inspect their results
			return true;

		for (CFG target : ((CFGCall) call).getTargetedCFGs()) {
			Boolean halts = halting.get(target);
			if (halts == null) {
				halts = false;
				Collection<AnalyzedCFG<A>> res = results.get(target);
				if (res == null)
					halts = true;
				else
					for (AnalyzedCFG<A> r : res)
						for (Statement node : target.getNodes())
							if (!r.getAnalysisStateAfter(node).getHalt().isBottom()) {
								halts = true;
								break;
							}
				halting.put(target, halts);
			}
			if (halts)
				return true;
		}
		return false;
	}
}
