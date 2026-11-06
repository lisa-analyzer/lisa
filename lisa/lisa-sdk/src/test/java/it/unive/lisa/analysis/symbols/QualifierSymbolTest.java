package it.unive.lisa.analysis.symbols;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

public class QualifierSymbolTest {

	@Test
	public void getQualifierReturnsTheConstructorArgument() {
		assertEquals("q", new QualifierSymbol("q").getQualifier());
	}

	@Test
	public void equalsIsBasedOnTheQualifier() {
		QualifierSymbol q1 = new QualifierSymbol("q");
		QualifierSymbol q2 = new QualifierSymbol("q");
		QualifierSymbol other = new QualifierSymbol("other");
		assertEquals(q1, q2);
		assertEquals(q1.hashCode(), q2.hashCode());
		assertNotEquals(q1, other);
	}

	@Test
	public void equalsIsFalseAgainstNullAndOtherSymbolKinds() {
		QualifierSymbol q = new QualifierSymbol("q");
		assertFalse(q.equals(null));
		assertNotEquals(q, new NameSymbol("q"));
		assertNotEquals(q, new QualifiedNameSymbol("q", "n"));
	}

	@Test
	public void toStringUsesTheQualifierNamePlaceholderConvention() {
		assertEquals("q::<name>", new QualifierSymbol("q").toString());
	}

}
