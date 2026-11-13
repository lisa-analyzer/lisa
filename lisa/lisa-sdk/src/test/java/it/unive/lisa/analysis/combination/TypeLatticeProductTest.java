package it.unive.lisa.analysis.combination;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.lattices.SingleTypeLattice;
import it.unive.lisa.program.SyntheticLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.type.Untyped;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link TypeLatticeProduct}, checking that
 * {@link TypeCartesianCombination} correctly propagates operations to both of
 * its components.
 */
public class TypeLatticeProductTest {

	private static final ProgramPoint fake = new ProgramPoint() {

		@Override
		public CodeLocation getLocation() {
			return SyntheticLocation.INSTANCE;
		}

		@Override
		public CFG getCFG() {
			return null;
		}
	};

	private static final ScopeToken token = new ScopeToken(() -> SyntheticLocation.INSTANCE);

	private final Identifier x = new Variable(Untyped.INSTANCE, "x", SyntheticLocation.INSTANCE);

	private TypeLatticeProduct<SingleTypeLattice, SingleTypeLattice> mk(
			SingleTypeLattice first,
			SingleTypeLattice second) {
		return new TypeLatticeProduct<>(first, second);
	}

	@Test
	public void testPushPopScopeDelegate()
			throws SemanticException {
		TypeLatticeProduct<SingleTypeLattice, SingleTypeLattice> product = mk(SingleTypeLattice.SINGLETON,
				SingleTypeLattice.SINGLETON);

		TypeLatticeProduct<SingleTypeLattice, SingleTypeLattice> pushed = product.pushScope(token, fake);
		TypeLatticeProduct<SingleTypeLattice, SingleTypeLattice> popped = pushed.popScope(token, fake);

		assertEquals(SingleTypeLattice.SINGLETON, popped.first);
		assertEquals(SingleTypeLattice.SINGLETON, popped.second);
	}

	@Test
	public void testKnowsIdentifierIsTrueIfEitherComponentKnowsIt() {
		// SingleTypeLattice never tracks identifiers, so both branches of the
		// "or" are exercised as false here; this documents the propagation
		// contract even though it cannot be flipped to true with this fixture
		TypeLatticeProduct<SingleTypeLattice, SingleTypeLattice> product = mk(SingleTypeLattice.SINGLETON,
				SingleTypeLattice.SINGLETON);

		assertFalse(product.knowsIdentifier(x));
	}

	@Test
	public void testForgetIdentifierDelegatesToBothComponents()
			throws SemanticException {
		TypeLatticeProduct<SingleTypeLattice, SingleTypeLattice> product = mk(SingleTypeLattice.SINGLETON,
				SingleTypeLattice.BOTTOM);

		TypeLatticeProduct<SingleTypeLattice, SingleTypeLattice> forgot = product.forgetIdentifier(x, fake);

		// SingleTypeLattice#forgetIdentifier is the identity
		assertEquals(SingleTypeLattice.SINGLETON, forgot.first);
		assertEquals(SingleTypeLattice.BOTTOM, forgot.second);
	}

	@Test
	public void testIsTopAndIsBottom() {
		assertTrue(mk(SingleTypeLattice.SINGLETON, SingleTypeLattice.SINGLETON).isTop());
		assertTrue(mk(SingleTypeLattice.BOTTOM, SingleTypeLattice.BOTTOM).isBottom());
		assertFalse(mk(SingleTypeLattice.SINGLETON, SingleTypeLattice.BOTTOM).isTop());
	}

}
