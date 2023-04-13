package it.unive.lisa.interprocedural.context;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
import it.unive.lisa.interprocedural.CFGResults;
import it.unive.lisa.interprocedural.CallGraphBasedAnalysis;
import it.unive.lisa.interprocedural.FixpointResults;
import it.unive.lisa.interprocedural.NoEntryPointException;
import it.unive.lisa.interprocedural.context.recursion.Recursion;
import it.unive.lisa.interprocedural.context.recursion.RecursionNode;
import it.unive.lisa.logging.IterationLogger;
import it.unive.lisa.logging.TimerLogger;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeMember;
import it.unive.lisa.program.cfg.Parameter;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.call.CFGCall;
import it.unive.lisa.program.language.parameterassignment.ParameterAssigningStrategy;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.util.collections.workset.WorkingSet;
import it.unive.lisa.util.datastructures.graph.algorithms.FixpointException;

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

	private final Collection<CodeMember> triggers;

	private final Set<Recursion<A, H, V, T>> recursions;

	private final Map<CFGCall, AnalysisState<A, H, V, T>> ongoingCalls;

	private FixpointResults<A, H, V, T> results;

	private Class<? extends WorkingSet<Statement>> workingSet;

	private ContextSensitivityToken token;

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
		this.token = token;
		triggers = new HashSet<>();
		recursions = new HashSet<>();
		ongoingCalls = new IdentityHashMap<>();
	}

	@Override
	public void fixpoint(
			AnalysisState<A, H, V, T> entryState,
			Class<? extends WorkingSet<Statement>> fixpointWorkingSet,
			FixpointConfiguration conf)
			throws FixpointException {
		this.workingSet = fixpointWorkingSet;
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
		ContextSensitivityToken empty = (ContextSensitivityToken) token.startingId();

		Collection<CFG> entryPoints = new TreeSet<>(
				(c1, c2) -> c1.getDescriptor().getLocation().compareTo(c2.getDescriptor().getLocation()));
		entryPoints.addAll(app.getEntryPoints());

		do {
			LOG.info("Performing {} fixpoint iteration", ordinal(iter + 1));
			triggers.clear();
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
							cfg.fixpoint(entryStateCFG, this, WorkingSet.of(workingSet), conf, empty));
				} catch (SemanticException | AnalysisSetupException e) {
					throw new AnalysisExecutionException("Error while creating the entrystate for " + cfg, e);
				} catch (FixpointException e) {
					throw new AnalysisExecutionException("Error while computing fixpoint for entrypoint " + cfg, e);
				}

			if (!recursions.isEmpty()) {
				Set<Recursion<A, H, V, T>> compacted = new HashSet<>();

				for (Recursion<A, H, V, T> recursion : recursions) {
					Set<Recursion<A, H, V, T>> eqclass = new HashSet<>();
					eqclass.add(recursion);
					for (Recursion<A, H, V, T> rec2 : recursions)
						if (!recursion.equals(rec2) && recursion.equalsUpToCalls(rec2))
							eqclass.add(rec2);
					recursions.removeAll(eqclass);
					if (eqclass.size() == 1)
						compacted.add(eqclass.iterator().next());
					else {
						Recursion<A, H, V, T> compact = null;
						for (Recursion<A, H, V, T> rr : eqclass)
							if (compact == null)
								compact = rr;
							else
								compact = compact.merge(rr);
						compacted.add(compact);
					}
				}

				for (Recursion<A, H, V, T> rec : compacted) {
					// TODO solve the recursions here
					triggers.addAll(rec.getInvolvedCFGs());
				}

				recursions.clear();
			}

			// starting from the callers of the cfgs that needed a lub,
			// find out the complete set of cfgs that might need to be
			// processed again
			Collection<CodeMember> toRemove = callgraph.getCallersTransitively(triggers);
			toRemove.removeAll(triggers);
			toRemove.stream().filter(CFG.class::isInstance).map(CFG.class::cast).forEach(results::forget);

			iter++;
		} while (!triggers.isEmpty());
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
	private AnalyzedCFG<A, H, V, T> computeFixpoint(
			CFG cfg,
			ContextSensitivityToken token,
			AnalysisState<A, H, V, T> entryState)
			throws FixpointException, SemanticException, AnalysisSetupException {
		AnalyzedCFG<A, H, V, T> fixpointResult = cfg.fixpoint(
				entryState,
				this,
				WorkingSet.of(workingSet),
				conf,
				token);
		Pair<Boolean, AnalyzedCFG<A, H, V, T>> res = results.putResult(cfg, token, fixpointResult);
		if (Boolean.TRUE.equals(res.getLeft()))
			// recursion members will be all added at the end of the recursion
			triggers.add(cfg);
		return res.getRight();
	}

	@Override
	public FixpointResults<A, H, V, T> getFixpointResults() {
		return results;
	}

	private void computeRecursionsEndingWith(CFGCall call) throws SemanticException {
		for (Collection<CodeMember> rec : callgraph.getRecursionsContaining(call.getCFG())) {
			Collection<CodeMember> remaining = new HashSet<>(rec);
			List<RecursionNode> nodes = new LinkedList<>();
			CFG iterator = call.getCFG();

			// we begin from the tail of the recursion
			nodes.add(new RecursionNode(call, iterator));
			remaining.remove(iterator);

			while (!remaining.isEmpty()) {
				Set<CFGCall> sites = callgraph.getCallSites(iterator).stream()
						.filter(site -> remaining.contains(site.getCFG()))
						.filter(CFGCall.class::isInstance)
						.map(CFGCall.class::cast)
						.collect(Collectors.toSet());

				if (sites.isEmpty())
					throw new SemanticException("Recursion with no valid entry point found");
				else {
					iterator = sites.iterator().next().getCFG();
					for (CFGCall site : sites)
						if (!site.getCFG().equals(iterator))
							throw new SemanticException("Recursion with non-linear path found");

					nodes.add(new RecursionNode(sites, iterator));
					remaining.remove(iterator);
				}
			}

			// iterator will now contain the entry node of the recursion
			Set<CFGCall> sites = callgraph.getCallSites(iterator).stream()
					.filter(site -> !rec.contains(site.getCFG()))
					.filter(CFGCall.class::isInstance)
					.filter(ongoingCalls::containsKey)
					.map(CFGCall.class::cast)
					.collect(Collectors.toSet());
			for (CFGCall site : sites)
				recursions.add(new Recursion<>(site, nodes, ongoingCalls.get(site)));
		}
	}

	@Override
	public AnalysisState<A, H, V, T> getAbstractResultOf(
			CFGCall call,
			AnalysisState<A, H, V, T> entryState,
			ExpressionSet<SymbolicExpression>[] parameters,
			StatementStore<A, H, V, T> expressions)
			throws SemanticException {
		callgraph.registerCall(call);
		ContextSensitivityToken callerToken = token;
		token = token.push(call);
		ongoingCalls.put(call, entryState);

		if (call.getTargetedCFGs().stream().anyMatch(call.getCFG()::equals)
				|| callgraph.getCalleesTransitively(call.getTargets()).contains(call.getCFG())) {
			// this calls introduces a loop in the call graph -> recursion
			// we need a special fixpoint to compute its result
			// we compute that at the end of each fixpoint iteration
			computeRecursionsEndingWith(call);
			// we return bottom for now
			return entryState.bottom();
		}

		ScopeToken scope = new ScopeToken(call);
		AnalysisState<A, H, V, T> result = entryState.bottom();

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
			if (states != null && prepared.getLeft().lessOrEqual(states.getEntryState()))
				// no need to compute the fixpoint: we already have an
				// (over-)approximation of the result computed starting from
				// an over-approximation of the entry state
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

		ongoingCalls.remove(call);
		token = callerToken;
		return result;
	}
}
