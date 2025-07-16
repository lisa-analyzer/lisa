package it.unive.lisa.analysis.traces;

import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.analysis.lattices.Satisfiability;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.program.cfg.controlFlow.ControlFlowStructure;
import it.unive.lisa.program.cfg.controlFlow.IfThenElse;
import it.unive.lisa.program.cfg.controlFlow.Loop;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

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
 * @see <a href=
 *          "https://doi.org/10.1145/1275497.1275501">https://doi.org/10.1145/1275497.1275501</a>
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
	public TraceLattice<A> assign(
			TraceLattice<A> state,
			Identifier id,
			SymbolicExpression expression,
			ProgramPoint pp)
			throws SemanticException {
		if (state.isBottom())
			return state;

		Map<ExecutionTrace, A> result = state.mkNewFunction(null, false);
		if (state.isTop() || state.function == null)
			result.put(ExecutionTrace.EMPTY, domain.assign(state.lattice.top(), id, expression, pp));
		else
			for (Entry<ExecutionTrace, A> trace : state)
				result.put(trace.getKey(), domain.assign(trace.getValue(), id, expression, pp));
		return new TraceLattice<>(state.lattice, result);
	}

	@Override
	public TraceLattice<A> smallStepSemantics(
			TraceLattice<A> state,
			SymbolicExpression expression,
			ProgramPoint pp)
			throws SemanticException {
		if (state.isBottom())
			return state;

		Map<ExecutionTrace, A> result = state.mkNewFunction(null, false);
		if (state.isTop() || state.function == null)
			result.put(ExecutionTrace.EMPTY, domain.smallStepSemantics(state.lattice.top(), expression, pp));
		else
			for (Entry<ExecutionTrace, A> trace : state)
				result.put(trace.getKey(), domain.smallStepSemantics(trace.getValue(), expression, pp));
		return new TraceLattice<>(state.lattice, result);
	}

	@Override
	public TraceLattice<A> assume(
			TraceLattice<A> state,
			SymbolicExpression expression,
			ProgramPoint src,
			ProgramPoint dest)
			throws SemanticException {
		if (state.isBottom())
			return state;

		ControlFlowStructure struct = src.getCFG().getControlFlowStructureOf(src);
		Map<ExecutionTrace, A> result = state.mkNewFunction(null, false);

		if (state.isTop() || state.function == null) {
			ExecutionTrace trace = ExecutionTrace.EMPTY;
			ExecutionTrace nextTrace = generateTraceFor(trace, struct, src, dest);
			result.put(nextTrace, state.lattice.top());
		} else
			for (Entry<ExecutionTrace, A> trace : state) {
				A st = trace.getValue();
				ExecutionTrace tokens = trace.getKey();
				A assume = domain.assume(st, expression, src, dest);
				if (assume.isBottom())
					// we only keep traces that can escape the loop
					continue;

				ExecutionTrace nextTrace = generateTraceFor(tokens, struct, src, dest);
				// when we hit one of the limits, more traces can get smashed
				// into one
				A prev = result.get(nextTrace);
				result.put(nextTrace, prev == null ? assume : assume.lub(prev));
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
			ProgramPoint pp)
			throws SemanticException {
		if (state.isTop())
			return Satisfiability.UNKNOWN;

		if (state.isBottom() || state.function == null)
			return Satisfiability.BOTTOM;

		Satisfiability result = Satisfiability.BOTTOM;
		for (Entry<ExecutionTrace, A> trace : state) {
			Satisfiability sat = domain.satisfies(trace.getValue(), expression, pp);
			if (sat == Satisfiability.BOTTOM)
				return sat;
			result = result.lub(sat);
		}
		return result;
	}

	@Override
	public SemanticOracle makeOracle(
			TraceLattice<A> state) {
		return new TraceOracle(state);
	}

	@Override
	public TraceLattice<A> makeLattice() {
		return new TraceLattice<>(domain.makeLattice());
	}

	private class TraceOracle
			implements
			SemanticOracle {

		private final TraceLattice<A> state;

		private TraceOracle(
				TraceLattice<A> state) {
			this.state = state;
		}

		@Override
		public ExpressionSet rewrite(
				SymbolicExpression expression,
				ProgramPoint pp)
				throws SemanticException {
			if (!expression.mightNeedRewriting())
				return new ExpressionSet(expression);

			if (state.isTop())
				return domain.makeOracle(state.lattice.top()).rewrite(expression, pp);
			else if (state.isBottom() || state.function == null)
				return domain.makeOracle(state.lattice.bottom()).rewrite(expression, pp);

			Set<SymbolicExpression> result = new HashSet<>();
			for (A st : state.getValues())
				result.addAll(domain.makeOracle(st).rewrite(expression, pp).elements());
			return new ExpressionSet(result);
		}

		@Override
		public ExpressionSet rewrite(
				ExpressionSet expressions,
				ProgramPoint pp)
				throws SemanticException {
			if (state.isTop())
				return domain.makeOracle(state.lattice.top()).rewrite(expressions, pp);
			else if (state.isBottom() || state.function == null)
				return domain.makeOracle(state.lattice.bottom()).rewrite(expressions, pp);

			Set<SymbolicExpression> result = new HashSet<>();
			for (A st : state.getValues())
				result.addAll(domain.makeOracle(st).rewrite(expressions, pp).elements());
			return new ExpressionSet(result);
		}

		@Override
		public Set<Type> getRuntimeTypesOf(
				SymbolicExpression e,
				ProgramPoint pp)
				throws SemanticException {
			if (state.isTop())
				return domain.makeOracle(state.lattice.top()).getRuntimeTypesOf(e, pp);
			else if (state.isBottom() || state.function == null)
				return domain.makeOracle(state.lattice.bottom()).getRuntimeTypesOf(e, pp);

			Set<Type> result = new HashSet<>();
			for (A st : state.getValues())
				result.addAll(domain.makeOracle(st).getRuntimeTypesOf(e, pp));
			return result;
		}

		@Override
		public Type getDynamicTypeOf(
				SymbolicExpression e,
				ProgramPoint pp)
				throws SemanticException {
			if (state.isTop())
				return domain.makeOracle(state.lattice.top()).getDynamicTypeOf(e, pp);
			else if (state.isBottom() || state.function == null)
				return domain.makeOracle(state.lattice.bottom()).getDynamicTypeOf(e, pp);

			Set<Type> result = new HashSet<>();
			for (A st : state.getValues())
				result.add(domain.makeOracle(st).getDynamicTypeOf(e, pp));
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
				Satisfiability sat = domain.makeOracle(trace.getValue()).alias(x, y, pp);
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
				Satisfiability sat = domain.makeOracle(trace.getValue()).isReachableFrom(x, y, pp);
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
				return domain.makeOracle(state.lattice.top()).reachableFrom(e, pp);
			else if (state.isBottom() || state.function == null)
				return domain.makeOracle(state.lattice.bottom()).reachableFrom(e, pp);

			Set<SymbolicExpression> result = new HashSet<>();
			for (A st : state.getValues())
				result.addAll(domain.makeOracle(st).reachableFrom(e, pp).elements());
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
				Satisfiability sat = domain.makeOracle(trace.getValue()).areMutuallyReachable(x, y, pp);
				if (sat == Satisfiability.BOTTOM)
					return sat;
				result = result.lub(sat);
			}
			return result;
		}

	}

}
