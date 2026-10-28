package it.unive.lisa.analysis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.Test;

/**
 * Tests for the default methods of {@link Lattice}, exercised through a minimal
 * implementer that does <b>not</b> mix in {@link BaseLattice}. This isolates
 * the behavior that {@link Lattice} itself provides (as opposed to the richer
 * dispatch template implemented by {@link BaseLattice}, which is tested
 * separately in {@link BaseLatticeTest}).
 */
public class LatticeTest {

	/**
	 * A bare-bones {@link Lattice} implementation that only overrides the
	 * methods it is forced to (plus {@link #equals(Object)}/
	 * {@link #hashCode()} for the tests that need value-based comparisons). All
	 * other operations rely purely on the default implementations offered by
	 * {@link Lattice}.
	 */
	private static class PlainLattice
			implements
			Lattice<PlainLattice> {

		private static final PlainLattice TOP = new PlainLattice(Integer.MAX_VALUE);
		private static final PlainLattice BOTTOM = new PlainLattice(Integer.MIN_VALUE);

		private final int value;

		private PlainLattice(
				int value) {
			this.value = value;
		}

		private static PlainLattice of(
				int value) {
			return new PlainLattice(value);
		}

		@Override
		public boolean lessOrEqual(
				PlainLattice other)
				throws SemanticException {
			return value <= other.value;
		}

		@Override
		public PlainLattice lub(
				PlainLattice other)
				throws SemanticException {
			return of(Math.max(value, other.value));
		}

		@Override
		public PlainLattice top() {
			return TOP;
		}

		@Override
		public PlainLattice bottom() {
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
			if (!(obj instanceof PlainLattice))
				return false;
			return value == ((PlainLattice) obj).value;
		}

		@Override
		public int hashCode() {
			return Objects.hash(value);
		}

	}

	@Test
	public void testTopAndBottomRepresentations() {
		assertEquals(new StringRepresentation(Lattice.TOP_STRING), Lattice.topRepresentation());
		assertEquals(new StringRepresentation(Lattice.BOTTOM_STRING), Lattice.bottomRepresentation());
	}

	@Test
	public void testIsTopAndIsBottomUseReferenceEquality()
			throws SemanticException {
		PlainLattice five = PlainLattice.of(5);
		assertTrue(PlainLattice.TOP.isTop());
		assertTrue(PlainLattice.BOTTOM.isBottom());
		assertTrue(!five.isTop());
		assertTrue(!five.isBottom());
		// an unrelated instance with the same value as top/bottom is *not*
		// considered top/bottom, since isTop()/isBottom() use reference
		// equality
		assertTrue(!PlainLattice.of(Integer.MAX_VALUE).isTop());
		assertTrue(!PlainLattice.of(Integer.MIN_VALUE).isBottom());
	}

	@Test
	public void testDefaultGlbAlwaysReturnsBottom()
			throws SemanticException {
		// Lattice#glb has no smart base-case handling: it unconditionally
		// returns bottom(), regardless of the argument (even null, or this
		// itself)
		PlainLattice five = PlainLattice.of(5);
		assertSame(PlainLattice.BOTTOM, five.glb(PlainLattice.of(3)));
		assertSame(PlainLattice.BOTTOM, five.glb(five));
		assertSame(PlainLattice.BOTTOM, five.glb((PlainLattice) null));
		assertSame(PlainLattice.BOTTOM, five.glb(PlainLattice.TOP));
	}

	@Test
	public void testDefaultUpchainDelegatesToLub()
			throws SemanticException {
		PlainLattice five = PlainLattice.of(5);
		PlainLattice three = PlainLattice.of(3);
		assertEquals(five.lub(three), five.upchain(three));
	}

	@Test
	public void testDefaultDownchainDelegatesToGlb()
			throws SemanticException {
		PlainLattice five = PlainLattice.of(5);
		PlainLattice three = PlainLattice.of(3);
		assertEquals(five.glb(three), five.downchain(three));
		assertSame(PlainLattice.BOTTOM, five.downchain(three));
	}

	@Test
	public void testDefaultWideningDelegatesToLub()
			throws SemanticException {
		PlainLattice five = PlainLattice.of(5);
		PlainLattice three = PlainLattice.of(3);
		assertEquals(five.lub(three), five.widening(three));
	}

	@Test
	public void testDefaultNarrowingDelegatesToGlb()
			throws SemanticException {
		PlainLattice five = PlainLattice.of(5);
		PlainLattice three = PlainLattice.of(3);
		assertEquals(five.glb(three), five.narrowing(three));
		assertSame(PlainLattice.BOTTOM, five.narrowing(three));
	}

	@Test
	public void testVarargsAndIterableFoldStartingFromThis()
			throws SemanticException {
		PlainLattice one = PlainLattice.of(1);
		PlainLattice two = PlainLattice.of(2);
		PlainLattice three = PlainLattice.of(3);
		PlainLattice start = PlainLattice.of(0);

		assertEquals(PlainLattice.of(3), start.lub(one, two, three));
		assertEquals(PlainLattice.of(3), start.lub(Arrays.asList(one, two, three)));
		assertEquals(PlainLattice.of(3), start.upchain(one, two, three));
		assertEquals(PlainLattice.of(3), start.widening(Arrays.asList(one, two, three)));
	}

	@Test
	public void testVarargsWithNoOthersReturnsThis()
			throws SemanticException {
		PlainLattice five = PlainLattice.of(5);
		assertSame(five, five.lub());
		assertSame(five, five.lub(List.of()));
	}

	@Test
	public void testUnknownValueDefaultsToTop() {
		PlainLattice five = PlainLattice.of(5);
		assertSame(PlainLattice.TOP, five.unknownValue(null));
	}

	@Test
	public void testGetAllLatticeInstancesDefaultChecksAssignability() {
		PlainLattice five = PlainLattice.of(5);

		Collection<PlainLattice> matches = five.getAllLatticeInstances(PlainLattice.class);
		assertEquals(new HashSet<>(List.of(five)), matches);

		Collection<UnrelatedLattice> none = five.getAllLatticeInstances(UnrelatedLattice.class);
		assertTrue(none.isEmpty());
	}

	@Test
	public void testGetLatticeInstanceDefaultsToNullWhenNoneFound()
			throws SemanticException {
		PlainLattice five = PlainLattice.of(5);
		assertNull(five.getLatticeInstance(UnrelatedLattice.class));
	}

	@Test
	public void testGetLatticeInstanceReturnsTheSingleMatch()
			throws SemanticException {
		PlainLattice five = PlainLattice.of(5);
		assertEquals(five, five.getLatticeInstance(PlainLattice.class));
	}

	/**
	 * A {@link Lattice} whose {@link #getAllLatticeInstances(Class)} is
	 * overridden to simulate a combination/composite lattice made of several
	 * inner {@link PlainLattice} instances, used to exercise the default
	 * lub-folding logic of {@link Lattice#getLatticeInstance(Class)}.
	 */
	private static class CompositeLattice
			implements
			Lattice<CompositeLattice> {

		private final List<PlainLattice> inner;

		private CompositeLattice(
				List<PlainLattice> inner) {
			this.inner = inner;
		}

		@Override
		public boolean lessOrEqual(
				CompositeLattice other)
				throws SemanticException {
			return true;
		}

		@Override
		public CompositeLattice lub(
				CompositeLattice other)
				throws SemanticException {
			return this;
		}

		@Override
		public CompositeLattice top() {
			return this;
		}

		@Override
		public CompositeLattice bottom() {
			return this;
		}

		@Override
		public StructuredRepresentation representation() {
			return new StringRepresentation("composite");
		}

		@SuppressWarnings("unchecked")
		@Override
		public <D extends Lattice<D>> Collection<D> getAllLatticeInstances(
				Class<D> domain) {
			Collection<D> result = new HashSet<>();
			for (PlainLattice p : inner)
				if (domain.isAssignableFrom(p.getClass()))
					result.add((D) p);
			return result;
		}

	}

	private static class UnrelatedLattice
			implements
			Lattice<UnrelatedLattice> {

		@Override
		public boolean lessOrEqual(
				UnrelatedLattice other)
				throws SemanticException {
			return true;
		}

		@Override
		public UnrelatedLattice lub(
				UnrelatedLattice other)
				throws SemanticException {
			return this;
		}

		@Override
		public UnrelatedLattice top() {
			return this;
		}

		@Override
		public UnrelatedLattice bottom() {
			return this;
		}

		@Override
		public StructuredRepresentation representation() {
			return new StringRepresentation("unrelated");
		}

	}

	@Test
	public void testGetLatticeInstanceLubsAllMatches()
			throws SemanticException {
		CompositeLattice composite = new CompositeLattice(
				Arrays.asList(PlainLattice.of(1), PlainLattice.of(7), PlainLattice.of(3)));
		// the default implementation lubs together every instance returned by
		// getAllLatticeInstances, so the max value should win here since
		// PlainLattice#lub keeps the maximum
		assertEquals(PlainLattice.of(7), composite.getLatticeInstance(PlainLattice.class));
	}

}
