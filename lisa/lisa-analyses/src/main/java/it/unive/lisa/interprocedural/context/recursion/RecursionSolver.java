package it.unive.lisa.interprocedural.context.recursion;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.OptimizedAnalyzedCFG;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.analysis.lattices.GenericMapLattice;
import it.unive.lisa.conf.FixpointConfiguration;
import it.unive.lisa.interprocedural.InterproceduralAnalysisException;
import it.unive.lisa.interprocedural.OpenCallPolicy;
import it.unive.lisa.interprocedural.callgraph.CallGraph;
import it.unive.lisa.interprocedural.context.ContextBasedAnalysis;
import it.unive.lisa.interprocedural.context.ContextSensitivityToken;
import it.unive.lisa.program.Application;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.fixpoints.CFGFixpoint.CompoundState;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.call.CFGCall;
import it.unive.lisa.program.cfg.statement.call.Call;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.PushInv;
import it.unive.lisa.util.StringUtilities;
import it.unive.lisa.util.collections.workset.WorkingSet;
import it.unive.lisa.util.datastructures.graph.algorithms.FixpointException;

/**
 * A recursion solver that applies the iterates of the recursion starting from
 * bottom. This solver operates by restarting the recursion from
 * {@link Recursion#getInvocation()} a number of times, until the results of all
 * the members stabilize.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <A> the type of {@link AbstractState} contained into the analysis
 *                state
 */
public class RecursionSolver<A extends AbstractState<A>> extends ContextBasedAnalysis<A> {

	private static final Logger LOG = LogManager.getLogger(RecursionSolver.class);

	private final Recursion<A> recursion;

	private final boolean returnsVoid;

	private final Map<CFGCall, Pair<AnalysisState<A>, ContextSensitivityToken>> finalEntryStates;

	private final BaseCasesFinder<A> baseCases;

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
	public RecursionSolver(ContextBasedAnalysis<A> backing, Recursion<A> recursion) {
		super(backing);
		this.recursion = recursion;
		finalEntryStates = new HashMap<>();
		// the return value of each back call must be the same as the one
		// starting the recursion, as they invoke the same cfg
		returnsVoid = returnsVoid(recursion.getInvocation(), null);
		baseCases = new BaseCasesFinder<>(backing, recursion, returnsVoid);
	}

	@Override
	public void init(
			Application app,
			CallGraph callgraph,
			OpenCallPolicy policy)
			throws InterproceduralAnalysisException {
		// we mark this as unsupported to make sure it never gets used as a root
		// analysis
		throw new UnsupportedOperationException();
	}

	@Override
	public void fixpoint(
			AnalysisState<A> entryState,
			Class<? extends WorkingSet<Statement>> fixpointWorkingSet,
			FixpointConfiguration conf)
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
			if (!res.getState().knowsIdentifier(meta)) {
				// if we have no information for the return value, we want to
				// force it to bottom as it means that this is either the first
				// execution (that must start from bottom) or that the recursion
				// diverges
				PushInv inv = new PushInv(meta.getStaticType(), call.getLocation());
				res = res.assign(meta, inv, call);
			}
			return res;
		}
		return super.getAbstractResultOf(call, entryState, parameters, expressions);
	}

	@Override
	protected boolean canShortcut(CFG cfg) {
		// we want to compute the recursive chain with no shortcuts
		return !recursion.getMembers().contains(cfg);
	}

	@Override
	protected boolean shouldCheckForRecursions() {
		return false;
	}

	/**
	 * Solves the recursion by applying its iterates starting from bottom.
	 * 
	 * @throws SemanticException if an exception happens during the computation
	 */
	public void solve() throws SemanticException {
		int recursionCount = 0;
		Call start = recursion.getInvocation();
		Collection<CFGCall> ends = finalEntryStates.keySet();
		CompoundState<A> entryState = recursion.getEntryState();

		LOG.info("Solving recursion at " + start.getLocation() + " for context " + recursion.getInvocationToken());

		recursiveApprox = new GenericMapLattice<>(entryState.postState.bottom());
		recursiveApprox = recursiveApprox.bottom();
		base = baseCases.find();

		Expression[] actuals = start.getParameters();
		ExpressionSet[] params = new ExpressionSet[actuals.length];
		for (int i = 0; i < params.length; i++)
			params[i] = entryState.intermediateStates.getState(actuals[i]).getComputedExpressions();

		do {
			LOG.debug(StringUtilities.ordinal(recursionCount + 1)
					+ " evaluation of recursive chain at "
					+ start.getLocation());

			previousApprox = recursiveApprox;

			// we reset the analysis at the point where the starting call can be
			// evaluated
			token = recursion.getInvocationToken();
			AnalysisState<A> post = start.expressionSemantics(
					this,
					entryState.postState,
					params,
					entryState.intermediateStates);

			for (CFGCall end : ends)
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

		if (conf.optimize)
			// as the fixpoint results do not contain an explicit entry for the
			// recursive call, we need to store the approximation for the
			// recursive call manually or the unwinding won't manage to solve it
			for (CFGCall call : ends) {
				Pair<AnalysisState<A>, ContextSensitivityToken> pair = finalEntryStates.get(call);
				AnalysisState<A> callEntry = pair.getLeft();
				ContextSensitivityToken callingToken = pair.getRight();

				// we get the cfg containing the call
				OptimizedAnalyzedCFG<A> caller = (OptimizedAnalyzedCFG<A>) results.get(call.getCFG())
						.get(callingToken);

				// we get the actual call that is part of the cfg
				Call source = call;
				while (source.getSource() != null)
					source = source.getSource();

				// it might happen that the call is a hotspot and we don't need
				// any additional work
				if (!caller.hasPostStateOf(source)) {
					// we add the value to the entry state, bringing in also the
					// base case
					AnalysisState<A> local = transferToCallsite(start, call, base);
					AnalysisState<A> returned = callEntry.lub(recursiveApprox.getState(call).lub(local));

					// finally, we store it in the result
					caller.storePostStateOf(source, returned);
				}
			}
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
				res = res.lub(state.assign(meta, variable, original));

		if (!res.getState().knowsIdentifier(meta)) {
			// if we have no information for the return value, we want to
			// force it to bottom as it means that this is either the first
			// execution (that must start from bottom) or that the recursion
			// diverges
			PushInv inv = new PushInv(meta.getStaticType(), destination.getLocation());
			res = res.assign(meta, inv, destination);
		}

		// we only keep variables that can be affected by the recursive
		// chain: the whole recursion return value, and all variables
		// that are not sensible to scoping. All other variables are
		// subjected to push and pop operations and cannot be
		// considered a contribution of the recursive call.
		res = res.forgetIdentifiersIf(i -> i.canBeScoped() && !i.equals(meta));
		return res;
	}
}
