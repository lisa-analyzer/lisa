package it.unive.lisa.program.cfg.statement.logic;

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
import it.unive.lisa.program.type.BoolType;
import it.unive.lisa.program.type.Int32Type;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.symbolic.value.operator.unary.LogicalNegation;
import it.unive.lisa.type.Untyped;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class NotTest {

	private final Variable operand = new Variable(Untyped.INSTANCE, "operand", TestFixtures.LOCATION);

	private RecordingDomain domain;

	private FakeInterproceduralAnalysis interprocedural;

	private AnalysisState<UnitLattice> state;

	private StatementStore<UnitLattice> expressions;

	private Not not() {
		domain = new RecordingDomain();
		interprocedural = new FakeInterproceduralAnalysis(domain);
		ProgramState<UnitLattice> programState = new ProgramState<>(UnitLattice.INSTANCE, new ExpressionSet());
		state = new AnalysisState<>(programState).withExecution(programState);
		expressions = new StatementStore<>(state);
		VariableRef param = new VariableRef(TestFixtures.CFG, TestFixtures.LOCATION, "operand");
		return new Not(TestFixtures.CFG, TestFixtures.LOCATION, param);
	}

	@Test
	public void booleanOperandBuildsTheNegationOperatorAndDelegatesToSmallStepSemantics()
			throws SemanticException {
		Not not = not();
		domain.setRuntimeTypes(operand, Set.of(BoolType.INSTANCE));

		AnalysisState<UnitLattice> result = not.fwdUnarySemantics(interprocedural, state, operand, expressions);

		assertEquals(1, domain.smallStepCalls.size());
		UnaryExpression built = (UnaryExpression) domain.smallStepCalls.get(0);
		assertEquals(LogicalNegation.INSTANCE, built.getOperator());
		assertEquals(not.getStaticType(), built.getStaticType());
		assertEquals(operand, built.getExpression());
		assertEquals(UnitLattice.INSTANCE, result.getExecutionState());
	}

	@Test
	public void nonBooleanOperandYieldsBottomWithoutComputingTheNegation()
			throws SemanticException {
		Not not = not();
		domain.setRuntimeTypes(operand, Set.of(Int32Type.INSTANCE));

		AnalysisState<UnitLattice> result = not.fwdUnarySemantics(interprocedural, state, operand, expressions);

		assertTrue(result.getExecutionState().isBottom());
		assertTrue(domain.smallStepCalls.isEmpty());
	}

}
