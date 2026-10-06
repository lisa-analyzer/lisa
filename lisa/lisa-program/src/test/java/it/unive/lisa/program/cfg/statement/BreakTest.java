package it.unive.lisa.program.cfg.statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.ProgramState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.program.testsupport.TestFixtures;
import it.unive.lisa.program.testsupport.UnitLattice;
import org.junit.jupiter.api.Test;

public class BreakTest {

	@Test
	public void hasLabelReflectsWhetherALabelWasGiven() {
		Break labeled = new Break(TestFixtures.CFG, TestFixtures.LOCATION, "outer");
		Break unlabeled = new Break(TestFixtures.CFG, TestFixtures.LOCATION, null);
		assertTrue(labeled.hasLabel());
		assertEquals("outer", labeled.getLabel());
		assertFalse(unlabeled.hasLabel());
		assertEquals(null, unlabeled.getLabel());
	}

	@Test
	public void alwaysBreaksControlFlow() {
		assertTrue(new Break(TestFixtures.CFG, TestFixtures.LOCATION, null).breaksControlFlow());
		assertTrue(new Break(TestFixtures.CFG, TestFixtures.LOCATION, "outer").breaksControlFlow());
	}

	@Test
	public void equalsHoldsForTheSameLabelAndLocation() {
		Break a = new Break(TestFixtures.CFG, TestFixtures.LOCATION, "outer");
		Break b = new Break(TestFixtures.CFG, TestFixtures.LOCATION, "outer");
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());

		Break unlabeledA = new Break(TestFixtures.CFG, TestFixtures.LOCATION, null);
		Break unlabeledB = new Break(TestFixtures.CFG, TestFixtures.LOCATION, null);
		assertEquals(unlabeledA, unlabeledB);
	}

	@Test
	public void equalsFailsForDifferentLabels() {
		Break labeled = new Break(TestFixtures.CFG, TestFixtures.LOCATION, "outer");
		Break unlabeled = new Break(TestFixtures.CFG, TestFixtures.LOCATION, null);
		Break otherLabel = new Break(TestFixtures.CFG, TestFixtures.LOCATION, "inner");
		assertFalse(labeled.equals(unlabeled));
		assertFalse(unlabeled.equals(labeled));
		assertFalse(labeled.equals(otherLabel));
	}

	@Test
	public void compareSameClassOrdersByLabelWithNullFirst() {
		Break unlabeled = new Break(TestFixtures.CFG, TestFixtures.LOCATION, null);
		Break a = new Break(TestFixtures.CFG, TestFixtures.LOCATION, "a");
		Break b = new Break(TestFixtures.CFG, TestFixtures.LOCATION, "b");

		assertTrue(unlabeled.compareSameClass(a) < 0);
		assertTrue(a.compareSameClass(unlabeled) > 0);
		assertTrue(a.compareSameClass(b) < 0);
		assertEquals(0, a.compareSameClass(new Break(TestFixtures.CFG, TestFixtures.LOCATION, "a")));
	}

	@Test
	public void toStringIncludesTheLabelOnlyWhenPresent() {
		assertEquals("break", new Break(TestFixtures.CFG, TestFixtures.LOCATION, null).toString());
		assertEquals("break outer", new Break(TestFixtures.CFG, TestFixtures.LOCATION, "outer").toString());
	}

	@Test
	public void forwardSemanticsReturnsTheEntryStateUnchanged()
			throws SemanticException {
		Break br = new Break(TestFixtures.CFG, TestFixtures.LOCATION, null);
		ProgramState<UnitLattice> programState = new ProgramState<>(UnitLattice.INSTANCE, new ExpressionSet());
		AnalysisState<UnitLattice> state = new AnalysisState<>(programState).withExecution(programState);

		assertSame(state, br.forwardSemantics(state, null, null));
	}

}
