package it.unive.lisa.program.cfg.statement;

import static it.unive.lisa.program.cfg.statement.StatementTestFixtures.LOC;
import static it.unive.lisa.program.cfg.statement.StatementTestFixtures.emptyState;
import static it.unive.lisa.program.cfg.statement.StatementTestFixtures.interprocedural;
import static it.unive.lisa.program.cfg.statement.StatementTestFixtures.newCfg;
import static it.unive.lisa.program.cfg.statement.StatementTestFixtures.store;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.TestAbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.symbolic.value.Skip;
import org.junit.jupiter.api.Test;

public class NoOpRetTest {

	@Test
	public void noOpDoesNotStopExecution() {
		assertFalse(new NoOp(newCfg(), LOC).stopsExecution());
	}

	@Test
	public void retStopsExecutionWithoutThrowing() {
		Ret ret = new Ret(newCfg(), LOC);
		assertTrue(ret.stopsExecution());
		assertFalse(ret.throwsError());
	}

	@Test
	public void toStringIsFixed() {
		assertEquals("no-op", new NoOp(newCfg(), LOC).toString());
		assertEquals("ret", new Ret(newCfg(), LOC).toString());
	}

	@Test
	public void equalsAndHashCodeAreBasedOnLocationOnly() {
		assertEquals(new NoOp(newCfg(), LOC), new NoOp(newCfg(), LOC));
		assertEquals(new Ret(newCfg(), LOC), new Ret(newCfg(), LOC));
		assertFalse(new NoOp(newCfg(), LOC).equals(new Ret(newCfg(), LOC)));
	}

	@Test
	public void bothLeaveASkipAsTheirComputedExpression()
			throws Exception {
		AnalysisState<TestAbstractState> entry = emptyState();
		NoOp noop = new NoOp(newCfg(), LOC);
		AnalysisState<TestAbstractState> noopResult = noop.forwardSemantics(entry, interprocedural(), store(entry));
		assertEquals(new ExpressionSet(new Skip(LOC)), noopResult.getExecution().getComputedExpressions());

		entry = emptyState();
		Ret ret = new Ret(newCfg(), LOC);
		AnalysisState<TestAbstractState> retResult = ret.forwardSemantics(entry, interprocedural(), store(entry));
		assertEquals(new ExpressionSet(new Skip(LOC)), retResult.getExecution().getComputedExpressions());
	}

}
