package it.unive.lisa.analysis.nonrelational.value;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.lattices.Satisfiability;
import it.unive.lisa.program.SyntheticLocation;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.type.Untyped;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link ValueEnvironment}, in particular its {@code store} semantics.
 */
public class ValueEnvironmentTest {

	private final Identifier x = new Variable(Untyped.INSTANCE, "x", SyntheticLocation.INSTANCE);

	private final Identifier y = new Variable(Untyped.INSTANCE, "y", SyntheticLocation.INSTANCE);

	@Test
	public void testStoreCopiesTheSourceValueOntoTheTarget()
			throws SemanticException {
		ValueEnvironment<Satisfiability> env = new ValueEnvironment<>(Satisfiability.UNKNOWN)
				.putState(x, Satisfiability.SATISFIED);

		ValueEnvironment<Satisfiability> stored = env.store(y, x);

		assertEquals(Satisfiability.SATISFIED, stored.getState(y));
		// the source is untouched by a store
		assertEquals(Satisfiability.SATISFIED, stored.getState(x));
	}

	@Test
	public void testStoreIsNoOpWhenSourceIsUnknown()
			throws SemanticException {
		ValueEnvironment<Satisfiability> env = new ValueEnvironment<>(Satisfiability.UNKNOWN);

		ValueEnvironment<Satisfiability> stored = env.store(y, x);

		assertFalse(stored.knowsIdentifier(y));
	}

	@Test
	public void testStoreOnTopOrBottomIsNoOp()
			throws SemanticException {
		ValueEnvironment<Satisfiability> top = new ValueEnvironment<Satisfiability>(Satisfiability.UNKNOWN).top();
		ValueEnvironment<Satisfiability> bottom = new ValueEnvironment<Satisfiability>(Satisfiability.UNKNOWN).bottom();

		assertTrue(top.store(y, x).isTop());
		assertTrue(bottom.store(y, x).isBottom());
	}

	@Test
	public void testTopAndBottomConventions() {
		ValueEnvironment<Satisfiability> empty = new ValueEnvironment<>(Satisfiability.UNKNOWN);
		assertTrue(empty.isTop(), "an empty environment over a top domain is top");

		ValueEnvironment<Satisfiability> emptyBottomDomain = new ValueEnvironment<>(Satisfiability.BOTTOM);
		assertTrue(emptyBottomDomain.isBottom(), "an empty environment over a bottom domain is bottom");
	}

}
