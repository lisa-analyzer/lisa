package it.unive.lisa.analysis.traces;

import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.AnalysisState.Error;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.events.EventQueue;
import it.unive.lisa.interprocedural.ScopeId;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.lattices.Satisfiability;
import it.unive.lisa.lattices.traces.Branching;
import it.unive.lisa.lattices.traces.ExecutionTrace;
import it.unive.lisa.lattices.traces.LoopIteration;
import it.unive.lisa.lattices.traces.LoopSummary;
import it.unive.lisa.lattices.traces.TraceLattice;
import it.unive.lisa.lattices.traces.TraceToken;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.program.cfg.controlFlow.ControlFlowStructure;
import it.unive.lisa.program.cfg.controlFlow.IfThenElse;
import it.unive.lisa.program.cfg.controlFlow.Loop;
import it.unive.lisa.program.cfg.protection.ProtectedBlock;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;
import it.unive.lisa.util.datastructures.trie.PatriciaTrieMap;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.apache.commons.lang3.tuple.Pair;

/**
 * The trace partitioning abstract domain that splits execution traces to
 * increase precision of the analysis. Individual traces are identified by
 * {@link ExecutionTrace}s composed of tokens representing the conditions
 * traversed during the analysis. Note that all {@link TraceToken}s represent
 * intraprocedural control-flow constructs, as calls are abstracted away before
 * reaching this domain. <br>
 * <br>
 * Traces are never merged: instead, we limit the size of the traces we can
 * track, and we leave the choice of when and where to compact traces to other
 * analysis components. Specifically, an {@link ExecutionTrace} will contain at
 * most {@link #max_conditions} {@link Branching} tokens, and will track at most
 * {@link #max_loop_iterations} iterations for each loop (through
 * {@link LoopIteration} tokens) before summarizing the next ones with a
 * {@link LoopSummary} token. Both values are editable and customizable before
 * the analysis starts.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <A> the kind of {@link AbstractLattice} being partitioned, produced by
 *                the domain {@code D}
 * @param <D> the kind of {@link AbstractDomain} that manages the underlying
 *                states
 *
 * @see <a href="https://doi.org/10.1145/1275497.1275501">Xavier Rival, Laurent
 *          Mauborgne. The trace partitioning abstract domain. ACM Transactions
 *          on Programming Languages and Systems, 29(5), article 26, 2007.</a>
 */
public class TracePartitioning<A extends AbstractLattice<A>,
		D extends AbstractDomain<A>>
		implements
		AbstractDomain<TraceLattice<A>> {

	/**
	 * The maximum number of {@link LoopIteration} tokens that a trace can
	 * contain for each loop appearing in it, before collapsing the next ones in
	 * a single {@link LoopSummary} token.
	 */
	private final int max_loop_iterations;

	/**
	 * The maximum number of {@link Branching} tokens that a trace can contain.
	 */
	private final int max_conditions;

	/**
	 * The underlying domain managing the abstract states.
	 */
	public final D domain;

	private EventQueue events;

	/**
	 * Builds a new instance of this domain, with the default limits on the
	 * number of loop iterations and conditions that can be tracked in a single
	 * trace (i.e., 5 loops and 5 conditions).
	 * 
	 * @param domain the underlying domain managing the abstract states
	 */
	public TracePartitioning(
			D domain) {
		this.max_loop_iterations = 5;
		this.max_conditions = 5;
		this.domain = domain;
	}

	/**
	 * Builds a new instance of this domain, with the given limits on the number
	 * of loop iterations and conditions that can be tracked in a single trace.
	 * 
	 * @param maxLoopIterations the maximum number of {@link LoopIteration}
	 *                              tokens that a trace can contain
	 * @param maxConditions     the maximum number of {@link Branching} tokens
	 *                              that a trace can contain
	 * @param domain            the underlying domain managing the abstract
	 *                              states
	 */
	public TracePartitioning(
			int maxLoopIterations,
			int maxConditions,
			D domain) {
		this.max_loop_iterations = maxLoopIterations;
		this.max_conditions = maxConditions;
		this.domain = domain;
	}

	@Override
	public void setEventQueue(
			EventQueue queue) {
		this.events = queue;
		domain.setEventQueue(queue);
	}

	@Override
	public TraceLattice<A> assign(
			TraceLattice<A> state,
			Identifier id,
			SymbolicExpression expression,
			ScopeId<?> context,
			ProgramPoint pp)
			throws SemanticException {
		if (state.isBottom())
			return state;

		PatriciaTrieMap<ExecutionTrace, A> result = state.mkNewFunction(null, false);
		if (state.isTop() || state.function == null)
			result = result.put(ExecutionTrace.EMPTY, domain.assign(state.lattice.top(), id, expression, context, pp));
		else
			for (Entry<ExecutionTrace, A> trace : state)
				result = result.put(trace.getKey(), domain.assign(trace.getValue(), id, expression, context, pp));
		return new TraceLattice<>(state.lattice, result);
	}

	@Override
	public TraceLattice<A> smallStepSemantics(
			TraceLattice<A> state,
			SymbolicExpression expression,
			ScopeId<?> context,
			ProgramPoint pp)
			throws SemanticException {
		if (state.isBottom())
			return state;

		PatriciaTrieMap<ExecutionTrace, A> result = state.mkNewFunction(null, false);
		if (state.isTop() || state.function == null)
			result = result.put(ExecutionTrace.EMPTY, domain.smallStepSemantics(state.lattice.top(), expression, context, pp));
		else
			for (Entry<ExecutionTrace, A> trace : state)
				result = result.put(trace.getKey(), domain.smallStepSemantics(trace.getValue(), expression, context, pp));
		return new TraceLattice<>(state.lattice, result);
	}

	@Override
	public TraceLattice<A> assume(
			TraceLattice<A> state,
			SymbolicExpression expression,
			ScopeId<?> context,
			ProgramPoint src,
			ProgramPoint dest)
			throws SemanticException {
		if (state.isBottom())
			return state;

		ControlFlowStructure struct = src.getCFG().getControlFlowStructureOf(src);
		PatriciaTrieMap<ExecutionTrace, A> result = state.mkNewFunction(null, false);

		if (state.isTop() || state.function == null) {
			ExecutionTrace trace = ExecutionTrace.EMPTY;
			ExecutionTrace nextTrace = generateTraceFor(trace, struct, src, dest);
			result = result.put(nextTrace, state.lattice.top());
		} else
			for (Entry<ExecutionTrace, A> trace : state) {
				A st = trace.getValue();
				ExecutionTrace tokens = trace.getKey();
				A assume = domain.assume(st, expression, context, src, dest);
				if (assume.isBottom())
					// we only keep traces that can escape the loop
					continue;

				ExecutionTrace nextTrace = generateTraceFor(tokens, struct, src, dest);
				// when we hit one of the limits, more traces can get smashed
				// into one
				A prev = result.get(nextTrace);
				result = result.put(nextTrace, prev == null ? assume : assume.lub(prev));
			}

		if (result.isEmpty())
			// no traces pass the condition, so this branch is unreachable
			return state.bottom();

		return new TraceLattice<>(state.lattice, result);
	}

	private ExecutionTrace generateTraceFor(
			ExecutionTrace trace,
			ControlFlowStructure struct,
			ProgramPoint src,
			ProgramPoint dest) {
		if (struct instanceof Loop && ((Loop) struct).getBody().contains(dest)) {
			// on loop exits we do not generate new traces
			TraceToken prev = trace.lastLoopTokenFor(src);
			if (prev == null)
				if (max_loop_iterations > 0)
					return trace.push(new LoopIteration(src, 0));
				else
					return trace.push(new LoopSummary(src));
			else if (prev instanceof LoopIteration) {
				LoopIteration li = (LoopIteration) prev;
				if (li.getIteration() < max_loop_iterations)
					return trace.push(new LoopIteration(src, li.getIteration() + 1));
				else
					return trace.push(new LoopSummary(src));
			}
			// we do nothing on loop summaries as we already reached
			// the maximum iterations for this loop
		} else if (struct instanceof IfThenElse && trace.numberOfBranches() < max_conditions)
			return trace.push(new Branching(src, ((IfThenElse) struct).getTrueBranch().contains(dest)));

		// no known conditional structure, or no need to push new tokens
		return trace;
	}

	@Override
	public Satisfiability satisfies(
			TraceLattice<A> state,
			SymbolicExpression expression,
			ScopeId<?> context,
			ProgramPoint pp)
			throws SemanticException {
		if (state.isTop())
			return Satisfiability.UNKNOWN;

		if (state.isBottom() || state.function == null)
			return Satisfiability.BOTTOM;

		Satisfiability result = Satisfiability.BOTTOM;
		for (Entry<ExecutionTrace, A> trace : state) {
			Satisfiability sat = domain.satisfies(trace.getValue(), expression, context, pp);
			if (sat == Satisfiability.BOTTOM)
				return sat;
			result = result.lub(sat);
		}
		return result;
	}

	@Override
	public SemanticOracle makeOracle(
			TraceLattice<A> state,
			ScopeId<?> context) {
		return new TraceOracle(state, context);
	}

	@Override
	public TraceLattice<A> makeLattice() {
		return new TraceLattice<>(domain.makeLattice()).top();
	}

	private class TraceOracle
			implements
			SemanticOracle {

		private final TraceLattice<A> state;

		private final ScopeId<?> context;

		private TraceOracle(
				TraceLattice<A> state,
				ScopeId<?> context) {
			this.state = state;
			this.context = context;
		}

		@Override
		public EventQueue getEventQueue() {
			return events;
		}

		@Override
		public ScopeId<?> getContext() {
			return context;
		}

		@Override
		public ExpressionSet rewrite(
				SymbolicExpression expression,
				ProgramPoint pp)
				throws SemanticException {
			if (!expression.mightNeedRewriting())
				return new ExpressionSet(expression);

			if (state.isTop())
				return domain.makeOracle(state.lattice.top(), context).rewrite(expression, pp);
			else if (state.isBottom() || state.function == null)
				return domain.makeOracle(state.lattice.bottom(), context).rewrite(expression, pp);

			Set<SymbolicExpression> result = new HashSet<>();
			for (A st : state.getValues())
				result.addAll(domain.makeOracle(st, context).rewrite(expression, pp).elements());
			return new ExpressionSet(result);
		}

		@Override
		public ExpressionSet rewrite(
				ExpressionSet expressions,
				ProgramPoint pp)
				throws SemanticException {
			if (state.isTop())
				return domain.makeOracle(state.lattice.top(), context).rewrite(expressions, pp);
			else if (state.isBottom() || state.function == null)
				return domain.makeOracle(state.lattice.bottom(), context).rewrite(expressions, pp);

			Set<SymbolicExpression> result = new HashSet<>();
			for (A st : state.getValues())
				result.addAll(domain.makeOracle(st, context).rewrite(expressions, pp).elements());
			return new ExpressionSet(result);
		}

		@Override
		public Set<Type> getRuntimeTypesOf(
				SymbolicExpression e,
				ProgramPoint pp)
				throws SemanticException {
			if (state.isTop())
				return domain.makeOracle(state.lattice.top(), context).getRuntimeTypesOf(e, pp);
			else if (state.isBottom() || state.function == null)
				return domain.makeOracle(state.lattice.bottom(), context).getRuntimeTypesOf(e, pp);

			Set<Type> result = new HashSet<>();
			for (A st : state.getValues())
				result.addAll(domain.makeOracle(st, context).getRuntimeTypesOf(e, pp));
			return result;
		}

		@Override
		public Type getDynamicTypeOf(
				SymbolicExpression e,
				ProgramPoint pp)
				throws SemanticException {
			if (state.isTop())
				return domain.makeOracle(state.lattice.top(), context).getDynamicTypeOf(e, pp);
			else if (state.isBottom() || state.function == null)
				return domain.makeOracle(state.lattice.bottom(), context).getDynamicTypeOf(e, pp);

			Set<Type> result = new HashSet<>();
			for (A st : state.getValues())
				result.add(domain.makeOracle(st, context).getDynamicTypeOf(e, pp));
			return Type.commonSupertype(result, Untyped.INSTANCE);
		}

		@Override
		public Satisfiability alias(
				SymbolicExpression x,
				SymbolicExpression y,
				ProgramPoint pp)
				throws SemanticException {
			if (state.isTop())
				return Satisfiability.UNKNOWN;

			if (state.isBottom() || state.function == null)
				return Satisfiability.BOTTOM;

			Satisfiability result = Satisfiability.BOTTOM;
			for (Entry<ExecutionTrace, A> trace : state) {
				Satisfiability sat = domain.makeOracle(trace.getValue(), context).alias(x, y, pp);
				if (sat == Satisfiability.BOTTOM)
					return sat;
				result = result.lub(sat);
			}
			return result;
		}

		@Override
		public Satisfiability isReachableFrom(
				SymbolicExpression x,
				SymbolicExpression y,
				ProgramPoint pp)
				throws SemanticException {
			if (state.isTop())
				return Satisfiability.UNKNOWN;

			if (state.isBottom() || state.function == null)
				return Satisfiability.BOTTOM;

			Satisfiability result = Satisfiability.BOTTOM;
			for (Entry<ExecutionTrace, A> trace : state) {
				Satisfiability sat = domain.makeOracle(trace.getValue(), context).isReachableFrom(x, y, pp);
				if (sat == Satisfiability.BOTTOM)
					return sat;
				result = result.lub(sat);
			}
			return result;
		}

		@Override
		public ExpressionSet reachableFrom(
				SymbolicExpression e,
				ProgramPoint pp)
				throws SemanticException {
			if (state.isTop())
				return domain.makeOracle(state.lattice.top(), context).reachableFrom(e, pp);
			else if (state.isBottom() || state.function == null)
				return domain.makeOracle(state.lattice.bottom(), context).reachableFrom(e, pp);

			Set<SymbolicExpression> result = new HashSet<>();
			for (A st : state.getValues())
				result.addAll(domain.makeOracle(st, context).reachableFrom(e, pp).elements());
			return new ExpressionSet(result);
		}

		@Override
		public Satisfiability areMutuallyReachable(
				SymbolicExpression x,
				SymbolicExpression y,
				ProgramPoint pp)
				throws SemanticException {
			if (state.isTop())
				return Satisfiability.UNKNOWN;

			if (state.isBottom() || state.function == null)
				return Satisfiability.BOTTOM;

			Satisfiability result = Satisfiability.BOTTOM;
			for (Entry<ExecutionTrace, A> trace : state) {
				Satisfiability sat = domain.makeOracle(trace.getValue(), context).areMutuallyReachable(x, y, pp);
				if (sat == Satisfiability.BOTTOM)
					return sat;
				result = result.lub(sat);
			}
			return result;
		}

		@Override
		public boolean hasWholeValueAnlysis() {
			return domain.makeOracle(state.lattice.top(), context).hasWholeValueAnlysis();
		}

		@Override
		public Set<BinaryExpression> constraints(
				ValueDomain<?> requesting,
				ValueExpression e,
				ProgramPoint pp)
				throws SemanticException {
			if (state.isBottom())
				return null;
			if (state.isTop() || state.function == null || state.function.isEmpty())
				return Collections.emptySet();

			Set<BinaryExpression> result = new HashSet<>();
			for (Entry<ExecutionTrace, A> trace : state)
				result.addAll(domain.makeOracle(trace.getValue(), context).constraints(requesting, e, pp));

			return result;
		}

	}

	@Override
	public TraceLattice<A> onCallReturn(
			TraceLattice<A> entryState,
			TraceLattice<A> callres,
			ScopeId<?> context,
			ProgramPoint call)
			throws SemanticException {
		// TODO the traces might differ between entryState and callres,
		// how do we match them with each other?
		return callres;
	}

	@Override
	public Pair<TraceLattice<A>, TraceLattice<A>> onExecutionToError(
			TraceLattice<A> remaining,
			TraceLattice<A> moved,
			Error error,
			ScopeId<?> context,
			ProgramPoint pp)
			throws SemanticException {
		// the callback is applied trace by trace, pairing the remaining and
		// moved executions that followed the same trace
		Map<ExecutionTrace, A> rem = new HashMap<>();
		if (!remaining.isBottom())
			if (remaining.isTop() || remaining.function == null)
				rem.put(ExecutionTrace.EMPTY, remaining.lattice.top());
			else
				for (Entry<ExecutionTrace, A> trace : remaining)
					rem.put(trace.getKey(), trace.getValue());
		Map<ExecutionTrace, A> mov = new HashMap<>();
		if (!moved.isBottom())
			if (moved.isTop() || moved.function == null)
				mov.put(ExecutionTrace.EMPTY, moved.lattice.top());
			else
				for (Entry<ExecutionTrace, A> trace : moved)
					mov.put(trace.getKey(), trace.getValue());
		Set<ExecutionTrace> keys = new HashSet<>(rem.keySet());
		keys.addAll(mov.keySet());

		A bottom = remaining.lattice.bottom();
		PatriciaTrieMap<ExecutionTrace, A> left = remaining.mkNewFunction(null, false);
		PatriciaTrieMap<ExecutionTrace, A> right = moved.mkNewFunction(null, false);
		for (ExecutionTrace trace : keys) {
			Pair<A, A> res = domain.onExecutionToError(
					rem.getOrDefault(trace, bottom),
					mov.getOrDefault(trace, bottom),
					error,
					context,
					pp);
			if (rem.containsKey(trace))
				left = left.put(trace, res.getLeft());
			if (mov.containsKey(trace))
				right = right.put(trace, res.getRight());
		}

		return Pair.of(
				rem.isEmpty() ? remaining : new TraceLattice<>(remaining.lattice, left),
				mov.isEmpty() ? moved : new TraceLattice<>(moved.lattice, right));
	}

	@Override
	public TraceLattice<A> onErrorToExecution(
			TraceLattice<A> state,
			ProtectedBlock protectedBlock,
			ScopeId<?> context,
			ProgramPoint pp)
			throws SemanticException {
		if (state.isBottom())
			return state;

		PatriciaTrieMap<ExecutionTrace, A> result = state.mkNewFunction(null, false);
		if (state.isTop() || state.function == null)
			result = result.put(ExecutionTrace.EMPTY,
					domain.onErrorToExecution(state.lattice.top(), protectedBlock, context, pp));
		else
			for (Entry<ExecutionTrace, A> trace : state)
				result = result.put(trace.getKey(), domain.onErrorToExecution(trace.getValue(), protectedBlock, context, pp));
		return new TraceLattice<>(state.lattice, result);
	}

	@Override
	public Pair<TraceLattice<A>, TraceLattice<A>> onExecutionToHalting(
			TraceLattice<A> remaining,
			TraceLattice<A> moved,
			ScopeId<?> context,
			ProgramPoint pp)
			throws SemanticException {
		// the callback is applied trace by trace, pairing the remaining and
		// moved executions that followed the same trace
		Map<ExecutionTrace, A> rem = new HashMap<>();
		if (!remaining.isBottom())
			if (remaining.isTop() || remaining.function == null)
				rem.put(ExecutionTrace.EMPTY, remaining.lattice.top());
			else
				for (Entry<ExecutionTrace, A> trace : remaining)
					rem.put(trace.getKey(), trace.getValue());
		Map<ExecutionTrace, A> mov = new HashMap<>();
		if (!moved.isBottom())
			if (moved.isTop() || moved.function == null)
				mov.put(ExecutionTrace.EMPTY, moved.lattice.top());
			else
				for (Entry<ExecutionTrace, A> trace : moved)
					mov.put(trace.getKey(), trace.getValue());
		Set<ExecutionTrace> keys = new HashSet<>(rem.keySet());
		keys.addAll(mov.keySet());

		A bottom = remaining.lattice.bottom();
		PatriciaTrieMap<ExecutionTrace, A> left = remaining.mkNewFunction(null, false);
		PatriciaTrieMap<ExecutionTrace, A> right = moved.mkNewFunction(null, false);
		for (ExecutionTrace trace : keys) {
			Pair<A, A> res = domain.onExecutionToHalting(
					rem.getOrDefault(trace, bottom),
					mov.getOrDefault(trace, bottom),
					context,
					pp);
			if (rem.containsKey(trace))
				left = left.put(trace, res.getLeft());
			if (mov.containsKey(trace))
				right = right.put(trace, res.getRight());
		}

		return Pair.of(
				rem.isEmpty() ? remaining : new TraceLattice<>(remaining.lattice, left),
				mov.isEmpty() ? moved : new TraceLattice<>(moved.lattice, right));
	}
}
