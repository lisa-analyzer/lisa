package it.unive.lisa.analysis.combination.constraints;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.lattices.Satisfiability;
import it.unive.lisa.program.SyntheticLocation;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.type.Untyped;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link WholeValue}, in particular that {@code store} is delegated to
 * each of its components rather than collapsing the whole value to bottom.
 */
public class WholeValueTest {

	private final Identifier x = new Variable(Untyped.INSTANCE, "x", SyntheticLocation.INSTANCE);

	private final Identifier y = new Variable(Untyped.INSTANCE, "y", SyntheticLocation.INSTANCE);

	private ValueEnvironment<Satisfiability> envWith(
			Identifier id,
			Satisfiability value) {
		return new ValueEnvironment<Satisfiability>(Satisfiability.UNKNOWN).putState(id, value);
	}

	@Test
	public void testStoreCopiesTheSourceValueOnEachComponent()
			throws SemanticException {
		ValueEnvironment<Satisfiability> first = envWith(x, Satisfiability.SATISFIED);
		ValueEnvironment<Satisfiability> second = new ValueEnvironment<>(Satisfiability.UNKNOWN);
		WholeValue value = new WholeValue(first, second);

		WholeValue stored = value.store(y, x);

		assertEquals(Satisfiability.SATISFIED, ((ValueEnvironment<?>) stored.get(0)).getState(y));
		// the second component never had a mapping for x, so its store is a
		// no-op, as opposed to collapsing to bottom
		assertFalse(stored.isBottom());
		assertTrue(((ValueEnvironment<?>) stored.get(1)).isTop());
	}

	@Test
	public void testGetByIndex()
			throws SemanticException {
		ValueEnvironment<Satisfiability> first = envWith(x, Satisfiability.SATISFIED);
		WholeValue value = new WholeValue(first);

		assertEquals(first, value.get(0));
	}

	@Test
	public void testGetOfMissingTypeThrows() {
		WholeValue value = new WholeValue(envWith(x, Satisfiability.SATISFIED));
		assertThrows(SemanticException.class, () -> value.get(NotAComponent.class));
	}

	private interface NotAComponent
			extends
			it.unive.lisa.analysis.value.ValueLattice<NotAComponent> {
	}

	@Test
	public void testIsTopAndIsBottom() {
		WholeValue top = new WholeValue(new ValueEnvironment<Satisfiability>(Satisfiability.UNKNOWN).top());
		WholeValue bottom = new WholeValue(new ValueEnvironment<Satisfiability>(Satisfiability.UNKNOWN).bottom());

		assertTrue(top.isTop());
		assertTrue(bottom.isBottom());
	}

	@Test
	public void testForgetIdentifierDelegatesToTheComponent()
			throws SemanticException {
		WholeValue value = new WholeValue(envWith(x, Satisfiability.SATISFIED));

		WholeValue forgot = value.forgetIdentifier(x, null);

		assertFalse(((ValueEnvironment<?>) forgot.get(0)).knowsIdentifier(x));
	}

}
