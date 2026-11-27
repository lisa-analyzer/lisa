package it.unive.lisa.analysis.types;

import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;
import it.unive.lisa.type.TypeTokenType;
import java.util.HashSet;
import java.util.Set;

/**
 * The instances of the types denoted by the type tokens that an expression
 * might evaluate to, for deciding type checks (see
 * {@link it.unive.lisa.symbolic.value.operator.binary.TypeCheck}). A token
 * denotes all of its types (a value is an instance of the token if it is an
 * instance of any of them, as for the tuple of classes of Python's
 * {@code isinstance}), while several tokens are alternatives: each execution
 * evaluates the expression to one of them. A value is thus surely an instance
 * only if it is an instance under every token ({@link #ofEach}), and surely not
 * one only if it is not an instance under any of them ({@link #ofAny}).
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
final class TypeTokens {

	private TypeTokens() {
	}

	/**
	 * Yields the types that are instances of the types of some of the given
	 * tokens (the other types are ignored).
	 *
	 * @param tokens the types, among which the tokens
	 * @param types  the type system
	 *
	 * @return the instances
	 */
	static Set<Type> ofAny(
			Set<Type> tokens,
			TypeSystem types) {
		Set<Type> result = new HashSet<>();
		for (Type t : tokens)
			if (t.isTypeTokenType())
				result.addAll(instances(t.asTypeTokenType(), types));
		return result;
	}

	/**
	 * Yields the types that are instances of the types of every given token
	 * (the other types are ignored), that is, the types whose values are
	 * instances whatever token an execution evaluates to.
	 *
	 * @param tokens the types, among which the tokens
	 * @param types  the type system
	 *
	 * @return the instances
	 */
	static Set<Type> ofEach(
			Set<Type> tokens,
			TypeSystem types) {
		Set<Type> result = null;
		for (Type t : tokens)
			if (t.isTypeTokenType()) {
				Set<Type> instances = instances(t.asTypeTokenType(), types);
				if (result == null)
					result = instances;
				else
					result.retainAll(instances);
			}
		return result == null ? new HashSet<>() : result;
	}

	private static Set<Type> instances(
			TypeTokenType token,
			TypeSystem types) {
		Set<Type> result = new HashSet<>();
		for (Type t : token.getTypes())
			result.addAll(t.allInstances(types));
		return result;
	}
}
