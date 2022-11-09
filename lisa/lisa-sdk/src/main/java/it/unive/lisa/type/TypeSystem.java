package it.unive.lisa.type;

import it.unive.lisa.program.Program;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * A type system, knowing about the types that can appear in a {@link Program}.
 * Types have to be registered through {@link #registerType(Type)} before the
 * analysis begins for them to be known to the system, and consequently to the
 * rest of the analysis.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public abstract class TypeSystem {

	/**
	 * The types registered in this type system, indexed by name
	 */
	private final Map<String, Type> types;

	/**
	 * Builds an empty type system, where only {@link #getBooleanType()},
	 * {@link #getStringType()} and {@link #getIntegerType()} are registered.
	 */
	protected TypeSystem() {
		this.types = new TreeMap<String, Type>();
	}

	/**
	 * Yields the collection of {@link Type}s that have been registered in this
	 * type system.
	 * 
	 * @return the collection of types
	 */
	public Set<Type> getTypes() {
		return new HashSet<>(types.values());
	}

	/**
	 * Yields the {@link Type} instance with the given name.
	 * 
	 * @param name the name of the type
	 * 
	 * @return the type, or {@code null}
	 */
	public Type getType(String name) {
		return types.get(name);
	}

	/**
	 * Registers a new {@link Type} that appears in the program.
	 * 
	 * @param type the type to add
	 * 
	 * @return {@code true} if there was no type with the given name,
	 *             {@code false} otherwise. If this method returns
	 *             {@code false}, the given type is discarded.
	 */
	public final boolean registerType(Type type) {
		return types.putIfAbsent(type.toString(), type) == null;
	}

	/**
	 * Simulates a cast operation, where an expression with possible runtime
	 * types {@code types} is being cast to one of the possible type tokens in
	 * {@code tokens}. All types in {@code tokens} that are not
	 * {@link TypeTokenType}s, according to {@link Type#isTypeTokenType()}, will
	 * be ignored. The returned set contains a subset of the types in
	 * {@code types}, keeping only the ones that can be assigned to one of the
	 * types represented by one of the tokens in {@code tokens}.<br>
	 * <br>
	 * Invoking this method is equivalent to invoking
	 * {@link #cast(Set, Set, AtomicBoolean)} passing {@code null} as third
	 * parameter.
	 * 
	 * @param types  the types of the expression being casted
	 * @param tokens the tokens representing the operand of the cast
	 * 
	 * @return the set of possible types after the cast
	 */
	public Set<Type> cast(Set<Type> types, Set<Type> tokens) {
		return cast(types, tokens, null);
	}

	/**
	 * Simulates a cast operation, where an expression with possible runtime
	 * types {@code types} is being cast to one of the possible type tokens in
	 * {@code tokens}. All types in {@code tokens} that are not
	 * {@link TypeTokenType}s, according to {@link Type#isTypeTokenType()}, will
	 * be ignored. The returned set contains a subset of the types in
	 * {@code types}, keeping only the ones that can be assigned to one of the
	 * types represented by one of the tokens in {@code tokens}.
	 * 
	 * @param types     the types of the expression being casted
	 * @param tokens    the tokens representing the operand of the cast
	 * @param mightFail a reference to the boolean to set if this cast might
	 *                      fail (e.g., casting an [int, string] to an int will
	 *                      yield a set containing int and will set the boolean
	 *                      to true)
	 * 
	 * @return the set of possible types after the cast
	 */
	public Set<Type> cast(Set<Type> types, Set<Type> tokens, AtomicBoolean mightFail) {
		if (mightFail != null)
			mightFail.set(false);

		Set<Type> result = new HashSet<>();
		Set<Type> filtered = tokens.stream().filter(Type::isTypeTokenType)
				.flatMap(t -> t.asTypeTokenType().getTypes().stream())
				.collect(Collectors.toSet());
		for (Type token : filtered)
			for (Type t : types)
				if (t.canBeAssignedTo(token))
					result.add(t);
				else if (mightFail != null)
					mightFail.set(true);

		return result;
	}

	/**
	 * Simulates a conversion operation, where an expression with possible
	 * runtime types {@code types} is being converted to one of the possible
	 * type tokens in {@code tokens}. All types in {@code tokens} that are not
	 * {@link TypeTokenType}s, according to {@link Type#isTypeTokenType()}, will
	 * be ignored. The returned set contains a subset of the types in
	 * {@code tokens}, keeping only the ones such that there exists at least one
	 * type in {@code types} that can be assigned to it.
	 * 
	 * @param types  the types of the expression being converted
	 * @param tokens the tokens representing the operand of the type conversion
	 * 
	 * @return the set of possible types after the type conversion
	 */
	public Set<Type> convert(Set<Type> types, Set<Type> tokens) {
		Set<Type> result = new HashSet<>();
		Set<Type> filtered = tokens.stream().filter(Type::isTypeTokenType)
				.flatMap(t -> t.asTypeTokenType().getTypes().stream())
				.collect(Collectors.toSet());

		for (Type token : filtered)
			for (Type t : types)
				if (t.canBeAssignedTo(token))
					result.add(token);

		return result;
	}

	/**
	 * Yields the {@link BooleanType} of this type system.
	 * 
	 * @return the boolean type
	 */
	public abstract BooleanType getBooleanType();

	/**
	 * Yields the {@link StringType} of this type system.
	 * 
	 * @return the string type
	 */
	public abstract StringType getStringType();

	/**
	 * Yields the integer type of this type system, that is, the integral
	 * {@link NumericType} that is typically used to index array elements and
	 * performing string operations.
	 * 
	 * @return the integer type
	 */
	public abstract NumericType getIntegerType();

	/**
	 * Yields whether or not values of the given type can be referenced, that
	 * is, if a pointer to memory locations containing them can be created. If
	 * this method returns {@code true}, LiSA will automatically register a
	 * {@link ReferenceType} into this type system that contains the given type.
	 *
	 * @param type the type to check
	 * 
	 * @return {@code true} if and only if the given type can be referenced
	 */
	public abstract boolean canBeReferenced(Type type);
}
