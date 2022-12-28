package it.unive.lisa.util.datastructures.automaton;

public class TestSymbol implements TransitionSymbol<TestSymbol> {

	private final String symbol;

	public TestSymbol(String symbol) {
		this.symbol = symbol;
	}

	@Override
	public int compareTo(TestSymbol o) {
		return symbol.compareTo(o.symbol);
	}

	@Override
	public boolean isEpsilon() {
		return symbol.isEmpty();
	}

	@Override
	public TestSymbol reverse() {
		return new TestSymbol(new StringBuilder(symbol).reverse().toString());
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
		TestSymbol other = (TestSymbol) obj;
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

	public String getSymbol() {
		return symbol;
	}

	public TestSymbol concat(TestSymbol other) {
		if (isEpsilon())
			return other;
		if (other.isEpsilon())
			return this;
		return new TestSymbol(symbol + other.symbol);
	}
}
