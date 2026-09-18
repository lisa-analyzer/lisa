package it.unive.lisa.analysis.symbols;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class SymbolAliasingTest {

	@Test
	public void defaultConstructorIsEmptyAndTop() {
		SymbolAliasing aliasing = new SymbolAliasing();
		assertTrue(aliasing.isTop());
	}

	@Test
	public void aliasRegistersASingleAliasForTheGivenSymbol() {
		NameSymbol toAlias = new NameSymbol("original");
		NameSymbol alias = new NameSymbol("renamed");
		SymbolAliasing aliasing = new SymbolAliasing().alias(toAlias, alias);
		assertEquals(new Aliases(alias), aliasing.getState(toAlias));
	}

	@Test
	public void putStateIsAStrongUpdateThatDiscardsThePreviousAliases() {
		NameSymbol toAlias = new NameSymbol("original");
		NameSymbol first = new NameSymbol("first");
		NameSymbol second = new NameSymbol("second");
		SymbolAliasing aliasing = new SymbolAliasing()
				.alias(toAlias, first)
				.alias(toAlias, second);
		assertEquals(new Aliases(second), aliasing.getState(toAlias));
	}

	@Test
	public void stateOfUnknownSymbolsIsBottom() {
		SymbolAliasing aliasing = new SymbolAliasing().alias(new NameSymbol("x"), new NameSymbol("y"));
		assertTrue(aliasing.getState(new NameSymbol("never registered")).isBottom());
	}

	@Test
	public void topAndBottomHaveNoMappings() {
		SymbolAliasing aliasing = new SymbolAliasing().alias(new NameSymbol("x"), new NameSymbol("y"));
		assertTrue(aliasing.top().getState(new NameSymbol("x")).isTop());
		assertTrue(aliasing.bottom().getState(new NameSymbol("x")).isBottom());
	}

	@Test
	public void isTopBecomesFalseAfterRegisteringAnAlias() {
		SymbolAliasing aliasing = new SymbolAliasing().alias(new NameSymbol("x"), new NameSymbol("y"));
		assertFalse(aliasing.isTop());
	}

}
