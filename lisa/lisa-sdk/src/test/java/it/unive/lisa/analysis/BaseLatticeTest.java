package it.unive.lisa.analysis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;
import java.util.Arrays;
import java.util.Objects;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link BaseLattice}'s dispatch template: the base cases handled by
 * {@code lessOrEqual}/{@code lub}/{@code glb}/{@code upchain}/
 * {@code downchain}/{@code widening}/{@code narrowing} before delegating to
 * their {@code *Aux} counterparts.
 */
public class BaseLatticeTest {

	/**
	 * A minimal {@link BaseLattice} implementer that only overrides the two
	 * methods it is forced to ({@link #lubAux(MinimalLattice)} and
	 * {@link #lessOrEqualAux(MinimalLattice)}), leaving every other
	 * {@code *Aux} hook to its default implementation. This is used to verify
	 * that, e.g., {@code glbAux} defaults to {@link Lattice#bottom()},
	 * {@code upchainAux}/{@code wideningAux} default to {@code lubAux}, and
	 * {@code downchainAux}/{@code narrowingAux} default to {@code glbAux}.
	 */
	private static class MinimalLattice
			implements
			BaseLattice<MinimalLattice> {

		private static final MinimalLattice TOP = new MinimalLattice(Integer.MAX_VALUE);
		private static final MinimalLattice BOTTOM = new MinimalLattice(Integer.MIN_VALUE);

		private final int value;

		private MinimalLattice(
				int value) {
			this.value = value;
		}

		private static MinimalLattice of(
				int value) {
			return new MinimalLattice(value);
		}

		@Override
		public MinimalLattice lubAux(
				MinimalLattice other)
				throws SemanticException {
			return of(Math.max(value, other.value));
		}

		@Override
		public boolean lessOrEqualAux(
				MinimalLattice other)
				throws SemanticException {
			return value <= other.value;
		}

		@Override
		public MinimalLattice top() {
			return TOP;
		}

		@Override
		public MinimalLattice bottom() {
			return BOTTOM;
		}

		@Override
		public StructuredRepresentation representation() {
			return new StringRepresentation(value);
		}

		@Override
		public boolean equals(
				Object obj) {
			if (this == obj)
				return true;
			if (!(obj instanceof MinimalLattice))
				return false;
			return value == ((MinimalLattice) obj).value;
		}

		@Override
		public int hashCode() {
			return Objects.hash(value);
		}

	}

	@Test
	public void testGlbAuxDefaultsToBottom()
			throws SemanticException {
		MinimalLattice five = MinimalLattice.of(5);
		MinimalLattice three = MinimalLattice.of(3);
		// neither top, bottom, null, nor equal: reaches glbAux, whose default
		// implementation always returns bottom()
		assertSame(MinimalLattice.BOTTOM, five.glb(three));
	}

	@Test
	public void testUpchainAuxDefaultsToLubAux()
			throws SemanticException {
		MinimalLattice five = MinimalLattice.of(5);
		MinimalLattice three = MinimalLattice.of(3);
		assertEquals(five.lub(three), five.upchain(three));
		assertEquals(MinimalLattice.of(5), five.upchain(three));
	}

	@Test
	public void testDownchainAuxDefaultsToGlbAux()
			throws SemanticException {
		MinimalLattice five = MinimalLattice.of(5);
		MinimalLattice three = MinimalLattice.of(3);
		assertEquals(five.glb(three), five.downchain(three));
		assertSame(MinimalLattice.BOTTOM, five.downchain(three));
	}

	@Test
	public void testWideningAuxDefaultsToLubAux()
			throws SemanticException {
		MinimalLattice five = MinimalLattice.of(5);
		MinimalLattice three = MinimalLattice.of(3);
		assertEquals(five.lub(three), five.widening(three));
	}

	@Test
	public void testNarrowingAuxDefaultsToGlbAux()
			throws SemanticException {
		MinimalLattice five = MinimalLattice.of(5);
		MinimalLattice three = MinimalLattice.of(3);
		assertEquals(five.glb(three), five.narrowing(three));
		assertSame(MinimalLattice.BOTTOM, five.narrowing(three));
	}

	@Test
	public void testNarrowingWithTopArgumentShortCircuitsToThis()
			throws SemanticException {
		// regression test: narrowing(other) must treat other.isTop() as a base
		// case (returning `this`, mirroring glb(top) == this) rather than
		// falling through to narrowingAux/glbAux with a top argument, which
		// would violate the documented precondition of glbAux ("other is
		// neither top nor bottom") and, with only the default glbAux
		// implementation available, would incorrectly yield bottom() instead
		// of `this`
		MinimalLattice five = MinimalLattice.of(5);
		assertSame(five, five.narrowing(MinimalLattice.TOP));
		// this must be consistent with what glb(top) yields, since narrowing's
		// *Aux hooks default to the glb ones
		assertEquals(five.glb(MinimalLattice.TOP), five.narrowing(MinimalLattice.TOP));
	}

	/**
	 * A {@link BaseLattice} implementer overriding every single {@code *Aux}
	 * hook with a distinguishable, non-commutative, non-associative operation,
	 * so that tests can assert that {@link BaseLattice}'s default methods
	 * dispatch to the <b>right</b> hook (and not, e.g., always to
	 * {@code lubAux}) and that the base-case short-circuiting in
	 * {@link BaseLattice} never lets a {@code null}/top/bottom/equal operand
	 * reach an {@code *Aux} method.
	 */
	private static class FullLattice
			implements
			BaseLattice<FullLattice> {

		private static final FullLattice TOP = new FullLattice(-1);
		private static final FullLattice BOTTOM = new FullLattice(-2);

		private final int value;

		private FullLattice(
				int value) {
			this.value = value;
		}

		private static FullLattice of(
				int value) {
			return new FullLattice(value);
		}

		private void checkPreconditions(
				FullLattice other) {
			// mirrors, at runtime, the guarantees documented on every *Aux
			// method of BaseLattice
			assertTrue(other != null, "*Aux invoked with a null argument");
			assertFalse(other.isTop(), "*Aux invoked with a top argument");
			assertFalse(other.isBottom(), "*Aux invoked with a bottom argument");
			assertFalse(this.isTop(), "*Aux invoked while `this` is top");
			assertFalse(this.isBottom(), "*Aux invoked while `this` is bottom");
			assertFalse(this.equals(other), "*Aux invoked with this.equals(other)");
			assertFalse(this == other, "*Aux invoked with this == other");
		}

		@Override
		public FullLattice lubAux(
				FullLattice other)
				throws SemanticException {
			checkPreconditions(other);
			return of(value * 10 + other.value);
		}

		@Override
		public FullLattice glbAux(
				FullLattice other)
				throws SemanticException {
			checkPreconditions(other);
			return of(value * 100 + other.value);
		}

		@Override
		public FullLattice upchainAux(
				FullLattice other)
				throws SemanticException {
			checkPreconditions(other);
			return of(value * 1000 + other.value);
		}

		@Override
		public FullLattice downchainAux(
				FullLattice other)
				throws SemanticException {
			checkPreconditions(other);
			return of(value * 10000 + other.value);
		}

		@Override
		public FullLattice wideningAux(
				FullLattice other)
				throws SemanticException {
			checkPreconditions(other);
			return of(value * 100000 + other.value);
		}

		@Override
		public FullLattice narrowingAux(
				FullLattice other)
				throws SemanticException {
			checkPreconditions(other);
			return of(value * 1000000 + other.value);
		}

		@Override
		public boolean lessOrEqualAux(
				FullLattice other)
				throws SemanticException {
			checkPreconditions(other);
			return value <= other.value;
		}

		@Override
		public FullLattice top() {
			return TOP;
		}

		@Override
		public FullLattice bottom() {
			return BOTTOM;
		}

		@Override
		public StructuredRepresentation representation() {
			return new StringRepresentation(value);
		}

		@Override
		public boolean equals(
				Object obj) {
			if (this == obj)
				return true;
			if (!(obj instanceof FullLattice))
				return false;
			return value == ((FullLattice) obj).value;
		}

		@Override
		public int hashCode() {
			return Objects.hash(value);
		}

	}

	@Test
	public void testEachOperationDispatchesToItsOwnAuxHook()
			throws SemanticException {
		FullLattice a = FullLattice.of(2);
		FullLattice b = FullLattice.of(3);

		assertEquals(FullLattice.of(23), a.lub(b));
		assertEquals(FullLattice.of(203), a.glb(b));
		assertEquals(FullLattice.of(2003), a.upchain(b));
		assertEquals(FullLattice.of(20003), a.downchain(b));
		assertEquals(FullLattice.of(200003), a.widening(b));
		assertEquals(FullLattice.of(2000003), a.narrowing(b));
	}

	@Test
	public void testLessOrEqualBaseCases()
			throws SemanticException {
		FullLattice a = FullLattice.of(2);
		FullLattice sameValue = FullLattice.of(2);

		assertFalse(a.lessOrEqual(null));
		assertTrue(a.lessOrEqual(a)); // this == other
		assertTrue(a.lessOrEqual(sameValue)); // this.equals(other)
		assertTrue(FullLattice.BOTTOM.lessOrEqual(a)); // this.isBottom()
		assertTrue(a.lessOrEqual(FullLattice.TOP)); // other.isTop()
		assertFalse(FullLattice.TOP.lessOrEqual(a)); // this.isTop(), other not
														// bottom/top/equal/same
		assertFalse(a.lessOrEqual(FullLattice.BOTTOM)); // other.isBottom(),
														// this not
														// top/bottom/equal/same
		// otherwise, delegates to lessOrEqualAux
		assertTrue(FullLattice.of(2).lessOrEqual(FullLattice.of(5)));
		assertFalse(FullLattice.of(5).lessOrEqual(FullLattice.of(2)));
	}

	@Test
	public void testLubBaseCases()
			throws SemanticException {
		FullLattice a = FullLattice.of(2);
		FullLattice sameValue = FullLattice.of(2);

		assertSame(a, a.lub((FullLattice) null));
		assertSame(a, a.lub(FullLattice.BOTTOM)); // other.isBottom()
		assertSame(a, a.lub(a)); // this == other
		assertEquals(a, a.lub(sameValue)); // this.equals(other)
		assertSame(FullLattice.TOP, FullLattice.TOP.lub(a)); // this.isTop()
		assertSame(a, FullLattice.BOTTOM.lub(a)); // this.isBottom() -> returns
													// other
		assertSame(FullLattice.TOP, a.lub(FullLattice.TOP)); // other.isTop() ->
																// returns other
	}

	@Test
	public void testGlbBaseCases()
			throws SemanticException {
		FullLattice a = FullLattice.of(2);
		FullLattice sameValue = FullLattice.of(2);

		assertSame(a, a.glb((FullLattice) null));
		assertSame(a, a.glb(FullLattice.TOP)); // other.isTop()
		assertSame(a, a.glb(a)); // this == other
		assertEquals(a, a.glb(sameValue)); // this.equals(other)
		assertSame(FullLattice.BOTTOM, FullLattice.BOTTOM.glb(a)); // this.isBottom()
		assertSame(a, FullLattice.TOP.glb(a)); // this.isTop() -> returns other
	}

	@Test
	public void testGlbWithBottomOtherReturnsOther()
			throws SemanticException {
		FullLattice a = FullLattice.of(2);
		assertSame(FullLattice.BOTTOM, a.glb(FullLattice.BOTTOM));
	}

	@Test
	public void testWideningBaseCasesMirrorLub()
			throws SemanticException {
		FullLattice a = FullLattice.of(2);
		FullLattice sameValue = FullLattice.of(2);

		assertSame(a, a.widening((FullLattice) null));
		assertSame(a, a.widening(FullLattice.BOTTOM));
		assertSame(a, a.widening(a));
		assertEquals(a, a.widening(sameValue));
		assertSame(FullLattice.TOP, FullLattice.TOP.widening(a));
		assertSame(a, FullLattice.BOTTOM.widening(a));
		assertSame(FullLattice.TOP, a.widening(FullLattice.TOP));
	}

	@Test
	public void testNarrowingBaseCasesMirrorGlb()
			throws SemanticException {
		FullLattice a = FullLattice.of(2);
		FullLattice sameValue = FullLattice.of(2);

		assertSame(a, a.narrowing((FullLattice) null));
		assertSame(a, a.narrowing(FullLattice.TOP));
		assertSame(a, a.narrowing(a));
		assertEquals(a, a.narrowing(sameValue));
		assertSame(FullLattice.BOTTOM, FullLattice.BOTTOM.narrowing(a));
		assertSame(a, FullLattice.TOP.narrowing(a));
		assertSame(FullLattice.BOTTOM, a.narrowing(FullLattice.BOTTOM));
	}

	@Test
	public void testUpchainAndDownchainBaseCasesMirrorLubAndGlb()
			throws SemanticException {
		FullLattice a = FullLattice.of(2);

		assertSame(a, a.upchain((FullLattice) null));
		assertSame(a, a.upchain(FullLattice.BOTTOM));
		assertSame(FullLattice.TOP, a.upchain(FullLattice.TOP));

		assertSame(a, a.downchain((FullLattice) null));
		assertSame(a, a.downchain(FullLattice.TOP));
		assertSame(FullLattice.BOTTOM, a.downchain(FullLattice.BOTTOM));
	}

	@Test
	public void testFoldOrderOfVarargsAndIterableOverloads()
			throws SemanticException {
		// wideningAux(x, y) = x * 100000 + y is neither commutative nor
		// associative, which makes it a good witness for the left-to-right
		// fold order used by the varargs/iterable overloads (shared "compress"
		// logic in Lattice)
		FullLattice start = FullLattice.of(5);
		FullLattice one = FullLattice.of(1);
		FullLattice two = FullLattice.of(2);
		FullLattice three = FullLattice.of(3);

		FullLattice expected = start.widening(one).widening(two).widening(three);
		assertEquals(expected, start.widening(one, two, three));
		assertEquals(expected, start.widening(Arrays.asList(one, two, three)));
	}

	@Test
	public void testAuxPreconditionsHoldAcrossManyCombinations()
			throws SemanticException {
		// exercises every operation across many top/bottom/equal/distinct
		// combinations; the FullLattice fixture self-asserts the documented
		// *Aux preconditions, so this test fails loudly if BaseLattice ever
		// lets an invalid operand slip through to an *Aux hook
		FullLattice[] values = {
				FullLattice.TOP,
				FullLattice.BOTTOM,
				FullLattice.of(1),
				FullLattice.of(1),
				FullLattice.of(7) };

		for (FullLattice x : values)
			for (FullLattice y : values) {
				x.lessOrEqual(y);
				x.lub(y);
				x.glb(y);
				x.upchain(y);
				x.downchain(y);
				x.widening(y);
				x.narrowing(y);
			}
	}

}
