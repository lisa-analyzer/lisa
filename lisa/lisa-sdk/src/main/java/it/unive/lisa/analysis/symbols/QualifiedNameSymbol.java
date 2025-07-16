package it.unive.lisa.analysis.symbols;

/**
 * A {@link Symbol} that represents a fully qualified name, composed of both a
 * name and a qualifier.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class QualifiedNameSymbol
		implements
		Symbol {

	private final String qualifier;

	private final String name;

	/**
	 * Builds the symbol.
	 * 
	 * @param qualifier the qualifier represented by this symbol
	 * @param name      the name represented by this symbol
	 */
	public QualifiedNameSymbol(
			String qualifier,
			String name) {
		this.qualifier = qualifier;
		this.name = name;
	}

	/**
	 * Yields the qualifier represented by this symbol.
	 * 
	 * @return the qualifier
	 */
	public String getQualifier() {
		return qualifier;
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
		result = prime * result + ((qualifier == null) ? 0 : qualifier.hashCode());
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
		QualifiedNameSymbol other = (QualifiedNameSymbol) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (qualifier == null) {
			if (other.qualifier != null)
				return false;
		} else if (!qualifier.equals(other.qualifier))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return qualifier + "::" + name;
	}

}
