package it.unive.lisa.util.datastructures.regex;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.util.datastructures.regex.symbolic.SymbolicString;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class RegularExpressionTest {

	@Test
	public void starOnEpsilonIsANoOp() {
		// epsilon* = epsilon, and the smart constructor avoids allocating a
		// Star wrapper in that case
		assertSame(Atom.EPSILON, Atom.EPSILON.star());
	}

	@Test
	public void starOnNonEpsilonWrapsInStar() {
		RegularExpression starred = new Atom("a").star();
		assertTrue(starred.isStar());
		assertEquals(new Atom("a"), starred.asStar().getOperand());
	}

	@Test
	public void compWithEpsilonIsANoOp() {
		RegularExpression a = new Atom("a");
		assertSame(a, a.comp(Atom.EPSILON));
		assertSame(a, Atom.EPSILON.comp(a));
	}

	@Test
	public void compWithNonEpsilonWrapsInComp() {
		RegularExpression a = new Atom("a"), b = new Atom("b");
		RegularExpression comp = a.comp(b);
		assertTrue(comp.isComp());
		assertEquals(a, comp.asComp().getFirst());
		assertEquals(b, comp.asComp().getSecond());
	}

	@Test
	public void orOfTwoEpsilonsIsANoOp() {
		assertSame(Atom.EPSILON, Atom.EPSILON.or(Atom.EPSILON));
	}

	@Test
	public void orOfNonEpsilonsWrapsInOr() {
		RegularExpression a = new Atom("a"), b = new Atom("b");
		RegularExpression or = a.or(b);
		assertTrue(or.isOr());
	}

	@Test
	public void isEpsilonHoldsForTheEpsilonConstantAndEmptyAtoms() {
		assertTrue(Atom.EPSILON.isEpsilon());
		assertTrue(new Atom("").isEpsilon());
		assertFalse(new Atom("a").isEpsilon());
		assertFalse(EmptySet.INSTANCE.isEpsilon());
	}

	@Test
	public void typeCastHelpersReturnNullForTheWrongType() {
		RegularExpression a = new Atom("a");
		assertNull(a.asComp());
		assertNull(a.asOr());
		assertNull(a.asStar());
		assertNull(a.asEmptySet());
		assertEquals(a, a.asAtom());
	}

	@Test
	public void substringExtractsExactSlicesFromAConcreteAtom() {
		RegularExpression hello = new Atom("hello");
		Set<SymbolicString> sub = hello.substring(1, 3);
		assertEquals(1, sub.size());
		assertEquals("el", sub.iterator().next().toString());
	}

	@Test
	public void substringOfTheEmptySetIsEmpty() {
		assertTrue(EmptySet.INSTANCE.substring(0, 1).isEmpty());
	}

}
