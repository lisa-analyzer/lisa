package it.unive.lisa.analysis.informationFlow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.TestParameterProvider;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.lattices.GenericMapLattice;
import it.unive.lisa.lattices.informationFlow.NonInterferenceEnvironment;
import it.unive.lisa.lattices.informationFlow.NonInterferenceValue;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.program.type.BoolType;
import it.unive.lisa.program.type.Int32Type;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.PushInv;
import it.unive.lisa.symbolic.value.TernaryExpression;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.symbolic.value.operator.binary.NumericNonOverflowingAdd;
import it.unive.lisa.symbolic.value.operator.ternary.StringSubstring;
import it.unive.lisa.symbolic.value.operator.unary.NumericNegation;
import java.util.HashMap;
import org.junit.jupiter.api.Test;

public class NonInterferenceTest {

	private final ProgramPoint pp = TestParameterProvider.provideParam(null, ProgramPoint.class);
	private final SemanticOracle oracle = TestParameterProvider.provideParam(null, SemanticOracle.class);
	private final NonInterference domain = new NonInterference();

	private Variable annotated(
			boolean lowConf,
			boolean highInt) {
		Variable v = new Variable(Int32Type.INSTANCE, "x", pp.getLocation());
		if (lowConf)
			v.addAnnotation(NonInterference.LOW_CONF_ANNOTATION);
		if (highInt)
			v.addAnnotation(NonInterference.HIGH_INT_ANNOTATION);
		return v;
	}

	@Test
	public void anUnannotatedVariableIsHighConfidentialityLowIntegrity()
			throws SemanticException {
		assertEquals(NonInterferenceValue.HIGH_LOW, domain.fixedVariable(annotated(false, false), pp, oracle));
	}

	@Test
	public void lowConfidentialityAnnotationAlone()
			throws SemanticException {
		assertEquals(NonInterferenceValue.LOW_LOW, domain.fixedVariable(annotated(true, false), pp, oracle));
	}

	@Test
	public void highIntegrityAnnotationAlone()
			throws SemanticException {
		assertEquals(NonInterferenceValue.HIGH_HIGH, domain.fixedVariable(annotated(false, true), pp, oracle));
	}

	@Test
	public void bothAnnotationsTogether()
			throws SemanticException {
		assertEquals(NonInterferenceValue.LOW_HIGH, domain.fixedVariable(annotated(true, true), pp, oracle));
	}

	@Test
	public void constantsAreLowHigh()
			throws SemanticException {
		Constant c = new Constant(Int32Type.INSTANCE, 5, pp.getLocation());
		assertEquals(NonInterferenceValue.LOW_HIGH, domain.evalConstant(c, pp, oracle));
	}

	@Test
	public void unaryExpressionPreservesTheOperandLevel()
			throws SemanticException {
		UnaryExpression exp = new UnaryExpression(
				Int32Type.INSTANCE,
				new Variable(Int32Type.INSTANCE, "x", pp.getLocation()),
				NumericNegation.INSTANCE,
				pp.getLocation());
		assertEquals(NonInterferenceValue.HIGH_HIGH,
				domain.evalUnaryExpression(exp, NonInterferenceValue.HIGH_HIGH, pp, oracle));
	}

	@Test
	public void binaryExpressionIsTheLubOfItsOperands()
			throws SemanticException {
		BinaryExpression exp = new BinaryExpression(
				Int32Type.INSTANCE,
				new Variable(Int32Type.INSTANCE, "x", pp.getLocation()),
				new Variable(Int32Type.INSTANCE, "y", pp.getLocation()),
				NumericNonOverflowingAdd.INSTANCE,
				pp.getLocation());
		assertEquals(NonInterferenceValue.HIGH_LOW,
				domain.evalBinaryExpression(exp, NonInterferenceValue.HIGH_HIGH, NonInterferenceValue.LOW_LOW, pp,
						oracle));
	}

	@Test
	public void ternaryExpressionIsTheLubOfItsOperands()
			throws SemanticException {
		TernaryExpression exp = new TernaryExpression(
				Int32Type.INSTANCE,
				new Variable(Int32Type.INSTANCE, "x", pp.getLocation()),
				new Variable(Int32Type.INSTANCE, "y", pp.getLocation()),
				new Variable(Int32Type.INSTANCE, "z", pp.getLocation()),
				StringSubstring.INSTANCE,
				pp.getLocation());
		assertEquals(NonInterferenceValue.HIGH_LOW,
				domain.evalTernaryExpression(exp, NonInterferenceValue.LOW_HIGH, NonInterferenceValue.HIGH_HIGH,
						NonInterferenceValue.LOW_LOW, pp, oracle));
	}

	@Test
	public void topAndBottom() {
		assertEquals(NonInterferenceValue.HIGH_LOW, domain.top());
		assertEquals(NonInterferenceValue.BOTTOM, domain.bottom());
	}

	@Test
	public void canProcessRejectsInvalidPushAnyUnlessItsStaticTypeIsAValueType() {
		PushInv boolInv = new PushInv(BoolType.INSTANCE, pp.getLocation());
		assertTrue(domain.canProcess(boolInv, pp, oracle));
	}

	// after a call returns, the guards active before the call (the caller's)
	// must be restored, discarding whatever guards were accumulated inside
	// the callee - unless they already coincide, in which case the callee's
	// environment is returned unchanged
	@Test
	public void onCallReturnRestoresTheCallerGuards()
			throws SemanticException {
		GenericMapLattice<ProgramPoint, NonInterferenceValue> callerGuards = new GenericMapLattice<ProgramPoint,
				NonInterferenceValue>(NonInterferenceValue.LOW_HIGH).top().putState(pp, NonInterferenceValue.LOW_LOW);
		NonInterferenceEnvironment entry = new NonInterferenceEnvironment(NonInterferenceValue.HIGH_LOW,
				new HashMap<>(), callerGuards);

		GenericMapLattice<ProgramPoint, NonInterferenceValue> calleeGuards = new GenericMapLattice<ProgramPoint,
				NonInterferenceValue>(NonInterferenceValue.LOW_HIGH).top().putState(pp, NonInterferenceValue.HIGH_HIGH);
		NonInterferenceEnvironment callResult = new NonInterferenceEnvironment(NonInterferenceValue.HIGH_LOW,
				new HashMap<>(), calleeGuards);

		NonInterferenceEnvironment result = domain.onCallReturn(entry, callResult, pp);
		assertEquals(callerGuards, result.guards);
	}

	@Test
	public void onCallReturnIsANoOpWhenGuardsAlreadyMatch()
			throws SemanticException {
		GenericMapLattice<ProgramPoint, NonInterferenceValue> guards = new GenericMapLattice<ProgramPoint,
				NonInterferenceValue>(NonInterferenceValue.LOW_HIGH).top().putState(pp, NonInterferenceValue.LOW_LOW);
		NonInterferenceEnvironment entry = new NonInterferenceEnvironment(NonInterferenceValue.HIGH_LOW,
				new HashMap<>(), guards);
		NonInterferenceEnvironment callResult = new NonInterferenceEnvironment(NonInterferenceValue.HIGH_LOW,
				new HashMap<>(), guards);

		assertSame(callResult, domain.onCallReturn(entry, callResult, pp));
	}

}
