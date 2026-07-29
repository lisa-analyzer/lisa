package it.unive.lisa.analysis.string;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.TestParameterProvider;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.lattices.Satisfiability;
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
import it.unive.lisa.symbolic.value.operator.binary.StringCharAt;
import it.unive.lisa.symbolic.value.operator.binary.StringConcat;
import it.unive.lisa.symbolic.value.operator.binary.StringContains;
import it.unive.lisa.symbolic.value.operator.binary.StringEquals;
import it.unive.lisa.symbolic.value.operator.binary.StringSubstringToEnd;
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

public class BricksTest {

	private static final ProgramPoint PP = TestParameterProvider.provideParam(null, ProgramPoint.class);
	private static final SemanticOracle ORACLE = TestParameterProvider.provideParam(null, SemanticOracle.class);

	private static final Variable VAR_X = new Variable(StringType.INSTANCE, "x", SyntheticLocation.INSTANCE);
	private static final Variable VAR_Y = new Variable(StringType.INSTANCE, "y", SyntheticLocation.INSTANCE);
	private static final Variable VAR_Z = new Variable(StringType.INSTANCE, "z", SyntheticLocation.INSTANCE);

	private final Bricks domain = new Bricks();

	// Helper: get a concrete BrickList for a single string
	private Bricks.BrickList bl(
			String str)
			throws SemanticException {
		return domain.evalConstant(new Constant(StringType.INSTANCE, str, SyntheticLocation.INSTANCE), PP, ORACLE);
	}

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

	private static class TwoArgWVAOracle
			extends
			TestParameterProvider.FakeOracle {
		private final ValueExpression midExpr;
		private final Set<BinaryExpression> midCs;
		private final ValueExpression rigExpr;
		private final Set<BinaryExpression> rigCs;

		TwoArgWVAOracle(
				ValueExpression midExpr,
				Set<BinaryExpression> midCs,
				ValueExpression rigExpr,
				Set<BinaryExpression> rigCs) {
			this.midExpr = midExpr;
			this.midCs = midCs;
			this.rigExpr = rigExpr;
			this.rigCs = rigCs;
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
			if (e == midExpr)
				return midCs;
			if (e == rigExpr)
				return rigCs;
			return new HashSet<>();
		}
	}

	private BinaryExpression mkBin(
			it.unive.lisa.symbolic.value.operator.binary.BinaryOperator op) {
		return new BinaryExpression(StringType.INSTANCE, VAR_X, VAR_Y, op, SyntheticLocation.INSTANCE);
	}

	private UnaryExpression mkUnary(
			it.unive.lisa.symbolic.value.operator.unary.UnaryOperator op) {
		return new UnaryExpression(StringType.INSTANCE, VAR_X, op, SyntheticLocation.INSTANCE);
	}

	private TernaryExpression mkTernary(
			it.unive.lisa.symbolic.value.operator.ternary.TernaryOperator op) {
		return new TernaryExpression(StringType.INSTANCE, VAR_X, VAR_Y, VAR_Z, op, SyntheticLocation.INSTANCE);
	}

	private Set<BinaryExpression> intConstraint(
			int val) {
		Set<BinaryExpression> cs = new HashSet<>();
		cs.add(new BinaryExpression(Int32Type.INSTANCE,
				new Constant(Int32Type.INSTANCE, val, SyntheticLocation.INSTANCE),
				VAR_X, ComparisonEq.INSTANCE, SyntheticLocation.INSTANCE));
		return cs;
	}

	private Set<BinaryExpression> strConstraint(
			String val) {
		Set<BinaryExpression> cs = new HashSet<>();
		cs.add(new BinaryExpression(StringType.INSTANCE,
				new Constant(StringType.INSTANCE, val, SyntheticLocation.INSTANCE),
				VAR_X, ComparisonEq.INSTANCE, SyntheticLocation.INSTANCE));
		return cs;
	}

	// ---- evalConstant ----

	@Test
	void evalConstantConcreteString() throws SemanticException {
		// "hello" → BrickList([Brick(1,1,{"hello"})])
		Bricks.BrickList result = bl("hello");
		assertTrue(!result.isTop() && !result.isBottom());
		// It should represent only "hello"
		assertTrue(result.isFinite());
		assertEquals(1, result.getReps().size());
		assertTrue(result.getReps().contains("hello"));
	}

	@Test
	void evalConstantEmptyString() throws SemanticException {
		Bricks.BrickList result = bl("");
		assertTrue(!result.isTop() && !result.isBottom());
		assertTrue(result.isFinite());
		assertEquals(1, result.getReps().size());
		assertTrue(result.getReps().contains(""));
	}

	@Test
	void evalConstantNonStringReturnsTop() throws SemanticException {
		Constant c = new Constant(Int32Type.INSTANCE, 42, SyntheticLocation.INSTANCE);
		assertTrue(domain.evalConstant(c, PP, ORACLE).isTop());
	}

	// ---- evalUnaryExpression ----

	@Test
	void evalUnaryReverseReturnsTop() throws SemanticException {
		// reverse is not handled → TOP
		assertTrue(domain.evalUnaryExpression(mkUnary(StringReverse.INSTANCE), bl("hello"), PP, ORACLE).isTop());
	}

	@Test
	void evalUnaryTrimReturnsTop() throws SemanticException {
		// trim is not handled → TOP
		assertTrue(domain.evalUnaryExpression(mkUnary(StringTrim.INSTANCE), bl("hello"), PP, ORACLE).isTop());
	}

	@Test
	void evalUnaryToLowerCase() throws SemanticException {
		Bricks.BrickList result = domain.evalUnaryExpression(mkUnary(StringToLowerCase.INSTANCE), bl("HELLO"), PP,
				ORACLE);
		assertTrue(result.isFinite());
		assertTrue(result.getReps().contains("hello"));
	}

	@Test
	void evalUnaryToUpperCase() throws SemanticException {
		Bricks.BrickList result = domain.evalUnaryExpression(mkUnary(StringToUpperCase.INSTANCE), bl("hello"), PP,
				ORACLE);
		assertTrue(result.isFinite());
		assertTrue(result.getReps().contains("HELLO"));
	}

	@Test
	void evalUnaryTopArgReturnsTop() throws SemanticException {
		assertTrue(domain.evalUnaryExpression(mkUnary(StringToUpperCase.INSTANCE),
				domain.top(), PP, ORACLE).isTop());
	}

	@Test
	void evalUnaryUnknownOperatorReturnsTop() throws SemanticException {
		assertTrue(domain.evalUnaryExpression(mkUnary(StringLength.INSTANCE), bl("hello"), PP, ORACLE).isTop());
	}

	@Test
	void evalUnaryNumericToStringWVA() throws SemanticException {
		Bricks.BrickList result = domain.evalUnaryExpression(mkUnary(NumericToString.INSTANCE),
				domain.top(), PP, new WVAOracle(strConstraint("42")));
		assertTrue(result.isFinite());
		assertTrue(result.getReps().contains("42"));
	}

	// ---- evalBinaryExpression ----

	@Test
	void evalBinaryConcatAppendsBrickLists() throws SemanticException {
		// "hello" concat "world" → BrickList([Brick("hello"), Brick("world")])
		// After normalization: rule2 merges two consecutive Brick(1,1,...) →
		// Brick(1,1,{"helloworld"})
		Bricks.BrickList result = domain.evalBinaryExpression(mkBin(StringConcat.INSTANCE),
				bl("hello"), bl("world"), PP, ORACLE);
		// The normalization should give us the concatenated string
		assertTrue(result.isFinite());
		assertTrue(result.getReps().contains("helloworld"));
	}

	@Test
	void evalBinaryTopLeftReturnsTop() throws SemanticException {
		assertTrue(domain.evalBinaryExpression(mkBin(StringConcat.INSTANCE),
				domain.top(), bl("world"), PP, ORACLE).isTop());
	}

	@Test
	void evalBinaryTopRightReturnsTop() throws SemanticException {
		assertTrue(domain.evalBinaryExpression(mkBin(StringConcat.INSTANCE),
				bl("hello"), domain.top(), PP, ORACLE).isTop());
	}

	@Test
	void evalBinaryCharAtWVA() throws SemanticException {
		// "hello".charAt(0) = "h"
		BinaryExpression expr = new BinaryExpression(StringType.INSTANCE, VAR_X, VAR_Y,
				StringCharAt.INSTANCE, SyntheticLocation.INSTANCE);
		Bricks.BrickList result = domain.evalBinaryExpression(expr, bl("hello"), domain.top(), PP,
				new WVAOracle(intConstraint(0)));
		assertTrue(result.isFinite());
		assertTrue(result.getReps().contains("h"));
	}

	@Test
	void evalBinarySubstringToEndWVA() throws SemanticException {
		// "hello".substringToEnd(2) = "llo"
		BinaryExpression expr = new BinaryExpression(StringType.INSTANCE, VAR_X, VAR_Y,
				StringSubstringToEnd.INSTANCE, SyntheticLocation.INSTANCE);
		Bricks.BrickList result = domain.evalBinaryExpression(expr, bl("hello"), domain.top(), PP,
				new WVAOracle(intConstraint(2)));
		assertTrue(result.isFinite());
		assertTrue(result.getReps().contains("llo"));
	}

	@Test
	void evalBinarySubstringToEndAtLengthWVA() throws SemanticException {
		// "hello".substringToEnd(5) = "" (valid: index == length)
		BinaryExpression expr = new BinaryExpression(StringType.INSTANCE, VAR_X, VAR_Y,
				StringSubstringToEnd.INSTANCE, SyntheticLocation.INSTANCE);
		Bricks.BrickList result = domain.evalBinaryExpression(expr, bl("hello"), domain.top(), PP,
				new WVAOracle(intConstraint(5)));
		assertTrue(result.isFinite());
		assertTrue(result.getReps().contains(""));
	}

	// ---- evalTernaryExpression ----

	@Test
	void evalTernarySubstringWVA() throws SemanticException {
		// "hello".substring(1, 4) = "ell"
		Set<BinaryExpression> midCs = intConstraint(1);
		Set<BinaryExpression> rigCs = new HashSet<>();
		rigCs.add(new BinaryExpression(Int32Type.INSTANCE,
				new Constant(Int32Type.INSTANCE, 4, SyntheticLocation.INSTANCE),
				VAR_Z, ComparisonEq.INSTANCE, SyntheticLocation.INSTANCE));
		TwoArgWVAOracle oracle = new TwoArgWVAOracle(VAR_Y, midCs, VAR_Z, rigCs);
		Bricks.BrickList result = domain.evalTernaryExpression(mkTernary(StringSubstring.INSTANCE),
				bl("hello"), domain.top(), domain.top(), PP, oracle);
		assertTrue(result.isFinite());
		assertTrue(result.getReps().contains("ell"));
	}

	@Test
	void evalTernarySubstringWVAAtFullLength() throws SemanticException {
		// "hello".substring(0, 5) = "hello" (end == length, previously buggy)
		Set<BinaryExpression> midCs = intConstraint(0);
		Set<BinaryExpression> rigCs = new HashSet<>();
		rigCs.add(new BinaryExpression(Int32Type.INSTANCE,
				new Constant(Int32Type.INSTANCE, 5, SyntheticLocation.INSTANCE),
				VAR_Z, ComparisonEq.INSTANCE, SyntheticLocation.INSTANCE));
		TwoArgWVAOracle oracle = new TwoArgWVAOracle(VAR_Y, midCs, VAR_Z, rigCs);
		Bricks.BrickList result = domain.evalTernaryExpression(mkTernary(StringSubstring.INSTANCE),
				bl("hello"), domain.top(), domain.top(), PP, oracle);
		assertTrue(result.isFinite());
		assertTrue(result.getReps().contains("hello"));
	}

	@Test
	void evalTernaryTopLeftReturnsTop() throws SemanticException {
		assertTrue(domain.evalTernaryExpression(mkTernary(StringSubstring.INSTANCE),
				domain.top(), domain.top(), domain.top(), PP, ORACLE).isTop());
	}

	// ---- satisfiesBinaryExpression ----

	@Test
	void satisfiesBinaryEqSameCharSatisfied() throws SemanticException {
		// BrickList("h") eq BrickList("h"): both single char → SATISFIED
		assertEquals(Satisfiability.SATISFIED,
				domain.satisfiesBinaryExpression(mkBin(ComparisonEq.INSTANCE),
						bl("h"), bl("h"), PP, ORACLE));
	}

	@Test
	void satisfiesBinaryEqDifferentCharsNotSatisfied() throws SemanticException {
		// BrickList("h") eq BrickList("x"): "h" and "x" are both single chars,
		// left.contains("x") = NOT_SATISFIED (h doesn't contain x)
		// left.contains(right).and(right.contains(left))
		// left.contains("x"): c="x", "h".contains("x")=false → NOT_SATISFIED
		// NOT_SATISFIED.and(...) = NOT_SATISFIED
		assertEquals(Satisfiability.NOT_SATISFIED,
				domain.satisfiesBinaryExpression(mkBin(ComparisonEq.INSTANCE),
						bl("h"), bl("x"), PP, ORACLE));
	}

	@Test
	void satisfiesBinaryContainsSingleCharFound() throws SemanticException {
		// "hello".contains("h"): the single-char check
		assertEquals(Satisfiability.SATISFIED,
				domain.satisfiesBinaryExpression(mkBin(StringContains.INSTANCE),
						bl("hello"), bl("h"), PP, ORACLE));
	}

	@Test
	void satisfiesBinaryContainsSingleCharNotFound() throws SemanticException {
		// "abc".contains("x"): x not in abc
		assertEquals(Satisfiability.NOT_SATISFIED,
				domain.satisfiesBinaryExpression(mkBin(StringContains.INSTANCE),
						bl("abc"), bl("x"), PP, ORACLE));
	}

	@Test
	void satisfiesBinaryContainsMultiCharReturnsUnknown() throws SemanticException {
		// contains only handles single-char right operands
		assertEquals(Satisfiability.UNKNOWN,
				domain.satisfiesBinaryExpression(mkBin(StringContains.INSTANCE),
						bl("hello"), bl("ell"), PP, ORACLE));
	}

	@Test
	void satisfiesBinaryTopReturnsUnknown() throws SemanticException {
		assertEquals(Satisfiability.UNKNOWN,
				domain.satisfiesBinaryExpression(mkBin(StringEquals.INSTANCE),
						domain.top(), bl("hello"), PP, ORACLE));
	}

	// ---- assumeBinaryExpression ----

	@Test
	void assumeEqRefinesIdentifierToRhs() throws SemanticException {
		// x is top, y = BrickList("hello"). Assume x == y → x refined to
		// BrickList("hello")
		Bricks.BrickList helloList = bl("hello");
		ValueEnvironment<Bricks.BrickList> env = domain.makeLattice()
				.putState(VAR_X, domain.top())
				.putState(VAR_Y, helloList);
		ValueEnvironment<Bricks.BrickList> result = domain.assumeBinaryExpression(env, mkBin(ComparisonEq.INSTANCE), PP,
				PP, ORACLE);
		assertEquals(helloList, result.getState(VAR_X));
	}

	@Test
	void assumeNonEqOperatorReturnsUnchanged() throws SemanticException {
		ValueEnvironment<Bricks.BrickList> env = domain.makeLattice()
				.putState(VAR_X, domain.top())
				.putState(VAR_Y, bl("hello"));
		ValueEnvironment<Bricks.BrickList> result = domain.assumeBinaryExpression(env, mkBin(StringContains.INSTANCE),
				PP, PP, ORACLE);
		assertEquals(env, result);
	}

	// ---- constraints ----

	@Test
	void constraintsOnBottomStateReturnsNull() throws SemanticException {
		assertNull(domain.constraints(null, domain.makeLattice().bottom(), VAR_X, PP, ORACLE));
	}

	@Test
	void constraintsOnTopStateReturnsEmpty() throws SemanticException {
		assertTrue(domain.constraints(null, domain.makeLattice(), VAR_X, PP, ORACLE).isEmpty());
	}

	@Test
	void constraintsOnSingleStringReturnsEqConstraint() throws SemanticException {
		// x = BrickList("hello") → equality constraint "hello"
		ValueEnvironment<Bricks.BrickList> env = domain.makeLattice().putState(VAR_X, bl("hello"));
		Set<BinaryExpression> cs = domain.constraints(null, env, VAR_X, PP, ORACLE);
		assertNotNull(cs);
		assertEquals(1, cs.size());
		BinaryExpression c = cs.iterator().next();
		assertEquals(ComparisonEq.INSTANCE, c.getOperator());
		assertEquals("hello", ((Constant) c.getLeft()).getValue());
	}

	@Test
	void constraintsOnTopVarReturnsEmpty() throws SemanticException {
		// x is TOP (unknown string) → no constraint
		ValueEnvironment<Bricks.BrickList> env = domain.makeLattice().putState(VAR_X, domain.top());
		Set<BinaryExpression> cs = domain.constraints(null, env, VAR_X, PP, ORACLE);
		assertNotNull(cs);
		assertTrue(cs.isEmpty());
	}

	@Test
	void constraintsOnStringLengthReturnsExactBound() throws SemanticException {
		// x = "hello" → length = 5 (exact)
		ValueEnvironment<Bricks.BrickList> env = domain.makeLattice().putState(VAR_X, bl("hello"));
		UnaryExpression lengthExpr = new UnaryExpression(Int32Type.INSTANCE, VAR_X,
				StringLength.INSTANCE, SyntheticLocation.INSTANCE);
		Set<BinaryExpression> cs = domain.constraints(null, env, lengthExpr, PP, ORACLE);
		assertNotNull(cs);
		assertTrue(cs.size() >= 1);
	}
}
