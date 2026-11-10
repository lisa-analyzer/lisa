package it.unive.lisa.interprocedural.context;

import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.events.EventQueue;
import it.unive.lisa.interprocedural.InterproceduralAnalysisException;
import it.unive.lisa.interprocedural.OpenCallPolicy;
import it.unive.lisa.interprocedural.callgraph.CallGraph;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.program.Application;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.statement.call.CFGCall;
import it.unive.lisa.symbolic.value.Variable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;

/**
 * A {@link ContextBasedAnalysis} that unrolls a recursive call, analyzing it as
 * a normal call, while its entry state brings new information: a recursive call
 * is solved with a fixpoint over the recursion only when the entry state of its
 * target, restricted to the parameters, is covered by the one of a call to the
 * same target already on the call stack, or when that target is already on the
 * call stack a given number of times. Calls are distinguished by the full call
 * stack. A recursion entered with exact values (e.g., {@code fib(9)}) is thus
 * computed call by call, as with inlining, while one entered with approximated
 * values (e.g., {@code fib(n)} with any {@code n}) is solved with a fixpoint as
 * soon as its entry state repeats.
 *
 * @author <a href="mailto:giacomo.zanatta@unive.it">Giacomo Zanatta</a>
 *
 * @param <A> the kind of {@link AbstractLattice} produced by the domain
 *                {@code D}
 * @param <D> the kind of {@link AbstractDomain} to run during the analysis
 */
public class EntryStateContextBasedAnalysis<A extends AbstractLattice<A>,
		D extends AbstractDomain<A>>
		extends
		ContextBasedAnalysis<A, D> {

	private final int bound;

	// the targets of the calls being evaluated, with the entry states of their
	// parameters, innermost last
	private final Deque<Pair<CFG, AnalysisState<A>>> stack = new ArrayDeque<>();

	// whether the call being evaluated is unrolled
	private boolean unroll;

	/**
	 * Builds the analysis.
	 *
	 * @param bound the maximum number of times a target is unrolled on the call
	 *                  stack
	 */
	public EntryStateContextBasedAnalysis(
			int bound) {
		super(-1);
		this.bound = bound;
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
		stack.clear();
		unroll = false;
	}

	@Override
	public AnalysisState<A> getAbstractResultOf(
			CFGCall call,
			AnalysisState<A> entryState,
			ExpressionSet[] parameters,
			StatementStore<A> expressions)
			throws SemanticException {
		List<Pair<CFG, AnalysisState<A>>> entries = new ArrayList<>();
		for (CFG cfg : call.getTargetedCFGs())
			entries.add(Pair.of(cfg, parametersOf(call, entryState, parameters, expressions, cfg)));
		unroll = entries.stream().allMatch(this::isNew);
		entries.forEach(stack::addLast);
		try {
			return super.getAbstractResultOf(call, entryState, parameters, expressions);
		} finally {
			entries.forEach(e -> stack.removeLast());
		}
	}

	@Override
	protected boolean isRecursion(
			CFGCall call,
			AnalysisState<A> entryState,
			ExpressionSet[] parameters,
			StatementStore<A> expressions)
			throws SemanticException {
		return !unroll && super.isRecursion(call, entryState, parameters, expressions);
	}

	// whether the target is on the stack less than bound times, always with
	// an entry state not covering the given one
	private boolean isNew(
			Pair<CFG, AnalysisState<A>> entry) {
		int count = 0;
		for (Pair<CFG, AnalysisState<A>> other : stack)
			if (other.getLeft().equals(entry.getLeft())) {
				if (++count >= bound)
					return false;
				try {
					if (entry.getRight().lessOrEqual(other.getRight()))
						return false;
				} catch (SemanticException e) {
					return false;
				}
			}
		return true;
	}

	// the entry state of the target, without errors and with the parameters
	// only (the identifiers of the callers are out of scope)
	private AnalysisState<A> parametersOf(
			CFGCall call,
			AnalysisState<A> entryState,
			ExpressionSet[] parameters,
			StatementStore<A> expressions,
			CFG cfg)
			throws SemanticException {
		AnalysisState<A> prepared = prepareEntryState(call, entryState.removeAllErrors(true), parameters,
				expressions, new ScopeToken(call), cfg).getLeft();
		AnalysisState<A> params = prepared.forgetIdentifiersIf(i -> !(i instanceof Variable), call);
		return params.bottom().withExecution(params.getExecution());
	}
}
