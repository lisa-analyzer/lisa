package it.unive.lisa.program.cfg.statement.numeric;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.ProgramState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.program.cfg.statement.VariableRef;
import it.unive.lisa.program.testsupport.FakeInterproceduralAnalysis;
import it.unive.lisa.program.testsupport.RecordingDomain;
import it.unive.lisa.program.testsupport.TestFixtures;
import it.unive.lisa.program.testsupport.UnitLattice;
import it.unive.lisa.program.type.Int32Type;
import it.unive.lisa.program.type.StringType;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.symbolic.value.operator.unary.NumericNegation;
import it.unive.lisa.type.Untyped;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class NegationTest {

	private final Variable operand = new Variable(Untyped.INSTANCE, "operand", TestFixtures.LOCATION);

	private RecordingDomain domain;

	private FakeInterproceduralAnalysis interprocedural;

	private AnalysisState<UnitLattice> state;

	private StatementStore<UnitLattice> expressions;

	private Negation negation() {
		domain = new RecordingDomain();
		interprocedural = new FakeInterproceduralAnalysis(domain);
		ProgramState<UnitLattice> programState = new ProgramState<>(UnitLattice.INSTANCE, new ExpressionSet());
		state = new AnalysisState<>(programState).withExecution(programState);
		expressions = new StatementStore<>(state);
		VariableRef expr = new VariableRef(TestFixtures.CFG, TestFixtures.LOCATION, "operand");
		return new Negation(TestFixtures.CFG, TestFixtures.LOCATION, expr);
	}

	@Test
	public void numericOperandBuildsTheNegationOperatorAndDelegatesToSmallStepSemantics()
			throws SemanticException {
		Negation neg = negation();
		domain.setRuntimeTypes(operand, Set.of(Int32Type.INSTANCE));

		AnalysisState<UnitLattice> result = neg.fwdUnarySemantics(interprocedural, state, operand, expressions);

		assertEquals(1, domain.smallStepCalls.size());
		UnaryExpression built = (UnaryExpression) domain.smallStepCalls.get(0);
		assertEquals(NumericNegation.INSTANCE, built.getOperator());
		// per Negation's own source, the built expression's static type is the
		// OPERAND's static type (operand.getStaticType()), not
		// neg.getStaticType()
		assertEquals(operand.getStaticType(), built.getStaticType());
		assertEquals(operand, built.getExpression());
		assertEquals(neg.getLocation(), built.getCodeLocation());
		assertEquals(UnitLattice.INSTANCE, result.getExecutionState());
		assertEquals(built, result.getExecutionExpressions().elements().iterator().next());
	}

	@Test
	public void nonNumericOperandYieldsBottomWithoutComputingTheNegation()
			throws SemanticException {
		Negation neg = negation();
		domain.setRuntimeTypes(operand, Set.of(StringType.INSTANCE));

		AnalysisState<UnitLattice> result = neg.fwdUnarySemantics(interprocedural, state, operand, expressions);

		assertTrue(result.getExecutionState().isBottom());
		assertTrue(domain.smallStepCalls.isEmpty());
	}

}
