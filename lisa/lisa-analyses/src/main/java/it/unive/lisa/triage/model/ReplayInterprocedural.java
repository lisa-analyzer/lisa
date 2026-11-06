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
import it.unive.lisa.analysis.symbols.SymbolAliasing;
import it.unive.lisa.checks.semantic.SemanticTool;
import it.unive.lisa.conf.FixpointConfiguration;
import it.unive.lisa.events.EventQueue;
import it.unive.lisa.interprocedural.FixpointResults;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.interprocedural.OpenCallPolicy;
import it.unive.lisa.interprocedural.ScopeId;
import it.unive.lisa.interprocedural.callgraph.CallGraph;
import it.unive.lisa.interprocedural.callgraph.CallResolutionException;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.lattices.GenericMapLattice;
import it.unive.lisa.lattices.GenericSetLattice;
import it.unive.lisa.program.Application;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.edge.Edge;
import it.unive.lisa.program.cfg.fixpoints.CompoundState;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.call.CFGCall;
import it.unive.lisa.program.cfg.statement.call.Call;
import it.unive.lisa.program.cfg.statement.call.OpenCall;
import it.unive.lisa.program.cfg.statement.call.UnresolvedCall;
import it.unive.lisa.program.language.parameterassignment.ParameterAssigningStrategy;
import it.unive.lisa.program.language.scoping.ScopingStrategy;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.heap.HeapDereference;
import it.unive.lisa.symbolic.heap.HeapReference;
import it.unive.lisa.symbolic.heap.MemoryAllocation;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.GlobalVariable;
import it.unive.lisa.symbolic.value.HeapLocation;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.MemoryPointer;
import it.unive.lisa.symbolic.value.PushAny;
import it.unive.lisa.symbolic.value.Skip;
import it.unive.lisa.symbolic.value.TernaryExpression;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.type.Type;
import it.unive.lisa.util.datastructures.graph.GraphVisitor;
import it.unive.lisa.util.datastructures.graph.algorithms.FixpointException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.UnaryOperator;
import org.apache.commons.lang3.tuple.Pair;

/**
 * The {@link InterproceduralAnalysis} used to replay the semantics of the
 * statements of one {@link Frame} of an {@link AlarmModel} (whose result is an
 * {@link AnalyzedCFG}, i.e., a cfg in a given context) through a
 * {@link ReplayDomain}. No fixpoint is ever computed:
 * <ul>
 * <li>calls to cfgs are <b>summarized</b>, unless they are expanded (see
 * below): their result is an unknown value (a {@link SummaryMarker}), and the
 * heap is unconstrained after them; errors raised by the callees, and
 * executions halting inside them, are taken from the results of the analysis,
 * with all values unconstrained and under an unknown condition (since the
 * executions of the callees are not modeled);</li>
 * <li>open calls are evaluated through the {@link OpenCallPolicy} of the
 * analysis (the unknown values they return are not inputs of the program, see
 * {@link it.unive.lisa.triage.inputs.InputSourceResolver});</li>
 * <li>calls are resolved by the interprocedural analysis that produced the
 * results, so that resolution matches the one of the analysis;</li>
 * <li>the current context is the one of the replayed result.</li>
 * </ul>
 * Each replay of a node is a <i>run</i> (see
 * {@link #startRun(Statement, CFGCall, boolean)}), during which every call to
 * cfgs is recorded, in evaluation order, as a {@link ReplayedCall}. A call is
 * <b>expanded</b> if it is expandable, and the frames of all its callees are
 * among the ones to expand given at construction. Starting from the location
 * before the node, the first expanded call that is reached <i>enters</i> its
 * callees: the entry state of each callee is prepared as the analysis does
 * (scoping the state and assigning the parameters), and its store is recorded
 * as the transition entering the frame of the callee (see
 * {@link #getEntered()}), while the run stops at the call (errors raised by the
 * callees are propagated from their frames, see {@link TransitionExtractor}).
 * When the run starts from the location where the node resumes after an
 * expanded call returned, all the expanded calls up to that one (included) have
 * already returned: their result is their meta variable, assigned by the
 * transition returning from the callee, with no effects on the store and no
 * errors raised. When the run reaches the call it resumes after, the store is
 * reset to the identity, since the effects of the part of the node evaluated
 * before the call are already part of the transition entering the callees. The
 * operands evaluated before the call that are still to be used are then
 * evaluated at the location where the node resumes: this requires them to yield
 * the same values there.<br>
 * <br>
 * A call is <b>expandable</b> if:
 * <ul>
 * <li>some of its targets have a result in the context of the call (computed as
 * the analysis does), so that their code can be part of the model;</li>
 * <li>it is not recursive, that is, none of its targets appears in the chain of
 * frames leading to the replayed one;</li>
 * <li>it is not part of the node where the property is checked (since the
 * property must be checked on a single location);</li>
 * <li>the run lost no value and the store contains no unknown value (except for
 * the amounts of objects allocated by summarized calls) in its path conditions
 * and in the values of variables before the call, since an unknown value
 * evaluated again when the node resumes would be a different one; the effects
 * of the part of the node evaluated before the call (e.g., assignments,
 * allocations, writes of the heap, summarized calls whose results are not used
 * afterwards) are allowed, as they are only part of the transition entering the
 * callees;</li>
 * <li>(<i>operand rule</i>) the operands evaluated in the node before the call
 * that are not actual parameters of the call do not read the heap nor global
 * variables (i.e., their values contain no {@link AccessChild},
 * {@link HeapLocation}, {@link GlobalVariable}, {@link MemoryPointer}, nor
 * {@link MemoryAllocation}), since the callees might change them before the
 * operands are evaluated again, and contain no unknown value.</li>
 * </ul>
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 *
 * @param <A> the type of the abstract states of the analysis
 * @param <D> the type of the domain of the analysis
 */
public class ReplayInterprocedural<A extends AbstractLattice<A>, D extends AbstractDomain<A>>
		implements
		InterproceduralAnalysis<ReplayState<A>, ReplayDomain<A, D>> {

	private final SemanticTool<A, D> tool;

	private final Frame frame;

	private final AnalyzedCFG<A> result;

	private final Analysis<ReplayState<A>, ReplayDomain<A, D>> analysis;

	private final Analysis<A, D> plain;

	private final InterproceduralAnalysis<A, D> original;

	private final OpenCallPolicy policy;

	private final Set<Frame> expanded;

	private final BooleanSupplier budget;

	private final ResultCache<A> cache;

	/**
	 * The results of the callees of each call, in the context of the call.
	 */
	private final Map<CFGCall, List<AnalyzedCFG<A>>> callees = new LinkedHashMap<>();

	/**
	 * The node replayed by the current run.
	 */
	private Statement node;

	/**
	 * The expanded call after which the current run resumes, until it is
	 * reached.
	 */
	private CFGCall resumed;

	/**
	 * Whether or not calls can be expanded in the current run.
	 */
	private boolean expansion;

	private final List<ReplayedCall> calls = new ArrayList<>();

	private final Map<Frame, SymbolicStore> entered = new LinkedHashMap<>();

	/**
	 * Whether or not the current run stopped at an expanded call.
	 */
	private boolean stopped;

	/**
	 * Builds the analysis, summarizing all calls.
	 *
	 * @param tool   the tool containing the results of the analysis
	 * @param result the result whose statements are replayed
	 * @param domain the domain used for the replay
	 */
	public ReplayInterprocedural(
			SemanticTool<A, D> tool,
			AnalyzedCFG<A> result,
			ReplayDomain<A, D> domain) {
		this(tool, Frame.root(result), domain, Collections.emptySet(), () -> false);
	}

	/**
	 * Builds the analysis.
	 *
	 * @param tool     the tool containing the results of the analysis
	 * @param frame    the frame whose statements are replayed
	 * @param domain   the domain used for the replay
	 * @param expanded the frames of the callees to expand
	 * @param budget   tells whether or not new callees can be entered (e.g., if
	 *                     the model has room for their locations); it is
	 *                     queried only when an expanded call is entered
	 */
	public ReplayInterprocedural(
			SemanticTool<A, D> tool,
			Frame frame,
			ReplayDomain<A, D> domain,
			Set<Frame> expanded,
			BooleanSupplier budget) {
		this(tool, frame, domain, expanded, budget, new ResultCache<>());
	}

	/**
	 * Builds the analysis.
	 *
	 * @param tool     the tool containing the results of the analysis
	 * @param frame    the frame whose statements are replayed
	 * @param domain   the domain used for the replay
	 * @param expanded the frames of the callees to expand
	 * @param budget   tells whether or not new callees can be entered (e.g., if
	 *                     the model has room for their locations); it is
	 *                     queried only when an expanded call is entered
	 * @param cache    the cache of the information computed from the results of
	 *                     the analysis
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public ReplayInterprocedural(
			SemanticTool<A, D> tool,
			Frame frame,
			ReplayDomain<A, D> domain,
			Set<Frame> expanded,
			BooleanSupplier budget,
			ResultCache<A> cache) {
		this.tool = tool;
		this.frame = frame;
		this.result = (AnalyzedCFG<A>) frame.getResult();
		this.analysis = new ReplayAnalysis<>(domain, tool.getConfiguration().shouldSmashError);
		// an analysis without events, as the replay is not part of the
		// fixpoint computation
		this.plain = new Analysis(tool.getConfiguration().analysis, tool.getConfiguration().shouldSmashError);
		this.original = (InterproceduralAnalysis<A, D>) tool.getConfiguration().interproceduralAnalysis;
		this.policy = tool.getConfiguration().openCallPolicy;
		this.expanded = expanded;
		this.budget = budget;
		this.cache = cache;
	}

	/**
	 * Yields the result whose statements are replayed.
	 *
	 * @return the result
	 */
	public AnalyzedCFG<A> getResult() {
		return result;
	}

	/**
	 * Yields the frame whose statements are replayed.
	 *
	 * @return the frame
	 */
	public Frame getFrame() {
		return frame;
	}

	/**
	 * Starts a new run, that is, the replay of a node from one of its
	 * locations, clearing the calls and the entered frames recorded by the
	 * previous run.
	 *
	 * @param node      the replayed node
	 * @param resumed   the expanded call after which the node resumes, or
	 *                      {@code null} if the replay starts before the node
	 * @param expansion whether or not calls can be expanded in this run
	 */
	public void startRun(
			Statement node,
			CFGCall resumed,
			boolean expansion) {
		this.node = node;
		this.resumed = resumed;
		this.expansion = expansion;
		this.calls.clear();
		this.entered.clear();
		this.stopped = false;
	}

	/**
	 * Yields {@code true} if the current run stopped at an expanded call,
	 * entering its callees. Executions continuing after such a call are
	 * spurious: their abstract state is bottom, even if their computed
	 * expressions might not be.
	 *
	 * @return whether or not the run stopped at an expanded call
	 */
	public boolean hasStopped() {
		return stopped;
	}

	/**
	 * Yields the calls to cfgs reached during the current run, in evaluation
	 * order.
	 *
	 * @return the calls
	 */
	public List<ReplayedCall> getCalls() {
		return new ArrayList<>(calls);
	}

	/**
	 * Yields the frames entered during the current run, each mapped to the
	 * transition from the beginning of the run to the entry of the callee.
	 *
	 * @return the entered frames
	 */
	public Map<Frame, SymbolicStore> getEntered() {
		return new LinkedHashMap<>(entered);
	}

	@Override
	public boolean needsCallGraph() {
		return false;
	}

	@Override
	public void init(
			Application app,
			CallGraph callgraph,
			OpenCallPolicy policy,
			EventQueue events,
			Analysis<ReplayState<A>, ReplayDomain<A, D>> analysis) {
		throw new UnsupportedOperationException("Replays do not need initialization");
	}

	@Override
	public Analysis<ReplayState<A>, ReplayDomain<A, D>> getAnalysis() {
		return analysis;
	}

	@Override
	public EventQueue getEventQueue() {
		// replays are not part of the analysis
		return null;
	}

	@Override
	public void fixpoint(
			AnalysisState<ReplayState<A>> entryState,
			FixpointConfiguration<ReplayState<A>, ReplayDomain<A, D>> conf)
			throws FixpointException {
		throw new UnsupportedOperationException("Replays do not compute fixpoints");
	}

	@Override
	public Collection<AnalyzedCFG<ReplayState<A>>> getAnalysisResultsOf(
			CFG cfg) {
		throw new UnsupportedOperationException("Replays do not produce results");
	}

	@Override
	public AnalysisState<ReplayState<A>> getAbstractResultOf(
			CFGCall call,
			AnalysisState<ReplayState<A>> entryState,
			ExpressionSet[] parameters,
			StatementStore<ReplayState<A>> expressions)
			throws SemanticException {
		if (stopped
				&& (entryState.getExecution().isBottom() || entryState.getExecutionState().isBottom()))
			// the call is not reached, since the run stopped at a previous
			// expanded call: the semantics of the call would make the
			// execution non-bottom
			return entryState.bottomExecution();
		List<AnalyzedCFG<A>> results = calleesOf(call);
		List<Frame> frames = new ArrayList<>(results.size());
		for (AnalyzedCFG<A> callee : results)
			frames.add(frame.enter(call, callee));
		String obstacle = obstacle(call, results, entryState, parameters, expressions);
		// the callees of a call are expanded together
		boolean expand = obstacle == null && !frames.isEmpty() && frames.stream().anyMatch(expanded::contains);

		if (resumed != null) {
			// the run resumes after an expanded call: the ones reached before
			// it (and the call itself) have already returned
			boolean last = resumed.equals(call);
			if (last || expand) {
				if (last)
					resumed = null;
				calls.add(new ReplayedCall(call, frames, obstacle, true));
				return returned(call, entryState, last);
			}
		} else if (expand && budget.getAsBoolean()) {
			calls.add(new ReplayedCall(call, frames, obstacle, true));
			return enter(call, results, frames, entryState, parameters, expressions);
		}

		calls.add(new ReplayedCall(call, frames, obstacle, false));
		return summarize(call, entryState);
	}

	/**
	 * Summarizes the given call (see the description of this class).
	 */
	private AnalysisState<ReplayState<A>> summarize(
			CFGCall call,
			AnalysisState<ReplayState<A>> entryState)
			throws SemanticException {
		ScopeId<ReplayState<A>> context = getCurrentContext();
		String reason = "result of call to " + call.getFullTargetName();
		AnalysisState<ReplayState<A>> callres;
		if (call.getStaticType().isVoidType())
			callres = analysis.smallStepSemantics(entryState, new Skip(call.getLocation()), context, call);
		else
			// as done by open call policies: the returned value is assigned
			// to the meta variable of the call
			callres = analysis.assign(
					entryState,
					call.getMetaVariable(),
					new SummaryMarker(call.getStaticType(), reason, call.getLocation()),
					context,
					call);
		// the callees might have written the heap, except for the immutable
		// fields they cannot write
		Set<Identifier> preserved = entryState.getExecutionState().getStore().getFactory().getHeapModel()
				.preservedBy(call.getTargets());
		callres = callres.withExecution(mapState(callres.getExecution(),
				s -> s.withStore(s.getStore().heapHavoc(preserved, call.getLocation()))));
		return withCalleeFailures(call, entryState, callres);
	}

	/**
	 * Adds to the given result of a call the errors raised and the halts
	 * reached inside its callees, as computed by the analysis, with all values
	 * unconstrained: whether they happen depends on the execution of the
	 * callees, that is not modeled, and is thus an unknown condition.
	 */
	private AnalysisState<ReplayState<A>> withCalleeFailures(
			CFGCall call,
			AnalysisState<ReplayState<A>> entryState,
			AnalysisState<ReplayState<A>> callres)
			throws SemanticException {
		if (entryState.getExecution().isBottom())
			return callres;
		AnalysisState<A> post = result.getAnalysisStateAfter(call.getSource() == null ? call : call.getSource());
		SummaryMarker happens = new SummaryMarker(call.getProgram().getTypes().getBooleanType(),
				"errors raised and halts reached by " + call.getFullTargetName(), call.getLocation());
		SymbolicStore lost = entryState.getExecutionState().getStore().havocAll().assume(happens, call);
		// errors raised by calls are attributed to the call in the program
		Call thrower = call;
		while (thrower.getSource() != null)
			thrower = thrower.getSource();
		// the errors raised by a call that is part of a larger expression
		// (e.g., the constructor call of an object creation) might be added
		// to the states of the enclosing expressions only: the errors of the
		// call are also looked for in the state after the node
		List<AnalysisState<A>> posts = new ArrayList<>(2);
		posts.add(post);
		if (node != null && node != call && result.containsNode(node))
			posts.add(result.getAnalysisStateAfter(node));
		Set<Error> added = new LinkedHashSet<>();
		for (AnalysisState<A> state : posts) {
			if (state == null)
				continue;
			GenericMapLattice<Error, ProgramState<A>> errors = state.getErrors();
			if (!errors.isBottom() && !errors.isTop())
				for (Error error : errors.getKeys())
					if (error.getThrower().equals(thrower) && !errors.getState(error).isBottom() && added.add(error))
						callres = callres.addError(error, lift(errors.getState(error), lost));
			GenericMapLattice<Type, GenericSetLattice<Statement>> smashed = state.getSmashedErrors();
			if (!smashed.isBottom() && !smashed.isTop() && !state.getSmashedErrorsState().isBottom())
				for (Type type : smashed.getKeys())
					if (smashed.getState(type).contains(thrower) && added.add(new Error(type, thrower)))
						callres = callres.addError(new Error(type, thrower),
								lift(state.getSmashedErrorsState(), lost));
		}
		if (!post.getHalt().isBottom())
			callres = callres.withHalt(callres.getHalt().lub(lift(post.getHalt(), lost)));
		return callres;
	}

	/**
	 * Enters the callees of the given expanded call: the entry state of each
	 * callee is prepared as the analysis does, and recorded as the transition
	 * to the frame of the callee. The normal execution stops at the call.
	 */
	private AnalysisState<ReplayState<A>> enter(
			CFGCall call,
			List<AnalyzedCFG<A>> results,
			List<Frame> frames,
			AnalysisState<ReplayState<A>> entryState,
			ExpressionSet[] parameters,
			StatementStore<ReplayState<A>> expressions)
			throws SemanticException {
		stopped = true;
		// as done by the analysis: errors and halts are not affected by the
		// call
		AnalysisState<ReplayState<A>> initial = entryState.removeAllErrors(true);
		ScopingStrategy scoping = call.getProgram().getFeatures().getScopingStrategy();
		ParameterAssigningStrategy assigning = call.getProgram().getFeatures().getAssigningStrategy();
		for (int i = 0; i < results.size(); i++) {
			Pair<AnalysisState<ReplayState<A>>, ExpressionSet[]> scoped = scoping.scope(call, new ScopeToken(call),
					initial, analysis, getCurrentContext(), parameters);
			Pair<AnalysisState<ReplayState<A>>, ExpressionSet[]> prepared = assigning.prepare(call,
					scoped.getLeft(), this, expressions, results.get(i).getDescriptor().getFormals(),
					scoped.getRight());
			AnalysisState<ReplayState<A>> callee = prepared.getLeft();
			if (!callee.getExecution().isBottom())
				entered.merge(frames.get(i), callee.getExecutionState().getStore(), SymbolicStore::lub);
		}

		// no computed expression is left, as it would make the execution
		// non-bottom; errors raised inside the callees are propagated from
		// their frames, and executions halting inside them reach no other
		// location
		return entryState.bottomExecution();
	}

	/**
	 * Yields the result of an expanded call whose callees already returned: its
	 * meta variable, assigned by the transition returning from the callees. The
	 * abstract state becomes the one computed by the analysis after the call,
	 * that accounts for the effects of the callees. If the call is the one the
	 * run resumes after, the effects of the part of the node evaluated before
	 * it are already part of the transition entering the callees: the store is
	 * reset to the identity, so that the rest of the run is relative to the
	 * location where the node resumes, and the errors raised before the call
	 * (already raised when entering the callees) are discarded.
	 */
	private AnalysisState<ReplayState<A>> returned(
			CFGCall call,
			AnalysisState<ReplayState<A>> entryState,
			boolean resume)
			throws SemanticException {
		ProgramState<ReplayState<A>> exec = entryState.getExecution();
		if (exec.isBottom())
			return entryState;
		ScopeId<ReplayState<A>> context = getCurrentContext();
		AnalysisState<A> post = result.getAnalysisStateAfter(call.getSource() == null ? call : call.getSource());
		A inner;
		if (!post.getExecution().isBottom())
			inner = post.getExecutionState();
		else {
			// the state after the call is not part of the results (e.g., the
			// call is synthesized during the analysis): the callees might
			// have changed the heap, and the returned value is unknown
			inner = exec.getState().getInner().withTopMemory();
			if (!call.returnsVoid(entryState))
				inner = analysis.domain.getDomain().assign(inner, call.getMetaVariable(),
						new PushAny(call.getStaticType(), call.getLocation()), result.getId(), call);
		}
		ReplayState<A> replayed = exec.getState().withInner(inner);
		AnalysisState<ReplayState<A>> state = entryState;
		if (resume) {
			replayed = replayed.withStore(replayed.getStore().identity());
			state = state.removeAllErrors(true);
		}
		state = state.withExecution(new ProgramState<>(
				replayed,
				exec.getComputedExpressions(),
				exec.getFixpointInformation()));
		if (call.returnsVoid(entryState))
			return analysis.smallStepSemantics(state, new Skip(call.getLocation()), context, call);
		return analysis.smallStepSemantics(state, call.getMetaVariable(), context, call);
	}

	/**
	 * Yields the reason why the given call is not expandable, or {@code null}
	 * if it is expandable (see the description of this class).
	 */
	private String obstacle(
			CFGCall call,
			List<AnalyzedCFG<A>> results,
			AnalysisState<ReplayState<A>> entryState,
			ExpressionSet[] parameters,
			StatementStore<ReplayState<A>> expressions) {
		for (Frame f = frame; f != null; f = f.getParent())
			for (CFG target : call.getTargetedCFGs())
				if (f.getResult().getDescriptor().equals(target.getDescriptor()))
					return "recursive call";
		if (results.isEmpty())
			return "no target has a result in the context of the call";
		if (!expansion)
			return "call in the node where the property is checked";
		// the effects of the part of the node evaluated before the call (e.g.,
		// writes of the heap) are only part of the transition entering the
		// callee, as the store is reset when the node resumes: only the values
		// they compute and that are used afterwards matter
		for (SymbolicCase c : entryState.getExecutionState().getStore().getCases()) {
			if (c.isHavocAll())
				return "values lost before the call";
			if (c.getPathCondition().stream().anyMatch(ReplayInterprocedural::hasFreshValues))
				return "unknown values produced before the call";
			for (Map.Entry<Identifier, SymbolicExpression> u : c.getUpdates().entrySet())
				if (!HeapModel.isHeapState(u.getKey()) && hasFreshValues(u.getValue()))
					return "unknown values produced before the call";
		}

		// the operand rule
		Set<SymbolicExpression> actuals = new LinkedHashSet<>();
		for (ExpressionSet param : parameters)
			for (SymbolicExpression e : param)
				actuals.add(e);
		for (Statement st : expressions.getKeys()) {
			if (st == call || isWithin(st, call))
				continue;
			AnalysisState<ReplayState<A>> state = expressions.getState(st);
			if (state.getExecution().isBottom())
				continue;
			for (SymbolicExpression e : state.getExecutionExpressions())
				if (!actuals.contains(e))
					if (readsMemory(e))
						return "operand reading the heap or globals evaluated before the call";
					else if (hasUnknownValues(e))
						return "unknown values produced before the call";
		}
		return null;
	}

	/**
	 * Yields {@code true} if the given statement is (part of) an actual
	 * parameter of the given call.
	 */
	private static boolean isWithin(
			Statement st,
			Call call) {
		List<Expression> params = new ArrayList<>();
		for (Call c = call; c != null; c = c.getSource())
			Collections.addAll(params, c.getParameters());
		Statement current = st;
		while (current instanceof Expression) {
			if (params.contains(current))
				return true;
			current = ((Expression) current).getParentStatement();
		}
		return false;
	}

	/**
	 * Yields {@code true} if evaluating the given expression reads the heap or
	 * global variables.
	 */
	private static boolean readsMemory(
			SymbolicExpression e) {
		if (e instanceof AccessChild || e instanceof HeapLocation || e instanceof GlobalVariable
				|| e instanceof MemoryPointer || e instanceof MemoryAllocation)
			return true;
		if (e instanceof HeapReference)
			return readsMemory(((HeapReference) e).getExpression());
		if (e instanceof HeapDereference)
			return readsMemory(((HeapDereference) e).getExpression());
		if (e instanceof UnaryExpression)
			return readsMemory(((UnaryExpression) e).getExpression());
		if (e instanceof BinaryExpression)
			return readsMemory(((BinaryExpression) e).getLeft()) || readsMemory(((BinaryExpression) e).getRight());
		if (e instanceof TernaryExpression)
			return readsMemory(((TernaryExpression) e).getLeft())
					|| readsMemory(((TernaryExpression) e).getMiddle())
					|| readsMemory(((TernaryExpression) e).getRight());
		return false;
	}

	private static boolean hasUnknownValues(
			SymbolicExpression e) {
		if (e instanceof PushAny || e instanceof FreshValue)
			return true;
		if (e instanceof UnaryExpression)
			return hasUnknownValues(((UnaryExpression) e).getExpression());
		if (e instanceof BinaryExpression)
			return hasUnknownValues(((BinaryExpression) e).getLeft())
					|| hasUnknownValues(((BinaryExpression) e).getRight());
		if (e instanceof TernaryExpression)
			return hasUnknownValues(((TernaryExpression) e).getLeft())
					|| hasUnknownValues(((TernaryExpression) e).getMiddle())
					|| hasUnknownValues(((TernaryExpression) e).getRight());
		return false;
	}

	private static boolean hasFreshValues(
			SymbolicExpression e) {
		if (e instanceof AllocationOffset)
			// the amount of objects allocated by summarized calls only
			// affects the counter of allocations
			return false;
		if (e instanceof FreshValue)
			return true;
		if (e instanceof UnaryExpression)
			return hasFreshValues(((UnaryExpression) e).getExpression());
		if (e instanceof BinaryExpression)
			return hasFreshValues(((BinaryExpression) e).getLeft())
					|| hasFreshValues(((BinaryExpression) e).getRight());
		if (e instanceof TernaryExpression)
			return hasFreshValues(((TernaryExpression) e).getLeft())
					|| hasFreshValues(((TernaryExpression) e).getMiddle())
					|| hasFreshValues(((TernaryExpression) e).getRight());
		return false;
	}

	/**
	 * Yields the results of the targets of the given call in the context of the
	 * call, that is, the context of this frame extended with the call (as
	 * computed by the analysis). If the context cannot be computed, the unique
	 * result of a target is used, if any.
	 */
	private List<AnalyzedCFG<A>> calleesOf(
			CFGCall call) {
		List<AnalyzedCFG<A>> cached = callees.get(call);
		if (cached != null)
			return cached;
		List<AnalyzedCFG<A>> res = new ArrayList<>();
		ScopeId<A> pushed = null;
		boolean computed = false;
		for (CFG target : call.getTargetedCFGs()) {
			AnalyzedCFG<A> forced = expandedResult(call, target);
			if (forced != null) {
				res.add(forced);
				continue;
			}
			Collection<AnalyzedCFG<A>> candidates = tool.getResultOf(target);
			if (candidates == null || candidates.isEmpty())
				continue;
			if (!computed) {
				pushed = pushedContext(call);
				computed = true;
			}
			AnalyzedCFG<A> match = null;
			if (pushed != null) {
				for (AnalyzedCFG<A> candidate : candidates)
					if (pushed.equals(candidate.getId()))
						match = candidate;
			} else if (candidates.size() == 1)
				match = candidates.iterator().next();
			ScopeId<A> context = pushed;
			if (pushed != null && dependsOnStates(call, pushed)) {
				// the context depends on the state at the call, that the
				// replay might not compute exactly as the analysis did (e.g.,
				// for calls synthesized by the semantics of other
				// statements): the states of all the results describe the
				// executions of the callee
				if (match == null)
					match = candidates.size() == 1 ? candidates.iterator().next()
							: cache.merged(candidates, pushed, () -> merge(target, candidates, context));
				else if (candidates.size() > 1) {
					ScopeId<A> id = match.getId();
					match = cache.merged(candidates, id, () -> merge(target, candidates, id));
				}
			}
			if (match != null)
				res.add(match);
		}
		callees.put(call, res);
		return res;
	}

	/**
	 * Yields the result of the given target of the given call in the frames to
	 * expand, if any. The frames to expand might come from a chain of calls
	 * computed from the results of the analysis (see
	 * {@link AlarmModelBuilder}), whose contexts might depend on states at the
	 * calls that the replay does not compute exactly as the analysis did: the
	 * result of the frame is used, so that the frames of the chain are entered.
	 * The result describes some of the executions of the callee from the call,
	 * as found by the analysis.
	 */
	@SuppressWarnings("unchecked")
	private AnalyzedCFG<A> expandedResult(
			CFGCall call,
			CFG target) {
		for (Frame f : expanded)
			if (!f.isRoot()
					&& frame.equals(f.getParent())
					&& call.equals(f.getCall())
					&& f.getResult().getDescriptor().equals(target.getDescriptor()))
				return (AnalyzedCFG<A>) f.getResult();
		return null;
	}

	/**
	 * Yields a result of the given cfg, with the given context, whose states
	 * are the lubs of the ones of the given results. The executions of a callee
	 * reached by an expanded call might not be all described by the result of
	 * the context of the call (e.g., with contexts depending on the states at
	 * the calls, the call of a loop leads to several results, one for each
	 * state reached by the fixpoint): the states of all the results of the
	 * callee soundly describe them.
	 */
	private AnalyzedCFG<A> merge(
			CFG cfg,
			Collection<AnalyzedCFG<A>> results,
			ScopeId<A> id) {
		try {
			Map<Statement, AnalysisState<A>> after = new LinkedHashMap<>();
			GraphVisitor<CFG, Statement, Edge, Void> collector = new GraphVisitor<>() {

				@Override
				public boolean visit(
						Void tool,
						CFG graph,
						Statement node) {
					after.put(node, null);
					return true;
				}

			};
			for (Statement node : cfg.getNodes())
				node.accept(collector, null);
			AnalysisState<A> template = null;
			for (Map.Entry<Statement, AnalysisState<A>> entry : after.entrySet()) {
				AnalysisState<A> lub = null;
				for (AnalyzedCFG<A> r : results) {
					AnalysisState<A> state = r.getAnalysisStateAfter(entry.getKey());
					if (state != null)
						lub = lub == null ? state : lub.lub(state);
				}
				entry.setValue(lub);
				if (template == null && lub != null)
					template = lub;
			}
			after.values().removeIf(state -> state == null);
			Map<Statement, AnalysisState<A>> entries = new LinkedHashMap<>();
			for (Statement entry : cfg.getEntrypoints()) {
				AnalysisState<A> lub = null;
				for (AnalyzedCFG<A> r : results) {
					AnalysisState<A> state = r.getAnalysisStateBefore(entry);
					if (state != null)
						lub = lub == null ? state : lub.lub(state);
				}
				if (lub != null) {
					entries.put(entry, lub);
					if (template == null)
						template = lub;
				}
			}
			if (template == null)
				return results.iterator().next();
			return new AnalyzedCFG<>(cfg, id, template.bottom(), entries, after);
		} catch (SemanticException e) {
			// the states cannot be merged: the callee is not expanded
			return null;
		}
	}

	/**
	 * Yields {@code true} if the contexts of the analysis depend on the states
	 * at the calls (and not only on the calls), that is, if pushing the given
	 * call without a state does not yield the given context: in that case, the
	 * callee might have several results for the same call (e.g., one for each
	 * state reached by the fixpoint at a call in a loop), and the one of the
	 * last state might not describe all the executions.
	 */
	private boolean dependsOnStates(
			CFGCall call,
			ScopeId<A> pushed) {
		try {
			return !pushed.equals(result.getId().push(call, null));
		} catch (RuntimeException e) {
			return true;
		}
	}

	private ScopeId<A> pushedContext(
			CFGCall call) {
		ScopeId<A> id = result.getId();
		if (id == null)
			return null;
		try {
			return id.push(call, callState(call));
		} catch (SemanticException | RuntimeException e) {
			return null;
		}
	}

	/**
	 * Yields the state used by the analysis to compute the context of the
	 * callees of the given call (see {@link CompoundState}): the one after the
	 * evaluation of the parameters, without computed expressions.
	 */
	private CompoundState<A> callState(
			CFGCall call)
			throws SemanticException {
		Call site = call.getSource() == null ? call : call.getSource();
		Statement root = node == null ? site.getRootStatement() : node;
		StatementStore<A> store = new StatementStore<>(result.getEntryState()).bottom();
		if (!result.containsNode(site.getRootStatement()))
			// the call has been synthesized during the analysis, and the
			// states of its expressions are not part of the results: we use
			// the state before the node containing it
			return CompoundState.of(
					cache.preStateOf(result, root, plain).withExecutionExpressions(new ExpressionSet()),
					store);
		Expression[] params = site.getParameters();
		for (Expression param : params)
			store.put(param, result.getAnalysisStateAfter(param));
		AnalysisState<A> state;
		if (params.length == 0) {
			Statement pred = site.getEvaluationPredecessor();
			state = pred == null ? cache.preStateOf(result, root, plain) : result.getAnalysisStateAfter(pred);
		} else
			state = result.getAnalysisStateAfter(params[site.getOrder().last(params.length)]);
		return CompoundState.of(state.withExecutionExpressions(new ExpressionSet()), store);
	}

	private ProgramState<ReplayState<A>> lift(
			ProgramState<A> state,
			SymbolicStore store) {
		return new ProgramState<>(
				new ReplayState<>(state.getState(), store),
				state.getComputedExpressions(),
				state.getFixpointInformation());
	}

	private static <A extends AbstractLattice<A>> ProgramState<ReplayState<A>> mapState(
			ProgramState<ReplayState<A>> state,
			UnaryOperator<ReplayState<A>> f) {
		if (state.isBottom())
			return state;
		return new ProgramState<>(f.apply(state.getState()), state.getComputedExpressions(),
				state.getFixpointInformation());
	}

	@Override
	public AnalysisState<ReplayState<A>> getAbstractResultOf(
			OpenCall call,
			AnalysisState<ReplayState<A>> entryState,
			ExpressionSet[] parameters,
			StatementStore<ReplayState<A>> expressions)
			throws SemanticException {
		return policy.apply(call, entryState, analysis, getCurrentContext(), parameters);
	}

	@Override
	public Call resolve(
			UnresolvedCall call,
			Set<Type>[] types,
			SymbolAliasing aliasing)
			throws CallResolutionException {
		return original.resolve(call, types, aliasing);
	}

	@Override
	public FixpointResults<ReplayState<A>> getFixpointResults() {
		throw new UnsupportedOperationException("Replays do not produce results");
	}

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public ScopeId<ReplayState<A>> getCurrentContext() {
		// the context of the replayed result is used as-is, so that the
		// abstract states see the same context they saw during the analysis
		return (ScopeId) result.getId();
	}
}
