package it.unive.lisa.analysis;

import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.heap.HeapSemanticOperation.HeapReplacement;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.analysis.lattices.Satisfiability;
import it.unive.lisa.analysis.type.TypeDomain;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.MemoryAllocation;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.type.Type;
import it.unive.lisa.util.representation.ObjectRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * An abstract state of the analysis, composed by a heap state modeling the
 * memory layout, a value state modeling values of program variables and memory
 * locations, and a type state that can give types to expressions knowing the
 * ones of variables.<br>
 * <br>
 * The interaction between heap and value/type domains follows the one defined
 * <a href=
 * "https://www.sciencedirect.com/science/article/pii/S0304397516300299">in this
 * paper</a>.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <H> the type of {@link HeapDomain} embedded in this state
 * @param <V> the type of {@link ValueDomain} embedded in this state
 * @param <T> the type of {@link TypeDomain} embedded in this state
 */
public class SimpleAbstractState<H extends HeapDomain<H>,
		V extends ValueDomain<V>,
		T extends TypeDomain<T>>
		implements
		BaseLattice<SimpleAbstractState<H, V, T>>,
		AbstractState<SimpleAbstractState<H, V, T>> {

	/**
	 * The key that should be used to store the instance of {@link HeapDomain}
	 * inside the {@link StructuredRepresentation} returned by
	 * {@link #representation()}.
	 */
	public static final String HEAP_REPRESENTATION_KEY = "heap";

	/**
	 * The key that should be used to store the instance of {@link TypeDomain}
	 * inside the {@link StructuredRepresentation} returned by
	 * {@link #representation()}.
	 */
	public static final String TYPE_REPRESENTATION_KEY = "type";

	/**
	 * The key that should be used to store the instance of {@link ValueDomain}
	 * inside the {@link StructuredRepresentation} returned by
	 * {@link #representation()}.
	 */
	public static final String VALUE_REPRESENTATION_KEY = "value";

	/**
	 * The domain containing information regarding heap structures
	 */
	private final H heapState;

	/**
	 * The domain containing information regarding values of program variables
	 * and concretized memory locations
	 */
	private final V valueState;

	/**
	 * The domain containing runtime types information regarding runtime types
	 * of program variables and concretized memory locations
	 */
	private final T typeState;

	/**
	 * Builds a new abstract state.
	 * 
	 * @param heapState  the domain containing information regarding heap
	 *                       structures
	 * @param valueState the domain containing information regarding values of
	 *                       program variables and concretized memory locations
	 * @param typeState  the domain containing information regarding runtime
	 *                       types of program variables and concretized memory
	 *                       locations
	 */
	public SimpleAbstractState(
			H heapState,
			V valueState,
			T typeState) {
		this.heapState = heapState;
		this.valueState = valueState;
		this.typeState = typeState;
	}

	/**
	 * Yields the {@link HeapDomain} contained in this state.
	 * 
	 * @return the heap domain
	 */
	public H getHeapState() {
		return heapState;
	}

	/**
	 * Yields the {@link ValueDomain} contained in this state.
	 * 
	 * @return the value domain
	 */
	public V getValueState() {
		return valueState;
	}

	/**
	 * Yields the {@link TypeDomain} contained in this state.
	 * 
	 * @return the type domain
	 */
	public T getTypeState() {
		return typeState;
	}

	@Override
	public SimpleAbstractState<H, V, T> assign(
			Identifier id,
			SymbolicExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (!expression.mightNeedRewriting()) {
			ValueExpression ve = (ValueExpression) expression;
			return new SimpleAbstractState<>(
					heapState.assign(id, expression, pp, this),
					valueState.assign(id, ve, pp, this),
					typeState.assign(id, ve, pp, this));
		}

		MutableOracle<H, V, T> mo = new MutableOracle<>(heapState, valueState, typeState);
		mo.heap = mo.heap.assign(id, expression, pp, mo);
		ExpressionSet exprs = mo.heap.rewrite(expression, pp, mo);
		if (exprs.isEmpty())
			return bottom();

		applySubstitution(mo, pp);

		if (exprs.elements.size() == 1) {
			SymbolicExpression expr = exprs.elements.iterator().next();
			if (!(expr instanceof ValueExpression))
				throw new SemanticException("Rewriting failed for expression " + expr);
			ValueExpression ve = (ValueExpression) expr;
			T t = mo.type.assign(id, ve, pp, mo);
			V v = mo.value.assign(id, ve, pp, mo);
			return new SimpleAbstractState<>(mo.heap, v, t);
		}

		T typeRes = mo.type.bottom();
		V valueRes = mo.value.bottom();
		for (SymbolicExpression expr : exprs) {
			if (!(expr instanceof ValueExpression))
				throw new SemanticException("Rewriting failed for expression " + expr);
			ValueExpression ve = (ValueExpression) expr;
			T t = mo.type.assign(id, ve, pp, mo);
			V v = mo.value.assign(id, ve, pp, mo);
			typeRes = typeRes.lub(t);
			valueRes = valueRes.lub(v);
		}

		return new SimpleAbstractState<>(mo.heap, valueRes, typeRes);
	}

	@Override
	public SimpleAbstractState<H, V, T> smallStepSemantics(
			SymbolicExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (!expression.mightNeedRewriting()) {
			ValueExpression ve = (ValueExpression) expression;
			return new SimpleAbstractState<>(
					heapState.smallStepSemantics(expression, pp, this),
					valueState.smallStepSemantics(ve, pp, this),
					typeState.smallStepSemantics(ve, pp, this));
		}

		MutableOracle<H, V, T> mo = new MutableOracle<>(heapState, valueState, typeState);
		mo.heap = mo.heap.smallStepSemantics(expression, pp, mo);
		ExpressionSet exprs = mo.heap.rewrite(expression, pp, mo);
		if (exprs.isEmpty())
			return bottom();

		applySubstitution(mo, pp);

		if (exprs.elements.size() == 1) {
			SymbolicExpression expr = exprs.elements.iterator().next();
			if (!(expr instanceof ValueExpression))
				throw new SemanticException("Rewriting failed for expression " + expr);
			ValueExpression ve = (ValueExpression) expr;
			T t = mo.type.smallStepSemantics(ve, pp, mo);
			if (expression instanceof MemoryAllocation && expr instanceof Identifier)
				// if the expression is a memory allocation, its type is
				// registered in the type domain
				t = t.assign((Identifier) ve, ve, pp, mo);
			V v = mo.value.smallStepSemantics(ve, pp, mo);
			return new SimpleAbstractState<>(mo.heap, v, t);
		}

		T typeRes = mo.type.bottom();
		V valueRes = mo.value.bottom();
		for (SymbolicExpression expr : exprs) {
			if (!(expr instanceof ValueExpression))
				throw new SemanticException("Rewriting failed for expression " + expr);
			ValueExpression ve = (ValueExpression) expr;
			T t = mo.type.smallStepSemantics(ve, pp, mo);
			if (expression instanceof MemoryAllocation && expr instanceof Identifier)
				// if the expression is a memory allocation, its type is
				// registered in the type domain
				t = t.assign((Identifier) ve, ve, pp, mo);
			V v = mo.value.smallStepSemantics(ve, pp, mo);
			typeRes = typeRes.lub(t);
			valueRes = valueRes.lub(v);
		}

		return new SimpleAbstractState<>(mo.heap, valueRes, typeRes);
	}

	private static <H extends HeapDomain<H>,
			V extends ValueDomain<V>,
			T extends TypeDomain<T>> void applySubstitution(
					MutableOracle<H, V, T> mo,
					ProgramPoint pp)
					throws SemanticException {
		List<HeapReplacement> subs = mo.heap.getSubstitution();
		if (subs != null)
			for (HeapReplacement repl : subs) {
				T t = mo.type.applyReplacement(repl, pp, mo);
				V v = mo.value.applyReplacement(repl, pp, mo);
				// we update the oracle after both replacements have been
				// applied to not lose info on the sources that will be removed
				mo.type = t;
				mo.value = v;
			}
	}

	@Override
	public SimpleAbstractState<H, V, T> assume(
			SymbolicExpression expression,
			ProgramPoint src,
			ProgramPoint dest,
			SemanticOracle oracle)
			throws SemanticException {
		if (!expression.mightNeedRewriting()) {
			ValueExpression ve = (ValueExpression) expression;
			H h = heapState.assume(expression, src, dest, this);
			if (h.isBottom())
				return bottom();
			T t = typeState.assume(ve, src, dest, this);
			if (t.isBottom())
				return bottom();
			V v = valueState.assume(ve, src, dest, this);
			if (v.isBottom())
				return bottom();
			return new SimpleAbstractState<>(h, v, t);
		}

		MutableOracle<H, V, T> mo = new MutableOracle<>(heapState, valueState, typeState);
		mo.heap = mo.heap.assume(expression, src, dest, mo);
		if (mo.heap.isBottom())
			return bottom();
		ExpressionSet exprs = mo.heap.rewrite(expression, src, mo);
		if (exprs.isEmpty())
			return bottom();

		applySubstitution(mo, src);

		if (exprs.elements.size() == 1) {
			SymbolicExpression expr = exprs.elements.iterator().next();
			if (!(expr instanceof ValueExpression))
				throw new SemanticException("Rewriting failed for expression " + expr);
			ValueExpression ve = (ValueExpression) expr;
			T t = mo.type.assume(ve, src, dest, mo);
			if (t.isBottom())
				return bottom();
			V v = mo.value.assume(ve, src, dest, mo);
			if (v.isBottom())
				return bottom();
			return new SimpleAbstractState<>(mo.heap, v, t);
		}

		T typeRes = mo.type.bottom();
		V valueRes = mo.value.bottom();
		for (SymbolicExpression expr : exprs) {
			if (!(expr instanceof ValueExpression))
				throw new SemanticException("Rewriting failed for expression " + expr);
			ValueExpression ve = (ValueExpression) expr;
			T t = mo.type.assume(ve, src, dest, mo);
			V v = mo.value.assume(ve, src, dest, mo);
			typeRes = typeRes.lub(t);
			valueRes = valueRes.lub(v);
		}

		if (typeRes.isBottom() || valueRes.isBottom())
			return bottom();

		return new SimpleAbstractState<>(mo.heap, valueRes, typeRes);
	}

	@Override
	public Satisfiability satisfies(
			SymbolicExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		Satisfiability heapsat = heapState.satisfies(expression, pp, this);
		if (heapsat == Satisfiability.BOTTOM)
			return Satisfiability.BOTTOM;

		if (!expression.mightNeedRewriting()) {
			ValueExpression ve = (ValueExpression) expression;
			Satisfiability typesat = typeState.satisfies(ve, pp, this);
			if (typesat == Satisfiability.BOTTOM)
				return Satisfiability.BOTTOM;
			Satisfiability valuesat = valueState.satisfies(ve, pp, this);
			if (valuesat == Satisfiability.BOTTOM)
				return Satisfiability.BOTTOM;
			return heapsat.glb(typesat).glb(valuesat);
		}

		ExpressionSet exprs = heapState.rewrite(expression, pp, this);
		if (exprs.isEmpty())
			return Satisfiability.BOTTOM;

		if (exprs.elements.size() == 1) {
			SymbolicExpression expr = exprs.elements.iterator().next();
			if (!(expr instanceof ValueExpression))
				throw new SemanticException("Rewriting failed for expression " + expr);
			ValueExpression ve = (ValueExpression) expression;
			Satisfiability typesat = typeState.satisfies(ve, pp, this);
			if (typesat == Satisfiability.BOTTOM)
				return Satisfiability.BOTTOM;
			Satisfiability valuesat = valueState.satisfies(ve, pp, this);
			if (valuesat == Satisfiability.BOTTOM)
				return Satisfiability.BOTTOM;
			return heapsat.glb(typesat).glb(valuesat);
		}

		Satisfiability typesat = Satisfiability.BOTTOM;
		Satisfiability valuesat = Satisfiability.BOTTOM;
		for (SymbolicExpression expr : exprs) {
			if (!(expr instanceof ValueExpression))
				throw new SemanticException("Rewriting failed for expression " + expr);
			ValueExpression ve = (ValueExpression) expr;
			Satisfiability sat = typeState.satisfies(ve, pp, this);
			if (sat == Satisfiability.BOTTOM)
				return sat;
			typesat = typesat.lub(sat);

			sat = valueState.satisfies(ve, pp, this);
			if (sat == Satisfiability.BOTTOM)
				return sat;
			valuesat = valuesat.lub(sat);
		}
		return heapsat.glb(typesat).glb(valuesat);
	}

	@Override
	public SimpleAbstractState<H, V, T> pushScope(
			ScopeToken scope)
			throws SemanticException {
		return new SimpleAbstractState<>(
				heapState.pushScope(scope),
				valueState.pushScope(scope),
				typeState.pushScope(scope));
	}

	@Override
	public SimpleAbstractState<H, V, T> popScope(
			ScopeToken scope)
			throws SemanticException {
		return new SimpleAbstractState<>(
				heapState.popScope(scope),
				valueState.popScope(scope),
				typeState.popScope(scope));
	}

	@Override
	public SimpleAbstractState<H, V, T> lubAux(
			SimpleAbstractState<H, V, T> other)
			throws SemanticException {
		return new SimpleAbstractState<>(
				heapState.lub(other.heapState),
				valueState.lub(other.valueState),
				typeState.lub(other.typeState));
	}

	@Override
	public SimpleAbstractState<H, V, T> glbAux(
			SimpleAbstractState<H, V, T> other)
			throws SemanticException {
		return new SimpleAbstractState<>(
				heapState.glb(other.heapState),
				valueState.glb(other.valueState),
				typeState.glb(other.typeState));
	}

	@Override
	public SimpleAbstractState<H, V, T> wideningAux(
			SimpleAbstractState<H, V, T> other)
			throws SemanticException {
		return new SimpleAbstractState<>(
				heapState.widening(other.heapState),
				valueState.widening(other.valueState),
				typeState.widening(other.typeState));
	}

	@Override
	public SimpleAbstractState<H, V, T> narrowingAux(
			SimpleAbstractState<H, V, T> other)
			throws SemanticException {
		return new SimpleAbstractState<>(
				heapState.narrowing(other.heapState),
				valueState.narrowing(other.valueState),
				typeState.narrowing(other.typeState));
	}

	@Override
	public boolean lessOrEqualAux(
			SimpleAbstractState<H, V, T> other)
			throws SemanticException {
		return heapState.lessOrEqual(other.heapState)
				&& valueState.lessOrEqual(other.valueState)
				&& typeState.lessOrEqual(other.typeState);
	}

	@Override
	public SimpleAbstractState<H, V, T> top() {
		return new SimpleAbstractState<>(heapState.top(), valueState.top(), typeState.top());
	}

	@Override
	public SimpleAbstractState<H, V, T> bottom() {
		return new SimpleAbstractState<>(heapState.bottom(), valueState.bottom(), typeState.bottom());
	}

	@Override
	public boolean isTop() {
		return heapState.isTop() && valueState.isTop() && typeState.isTop();
	}

	@Override
	public boolean isBottom() {
		return heapState.isBottom() && valueState.isBottom() && typeState.isBottom();
	}

	@Override
	public SimpleAbstractState<H, V, T> forgetIdentifier(
			Identifier id)
			throws SemanticException {
		return new SimpleAbstractState<>(
				heapState.forgetIdentifier(id),
				valueState.forgetIdentifier(id),
				typeState.forgetIdentifier(id));
	}

	@Override
	public SimpleAbstractState<H, V, T> forgetIdentifiersIf(
			Predicate<Identifier> test)
			throws SemanticException {
		return new SimpleAbstractState<>(
				heapState.forgetIdentifiersIf(test),
				valueState.forgetIdentifiersIf(test),
				typeState.forgetIdentifiersIf(test));
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((heapState == null) ? 0 : heapState.hashCode());
		result = prime * result + ((valueState == null) ? 0 : valueState.hashCode());
		result = prime * result + ((typeState == null) ? 0 : typeState.hashCode());
		return result;
	}

	@Override
	public boolean equals(
			Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SimpleAbstractState<?, ?, ?> other = (SimpleAbstractState<?, ?, ?>) obj;
		if (heapState == null) {
			if (other.heapState != null)
				return false;
		} else if (!heapState.equals(other.heapState))
			return false;
		if (valueState == null) {
			if (other.valueState != null)
				return false;
		} else if (!valueState.equals(other.valueState))
			return false;
		if (typeState == null) {
			if (other.typeState != null)
				return false;
		} else if (!typeState.equals(other.typeState))
			return false;
		return true;
	}

	@Override
	public StructuredRepresentation representation() {
		if (isBottom())
			return Lattice.bottomRepresentation();
		if (isTop())
			return Lattice.topRepresentation();

		StructuredRepresentation h = heapState.representation();
		StructuredRepresentation t = typeState.representation();
		StructuredRepresentation v = valueState.representation();
		return new ObjectRepresentation(Map.of(
				HEAP_REPRESENTATION_KEY, h,
				TYPE_REPRESENTATION_KEY, t,
				VALUE_REPRESENTATION_KEY, v));
	}

	@Override
	public String toString() {
		return representation().toString();
	}

	@Override
	public <D extends SemanticDomain<?, ?, ?>> Collection<D> getAllDomainInstances(
			Class<D> domain) {
		Collection<D> result = AbstractState.super.getAllDomainInstances(domain);
		result.addAll(heapState.getAllDomainInstances(domain));
		result.addAll(typeState.getAllDomainInstances(domain));
		result.addAll(valueState.getAllDomainInstances(domain));
		return result;
	}

	@Override
	public ExpressionSet rewrite(
			SymbolicExpression expression,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (!expression.mightNeedRewriting())
			return new ExpressionSet(expression);
		return heapState.rewrite(expression, pp, oracle);
	}

	@Override
	public ExpressionSet rewrite(
			ExpressionSet expressions,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return heapState.rewrite(expressions, pp, oracle);
	}

	@Override
	public Set<Type> getRuntimeTypesOf(
			SymbolicExpression e,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return typeState.getRuntimeTypesOf(e, pp, oracle);
	}

	@Override
	public Type getDynamicTypeOf(
			SymbolicExpression e,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return typeState.getDynamicTypeOf(e, pp, oracle);
	}

	@Override
	public boolean knowsIdentifier(
			Identifier id) {
		return heapState.knowsIdentifier(id) || valueState.knowsIdentifier(id) || typeState.knowsIdentifier(id);
	}

	@Override
	public SimpleAbstractState<H, V, T> withTopMemory() {
		return new SimpleAbstractState<>(heapState.top(), valueState, typeState);
	}

	@Override
	public SimpleAbstractState<H, V, T> withTopValues() {
		return new SimpleAbstractState<>(heapState, valueState.top(), typeState);
	}

	@Override
	public SimpleAbstractState<H, V, T> withTopTypes() {
		return new SimpleAbstractState<>(heapState, valueState, typeState.top());
	}

	private static class MutableOracle<H extends HeapDomain<H>,
			V extends ValueDomain<V>,
			T extends TypeDomain<T>> implements SemanticOracle {

		private H heap;
		private V value;
		private T type;

		public MutableOracle(
				H heap,
				V value,
				T type) {
			this.heap = heap;
			this.value = value;
			this.type = type;
		}

		@Override
		public ExpressionSet rewrite(
				SymbolicExpression expression,
				ProgramPoint pp,
				SemanticOracle oracle)
				throws SemanticException {
			if (!expression.mightNeedRewriting())
				return new ExpressionSet(expression);
			return heap.rewrite(expression, pp, this);
		}

		@Override
		public Set<Type> getRuntimeTypesOf(
				SymbolicExpression e,
				ProgramPoint pp,
				SemanticOracle oracle)
				throws SemanticException {
			return type.getRuntimeTypesOf(e, pp, this);
		}

		@Override
		public Type getDynamicTypeOf(
				SymbolicExpression e,
				ProgramPoint pp,
				SemanticOracle oracle)
				throws SemanticException {
			return type.getDynamicTypeOf(e, pp, this);
		}

		@Override
		public String toString() {
			if (heap.isBottom() || type.isBottom() || value.isBottom())
				return Lattice.bottomRepresentation().toString();
			if (heap.isTop() && type.isTop() && value.isTop())
				return Lattice.topRepresentation().toString();

			StructuredRepresentation h = heap.representation();
			StructuredRepresentation t = type.representation();
			StructuredRepresentation v = value.representation();
			return new ObjectRepresentation(Map.of(
					HEAP_REPRESENTATION_KEY, h,
					TYPE_REPRESENTATION_KEY, t,
					VALUE_REPRESENTATION_KEY, v)).toString();
		}

		@Override
		public Satisfiability alias(
				SymbolicExpression x,
				SymbolicExpression y,
				ProgramPoint pp,
				SemanticOracle oracle)
				throws SemanticException {
			return heap.alias(x, y, pp, oracle);
		}

		@Override
		public Satisfiability isReachableFrom(
				SymbolicExpression x,
				SymbolicExpression y,
				ProgramPoint pp,
				SemanticOracle oracle)
				throws SemanticException {
			return heap.isReachableFrom(x, y, pp, oracle);
		}
	}

	@Override
	public Satisfiability alias(
			SymbolicExpression x,
			SymbolicExpression y,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return heapState.alias(x, y, pp, oracle);
	}

	@Override
	public Satisfiability isReachableFrom(
			SymbolicExpression x,
			SymbolicExpression y,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		return heapState.isReachableFrom(x, y, pp, oracle);
	}
}
