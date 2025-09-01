package it.unive.lisa.analysis.continuations;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.type.Type;
import it.unive.lisa.util.representation.MapRepresentation;
import it.unive.lisa.util.representation.SetRepresentation;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;

/**
 * A {@link Continuation} that represents an erroneous state caused by one or
 * more exceptions of a set of possible types being raised. This is usually used
 * to smash the states corresponding to exceptions that are either not
 * interesting or that are thrown several times for each analysis, causing a lot
 * of noise.
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class Exceptions
		extends
		Continuation {

	private final Map<Type, Set<Statement>> types;

	/**
	 * Builds a new continuation for the given exception type.
	 * 
	 * @param type the exception type
	 * @param thrower the statement that threw the exception
	 */
	public Exceptions(
			Type type,
			Statement thrower) {
		this.types = Collections.singletonMap(type, Collections.singleton(thrower));
	}

	private Exceptions(
			Map<Type, Set<Statement>> types) {
		this.types = Collections.unmodifiableMap(types);
	}

	/**
	 * Yields the exception types associated with this continuation, each mapped to the statements that threw them.
	 * 
	 * @return the exception types
	 */
	public Map<Type, Set<Statement>> getTypes() {
		return types;
	}

	/**
	 * Yields a new continuation that is equal to this one but with the given
	 * type and thrower added.
	 * 
	 * @param type the type to add
	 * @param thrower the statement that threw the exception
	 * 
	 * @return a new continuation with the added type and thrower if
	 *         the type was not already present, otherwise a new continuation with
	 *         thrower added to the existing type
	 */
	public Exceptions add(
			Type type,
			Statement thrower) {
		Map<Type, Set<Statement>> newTypes = new HashMap<>(types);
		newTypes.computeIfAbsent(type, k -> new HashSet<>()).add(thrower);
		return types.equals(newTypes) ? this : new Exceptions(newTypes);
	}

	/**
	 * Yields a new continuation that is equal to this one but with the given
	 * types and throwers added.
	 * 
	 * @param types the types to add
	 * 
	 * @return a new continuation with the added types, where each one is either
	 * added to the mapping with the given throwers if not already present, and merged with
	 * the given throwers otherwise
	 */
	public Exceptions addAll(
			Map<Type, Set<Statement>> types) {
		Map<Type, Set<Statement>> newTypes = new HashMap<>(this.types);
		types.forEach((k, v) -> newTypes.merge(k, v, (oldV, newV) -> {
			oldV.addAll(newV);
			return oldV;
		}));
		return this.types.equals(newTypes) ? this : new Exceptions(newTypes);
	}

	/**
	 * Yields a new continuation that is equal to this one but with the given
	 * type removed.
	 * 
	 * @param type the type to remove
	 * 
	 * @return a new continuation with the removed type
	 */
	public Exceptions remove(
			Type type) {
		if (!types.containsKey(type))
			return this;
		Map<Type, Set<Statement>> newTypes = new HashMap<>(types);
		newTypes.remove(type);
		return new Exceptions(newTypes);
	}

	/**
	 * Yields a new continuation that is equal to this one but with the given
	 * thrower removed for the given type.
	 * 
	 * @param type the type to remove the thrower from
	 * @param thrower the statement that threw the exception
	 * 
	 * @return a new continuation with the removed thrower
	 */
	public Exceptions removeThrower(
			Type type,
			Statement thrower) {
		if (!types.containsKey(type) || !types.get(type).contains(thrower))
			return this;
		Map<Type, Set<Statement>> newTypes = new HashMap<>(types);
		newTypes.get(type).remove(thrower);
		if (newTypes.get(type).isEmpty())
			newTypes.remove(type);
		return new Exceptions(newTypes);
	}

	/**
	 * Yields a new continuation that is equal to this one but with the given
	 * types removed.
	 * 
	 * @param types the types to remove
	 * @param throwers the statements that threw the exceptions
	 * 
	 * @return a new continuation with the removed types
	 */
	public Exceptions removeAll(
			Map<Type, Set<Statement>> types) {
		Map<Type, Set<Statement>> newTypes = new HashMap<>(this.types);
		types.forEach((k, v) -> newTypes.remove(k));
		Set<Type> toRemove = newTypes.entrySet().stream().filter(p -> p.getValue().isEmpty()).map(Map.Entry::getKey).collect(Collectors.toSet());
		toRemove.forEach(newTypes::remove);
		return this.types.equals(newTypes) ? this : new Exceptions(newTypes);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((types == null) ? 0 : types.hashCode());
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
		Exceptions other = (Exceptions) obj;
		if (types == null) {
			if (other.types != null)
				return false;
		} else if (!types.equals(other.types))
			return false;
		return true;
	}

	@Override
	public StructuredRepresentation representation() {
		return new MapRepresentation(types, StringRepresentation::new, set -> new SetRepresentation(set, StringRepresentation::new));
	}
}
