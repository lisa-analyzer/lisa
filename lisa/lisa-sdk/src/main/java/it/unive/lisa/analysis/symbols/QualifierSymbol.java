package it.unive.lisa.analysis.symbols;

/**
 * A {@link Symbol} that represents a qualifier (i.e. the name of a unit).
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class QualifierSymbol implements Symbol {

	private final String qualifier;

	/**
	 * Builds the symbol.
	 * 
	 * @param qualifier the qualifier represented by this symbol
	 */
	public QualifierSymbol(
			String qualifier) {
		this.qualifier = qualifier;
	}

	/**
	 * Yields the qualifier represented by this symbol.
	 * 
	 * @return the qualifier
	 */
	public String getQualifier() {
		return qualifier;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
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
		QualifierSymbol other = (QualifierSymbol) obj;
		if (qualifier == null) {
			if (other.qualifier != null)
				return false;
		} else if (!qualifier.equals(other.qualifier))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return qualifier + "::<name>";
	}
}
