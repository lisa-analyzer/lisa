package it.unive.lisa.program.cfg.statement;

import static it.unive.lisa.program.cfg.statement.StatementTestFixtures.LOC;
import static it.unive.lisa.program.cfg.statement.StatementTestFixtures.emptyState;
import static it.unive.lisa.program.cfg.statement.StatementTestFixtures.interprocedural;
import static it.unive.lisa.program.cfg.statement.StatementTestFixtures.newCfg;
import static it.unive.lisa.program.cfg.statement.StatementTestFixtures.store;
import static org.junit.jupiter.api.Assertions.assertEquals;

import it.unive.lisa.TestAbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.statement.evaluation.RightToLeftEvaluation;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.type.Untyped;
import org.junit.jupiter.api.Test;

public class AssignmentTest {

	@Test
	public void defaultEvaluationOrderIsRightToLeft() {
		Assignment a = new Assignment(newCfg(), LOC, new VariableRef(newCfg(), LOC, "x"),
				new VariableRef(newCfg(), LOC, "y"));
		assertEquals(RightToLeftEvaluation.INSTANCE, a.getOrder());
	}

	@Test
	public void toStringIsTargetEqualsExpression() {
		CFG cfg = newCfg();
		Assignment a = new Assignment(cfg, LOC, new VariableRef(cfg, LOC, "x"), new VariableRef(cfg, LOC, "y"));
		assertEquals("x = y", a.toString());
	}

	@Test
	public void forwardSemanticsAssignsTheComputedRightHandSideToTheComputedTarget()
			throws Exception {
		CFG cfg = newCfg();
		Assignment a = new Assignment(cfg, LOC, new VariableRef(cfg, LOC, "x"), new VariableRef(cfg, LOC, "y"));
		AnalysisState<TestAbstractState> entry = emptyState();
		AnalysisState<TestAbstractState> result = a.forwardSemantics(entry, interprocedural(), store(entry));

		Variable target = new Variable(Untyped.INSTANCE, "x", LOC);
		assertEquals(new ExpressionSet(target), result.getExecution().getComputedExpressions());
	}

}
