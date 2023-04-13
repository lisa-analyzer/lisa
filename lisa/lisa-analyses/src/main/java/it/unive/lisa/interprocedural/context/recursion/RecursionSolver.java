package it.unive.lisa.interprocedural.context.recursion;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.AnalyzedCFG;
import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.analysis.lattices.GenericMapLattice;
import it.unive.lisa.analysis.value.TypeDomain;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.conf.FixpointConfiguration;
import it.unive.lisa.interprocedural.InterproceduralAnalysisException;
import it.unive.lisa.interprocedural.OpenCallPolicy;
import it.unive.lisa.interprocedural.callgraph.CallGraph;
import it.unive.lisa.interprocedural.context.ContextBasedAnalysis;
import it.unive.lisa.interprocedural.context.ContextSensitivityToken;
import it.unive.lisa.program.Application;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.fixpoints.CFGFixpoint.CompoundState;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.call.CFGCall;
import it.unive.lisa.program.cfg.statement.call.Call;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.util.StringUtilities;
import it.unive.lisa.util.collections.workset.WorkingSet;
import it.unive.lisa.util.datastructures.graph.algorithms.FixpointException;

public class RecursionSolver<A extends AbstractState<A, H, V, T>,
		H extends HeapDomain<H>,
		V extends ValueDomain<V>,
		T extends TypeDomain<T>>
		extends ContextBasedAnalysis<A, H, V, T> {

	private static final Logger LOG = LogManager.getLogger(RecursionSolver.class);

	private final Recursion<A, H, V, T> recursion;

	private GenericMapLattice<CFGCall, AnalysisState<A, H, V, T>> previousApprox;

	private GenericMapLattice<CFGCall, AnalysisState<A, H, V, T>> recursiveApprox;

	private final Map<CFGCall, Triple<
			AnalysisState<A, H, V, T>,
			ExpressionSet<SymbolicExpression>[],
			StatementStore<A, H, V, T>>> finalEntryStates;

	public RecursionSolver(ContextBasedAnalysis<A, H, V, T> backing, Recursion<A, H, V, T> recursion) {
		super(backing);
		this.recursion = recursion;
		finalEntryStates = new HashMap<>();
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
			AnalysisState<A, H, V, T> entryState,
			Class<? extends WorkingSet<Statement>> fixpointWorkingSet,
			FixpointConfiguration conf)
			throws FixpointException {
		// we mark this as unsupported to make sure it never gets used as a root
		// analysis
		throw new UnsupportedOperationException();
	}

	@Override
	public AnalysisState<A, H, V, T> getAbstractResultOf(
			CFGCall call,
			AnalysisState<A, H, V, T> entryState,
			ExpressionSet<SymbolicExpression>[] parameters,
			StatementStore<A, H, V, T> expressions)
			throws SemanticException {
		AnalysisState<A, H, V, T> approx = recursiveApprox.getMap().get(call);
		if (approx != null) {
			finalEntryStates.put(call, Triple.of(entryState, parameters, expressions));
			// we bring in the entry state to carry over the correct scope
			return approx.lub(entryState);
		}
		return super.getAbstractResultOf(call, entryState, parameters, expressions);
	}

	@Override
	protected boolean canShortcut(CFG cfg) {
		// we want to compute the recursive chain with no shortcuts
		return !recursion.getInvolvedCFGs().contains(cfg);
	}

	public void solve() throws SemanticException {
		int recursionCount = 0;
		Call start = recursion.getStart();
		Map<CFGCall, ContextSensitivityToken> ends = recursion.getBackCalls();
		CompoundState<A, H, V, T> entryState = recursion.getEntryState();

		LOG.info("Solving recursion at " + start.getLocation());

		recursiveApprox = new GenericMapLattice<>(entryState.postState.bottom());
		// the return value of each back call must be the same as the one
		// starting the recursion, as they invoke the same cfg
		boolean returnsVoid = returnsVoid(start, null);
		for (CFGCall end : ends.keySet())
			if (returnsVoid)
				recursiveApprox = recursiveApprox.putState(end, entryState.postState.bottom());
			else
				recursiveApprox = recursiveApprox.putState(end, new AnalysisState<>(
						entryState.postState.getState().bottom(),
						end.getMetaVariable(),
						entryState.postState.getAliasing().bottom()));

		previousApprox = recursiveApprox;

		do {
			LOG.debug(StringUtilities.ordinal(recursionCount + 1)
					+ " evaluation of recursive chain at "
					+ start.getLocation());

			previousApprox = recursiveApprox;
			AnalysisState<A, H, V, T> post = start.semantics(entryState.postState, this, entryState.intermediateStates);
			for (CFGCall end : ends.keySet()) {
				AnalysisState<A, H, V, T> res = post.bottom();
				Identifier meta = end.getMetaVariable();
				if (returnsVoid)
					res = post;
				else
					for (Identifier variable : start.getMetaVariables())
						// we transfer the return value
						res = res.lub(post.assign(meta, variable, start));

				// we don't keep variables that are in scope here:
				// those will not be in scope at the recursion closure
				// except for the return value of the specific call
				res = res.forgetIdentifiersIf(i -> !i.isScopedByCall() && !i.equals(meta));
				recursiveApprox = recursiveApprox.putState(end, res);
			}

			if (conf.wideningThreshold < 0)
				recursiveApprox = previousApprox.lub(recursiveApprox);
			else if (recursionCount == conf.wideningThreshold)
				recursiveApprox = previousApprox.widening(recursiveApprox);
			else {
				recursionCount++;
				recursiveApprox = previousApprox.lub(recursiveApprox);
			}
		} while (!recursiveApprox.lessOrEqual(previousApprox));

		// at this point, the fixpoint results are just missing an entry for the
		// recursive target of the ending calls
		for (Entry<CFGCall, ContextSensitivityToken> end : ends.entrySet()) {
			AnalyzedCFG<A, H, V, T> recHead = results.get(recursion.getHead()).get(recursion.getToken());
			CFGCall call = end.getKey();
			Triple<AnalysisState<A, H, V, T>,
					ExpressionSet<SymbolicExpression>[],
					StatementStore<A, H, V, T>> triple = finalEntryStates.get(call);
			Pair<AnalysisState<A, H, V, T>, ExpressionSet<SymbolicExpression>[]> entry = prepareEntryState(
					end.getKey(),
					triple.getLeft(),
					triple.getMiddle(),
					triple.getRight(),
					new ScopeToken(call),
					recHead);
			results.putResult(recursion.getHead(), end.getValue(), recHead.withModifiedEntryState(entry.getLeft()));
		}
	}
}
