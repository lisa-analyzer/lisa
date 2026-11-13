package it.unive.lisa.program.cfg.statement;

import static it.unive.lisa.program.cfg.statement.StatementTestFixtures.LOC;
import static it.unive.lisa.program.cfg.statement.StatementTestFixtures.emptyState;
import static it.unive.lisa.program.cfg.statement.StatementTestFixtures.interprocedural;
import static it.unive.lisa.program.cfg.statement.StatementTestFixtures.newCfg;
import static it.unive.lisa.program.cfg.statement.StatementTestFixtures.store;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.TestAbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.symbolic.CFGThrow;
import it.unive.lisa.symbolic.value.CFGReturn;
import it.unive.lisa.type.Untyped;
import org.junit.jupiter.api.Test;

public class ReturnThrowTest {

	@Test
	public void returnStopsExecutionButDoesNotThrow() {
		Return r = new Return(newCfg(), LOC, new VariableRef(newCfg(), LOC, "x"));
		assertTrue(r.stopsExecution());
		assertFalse(r.throwsError());
	}

	@Test
	public void throwStopsExecutionAndThrows() {
		Throw t = new Throw(newCfg(), LOC, new VariableRef(newCfg(), LOC, "x"));
		assertTrue(t.stopsExecution());
		assertTrue(t.throwsError());
	}

	@Test
	public void returnYieldsItsSubExpressionAndBuildsACFGReturnMetaVariable() {
		CFG cfg = newCfg();
		VariableRef sub = new VariableRef(cfg, LOC, "x");
		Return r = new Return(cfg, LOC, sub);
		assertSame(sub, r.yieldedValue());
		assertEquals(new CFGReturn(cfg, Untyped.INSTANCE, LOC), r.getMetaVariable());
	}

	@Test
	public void throwYieldsItsSubExpressionAndBuildsACFGThrowMetaVariable() {
		CFG cfg = newCfg();
		VariableRef sub = new VariableRef(cfg, LOC, "x");
		Throw t = new Throw(cfg, LOC, sub);
		assertSame(sub, t.yieldedValue());
		assertEquals(new CFGThrow(cfg, Untyped.INSTANCE, LOC), t.getMetaVariable());
	}

	@Test
	public void withValueBuildsAnEquivalentInstanceYieldingTheGivenValue() {
		CFG cfg = newCfg();
		VariableRef newValue = new VariableRef(cfg, LOC, "y");

		Return r = new Return(cfg, LOC, new VariableRef(cfg, LOC, "x"));
		Statement withValue = r.withValue(newValue);
		assertTrue(withValue instanceof Return);
		assertSame(newValue, ((Return) withValue).yieldedValue());

		Throw t = new Throw(cfg, LOC, new VariableRef(cfg, LOC, "x"));
		Statement rethrown = t.withValue(newValue);
		assertTrue(rethrown instanceof Throw);
		assertSame(newValue, ((Throw) rethrown).yieldedValue());
	}

	@Test
	public void isAtomicReflectsWhetherTheYieldedValueIsAVariableRefOrLiteral() {
		CFG cfg = newCfg();
		Return atomic = new Return(cfg, LOC, new VariableRef(cfg, LOC, "x"));
		assertTrue(atomic.isAtomic());

		// an assignment is neither a VariableRef nor a Literal
		Return nonAtomic = new Return(cfg, LOC,
				new Assignment(cfg, LOC, new VariableRef(cfg, LOC, "x"), new VariableRef(cfg, LOC, "y")));
		assertFalse(nonAtomic.isAtomic());
	}

	@Test
	public void returnForwardSemanticsAssignsTheComputedSubExpressionToTheCFGReturnMetaVariable()
			throws Exception {
		CFG cfg = newCfg();
		Return r = new Return(cfg, LOC, new VariableRef(cfg, LOC, "x"));
		AnalysisState<TestAbstractState> entry = emptyState();
		AnalysisState<TestAbstractState> result = r.forwardSemantics(entry, interprocedural(), store(entry));

		assertEquals(new ExpressionSet(r.getMetaVariable()), result.getExecution().getComputedExpressions());
	}

}
