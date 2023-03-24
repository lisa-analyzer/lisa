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
import it.unive.lisa.util.collections.workset.VisitOnceWorkingSet;
import it.unive.lisa.util.collections.workset.WorkingSet;
import it.unive.lisa.util.datastructures.graph.algorithms.FixpointException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
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

	private final ContextSensitivityToken tokenCreator;

	private final Collection<CFG> fixpointTriggers;

	private final LinkedList<Pair<CFGCall, ContextSensitivityToken>> callStack;

	private FixpointResults<A, H, V, T> results;

	private Class<? extends WorkingSet<Statement>> fixpointWorkingSet;

	private FixpointConfiguration conf;

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
		this.tokenCreator = token;
		fixpointTriggers = new HashSet<>();
		callStack = new LinkedList<>();
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
		ContextSensitivityToken empty = (ContextSensitivityToken) tokenCreator.startingId();

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
			VisitOnceWorkingSet<CFG> ws = VisitOnceFIFOWorkingSet.mk();
			fixpointTriggers.forEach(cfg -> callgraph.getCallers(cfg)
					.stream()
					.filter(CFG.class::isInstance)
					.map(CFG.class::cast)
					.forEach(ws::push));
			while (!ws.isEmpty())
				callgraph.getCallers(ws.pop())
						.stream()
						.filter(CFG.class::isInstance)
						.map(CFG.class::cast)
						.forEach(ws::push);

			Collection<CFG> seen = ws.getSeen();
			seen.removeAll(fixpointTriggers);
			seen.forEach(results::forget);

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
	 * @param token      the scope identifier that identifies the computation
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
			ContextSensitivityToken token,
			AnalysisState<A, H, V, T> entryState)
			throws FixpointException, SemanticException, AnalysisSetupException {
		AnalyzedCFG<A, H, V, T> fixpointResult = cfg.fixpoint(
				entryState,
				this,
				WorkingSet.of(fixpointWorkingSet),
				conf,
				token);
		Pair<Boolean, AnalyzedCFG<A, H, V, T>> res = results.putResult(cfg, token, fixpointResult);
		if (recursionStart == -1 && Boolean.TRUE.equals(res.getLeft()))
			// avoid re-evaluating the triggers until the recursion is complete
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
	 * @return {@code -1} if the call has been added to the stack, otherwise the
	 *             stack is not modified and the position of the same
	 *             {@code <c, stack>} pair is returned
	 */
	protected int registerCallStack(CFGCall c, ContextSensitivityToken stack) {
		Pair<CFGCall, ContextSensitivityToken> entry = Pair.of(c, stack);
		int last = callStack.lastIndexOf(entry);
		if (last != -1)
			return last;
		callStack.addLast(entry);
		return -1;
	}

	/**
	 * Removes the given stack from the ones registered for the given call.
	 * 
	 * @param c     the call that the stack is associated with
	 * @param stack the stack to unregister
	 * 
	 * @throws SemanticException if the top of the call stack does not match the
	 *                               {@code <c, stack>} pair
	 */
	protected void unregisterCallStack(CFGCall c, ContextSensitivityToken stack) throws SemanticException {
		Pair<CFGCall, ContextSensitivityToken> last = callStack.removeLast();
		if (!last.equals(Pair.of(c, stack)))
			throw new SemanticException("Top of the call stack ('"
					+ last.getLeft()
					+ "' with stack hash "
					+ last.getRight().hashCode()
					+ ") does not match the call that is returning ("
					+ c
					+ " with stack hash "
					+ stack.hashCode()
					+ ")");
	}

	private AnalysisState<A, H, V, T> recursiveApprox, previousApprox;
	private int recursionCount, recursionStart = -1;

	/**
	 * Determines the result of the recursive call {@code call}.
	 * 
	 * @param recPos the position in the call stack of the call that started the
	 *                   recursion
	 * @param call   the recursive call
	 * @param state  a singleton instance of the analysis state
	 * @param token  the token causing the recursion
	 * 
	 * @return the result of the call
	 * 
	 * @throws SemanticException if something goes wrong during the computation
	 */
	protected AnalysisState<A, H, V, T> handleRecursion(
			int recPos,
			CFGCall call,
			AnalysisState<A, H, V, T> state,
			ContextSensitivityToken token)
			throws SemanticException {
		if (recursionStart == -1) {
			LOG.info("Found recursion at '" + call.getLocation() + "' with token " + token);
			recursionStart = recPos;
			if (returnsVoid(call, null))
				recursiveApprox = state.bottom();
			else
				recursiveApprox = new AnalysisState<>(
						state.getState().bottom(),
						call.getMetaVariable(),
						state.getAliasing().bottom());
			recursionCount = 0;
		} else {
			LOG.info(ordinal(recursionCount + 2) + " evaluation of recursive chain at '" + call.getLocation());
			if (conf.wideningThreshold < 0)
				recursiveApprox = previousApprox.lub(recursiveApprox);

			if (recursionCount == conf.wideningThreshold)
				recursiveApprox = previousApprox.widening(recursiveApprox);
			else {
				recursionCount++;
				recursiveApprox = previousApprox.lub(recursiveApprox);
			}
		}

		AnalysisState<A, H, V, T> approx = recursiveApprox;
		if (previousApprox != null && recursiveApprox.lessOrEqual(previousApprox)) {
			recursionStart = -1;
			recursiveApprox = previousApprox = null;
			recursionCount = 0;
		}
		return approx;
	}

	@Override
	public AnalysisState<A, H, V, T> getAbstractResultOf(
			CFGCall call,
			AnalysisState<A, H, V, T> entryState,
			ExpressionSet<SymbolicExpression>[] parameters,
			StatementStore<A, H, V, T> expressions)
			throws SemanticException {
		ContextSensitivityToken token = tokenCreator.pushOnFullStack(callStack, call);
		int recPos = registerCallStack(call, token);
		if (recPos != -1)
			// if we already reached this call with the same token, then
			// this is a recursion and we have to use a separate fixpoint
			// to compute its result
			return handleRecursion(recPos, call, entryState, token);

		ScopeToken scope = new ScopeToken(call);
		AnalysisState<A, H, V, T> result = entryState.bottom();

		boolean isActiveRecursionHead;
		do {
			// compute the result over all possible targets, and take the lub of
			// the results
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

				// assign parameters between the caller and the callee contexts
				ParameterAssigningStrategy strategy = call.getProgram().getFeatures().getAssigningStrategy();
				Pair<AnalysisState<A, H, V, T>, ExpressionSet<SymbolicExpression>[]> prepared = strategy.prepare(
						call,
						callState,
						this,
						expressions,
						formals,
						locals);

				AnalysisState<A, H, V, T> exitState;
				if (recursionStart == -1 && states != null && prepared.getLeft().lessOrEqual(states.getEntryState()))
					// no need to compute the fixpoint: we already have an
					// (over-)approximation of the result computed starting from
					// an over-approximation of the entry state
					// note that we skip this entirely if we are evaluating a
					// recursive chain, as we don't want to interfere with the
					// fixpoint computation, as we must reach the recursion
					// point every time without short-cutting to the result
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

			// here, the top of call-stack will be <call, token>
			// we exit if:
			// - recursionStart == -1 (no active recursion)
			// - recursionStart != callStack.head.position
			// otherwise, this is the exact call that starts the
			// recursion, and we have to repeat the fixpoint
			isActiveRecursionHead = recursionStart != -1 && callStack.size() - 1 == recursionStart;

			if (isActiveRecursionHead) {
				// store the result of this computation to use it at the start
				// of the next fixpoint iteration
				previousApprox = recursiveApprox;
				recursiveApprox = result;
			}
		} while (isActiveRecursionHead);

		unregisterCallStack(call, token);
		callgraph.registerCall(call);

		return result;
	}
}
