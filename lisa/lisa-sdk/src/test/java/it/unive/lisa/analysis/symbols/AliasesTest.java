package it.unive.lisa.analysis.symbols;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.analysis.SemanticException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class AliasesTest {

	@Test
	public void defaultConstructorIsTop() {
		Aliases aliases = new Aliases();
		assertTrue(aliases.isTop());
		assertFalse(aliases.isBottom());
	}

	@Test
	public void constructorWithASingleSymbolContainsExactlyThatSymbol() {
		NameSymbol s = new NameSymbol("x");
		Aliases aliases = new Aliases(s);
		assertFalse(aliases.isTop());
		assertFalse(aliases.isBottom());
		assertEquals(Set.of(s), aliases.elements);
	}

	@Test
	public void constructorWithAnEmptySetIsBottomNotTop() {
		// mirrors the fixed GenericSetLattice convention: an incidentally
		// empty concrete set is the bottom of the lattice, not its top
		Aliases aliases = new Aliases(Collections.emptySet());
		assertTrue(aliases.isBottom());
		assertFalse(aliases.isTop());
	}

	@Test
	public void constructorWithANonEmptySetContainsAllOfThem() {
		NameSymbol a = new NameSymbol("a");
		NameSymbol b = new NameSymbol("b");
		Aliases aliases = new Aliases(Set.of(a, b));
		assertEquals(Set.of(a, b), aliases.elements);
	}

	@Test
	public void topAndBottomAreUniqueAndDistinct() {
		Aliases aliases = new Aliases(new NameSymbol("x"));
		assertEquals(new Aliases().top(), aliases.top());
		assertEquals(new Aliases().bottom(), aliases.bottom());
		assertFalse(aliases.top().equals(aliases.bottom()));
	}

	@Test
	public void mkBuildsANonTopInstanceWrappingTheGivenSet() {
		NameSymbol s = new NameSymbol("x");
		Aliases aliases = new Aliases().mk(Set.of(s));
		assertEquals(Set.of(s), aliases.elements);
		assertFalse(aliases.isTop());
	}

	@Test
	public void lubIsSetUnion()
			throws SemanticException {
		NameSymbol a = new NameSymbol("a");
		NameSymbol b = new NameSymbol("b");
		Aliases left = new Aliases(a);
		Aliases right = new Aliases(b);
		assertEquals(Set.of(a, b), left.lub(right).elements);
	}

	@Test
	public void lessOrEqualIsSubset()
			throws SemanticException {
		NameSymbol a = new NameSymbol("a");
		NameSymbol b = new NameSymbol("b");
		Aliases smaller = new Aliases(a);
		Aliases bigger = new Aliases(Set.of(a, b));
		assertTrue(smaller.lessOrEqual(bigger));
		assertFalse(bigger.lessOrEqual(smaller));
	}

	@Test
	public void castElementsCastsEveryElementToTheGivenType() {
		QualifierSymbol q1 = new QualifierSymbol("q1");
		QualifierSymbol q2 = new QualifierSymbol("q2");
		Aliases aliases = new Aliases(Set.of(q1, q2));
		Set<QualifierSymbol> casted = new HashSet<>();
		aliases.castElements(QualifierSymbol.class).forEach(casted::add);
		assertEquals(Set.of(q1, q2), casted);
	}

	@Test
	public void castElementsThrowsWhenAnElementDoesNotMatchTheGivenType() {
		Aliases aliases = new Aliases(Set.of(new NameSymbol("x")));
		Iterable<QualifierSymbol> casted = aliases.castElements(QualifierSymbol.class);
		assertThrows(ClassCastException.class, () -> casted.forEach(e -> {
		}));
	}

}
