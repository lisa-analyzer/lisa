package it.unive.lisa.analysis.nonrelational.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.lattices.SingleTypeLattice;
import it.unive.lisa.program.SyntheticLocation;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.type.Untyped;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link TypeEnvironment}, in particular its {@code store} semantics.
 */
public class TypeEnvironmentTest {

	private final Identifier x = new Variable(Untyped.INSTANCE, "x", SyntheticLocation.INSTANCE);

	private final Identifier y = new Variable(Untyped.INSTANCE, "y", SyntheticLocation.INSTANCE);

	@Test
	public void testStoreCopiesTheSourceValueOntoTheTarget()
			throws SemanticException {
		TypeEnvironment<SingleTypeLattice> env = new TypeEnvironment<>(SingleTypeLattice.BOTTOM)
				.putState(x, SingleTypeLattice.SINGLETON);

		TypeEnvironment<SingleTypeLattice> stored = env.store(y, x);

		assertEquals(SingleTypeLattice.SINGLETON, stored.getState(y));
		assertEquals(SingleTypeLattice.SINGLETON, stored.getState(x));
	}

	@Test
	public void testStoreIsNoOpWhenSourceIsUnknown()
			throws SemanticException {
		TypeEnvironment<SingleTypeLattice> env = new TypeEnvironment<>(SingleTypeLattice.BOTTOM);

		TypeEnvironment<SingleTypeLattice> stored = env.store(y, x);

		assertFalse(stored.knowsIdentifier(y));
	}

	@Test
	public void testStoreOnTopOrBottomIsNoOp()
			throws SemanticException {
		TypeEnvironment<SingleTypeLattice> top = new TypeEnvironment<SingleTypeLattice>(SingleTypeLattice.SINGLETON)
				.top();
		TypeEnvironment<SingleTypeLattice> bottom = new TypeEnvironment<SingleTypeLattice>(SingleTypeLattice.SINGLETON)
				.bottom();

		assertTrue(top.store(y, x).isTop());
		assertTrue(bottom.store(y, x).isBottom());
	}

}
