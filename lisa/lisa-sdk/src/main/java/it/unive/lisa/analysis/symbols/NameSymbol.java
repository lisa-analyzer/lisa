package it.unive.lisa.analysis.symbols;

/**
 * A {@link Symbol} that represents a name (e.g. the name of a code member).
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class NameSymbol implements Symbol {

	private final String name;

	/**
	 * Builds the symbol.
	 * 
	 * @param name the name represented by this symbol
	 */
	public NameSymbol(
			String name) {
		this.name = name;
	}

	/**
	 * Yields the name represented by this symbol.
	 * 
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
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
		NameSymbol other = (NameSymbol) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "<qualifier>::" + name;
	}
}
