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

public class ContinueTest {

	@Test
	public void hasLabelReflectsWhetherALabelWasGiven() {
		Continue labeled = new Continue(TestFixtures.CFG, TestFixtures.LOCATION, "outer");
		Continue unlabeled = new Continue(TestFixtures.CFG, TestFixtures.LOCATION, null);
		assertTrue(labeled.hasLabel());
		assertEquals("outer", labeled.getLabel());
		assertFalse(unlabeled.hasLabel());
		assertEquals(null, unlabeled.getLabel());
	}

	@Test
	public void alwaysContinuesControlFlow() {
		assertTrue(new Continue(TestFixtures.CFG, TestFixtures.LOCATION, null).continuesControlFlow());
		assertTrue(new Continue(TestFixtures.CFG, TestFixtures.LOCATION, "outer").continuesControlFlow());
	}

	@Test
	public void equalsHoldsForTheSameLabelAndLocation() {
		Continue a = new Continue(TestFixtures.CFG, TestFixtures.LOCATION, "outer");
		Continue b = new Continue(TestFixtures.CFG, TestFixtures.LOCATION, "outer");
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());

		Continue unlabeledA = new Continue(TestFixtures.CFG, TestFixtures.LOCATION, null);
		Continue unlabeledB = new Continue(TestFixtures.CFG, TestFixtures.LOCATION, null);
		assertEquals(unlabeledA, unlabeledB);
	}

	@Test
	public void equalsFailsForDifferentLabels() {
		Continue labeled = new Continue(TestFixtures.CFG, TestFixtures.LOCATION, "outer");
		Continue unlabeled = new Continue(TestFixtures.CFG, TestFixtures.LOCATION, null);
		Continue otherLabel = new Continue(TestFixtures.CFG, TestFixtures.LOCATION, "inner");
		assertFalse(labeled.equals(unlabeled));
		assertFalse(unlabeled.equals(labeled));
		assertFalse(labeled.equals(otherLabel));
	}

	@Test
	public void compareSameClassOrdersByLabelWithNullFirst() {
		Continue unlabeled = new Continue(TestFixtures.CFG, TestFixtures.LOCATION, null);
		Continue a = new Continue(TestFixtures.CFG, TestFixtures.LOCATION, "a");
		Continue b = new Continue(TestFixtures.CFG, TestFixtures.LOCATION, "b");

		assertTrue(unlabeled.compareSameClass(a) < 0);
		assertTrue(a.compareSameClass(unlabeled) > 0);
		assertTrue(a.compareSameClass(b) < 0);
		assertEquals(0, a.compareSameClass(new Continue(TestFixtures.CFG, TestFixtures.LOCATION, "a")));
	}

	@Test
	public void toStringIncludesTheLabelOnlyWhenPresent() {
		assertEquals("continue", new Continue(TestFixtures.CFG, TestFixtures.LOCATION, null).toString());
		assertEquals("continue outer", new Continue(TestFixtures.CFG, TestFixtures.LOCATION, "outer").toString());
	}

	@Test
	public void forwardSemanticsReturnsTheEntryStateUnchanged()
			throws SemanticException {
		Continue cont = new Continue(TestFixtures.CFG, TestFixtures.LOCATION, null);
		ProgramState<UnitLattice> programState = new ProgramState<>(UnitLattice.INSTANCE, new ExpressionSet());
		AnalysisState<UnitLattice> state = new AnalysisState<>(programState).withExecution(programState);

		assertSame(state, cont.forwardSemantics(state, null, null));
	}

}
