package it.unive.lisa.analysis.symbols;

import it.unive.lisa.analysis.lattices.FunctionalLattice;
import java.util.Map;

public class SymbolAliasing extends FunctionalLattice<SymbolAliasing, Symbol, Aliases> {

	public SymbolAliasing() {
		super(new Aliases());
	}

	private SymbolAliasing(Aliases lattice, Map<Symbol, Aliases> function) {
		super(lattice, function);
	}

	public SymbolAliasing putState(Symbol toAlias, Symbol alias) {
		return super.putState(toAlias, new Aliases(alias));
	}

	@Override
	public SymbolAliasing top() {
		return new SymbolAliasing(lattice.top(), null);
	}

	@Override
	public boolean isTop() {
		return lattice.isTop() && function == null;
	}

	@Override
	public SymbolAliasing bottom() {
		return new SymbolAliasing(lattice.bottom(), null);
	}

	@Override
	public boolean isBottom() {
		return lattice.isBottom() && function == null;
	}

	@Override
	protected SymbolAliasing mk(Aliases lattice, Map<Symbol, Aliases> function) {
		return new SymbolAliasing(lattice, function);
	}
}
