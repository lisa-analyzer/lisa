package it.unive.lisa.symbolic.value;

import it.unive.lisa.type.Type;
import it.unive.lisa.util.collections.ExternalSet;

/**
 * An identifier of a program variable, representing either a program variable
 * (as an instance of {@link ValueIdentifier}), or a resolved memory location
 * (as an instance of {@link HeapIdentifier}).
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public abstract class Identifier extends ValueExpression {

	/**
	 * The name of the identifier
	 */
	private final String name;

	/**
	 * Whether or not this identifier is weak, meaning that it should only
	 * receive weak assignments
	 */
	private final boolean weak;

	/**
	 * Builds the identifier.
	 * 
	 * @param types the runtime types of this expression
	 * @param name  the name of the identifier
	 * @param weak  whether or not this identifier is weak, meaning that it
	 *                  should only receive weak assignments
	 */
	protected Identifier(ExternalSet<Type> types, String name, boolean weak) {
		super(types);
		this.name = name;
		this.weak = weak;
	}

	/**
	 * Yields the name of this identifier.
	 * 
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Yields whether or not this identifier is weak. Weak identifiers should
	 * only receive weak assignments, that is, the value of the identifier after
	 * the assignment should be the least upper bound between its old value and
	 * the fresh value being assigned. With strong identifiers instead, the new
	 * value corresponds to the freshly provided value.
	 * 
	 * @return {@code true} if this identifier is weak, {@code false} otherwise
	 */
	public boolean isWeak() {
		return weak;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		// we do not call super here since variables should be uniquely
		// identified by their name, regardless of their type
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		// we do not call super here since variables should be uniquely
		// identified by their name, regardless of their type
		if (getClass() != obj.getClass())
			return false;
		Identifier other = (Identifier) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}
}
