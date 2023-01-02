package it.unive.lisa.analysis.string.fsa;

import it.unive.lisa.util.datastructures.automaton.TransitionSymbol;

/**
 * A {@link TransitionSymbol} for single characters, represented as strings for
 * simple modeling of epsilon.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class StringSymbol implements TransitionSymbol<StringSymbol> {

	/**
	 * Singleton symbol for the epsilon.
	 */
	public static final StringSymbol EPSILON = new StringSymbol("");

	private final String symbol;

	/**
	 * Builds the symbol for the given string. Note that, even if instances of
	 * this class should represent single characters, having a string enables
	 * modeling of epsilon.
	 * 
	 * @param symbol the string
	 */
	public StringSymbol(String symbol) {
		this.symbol = symbol;
	}

	/**
	 * Builds the symbol for the given character.
	 * 
	 * @param symbol the character
	 */
	public StringSymbol(char symbol) {
		this.symbol = Character.toString(symbol);
	}

	@Override
	public int compareTo(StringSymbol o) {
		return symbol.compareTo(o.symbol);
	}

	@Override
	public boolean isEpsilon() {
		return this == EPSILON || symbol.isEmpty();
	}

	@Override
	public StringSymbol reverse() {
		return isEpsilon() ? this : new StringSymbol(new StringBuilder(symbol).reverse().toString());
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((symbol == null) ? 0 : symbol.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		StringSymbol other = (StringSymbol) obj;
		if (symbol == null) {
			if (other.symbol != null)
				return false;
		} else if (!symbol.equals(other.symbol))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return symbol;
	}

	/**
	 * Yields the string represented by this symbol.
	 * 
	 * @return the concrete string
	 */
	public String getSymbol() {
		return symbol;
	}

	/**
	 * Merges the two symbols in a unique one by joining the two inner strings.
	 * 
	 * @param other the other symbol
	 * 
	 * @return the merged (joined) symbol
	 */
	public StringSymbol concat(StringSymbol other) {
		if (isEpsilon())
			return other;
		if (other.isEpsilon())
			return this;
		return new StringSymbol(symbol + other.symbol);
	}

	@Override
	public int maxLength() {
		return symbol.length();
	}

	@Override
	public int minLength() {
		return symbol.length();
	}
}
