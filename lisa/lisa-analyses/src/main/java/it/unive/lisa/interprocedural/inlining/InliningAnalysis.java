package it.unive.lisa.interprocedural.inlining;

import it.unive.lisa.AnalysisExecutionException;
import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.AnalyzedCFG;
import it.unive.lisa.analysis.OptimizedAnalyzedCFG;
import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.conf.FixpointConfiguration;
import it.unive.lisa.events.EventQueue;
import it.unive.lisa.interprocedural.CFGResults;
import it.unive.lisa.interprocedural.CallGraphBasedAnalysis;
import it.unive.lisa.interprocedural.FixpointResults;
import it.unive.lisa.interprocedural.InterproceduralAnalysisException;
import it.unive.lisa.interprocedural.NoEntryPointException;
import it.unive.lisa.interprocedural.OpenCallPolicy;
import it.unive.lisa.interprocedural.Recursion;
import it.unive.lisa.interprocedural.callgraph.CallGraph;
import it.unive.lisa.interprocedural.events.CFGFixpointEnd;
import it.unive.lisa.interprocedural.events.CFGFixpointStart;
import it.unive.lisa.interprocedural.events.CFGFixpointStored;
import it.unive.lisa.interprocedural.events.ComputedCallResult;
import it.unive.lisa.interprocedural.events.ComputedCallState;
import it.unive.lisa.interprocedural.events.FixpointEnd;
import it.unive.lisa.interprocedural.events.FixpointIterationEnd;
import it.unive.lisa.interprocedural.events.FixpointIterationStart;
import it.unive.lisa.interprocedural.events.FixpointStart;
import it.unive.lisa.interprocedural.events.PrecomputedCallResult;
import it.unive.lisa.interprocedural.inlining.recursion.RecursionSolver;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.logging.IterationLogger;
import it.unive.lisa.program.Application;
import it.unive.lisa.program.CodeUnit;
import it.unive.lisa.program.SyntheticLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeMember;
import it.unive.lisa.program.cfg.CodeMemberDescriptor;
import it.unive.lisa.program.cfg.fixpoints.CompoundState;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.call.CFGCall;
import it.unive.lisa.program.cfg.statement.call.Call;
import it.unive.lisa.program.language.scoping.ScopingStrategy;
import it.unive.lisa.util.collections.workset.WorkingSet;
import it.unive.lisa.util.datastructures.graph.algorithms.FixpointException;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * An inlining-based interprocedural analysis. This means that each call
 * receives its own result, with no "compacting" based on context or other
 * technique: each call receives its own result that is uniquely determined by
 * the call's entry strate. Recursions are not supported: either they converge
 * to a result, or the analysis (i) diverges if no maximum call stack depth is
 * set through the constructor, or (ii) terminates with an exception when the
 * maximum call stack depth has been reached.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <A> the kind of {@link AbstractLattice} produced by the domain
 *                {@code D}
 * @param <D> the kind of {@link AbstractDomain} to run during the analysis
 */
public class InliningAnalysis<A extends AbstractLattice<A>,
		D extends AbstractDomain<A>>
		extends
		CallGraphBasedAnalysis<A, D> {

	private static final Logger LOG = LogManager.getLogger(InliningAnalysis.class);

	/**
	 * The maximum call stack depth. A negative value means infinite depth. If a
	 * call chain exceeds this depth, an exception is raised or top is returned,
	 * depending on {@link #shouldRaiseException}.
	 */
	protected final int maxCallStackDepth;

	/**
	 * Whether an exception should be raised when the maximum call stack depth
	 * is reached. If {@code false}, then top is returned instead.
	 */
	protected final boolean shouldRaiseException;

	/**
	 * The current call stack.
	 */
	protected CallStackId<A> token;

	/**
	 * The entry state of each call currently on the call stack.
	 */
	protected Map<CFGCall, CompoundState<A>> entries = new HashMap<>();

	/**
	 * The results computed by this analysis.
	 */
	protected FixpointResults<A> results;

	/**
	 * The kind of {@link WorkingSet} to use during this analysis.
	 */
	protected WorkingSet<Statement> workingSet;

	/**
	 * The fixpoint configuration.
	 */
	protected FixpointConfiguration<A, D> conf;

	/**
	 * The results of the recursive calls, if any.
	 */
	protected Deque<Map<CFGCall, AnalysisState<A>>> recursionResults = new LinkedList<>();

	/**
	 * Builds the analysis, using an infinite call stack depth.
	 */
	public InliningAnalysis() {
		this(-1, true);
	}

	/**
	 * Builds the analysis that raises an exception when the maximum call stack
	 * depth is reached.
	 *
	 * @param maxCallStackDepth the maximum call stack depth. A negative value
	 *                              means infinite depth. If a call chain
	 *                              exceeds this depth
	 */
	public InliningAnalysis(
			int maxCallStackDepth) {
		this(maxCallStackDepth, true);
	}

	/**
	 * Builds the analysis.
	 *
	 * @param maxCallStackDepth    the maximum call stack depth. A negative
	 *                                 value means infinite depth. If a call
	 *                                 chain exceeds this depth
	 * @param shouldRaiseException whether an exception should be raised when
	 *                                 the maximum call stack depth is reached.
	 *                                 If {@code false}, then top is returned
	 *                                 instead
	 */
	public InliningAnalysis(
			int maxCallStackDepth,
			boolean shouldRaiseException) {
		this.maxCallStackDepth = maxCallStackDepth;
		this.shouldRaiseException = shouldRaiseException;
		this.token = CallStackId.create();
	}

	/**
	 * Builds the analysis by copying the given one.
	 * 
	 * @param other the original analysis to copy
	 */
	protected InliningAnalysis(
			InliningAnalysis<A, D> other) {
		super(other);
		this.maxCallStackDepth = other.maxCallStackDepth;
		this.shouldRaiseException = other.shouldRaiseException;
		this.token = other.token;
		this.results = other.results;
		this.workingSet = other.workingSet;
		this.conf = other.conf;
	}

	@Override
	public void init(
			Application app,
			CallGraph callgraph,
			OpenCallPolicy policy,
			EventQueue events,
			Analysis<A, D> analysis)
			throws InterproceduralAnalysisException {
		super.init(app, callgraph, policy, events, analysis);
		this.conf = null;
		this.results = null;
	}

	@Override
	public void fixpoint(
			AnalysisState<A> entryState,
			FixpointConfiguration<A, D> conf)
			throws FixpointException {
		if (conf.forwardFixpoint == null)
			throw new IllegalArgumentException("A forward fixpoint is required for this analysis");

		this.workingSet = conf.fixpointWorkingSet;
		this.conf = conf;

		// new fixpoint execution: reset
		CodeUnit unit = new CodeUnit(SyntheticLocation.INSTANCE, app.getPrograms()[0], "singleton");
		CFG singleton = new CFG(new CodeMemberDescriptor(SyntheticLocation.INSTANCE, unit, false, "singleton"));
		CallStackId<A> empty = token.startingId();
		AnalyzedCFG<A> graph = conf.usesOptimizedForwardFixpoint()
				? new OptimizedAnalyzedCFG<>(singleton, empty, entryState.bottom(), this)
				: new AnalyzedCFG<>(singleton, empty, entryState);
		CFGResults<A> value = new CFGResults<>(graph);
		this.results = new FixpointResults<>(value.top());

		if (app.getEntryPoints().isEmpty())
			throw new NoEntryPointException();

		Collection<CFG> entryPoints = new TreeSet<>(
				(
						c1,
						c2) -> c1.getDescriptor().getLocation().compareTo(c2.getDescriptor().getLocation()));
		entryPoints.addAll(app.getEntryPoints());

		if (events != null) {
			events.post(new FixpointStart());
			events.post(new FixpointIterationStart(1));
		}

		for (CFG cfg : IterationLogger.iterate(LOG, entryPoints, "Processing entrypoints", "entries"))
			try {
				token = empty;
				AnalysisState<A> entryStateCFG = prepareEntryStateOfEntryPoint(entryState, cfg);

				if (events != null)
					events.post(new CFGFixpointStart<>(cfg, token, entryState));

				Map<CFGCall, AnalysisState<A>> recursiveCalls = new HashMap<>();
				recursionResults.addLast(recursiveCalls);
				AnalyzedCFG<A> fixpointResult = cfg.fixpoint(entryStateCFG, this, workingSet.mk(), conf, empty);
				recursionResults.removeLast();

				if (events != null) {
					events.post(new CFGFixpointEnd<>(cfg, token, entryState, fixpointResult));
					events.post(new CFGFixpointStored<>(cfg, token, entryState, fixpointResult, fixpointResult));
				}

				results.putResult(cfg, empty, fixpointResult);
			} catch (SemanticException e) {
				throw new AnalysisExecutionException("Error while creating the entrystate for " + cfg, e);
			} catch (FixpointException e) {
				throw new AnalysisExecutionException("Error while computing fixpoint for entrypoint " + cfg, e);
			}

		if (events != null) {
			events.post(new FixpointIterationEnd(1));
			events.post(new FixpointEnd());
		}
	}

	@Override
	public Collection<AnalyzedCFG<A>> getAnalysisResultsOf(
			CFG cfg) {
		if (results.contains(cfg))
			return results.getState(cfg).getAll();
		else
			return Collections.emptySet();
	}

	/**
	 * Runs a fixpoint over the given {@link CFG}.
	 * 
	 * @param cfg        the target of the fixpoint
	 * @param token      the scope identifier that identifies the computation
	 * @param entryState the entry state for the fixpoint
	 * 
	 * @return the result of the fixpoint computation
	 * 
	 * @throws FixpointException if the fixpoint terminates abruptly
	 * @throws SemanticException if an exception happens while storing the
	 *                               result of the fixpoint
	 */
	protected AnalyzedCFG<A> computeFixpoint(
			CFG cfg,
			CallStackId<A> token,
			AnalysisState<A> entryState)
			throws FixpointException,
			SemanticException {
		if (events != null)
			events.post(new CFGFixpointStart<>(cfg, token, entryState));

		Map<CFGCall, AnalysisState<A>> recursiveCalls = new HashMap<>();
		recursionResults.addLast(recursiveCalls);
		AnalyzedCFG<A> fixpointResult = cfg.fixpoint(entryState, this, workingSet.mk(), conf, token);
		recursionResults.removeLast();

		if (conf.usesOptimizedForwardFixpoint() && !recursiveCalls.isEmpty())
			// as the fixpoint results do not contain an explicit entry for the
			// recursive call, we need to store the approximation for the
			// recursive call manually or the unwinding won't manage to solve it
			for (Entry<CFGCall, AnalysisState<A>> entry : recursiveCalls.entrySet()) {
				// we get the cfg containing the call
				@SuppressWarnings("unchecked")
				OptimizedAnalyzedCFG<A, D> fixRes = (OptimizedAnalyzedCFG<A, D>) fixpointResult;

				// we get the actual call that is part of the cfg
				Call source = entry.getKey();
				while (source.getSource() != null)
					source = source.getSource();

				// it might happen that the call is a hotspot and we don't need
				// any additional work
				if (!fixRes.hasPostStateOf(source))
					// finally, we store it in the result
					fixRes.storePostStateOf(source, entry.getValue());
			}

		if (events != null)
			events.post(new CFGFixpointEnd<>(cfg, token, entryState, fixpointResult));

		if (shouldStoreFixpointResults()) {
			Pair<Boolean, AnalyzedCFG<A>> res = results.putResult(cfg, token, fixpointResult);
			if (shouldStoreFixpointResults() && Boolean.TRUE.equals(res.getLeft()))
				throw new FixpointException("Inconsistent fixpoint result for " + cfg + " under token " + token);

			if (events != null)
				events.post(new CFGFixpointStored<>(cfg, token, entryState, fixpointResult, res.getRight()));

			fixpointResult = res.getRight();
		}

		return fixpointResult;
	}

	@Override
	public FixpointResults<A> getFixpointResults() {
		return results;
	}

	@Override
	public AnalysisState<A> getAbstractResultOf(
			CFGCall call,
			AnalysisState<A> entryState,
			ExpressionSet[] parameters,
			StatementStore<A> expressions)
			throws SemanticException {
		callgraph.registerCall(call);

		if (shouldCheckForRecursions() && maxCallStackDepth == token.size())
			if (shouldRaiseException)
				throw new SemanticException("Maximum call stack depth reached");
			else {
				Recursion<A> rec = buildRecursionFor(call, entryState, parameters, expressions);
				AnalysisState<
						A> result = new RecursionSolver<>(this, rec).solve(call, entryState, parameters, expressions);
				AnalysisState<A> prev = recursionResults.getLast().put(call, result);
				if (prev != null)
					throw new SemanticException("Inconsistent recursion result for " + call + " under token " + token);
				return result;
			}

		CallStackId<A> callerToken = token;
		token = token.push(call, CompoundState.of(entryState, expressions));
		ScopeToken scope = new ScopeToken(call);
		entries.put(call, CompoundState.of(entryState, expressions));

		// we exclude erroneous/halting executions from the
		// initial states, since they will not be affected
		// by the call; they are still part of the result
		// and they will be lubbed with the return values
		AnalysisState<A> result = entryState.bottomExecution();
		AnalysisState<A> initialState = entryState.removeAllErrors(true);

		// compute the result over all possible targets, and take the lub of
		// the results
		for (CFG cfg : call.getTargetedCFGs()) {
			CFGResults<A> localResults = results.get(cfg);
			AnalyzedCFG<A> states = localResults == null ? null : localResults.get(token);
			Pair<AnalysisState<A>,
					ExpressionSet[]> prepared = prepareEntryState(
							call,
							initialState,
							parameters,
							expressions,
							scope,
							cfg);

			if (events != null)
				events.post(new ComputedCallState<>(call, prepared.getLeft(), prepared.getRight()));

			AnalysisState<A> exitState;
			if (canShortcut(cfg) && states != null) {
				// no need to compute the fixpoint: we already have an
				// exact approximation of the result having the same
				// call stack and entry states
				exitState = states.getExitState();
				if (events != null)
					events.post(new PrecomputedCallResult<>(
							call,
							token,
							prepared.getLeft(),
							prepared.getRight(),
							exitState));
			} else {
				// compute the result with a fixpoint iteration
				AnalyzedCFG<A> fixpointResult = null;
				try {
					fixpointResult = computeFixpoint(cfg, token, prepared.getLeft());
				} catch (FixpointException e) {
					throw new SemanticException("Exception during the interprocedural analysis", e);
				}

				exitState = initialState.bottom();
				for (Statement exit : fixpointResult.getAllExitpoints())
					exitState = exitState.lub(
							analysis.removeCaughtErrors(
									fixpointResult.getAnalysisStateAfter(exit),
									exit));

				if (events != null)
					events.post(new ComputedCallResult<>(
							call,
							token,
							prepared.getLeft(),
							prepared.getRight(),
							exitState));
			}

			// save the resulting state
			ScopingStrategy strategy = call.getProgram().getFeatures().getScopingStrategy();
			AnalysisState<A> callres = strategy.unscope(call, scope, exitState, analysis);
			callres = analysis.transferThrowers(callres, call, cfg);
			callres = analysis.onCallReturn(entryState, callres, call);
			result = result.lub(callres);
		}

		token = callerToken;
		entries.remove(call);
		return result;
	}

	private Recursion<A> buildRecursionFor(
			CFGCall call,
			AnalysisState<A> entryState,
			ExpressionSet[] parameters,
			StatementStore<A> expressions)
			throws SemanticException {
		Collection<Collection<CodeMember>> recursions = callgraph.getRecursionsContaining(call.getCFG());
		if (recursions.isEmpty())
			throw new SemanticException("Maximum call stack depth reached and no recursion found for " + call);
		else if (recursions.size() > 1)
			throw new SemanticException("Multiple recursions found for " + call + ": " + recursions);
		Collection<CodeMember> members = recursions.iterator().next();

		Set<CFG> heads = new HashSet<>();
		for (CFG candidate : call.getTargetedCFGs())
			if (members.contains(candidate))
				heads.add(candidate);
		if (heads.isEmpty())
			throw new SemanticException("No recursion head found for " + call);
		else if (heads.size() > 1)
			throw new SemanticException("Multiple recursions heads for " + call + ": " + heads);
		CFG head = heads.iterator().next();

		return new Recursion<>(call, token, CompoundState.of(entryState, expressions), head, members);
	}

}
