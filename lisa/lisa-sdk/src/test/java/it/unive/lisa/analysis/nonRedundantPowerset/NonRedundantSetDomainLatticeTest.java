package it.unive.lisa.analysis.nonRedundantPowerset;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.value.ValueLattice;
import it.unive.lisa.program.SyntheticLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.type.Untyped;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link NonRedundantSetDomainLattice}. In particular, these tests
 * are a regression suite for a bug where {@code forgetIdentifier(s)},
 * {@code forgetIdentifiersIf}, {@code pushScope} and {@code popScope} collected
 * their results into a {@link java.util.TreeSet}, even though the element type
 * {@code L} is only required to extend {@link ValueLattice}, with no
 * {@link Comparable} bound. Since {@link FakeIdSet} below deliberately does not
 * implement {@link Comparable}, any of these operations producing two or more
 * elements would previously throw a {@link ClassCastException}.
 */
public class NonRedundantSetDomainLatticeTest {

	private static final ProgramPoint PP = new ProgramPoint() {

		@Override
		public CodeLocation getLocation() {
			return SyntheticLocation.INSTANCE;
		}

		@Override
		public CFG getCFG() {
			return null;
		}
	};

	private final Identifier x = new Variable(Untyped.INSTANCE, "x", SyntheticLocation.INSTANCE);
	private final Identifier a = new Variable(Untyped.INSTANCE, "a", SyntheticLocation.INSTANCE);
	private final Identifier b = new Variable(Untyped.INSTANCE, "b", SyntheticLocation.INSTANCE);

	/**
	 * A minimal {@link ValueLattice} tracking a set of identifiers, that
	 * deliberately does <b>not</b> implement {@link Comparable}.
	 */
	private static final class FakeIdSet
			implements
			ValueLattice<FakeIdSet> {

		private final Set<Identifier> tracked;

		private FakeIdSet(
				Set<Identifier> tracked) {
			this.tracked = tracked;
		}

		private static FakeIdSet of(
				Identifier... ids) {
			return new FakeIdSet(new HashSet<>(Set.of(ids)));
		}

		@Override
		public boolean isBottom() {
			return false;
		}

		@Override
		public boolean isTop() {
			return false;
		}

		@Override
		public FakeIdSet top() {
			return new FakeIdSet(Set.of());
		}

		@Override
		public FakeIdSet bottom() {
			return new FakeIdSet(Set.of());
		}

		@Override
		public boolean lessOrEqual(
				FakeIdSet other) {
			return other.tracked.containsAll(tracked);
		}

		@Override
		public FakeIdSet lub(
				FakeIdSet other) {
			Set<Identifier> union = new HashSet<>(tracked);
			union.addAll(other.tracked);
			return new FakeIdSet(union);
		}

		@Override
		public boolean knowsIdentifier(
				Identifier id) {
			return tracked.contains(id);
		}

		@Override
		public FakeIdSet forgetIdentifier(
				Identifier id,
				ProgramPoint pp) {
			Set<Identifier> updated = new HashSet<>(tracked);
			updated.remove(id);
			return new FakeIdSet(updated);
		}

		@Override
		public FakeIdSet forgetIdentifiers(
				Iterable<Identifier> ids,
				ProgramPoint pp) {
			Set<Identifier> updated = new HashSet<>(tracked);
			ids.forEach(updated::remove);
			return new FakeIdSet(updated);
		}

		@Override
		public FakeIdSet forgetIdentifiersIf(
				Predicate<Identifier> test,
				ProgramPoint pp) {
			Set<Identifier> updated = new HashSet<>(tracked);
			updated.removeIf(test);
			return new FakeIdSet(updated);
		}

		@Override
		public FakeIdSet pushScope(
				ScopeToken token,
				ProgramPoint pp) {
			return this;
		}

		@Override
		public FakeIdSet popScope(
				ScopeToken token,
				ProgramPoint pp) {
			return this;
		}

		@Override
		public FakeIdSet store(
				Identifier target,
				Identifier source) {
			return this;
		}

		@Override
		public StructuredRepresentation representation() {
			return new StringRepresentation(tracked.toString());
		}
	}

	private static final class TestDomainPowerset
			extends
			NonRedundantSetDomainLattice<TestDomainPowerset, FakeIdSet> {

		private TestDomainPowerset(
				Set<FakeIdSet> elements) {
			super(elements, new FakeIdSet(Set.of()));
		}

		@Override
		public TestDomainPowerset mk(
				Set<FakeIdSet> set) {
			return new TestDomainPowerset(set);
		}
	}

	private TestDomainPowerset twoElementDomain() {
		// two distinct, non-comparable elements: forgetting/pushing/popping
		// must retain both (as long as they remain non-redundant), which
		// requires collecting them into a Set that does not need a total
		// order
		return new TestDomainPowerset(Set.of(FakeIdSet.of(x, a), FakeIdSet.of(x, b)));
	}

	@Test
	public void testForgetIdentifierWithNonComparableElementsDoesNotThrow()
			throws SemanticException {
		TestDomainPowerset domain = twoElementDomain();
		TestDomainPowerset after = assertDoesNotThrow(() -> domain.forgetIdentifier(x, PP));
		assertFalse(after.knowsIdentifier(x));
		assertTrue(after.knowsIdentifier(a));
		assertTrue(after.knowsIdentifier(b));
		assertEquals(2, after.elements.size());
	}

	@Test
	public void testForgetIdentifiersWithNonComparableElementsDoesNotThrow()
			throws SemanticException {
		TestDomainPowerset domain = twoElementDomain();
		TestDomainPowerset after = assertDoesNotThrow(() -> domain.forgetIdentifiers(Set.of(x, a), PP));
		assertFalse(after.knowsIdentifier(x));
		assertFalse(after.knowsIdentifier(a));
		assertTrue(after.knowsIdentifier(b));
	}

	@Test
	public void testForgetIdentifiersIfWithNonComparableElementsDoesNotThrow()
			throws SemanticException {
		TestDomainPowerset domain = twoElementDomain();
		TestDomainPowerset after = assertDoesNotThrow(
				() -> domain.forgetIdentifiersIf(id -> id.getName().equals("x"), PP));
		assertFalse(after.knowsIdentifier(x));
		assertTrue(after.knowsIdentifier(a));
		assertTrue(after.knowsIdentifier(b));
	}

	@Test
	public void testPushScopeWithNonComparableElementsDoesNotThrow()
			throws SemanticException {
		TestDomainPowerset domain = twoElementDomain();
		TestDomainPowerset after = assertDoesNotThrow(() -> domain.pushScope(new ScopeToken(PP), PP));
		assertEquals(2, after.elements.size());
	}

	@Test
	public void testPopScopeWithNonComparableElementsDoesNotThrow()
			throws SemanticException {
		TestDomainPowerset domain = twoElementDomain();
		TestDomainPowerset after = assertDoesNotThrow(() -> domain.popScope(new ScopeToken(PP), PP));
		assertEquals(2, after.elements.size());
	}

	@Test
	public void testKnowsIdentifierIsTrueIfAnyElementKnowsIt() {
		TestDomainPowerset domain = twoElementDomain();
		assertTrue(domain.knowsIdentifier(x));
		assertTrue(domain.knowsIdentifier(a));
		assertTrue(domain.knowsIdentifier(b));
		Identifier other = new Variable(Untyped.INSTANCE, "other", SyntheticLocation.INSTANCE);
		assertFalse(domain.knowsIdentifier(other));
	}

}
