package it.unive.lisa.triage.smt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;
import org.junit.jupiter.api.Test;

public class SExpressionTest {

	@Test
	public void atomsAndLists() {
		SExpression atom = SExpression.parse("  sat\n");
		assertTrue(atom.isAtom());
		assertEquals("sat", atom.getAtom());

		SExpression list = SExpression.parse("((x 4)\n  (|a b| (- 3)))");
		assertEquals(2, list.getChildren().size());
		assertEquals("(x 4)", list.getChildren().get(0).toString());
		assertEquals("|a b|", list.getChildren().get(1).getChildren().get(0).getAtom());
		assertEquals("(- 3)", list.getChildren().get(1).getChildren().get(1).toString());
	}

	@Test
	public void stringsAndComments() {
		SExpression s = SExpression.parse("(error \"line 1: a \"\"quoted\"\" (word)\") ; comment");
		assertEquals("\"line 1: a \"\"quoted\"\" (word)\"", s.getChildren().get(1).getAtom());
	}

	@Test
	public void readsSequences() throws Exception {
		StringReader reader = new StringReader("success\n(x y) ; trailing\n");
		assertEquals("success", SExpression.read(reader).toString());
		assertEquals("(x y)", SExpression.read(reader).toString());
		assertNull(SExpression.read(reader));
	}

	@Test
	public void malformed() {
		assertThrows(SmtException.class, () -> SExpression.parse("(a (b)"));
		assertThrows(SmtException.class, () -> SExpression.parse(")"));
		assertThrows(SmtException.class, () -> SExpression.parse("\"open"));
		assertThrows(SmtException.class, () -> SExpression.parse(""));
	}
}
