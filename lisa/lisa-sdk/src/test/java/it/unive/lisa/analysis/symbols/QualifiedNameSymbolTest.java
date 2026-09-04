package it.unive.lisa.analysis.symbols;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

public class QualifiedNameSymbolTest {

	@Test
	public void gettersReturnTheConstructorArguments() {
		QualifiedNameSymbol s = new QualifiedNameSymbol("q", "n");
		assertEquals("q", s.getQualifier());
		assertEquals("n", s.getName());
	}

	@Test
	public void equalsRequiresBothQualifierAndName() {
		QualifiedNameSymbol s1 = new QualifiedNameSymbol("q", "n");
		QualifiedNameSymbol s2 = new QualifiedNameSymbol("q", "n");
		QualifiedNameSymbol differentQualifier = new QualifiedNameSymbol("other", "n");
		QualifiedNameSymbol differentName = new QualifiedNameSymbol("q", "other");
		assertEquals(s1, s2);
		assertEquals(s1.hashCode(), s2.hashCode());
		assertNotEquals(s1, differentQualifier);
		assertNotEquals(s1, differentName);
	}

	@Test
	public void equalsIsFalseAgainstNullAndOtherSymbolKinds() {
		QualifiedNameSymbol s = new QualifiedNameSymbol("q", "n");
		assertFalse(s.equals(null));
		assertNotEquals(s, new NameSymbol("n"));
		assertNotEquals(s, new QualifierSymbol("q"));
	}

	@Test
	public void toStringJoinsQualifierAndNameWithTheDoubleColonSeparator() {
		assertEquals("q::n", new QualifiedNameSymbol("q", "n").toString());
	}

}
