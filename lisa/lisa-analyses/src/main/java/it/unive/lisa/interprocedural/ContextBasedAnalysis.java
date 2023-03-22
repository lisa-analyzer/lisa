package it.unive.lisa.interprocedural;

import it.unive.lisa.AnalysisExecutionException;
import it.unive.lisa.AnalysisSetupException;
import it.unive.lisa.DefaultParameters;
import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.AnalyzedCFG;
import it.unive.lisa.analysis.OptimizedAnalyzedCFG;
import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.analysis.value.TypeDomain;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.conf.FixpointConfiguration;
import it.unive.lisa.logging.IterationLogger;
import it.unive.lisa.logging.TimerLogger;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.Parameter;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.call.CFGCall;
import it.unive.lisa.program.language.parameterassignment.ParameterAssigningStrategy;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.util.collections.workset.VisitOnceFIFOWorkingSet;
import it.unive.lisa.util.collections.workset.WorkingSet;
import it.unive.lisa.util.datastructures.graph.algorithms.FixpointException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeSet;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A context sensitive interprocedural analysis. The context sensitivity is
 * tuned by the kind of {@link ContextSensitivityToken} used.
 * 
 * @param <A> the abstract state of the analysis
 * @param <H> the heap domain
 * @param <V> the value domain
 * @param <T> the type domain
 */
@DefaultParameters({ RecursionFreeToken.class })
public class ContextBasedAnalysis<A extends AbstractState<A, H, V, T>,
		H extends HeapDomain<H>,
		V extends ValueDomain<V>,
		T extends TypeDomain<T>>
		extends CallGraphBasedAnalysis<A, H, V, T> {

	private static final Logger LOG = LogManager.getLogger(ContextBasedAnalysis.class);

	private FixpointResults<A, H, V, T> results;

	private ContextSensitivityToken token;

	private Class<? extends WorkingSet<Statement>> fixpointWorkingSet;

	private FixpointConfiguration conf;

	private final Collection<CFG> fixpointTriggers;

	private final Map<CFGCall, Collection<ContextSensitivityToken>> callStacks;

	/**
	 * Builds the analysis, using {@link LastCallToken}s.
	 */
	public ContextBasedAnalysis() {
		this(LastCallToken.getSingleton());
	}

	/**
	 * Builds the analysis.
	 *
	 * @param token an instance of the tokens to be used to partition w.r.t.
	 *                  context sensitivity
	 */
	public ContextBasedAnalysis(ContextSensitivityToken token) {
		this.token = token.empty();
		fixpointTriggers = new HashSet<>();
		callStacks = new HashMap<>();
	}

	@Override
	public void fixpoint(
			AnalysisState<A, H, V, T> entryState,
			Class<? extends WorkingSet<Statement>> fixpointWorkingSet,
			FixpointConfiguration conf)
			throws FixpointException {
		this.fixpointWorkingSet = fixpointWorkingSet;
		this.conf = conf;
		// new fixpoint execution: reset
		this.results = null;

		if (app.getEntryPoints().isEmpty())
			throw new NoEntryPointException();

		TimerLogger.execAction(LOG, "Computing fixpoint over the whole program", () -> this.fixpointAux(entryState));
	}

	private static String ordinal(int i) {
		int n = i % 100;
		if (n == 11 || n == 12 || n == 13 || n % 10 == 0 || n % 10 > 3)
			return i + "th";

		if (n % 10 == 1)
			return i + "st";

		if (n % 10 == 2)
			return i + "nd";

		return i + "rd";
	}

	private void fixpointAux(AnalysisState<A, H, V, T> entryState) throws AnalysisExecutionException {
		int iter = 0;
		ContextSensitivityToken empty = token.empty();

		Collection<CFG> entryPoints = new TreeSet<>(
				(c1, c2) -> c1.getDescriptor().getLocation().compareTo(c2.getDescriptor().getLocation()));
		entryPoints.addAll(app.getEntryPoints());

		do {
			LOG.info("Performing {} fixpoint iteration", ordinal(iter + 1));
			fixpointTriggers.clear();
			for (CFG cfg : IterationLogger.iterate(LOG, entryPoints, "Processing entrypoints", "entries"))
				try {
					if (results == null) {
						AnalyzedCFG<A, H, V, T> graph = conf.optimize
								? new OptimizedAnalyzedCFG<>(cfg, empty, entryState.bottom(), this)
								: new AnalyzedCFG<>(cfg, empty, entryState);
						CFGResults<A, H, V, T> value = new CFGResults<>(graph);
						this.results = new FixpointResults<>(value.top());
					}

					AnalysisState<A, H, V, T> entryStateCFG = prepareEntryStateOfEntryPoint(entryState, cfg);
					results.putResult(cfg, empty,
							cfg.fixpoint(entryStateCFG, this, WorkingSet.of(fixpointWorkingSet), conf, empty));
				} catch (SemanticException | AnalysisSetupException e) {
					throw new AnalysisExecutionException("Error while creating the entrystate for " + cfg, e);
				} catch (FixpointException e) {
					throw new AnalysisExecutionException("Error while computing fixpoint for entrypoint " + cfg, e);
				}

			// starting from the callers of the cfgs that needed a lub,
			// find out the complete set of cfgs that might need to be
			// processed again
			VisitOnceFIFOWorkingSet<CFG> ws = VisitOnceFIFOWorkingSet.mk();
			fixpointTriggers.forEach(cfg -> callgraph.getCallers(cfg).stream().filter(CFG.class::isInstance)
					.map(CFG.class::cast).forEach(ws::push));
			while (!ws.isEmpty())
				callgraph.getCallers(ws.pop()).stream().filter(CFG.class::isInstance).map(CFG.class::cast)
						.forEach(ws::push);

			ws.getSeen().forEach(results::forget);

			iter++;
		} while (!fixpointTriggers.isEmpty());
	}

	@Override
	public Collection<AnalyzedCFG<A, H, V, T>> getAnalysisResultsOf(CFG cfg) {
		if (results.contains(cfg))
			return results.getState(cfg).getAll();
		else
			return Collections.emptySet();
	}

	/**
	 * Runs a fixpoint over the given {@link CFG}.
	 * 
	 * @param cfg        the target of the fixpoint
	 * @param localToken the scope identifier that identifies the computation
	 * @param entryState the entry state for the fixpoint
	 * 
	 * @return the result of the fixpoint computation
	 * 
	 * @throws FixpointException      if the fixpoint terminates abruptly
	 * @throws SemanticException      if an exception happens while storing the
	 *                                    result of the fixpoint
	 * @throws AnalysisSetupException if the {@link WorkingSet} for the fixpoint
	 *                                    cannot be created
	 */
	protected AnalyzedCFG<A, H, V, T> computeFixpoint(
			CFG cfg,
			ContextSensitivityToken localToken,
			AnalysisState<A, H, V, T> entryState)
			throws FixpointException, SemanticException, AnalysisSetupException {
		AnalyzedCFG<A, H, V, T> fixpointResult = cfg.fixpoint(
				entryState,
				this,
				WorkingSet.of(fixpointWorkingSet),
				conf,
				localToken);
		Pair<Boolean, AnalyzedCFG<A, H, V, T>> res = results.putResult(cfg, localToken, fixpointResult);
		if (Boolean.TRUE.equals(res.getLeft()))
			fixpointTriggers.add(cfg);
		return res.getRight();
	}

	@Override
	public FixpointResults<A, H, V, T> getFixpointResults() {
		return results;
	}

	/**
	 * Registers the given call stack for the given call.
	 * 
	 * @param c     the call that the stack is associated with
	 * @param stack the stack to register
	 * 
	 * @return whether or not the stack has been registered (if {@code false},
	 *             the stack already exists for the given call)
	 */
	protected boolean registerCallStack(CFGCall c, ContextSensitivityToken stack) {
		return callStacks.computeIfAbsent(c, tk -> new HashSet<>()).add(stack);
	}

	/**
	 * Removes the given stack from the ones registered for the given call.
	 * 
	 * @param c     the call that the stack is associated with
	 * @param stack the stack to unregister
	 */
	protected void unregisterCallStack(CFGCall c, ContextSensitivityToken stack) {
		Collection<ContextSensitivityToken> stacks = callStacks.get(c);
		if (stacks.size() == 1)
			callStacks.remove(c);
		else
			stacks.remove(stack);
	}

	/**
	 * Determines the result of the recursive call {@code call}.
	 * 
	 * @param call the recursive call
	 * 
	 * @return the result of the call
	 * 
	 * @throws SemanticException if something goes wrong during the computation
	 */
	protected AnalysisState<A, H, V, T> handleRecursion(CFGCall call) throws SemanticException {
		throw new SemanticException("Recursion found at '" + call + "' : " + token);
	}

	@Override
	public AnalysisState<A, H, V, T> getAbstractResultOf(
			CFGCall call,
			AnalysisState<A, H, V, T> entryState,
			ExpressionSet<SymbolicExpression>[] parameters,
			StatementStore<A, H, V, T> expressions)
			throws SemanticException {
		token = token.pushCall(call);
		if (!registerCallStack(call, token))
			// if we already reached this call with the same token, then
			// this is a recursion and we have to use a separate fixpoint
			// to compute its result
			return handleRecursion(call);

		ScopeToken scope = new ScopeToken(call);
		AnalysisState<A, H, V, T> result = entryState.bottom();
		for (CFG cfg : call.getTargetedCFGs()) {
			CFGResults<A, H, V, T> localResults = results.get(cfg);
			AnalyzedCFG<A, H, V, T> states = localResults == null ? null : localResults.get(token);
			Parameter[] formals = cfg.getDescriptor().getFormals();

			// prepare the state for the call: hide the visible variables
			Pair<AnalysisState<A, H, V, T>, ExpressionSet<SymbolicExpression>[]> scoped = scope(
					entryState,
					scope,
					parameters);
			AnalysisState<A, H, V, T> callState = scoped.getLeft();
			ExpressionSet<SymbolicExpression>[] locals = scoped.getRight();

			// assign parameters between the caller context and the callee
			// context
			ParameterAssigningStrategy strategy = call.getProgram().getFeatures().getAssigningStrategy();
			Pair<AnalysisState<A, H, V, T>, ExpressionSet<SymbolicExpression>[]> prepared = strategy.prepare(
					call,
					callState,
					this,
					expressions,
					formals,
					locals);

			AnalysisState<A, H, V, T> exitState;
			if (states != null && prepared.getLeft().lessOrEqual(states.getEntryState()))
				// no need to compute the fixpoint: we already have an
				// (over-)approximation of the result computed starting from an
				// over-approximation of the entry state
				exitState = states.getExitState();
			else {
				// compute the result with a fixpoint iteration
				AnalyzedCFG<A, H, V, T> fixpointResult = null;
				try {
					fixpointResult = computeFixpoint(cfg, token, prepared.getLeft());
				} catch (FixpointException | AnalysisSetupException e) {
					throw new SemanticException("Exception during the interprocedural analysis", e);
				}

				exitState = fixpointResult.getExitState();
			}

			// save the resulting state
			result = result.lub(unscope(call, scope, exitState));
		}

		unregisterCallStack(call, token);
		token = token.popCall(call);
		callgraph.registerCall(call);

		return result;
	}
}
