package it.unive.lisa.analysis.combination;

import static org.junit.jupiter.api.Assertions.assertEquals;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.lattices.Satisfiability;
import it.unive.lisa.lattices.SingleValueLattice;
import it.unive.lisa.program.SyntheticLocation;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.type.Untyped;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link ValueLatticeProduct}, in particular that {@code store} is
 * propagated to both components, as required by
 * {@link it.unive.lisa.analysis.value.LatticeWithReplacement}.
 */
public class ValueLatticeProductTest {

	private final Identifier x = new Variable(Untyped.INSTANCE, "x", SyntheticLocation.INSTANCE);

	private final Identifier y = new Variable(Untyped.INSTANCE, "y", SyntheticLocation.INSTANCE);

	@Test
	public void testStoreDelegatesToBothComponents()
			throws SemanticException {
		ValueEnvironment<Satisfiability> env = new ValueEnvironment<Satisfiability>(Satisfiability.UNKNOWN)
				.putState(x, Satisfiability.SATISFIED);
		SingleValueLattice single = SingleValueLattice.SINGLETON;

		ValueLatticeProduct<ValueEnvironment<Satisfiability>, SingleValueLattice> product = new ValueLatticeProduct<>(
				env, single);

		ValueLatticeProduct<ValueEnvironment<Satisfiability>, SingleValueLattice> stored = product.store(y, x);

		// the first component actually copies x's value onto y, the second
		// (a single-value lattice) is unaffected since it does not track
		// individual identifiers
		assertEquals(Satisfiability.SATISFIED, stored.first.getState(y));
		assertEquals(SingleValueLattice.SINGLETON, stored.second);
	}

}
