package it.unive.lisa.symbolic.heap;

import it.unive.lisa.cfg.type.Type;

/**
 * A reference to a memory location, identified by its name.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class HeapReference extends HeapExpression {

	/**
	 * The name representing the memory location
	 */
	private final String name;

	/**
	 * Builds the heap reference.
	 * 
	 * @param type the runtime type of this expression
	 * @param name the name that identifies the memory location
	 */
	public HeapReference(Type type, String name) {
		super(type);
		this.name = name;
	}

	/**
	 * Yields the name that identifies the memory location.
	 * 
	 * @return the name
	 */
	public final String getName() {
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
		HeapReference other = (HeapReference) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "heap$" + name;
	}
}
