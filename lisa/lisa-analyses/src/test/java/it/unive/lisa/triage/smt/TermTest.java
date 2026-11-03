package it.unive.lisa.triage.smt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class TermTest {

	@Test
	public void symbolsAreQuotedOnlyWhenNeeded() {
		assertEquals("x", Term.symbol("x"));
		assertEquals("x.y$1", Term.symbol("x.y$1"));
		assertEquals("|a b|", Term.symbol("a b"));
		assertEquals("|1x|", Term.symbol("1x"));
		assertEquals("|a_b_c|", Term.symbol("a|b\\c"));
	}

	@Test
	public void literals() {
		assertEquals("5", Term.integer(5).toSmtLib());
		assertEquals("(- 5)", Term.integer(-5).toSmtLib());
		assertEquals(Sort.INT, Term.integer(-5).getSort());
		assertEquals("(_ bv255 8)", Term.bitVector(-1, 8).toSmtLib());
		assertEquals("(_ bv128 8)", Term.bitVector(128, 8).toSmtLib());
		assertEquals(Sort.bitVector(8), Term.bitVector(1, 8).getSort());
		assertEquals("\"a\"\"b\"", Term.string("a\"b").toSmtLib());
		assertEquals("\"a\\u{5c}\\u{a}\"", Term.string("a\\\n").toSmtLib());
		assertSame(Term.TRUE, Term.bool(true));
		assertThrows(IllegalArgumentException.class, () -> Sort.bitVector(0));
	}

	@Test
	public void applications() {
		Term x = Term.constant("x", Sort.INT);
		Term gt = Term.app(">", Sort.BOOL, x, Term.integer(3));
		assertEquals("(> x 3)", gt.toSmtLib());
		Term lt = Term.app("<", Sort.BOOL, x, Term.integer(5));
		assertEquals("(and (> x 3) (< x 5))", Term.and(gt, lt).toSmtLib());
		// constants are simplified
		assertSame(gt, Term.and(gt, Term.TRUE));
		assertSame(Term.FALSE, Term.and(gt, Term.FALSE));
		assertSame(gt, Term.or(Term.FALSE, gt));
		assertSame(Term.TRUE, Term.or(gt, Term.TRUE));
		assertSame(Term.FALSE, Term.not(Term.TRUE));
		assertSame(gt, Term.and(gt));
		assertSame(Term.TRUE, Term.and());
		assertSame(Term.FALSE, Term.or());
		assertEquals("(ite (> x 3) x 0)", Term.ite(gt, x, Term.integer(0)).toSmtLib());
		assertEquals(Sort.INT, Term.ite(gt, x, Term.integer(0)).getSort());
		assertEquals("(Array Int (_ BitVec 32))", Sort.array(Sort.INT, Sort.bitVector(32)).toSmtLib());
	}
}
