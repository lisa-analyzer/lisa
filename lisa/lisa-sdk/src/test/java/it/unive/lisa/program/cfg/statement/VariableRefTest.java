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
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.type.Untyped;
import org.junit.jupiter.api.Test;

public class VariableRefTest {

	@Test
	public void getVariableBuildsAVariableWithTheSameNameLocationAndType() {
		CFG cfg = newCfg();
		VariableRef v = new VariableRef(cfg, LOC, "x");
		assertEquals(new Variable(Untyped.INSTANCE, "x", LOC), v.getVariable());
	}

	@Test
	public void equalsAndHashCodeAreAlsoBasedOnTheName() {
		CFG cfg = newCfg();
		VariableRef a = new VariableRef(cfg, LOC, "x");
		VariableRef b = new VariableRef(cfg, LOC, "x");
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());

		VariableRef differentName = new VariableRef(cfg, LOC, "y");
		assertFalse(a.equals(differentName));
	}

	@Test
	public void compareSameClassComparesByName() {
		CFG cfg = newCfg();
		VariableRef a = new VariableRef(cfg, LOC, "a");
		VariableRef z = new VariableRef(cfg, LOC, "z");
		assertTrue(a.compareTo(z) < 0);
	}

	@Test
	public void toStringIsTheVariableName() {
		assertEquals("x", new VariableRef(newCfg(), LOC, "x").toString());
	}

	@Test
	public void forwardSemanticsLeavesTheVariableItselfAsTheComputedExpression()
			throws Exception {
		VariableRef v = new VariableRef(newCfg(), LOC, "x");
		AnalysisState<TestAbstractState> entry = emptyState();
		AnalysisState<TestAbstractState> result = v.forwardSemantics(entry, interprocedural(), store(entry));
		assertEquals(new ExpressionSet(v.getVariable()), result.getExecution().getComputedExpressions());
	}

}
