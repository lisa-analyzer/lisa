package it.unive.lisa.analysis.traces;

import it.unive.lisa.analysis.AbstractState;
import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticDomain;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.lattices.FunctionalLattice;
import it.unive.lisa.analysis.representation.DomainRepresentation;
import it.unive.lisa.analysis.representation.MapRepresentation;
import it.unive.lisa.analysis.representation.StringRepresentation;
import it.unive.lisa.analysis.value.TypeDomain;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.program.cfg.controlFlow.ControlFlowStructure;
import it.unive.lisa.program.cfg.controlFlow.IfThenElse;
import it.unive.lisa.program.cfg.controlFlow.Loop;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Identifier;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Predicate;

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
 * most {@link #MAX_CONDITIONS} {@link Branching} tokens, and will track at most
 * {@link #MAX_LOOP_ITERATIONS} iterations for each loop (through
 * {@link LoopIteration} tokens) before summarizing the next ones with a
 * {@link LoopSummary} token. Both values are editable and customizable before
 * the analysis starts.<br>
 * <br>
 * As this class extends {@link FunctionalLattice}, one access individual traces
 * and their approximations using {@link #getKeys()}, {@link #getValues()},
 * {@link #getMap()} or by iterating over the instance itself. Approximations of
 * different traces can instead be collapsed and accessed by querying
 * {@link #getHeapState()}, {@link #getValueState()}, and
 * {@link #getTypeState()}, or {@link #collapse()}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <A> the type of {@link AbstractState} that this is being partitioned
 * @param <H> the type of {@link HeapDomain} embedded in the abstract states
 * @param <V> the type of {@link ValueDomain} embedded in the abstract states
 * @param <T> the type of {@link TypeDomain} embedded in the abstract states
 * 
 * @see <a href=
 *          "https://doi.org/10.1145/1275497.1275501">https://doi.org/10.1145/1275497.1275501</a>
 */
public class TracePartitioning<A extends AbstractState<A, H, V, T>,
		H extends HeapDomain<H>,
		V extends ValueDomain<V>,
		T extends TypeDomain<T>>
		extends FunctionalLattice<TracePartitioning<A, H, V, T>, ExecutionTrace, A>
		implements AbstractState<TracePartitioning<A, H, V, T>, H, V, T> {

	/**
	 * The maximum number of {@link LoopIteration} tokens that a trace can
	 * contain for each loop appearing in it, before collapsing the next ones in
	 * a single {@link LoopSummary} token.
	 */
	public static int MAX_LOOP_ITERATIONS = 5;

	/**
	 * The maximum number of {@link Branching} tokens that a trace can contain.
	 */
	public static int MAX_CONDITIONS = 5;

	/**
	 * Builds a new instance of this domain.
	 * 
	 * @param lattice a singleton of the underlying abstract states
	 */
	public TracePartitioning(A lattice) {
		super(lattice);
	}

	private TracePartitioning(A lattice, Map<ExecutionTrace, A> function) {
		super(lattice, function);
	}

	@Override
	public <D extends SemanticDomain<?, ?, ?>> Collection<D> getAllDomainInstances(Class<D> domain) {
		Collection<D> result = AbstractState.super.getAllDomainInstances(domain);
		if (isTop())
			return lattice.top().getAllDomainInstances(domain);
		else if (isBottom() || function == null)
			return lattice.bottom().getAllDomainInstances(domain);

		for (A dom : getValues())
			result.addAll(dom.getAllDomainInstances(domain));
		return result;
	}

	@Override
	public TracePartitioning<A, H, V, T> top() {
		return new TracePartitioning<>(lattice.top(), null);
	}

	@Override
	public TracePartitioning<A, H, V, T> bottom() {
		return new TracePartitioning<>(lattice.bottom(), null);
	}

	@Override
	public TracePartitioning<A, H, V, T> assign(Identifier id, SymbolicExpression expression, ProgramPoint pp)
			throws SemanticException {
		if (isBottom())
			return this;

		Map<ExecutionTrace, A> result = mkNewFunction(null, false);
		if (isTop() || function == null)
			result.put(new ExecutionTrace(), lattice.assign(id, expression, pp));
		else
			for (Entry<ExecutionTrace, A> trace : this)
				result.put(trace.getKey(), trace.getValue().assign(id, expression, pp));
		return new TracePartitioning<>(lattice, result);
	}

	@Override
	public TracePartitioning<A, H, V, T> smallStepSemantics(SymbolicExpression expression, ProgramPoint pp)
			throws SemanticException {
		if (isBottom())
			return this;

		Map<ExecutionTrace, A> result = mkNewFunction(null, false);
		if (isTop() || function == null)
			result.put(new ExecutionTrace(), lattice.smallStepSemantics(expression, pp));
		else
			for (Entry<ExecutionTrace, A> trace : this)
				result.put(trace.getKey(), trace.getValue().smallStepSemantics(expression, pp));
		return new TracePartitioning<>(lattice, result);
	}

	@Override
	public TracePartitioning<A, H, V, T> assume(SymbolicExpression expression, ProgramPoint src, ProgramPoint dest)
			throws SemanticException {
		if (isBottom())
			return this;

		ControlFlowStructure struct = src.getCFG().getControlFlowStructureOf(src);
		Map<ExecutionTrace, A> result = mkNewFunction(null, false);

		if (isTop() || function == null) {
			ExecutionTrace trace = new ExecutionTrace();
			ExecutionTrace nextTrace = generateTraceFor(trace, struct, src, dest);
			result.put(nextTrace, lattice.top());
		} else
			for (Entry<ExecutionTrace, A> trace : this) {
				A state = trace.getValue();
				ExecutionTrace tokens = trace.getKey();
				A assume = state.assume(expression, src, dest);
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
			return bottom();

		return new TracePartitioning<>(lattice, result);
	}

	private static ExecutionTrace generateTraceFor(ExecutionTrace trace, ControlFlowStructure struct, ProgramPoint src,
			ProgramPoint dest) {
		if (struct instanceof Loop && ((Loop) struct).getBody().contains(dest)) {
			// on loop exits we do not generate new traces
			TraceToken prev = trace.lastLoopTokenFor(src);
			if (prev == null)
				if (MAX_LOOP_ITERATIONS > 0)
					return trace.push(new LoopIteration(src, 0));
				else
					return trace.push(new LoopSummary(src));
			else if (prev instanceof LoopIteration) {
				LoopIteration li = (LoopIteration) prev;
				if (li.getIteration() < MAX_LOOP_ITERATIONS)
					return trace.push(new LoopIteration(src, li.getIteration() + 1));
				else
					return trace.push(new LoopSummary(src));
			}
			// we do nothing on loop summaries as we already reached
			// the maximum iterations for this loop
		} else if (struct instanceof IfThenElse && trace.numberOfBranches() < MAX_CONDITIONS)
			return trace.push(new Branching(src, ((IfThenElse) struct).getTrueBranch().contains(dest)));

		// no known conditional structure, or no need to push new tokens
		return trace;
	}

	@Override
	public TracePartitioning<A, H, V, T> forgetIdentifier(Identifier id) throws SemanticException {
		if (isTop() || isBottom() || function == null)
			return this;

		Map<ExecutionTrace, A> result = mkNewFunction(null, false);
		for (Entry<ExecutionTrace, A> trace : this)
			result.put(trace.getKey(), trace.getValue().forgetIdentifier(id));
		return new TracePartitioning<>(lattice, result);
	}

	@Override
	public TracePartitioning<A, H, V, T> forgetIdentifiersIf(Predicate<Identifier> test) throws SemanticException {
		if (isTop() || isBottom() || function == null)
			return this;

		Map<ExecutionTrace, A> result = mkNewFunction(null, false);
		for (Entry<ExecutionTrace, A> trace : this)
			result.put(trace.getKey(), trace.getValue().forgetIdentifiersIf(test));
		return new TracePartitioning<>(lattice, result);
	}

	@Override
	public Satisfiability satisfies(SymbolicExpression expression, ProgramPoint pp) throws SemanticException {
		if (isTop())
			return Satisfiability.UNKNOWN;

		if (isBottom() || function == null)
			return Satisfiability.BOTTOM;

		Satisfiability result = Satisfiability.BOTTOM;
		for (Entry<ExecutionTrace, A> trace : this) {
			Satisfiability sat = trace.getValue().satisfies(expression, pp);
			if (sat == Satisfiability.BOTTOM)
				return sat;
			result = result.lub(sat);
		}
		return result;
	}

	@Override
	public TracePartitioning<A, H, V, T> pushScope(ScopeToken token) throws SemanticException {
		if (isTop() || isBottom() || function == null)
			return this;

		Map<ExecutionTrace, A> result = mkNewFunction(null, false);
		for (Entry<ExecutionTrace, A> trace : this)
			result.put(trace.getKey(), trace.getValue().pushScope(token));
		return new TracePartitioning<>(lattice, result);
	}

	@Override
	public TracePartitioning<A, H, V, T> popScope(ScopeToken token) throws SemanticException {
		if (isTop() || isBottom() || function == null)
			return this;

		Map<ExecutionTrace, A> result = mkNewFunction(null, false);
		for (Entry<ExecutionTrace, A> trace : this)
			result.put(trace.getKey(), trace.getValue().popScope(token));
		return new TracePartitioning<>(lattice, result);
	}

	@Override
	public DomainRepresentation representation() {
		if (isTop())
			return Lattice.topRepresentation();

		if (isBottom())
			return Lattice.bottomRepresentation();

		if (function == null)
			return new StringRepresentation("empty");

		return new MapRepresentation(function, StringRepresentation::new, AbstractState::representation);
	}

	@Override
	public H getHeapState() {
		if (isTop())
			return lattice.getHeapState().top();

		H result = lattice.getHeapState().bottom();
		if (isBottom() || function == null)
			return result;

		try {
			for (Entry<ExecutionTrace, A> trace : this)
				result = result.lub(trace.getValue().getHeapState());
		} catch (SemanticException e) {
			return result.bottom();
		}
		return result;
	}

	@Override
	public V getValueState() {
		if (isTop())
			return lattice.getValueState().top();

		V result = lattice.getValueState().bottom();
		if (isBottom() || function == null)
			return result;

		try {
			for (Entry<ExecutionTrace, A> trace : this)
				result = result.lub(trace.getValue().getValueState());
		} catch (SemanticException e) {
			return result.bottom();
		}
		return result;
	}

	@Override
	public T getTypeState() {
		if (isTop())
			return lattice.getTypeState().top();

		T result = lattice.getTypeState().bottom();
		if (isBottom() || function == null)
			return result;

		try {
			for (Entry<ExecutionTrace, A> trace : this)
				result = result.lub(trace.getValue().getTypeState());
		} catch (SemanticException e) {
			return result.bottom();
		}
		return result;
	}

	@Override
	public TracePartitioning<A, H, V, T> withTopHeap() {
		if (isTop() || isBottom() || function == null)
			return this;

		Map<ExecutionTrace, A> result = mkNewFunction(null, false);
		for (Entry<ExecutionTrace, A> trace : this)
			result.put(trace.getKey(), trace.getValue().withTopHeap());
		return new TracePartitioning<>(lattice, result);
	}

	@Override
	public TracePartitioning<A, H, V, T> withTopValue() {
		if (isTop() || isBottom() || function == null)
			return this;

		Map<ExecutionTrace, A> result = mkNewFunction(null, false);
		for (Entry<ExecutionTrace, A> trace : this)
			result.put(trace.getKey(), trace.getValue().withTopValue());
		return new TracePartitioning<>(lattice, result);
	}

	@Override
	public TracePartitioning<A, H, V, T> withTopType() {
		if (isTop() || isBottom() || function == null)
			return this;

		Map<ExecutionTrace, A> result = mkNewFunction(null, false);
		for (Entry<ExecutionTrace, A> trace : this)
			result.put(trace.getKey(), trace.getValue().withTopType());
		return new TracePartitioning<>(lattice, result);
	}

	@Override
	public TracePartitioning<A, H, V, T> mk(A lattice, Map<ExecutionTrace, A> function) {
		return new TracePartitioning<>(lattice, function);
	}

	/**
	 * Collapses all of the traces contained in this domain, returning a unique
	 * abstract state that over-approximates all of them.
	 * 
	 * @return the collapsed state
	 */
	public A collapse() {
		if (isTop())
			return lattice.top();

		A result = lattice.bottom();
		if (isBottom() || function == null)
			return result;

		try {
			for (Entry<ExecutionTrace, A> trace : this)
				result = result.lub(trace.getValue());
		} catch (SemanticException e) {
			return result.bottom();
		}
		return result;
	}

	@Override
	public String toString() {
		return representation().toString();
	}
}
