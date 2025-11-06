package it.unive.lisa.imp.types;

import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.interprocedural.InterproceduralAnalysis;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.statement.DefaultParamInitialization;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.type.Int32Type;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.heap.HeapReference;
import it.unive.lisa.symbolic.heap.MemoryAllocation;
import it.unive.lisa.symbolic.value.InstrumentedReceiver;
import it.unive.lisa.symbolic.value.PushAny;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.type.ReferenceType;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;
import it.unive.lisa.type.Untyped;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.tuple.Pair;

/**
 * A type representing an IMP array defined in an IMP program. ArrayTypes are
 * instances of {@link it.unive.lisa.type.ArrayType}, have a {@link Type} and a
 * dimension. To ensure uniqueness of ArrayType objects,
 * {@link #lookup(Type, int)} must be used to retrieve existing instances (or
 * automatically create one if no matching instance exists).
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public final class ArrayType
		implements
		it.unive.lisa.type.ArrayType {

	private static final Map<Pair<Type, Integer>, ArrayType> types = new HashMap<>();

	/**
	 * Clears the cache of {@link ArrayType}s created up to now.
	 */
	public static void clearAll() {
		types.clear();
	}

	/**
	 * Yields all the {@link ArrayType}s defined up to now.
	 * 
	 * @return the collection of all the array types
	 */
	public static Collection<ArrayType> all() {
		return types.values();
	}

	/**
	 * Yields a unique existing instance of {@link ArrayType} representing an
	 * array with the given {@code base} type and the given {@code dimensions}.
	 * 
	 * @param base       the base type of the array
	 * @param dimensions the number of dimensions of this array
	 * 
	 * @return the unique instance of {@link ArrayType} representing the class
	 *             with the given name
	 */
	public static ArrayType lookup(
			Type base,
			int dimensions) {
		return types.get(Pair.of(base, dimensions));
	}

	/**
	 * Yields a fresh unique instance of {@link ArrayType} representing an array
	 * with the given {@code base} type and the given {@code dimensions}, and
	 * stores it in the internal cache.
	 * 
	 * @param base       the base type of the array
	 * @param dimensions the number of dimensions of this array
	 * 
	 * @return the unique instance of {@link ArrayType} representing the class
	 *             with the given name
	 */
	public static ArrayType register(
			Type base,
			int dimensions) {
		Pair<Type, Integer> key = Pair.of(base, dimensions);
		if (types.containsKey(key))
			return types.get(key);
		ArrayType at = new ArrayType(base, dimensions);
		types.put(key, at);
		return at;
	}

	private final Type base;

	private final int dimensions;

	private ArrayType(
			Type base,
			int dimensions) {
		this.base = base;
		if (dimensions != 1)
			throw new IllegalArgumentException("Can only create an array type with 1 dimension");
		this.dimensions = dimensions;
	}

	@Override
	public final boolean canBeAssignedTo(
			Type other) {
		return other instanceof ArrayType && getInnerType().canBeAssignedTo(other.asArrayType().getInnerType());
	}

	@Override
	public Type commonSupertype(
			Type other) {
		if (canBeAssignedTo(other))
			return other;

		if (other.canBeAssignedTo(this))
			return this;

		if (other.isNullType())
			return this;

		if (!other.isArrayType())
			return Untyped.INSTANCE;

		// TODO not sure about this
		return getInnerType().commonSupertype(other.asArrayType().getInnerType());
	}

	@Override
	public String toString() {
		return base + "[]".repeat(dimensions);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((base == null) ? 0 : base.hashCode());
		result = prime * result + dimensions;
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
		ArrayType other = (ArrayType) obj;
		if (base == null) {
			if (other.base != null)
				return false;
		} else if (!base.equals(other.base))
			return false;
		if (dimensions != other.dimensions)
			return false;
		return true;
	}

	@Override
	public Type getInnerType() {
		if (dimensions == 1)
			return base;
		return lookup(base, dimensions - 1);
	}

	@Override
	public Type getBaseType() {
		return base;
	}

	@Override
	public int getDimensions() {
		return dimensions;
	}

	@Override
	public Set<Type> allInstances(
			TypeSystem types) {
		return Collections.singleton(this);
	}

	@Override
	public Expression unknownValue(
			CFG cfg,
			CodeLocation location) {
		return new DefaultParamInitialization(cfg, location, this) {

			@Override
			public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> forwardSemantics(
					AnalysisState<A> state,
					InterproceduralAnalysis<A, D> interprocedural,
					StatementStore<A> expressions)
					throws SemanticException {
				Analysis<A, D> analysis = interprocedural.getAnalysis();
				Type type = getStaticType();
				ReferenceType reftype = cfg.getProgram().getTypes().getReference(type);
				MemoryAllocation creation = new MemoryAllocation(type, getLocation(), false);
				HeapReference ref = new HeapReference(reftype, creation, getLocation());

				// we start by allocating the memory region
				AnalysisState<A> allocated = analysis.smallStepSemantics(state, creation, this);

				// we create the synthetic variable that will hold the reference
				// to the
				// newly allocated array until it is assigned to a variable
				InstrumentedReceiver array = new InstrumentedReceiver(reftype, true, getLocation());
				AnalysisState<A> tmp = analysis.assign(allocated, array, ref, this);

				// we define the length of the array as a child element
				AccessChild len = new AccessChild(
						Int32Type.INSTANCE,
						array,
						new Variable(Untyped.INSTANCE, "len", getLocation()),
						getLocation());

				// TODO fix when we'll support multidimensional arrays
				AnalysisState<
						A> lenSt = analysis.assign(tmp, len, new PushAny(Int32Type.INSTANCE, getLocation()), this);

				// we leave the synthetic array in the program variables
				// until it is popped from the stack to keep a reference to the
				// newly created array
				getMetaVariables().add(array);

				// finally, we leave a reference to the newly created array on
				// the stack
				return analysis.smallStepSemantics(lenSt, array, this);
			}

			@Override
			public <A extends AbstractLattice<A>, D extends AbstractDomain<A>> AnalysisState<A> backwardSemantics(
					AnalysisState<A> exitState,
					InterproceduralAnalysis<A, D> interprocedural,
					StatementStore<A> expressions)
					throws SemanticException {
				// TODO implement this when backward analysis will be out of
				// beta
				throw new UnsupportedOperationException();
			}

		};
	}

}
