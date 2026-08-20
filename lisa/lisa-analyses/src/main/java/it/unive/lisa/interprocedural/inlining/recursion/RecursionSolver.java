package it.unive.lisa.interprocedural.inlining.recursion;

import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.conf.FixpointConfiguration;
import it.unive.lisa.events.EventQueue;
import it.unive.lisa.interprocedural.InterproceduralAnalysisException;
import it.unive.lisa.interprocedural.OpenCallPolicy;
import it.unive.lisa.interprocedural.Recursion;
import it.unive.lisa.interprocedural.callgraph.CallGraph;
import it.unive.lisa.interprocedural.inlining.CallStackId;
import it.unive.lisa.interprocedural.inlining.InliningAnalysis;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.lattices.GenericMapLattice;
import it.unive.lisa.program.Application;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.fixpoints.CompoundState;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.call.CFGCall;
import it.unive.lisa.program.cfg.statement.call.Call;
import it.unive.lisa.program.language.scoping.ScopingStrategy;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.PushInv;
import it.unive.lisa.util.StringUtilities;
import it.unive.lisa.util.datastructures.graph.algorithms.FixpointException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A recursion solver that applies the iterates of the recursion starting from
 * bottom. This solver operates by restarting the recursion from
 * {@link Recursion#getInvocation()} a number of times, until the results of all
 * the members stabilize.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <A> the kind of {@link AbstractLattice} produced by the domain
 *                {@code D}
 * @param <D> the kind of {@link AbstractDomain} to run during the analysis
 */
public class RecursionSolver<A extends AbstractLattice<A>,
		D extends AbstractDomain<A>>
		extends
		InliningAnalysis<A, D> {

	private static final Logger LOG = LogManager.getLogger(RecursionSolver.class);

	private final Recursion<A> recursion;

	private final boolean returnsVoid;

	private final Map<CFGCall, Pair<AnalysisState<A>, CallStackId<A>>> finalEntryStates;

	private final BaseCasesFinder<A, D> baseCases;

	private GenericMapLattice<CFGCall, AnalysisState<A>> previousApprox;

	private GenericMapLattice<CFGCall, AnalysisState<A>> recursiveApprox;

	private AnalysisState<A> base;

	/**
	 * Builds the solver.
	 * 
	 * @param backing   the analysis that backs this solver, and that can be
	 *                      used to query call results
	 * @param recursion the recursion to solve
	 */
	public RecursionSolver(
			InliningAnalysis<A, D> backing,
			Recursion<A> recursion) {
		super(backing);
		this.recursion = recursion;
		finalEntryStates = new HashMap<>();
		// the return value of each back call must be the same as the one
		// starting the recursion, as they invoke the same cfg
		returnsVoid = recursion.getInvocation().returnsVoid(null);
		baseCases = new BaseCasesFinder<>(backing, recursion, returnsVoid);
	}

	@Override
	public void init(
			Application app,
			CallGraph callgraph,
			OpenCallPolicy policy,
			EventQueue events,
			Analysis<A, D> analysis)
			throws InterproceduralAnalysisException {
		// we mark this as unsupported to make sure it never gets used as a root
		// analysis
		throw new UnsupportedOperationException();
	}

	@Override
	public void fixpoint(
			AnalysisState<A> entryState,
			FixpointConfiguration<A, D> conf)
			throws FixpointException {
		// we mark this as unsupported to make sure it never gets used as a root
		// analysis
		throw new UnsupportedOperationException();
	}

	@Override
	public AnalysisState<A> getAbstractResultOf(
			CFGCall call,
			AnalysisState<A> entryState,
			ExpressionSet[] parameters,
			StatementStore<A> expressions)
			throws SemanticException {
		boolean inRecursion = recursion.getMembers().contains(call.getCFG());
		if (inRecursion && call.getTargetedCFGs().contains(recursion.getRecursionHead())) {
			// this is a back call
			finalEntryStates.put(call, Pair.of(entryState, token));

			AnalysisState<A> approx = null;
			if (recursiveApprox.getMap() != null)
				approx = recursiveApprox.getMap().get(call);
			if (approx == null)
				// no state: we must start with the base cases
				approx = transferToCallsite(recursion.getInvocation(), call, base);
			// we bring in the entry state to carry over the correct scope
			AnalysisState<A> res = approx.lub(entryState);
			Identifier meta = call.getMetaVariable();
			if (!res.getExecutionState().knowsIdentifier(meta)) {
				// if we have no information for the return value, we want to
				// force it to bottom as it means that this is either the first
				// execution (that must start from bottom) or that the recursion
				// diverges
				PushInv inv = new PushInv(meta.getStaticType(), call.getLocation());
				res = analysis.assign(res, meta, inv, call);
			}
			return res;
		}
		return super.getAbstractResultOf(call, entryState, parameters, expressions);
	}

	@Override
	protected boolean canShortcut(
			CFG cfg) {
		// we want to compute the recursive chain with no shortcuts
		return !recursion.getMembers().contains(cfg);
	}

	@Override
	protected boolean shouldCheckForRecursions() {
		return false;
	}

	@Override
	protected boolean shouldStoreFixpointResults() {
		return false;
	}

	/**
	 * Solves the recursion by applying its iterates starting from bottom.
	 *
	 * @param call      the call that is being solved
	 * @param callEntry the entry state of the call
	 * 
	 * @return the state returned by the recursive chain
	 * 
	 * @throws SemanticException if an exception happens during the computation
	 */
	public AnalysisState<A> solve(
			CFGCall call,
			AnalysisState<A> callEntry)
			throws SemanticException {
		int recursionCount = 0;
		Call start = recursion.getInvocation();
		Collection<CFGCall> ends = finalEntryStates.keySet();
		CompoundState<A> entryState = recursion.getEntryState();

		LOG.info("Solving recursion at " + start.getLocation() + " for context " + recursion.getInvocationToken());

		recursiveApprox = new GenericMapLattice<CFGCall, AnalysisState<A>>(entryState.postState).bottom();
		base = baseCases.find();

		Expression[] actuals = start.getParameters();
		ExpressionSet[] params = new ExpressionSet[actuals.length];
		for (int i = 0; i < params.length; i++)
			params[i] = entryState.intermediateStates.getState(actuals[i]).getExecutionExpressions();

		do {
			LOG.debug(
					StringUtilities.ordinal(recursionCount + 1)
							+ " evaluation of recursive chain at "
							+ start.getLocation());

			previousApprox = recursiveApprox;

			// we reset the analysis at the point where the starting call can be
			// evaluated
			token = (CallStackId<A>) recursion.getInvocationToken();
			AnalysisState<A> post = start
					.forwardSemanticsAux(this, entryState.postState, params, entryState.intermediateStates);

			for (CFGCall end : ends)
				// no need to lub: the keys are the calls and
				// are thus unique
				recursiveApprox = recursiveApprox.putState(end, transferToCallsite(start, end, post));

			if (conf.recursionWideningThreshold < 0)
				recursiveApprox = previousApprox.lub(recursiveApprox);
			else if (recursionCount == conf.recursionWideningThreshold)
				recursiveApprox = previousApprox.widening(recursiveApprox);
			else {
				recursionCount++;
				recursiveApprox = previousApprox.lub(recursiveApprox);
			}
		} while (!recursiveApprox.lessOrEqual(previousApprox));

		// we exclude erroneous/halting executions from the
		// initial states, since they will not be affected
		// by the call; they are still part of the result
		// and they will be lubbed with the return values
		AnalysisState<A> result = entryState.postState.bottomExecution();
		for (CFG cfg : call.getTargetedCFGs()) {
			AnalysisState<A> exitState = recursiveApprox.getState(call);
			ScopeToken scope = new ScopeToken(call);
			ScopingStrategy strategy = call.getProgram().getFeatures().getScopingStrategy();
			AnalysisState<A> callres = strategy.unscope(call, scope, exitState, analysis);
			AnalysisState<A> local = transferToCallsite(start, call, base);
			AnalysisState<A> returned = callEntry.lub(callres.lub(local));
			Identifier meta = call.getMetaVariable();
			if (!returned.getExecutionState().knowsIdentifier(meta)) {
				// if we have no information for the return value, we
				// want to force it to bottom as it means that this is either
				// the first execution (that must start from bottom) or that
				// the recursion diverges
				PushInv inv = new PushInv(meta.getStaticType(), call.getLocation());
				returned = analysis.assign(returned, meta, inv, call);
			}
			returned = analysis.transferThrowers(returned, call, cfg);
			returned = analysis.onCallReturn(entryState.postState, returned, call);
			result = result.lub(returned);
		}

		return result;
	}

	private AnalysisState<A> transferToCallsite(
			Call original,
			CFGCall destination,
			AnalysisState<A> state)
			throws SemanticException {
		AnalysisState<A> res = state.bottom();
		Identifier meta = destination.getMetaVariable();
		if (returnsVoid)
			res = state;
		else
			for (Identifier variable : original.getMetaVariables())
				// we transfer the return value
				res = res.lub(analysis.assign(state, meta, variable, original));

		if (!res.getExecutionState().knowsIdentifier(meta)) {
			// if we have no information for the return value, we want to
			// force it to bottom as it means that this is either the first
			// execution (that must start from bottom) or that the recursion
			// diverges
			PushInv inv = new PushInv(meta.getStaticType(), destination.getLocation());
			res = analysis.assign(res, meta, inv, destination);
		}

		// we only keep variables that can be affected by the recursive
		// chain: the whole recursion return value, and all variables
		// that are not sensible to scoping. All other variables are
		// subjected to push and pop operations and cannot be
		// considered a contribution of the recursive call.
		res = res.forgetIdentifiersIf(i -> i.canBeScoped() && !i.equals(meta), original);
		return res;
	}

}
