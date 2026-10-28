package it.unive.lisa.program.cfg.statement;

import static it.unive.lisa.program.cfg.statement.StatementTestFixtures.LOC;
import static it.unive.lisa.program.cfg.statement.StatementTestFixtures.newCfg;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.cfg.CFG;
import org.junit.jupiter.api.Test;

public class StatementTest {

	@Test
	public void equalsAndHashCodeAreBasedOnClassAndLocation() {
		CFG cfg = newCfg();
		NoOp a = new NoOp(cfg, LOC);
		NoOp b = new NoOp(cfg, LOC);
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());

		NoOp differentLocation = new NoOp(cfg, new SourceCodeLocation("test", 2, 2));
		assertFalse(a.equals(differentLocation));

		Ret differentClass = new Ret(cfg, LOC);
		assertFalse(a.equals(differentClass));
	}

	@Test
	public void compareToOrdersByLocationThenClassNameThenCompareSameClass() {
		CFG cfg = newCfg();
		NoOp early = new NoOp(cfg, new SourceCodeLocation("test", 1, 1));
		NoOp late = new NoOp(cfg, new SourceCodeLocation("test", 2, 1));
		assertTrue(early.compareTo(late) < 0);
		assertTrue(late.compareTo(early) > 0);
		assertEquals(0, early.compareTo(new NoOp(cfg, new SourceCodeLocation("test", 1, 1))));

		// same location, different class: ordered by class name
		Ret ret = new Ret(cfg, new SourceCodeLocation("test", 1, 1));
		assertEquals(
				NoOp.class.getName().compareTo(Ret.class.getName()) < 0,
				early.compareTo(ret) < 0);
	}

	@Test
	public void defaultStatementIsNotStandaloneAndDoesNotAlterControlFlow() {
		NoOp noop = new NoOp(newCfg(), LOC);
		assertFalse(noop.stopsExecution());
		assertFalse(noop.throwsError());
		assertFalse(noop.breaksControlFlow());
		assertFalse(noop.continuesControlFlow());
	}

	@Test
	public void aStatementNotContainedInAnythingHasNoEvaluationPredecessorOrSuccessor() {
		NoOp noop = new NoOp(newCfg(), LOC);
		assertNull(noop.getEvaluationPredecessor());
		assertNull(noop.getEvaluationSuccessor());
		assertNull(noop.getStatementEvaluatedBefore(noop));
		assertNull(noop.getStatementEvaluatedAfter(noop));
	}

}
