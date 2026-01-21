package it.unive.lisa.analysis;

import it.unive.lisa.analysis.events.DomainAssignEnd;
import it.unive.lisa.analysis.events.DomainAssignStart;
import it.unive.lisa.analysis.events.DomainAssumeEnd;
import it.unive.lisa.analysis.events.DomainAssumeStart;
import it.unive.lisa.analysis.events.DomainSatisfiesEnd;
import it.unive.lisa.analysis.events.DomainSatisfiesStart;
import it.unive.lisa.analysis.events.DomainSmallStepEnd;
import it.unive.lisa.analysis.events.DomainSmallStepStart;
import it.unive.lisa.analysis.events.HeapRewriteEnd;
import it.unive.lisa.analysis.events.HeapRewriteStart;
import it.unive.lisa.analysis.events.SADSubsEnd;
import it.unive.lisa.analysis.events.SADSubsStart;
import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.heap.HeapDomain.HeapReplacement;
import it.unive.lisa.analysis.heap.HeapLattice;
import it.unive.lisa.analysis.type.TypeDomain;
import it.unive.lisa.analysis.type.TypeLattice;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.analysis.value.ValueLattice;
import it.unive.lisa.events.EventQueue;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.lattices.Satisfiability;
import it.unive.lisa.lattices.SimpleAbstractState;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.MemoryAllocation;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.type.Type;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.tuple.Pair;

/**
 * An abstract domain that combines a heap, a value, and a type domain into a
 * single abstract domain of type {@link SimpleAbstractState}.<br>
 * <br>
 * The interaction between heap and value/type domains follows the one defined
 * <a href=
 * "https://www.sciencedirect.com/science/article/pii/S0304397516300299">in this
 * paper</a>.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <H> the type of {@link HeapLattice} embedded in the states produced by
 *                this domain
 * @param <V> the type of {@link ValueLattice} embedded in the states produced
 *                by this domain
 * @param <T> the type of {@link TypeLattice} embedded in the states produced by
 *                this domain
 */
public class SimpleAbstractDomain<H extends HeapLattice<H>, V extends ValueLattice<V>, T extends TypeLattice<T>>
		implements
		AbstractDomain<SimpleAbstractState<H, V, T>> {

	/**
	 * The heap domain used by this abstract domain.
	 */
	public final HeapDomain<H> heapDomain;

	/**
	 * The value domain used by this abstract domain.
	 */
	public final ValueDomain<V> valueDomain;

	/**
	 * The type domain used by this abstract domain.
	 */
	public final TypeDomain<T> typeDomain;

	private EventQueue events;

	/**
	 * Builds a new abstract domain. The missing domains are set to the default
	 * no-op ones (i.e., {@link NoOpHeap}, {@link NoOpValues}, and
	 * {@link NoOpTypes}).
	 * 
	 * @param heapDomain the domain containing information regarding heap
	 *                       structures
	 */
	@SuppressWarnings("unchecked")
	public SimpleAbstractDomain(
			HeapDomain<H> heapDomain) {
		this.heapDomain = heapDomain;
		this.valueDomain = (ValueDomain<V>) new NoOpValues();
		this.typeDomain = (TypeDomain<T>) new NoOpTypes();
	}

	/**
	 * Builds a new abstract domain. The missing domains are set to the default
	 * no-op ones (i.e., {@link NoOpHeap}, {@link NoOpValues}, and
	 * {@link NoOpTypes}).
	 * 
	 * @param valueDomain the domain containing information regarding values of
	 *                        program variables and concretized memory locations
	 */
	@SuppressWarnings("unchecked")
	public SimpleAbstractDomain(
			ValueDomain<V> valueDomain) {
		this.heapDomain = (HeapDomain<H>) new NoOpHeap();
		this.valueDomain = valueDomain;
		this.typeDomain = (TypeDomain<T>) new NoOpTypes();
	}

	/**
	 * Builds a new abstract domain. The missing domains are set to the default
	 * no-op ones (i.e., {@link NoOpHeap}, {@link NoOpValues}, and
	 * {@link NoOpTypes}).
	 * 
	 * @param typeDomain the domain containing information regarding runtime
	 *                       types of program variables and concretized memory
	 *                       locations
	 */
	@SuppressWarnings("unchecked")
	public SimpleAbstractDomain(
			TypeDomain<T> typeDomain) {
		this.heapDomain = (HeapDomain<H>) new NoOpHeap();
		this.valueDomain = (ValueDomain<V>) new NoOpValues();
		this.typeDomain = typeDomain;
	}

	/**
	 * Builds a new abstract domain. The missing domains are set to the default
	 * no-op ones (i.e., {@link NoOpHeap}, {@link NoOpValues}, and
	 * {@link NoOpTypes}).
	 * 
	 * @param heapDomain  the domain containing information regarding heap
	 *                        structures
	 * @param valueDomain the domain containing information regarding values of
	 *                        program variables and concretized memory locations
	 */
	@SuppressWarnings("unchecked")
	public SimpleAbstractDomain(
			HeapDomain<H> heapDomain,
			ValueDomain<V> valueDomain) {
		this.heapDomain = heapDomain;
		this.valueDomain = valueDomain;
		this.typeDomain = (TypeDomain<T>) new NoOpTypes();
	}

	/**
	 * Builds a new abstract domain. The missing domains are set to the default
	 * no-op ones (i.e., {@link NoOpHeap}, {@link NoOpValues}, and
	 * {@link NoOpTypes}).
	 * 
	 * @param heapDomain the domain containing information regarding heap
	 *                       structures
	 * @param typeDomain the domain containing information regarding runtime
	 *                       types of program variables and concretized memory
	 *                       locations
	 */
	@SuppressWarnings("unchecked")
	public SimpleAbstractDomain(
			HeapDomain<H> heapDomain,
			TypeDomain<T> typeDomain) {
		this.heapDomain = heapDomain;
		this.valueDomain = (ValueDomain<V>) new NoOpValues();
		this.typeDomain = typeDomain;
	}

	/**
	 * Builds a new abstract domain. The missing domains are set to the default
	 * no-op ones (i.e., {@link NoOpHeap}, {@link NoOpValues}, and
	 * {@link NoOpTypes}).
	 * 
	 * @param valueDomain the domain containing information regarding values of
	 *                        program variables and concretized memory locations
	 * @param typeDomain  the domain containing information regarding runtime
	 *                        types of program variables and concretized memory
	 *                        locations
	 */
	@SuppressWarnings("unchecked")
	public SimpleAbstractDomain(
			ValueDomain<V> valueDomain,
			TypeDomain<T> typeDomain) {
		this.heapDomain = (HeapDomain<H>) new NoOpHeap();
		this.valueDomain = valueDomain;
		this.typeDomain = typeDomain;
	}

	/**
	 * Builds a new simple abstract domain that combines the given heap, value,
	 * and type domains.
	 * 
	 * @param heapDomain  the heap domain used by this abstract domain
	 * @param valueDomain the value domain used by this abstract domain
	 * @param typeDomain  the type domain used by this abstract domain
	 */
	public SimpleAbstractDomain(
			HeapDomain<H> heapDomain,
			ValueDomain<V> valueDomain,
			TypeDomain<T> typeDomain) {
		this.heapDomain = heapDomain;
		this.valueDomain = valueDomain;
		this.typeDomain = typeDomain;
	}

	@Override
	public void setEventQueue(
			EventQueue queue) {
		this.events = queue;
	}

	private void applySubstitution(
			List<HeapReplacement> subs,
			MutableOracle mo,
			ProgramPoint pp)
			throws SemanticException {
		if (subs != null) {
			T t0 = mo.type;
			V v0 = mo.value;

			if (events != null)
				events.post(new SADSubsStart<>(v0, t0, subs));

			for (HeapReplacement repl : subs) {
				T t = typeDomain.applyReplacement(mo.type, repl, pp, mo);
				V v = valueDomain.applyReplacement(mo.value, repl, pp, mo);
				// we update the oracle after both replacements have been
				// applied to not lose info on the sources that will be removed
				mo.type = t;
				mo.value = v;
			}

			if (events != null)
				events.post(new SADSubsEnd<>(v0, mo.value, t0, mo.type, subs));
		}
	}

	@Override
	public SimpleAbstractState<H, V, T> assign(
			SimpleAbstractState<H, V, T> state,
			Identifier id,
			SymbolicExpression expression,
			ProgramPoint pp)
			throws SemanticException {
		MutableOracle mo = new MutableOracle(state);

		if (events != null)
			events.post(new DomainAssignStart<>(getClass(), state, id, expression));

		if (!expression.mightNeedRewriting()) {
			ValueExpression ve = (ValueExpression) expression;
			if (events != null)
				events.post(new DomainAssignStart<>(heapDomain.getClass(), state.heapState, id, expression));
			mo.heap = heapDomain.assign(mo.heap, id, expression, pp, mo).getLeft();
			if (events != null) {
				events.post(new DomainAssignEnd<>(heapDomain.getClass(), pp, state.heapState, mo.heap, id, expression));
				events.post(new DomainAssignStart<>(typeDomain.getClass(), state.typeState, id, expression));
			}
			mo.type = typeDomain.assign(mo.type, id, ve, pp, mo);
			if (events != null) {
				events.post(new DomainAssignEnd<>(typeDomain.getClass(), pp, state.typeState, mo.type, id, expression));
				events.post(new DomainAssignStart<>(valueDomain.getClass(), state.valueState, id, expression));
			}
			mo.value = valueDomain.assign(mo.value, id, ve, pp, mo);
			if (events != null)
				events.post(
						new DomainAssignEnd<>(valueDomain.getClass(), pp, state.valueState, mo.value, id, expression));

			SimpleAbstractState<H, V, T> res = new SimpleAbstractState<>(mo);
			if (events != null)
				events.post(new DomainAssignEnd<>(getClass(), pp, state, res, id, expression));
			return res;
		}

		if (events != null)
			events.post(new DomainAssignStart<>(heapDomain.getClass(), state.heapState, id, expression));
		Pair<H, List<HeapReplacement>> heap = heapDomain.assign(mo.heap, id, expression, pp, mo);
		mo.heap = heap.getLeft();

		if (events != null) {
			events.post(new DomainAssignEnd<>(heapDomain.getClass(), pp, state.heapState, mo.heap, id, expression));
			events.post(new HeapRewriteStart<>(heapDomain.getClass(), mo.heap, expression));
		}

		ExpressionSet exprs = heapDomain.rewrite(mo.heap, expression, pp, mo);
		if (events != null)
			events.post(new HeapRewriteEnd<>(heapDomain.getClass(), mo.heap, expression, exprs));
		if (exprs.isEmpty()) {
			SimpleAbstractState<H, V, T> res = state.bottom();
			if (events != null)
				events.post(new DomainAssignEnd<>(getClass(), pp, state, res, id, expression));
			return res;
		}

		applySubstitution(heap.getRight(), mo, pp);

		if (exprs.elements.size() == 1) {
			SymbolicExpression expr = exprs.elements.iterator().next();
			if (!(expr instanceof ValueExpression))
				throw new SemanticException("Rewriting failed for expression " + expr);
			ValueExpression ve = (ValueExpression) expr;
			if (events != null)
				events.post(new DomainAssignStart<>(typeDomain.getClass(), state.typeState, id, expr));
			mo.type = typeDomain.assign(mo.type, id, ve, pp, mo);
			if (events != null) {
				events.post(new DomainAssignEnd<>(typeDomain.getClass(), pp, state.typeState, mo.type, id, expr));
				events.post(new DomainAssignStart<>(valueDomain.getClass(), state.valueState, id, expr));
			}
			mo.value = valueDomain.assign(mo.value, id, ve, pp, mo);
			if (events != null)
				events.post(new DomainAssignEnd<>(valueDomain.getClass(), pp, state.valueState, mo.value, id, expr));

			SimpleAbstractState<H, V, T> res = new SimpleAbstractState<>(mo);
			if (events != null)
				events.post(new DomainAssignEnd<>(getClass(), pp, state, res, id, expression));
			return res;
		}

		T typeRes = mo.type.bottom();
		V valueRes = mo.value.bottom();
		for (SymbolicExpression expr : exprs) {
			if (!(expr instanceof ValueExpression))
				throw new SemanticException("Rewriting failed for expression " + expr);
			ValueExpression ve = (ValueExpression) expr;
			T t = mo.type;
			if (events != null)
				events.post(new DomainAssignStart<>(typeDomain.getClass(), state.typeState, id, expr));
			mo.type = typeDomain.assign(mo.type, id, ve, pp, mo);
			if (events != null) {
				events.post(new DomainAssignEnd<>(typeDomain.getClass(), pp, state.typeState, mo.type, id, expr));
				events.post(new DomainAssignStart<>(valueDomain.getClass(), state.valueState, id, expr));
			}
			V v = valueDomain.assign(mo.value, id, ve, pp, mo);
			if (events != null)
				events.post(new DomainAssignEnd<>(valueDomain.getClass(), pp, state.valueState, mo.value, id, expr));
			typeRes = typeRes.lub(mo.type);
			valueRes = valueRes.lub(v);
			// we rollback the pre-eval state for the next expression
			mo.type = t;
		}

		SimpleAbstractState<H, V, T> res = new SimpleAbstractState<>(mo.heap, valueRes, typeRes);
		if (events != null)
			events.post(new DomainAssignEnd<>(getClass(), pp, state, res, id, expression));
		return res;
	}

	@Override
	public SimpleAbstractState<H, V, T> smallStepSemantics(
			SimpleAbstractState<H, V, T> state,
			SymbolicExpression expression,
			ProgramPoint pp)
			throws SemanticException {
		MutableOracle mo = new MutableOracle(state);

		if (events != null)
			events.post(new DomainSmallStepStart<>(getClass(), state, expression));

		if (!expression.mightNeedRewriting()) {
			ValueExpression ve = (ValueExpression) expression;
			if (events != null)
				events.post(new DomainSmallStepStart<>(heapDomain.getClass(), state.heapState, expression));
			mo.heap = heapDomain.smallStepSemantics(mo.heap, expression, pp, mo).getLeft();
			if (events != null) {
				events.post(new DomainSmallStepEnd<>(heapDomain.getClass(), pp, state.heapState, mo.heap, expression));
				events.post(new DomainSmallStepStart<>(typeDomain.getClass(), state.typeState, expression));
			}
			mo.type = typeDomain.smallStepSemantics(mo.type, ve, pp, mo);
			if (events != null) {
				events.post(new DomainSmallStepEnd<>(typeDomain.getClass(), pp, state.typeState, mo.type, expression));
				events.post(new DomainSmallStepStart<>(valueDomain.getClass(), state.valueState, expression));
			}
			mo.value = valueDomain.smallStepSemantics(mo.value, ve, pp, mo);
			if (events != null)
				events.post(
						new DomainSmallStepEnd<>(valueDomain.getClass(), pp, state.valueState, mo.value, expression));

			SimpleAbstractState<H, V, T> res = new SimpleAbstractState<>(mo);
			if (events != null)
				events.post(new DomainSmallStepEnd<>(getClass(), pp, state, res, expression));
			return res;
		}

		if (events != null)
			events.post(new DomainSmallStepStart<>(heapDomain.getClass(), state.heapState, expression));
		Pair<H, List<HeapReplacement>> heap = heapDomain.smallStepSemantics(mo.heap, expression, pp, mo);
		mo.heap = heap.getLeft();

		if (events != null) {
			events.post(new DomainSmallStepEnd<>(heapDomain.getClass(), pp, state.heapState, mo.heap, expression));
			events.post(new HeapRewriteStart<>(heapDomain.getClass(), mo.heap, expression));
		}

		ExpressionSet exprs = heapDomain.rewrite(heap.getLeft(), expression, pp, mo);
		if (events != null)
			events.post(new HeapRewriteEnd<>(heapDomain.getClass(), mo.heap, expression, exprs));
		if (exprs.isEmpty()) {
			SimpleAbstractState<H, V, T> res = state.bottom();
			if (events != null)
				events.post(new DomainSmallStepEnd<>(getClass(), pp, state, res, expression));
			return res;
		}

		applySubstitution(heap.getRight(), mo, pp);

		if (exprs.elements.size() == 1) {
			SymbolicExpression expr = exprs.elements.iterator().next();
			if (!(expr instanceof ValueExpression))
				throw new SemanticException("Rewriting failed for expression " + expr);
			ValueExpression ve = (ValueExpression) expr;
			if (events != null)
				events.post(new DomainSmallStepStart<>(typeDomain.getClass(), state.typeState, expr));
			mo.type = typeDomain.smallStepSemantics(mo.type, ve, pp, mo);
			if (expression instanceof MemoryAllocation && expr instanceof Identifier)
				// if the expression is a memory allocation, its type is
				// registered in the type domain
				mo.type = typeDomain.assign(mo.type, (Identifier) ve, ve, pp, mo);
			if (events != null) {
				events.post(new DomainSmallStepEnd<>(typeDomain.getClass(), pp, state.typeState, mo.type, expr));
				events.post(new DomainSmallStepStart<>(valueDomain.getClass(), state.valueState, expr));
			}
			mo.value = valueDomain.smallStepSemantics(mo.value, ve, pp, mo);
			if (events != null)
				events.post(new DomainSmallStepEnd<>(valueDomain.getClass(), pp, state.valueState, mo.value, expr));

			SimpleAbstractState<H, V, T> res = new SimpleAbstractState<>(mo);
			if (events != null)
				events.post(new DomainSmallStepEnd<>(getClass(), pp, state, res, expression));
			return res;
		}

		T typeRes = mo.type.bottom();
		V valueRes = mo.value.bottom();
		for (SymbolicExpression expr : exprs) {
			if (!(expr instanceof ValueExpression))
				throw new SemanticException("Rewriting failed for expression " + expr);
			ValueExpression ve = (ValueExpression) expr;
			T t = mo.type;
			if (events != null)
				events.post(new DomainSmallStepStart<>(typeDomain.getClass(), state.typeState, expr));
			mo.type = typeDomain.smallStepSemantics(mo.type, ve, pp, mo);
			if (expression instanceof MemoryAllocation && expr instanceof Identifier)
				// if the expression is a memory allocation, its type is
				// registered in the type domain
				mo.type = typeDomain.assign(mo.type, (Identifier) ve, ve, pp, mo);
			if (events != null) {
				events.post(new DomainSmallStepEnd<>(typeDomain.getClass(), pp, state.typeState, mo.type, expr));
				events.post(new DomainSmallStepStart<>(valueDomain.getClass(), state.valueState, expr));
			}
			V v = valueDomain.smallStepSemantics(mo.value, ve, pp, mo);
			if (events != null)
				events.post(new DomainSmallStepEnd<>(valueDomain.getClass(), pp, state.valueState, mo.value, expr));
			typeRes = typeRes.lub(mo.type);
			valueRes = valueRes.lub(v);
			// we rollback the pre-eval state for the next expression
			mo.type = t;
		}

		SimpleAbstractState<H, V, T> res = new SimpleAbstractState<>(mo.heap, valueRes, typeRes);
		if (events != null)
			events.post(new DomainSmallStepEnd<>(getClass(), pp, state, res, expression));
		return res;
	}

	@Override
	public SimpleAbstractState<H, V, T> assume(
			SimpleAbstractState<H, V, T> state,
			SymbolicExpression expression,
			ProgramPoint src,
			ProgramPoint dest)
			throws SemanticException {
		MutableOracle mo = new MutableOracle(state);

		if (events != null)
			events.post(new DomainAssumeStart<>(getClass(), state, expression));

		if (!expression.mightNeedRewriting()) {
			ValueExpression ve = (ValueExpression) expression;
			if (events != null)
				events.post(new DomainAssumeStart<>(heapDomain.getClass(), state.heapState, expression));
			mo.heap = heapDomain.assume(mo.heap, expression, src, dest, mo).getLeft();
			if (events != null)
				events.post(new DomainAssumeEnd<>(heapDomain.getClass(), src, state.heapState, mo.heap, expression));
			if (mo.heap.isBottom()) {
				SimpleAbstractState<H, V, T> res = state.bottom();
				if (events != null)
					events.post(new DomainAssumeEnd<>(getClass(), src, state, res, expression));
				return res;
			}

			if (events != null)
				events.post(new DomainAssumeStart<>(typeDomain.getClass(), state.typeState, expression));
			mo.type = typeDomain.assume(mo.type, ve, src, dest, mo);
			if (events != null)
				events.post(new DomainAssumeEnd<>(typeDomain.getClass(), src, state.typeState, mo.type, expression));
			if (mo.type.isBottom()) {
				SimpleAbstractState<H, V, T> res = state.bottom();
				if (events != null)
					events.post(new DomainAssumeEnd<>(getClass(), src, state, res, expression));
				return res;
			}

			if (events != null)
				events.post(new DomainAssumeStart<>(valueDomain.getClass(), state.valueState, expression));
			mo.value = valueDomain.assume(mo.value, ve, src, dest, mo);
			if (events != null)
				events.post(new DomainAssumeEnd<>(valueDomain.getClass(), src, state.valueState, mo.value, expression));
			if (mo.value.isBottom()) {
				SimpleAbstractState<H, V, T> res = state.bottom();
				if (events != null)
					events.post(new DomainAssumeEnd<>(getClass(), src, state, res, expression));
				return res;
			}

			SimpleAbstractState<H, V, T> res = new SimpleAbstractState<>(mo);
			if (events != null)
				events.post(new DomainAssumeEnd<>(getClass(), src, state, res, expression));
			return res;
		}

		if (events != null)
			events.post(new DomainAssumeStart<>(heapDomain.getClass(), state.heapState, expression));
		Pair<H, List<HeapReplacement>> heap = heapDomain.assume(mo.heap, expression, src, dest, mo);
		mo.heap = heap.getLeft();
		if (events != null)
			events.post(new DomainAssumeEnd<>(heapDomain.getClass(), src, state.heapState, mo.heap, expression));
		if (mo.heap.isBottom()) {
			SimpleAbstractState<H, V, T> res = state.bottom();
			if (events != null)
				events.post(new DomainAssumeEnd<>(getClass(), src, state, res, expression));
			return res;
		}

		if (events != null)
			events.post(new HeapRewriteStart<>(heapDomain.getClass(), mo.heap, expression));
		ExpressionSet exprs = heapDomain.rewrite(mo.heap, expression, src, mo);
		if (events != null)
			events.post(new HeapRewriteEnd<>(heapDomain.getClass(), mo.heap, expression, exprs));
		if (exprs.isEmpty()) {
			SimpleAbstractState<H, V, T> res = state.bottom();
			if (events != null)
				events.post(new DomainAssumeEnd<>(getClass(), src, state, res, expression));
			return res;
		}

		applySubstitution(heap.getRight(), mo, src);

		if (exprs.elements.size() == 1) {
			SymbolicExpression expr = exprs.elements.iterator().next();
			if (!(expr instanceof ValueExpression))
				throw new SemanticException("Rewriting failed for expression " + expr);
			ValueExpression ve = (ValueExpression) expr;
			if (events != null)
				events.post(new DomainAssumeStart<>(typeDomain.getClass(), state.typeState, expr));
			mo.type = typeDomain.assume(mo.type, ve, src, dest, mo);
			if (events != null)
				events.post(new DomainAssumeEnd<>(typeDomain.getClass(), src, state.typeState, mo.type, expr));
			if (mo.type.isBottom()) {
				SimpleAbstractState<H, V, T> res = state.bottom();
				if (events != null)
					events.post(new DomainAssumeEnd<>(getClass(), src, state, res, expression));
				return res;
			}

			if (events != null)
				events.post(new DomainAssumeStart<>(valueDomain.getClass(), state.valueState, expr));
			mo.value = valueDomain.assume(mo.value, ve, src, dest, mo);
			if (events != null)
				events.post(new DomainAssumeEnd<>(valueDomain.getClass(), src, state.valueState, mo.value, expr));
			if (mo.value.isBottom()) {
				SimpleAbstractState<H, V, T> res = state.bottom();
				if (events != null)
					events.post(new DomainAssumeEnd<>(getClass(), src, state, res, expression));
				return res;
			}

			SimpleAbstractState<H, V, T> res = new SimpleAbstractState<>(mo);
			if (events != null)
				events.post(new DomainAssumeEnd<>(getClass(), src, state, res, expression));
			return res;
		}

		T typeRes = mo.type.bottom();
		V valueRes = mo.value.bottom();
		for (SymbolicExpression expr : exprs) {
			if (!(expr instanceof ValueExpression))
				throw new SemanticException("Rewriting failed for expression " + expr);
			ValueExpression ve = (ValueExpression) expr;
			T t = mo.type;
			if (events != null)
				events.post(new DomainAssumeStart<>(typeDomain.getClass(), state.typeState, expr));
			mo.type = typeDomain.assume(mo.type, ve, src, dest, mo);
			if (events != null) {
				events.post(new DomainAssumeEnd<>(typeDomain.getClass(), src, state.typeState, mo.type, expr));
				events.post(new DomainAssumeStart<>(valueDomain.getClass(), state.valueState, expr));
			}
			V v = valueDomain.assume(mo.value, ve, src, dest, mo);
			if (events != null)
				events.post(new DomainAssumeEnd<>(valueDomain.getClass(), src, state.valueState, mo.value, expr));
			typeRes = typeRes.lub(mo.type);
			valueRes = valueRes.lub(v);
			// we rollback the pre-eval state for the next expression
			mo.type = t;
		}

		SimpleAbstractState<H, V, T> res;
		if (typeRes.isBottom() || valueRes.isBottom())
			res = state.bottom();
		else
			res = new SimpleAbstractState<>(mo.heap, valueRes, typeRes);
		if (events != null)
			events.post(new DomainAssumeEnd<>(getClass(), src, state, res, expression));
		return res;
	}

	@Override
	public Satisfiability satisfies(
			SimpleAbstractState<H, V, T> state,
			SymbolicExpression expression,
			ProgramPoint pp)
			throws SemanticException {
		MutableOracle mo = new MutableOracle(state);

		if (events != null)
			events.post(new DomainSatisfiesStart<>(getClass(), state, expression));

		if (events != null)
			events.post(new DomainSatisfiesStart<>(heapDomain.getClass(), state.heapState, expression));
		Satisfiability heapsat = heapDomain.satisfies(state.heapState, expression, pp, mo);
		if (events != null)
			events.post(new DomainSatisfiesEnd<>(heapDomain.getClass(), pp, state.heapState, heapsat, expression));
		if (heapsat == Satisfiability.BOTTOM) {
			if (events != null)
				events.post(new DomainSatisfiesEnd<>(getClass(), pp, state, heapsat, expression));
			return heapsat;
		}

		if (!expression.mightNeedRewriting()) {
			ValueExpression ve = (ValueExpression) expression;
			if (events != null)
				events.post(new DomainSatisfiesStart<>(typeDomain.getClass(), state.typeState, expression));
			Satisfiability typesat = typeDomain.satisfies(mo.type, ve, pp, mo);
			if (events != null)
				events.post(new DomainSatisfiesEnd<>(typeDomain.getClass(), pp, state.typeState, typesat, expression));
			if (typesat == Satisfiability.BOTTOM) {
				if (events != null)
					events.post(new DomainSatisfiesEnd<>(getClass(), pp, state, typesat, expression));
				return typesat;
			}

			if (events != null)
				events.post(new DomainSatisfiesStart<>(valueDomain.getClass(), state.valueState, expression));
			Satisfiability valuesat = valueDomain.satisfies(mo.value, ve, pp, mo);
			if (events != null)
				events.post(
						new DomainSatisfiesEnd<>(valueDomain.getClass(), pp, state.valueState, valuesat, expression));
			if (valuesat == Satisfiability.BOTTOM) {
				if (events != null)
					events.post(new DomainSatisfiesEnd<>(getClass(), pp, state, valuesat, expression));
				return valuesat;
			}

			Satisfiability glb = heapsat.glb(typesat).glb(valuesat);
			if (events != null)
				events.post(new DomainSatisfiesEnd<>(getClass(), pp, state, glb, expression));
			return glb;
		}

		if (events != null)
			events.post(new HeapRewriteStart<>(heapDomain.getClass(), mo.heap, expression));
		ExpressionSet exprs = heapDomain.rewrite(mo.heap, expression, pp, mo);
		if (events != null)
			events.post(new HeapRewriteEnd<>(heapDomain.getClass(), mo.heap, expression, exprs));
		if (exprs.isEmpty()) {
			if (events != null)
				events.post(new DomainSatisfiesEnd<>(getClass(), pp, state, Satisfiability.BOTTOM, expression));
			return Satisfiability.BOTTOM;
		}

		if (exprs.elements.size() == 1) {
			SymbolicExpression expr = exprs.elements.iterator().next();
			if (!(expr instanceof ValueExpression))
				throw new SemanticException("Rewriting failed for expression " + expr);
			ValueExpression ve = (ValueExpression) expr;
			if (events != null)
				events.post(new DomainSatisfiesStart<>(typeDomain.getClass(), state.typeState, expr));
			Satisfiability typesat = typeDomain.satisfies(mo.type, ve, pp, mo);
			if (events != null)
				events.post(new DomainSatisfiesEnd<>(typeDomain.getClass(), pp, state.typeState, typesat, expr));
			if (typesat == Satisfiability.BOTTOM) {
				if (events != null)
					events.post(new DomainSatisfiesEnd<>(getClass(), pp, state, typesat, expression));
				return typesat;
			}

			if (events != null)
				events.post(new DomainSatisfiesStart<>(valueDomain.getClass(), state.valueState, expr));
			Satisfiability valuesat = valueDomain.satisfies(mo.value, ve, pp, mo);
			if (events != null)
				events.post(new DomainSatisfiesEnd<>(valueDomain.getClass(), pp, state.valueState, valuesat, expr));
			if (valuesat == Satisfiability.BOTTOM) {
				if (events != null)
					events.post(new DomainSatisfiesEnd<>(getClass(), pp, state, valuesat, expression));
				return valuesat;
			}

			Satisfiability glb = heapsat.glb(typesat).glb(valuesat);
			if (events != null)
				events.post(new DomainSatisfiesEnd<>(getClass(), pp, state, glb, expression));
			return glb;
		}

		Satisfiability typesat = Satisfiability.BOTTOM;
		Satisfiability valuesat = Satisfiability.BOTTOM;
		for (SymbolicExpression expr : exprs) {
			if (!(expr instanceof ValueExpression))
				throw new SemanticException("Rewriting failed for expression " + expr);
			ValueExpression ve = (ValueExpression) expr;
			if (events != null)
				events.post(new DomainSatisfiesStart<>(typeDomain.getClass(), state.typeState, expr));
			Satisfiability sat = typeDomain.satisfies(mo.type, ve, pp, mo);
			if (events != null)
				events.post(new DomainSatisfiesEnd<>(typeDomain.getClass(), pp, state.typeState, sat, expr));
			if (sat == Satisfiability.BOTTOM) {
				if (events != null)
					events.post(new DomainSatisfiesEnd<>(getClass(), pp, state, sat, expression));
				return sat;
			}
			typesat = typesat.lub(sat);

			if (events != null)
				events.post(new DomainSatisfiesStart<>(valueDomain.getClass(), state.valueState, expr));
			sat = valueDomain.satisfies(mo.value, ve, pp, mo);
			if (events != null)
				events.post(new DomainSatisfiesEnd<>(valueDomain.getClass(), pp, state.valueState, sat, expr));
			if (sat == Satisfiability.BOTTOM) {
				if (events != null)
					events.post(new DomainSatisfiesEnd<>(getClass(), pp, state, sat, expression));
				return sat;
			}
			valuesat = valuesat.lub(sat);
		}

		Satisfiability glb = heapsat.glb(typesat).glb(valuesat);
		if (events != null)
			events.post(new DomainSatisfiesEnd<>(getClass(), pp, state, glb, expression));
		return glb;
	}

	@Override
	public SimpleAbstractState<H, V, T> makeLattice() {
		return new SimpleAbstractState<>(
				heapDomain.makeLattice(),
				valueDomain.makeLattice(),
				typeDomain.makeLattice());
	}

	@Override
	public SimpleAbstractState<H, V, T> onCallReturn(
			SimpleAbstractState<H, V, T> entryState,
			SimpleAbstractState<H, V, T> callres,
			ProgramPoint call)
			throws SemanticException {
		H h = heapDomain.onCallReturn(
				entryState.heapState,
				callres.heapState,
				call);
		V v = valueDomain.onCallReturn(
				entryState.valueState,
				callres.valueState,
				call);
		T t = typeDomain.onCallReturn(
				entryState.typeState,
				callres.typeState,
				call);
		if (h == callres.heapState && v == callres.valueState && t == callres.typeState)
			return callres;
		return new SimpleAbstractState<>(h, v, t);
	}

	@Override
	public SemanticOracle makeOracle(
			SimpleAbstractState<H, V, T> state) {
		return new MutableOracle(state);
	}

	/**
	 * An oracle for {@link SimpleAbstractState}s that can be muted, i.e., whose
	 * fields are not final and can be updated while the computation is
	 * happening.
	 * 
	 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
	 */
	public class MutableOracle
			implements
			SemanticOracle {

		/**
		 * The state containing information regarding heap structures.
		 */
		public H heap;

		/**
		 * The state containing information regarding values of program
		 * variables and concretized memory locations.
		 */
		public V value;

		/**
		 * The state containing runtime types information regarding runtime
		 * types of program variables and concretized memory locations.
		 */
		public T type;

		/**
		 * Builds the oracle.
		 * 
		 * @param state the state to use as a starting point for this oracle
		 */
		public MutableOracle(
				SimpleAbstractState<H, V, T> state) {
			this.heap = state.heapState;
			this.value = state.valueState;
			this.type = state.typeState;
		}

		@Override
		public EventQueue getEventQueue() {
			return events;
		}

		@Override
		public ExpressionSet rewrite(
				SymbolicExpression expression,
				ProgramPoint pp)
				throws SemanticException {
			if (!expression.mightNeedRewriting())
				return new ExpressionSet(expression);
			return heapDomain.rewrite(heap, expression, pp, this);
		}

		@Override
		public Set<Type> getRuntimeTypesOf(
				SymbolicExpression e,
				ProgramPoint pp)
				throws SemanticException {
			return typeDomain.getRuntimeTypesOf(type, e, pp, this);
		}

		@Override
		public Type getDynamicTypeOf(
				SymbolicExpression e,
				ProgramPoint pp)
				throws SemanticException {
			return typeDomain.getDynamicTypeOf(type, e, pp, this);
		}

		@Override
		public String toString() {
			return new SimpleAbstractState<>(this).toString();
		}

		@Override
		public Satisfiability alias(
				SymbolicExpression x,
				SymbolicExpression y,
				ProgramPoint pp)
				throws SemanticException {
			return heapDomain.alias(heap, x, y, pp, this);
		}

		@Override
		public Satisfiability isReachableFrom(
				SymbolicExpression x,
				SymbolicExpression y,
				ProgramPoint pp)
				throws SemanticException {
			return heapDomain.isReachableFrom(heap, x, y, pp, this);
		}

		@Override
		public ExpressionSet rewrite(
				ExpressionSet expressions,
				ProgramPoint pp)
				throws SemanticException {
			return heapDomain.rewrite(heap, expressions, pp, this);
		}

		@Override
		public ExpressionSet reachableFrom(
				SymbolicExpression e,
				ProgramPoint pp)
				throws SemanticException {
			return heapDomain.reachableFrom(heap, e, pp, this);
		}

		@Override
		public Satisfiability areMutuallyReachable(
				SymbolicExpression x,
				SymbolicExpression y,
				ProgramPoint pp)
				throws SemanticException {
			return heapDomain.areMutuallyReachable(heap, x, y, pp, this);
		}

	}
}
