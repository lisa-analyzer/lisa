package it.unive.lisa.symbolic.value;

import it.unive.lisa.cfg.type.Type;
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
	 * Builds the identifier.
	 * 
	 * @param types the runtime types of this expression
	 * @param name  the name of the identifier
	 */
	protected Identifier(ExternalSet<Type> types, String name) {
		super(types);
		this.name = name;
	}

	/**
	 * Yields the name of this identifier.
	 * 
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
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
