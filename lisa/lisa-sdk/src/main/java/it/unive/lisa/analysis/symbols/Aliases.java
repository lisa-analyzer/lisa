package it.unive.lisa.analysis.symbols;

import it.unive.lisa.analysis.lattices.SetLattice;
import it.unive.lisa.util.collections.CastIterable;
import java.util.Collections;
import java.util.Set;

public class Aliases extends SetLattice<Aliases, Symbol> {

	private static final Aliases TOP = new Aliases();
	private static final Aliases BOTTOM = new Aliases(Collections.emptySet(), false);

	private final boolean isTop;

	public Aliases() {
		this(Collections.emptySet(), true);
	}

	public Aliases(Symbol symbol) {
		this(Collections.singleton(symbol), false);
	}

	public Aliases(Set<Symbol> symbols) {
		this(symbols, false);
	}

	private Aliases(Set<Symbol> symbols, boolean isTop) {
		super(symbols);
		this.isTop = isTop;
	}

	@Override
	public Aliases top() {
		return TOP;
	}

	@Override
	public Aliases bottom() {
		return BOTTOM;
	}

	@Override
	protected Aliases mk(Set<Symbol> set) {
		return new Aliases(set);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + (isTop ? 1231 : 1237);
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
		Aliases other = (Aliases) obj;
		if (isTop != other.isTop)
			return false;
		return true;
	}

	public <T extends Symbol> Iterable<T> castElements(Class<T> type) {
		return new CastIterable<>(this, type);
	}
}
