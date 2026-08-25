package it.unive.lisa.interprocedural.inlining.recursion;

import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.AnalyzedCFG;
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
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.util.StringUtilities;
import it.unive.lisa.util.datastructures.graph.algorithms.FixpointException;
import java.util.Collection;
import java.util.HashSet;
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

	private final Collection<CFGCall> backCalls;

	private final BaseCasesFinder<A, D> baseCases;

	private GenericMapLattice<CFGCall, AnalysisState<A>> previousApprox;

	private GenericMapLattice<CFGCall, AnalysisState<A>> recursiveApprox;

	private AnalysisState<A> base;

	private AnalysisState<A> previousEntry;

	private AnalysisState<A> recursiveEntry;

	private int recursionCount;

	private boolean committingSummary;

	private boolean recursionKickOff;

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
		backCalls = new HashSet<>();
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
		if (!recursionKickOff && inRecursion && call.getTargetedCFGs().contains(recursion.getRecursionHead())) {
			backCalls.add(call);
			for (CFG target : call.getTargetedCFGs()) {
				ScopeToken scope = new ScopeToken(call);
				AnalysisState<A> entry = prepareEntryState(call, entryState, parameters, expressions, scope, target)
						.getLeft();
				// we are interested in lub/widen only the call parameters
				recursiveEntry = recursiveEntry.lub(
						entry.forgetIdentifiersIf(i -> !(i instanceof Variable), call));
			}

			AnalysisState<A> approx = null;
			if (previousApprox.getMap() != null)
				approx = previousApprox.getMap().get(call);
			if (approx == null)
				// no state: we must start with the base cases
				approx = transferToCallsite(recursion.getInvocation(), call, base);
			// we bring in the entry state to carry over the correct scope
			AnalysisState<A> res = approx.lub(entryState);
			Identifier meta = call.getMetaVariable();
			if (!returnsVoid && !res.getExecutionState().knowsIdentifier(meta)) {
				// if we have no information for the return value, we want to
				// force it to bottom as it means that this is either the first
				// execution (that must start from bottom) or that the recursion
				// diverges
				PushInv inv = new PushInv(meta.getStaticType(), call.getLocation());
				res = analysis.assign(res, meta, inv, call);
			}

			AnalysisState<A> prev = recursionResults.getLast().put(call, res);
			if (prev != null)
				throw new SemanticException("Inconsistent recursion result for " + call + " under token " + token);
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
		return committingSummary;
	}

	@Override
	protected AnalyzedCFG<A> computeFixpoint(
			CFG cfg,
			CallStackId<A> token,
			AnalysisState<A> entryState)
			throws FixpointException,
			SemanticException {
		if (recursionKickOff) {
			// we inject the widened entry state in the recursion head
			entryState = entryState.lub(previousEntry);
			recursionKickOff = false;
		}
		return super.computeFixpoint(cfg, token, entryState);
	}

	@Override
	protected Pair<AnalysisState<A>, ExpressionSet[]> prepareEntryState(
			CFGCall call,
			AnalysisState<A> entryState,
			ExpressionSet[] parameters,
			StatementStore<A> expressions,
			ScopeToken scope,
			CFG cfg)
			throws SemanticException {
		Pair<AnalysisState<A>, ExpressionSet[]> prepared = super.prepareEntryState(call, entryState, parameters,
				expressions, scope, cfg);
		if (recursionKickOff)
			prepared = Pair.of(prepared.getLeft().lub(previousEntry), prepared.getRight());
		return prepared;
	}

	/**
	 * Solves the recursion by applying its iterates starting from bottom.
	 *
	 * @param call        the call that is being solved
	 * @param callEntry   the entry state of the call
	 * @param parameters  the parameters of the call
	 * @param expressions the expressions store
	 * 
	 * @return the state returned by the recursive chain
	 * 
	 * @throws SemanticException if an exception happens during the computation
	 */
	public AnalysisState<A> solve(
			CFGCall call,
			AnalysisState<A> callEntry,
			ExpressionSet[] parameters,
			StatementStore<A> expressions)
			throws SemanticException {
		CallStackId<A> triggeringToken = token;

		recursionCount = 0;
		Call start = recursion.getInvocation();
		CompoundState<A> entryState = recursion.getEntryState();

		LOG.info("Solving recursion at " + start.getLocation() + " for context " + recursion.getInvocationToken());

		base = baseCases.find();

		Expression[] actuals = start.getParameters();
		ExpressionSet[] params = new ExpressionSet[actuals.length];
		for (int i = 0; i < params.length; i++)
			params[i] = entryState.intermediateStates.getState(actuals[i]).getExecutionExpressions();

		recursiveApprox = new GenericMapLattice<CFGCall, AnalysisState<A>>(entryState.postState);
		recursiveEntry = entryState.postState.bottom();

		do {
			LOG.debug(
					StringUtilities.ordinal(recursionCount + 1)
							+ " evaluation of recursive chain at "
							+ start.getLocation());

			previousApprox = recursiveApprox;
			previousEntry = recursiveEntry;
			recursiveEntry = recursiveEntry.bottom();
			recursiveApprox = recursiveApprox.bottom();

			// we reset the analysis at the point where the starting call can be
			// evaluated
			token = (CallStackId<A>) recursion.getInvocationToken();
			recursionKickOff = true;
			AnalysisState<A> post = start
					.forwardSemanticsAux(this, entryState.postState, params, entryState.intermediateStates);

			for (CFGCall end : backCalls)
				// no need to lub: the keys are the calls and
				// are thus unique
				recursiveApprox = recursiveApprox.putState(end, transferToCallsite(start, end, post));

			if (conf.recursionWideningThreshold < 0) {
				recursiveApprox = previousApprox.lub(recursiveApprox);
				recursiveEntry = previousEntry.lub(recursiveEntry);
			} else if (recursionCount == conf.recursionWideningThreshold) {
				recursiveApprox = previousApprox.widening(recursiveApprox);
				recursiveEntry = previousEntry.widening(recursiveEntry);
			} else {
				recursionCount++;
				recursiveApprox = previousApprox.lub(recursiveApprox);
				recursiveEntry = previousEntry.lub(recursiveEntry);
			}
		} while (!recursiveApprox.lessOrEqual(previousApprox) || !recursiveEntry.lessOrEqual(previousEntry));

		storeRecursionSummary(triggeringToken, call, callEntry, parameters, expressions);

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
			if (!returnsVoid && !returned.getExecutionState().knowsIdentifier(meta)) {
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

	/**
	 * Stores an additional, synthetic result for the recursion head, one call
	 * stack level deeper than {@code triggeringToken} (that is, at
	 * {@code maxCallStackDepth + 1}, a length otherwise unreachable since
	 * recursions are always solved before that depth is hit). This result uses
	 * the widened entry state computed by iterating the recursive chain (see
	 * {@link #computeFixpoint(CFG, CallStackId, AnalysisState)}) instead of the
	 * concrete one used by {@code triggeringToken}, together with the
	 * stabilized approximation of the recursive calls: this makes the effect of
	 * widening on the entry state observable, without altering the concrete,
	 * precise result already stored for {@code triggeringToken}.
	 *
	 * @param triggeringToken the call stack active when the recursion was
	 *                            triggered
	 * @param call            the call that is being solved
	 * @param initialState    the entry state of the call
	 * @param parameters      the parameters of the call
	 * @param expressions     the expressions store
	 *
	 * @throws SemanticException if an exception happens during the computation
	 */
	private void storeRecursionSummary(
			CallStackId<A> triggeringToken,
			CFGCall call,
			AnalysisState<A> initialState,
			ExpressionSet[] parameters,
			StatementStore<A> expressions)
			throws SemanticException {
		CallStackId<A> summaryToken = triggeringToken.push(
				call,
				CompoundState.of(recursiveEntry, new StatementStore<>(recursiveEntry.bottom())));
		token = summaryToken;
		committingSummary = true;
		Pair<AnalysisState<A>, ExpressionSet[]> prepared = prepareEntryState(
				call,
				initialState,
				parameters,
				expressions,
				new ScopeToken(call),
				recursion.getRecursionHead());
		try {
			computeFixpoint(recursion.getRecursionHead(), summaryToken, prepared.getLeft().lub(recursiveEntry));
		} catch (FixpointException e) {
			throw new SemanticException("Exception while storing the recursion summary for " + call, e);
		} finally {
			committingSummary = false;
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
		else {
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
