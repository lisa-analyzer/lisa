package it.unive.lisa.lattices.heap.allocations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.analysis.AbstractLattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.interprocedural.ScopeId;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.fixpoints.CompoundState;
import it.unive.lisa.program.cfg.statement.call.CFGCall;
import it.unive.lisa.program.type.Int32Type;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.type.Untyped;
import java.util.Objects;
import org.junit.jupiter.api.Test;

public class HeapAllocationSiteTest {

	private final CodeLocation loc = new SourceCodeLocation("fake", 1, 1);

	/**
	 * A context with a fixed hash, that is a starting id only if requested.
	 */
	private static class FixedContext<A extends AbstractLattice<A>>
			implements
			ScopeId<A> {

		private final String hash;

		private final boolean starting;

		private FixedContext(
				String hash,
				boolean starting) {
			this.hash = hash;
			this.starting = starting;
		}

		@Override
		public ScopeId<A> startingId() {
			return new FixedContext<>("start", true);
		}

		@Override
		public boolean isStartingId() {
			return starting;
		}

		@Override
		public ScopeId<A> push(
				CFGCall c,
				CompoundState<A> state) {
			throw new UnsupportedOperationException();
		}

		@Override
		public String getHash() {
			return hash;
		}

		@Override
		public int hashCode() {
			return Objects.hash(hash, starting);
		}

		@Override
		public boolean equals(
				Object obj) {
			if (this == obj)
				return true;
			if (obj == null || getClass() != obj.getClass())
				return false;
			FixedContext<?> other = (FixedContext<?>) obj;
			return starting == other.starting && Objects.equals(hash, other.hash);
		}
	}

	@Test
	public void contextHashIsPartOfTheName() {
		FixedContext<?> ctx = new FixedContext<>("h1", false);
		HeapAllocationSite noField = new HeapAllocationSite(Untyped.INSTANCE, "l", ctx, false, loc);
		assertEquals("pp@l<h1>", noField.getName());
		assertEquals(ctx, noField.getContext());

		HeapAllocationSite withField = new HeapAllocationSite(Untyped.INSTANCE, "l", "f", ctx, false, loc);
		assertEquals("pp@l<h1>[f]", withField.getName());
	}

	@Test
	public void nullOrStartingContextsAreDiscarded() {
		HeapAllocationSite plain = new HeapAllocationSite(Untyped.INSTANCE, "l", "f", false, loc);
		HeapAllocationSite nullCtx = new HeapAllocationSite(Untyped.INSTANCE, "l", "f", null, false, loc);
		HeapAllocationSite startCtx = new HeapAllocationSite(Untyped.INSTANCE, "l", "f",
				new FixedContext<>("h1", true), false, loc);
		assertEquals(plain, nullCtx);
		assertEquals(plain, startCtx);
		assertEquals("pp@l[f]", startCtx.getName());
		assertTrue(startCtx.getContext() == null);
	}

	@Test
	public void differentContextsMakeSitesUnequal() {
		HeapAllocationSite a = new HeapAllocationSite(Untyped.INSTANCE, "l", "f", new FixedContext<>("h1", false),
				false, loc);
		HeapAllocationSite b = new HeapAllocationSite(Untyped.INSTANCE, "l", "f", new FixedContext<>("h2", false),
				false, loc);
		HeapAllocationSite c = new HeapAllocationSite(Untyped.INSTANCE, "l", "f", new FixedContext<>("h1", false),
				false, loc);
		HeapAllocationSite plain = new HeapAllocationSite(Untyped.INSTANCE, "l", "f", false, loc);
		assertNotEquals(a, b);
		assertNotEquals(a, plain);
		assertEquals(a, c);
	}

	@Test
	public void transformationsPreserveTheContext() {
		FixedContext<?> ctx = new FixedContext<>("h1", false);
		HeapAllocationSite site = new HeapAllocationSite(Untyped.INSTANCE, "l", ctx, false, loc);
		site.setAllocation(true);

		assertEquals(ctx, site.toWeak().getContext());
		assertEquals("pp@l<h1>", site.toWeak().getName());
		HeapAllocationSite withField = site.withField(new Variable(Untyped.INSTANCE, "f", loc));
		assertEquals(ctx, withField.getContext());
		assertEquals("pp@l<h1>[f]", withField.getName());
		assertEquals(ctx, withField.withoutField().getContext());
		assertEquals("pp@l<h1>", withField.withoutField().getName());
		assertEquals(ctx, site.withType(Int32Type.INSTANCE).getContext());
		assertEquals(ctx, site.asNonAllocation().getContext());
		assertFalse(site.asNonAllocation().isAllocation());

		FixedContext<?> other = new FixedContext<>("h2", false);
		assertEquals("pp@l<h2>", site.withContext(other).getName());
		assertEquals("pp@l", site.withContext(null).getName());
	}

	@Test
	public void nameEncodesLocationAndField() {
		HeapAllocationSite noField = new HeapAllocationSite(Untyped.INSTANCE, "l", false, loc);
		assertEquals("pp@l", noField.getName());

		HeapAllocationSite withField = new HeapAllocationSite(Untyped.INSTANCE, "l", "f", false, loc);
		assertEquals("pp@l[f]", withField.getName());
	}

	@Test
	public void fieldConstructorFromSymbolicExpressionUsesItsToString() {
		HeapAllocationSite site = new HeapAllocationSite(
				Untyped.INSTANCE, "l", new Variable(Untyped.INSTANCE, "f", loc), false, loc);
		assertEquals("f", site.getField());
		assertEquals("pp@l[f]", site.getName());
	}

	@Test
	public void twoSitesWithSameNameAndWeaknessAreEqualRegardlessOfType() {
		HeapAllocationSite untypedSite = new HeapAllocationSite(Untyped.INSTANCE, "l", "f", false, loc);
		HeapAllocationSite intSite = new HeapAllocationSite(Int32Type.INSTANCE, "l", "f", false, loc);
		// identity of a heap location is name + weakness, not its static type
		// (see HeapLocation#equals/#hashCode)
		assertEquals(untypedSite, intSite);
		assertEquals(untypedSite.hashCode(), intSite.hashCode());
	}

	@Test
	public void differentWeaknessMakesSitesUnequal() {
		HeapAllocationSite strong = new HeapAllocationSite(Untyped.INSTANCE, "l", "f", false, loc);
		HeapAllocationSite weak = new HeapAllocationSite(Untyped.INSTANCE, "l", "f", true, loc);
		assertNotEquals(strong, weak);
	}

	@Test
	public void differentLocationOrFieldMakesSitesUnequal() {
		HeapAllocationSite base = new HeapAllocationSite(Untyped.INSTANCE, "l", "f", false, loc);
		HeapAllocationSite otherLocation = new HeapAllocationSite(Untyped.INSTANCE, "m", "f", false, loc);
		HeapAllocationSite otherField = new HeapAllocationSite(Untyped.INSTANCE, "l", "g", false, loc);
		HeapAllocationSite noField = new HeapAllocationSite(Untyped.INSTANCE, "l", false, loc);
		assertNotEquals(base, otherLocation);
		assertNotEquals(base, otherField);
		assertNotEquals(base, noField);
	}

	@Test
	public void aHeapAllocationSiteIsNeverEqualToAStackAllocationSiteWithTheSameNameAndWeakness() {
		HeapAllocationSite heap = new HeapAllocationSite(Untyped.INSTANCE, "l", "f", false, loc);
		StackAllocationSite stack = new StackAllocationSite(Untyped.INSTANCE, "l", "f", false, loc);
		// heap- and stack-allocated locations denote different kinds of
		// memory and must never be conflated by equals, even when they share
		// the same location name, field and weakness
		assertNotEquals(heap, stack);
	}

	@Test
	public void toStringReflectsWeaknessAndName() {
		HeapAllocationSite strong = new HeapAllocationSite(Untyped.INSTANCE, "l", false, loc);
		HeapAllocationSite weak = new HeapAllocationSite(Untyped.INSTANCE, "l", true, loc);
		assertEquals("heap[s]:pp@l", strong.toString());
		assertEquals("heap[w]:pp@l", weak.toString());
	}

	@Test
	public void lubOfAStrongAndAWeakSiteWithTheSameNameIsTheWeakOne()
			throws SemanticException {
		HeapAllocationSite strong = new HeapAllocationSite(Untyped.INSTANCE, "l", false, loc);
		HeapAllocationSite weak = new HeapAllocationSite(Untyped.INSTANCE, "l", true, loc);
		assertEquals(weak, strong.lub(weak));
		assertEquals(weak, weak.lub(strong));
	}

	@Test
	public void lubOfTwoStrongSitesWithTheSameNameIsEitherOfThem()
			throws SemanticException {
		HeapAllocationSite a = new HeapAllocationSite(Untyped.INSTANCE, "l", false, loc);
		HeapAllocationSite b = new HeapAllocationSite(Untyped.INSTANCE, "l", false, loc);
		assertEquals(a, a.lub(b));
	}

	@Test
	public void lubOfSitesWithDifferentNamesThrows() {
		HeapAllocationSite a = new HeapAllocationSite(Untyped.INSTANCE, "l", false, loc);
		HeapAllocationSite b = new HeapAllocationSite(Untyped.INSTANCE, "m", false, loc);
		assertThrows(SemanticException.class, () -> a.lub(b));
	}

	@Test
	public void isAllocationDefaultsToFalseUntilExplicitlySet() {
		HeapAllocationSite site = new HeapAllocationSite(Untyped.INSTANCE, "l", false, loc);
		assertFalse(site.isAllocation());
		site.setAllocation(true);
		assertTrue(site.isAllocation());
	}

}
