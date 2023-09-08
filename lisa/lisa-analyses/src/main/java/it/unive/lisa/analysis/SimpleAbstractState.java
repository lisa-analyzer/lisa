package it.unive.lisa.analysis;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import it.unive.lisa.analysis.heap.HeapDomain;
import it.unive.lisa.analysis.heap.HeapSemanticOperation.HeapReplacement;
import it.unive.lisa.analysis.lattices.ExpressionSet;
import it.unive.lisa.analysis.representation.DomainRepresentation;
import it.unive.lisa.analysis.representation.ObjectRepresentation;
import it.unive.lisa.analysis.value.TypeDomain;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.MemoryAllocation;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;

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
		implements BaseLattice<SimpleAbstractState<H, V, T>>,
		AbstractState<SimpleAbstractState<H, V, T>> {

	/**
	 * The key that should be used to store the instance of {@link HeapDomain}
	 * inside the {@link DomainRepresentation} returned by
	 * {@link #representation()}.
	 */
	public static final String HEAP_REPRESENTATION_KEY = "heap";

	/**
	 * The key that should be used to store the instance of {@link TypeDomain}
	 * inside the {@link DomainRepresentation} returned by
	 * {@link #representation()}.
	 */
	public static final String TYPE_REPRESENTATION_KEY = "type";

	/**
	 * The key that should be used to store the instance of {@link ValueDomain}
	 * inside the {@link DomainRepresentation} returned by
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
	public SimpleAbstractState(H heapState, V valueState, T typeState) {
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
	public SimpleAbstractState<H, V, T> assign(Identifier id, SymbolicExpression expression, ProgramPoint pp)
			throws SemanticException {
		H heap = heapState.assign(id, expression, pp);
		ExpressionSet<ValueExpression> exprs = heap.rewrite(expression, pp);

		SimpleAbstractState<H, V, T> as = applySubstitution(heap, valueState, typeState, pp);
		T type = as.getTypeState();
		V value = as.getValueState();

		T typeRes = type.bottom();
		V valueRes = value.bottom();
		for (ValueExpression expr : exprs) {
			T tmp = type.assign(id, expr, pp);

			Set<Type> rt = tmp.getRuntimeTypesOf(expr, pp);
			id.setRuntimeTypes(rt);
			expr.setRuntimeTypes(rt);

			typeRes = typeRes.lub(tmp);
			valueRes = valueRes.lub(value.assign(id, expr, pp));
		}

		return new SimpleAbstractState<>(heap, valueRes, typeRes);
	}

	@Override
	public SimpleAbstractState<H, V, T> smallStepSemantics(SymbolicExpression expression, ProgramPoint pp)
			throws SemanticException {
		H heap = heapState.smallStepSemantics(expression, pp);
		ExpressionSet<ValueExpression> exprs = heap.rewrite(expression, pp);

		SimpleAbstractState<H, V, T> as = applySubstitution(heap, valueState, typeState, pp);
		T type = as.getTypeState();
		V value = as.getValueState();

		T typeRes = type.bottom();
		V valueRes = value.bottom();
		for (ValueExpression expr : exprs) {
			T tmp = type.smallStepSemantics(expr, pp);

			Set<Type> rt = tmp.getRuntimeTypesOf(expr, pp);
			expr.setRuntimeTypes(rt);

			// if the expression is a memory allocation, its type is registered
			// in the type domain
			if (expression instanceof MemoryAllocation && expr instanceof Identifier)
				tmp = tmp.assign((Identifier) expr, expr, pp);

			typeRes = typeRes.lub(tmp);
			valueRes = valueRes.lub(value.smallStepSemantics(expr, pp));
		}

		return new SimpleAbstractState<>(heap, valueRes, typeRes);
	}

	private SimpleAbstractState<H, V, T> applySubstitution(H heap, V value, T type, ProgramPoint pp)
			throws SemanticException {
		if (heap.getSubstitution() != null && !heap.getSubstitution().isEmpty()) {
			for (HeapReplacement repl : heap.getSubstitution()) {
				Set<Type> runtimeTypes;
				Set<Type> allTypes = new HashSet<Type>();
				for (Identifier source : repl.getSources()) {
					runtimeTypes = type.smallStepSemantics(source, pp).getRuntimeTypesOf(source, pp);
					source.setRuntimeTypes(runtimeTypes);
					allTypes.addAll(runtimeTypes);
				}

				for (Identifier target : repl.getTargets())
					target.setRuntimeTypes(allTypes);

				if (repl.getSources().isEmpty())
					continue;
				T lub = type.bottom();
				for (Identifier source : repl.getSources()) {
					T partial = type;
					for (Identifier target : repl.getTargets())
						partial = partial.assign(target, source, pp);
					lub = lub.lub(partial);
				}
				type = lub.forgetIdentifiers(repl.getIdsToForget());
				value = value.applyReplacement(repl, pp);
			}
		}

		return new SimpleAbstractState<>(heap, value, type);
	}

	@Override
	public SimpleAbstractState<H, V, T> assume(SymbolicExpression expression, ProgramPoint src, ProgramPoint dest)
			throws SemanticException {
		H heap = heapState.assume(expression, src, dest);
		if (heap.isBottom())
			return bottom();

		ExpressionSet<ValueExpression> exprs = heap.rewrite(expression, src);
		SimpleAbstractState<H, V, T> as = applySubstitution(heap, valueState, typeState, src);
		T type = as.getTypeState();
		V value = as.getValueState();

		T typeRes = type.bottom();
		V valueRes = value.bottom();
		for (ValueExpression expr : exprs) {
			T tmp = type.smallStepSemantics(expr, src);
			Set<Type> rt = tmp.getRuntimeTypesOf(expr, src);
			expr.setRuntimeTypes(rt);

			typeRes = typeRes.lub(type.assume(expr, src, dest));
			valueRes = valueRes.lub(value.assume(expr, src, dest));
		}

		if (typeRes.isBottom() || valueRes.isBottom())
			return bottom();

		return new SimpleAbstractState<>(heap, valueRes, typeRes);
	}

	@Override
	public Satisfiability satisfies(SymbolicExpression expression, ProgramPoint pp) throws SemanticException {
		Satisfiability heapsat = heapState.satisfies(expression, pp);
		if (heapsat == Satisfiability.BOTTOM)
			return Satisfiability.BOTTOM;

		ExpressionSet<ValueExpression> rewritten = heapState.rewrite(expression, pp);
		Satisfiability typesat = Satisfiability.BOTTOM;
		Satisfiability valuesat = Satisfiability.BOTTOM;
		for (ValueExpression expr : rewritten) {
			T tmp = typeState.smallStepSemantics(expr, pp);
			Set<Type> rt = tmp.getRuntimeTypesOf(expr, pp);
			expr.setRuntimeTypes(rt);

			Satisfiability sat = typeState.satisfies(expr, pp);
			if (sat == Satisfiability.BOTTOM)
				return sat;
			typesat = typesat.lub(sat);

			sat = valueState.satisfies(expr, pp);
			if (sat == Satisfiability.BOTTOM)
				return sat;
			valuesat = valuesat.lub(sat);
		}
		return heapsat.glb(typesat).glb(valuesat);
	}

	@Override
	public SimpleAbstractState<H, V, T> pushScope(ScopeToken scope) throws SemanticException {
		return new SimpleAbstractState<>(
				heapState.pushScope(scope),
				valueState.pushScope(scope),
				typeState.pushScope(scope));
	}

	@Override
	public SimpleAbstractState<H, V, T> popScope(ScopeToken scope) throws SemanticException {
		return new SimpleAbstractState<>(
				heapState.popScope(scope),
				valueState.popScope(scope),
				typeState.popScope(scope));
	}

	@Override
	public SimpleAbstractState<H, V, T> lubAux(SimpleAbstractState<H, V, T> other) throws SemanticException {
		return new SimpleAbstractState<>(
				heapState.lub(other.heapState),
				valueState.lub(other.valueState),
				typeState.lub(other.typeState));
	}

	@Override
	public SimpleAbstractState<H, V, T> glbAux(SimpleAbstractState<H, V, T> other) throws SemanticException {
		return new SimpleAbstractState<>(
				heapState.glb(other.heapState),
				valueState.glb(other.valueState),
				typeState.glb(other.typeState));
	}

	@Override
	public SimpleAbstractState<H, V, T> wideningAux(SimpleAbstractState<H, V, T> other) throws SemanticException {
		return new SimpleAbstractState<>(
				heapState.widening(other.heapState),
				valueState.widening(other.valueState),
				typeState.widening(other.typeState));
	}

	@Override
	public SimpleAbstractState<H, V, T> narrowingAux(SimpleAbstractState<H, V, T> other) throws SemanticException {
		return new SimpleAbstractState<>(
				heapState.narrowing(other.heapState),
				valueState.narrowing(other.valueState),
				typeState.narrowing(other.typeState));
	}

	@Override
	public boolean lessOrEqualAux(SimpleAbstractState<H, V, T> other) throws SemanticException {
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
	public SimpleAbstractState<H, V, T> forgetIdentifier(Identifier id) throws SemanticException {
		return new SimpleAbstractState<>(
				heapState.forgetIdentifier(id),
				valueState.forgetIdentifier(id),
				typeState.forgetIdentifier(id));
	}

	@Override
	public SimpleAbstractState<H, V, T> forgetIdentifiersIf(Predicate<Identifier> test) throws SemanticException {
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
	public boolean equals(Object obj) {
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
	public DomainRepresentation representation() {
		if (isBottom())
			return Lattice.bottomRepresentation();
		if (isTop())
			return Lattice.topRepresentation();

		DomainRepresentation h = heapState.representation();
		DomainRepresentation t = typeState.representation();
		DomainRepresentation v = valueState.representation();
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
	public <D extends SemanticDomain<?, ?, ?>> Collection<D> getAllDomainInstances(Class<D> domain) {
		Collection<D> result = AbstractState.super.getAllDomainInstances(domain);
		result.addAll(heapState.getAllDomainInstances(domain));
		result.addAll(typeState.getAllDomainInstances(domain));
		result.addAll(valueState.getAllDomainInstances(domain));
		return result;
	}

	@Override
	public ExpressionSet<SymbolicExpression> rewrite(
			SymbolicExpression expression,
			ProgramPoint pp)
			throws SemanticException {
		Set<SymbolicExpression> rewritten = new HashSet<>();
		rewritten.addAll(heapState.rewrite(expression, pp).elements());
		return new ExpressionSet<>(rewritten);
	}

	@Override
	public ExpressionSet<SymbolicExpression> rewrite(
			ExpressionSet<SymbolicExpression> expressions,
			ProgramPoint pp)
			throws SemanticException {
		Set<SymbolicExpression> rewritten = new HashSet<>();
		for (SymbolicExpression expression : expressions)
			rewritten.addAll(heapState.rewrite(expression, pp).elements());
		return new ExpressionSet<>(rewritten);
	}

	@Override
	public Set<Type> getRuntimeTypesOf(SymbolicExpression e, ProgramPoint pp) throws SemanticException {
		Set<Type> types = new HashSet<>();
		for (SymbolicExpression ex : rewrite(e, pp))
			types.addAll(typeState.getRuntimeTypesOf((ValueExpression) ex, pp));
		return types;
	}

	@Override
	public Type getDynamicTypeOf(SymbolicExpression e, ProgramPoint pp) throws SemanticException {
		Set<Type> types = new HashSet<>();
		for (SymbolicExpression ex : rewrite(e, pp))
			types.add(typeState.getDynamicTypeOf((ValueExpression) ex, pp));
		if (types.isEmpty())
			return Untyped.INSTANCE;
		return Type.commonSupertype(types, Untyped.INSTANCE);
	}
}
