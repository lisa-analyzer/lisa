package it.unive.lisa.triage.encoding;

import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.triage.smt.Sort;
import it.unive.lisa.type.Type;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * A policy deciding the SMT {@link Sort} used to encode program values. Values
 * of integral numeric types are encoded as mathematical integers (overflowing
 * operators wrap their results explicitly), booleans as booleans, and strings
 * as strings. Values of any other type (e.g., floating point numbers,
 * references, characters), and values whose type is unknown or not unique, are
 * encoded through the {@link ValueSort} datatype.
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class SortPolicy {

	private final Map<Identifier, Set<Type>> types;

	private final Collection<Type> programTypes;

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
		if (type.isNumericType() && type.asNumericType().isIntegral())
			return Sort.INT;
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
		Set<Type> ts = types.get(id);
		if (ts == null || ts.isEmpty())
			return ValueSort.SORT;
		return sortOf(ts);
	}
}
