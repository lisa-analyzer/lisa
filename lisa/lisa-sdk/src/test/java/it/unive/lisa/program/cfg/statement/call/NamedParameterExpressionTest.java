package it.unive.lisa.program.cfg.statement.call;

import static it.unive.lisa.program.cfg.statement.call.CallFixtures.LOC;
import static it.unive.lisa.program.cfg.statement.call.CallFixtures.newCfg;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import it.unive.lisa.TestAbstractDomain;
import it.unive.lisa.TestAbstractState;
import it.unive.lisa.TestInterproceduralAnalysis;
import it.unive.lisa.analysis.AbstractDomain;
import it.unive.lisa.analysis.Analysis;
import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.ProgramState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.statement.VariableRef;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.type.Untyped;
import org.junit.jupiter.api.Test;

public class NamedParameterExpressionTest {

	@Test
	public void staticTypeIsTheOneOfTheWrappedSubExpression() {
		CFG cfg = newCfg("c");
		VariableRef sub = new VariableRef(cfg, LOC, "x", Untyped.INSTANCE);
		NamedParameterExpression named = new NamedParameterExpression(cfg, LOC, "arg", sub);
		assertSame(Untyped.INSTANCE, named.getStaticType());
		assertEquals("arg", named.getParameterName());
	}

	@Test
	public void equalsAndHashCodeAreBasedOnTheParameterNameAndSuper() {
		CFG cfg = newCfg("c");
		NamedParameterExpression a = new NamedParameterExpression(cfg, LOC, "arg", new VariableRef(cfg, LOC, "x"));
		NamedParameterExpression b = new NamedParameterExpression(cfg, LOC, "arg", new VariableRef(cfg, LOC, "x"));
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());

		NamedParameterExpression different = new NamedParameterExpression(
				cfg, LOC, "other", new VariableRef(cfg, LOC, "x"));
		assertNotEquals(a, different);
	}

	@Test
	public void compareSameClassAndParamsComparesTheParameterName() {
		CFG cfg = newCfg("c");
		NamedParameterExpression a = new NamedParameterExpression(cfg, LOC, "a", new VariableRef(cfg, LOC, "x"));
		NamedParameterExpression b = new NamedParameterExpression(cfg, LOC, "b", new VariableRef(cfg, LOC, "x"));
		assertEquals("a".compareTo("b"), a.compareTo(b));
	}

	@Test
	public void fwdUnarySemanticsDelegatesToSmallStepSemanticsOnTheGivenExpression() throws SemanticException {
		CFG cfg = newCfg("c");
		NamedParameterExpression named = new NamedParameterExpression(cfg, LOC, "arg", new VariableRef(cfg, LOC, "x"));
		AnalysisState<TestAbstractState> entry = new AnalysisState<>(
				new ProgramState<>(new TestAbstractState(), new ExpressionSet()));
		Analysis<TestAbstractState, AbstractDomain<TestAbstractState>> analysis = new Analysis<>(
				new TestAbstractDomain());
		TestInterproceduralAnalysis<TestAbstractState,
				AbstractDomain<TestAbstractState>> interprocedural = new TestInterproceduralAnalysis<>() {
					@Override
					public Analysis<TestAbstractState, AbstractDomain<TestAbstractState>> getAnalysis() {
						return analysis;
					}
				};

		SymbolicExpression pushed = new Constant(Untyped.INSTANCE, 1, LOC);
		AnalysisState<TestAbstractState> result = named.fwdUnarySemantics(
				interprocedural, entry, pushed, new StatementStore<>(entry));

		assertEquals(1, result.getExecutionExpressions().size());
		assertSame(pushed, result.getExecutionExpressions().iterator().next());
	}

}
