package it.unive.lisa.program.language.parameterassignment;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.analysis.AnalysisState;
import it.unive.lisa.analysis.ProgramState;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.StatementStore;
import it.unive.lisa.lattices.ExpressionSet;
import it.unive.lisa.program.cfg.Parameter;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.program.cfg.statement.VariableRef;
import it.unive.lisa.program.cfg.statement.call.Call.CallType;
import it.unive.lisa.program.cfg.statement.call.NamedParameterExpression;
import it.unive.lisa.program.cfg.statement.call.UnresolvedCall;
import it.unive.lisa.program.cfg.statement.literal.Int32Literal;
import it.unive.lisa.program.testsupport.FakeInterproceduralAnalysis;
import it.unive.lisa.program.testsupport.RecordingDomain;
import it.unive.lisa.program.testsupport.TestFixtures;
import it.unive.lisa.program.testsupport.UnitLattice;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.type.Untyped;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ParameterAssigningStrategyTest {

	private final Variable x = new Variable(Untyped.INSTANCE, "x", TestFixtures.LOCATION);

	private final Variable y = new Variable(Untyped.INSTANCE, "y", TestFixtures.LOCATION);

	private RecordingDomain domain;

	private FakeInterproceduralAnalysis interprocedural;

	private AnalysisState<UnitLattice> state;

	private StatementStore<UnitLattice> expressions;

	@BeforeEach
	public void init() {
		domain = new RecordingDomain();
		interprocedural = new FakeInterproceduralAnalysis(domain);
		// a non-empty stack, to check that it gets cleared by the strategies
		ProgramState<UnitLattice> programState = new ProgramState<>(UnitLattice.INSTANCE, new ExpressionSet(x));
		state = new AnalysisState<>(programState).withExecution(programState);
		expressions = new StatementStore<>(state);
	}

	private static Parameter param(
			String name) {
		return new Parameter(TestFixtures.LOCATION, name);
	}

	private static Parameter paramWithDefault(
			String name,
			Expression def) {
		return new Parameter(TestFixtures.LOCATION, name, def);
	}

	private static VariableRef ref(
			String name) {
		return new VariableRef(TestFixtures.CFG, TestFixtures.LOCATION, name);
	}

	private static UnresolvedCall call(
			Expression... actuals) {
		return new UnresolvedCall(TestFixtures.CFG, TestFixtures.LOCATION, CallType.STATIC, null, "f", actuals);
	}

	@Test
	public void orderPreservingPrepareAssignsFormals()
			throws SemanticException {
		Parameter[] formals = { param("a"), param("b") };
		ExpressionSet[] actuals = { new ExpressionSet(x), new ExpressionSet(y) };

		Pair<AnalysisState<UnitLattice>, ExpressionSet[]> res = OrderPreservingAssigningStrategy.INSTANCE.prepare(
				call(ref("x"), ref("y")), state, interprocedural, expressions, formals, actuals);

		assertEquals(List.of(x, y), domain.assignCalls);
		assertArrayEquals(actuals, res.getRight());
		assertTrue(res.getLeft().getExecutionExpressions().isEmpty());
	}

	@Test
	public void orderPreservingPrepareForNativeDoesNotAssignFormals()
			throws SemanticException {
		Parameter[] formals = { param("a"), param("b") };
		ExpressionSet[] actuals = { new ExpressionSet(x), new ExpressionSet(y) };

		Pair<AnalysisState<UnitLattice>, ExpressionSet[]> res = OrderPreservingAssigningStrategy.INSTANCE
				.prepareForNative(call(ref("x"), ref("y")), state, interprocedural, expressions, formals, actuals);

		assertTrue(domain.assignCalls.isEmpty());
		assertArrayEquals(actuals, res.getRight());
		assertTrue(res.getLeft().getExecutionExpressions().isEmpty());
		assertEquals(state.getExecutionState(), res.getLeft().getExecutionState());
	}

	@Test
	public void pythonLikePrepareForNativeShufflesKeywordArgumentsWithoutAssigningFormals()
			throws SemanticException {
		Parameter[] formals = { param("a"), param("b") };
		Expression[] callActuals = {
				new NamedParameterExpression(TestFixtures.CFG, TestFixtures.LOCATION, "b", ref("x")),
				new NamedParameterExpression(TestFixtures.CFG, TestFixtures.LOCATION, "a", ref("y")) };
		ExpressionSet[] actuals = { new ExpressionSet(x), new ExpressionSet(y) };

		Pair<AnalysisState<UnitLattice>, ExpressionSet[]> res = PythonLikeAssigningStrategy.INSTANCE
				.prepareForNative(call(callActuals), state, interprocedural, expressions, formals, actuals);

		assertTrue(domain.assignCalls.isEmpty());
		assertArrayEquals(new ExpressionSet[] { new ExpressionSet(y), new ExpressionSet(x) }, res.getRight());
		assertTrue(res.getLeft().getExecutionExpressions().isEmpty());
	}

	@Test
	public void pythonLikePrepareForNativeStillEvaluatesDefaultValues()
			throws SemanticException {
		Int32Literal def = new Int32Literal(TestFixtures.CFG, TestFixtures.LOCATION, 5);
		Parameter[] formals = { param("a"), paramWithDefault("b", def) };
		ExpressionSet[] actuals = { new ExpressionSet(x) };

		Pair<AnalysisState<UnitLattice>, ExpressionSet[]> res = PythonLikeAssigningStrategy.INSTANCE
				.prepareForNative(call(ref("x")), state, interprocedural, expressions, formals, actuals);

		assertTrue(domain.assignCalls.isEmpty());
		assertEquals(1, domain.smallStepCalls.size());
		SymbolicExpression constant = domain.smallStepCalls.get(0);
		assertTrue(constant instanceof Constant);
		assertEquals(5, ((Constant) constant).getValue());
		assertArrayEquals(new ExpressionSet[] { new ExpressionSet(x), new ExpressionSet(constant) }, res.getRight());
		assertTrue(res.getLeft().getExecutionExpressions().isEmpty());
	}

	@Test
	public void pythonLikePrepareAssignsFormalsIncludingDefaults()
			throws SemanticException {
		Int32Literal def = new Int32Literal(TestFixtures.CFG, TestFixtures.LOCATION, 5);
		Parameter[] formals = { param("a"), paramWithDefault("b", def) };
		ExpressionSet[] actuals = { new ExpressionSet(x) };

		Pair<AnalysisState<UnitLattice>, ExpressionSet[]> res = PythonLikeAssigningStrategy.INSTANCE
				.prepare(call(ref("x")), state, interprocedural, expressions, formals, actuals);

		assertEquals(List.of(x, domain.smallStepCalls.get(0)), domain.assignCalls);
		assertTrue(res.getLeft().getExecutionExpressions().isEmpty());
	}

	@Test
	public void pythonLikePrepareForNativeYieldsBottomOnTooManyArguments()
			throws SemanticException {
		Parameter[] formals = { param("a") };
		ExpressionSet[] actuals = { new ExpressionSet(x), new ExpressionSet(y) };

		Pair<AnalysisState<UnitLattice>, ExpressionSet[]> res = PythonLikeAssigningStrategy.INSTANCE
				.prepareForNative(call(ref("x"), ref("y")), state, interprocedural, expressions, formals, actuals);

		assertTrue(domain.assignCalls.isEmpty());
		assertTrue(res.getLeft().getExecutionState().isBottom());
	}

}
