package it.unive.lisa.analysis.symbols;

import it.unive.lisa.analysis.FixpointInfo;
import it.unive.lisa.analysis.lattices.FunctionalLattice;
import java.util.Map;

/**
 * A {@link FunctionalLattice} mapping {@link Symbol}s to {@link Aliases}, that
 * is, sets of symbols. Instances of this domain can be used to resolve targets
 * of calls when the names used in the call are different from the ones in the
 * target's signature. This lattice is designed to be stored within the
 * analysis' {@link FixpointInfo} instance, using key {@value #INFO_KEY} (from
 * field {@link #INFO_KEY}).
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class SymbolAliasing extends FunctionalLattice<SymbolAliasing, Symbol, Aliases> {

	/**
	 * The key to use for accessing instances of this class within a
	 * {@link FixpointInfo} instance.
	 */
	public static final String INFO_KEY = "sym-aliasing";

	/**
	 * Builds an empty map of aliases.
	 */
	public SymbolAliasing() {
		super(new Aliases());
	}

	private SymbolAliasing(
			Aliases lattice,
			Map<Symbol, Aliases> function) {
		super(lattice, function);
	}

	/**
	 * Registers an alias for the given symbol. Any previous aliases will be
	 * deleted.
	 * 
	 * @param toAlias the symbol being aliased
	 * @param alias   the alias for {@code toAlias}
	 * 
	 * @return a copy of this domain, with the new alias
	 */
	public SymbolAliasing putState(
			Symbol toAlias,
			Symbol alias) {
		return super.putState(toAlias, new Aliases(alias));
	}

	@Override
	public SymbolAliasing top() {
		return new SymbolAliasing(lattice.top(), null);
	}

	@Override
	public SymbolAliasing bottom() {
		return new SymbolAliasing(lattice.bottom(), null);
	}

	@Override
	public SymbolAliasing mk(
			Aliases lattice,
			Map<Symbol, Aliases> function) {
		return new SymbolAliasing(lattice, function);
	}

	@Override
	public Aliases stateOfUnknown(
			Symbol key) {
		return lattice.bottom();
	}

	/**
	 * Registers an alias for the given symbol. Any previous aliases will be
	 * deleted.
	 * 
	 * @param toAlias the symbol being aliased
	 * @param alias   the alias for {@code toAlias}
	 * 
	 * @return a copy of this analysis state, with the new alias
	 */
	public SymbolAliasing alias(
			Symbol toAlias,
			Symbol alias) {
		return putState(toAlias, alias);
	}

}
