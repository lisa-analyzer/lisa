package it.unive.lisa.program.cfg.statement;

import static it.unive.lisa.program.cfg.statement.StatementTestFixtures.LOC;
import static it.unive.lisa.program.cfg.statement.StatementTestFixtures.emptyState;
import static it.unive.lisa.program.cfg.statement.StatementTestFixtures.interprocedural;
import static it.unive.lisa.program.cfg.statement.StatementTestFixtures.newCfg;
import static it.unive.lisa.program.cfg.statement.StatementTestFixtures.store;
import static org.junit.jupiter.api.Assertions.assertEquals;

import it.unive.lisa.TestAbstractState;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.symbolic.value.PushAny;
import it.unive.lisa.type.Untyped;
import org.junit.jupiter.api.Test;

public class DefaultParamInitializationTest {

	@Test
	public void toStringIsTheLatticeTopMarker() {
		DefaultParamInitialization d = new DefaultParamInitialization(newCfg(), LOC, Untyped.INSTANCE);
		assertEquals(Lattice.TOP_STRING, d.toString());
	}

	@Test
	public void forwardSemanticsLeavesAPushAnyOfItsStaticTypeAsTheComputedExpression()
			throws Exception {
		DefaultParamInitialization d = new DefaultParamInitialization(newCfg(), LOC, Untyped.INSTANCE);
		AnalysisState<TestAbstractState> entry = emptyState();
		AnalysisState<TestAbstractState> result = d.forwardSemantics(entry, interprocedural(), store(entry));
		assertEquals(new ExpressionSet(new PushAny(Untyped.INSTANCE, LOC)),
				result.getExecution().getComputedExpressions());
	}

}
