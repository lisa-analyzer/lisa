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
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Predicate;

public class TracePartitioning<A extends AbstractState<A, H, V, T>,
		H extends HeapDomain<H>,
		V extends ValueDomain<V>,
		T extends TypeDomain<T>>
		extends FunctionalLattice<TracePartitioning<A, H, V, T>, TokenList, A>
		implements AbstractState<TracePartitioning<A, H, V, T>, H, V, T> {

	private static int MAX_LOOP_ITERATIONS = 5;

	private static int MAX_CONDITIONS = 5;

	public TracePartitioning(A lattice) {
		super(lattice);
	}

	private TracePartitioning(A lattice, Map<TokenList, A> function) {
		super(lattice, function);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <D extends SemanticDomain<?, ?, ?>> D getDomainInstance(Class<D> domain) {
		if (domain.isAssignableFrom(getClass()))
			return (D) this;

		D result = null;

		for (A dom : getValues()) {
			D tmp = dom.getDomainInstance(domain);
			if (tmp != null)
				if (result == null)
					result = tmp;
				else
					result = null; // TODO
		}

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

		Map<TokenList, A> result = mkNewFunction(null, false);
		if (isTop() || function == null)
			result.put(new TokenList(), lattice.assign(id, expression, pp));
		else
			for (Entry<TokenList, A> trace : this)
				result.put(trace.getKey(), trace.getValue().assign(id, expression, pp));
		return new TracePartitioning<>(lattice, result);
	}

	@Override
	public TracePartitioning<A, H, V, T> smallStepSemantics(SymbolicExpression expression, ProgramPoint pp)
			throws SemanticException {
		if (isBottom())
			return this;

		Map<TokenList, A> result = mkNewFunction(null, false);
		if (isTop() || function == null)
			result.put(new TokenList(), lattice.smallStepSemantics(expression, pp));
		else
			for (Entry<TokenList, A> trace : this)
				result.put(trace.getKey(), trace.getValue().smallStepSemantics(expression, pp));
		return new TracePartitioning<>(lattice, result);
	}

	@Override
	public TracePartitioning<A, H, V, T> assume(SymbolicExpression expression, ProgramPoint src, ProgramPoint dest)
			throws SemanticException {
		if (isBottom())
			return this;

		ControlFlowStructure struct = src.getCFG().getControlFlowStructureOf(src);
		Map<TokenList, A> result = mkNewFunction(null, false);
		if (struct instanceof Loop) {
			boolean isInBody = ((Loop) struct).getBody().contains(dest);
			if (!isInBody) {
				if (isTop() || function == null)
					// no need to generate an empty trace here
					return this;
				else
					for (Entry<TokenList, A> trace : this) {
						A assume = trace.getValue().assume(expression, src, dest);
						if (!assume.isBottom())
							// we only keep traces that can escape the loop
							result.put(trace.getKey(), assume);
					}
			} else {
				if (isTop() || function == null) {
					Token token;
					if (MAX_LOOP_ITERATIONS > 0)
						token = new LoopIterationToken(src, 0);
					else
						token = new LoopSummaryToken(src);
					result.put(new TokenList().push(token), lattice.top());
				} else
					for (Entry<TokenList, A> trace : this) {
						A assume = trace.getValue().assume(expression, src, dest);
						if (assume.isBottom())
							// the trace will not appear
							continue;

						TokenList tokens = trace.getKey();
						Token prev = tokens.lastLoopTokenFor(src);
						if (prev instanceof LoopIterationToken) {
							LoopIterationToken li = (LoopIterationToken) prev;
							if (li.getIteration() < MAX_LOOP_ITERATIONS)
								tokens = tokens.push(new LoopIterationToken(src, li.getIteration() + 1));
							else
								tokens = tokens.push(new LoopSummaryToken(src));
						}
						// we do nothing on loop summaries as we already reached
						// the maximum iterations for this loop

						result.put(tokens, assume);
					}
			}
		} else if (struct instanceof IfThenElse) {
			boolean isTrueBranch = ((IfThenElse) struct).getTrueBranch().contains(dest);
			ConditionalToken token = new ConditionalToken(src, isTrueBranch);

			if (isTop() || function == null) {
				if (MAX_CONDITIONS < 1)
					return this;
				// we still generate the trace as the two branches might behave
				// differently
				result.put(new TokenList().push(token), lattice.top());
			} else
				for (Entry<TokenList, A> trace : this) {
					A assume = trace.getValue().assume(expression, src, dest);
					if (!assume.isBottom())
						result.put(trace.getKey().numberOfConditions() < MAX_CONDITIONS
								? trace.getKey().push(token)
								: trace.getKey(),
								assume);
				}
		} else {
			// no known conditional structure: we ignore it
			if (isTop() || function == null)
				return this;

			for (Entry<TokenList, A> trace : this)
				result.put(trace.getKey(), trace.getValue().assume(expression, src, dest));
		}

		return new TracePartitioning<>(lattice, result);
	}

	@Override
	public TracePartitioning<A, H, V, T> forgetIdentifier(Identifier id) throws SemanticException {
		if (isTop() || isBottom() || function == null)
			return this;

		Map<TokenList, A> result = mkNewFunction(null, false);
		for (Entry<TokenList, A> trace : this)
			result.put(trace.getKey(), trace.getValue().forgetIdentifier(id));
		return new TracePartitioning<>(lattice, result);
	}

	@Override
	public TracePartitioning<A, H, V, T> forgetIdentifiersIf(Predicate<Identifier> test) throws SemanticException {
		if (isTop() || isBottom() || function == null)
			return this;

		Map<TokenList, A> result = mkNewFunction(null, false);
		for (Entry<TokenList, A> trace : this)
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
		for (Entry<TokenList, A> trace : this) {
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

		Map<TokenList, A> result = mkNewFunction(null, false);
		for (Entry<TokenList, A> trace : this)
			result.put(trace.getKey(), trace.getValue().pushScope(token));
		return new TracePartitioning<>(lattice, result);
	}

	@Override
	public TracePartitioning<A, H, V, T> popScope(ScopeToken token) throws SemanticException {
		if (isTop() || isBottom() || function == null)
			return this;

		Map<TokenList, A> result = mkNewFunction(null, false);
		for (Entry<TokenList, A> trace : this)
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
			for (Entry<TokenList, A> trace : this)
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
			for (Entry<TokenList, A> trace : this)
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
			for (Entry<TokenList, A> trace : this)
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

		Map<TokenList, A> result = mkNewFunction(null, false);
		for (Entry<TokenList, A> trace : this)
			result.put(trace.getKey(), trace.getValue().withTopHeap());
		return new TracePartitioning<>(lattice, result);
	}

	@Override
	public TracePartitioning<A, H, V, T> withTopValue() {
		if (isTop() || isBottom() || function == null)
			return this;

		Map<TokenList, A> result = mkNewFunction(null, false);
		for (Entry<TokenList, A> trace : this)
			result.put(trace.getKey(), trace.getValue().withTopValue());
		return new TracePartitioning<>(lattice, result);
	}

	@Override
	public TracePartitioning<A, H, V, T> withTopType() {
		if (isTop() || isBottom() || function == null)
			return this;

		Map<TokenList, A> result = mkNewFunction(null, false);
		for (Entry<TokenList, A> trace : this)
			result.put(trace.getKey(), trace.getValue().withTopType());
		return new TracePartitioning<>(lattice, result);
	}

	@Override
	public TracePartitioning<A, H, V, T> mk(A lattice, Map<TokenList, A> function) {
		return new TracePartitioning<>(lattice, function);
	}

}
