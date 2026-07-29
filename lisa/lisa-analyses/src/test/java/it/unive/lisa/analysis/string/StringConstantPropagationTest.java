package it.unive.lisa.analysis.string;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.TestParameterProvider;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.lattices.Satisfiability;
import it.unive.lisa.lattices.string.StringConstant;
import it.unive.lisa.program.SyntheticLocation;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.program.type.Int32Type;
import it.unive.lisa.program.type.StringType;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.TernaryExpression;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonEq;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonNe;
import it.unive.lisa.symbolic.value.operator.binary.StringCharAt;
import it.unive.lisa.symbolic.value.operator.binary.StringConcat;
import it.unive.lisa.symbolic.value.operator.binary.StringContains;
import it.unive.lisa.symbolic.value.operator.binary.StringEndsWith;
import it.unive.lisa.symbolic.value.operator.binary.StringEquals;
import it.unive.lisa.symbolic.value.operator.binary.StringIsPrefixOf;
import it.unive.lisa.symbolic.value.operator.binary.StringStartsWith;
import it.unive.lisa.symbolic.value.operator.binary.StringSubstringToEnd;
import it.unive.lisa.symbolic.value.operator.ternary.StringReplace;
import it.unive.lisa.symbolic.value.operator.ternary.StringReplaceAll;
import it.unive.lisa.symbolic.value.operator.ternary.StringSubstring;
import it.unive.lisa.symbolic.value.operator.unary.NumericToString;
import it.unive.lisa.symbolic.value.operator.unary.StringLength;
import it.unive.lisa.symbolic.value.operator.unary.StringReverse;
import it.unive.lisa.symbolic.value.operator.unary.StringToLowerCase;
import it.unive.lisa.symbolic.value.operator.unary.StringToUpperCase;
import it.unive.lisa.symbolic.value.operator.unary.StringTrim;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class StringConstantPropagationTest {

	@Test
	public void testEvalBinary() throws SemanticException {
		StringConstantPropagation domain = new StringConstantPropagation();
		StringConstant s1 = new StringConstant("abc");
		StringConstant s2 = new StringConstant("def");

		StringConstant res = domain.evalBinaryExpression(
				new BinaryExpression(
						StringType.INSTANCE,
						new Variable(StringType.INSTANCE, "x", SyntheticLocation.INSTANCE),
						new Variable(StringType.INSTANCE, "y", SyntheticLocation.INSTANCE),
						StringConcat.INSTANCE,
						SyntheticLocation.INSTANCE),
				s1,
				s2,
				null,
				new TestParameterProvider.FakeOracle());

		assertEquals(res, new StringConstant("abcdef"));
	}

	@Test
	public void testEvalTernary() throws SemanticException {
		StringConstantPropagation domain = new StringConstantPropagation();
		StringConstant s1 = new StringConstant("aaa");
		StringConstant s2 = new StringConstant("aa");
		StringConstant s3 = new StringConstant("b");

		StringConstant res = domain.evalTernaryExpression(
				new TernaryExpression(
						StringType.INSTANCE,
						new Variable(StringType.INSTANCE, "x", SyntheticLocation.INSTANCE),
						new Variable(StringType.INSTANCE, "y", SyntheticLocation.INSTANCE),
						new Variable(StringType.INSTANCE, "z", SyntheticLocation.INSTANCE),
						StringReplace.INSTANCE,
						SyntheticLocation.INSTANCE),
				s1,
				s2,
				s3,
				null,
				new TestParameterProvider.FakeOracle());

		assertEquals(res, new StringConstant("ba"));
	}

	// ---- inner WVAOracle ----

	private static class WVAOracle
			extends
			TestParameterProvider.FakeOracle {
		private final Set<BinaryExpression> cs;

		WVAOracle(
				Set<BinaryExpression> cs) {
			this.cs = cs;
		}

		@Override
		public boolean hasWholeValueAnlysis() {
			return true;
		}

		@Override
		public Set<BinaryExpression> constraints(
				ValueDomain<?> requesting,
				ValueExpression e,
				ProgramPoint pp) {
			return cs;
		}
	}

	private static final ProgramPoint PP = TestParameterProvider.provideParam(null, ProgramPoint.class);

	private static final Variable VAR_X = new Variable(StringType.INSTANCE, "x", SyntheticLocation.INSTANCE);
	private static final Variable VAR_Y = new Variable(StringType.INSTANCE, "y", SyntheticLocation.INSTANCE);
	private static final Variable VAR_Z = new Variable(StringType.INSTANCE, "z", SyntheticLocation.INSTANCE);

	private static BinaryExpression mkBin(
			it.unive.lisa.symbolic.value.operator.binary.BinaryOperator op) {
		return new BinaryExpression(StringType.INSTANCE, VAR_X, VAR_Y, op, SyntheticLocation.INSTANCE);
	}

	private static UnaryExpression mkUnary(
			it.unive.lisa.symbolic.value.operator.unary.UnaryOperator op) {
		return new UnaryExpression(StringType.INSTANCE, VAR_X, op, SyntheticLocation.INSTANCE);
	}

	private static TernaryExpression mkTernary(
			it.unive.lisa.symbolic.value.operator.ternary.TernaryOperator op) {
		return new TernaryExpression(StringType.INSTANCE, VAR_X, VAR_Y, VAR_Z, op, SyntheticLocation.INSTANCE);
	}

	/**
	 * Returns a constraint set: Constant(val) == VAR_X, usable by
	 * IntegerConstantPropagation.generate().
	 */
	private static Set<BinaryExpression> intConstraint(
			int val) {
		Set<BinaryExpression> cs = new HashSet<>();
		cs.add(new BinaryExpression(
				Int32Type.INSTANCE,
				new Constant(Int32Type.INSTANCE, val, SyntheticLocation.INSTANCE),
				VAR_X,
				ComparisonEq.INSTANCE,
				SyntheticLocation.INSTANCE));
		return cs;
	}

	/**
	 * Returns a constraint set: Constant(val) == VAR_X, usable by
	 * StringConstantPropagation.generate().
	 */
	private static Set<BinaryExpression> strConstraint(
			String val) {
		Set<BinaryExpression> cs = new HashSet<>();
		cs.add(new BinaryExpression(
				StringType.INSTANCE,
				new Constant(StringType.INSTANCE, val, SyntheticLocation.INSTANCE),
				VAR_X,
				ComparisonEq.INSTANCE,
				SyntheticLocation.INSTANCE));
		return cs;
	}

	// ---- evalConstant ----

	@Test
	public void evalConstantString() throws SemanticException {
		StringConstantPropagation domain = new StringConstantPropagation();
		Constant c = new Constant(StringType.INSTANCE, "hello", SyntheticLocation.INSTANCE);
		assertEquals(new StringConstant("hello"), domain.evalConstant(c, null, new TestParameterProvider.FakeOracle()));
	}

	@Test
	public void evalConstantNonStringReturnsTop() throws SemanticException {
		StringConstantPropagation domain = new StringConstantPropagation();
		Constant c = new Constant(Int32Type.INSTANCE, 42, SyntheticLocation.INSTANCE);
		assertTrue(domain.evalConstant(c, null, new TestParameterProvider.FakeOracle()).isTop());
	}

	// ---- evalUnaryExpression ----

	@Test
	public void evalUnaryReverse() throws SemanticException {
		StringConstantPropagation domain = new StringConstantPropagation();
		assertEquals(new StringConstant("olleh"),
				domain.evalUnaryExpression(mkUnary(StringReverse.INSTANCE),
						new StringConstant("hello"), null, new TestParameterProvider.FakeOracle()));
	}

	@Test
	public void evalUnaryToLowerCase() throws SemanticException {
		StringConstantPropagation domain = new StringConstantPropagation();
		assertEquals(new StringConstant("hello"),
				domain.evalUnaryExpression(mkUnary(StringToLowerCase.INSTANCE),
						new StringConstant("HELLO"), null, new TestParameterProvider.FakeOracle()));
	}

	@Test
	public void evalUnaryToUpperCase() throws SemanticException {
		StringConstantPropagation domain = new StringConstantPropagation();
		assertEquals(new StringConstant("HELLO"),
				domain.evalUnaryExpression(mkUnary(StringToUpperCase.INSTANCE),
						new StringConstant("hello"), null, new TestParameterProvider.FakeOracle()));
	}

	@Test
	public void evalUnaryTrim() throws SemanticException {
		StringConstantPropagation domain = new StringConstantPropagation();
		assertEquals(new StringConstant("hi"),
				domain.evalUnaryExpression(mkUnary(StringTrim.INSTANCE),
						new StringConstant("  hi  "), null, new TestParameterProvider.FakeOracle()));
	}

	@Test
	public void evalUnaryTopArgReturnsTop() throws SemanticException {
		StringConstantPropagation domain = new StringConstantPropagation();
		assertTrue(domain.evalUnaryExpression(mkUnary(StringToUpperCase.INSTANCE),
				StringConstant.TOP, null, new TestParameterProvider.FakeOracle()).isTop());
	}

	@Test
	public void evalUnaryNumericToStringWVA() throws SemanticException {
		StringConstantPropagation domain = new StringConstantPropagation();
		// WVA: oracle provides "42" == expr → should give StringConstant("42")
		assertEquals(new StringConstant("42"),
				domain.evalUnaryExpression(mkUnary(NumericToString.INSTANCE),
						StringConstant.TOP, null, new WVAOracle(strConstraint("42"))));
	}

	// ---- evalBinaryExpression ----

	@Test
	public void evalBinaryConcatTopLeftReturnsTop() throws SemanticException {
		StringConstantPropagation domain = new StringConstantPropagation();
		assertTrue(domain.evalBinaryExpression(mkBin(StringConcat.INSTANCE),
				StringConstant.TOP, new StringConstant("world"), null,
				new TestParameterProvider.FakeOracle()).isTop());
	}

	@Test
	public void evalBinaryConcatTopRightReturnsTop() throws SemanticException {
		StringConstantPropagation domain = new StringConstantPropagation();
		assertTrue(domain.evalBinaryExpression(mkBin(StringConcat.INSTANCE),
				new StringConstant("hello"), StringConstant.TOP, null,
				new TestParameterProvider.FakeOracle()).isTop());
	}

	@Test
	public void evalBinaryCharAtWVA() throws SemanticException {
		StringConstantPropagation domain = new StringConstantPropagation();
		// WVA: charAt index=1 on "hello" → "e"
		assertEquals(new StringConstant("e"),
				domain.evalBinaryExpression(mkBin(StringCharAt.INSTANCE),
						new StringConstant("hello"), StringConstant.TOP, null,
						new WVAOracle(intConstraint(1))));
	}

	@Test
	public void evalBinarySubstringToEndWVA() throws SemanticException {
		StringConstantPropagation domain = new StringConstantPropagation();
		// WVA: substringToEnd(2) on "hello" → "llo"
		assertEquals(new StringConstant("llo"),
				domain.evalBinaryExpression(mkBin(StringSubstringToEnd.INSTANCE),
						new StringConstant("hello"), StringConstant.TOP, null,
						new WVAOracle(intConstraint(2))));
	}

	// ---- evalTernaryExpression ----

	@Test
	public void evalTernaryReplaceAll() throws SemanticException {
		StringConstantPropagation domain = new StringConstantPropagation();
		assertEquals(new StringConstant("aXbXc"),
				domain.evalTernaryExpression(mkTernary(StringReplaceAll.INSTANCE),
						new StringConstant("a1b2c"), new StringConstant("[0-9]"),
						new StringConstant("X"), null, new TestParameterProvider.FakeOracle()));
	}

	@Test
	public void evalTernaryTopLeftReturnsTop() throws SemanticException {
		StringConstantPropagation domain = new StringConstantPropagation();
		assertTrue(domain.evalTernaryExpression(mkTernary(StringReplace.INSTANCE),
				StringConstant.TOP, new StringConstant("a"), new StringConstant("b"),
				null, new TestParameterProvider.FakeOracle()).isTop());
	}

	@Test
	public void evalTernarySubstringWVA() throws SemanticException {
		StringConstantPropagation domain = new StringConstantPropagation();
		// WVA: substring("hello", 1, 4) → "ell"
		// oracle provides middle=1, right=4 constraints — since oracle returns
		// same cs for both calls,
		// we need a single value that works for both begin and end.
		// Let's test substring(1,4) on "hello": we need two distinct
		// constraints.
		// We use a different oracle approach: return mid=1 for middle, end=4
		// for right.
		// Since the oracle is called twice (once for middle, once for right),
		// and we
		// return the same constraint set for both, we test the case where both
		// are equal.
		// Let's test "hello".substring(2,2) → ""
		WVAOracle oracle = new WVAOracle(intConstraint(2));
		assertEquals(new StringConstant(""),
				domain.evalTernaryExpression(mkTernary(StringSubstring.INSTANCE),
						new StringConstant("hello"), StringConstant.TOP, StringConstant.TOP,
						null, oracle));
	}

	// ---- satisfiesBinaryExpression ----

	@Test
	public void satisfiesBinaryEqSatisfied() throws SemanticException {
		StringConstantPropagation domain = new StringConstantPropagation();
		assertEquals(Satisfiability.SATISFIED,
				domain.satisfiesBinaryExpression(mkBin(ComparisonEq.INSTANCE),
						new StringConstant("hi"), new StringConstant("hi"),
						null, new TestParameterProvider.FakeOracle()));
	}

	@Test
	public void satisfiesBinaryEqNotSatisfied() throws SemanticException {
		StringConstantPropagation domain = new StringConstantPropagation();
		assertEquals(Satisfiability.NOT_SATISFIED,
				domain.satisfiesBinaryExpression(mkBin(ComparisonEq.INSTANCE),
						new StringConstant("hi"), new StringConstant("bye"),
						null, new TestParameterProvider.FakeOracle()));
	}

	@Test
	public void satisfiesBinaryNeNotSatisfied() throws SemanticException {
		StringConstantPropagation domain = new StringConstantPropagation();
		assertEquals(Satisfiability.NOT_SATISFIED,
				domain.satisfiesBinaryExpression(mkBin(ComparisonNe.INSTANCE),
						new StringConstant("hi"), new StringConstant("hi"),
						null, new TestParameterProvider.FakeOracle()));
	}

	@Test
	public void satisfiesBinaryContains() throws SemanticException {
		StringConstantPropagation domain = new StringConstantPropagation();
		assertEquals(Satisfiability.SATISFIED,
				domain.satisfiesBinaryExpression(mkBin(StringContains.INSTANCE),
						new StringConstant("hello world"), new StringConstant("world"),
						null, new TestParameterProvider.FakeOracle()));
		assertEquals(Satisfiability.NOT_SATISFIED,
				domain.satisfiesBinaryExpression(mkBin(StringContains.INSTANCE),
						new StringConstant("hello"), new StringConstant("xyz"),
						null, new TestParameterProvider.FakeOracle()));
	}

	@Test
	public void satisfiesBinaryStartsWith() throws SemanticException {
		StringConstantPropagation domain = new StringConstantPropagation();
		assertEquals(Satisfiability.SATISFIED,
				domain.satisfiesBinaryExpression(mkBin(StringStartsWith.INSTANCE),
						new StringConstant("hello"), new StringConstant("hel"),
						null, new TestParameterProvider.FakeOracle()));
		assertEquals(Satisfiability.NOT_SATISFIED,
				domain.satisfiesBinaryExpression(mkBin(StringStartsWith.INSTANCE),
						new StringConstant("hello"), new StringConstant("lo"),
						null, new TestParameterProvider.FakeOracle()));
	}

	@Test
	public void satisfiesBinaryEndsWith() throws SemanticException {
		StringConstantPropagation domain = new StringConstantPropagation();
		assertEquals(Satisfiability.SATISFIED,
				domain.satisfiesBinaryExpression(mkBin(StringEndsWith.INSTANCE),
						new StringConstant("hello"), new StringConstant("llo"),
						null, new TestParameterProvider.FakeOracle()));
	}

	@Test
	public void satisfiesBinaryIsPrefixOf() throws SemanticException {
		StringConstantPropagation domain = new StringConstantPropagation();
		// "he" isPrefixOf "hello" → "hello".startsWith("he") → SATISFIED
		assertEquals(Satisfiability.SATISFIED,
				domain.satisfiesBinaryExpression(mkBin(StringIsPrefixOf.INSTANCE),
						new StringConstant("he"), new StringConstant("hello"),
						null, new TestParameterProvider.FakeOracle()));
	}

	@Test
	public void satisfiesBinaryTopReturnsUnknown() throws SemanticException {
		StringConstantPropagation domain = new StringConstantPropagation();
		assertEquals(Satisfiability.UNKNOWN,
				domain.satisfiesBinaryExpression(mkBin(StringEquals.INSTANCE),
						StringConstant.TOP, new StringConstant("hi"),
						null, new TestParameterProvider.FakeOracle()));
	}

	// ---- assumeBinaryExpression ----

	@Test
	public void assumeEqRefinesIdentifier() throws SemanticException {
		StringConstantPropagation domain = new StringConstantPropagation();
		ValueEnvironment<StringConstant> env = domain.makeLattice();
		BinaryExpression expr = new BinaryExpression(
				StringType.INSTANCE, VAR_X,
				new Constant(StringType.INSTANCE, "hello", SyntheticLocation.INSTANCE),
				ComparisonEq.INSTANCE, SyntheticLocation.INSTANCE);
		ValueEnvironment<StringConstant> result = domain.assumeBinaryExpression(env, expr, PP, PP,
				new TestParameterProvider.FakeOracle());
		assertEquals(new StringConstant("hello"), result.getState(VAR_X));
	}

	@Test
	public void assumeEqNotSatisfiedReturnsBottom() throws SemanticException {
		StringConstantPropagation domain = new StringConstantPropagation();
		// x is "bye", assume x == "hello" → NOT_SATISFIED → bottom
		ValueEnvironment<StringConstant> env = domain.makeLattice().putState(VAR_X, new StringConstant("bye"));
		BinaryExpression expr = new BinaryExpression(
				StringType.INSTANCE, VAR_X,
				new Constant(StringType.INSTANCE, "hello", SyntheticLocation.INSTANCE),
				ComparisonEq.INSTANCE, SyntheticLocation.INSTANCE);
		ValueEnvironment<StringConstant> result = domain.assumeBinaryExpression(env, expr, PP, PP,
				new TestParameterProvider.FakeOracle());
		assertTrue(result.isBottom());
	}

	// ---- constraints ----

	@Test
	public void constraintsOnBottomStateReturnsNull() throws SemanticException {
		StringConstantPropagation domain = new StringConstantPropagation();
		assertNull(domain.constraints(null, domain.makeLattice().bottom(), VAR_X, PP,
				new TestParameterProvider.FakeOracle()));
	}

	@Test
	public void constraintsOnTopStateReturnsEmpty() throws SemanticException {
		StringConstantPropagation domain = new StringConstantPropagation();
		assertTrue(domain.constraints(null, domain.makeLattice(), VAR_X, PP,
				new TestParameterProvider.FakeOracle()).isEmpty());
	}

	@Test
	public void constraintsOnConcreteStringReturnsEqConstraint() throws SemanticException {
		StringConstantPropagation domain = new StringConstantPropagation();
		ValueEnvironment<StringConstant> env = domain.makeLattice().putState(VAR_X, new StringConstant("hello"));
		Set<BinaryExpression> cs = domain.constraints(null, env, VAR_X, PP, new TestParameterProvider.FakeOracle());
		assertNotNull(cs);
		assertEquals(1, cs.size());
		BinaryExpression c = cs.iterator().next();
		assertEquals(ComparisonEq.INSTANCE, c.getOperator());
		assertEquals("hello", ((Constant) c.getLeft()).getValue());
	}

	@Test
	public void constraintsOnStringLengthOp() throws SemanticException {
		StringConstantPropagation domain = new StringConstantPropagation();
		ValueEnvironment<StringConstant> env = domain.makeLattice().putState(VAR_X, new StringConstant("hello"));
		UnaryExpression lengthExpr = new UnaryExpression(Int32Type.INSTANCE, VAR_X,
				StringLength.INSTANCE, SyntheticLocation.INSTANCE);
		Set<BinaryExpression> cs = domain.constraints(null, env, lengthExpr, PP,
				new TestParameterProvider.FakeOracle());
		assertNotNull(cs);
		// length of "hello" = 5 → should produce {5 == length(x)}
		assertEquals(1, cs.size());
		BinaryExpression c = cs.iterator().next();
		assertEquals(ComparisonEq.INSTANCE, c.getOperator());
		assertEquals(5, ((Constant) c.getLeft()).getValue());
	}

}
