package it.unive.lisa.symbolic.value;

import it.unive.lisa.caches.Caches;
import it.unive.lisa.type.Type;

/**
 * A constant value.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class Constant extends ValueExpression {

	/**
	 * The constant
	 */
	private final Object value;

	/**
	 * Builds the constant.
	 * 
	 * @param type  the type of the constant
	 * @param value the constant value
	 */
	public Constant(Type type, Object value) {
		super(Caches.types().mkSingletonSet(type));
		this.value = value;
	}

	/**
	 * Yields the constant value.
	 * 
	 * @return the value
	 */
	public Object getValue() {
		return value;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((value == null) ? 0 : value.hashCode());
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
		Constant other = (Constant) obj;
		if (value == null) {
			if (other.value != null)
				return false;
		} else if (!value.equals(other.value))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return value.toString();
	}
}
