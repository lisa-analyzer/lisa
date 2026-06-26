package it.unive.lisa.lattices;

import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.BaseLattice;
import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SimpleAbstractDomain;
import it.unive.lisa.analysis.memory.MemoryDomain;
import it.unive.lisa.analysis.memory.MemoryDomain.MemoryReplacement;
import it.unive.lisa.analysis.memory.MemoryLattice;
import it.unive.lisa.analysis.type.TypeDomain;
import it.unive.lisa.analysis.type.TypeLattice;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.analysis.value.ValueLattice;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.util.representation.ObjectRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import org.apache.commons.lang3.tuple.Pair;

/**
 * An abstract state of the analysis, composed by a memory state modeling the
 * memory layout, a value state modeling values of program variables and memory
 * locations, and a type state that can give types to expressions knowing the
 * ones of variables.<br>
 * <br>
 * The interaction between memory and value/type states follows the one defined
 * <a href=
 * "https://www.sciencedirect.com/science/article/pii/S0304397516300299">in this
 * paper</a>.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <M> the type of {@link MemoryLattice} embedded in this state
 * @param <V> the type of {@link ValueLattice} embedded in this state
 * @param <T> the type of {@link TypeLattice} embedded in this state
 */
public class SimpleAbstractState<
		M extends MemoryLattice<M>,
		V extends ValueLattice<V>,
		T extends TypeLattice<T>>
		implements
		BaseLattice<SimpleAbstractState<M, V, T>>,
		AbstractLattice<SimpleAbstractState<M, V, T>> {

	/**
	 * The key that should be used to store the instance of {@link MemoryDomain}
	 * inside the {@link StructuredRepresentation} returned by
	 * {@link #representation()}.
	 */
	public static final String MEMORY_REPRESENTATION_KEY = "memory";

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
	 * The state containing information regarding memory structures.
	 */
	public final M memoryState;

	/**
	 * The state containing information regarding values of program variables
	 * and concretized memory locations.
	 */
	public final V valueState;

	/**
	 * The state containing runtime types information regarding runtime types of
	 * program variables and concretized memory locations.
	 */
	public final T typeState;

	/**
	 * Builds a new abstract state. The missing states are set to the default
	 * no-op ones (i.e., {@link SingleMemoryLattice},
	 * {@link SingleValueLattice}, and {@link SingleTypeLattice}).
	 * 
	 * @param memoryState the state containing information regarding memory
	 *                        structures
	 */
	@SuppressWarnings("unchecked")
	public SimpleAbstractState(
			M memoryState) {
		this.memoryState = memoryState;
		this.valueState = (V) SingleValueLattice.SINGLETON;
		this.typeState = (T) SingleTypeLattice.SINGLETON;
	}

	/**
	 * Builds a new abstract state. The missing states are set to the default
	 * no-op ones (i.e., {@link SingleMemoryLattice},
	 * {@link SingleValueLattice}, and {@link SingleTypeLattice}).
	 * 
	 * @param valueState the state containing information regarding values of
	 *                       program variables and concretized memory locations
	 */
	@SuppressWarnings("unchecked")
	public SimpleAbstractState(
			V valueState) {
		this.memoryState = (M) SingleMemoryLattice.SINGLETON;
		this.valueState = valueState;
		this.typeState = (T) SingleTypeLattice.SINGLETON;
	}

	/**
	 * Builds a new abstract state. The missing states are set to the default
	 * no-op ones (i.e., {@link SingleMemoryLattice},
	 * {@link SingleValueLattice}, and {@link SingleTypeLattice}).
	 * 
	 * @param typeState the state containing information regarding runtime types
	 *                      of program variables and concretized memory
	 *                      locations
	 */
	@SuppressWarnings("unchecked")
	public SimpleAbstractState(
			T typeState) {
		this.memoryState = (M) SingleMemoryLattice.SINGLETON;
		this.valueState = (V) SingleValueLattice.SINGLETON;
		this.typeState = typeState;
	}

	/**
	 * Builds a new abstract state. The missing states are set to the default
	 * no-op ones (i.e., {@link SingleMemoryLattice},
	 * {@link SingleValueLattice}, and {@link SingleTypeLattice}).
	 * 
	 * @param memoryState the state containing information regarding memory
	 *                        structures
	 * @param valueState  the state containing information regarding values of
	 *                        program variables and concretized memory locations
	 */
	@SuppressWarnings("unchecked")
	public SimpleAbstractState(
			M memoryState,
			V valueState) {
		this.memoryState = memoryState;
		this.valueState = valueState;
		this.typeState = (T) SingleTypeLattice.SINGLETON;
	}

	/**
	 * Builds a new abstract state. The missing states are set to the default
	 * no-op ones (i.e., {@link SingleMemoryLattice},
	 * {@link SingleValueLattice}, and {@link SingleTypeLattice}).
	 * 
	 * @param memoryState the state containing information regarding memory
	 *                        structures
	 * @param typeState   the state containing information regarding runtime
	 *                        types of program variables and concretized memory
	 *                        locations
	 */
	@SuppressWarnings("unchecked")
	public SimpleAbstractState(
			M memoryState,
			T typeState) {
		this.memoryState = memoryState;
		this.valueState = (V) SingleValueLattice.SINGLETON;
		this.typeState = typeState;
	}

	/**
	 * Builds a new abstract state. The missing states are set to the default
	 * no-op ones (i.e., {@link SingleMemoryLattice},
	 * {@link SingleValueLattice}, and {@link SingleTypeLattice}).
	 * 
	 * @param valueState the state containing information regarding values of
	 *                       program variables and concretized memory locations
	 * @param typeState  the state containing information regarding runtime
	 *                       types of program variables and concretized memory
	 *                       locations
	 */
	@SuppressWarnings("unchecked")
	public SimpleAbstractState(
			V valueState,
			T typeState) {
		this.memoryState = (M) SingleMemoryLattice.SINGLETON;
		this.valueState = valueState;
		this.typeState = typeState;
	}

	/**
	 * Builds a new abstract state.
	 * 
	 * @param memoryState the state containing information regarding memory
	 *                        structures
	 * @param valueState  the state containing information regarding values of
	 *                        program variables and concretized memory locations
	 * @param typeState   the state containing information regarding runtime
	 *                        types of program variables and concretized memory
	 *                        locations
	 */
	public SimpleAbstractState(
			M memoryState,
			V valueState,
			T typeState) {
		this.memoryState = memoryState;
		this.valueState = valueState;
		this.typeState = typeState;
	}

	/**
	 * Builds a new abstract state with the information contained in the given
	 * oracle.
	 * 
	 * @param mo the oracle containing the components for the state to be
	 *               created
	 */
	public SimpleAbstractState(
			SimpleAbstractDomain<M, V, T>.MutableOracle mo) {
		this(mo.memory, mo.value, mo.type);
	}

	private SimpleAbstractState<M, V, T> applySubstitution(
			List<MemoryReplacement> subs,
			ProgramPoint pp)
			throws SemanticException {
		T t = typeState;
		V v = valueState;
		if (subs != null)
			for (MemoryReplacement repl : subs) {
				t = t.applyReplacement(repl, pp);
				v = v.applyReplacement(repl, pp);
			}
		return new SimpleAbstractState<>(memoryState, v, t);
	}

	@Override
	public SimpleAbstractState<M, V, T> pushScope(
			ScopeToken scope,
			ProgramPoint pp)
			throws SemanticException {
		// it should not be necessary to apply substitutions here,
		// as we are not deleting variables and the memory locations
		// won't be masked by the scope
		return new SimpleAbstractState<>(
				memoryState.pushScope(scope, pp).getLeft(),
				valueState.pushScope(scope, pp),
				typeState.pushScope(scope, pp));
	}

	@Override
	public SimpleAbstractState<M, V, T> popScope(
			ScopeToken scope,
			ProgramPoint pp)
			throws SemanticException {
		Pair<M, List<MemoryReplacement>> memory = memoryState.popScope(scope, pp);
		SimpleAbstractState<M, V, T> subs = applySubstitution(memory.getRight(), pp);
		V v = subs.valueState.popScope(scope, pp);
		T t = subs.typeState.popScope(scope, pp);
		return new SimpleAbstractState<>(memory.getLeft(), v, t);
	}

	@Override
	public SimpleAbstractState<M, V, T> lubAux(
			SimpleAbstractState<M, V, T> other)
			throws SemanticException {
		return new SimpleAbstractState<>(
				memoryState.lub(other.memoryState),
				valueState.lub(other.valueState),
				typeState.lub(other.typeState));
	}

	@Override
	public SimpleAbstractState<M, V, T> upchainAux(
			SimpleAbstractState<M, V, T> other)
			throws SemanticException {
		return new SimpleAbstractState<>(
				memoryState.upchain(other.memoryState),
				valueState.upchain(other.valueState),
				typeState.upchain(other.typeState));
	}

	@Override
	public SimpleAbstractState<M, V, T> glbAux(
			SimpleAbstractState<M, V, T> other)
			throws SemanticException {
		return new SimpleAbstractState<>(
				memoryState.glb(other.memoryState),
				valueState.glb(other.valueState),
				typeState.glb(other.typeState));
	}

	@Override
	public SimpleAbstractState<M, V, T> downchainAux(
			SimpleAbstractState<M, V, T> other)
			throws SemanticException {
		return new SimpleAbstractState<>(
				memoryState.downchain(other.memoryState),
				valueState.downchain(other.valueState),
				typeState.downchain(other.typeState));
	}

	@Override
	public SimpleAbstractState<M, V, T> wideningAux(
			SimpleAbstractState<M, V, T> other)
			throws SemanticException {
		return new SimpleAbstractState<>(
				memoryState.widening(other.memoryState),
				valueState.widening(other.valueState),
				typeState.widening(other.typeState));
	}

	@Override
	public SimpleAbstractState<M, V, T> narrowingAux(
			SimpleAbstractState<M, V, T> other)
			throws SemanticException {
		return new SimpleAbstractState<>(
				memoryState.narrowing(other.memoryState),
				valueState.narrowing(other.valueState),
				typeState.narrowing(other.typeState));
	}

	@Override
	public boolean lessOrEqualAux(
			SimpleAbstractState<M, V, T> other)
			throws SemanticException {
		return memoryState.lessOrEqual(other.memoryState)
				&& valueState.lessOrEqual(other.valueState)
				&& typeState.lessOrEqual(other.typeState);
	}

	@Override
	public SimpleAbstractState<M, V, T> top() {
		return new SimpleAbstractState<>(memoryState.top(), valueState.top(), typeState.top());
	}

	@Override
	public SimpleAbstractState<M, V, T> bottom() {
		return new SimpleAbstractState<>(memoryState.bottom(), valueState.bottom(), typeState.bottom());
	}

	@Override
	public boolean isTop() {
		return memoryState.isTop() && valueState.isTop() && typeState.isTop();
	}

	@Override
	public boolean isBottom() {
		return memoryState.isBottom() && valueState.isBottom() && typeState.isBottom();
	}

	@Override
	public SimpleAbstractState<M, V, T> forgetIdentifier(
			Identifier id,
			ProgramPoint pp)
			throws SemanticException {
		Pair<M, List<MemoryReplacement>> memory = memoryState.forgetIdentifier(id, pp);
		SimpleAbstractState<M, V, T> subs = applySubstitution(memory.getRight(), pp);
		V v = subs.valueState.forgetIdentifier(id, pp);
		T t = subs.typeState.forgetIdentifier(id, pp);
		return new SimpleAbstractState<>(memory.getLeft(), v, t);
	}

	@Override
	public SimpleAbstractState<M, V, T> forgetIdentifiers(
			Iterable<Identifier> ids,
			ProgramPoint pp)
			throws SemanticException {
		Pair<M, List<MemoryReplacement>> memory = memoryState.forgetIdentifiers(ids, pp);
		SimpleAbstractState<M, V, T> subs = applySubstitution(memory.getRight(), pp);
		V v = subs.valueState.forgetIdentifiers(ids, pp);
		T t = subs.typeState.forgetIdentifiers(ids, pp);
		return new SimpleAbstractState<>(memory.getLeft(), v, t);
	}

	@Override
	public SimpleAbstractState<M, V, T> forgetIdentifiersIf(
			Predicate<Identifier> test,
			ProgramPoint pp)
			throws SemanticException {
		Pair<M, List<MemoryReplacement>> memory = memoryState.forgetIdentifiersIf(test, pp);
		SimpleAbstractState<M, V, T> subs = applySubstitution(memory.getRight(), pp);
		V v = subs.valueState.forgetIdentifiersIf(test, pp);
		T t = subs.typeState.forgetIdentifiersIf(test, pp);
		return new SimpleAbstractState<>(memory.getLeft(), v, t);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((memoryState == null) ? 0 : memoryState.hashCode());
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
		if (memoryState == null) {
			if (other.memoryState != null)
				return false;
		} else if (!memoryState.equals(other.memoryState))
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

		StructuredRepresentation m = memoryState.representation();
		StructuredRepresentation t = typeState.representation();
		StructuredRepresentation v = valueState.representation();
		return new ObjectRepresentation(
				Map.of(MEMORY_REPRESENTATION_KEY, m, TYPE_REPRESENTATION_KEY, t, VALUE_REPRESENTATION_KEY, v));
	}

	@Override
	public String toString() {
		return representation().toString();
	}

	@Override
	public boolean knowsIdentifier(
			Identifier id) {
		return memoryState.knowsIdentifier(id) || valueState.knowsIdentifier(id) || typeState.knowsIdentifier(id);
	}

	@Override
	public SimpleAbstractState<M, V, T> withTopMemory() {
		return new SimpleAbstractState<>(memoryState.top(), valueState, typeState);
	}

	@Override
	public SimpleAbstractState<M, V, T> withTopValues() {
		return new SimpleAbstractState<>(memoryState, valueState.top(), typeState);
	}

	@Override
	public SimpleAbstractState<M, V, T> withTopTypes() {
		return new SimpleAbstractState<>(memoryState, valueState, typeState.top());
	}

	@Override
	public <D extends Lattice<D>> Collection<D> getAllLatticeInstances(
			Class<D> lattice) {
		Collection<D> result = AbstractLattice.super.getAllLatticeInstances(lattice);
		result.addAll(memoryState.getAllLatticeInstances(lattice));
		result.addAll(typeState.getAllLatticeInstances(lattice));
		result.addAll(valueState.getAllLatticeInstances(lattice));
		return result;
	}

}
