package it.unive.lisa.triage.encoding;

import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.triage.model.HeapType;
import it.unive.lisa.triage.smt.Sort;
import it.unive.lisa.type.Type;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * A policy deciding the SMT {@link Sort} used to encode program values. Values
 * of integral numeric types are encoded as mathematical integers (overflowing
 * operators wrap their results explicitly), booleans as booleans, strings as
 * strings, and floating point numbers of 32 or 64 bits as IEEE 754 floating
 * point numbers. Values of any other type (e.g., references), and values whose
 * type is unknown or not unique, are encoded through the {@link ValueSort}
 * datatype.
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class SortPolicy {

	private final Map<Identifier, Set<Type>> types;

	private final Collection<Type> programTypes;

	private final boolean floats;

	/**
	 * Builds the policy, with no knowledge of the types of the program.
	 *
	 * @param types the runtime types that each identifier might have (an
	 *                  identifier with no types, or that is not in the map, is
	 *                  encoded through {@link ValueSort})
	 */
	public SortPolicy(
			Map<Identifier, Set<Type>> types) {
		this(types, null);
	}

	/**
	 * Builds the policy.
	 *
	 * @param types        the runtime types that each identifier might have (an
	 *                         identifier with no types, or that is not in the
	 *                         map, is encoded through {@link ValueSort})
	 * @param programTypes all the types of the program, used to decide whether
	 *                         a type covers all the values of a sort (can be
	 *                         {@code null} if unknown)
	 */
	public SortPolicy(
			Map<Identifier, Set<Type>> types,
			Collection<Type> programTypes) {
		this(types, programTypes, true);
	}

	private SortPolicy(
			Map<Identifier, Set<Type>> types,
			Collection<Type> programTypes,
			boolean floats) {
		this.floats = floats;
		this.types = types;
		this.programTypes = programTypes;
	}

	/**
	 * Yields {@code true} if every value encoded with the given sort has a type
	 * that can be assigned to the given one, that is, if all the types of the
	 * program encoded with that sort can be assigned to the given one (e.g., if
	 * {@code int} is the only integral type of the program, it covers all
	 * integer values). Yields {@code false} if the types of the program are
	 * unknown.
	 *
	 * @param target the type
	 * @param sort   the sort, that must not be {@link ValueSort#SORT}
	 *
	 * @return whether or not the type covers the sort
	 */
	public boolean covers(
			Type target,
			Sort sort) {
		if (programTypes == null)
			return false;
		boolean any = false;
		for (Type t : programTypes)
			if (sortOf(t).equals(sort)) {
				if (!t.canBeAssignedTo(target))
					return false;
				any = true;
			}
		return any;
	}

	/**
	 * Yields the sort used to encode values of the given type.
	 *
	 * @param type the type
	 *
	 * @return the sort
	 */
	public Sort sortOf(
			Type type) {
		if (type instanceof HeapType)
			return ((HeapType) type).getSort();
		if (type.isNumericType() && type.asNumericType().isIntegral())
			return Sort.INT;
		if (floats && type.isNumericType() && type.asNumericType().is32Bits())
			return Sort.FLOAT32;
		if (floats && type.isNumericType() && type.asNumericType().is64Bits())
			return Sort.FLOAT64;
		if (type.isBooleanType())
			return Sort.BOOL;
		if (type.isStringType())
			return Sort.STRING;
		return ValueSort.SORT;
	}

	/**
	 * Yields the sort used to encode values of any of the given types, that is,
	 * the sort of all of them if they agree, or {@link ValueSort#SORT}
	 * otherwise.
	 *
	 * @param types the types
	 *
	 * @return the sort
	 */
	public Sort sortOf(
			Collection<Type> types) {
		Sort sort = null;
		for (Type t : types) {
			Sort s = sortOf(t);
			if (sort == null)
				sort = s;
			else if (!sort.equals(s))
				return ValueSort.SORT;
		}
		return sort == null ? ValueSort.SORT : sort;
	}

	/**
	 * Yields the sort used to encode the values of the given identifier.
	 *
	 * @param id the identifier
	 *
	 * @return the sort
	 */
	public Sort sortOf(
			Identifier id) {
		if (id.getStaticType() instanceof HeapType)
			return ((HeapType) id.getStaticType()).getSort();
		Set<Type> ts = types.get(id);
		if (ts == null || ts.isEmpty())
			return ValueSort.SORT;
		return sortOf(ts);
	}

	/**
	 * Yields a policy deciding the same sorts as this one, except that floating
	 * point numbers are not encoded as such, but as opaque values of the
	 * {@link ValueSort} datatype (whose meaning is not modeled), and the
	 * constructors of floating point values of {@link ValueSort} are not used.
	 *
	 * @return the policy
	 */
	public SortPolicy withoutFloats() {
		return new SortPolicy(types, programTypes, false);
	}

	/**
	 * Yields {@code true} if floating point numbers are encoded as such (see
	 * {@link #withoutFloats()}).
	 *
	 * @return whether or not floating point numbers are encoded
	 */
	public boolean encodesFloats() {
		return floats;
	}

	/**
	 * Yields {@code true} if the given identifier might hold floating point
	 * numbers, according to its runtime types.
	 *
	 * @param id the identifier
	 *
	 * @return whether or not the identifier might hold floating point numbers
	 */
	public boolean mayBeFloat(
			Identifier id) {
		Set<Type> ts = types.get(id);
		return ts != null && ts.stream().anyMatch(SortPolicy::isFloat);
	}

	/**
	 * Yields {@code true} if the given identifier only holds floating point
	 * numbers, according to its runtime types (or to its static type, if they
	 * are not known).
	 *
	 * @param id the identifier
	 *
	 * @return whether or not the identifier only holds floating point numbers
	 */
	public boolean holdsOnlyFloats(
			Identifier id) {
		Set<Type> ts = types.get(id);
		if (ts == null || ts.isEmpty())
			return isFloat(id.getStaticType());
		return ts.stream().allMatch(SortPolicy::isFloat);
	}

	/**
	 * Yields {@code true} if the given type is a floating point type.
	 *
	 * @param type the type
	 *
	 * @return whether or not the type is a floating point type
	 */
	public static boolean isFloat(
			Type type) {
		return type.isNumericType() && !type.asNumericType().isIntegral();
	}

	/**
	 * Yields {@code true} if the given expression might be encoded as an opaque
	 * value of the {@link ValueSort} datatype, that is, if its type is unknown,
	 * or if it is a floating point type.
	 *
	 * @param expression the expression
	 *
	 * @return whether or not the expression might be encoded as an opaque value
	 */
	public boolean mayBeOpaque(
			SymbolicExpression expression) {
		if (expression instanceof Identifier) {
			Set<Type> ts = types.get(expression);
			if (ts != null && !ts.isEmpty())
				return ts.stream().anyMatch(SortPolicy::mayBeOpaque);
		}
		return mayBeOpaque(expression.getStaticType());
	}

	private static boolean mayBeOpaque(
			Type type) {
		return type.isUntyped() || (type.isNumericType() && !type.asNumericType().isIntegral());
	}
}
