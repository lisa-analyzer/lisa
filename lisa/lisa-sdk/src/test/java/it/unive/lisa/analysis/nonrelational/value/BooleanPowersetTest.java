package it.unive.lisa.analysis.nonrelational.value;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.TestAbstractDomain;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.lattices.Satisfiability;
import it.unive.lisa.program.SyntheticLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonEq;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonNe;
import it.unive.lisa.symbolic.value.operator.binary.LogicalAnd;
import it.unive.lisa.symbolic.value.operator.binary.LogicalOr;
import it.unive.lisa.symbolic.value.operator.unary.LogicalNegation;
import it.unive.lisa.type.Untyped;
import org.junit.jupiter.api.Test;

/**
 * Meaning-based tests for {@link BooleanPowerset}, checking that it correctly
 * models sets of boolean values through {@link Satisfiability}.
 */
public class BooleanPowersetTest {

	private static final ProgramPoint fake = new ProgramPoint() {

		@Override
		public CodeLocation getLocation() {
			return SyntheticLocation.INSTANCE;
		}

		@Override
		public CFG getCFG() {
			return null;
		}
	};

	private static final SemanticOracle oracle = new TestAbstractDomain().new TestOracle();

	private final BooleanPowerset dom = new BooleanPowerset();

	private final Identifier x = new Variable(Untyped.INSTANCE, "x", SyntheticLocation.INSTANCE);

	private Constant bool(
			boolean b) {
		return new Constant(Untyped.INSTANCE, b, SyntheticLocation.INSTANCE);
	}

	@Test
	public void testEvalBooleanConstants()
			throws SemanticException {
		ValueEnvironment<Satisfiability> env = new ValueEnvironment<>(Satisfiability.UNKNOWN);
		assertEquals(Satisfiability.SATISFIED, dom.eval(env, bool(true), fake, oracle));
		assertEquals(Satisfiability.NOT_SATISFIED, dom.eval(env, bool(false), fake, oracle));
	}

	@Test
	public void testEvalNonBooleanConstantIsUnknown()
			throws SemanticException {
		ValueEnvironment<Satisfiability> env = new ValueEnvironment<>(Satisfiability.UNKNOWN);
		Constant c = new Constant(Untyped.INSTANCE, 42, SyntheticLocation.INSTANCE);
		assertEquals(Satisfiability.UNKNOWN, dom.eval(env, c, fake, oracle));
	}

	@Test
	public void testEvalLogicalNegation()
			throws SemanticException {
		ValueEnvironment<Satisfiability> env = new ValueEnvironment<>(Satisfiability.UNKNOWN);
		UnaryExpression neg = new UnaryExpression(
				Untyped.INSTANCE,
				bool(true),
				LogicalNegation.INSTANCE,
				SyntheticLocation.INSTANCE);

		assertEquals(Satisfiability.NOT_SATISFIED, dom.eval(env, neg, fake, oracle));
	}

	@Test
	public void testEvalLogicalAndOr()
			throws SemanticException {
		ValueEnvironment<Satisfiability> env = new ValueEnvironment<>(Satisfiability.UNKNOWN);
		BinaryExpression and = new BinaryExpression(
				Untyped.INSTANCE,
				bool(true),
				bool(false),
				LogicalAnd.INSTANCE,
				SyntheticLocation.INSTANCE);
		BinaryExpression or = new BinaryExpression(
				Untyped.INSTANCE,
				bool(true),
				bool(false),
				LogicalOr.INSTANCE,
				SyntheticLocation.INSTANCE);

		assertEquals(Satisfiability.NOT_SATISFIED, dom.eval(env, and, fake, oracle));
		assertEquals(Satisfiability.SATISFIED, dom.eval(env, or, fake, oracle));
	}

	@Test
	public void testEvalComparisonEqAndNe()
			throws SemanticException {
		ValueEnvironment<Satisfiability> env = new ValueEnvironment<>(Satisfiability.UNKNOWN);
		BinaryExpression eq = new BinaryExpression(
				Untyped.INSTANCE,
				bool(true),
				bool(true),
				ComparisonEq.INSTANCE,
				SyntheticLocation.INSTANCE);
		BinaryExpression ne = new BinaryExpression(
				Untyped.INSTANCE,
				bool(true),
				bool(true),
				ComparisonNe.INSTANCE,
				SyntheticLocation.INSTANCE);

		assertEquals(Satisfiability.SATISFIED, dom.eval(env, eq, fake, oracle));
		assertEquals(Satisfiability.NOT_SATISFIED, dom.eval(env, ne, fake, oracle));
	}

	@Test
	public void testAssumeIdentifierSetsItToSatisfied()
			throws SemanticException {
		ValueEnvironment<Satisfiability> env = new ValueEnvironment<>(Satisfiability.UNKNOWN);
		ValueEnvironment<Satisfiability> assumed = dom.assume(env, x, fake, fake, oracle);
		assertEquals(Satisfiability.SATISFIED, assumed.getState(x));
	}

	@Test
	public void testAssumeNegatedIdentifierSetsItToNotSatisfied()
			throws SemanticException {
		ValueEnvironment<Satisfiability> env = new ValueEnvironment<>(Satisfiability.UNKNOWN);
		UnaryExpression negX = new UnaryExpression(Untyped.INSTANCE, x, LogicalNegation.INSTANCE,
				SyntheticLocation.INSTANCE);

		ValueEnvironment<Satisfiability> assumed = dom.assume(env, negX, fake, fake, oracle);
		assertEquals(Satisfiability.NOT_SATISFIED, assumed.getState(x));
	}

	@Test
	public void testAssumeFalseConstantYieldsBottom()
			throws SemanticException {
		ValueEnvironment<Satisfiability> env = new ValueEnvironment<>(Satisfiability.UNKNOWN);
		ValueEnvironment<Satisfiability> assumed = dom.assume(env, bool(false), fake, fake, oracle);
		assertTrue(assumed.isBottom());
	}

	@Test
	public void testSatisfiesDelegatesToTheStoredValue()
			throws SemanticException {
		ValueEnvironment<Satisfiability> env = new ValueEnvironment<Satisfiability>(Satisfiability.UNKNOWN)
				.putState(x, Satisfiability.SATISFIED);
		assertEquals(Satisfiability.SATISFIED, dom.satisfies(env, x, fake, oracle));
	}

}
