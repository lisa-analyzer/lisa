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
import it.unive.lisa.symbolic.value.operator.binary.ComparisonNe;
import it.unive.lisa.symbolic.value.operator.binary.StringCharAt;
import it.unive.lisa.symbolic.value.operator.binary.StringConcat;
import it.unive.lisa.symbolic.value.operator.binary.StringContains;
import it.unive.lisa.symbolic.value.operator.binary.StringEndsWith;
import it.unive.lisa.symbolic.value.operator.binary.StringEquals;
import it.unive.lisa.symbolic.value.operator.binary.StringStartsWith;
import it.unive.lisa.symbolic.value.operator.binary.StringSubstringToEnd;
import it.unive.lisa.symbolic.value.operator.ternary.StringReplaceAll;
import it.unive.lisa.symbolic.value.operator.ternary.StringSubstring;
import it.unive.lisa.symbolic.value.operator.unary.NumericToString;
import it.unive.lisa.symbolic.value.operator.unary.StringLength;
import it.unive.lisa.symbolic.value.operator.unary.StringReverse;
import it.unive.lisa.symbolic.value.operator.unary.StringToLowerCase;
import it.unive.lisa.symbolic.value.operator.unary.StringToUpperCase;
import it.unive.lisa.symbolic.value.operator.unary.StringTrim;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class BoundedStringSetTest {

	private static final ProgramPoint PP = TestParameterProvider.provideParam(null, ProgramPoint.class);
	private static final SemanticOracle ORACLE = TestParameterProvider.provideParam(null, SemanticOracle.class);

	private static final Variable VAR_X = new Variable(StringType.INSTANCE, "x", SyntheticLocation.INSTANCE);
	private static final Variable VAR_Y = new Variable(StringType.INSTANCE, "y", SyntheticLocation.INSTANCE);
	private static final Variable VAR_Z = new Variable(StringType.INSTANCE, "z", SyntheticLocation.INSTANCE);

	private final BoundedStringSet domain = new BoundedStringSet();

	// Helper: create a concrete BSS with the given string elements via BSS.mk()
	private BoundedStringSet.BSS bss(
			String... elems) {
		return domain.top().mk(new HashSet<>(Arrays.asList(elems)));
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

	// Oracle that returns different constraint sets depending on which
	// expression is queried
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
		Constant c = new Constant(StringType.INSTANCE, "hello", SyntheticLocation.INSTANCE);
		assertEquals(bss("hello"), domain.evalConstant(c, PP, ORACLE));
	}

	@Test
	void evalConstantEmptyStringReturnsSingletonBSS() throws SemanticException {
		// BSS accepts empty strings unlike Prefix/Suffix
		Constant c = new Constant(StringType.INSTANCE, "", SyntheticLocation.INSTANCE);
		assertEquals(bss(""), domain.evalConstant(c, PP, ORACLE));
	}

	@Test
	void evalConstantNonStringReturnsTop() throws SemanticException {
		Constant c = new Constant(Int32Type.INSTANCE, 42, SyntheticLocation.INSTANCE);
		assertTrue(domain.evalConstant(c, PP, ORACLE).isTop());
	}

	// ---- evalUnaryExpression ----

	@Test
	void evalUnaryReverse() throws SemanticException {
		assertEquals(bss("olleh"),
				domain.evalUnaryExpression(mkUnary(StringReverse.INSTANCE), bss("hello"), PP, ORACLE));
	}

	@Test
	void evalUnaryToLowerCase() throws SemanticException {
		assertEquals(bss("hello"),
				domain.evalUnaryExpression(mkUnary(StringToLowerCase.INSTANCE), bss("HELLO"), PP, ORACLE));
	}

	@Test
	void evalUnaryToUpperCase() throws SemanticException {
		assertEquals(bss("HELLO"),
				domain.evalUnaryExpression(mkUnary(StringToUpperCase.INSTANCE), bss("hello"), PP, ORACLE));
	}

	@Test
	void evalUnaryTrim() throws SemanticException {
		assertEquals(bss("hello"),
				domain.evalUnaryExpression(mkUnary(StringTrim.INSTANCE), bss("  hello  "), PP, ORACLE));
	}

	@Test
	void evalUnaryTopArgReturnsTop() throws SemanticException {
		assertTrue(domain.evalUnaryExpression(mkUnary(StringToUpperCase.INSTANCE),
				domain.top(), PP, ORACLE).isTop());
	}

	@Test
	void evalUnaryUnknownOperatorReturnsTop() throws SemanticException {
		// StringLength is not handled by evalUnaryExpression → returns top via
		// forEach's onNull
		assertTrue(domain.evalUnaryExpression(mkUnary(StringLength.INSTANCE),
				bss("hello"), PP, ORACLE).isTop());
	}

	@Test
	void evalUnaryNumericToStringWVA() throws SemanticException {
		assertEquals(bss("42"),
				domain.evalUnaryExpression(mkUnary(NumericToString.INSTANCE),
						domain.top(), PP, new WVAOracle(strConstraint("42"))));
	}

	@Test
	void evalUnaryNumericToStringWVAEmptyConstraintsReturnsTop() throws SemanticException {
		assertTrue(domain.evalUnaryExpression(mkUnary(NumericToString.INSTANCE),
				domain.top(), PP, new WVAOracle(new HashSet<>())).isTop());
	}

	@Test
	void evalUnaryReverseOnMultipleElements() throws SemanticException {
		// Each element is reversed independently
		assertEquals(bss("olleh", "dlrow"),
				domain.evalUnaryExpression(mkUnary(StringReverse.INSTANCE), bss("hello", "world"), PP, ORACLE));
	}

	// ---- evalBinaryExpression ----

	@Test
	void evalBinaryConcatSingletons() throws SemanticException {
		assertEquals(bss("helloworld"),
				domain.evalBinaryExpression(mkBin(StringConcat.INSTANCE),
						bss("hello"), bss("world"), PP, ORACLE));
	}

	@Test
	void evalBinaryConcatCrossProduct() throws SemanticException {
		// {"a","b"} concat {"x","y"} → {"ax","ay","bx","by"}
		BoundedStringSet.BSS result = domain.evalBinaryExpression(mkBin(StringConcat.INSTANCE),
				bss("a", "b"), bss("x", "y"), PP, ORACLE);
		assertEquals(bss("ax", "ay", "bx", "by"), result);
	}

	@Test
	void evalBinaryTopLeftReturnsTop() throws SemanticException {
		assertTrue(domain.evalBinaryExpression(mkBin(StringConcat.INSTANCE),
				domain.top(), bss("world"), PP, ORACLE).isTop());
	}

	@Test
	void evalBinaryTopRightReturnsTop() throws SemanticException {
		assertTrue(domain.evalBinaryExpression(mkBin(StringConcat.INSTANCE),
				bss("hello"), domain.top(), PP, ORACLE).isTop());
	}

	@Test
	void evalBinaryCharAtWVA() throws SemanticException {
		// "hello".charAt(0) = "h"
		BinaryExpression expr = new BinaryExpression(StringType.INSTANCE, VAR_X, VAR_Y,
				StringCharAt.INSTANCE, SyntheticLocation.INSTANCE);
		BoundedStringSet.BSS result = domain.evalBinaryExpression(expr,
				bss("hello"), domain.top(), PP, new WVAOracle(intConstraint(0)));
		assertEquals(bss("h"), result);
	}

	@Test
	void evalBinarySubstringToEndWVA() throws SemanticException {
		// "hello".substringToEnd(2) = "llo"
		BinaryExpression expr = new BinaryExpression(StringType.INSTANCE, VAR_X, VAR_Y,
				StringSubstringToEnd.INSTANCE, SyntheticLocation.INSTANCE);
		BoundedStringSet.BSS result = domain.evalBinaryExpression(expr,
				bss("hello"), domain.top(), PP, new WVAOracle(intConstraint(2)));
		assertEquals(bss("llo"), result);
	}

	@Test
	void evalBinaryCharAtMultipleStringsWVA() throws SemanticException {
		// {"hello","world"}.charAt(0) = {"h","w"}
		BinaryExpression expr = new BinaryExpression(StringType.INSTANCE, VAR_X, VAR_Y,
				StringCharAt.INSTANCE, SyntheticLocation.INSTANCE);
		BoundedStringSet.BSS result = domain.evalBinaryExpression(expr,
				bss("hello", "world"), domain.top(), PP, new WVAOracle(intConstraint(0)));
		assertEquals(bss("h", "w"), result);
	}

	// ---- evalTernaryExpression ----

	@Test
	void evalTernaryReplaceAll() throws SemanticException {
		// "hello".replaceAll("l", "r") = "herro"
		BoundedStringSet.BSS result = domain.evalTernaryExpression(mkTernary(StringReplaceAll.INSTANCE),
				bss("hello"), bss("l"), bss("r"), PP, ORACLE);
		assertEquals(bss("herro"), result);
	}

	@Test
	void evalTernaryReplaceAllMultipleStrings() throws SemanticException {
		// {"hello","world"}.replaceAll("o", "0") = {"hell0","w0rld"}
		BoundedStringSet.BSS result = domain.evalTernaryExpression(mkTernary(StringReplaceAll.INSTANCE),
				bss("hello", "world"), bss("o"), bss("0"), PP, ORACLE);
		assertEquals(bss("hell0", "w0rld"), result);
	}

	@Test
	void evalTernarySubstringWVA() throws SemanticException {
		// "hello".substring(1, 4) = "ell"
		// The oracle is called with expression.getMiddle() = VAR_Y and
		// expression.getRight() = VAR_Z
		Set<BinaryExpression> midCs = intConstraint(1);
		Set<BinaryExpression> rigCs = new HashSet<>();
		rigCs.add(new BinaryExpression(Int32Type.INSTANCE,
				new Constant(Int32Type.INSTANCE, 4, SyntheticLocation.INSTANCE),
				VAR_Z, ComparisonEq.INSTANCE, SyntheticLocation.INSTANCE));
		TwoArgWVAOracle oracle = new TwoArgWVAOracle(VAR_Y, midCs, VAR_Z, rigCs);
		TernaryExpression expr = mkTernary(StringSubstring.INSTANCE);
		BoundedStringSet.BSS result = domain.evalTernaryExpression(expr,
				bss("hello"), domain.top(), domain.top(), PP, oracle);
		assertEquals(bss("ell"), result);
	}

	@Test
	void evalTernaryTopLeftReturnsTop() throws SemanticException {
		assertTrue(domain.evalTernaryExpression(mkTernary(StringReplaceAll.INSTANCE),
				domain.top(), bss("l"), bss("r"), PP, ORACLE).isTop());
	}

	@Test
	void evalTernaryTopMiddleReturnsTop() throws SemanticException {
		assertTrue(domain.evalTernaryExpression(mkTernary(StringReplaceAll.INSTANCE),
				bss("hello"), domain.top(), bss("r"), PP, ORACLE).isTop());
	}

	// ---- satisfiesBinaryExpression ----

	@Test
	void satisfiesBinaryEqSameElementSatisfied() throws SemanticException {
		assertEquals(Satisfiability.SATISFIED,
				domain.satisfiesBinaryExpression(mkBin(ComparisonEq.INSTANCE),
						bss("hello"), bss("hello"), PP, ORACLE));
	}

	@Test
	void satisfiesBinaryEqDifferentElementsNotSatisfied() throws SemanticException {
		assertEquals(Satisfiability.NOT_SATISFIED,
				domain.satisfiesBinaryExpression(mkBin(ComparisonEq.INSTANCE),
						bss("hello"), bss("world"), PP, ORACLE));
	}

	@Test
	void satisfiesBinaryEqMultipleElementsMixedReturnsUnknown() throws SemanticException {
		// {"hello","world"} eq {"hello"}: one pair equal, one not → UNKNOWN
		assertEquals(Satisfiability.UNKNOWN,
				domain.satisfiesBinaryExpression(mkBin(ComparisonEq.INSTANCE),
						bss("hello", "world"), bss("hello"), PP, ORACLE));
	}

	@Test
	void satisfiesBinaryNeSameElementNotSatisfied() throws SemanticException {
		assertEquals(Satisfiability.NOT_SATISFIED,
				domain.satisfiesBinaryExpression(mkBin(ComparisonNe.INSTANCE),
						bss("hello"), bss("hello"), PP, ORACLE));
	}

	@Test
	void satisfiesBinaryNeDifferentElementsSatisfied() throws SemanticException {
		assertEquals(Satisfiability.SATISFIED,
				domain.satisfiesBinaryExpression(mkBin(ComparisonNe.INSTANCE),
						bss("hello"), bss("world"), PP, ORACLE));
	}

	@Test
	void satisfiesBinaryContainsFound() throws SemanticException {
		assertEquals(Satisfiability.SATISFIED,
				domain.satisfiesBinaryExpression(mkBin(StringContains.INSTANCE),
						bss("hello"), bss("ell"), PP, ORACLE));
	}

	@Test
	void satisfiesBinaryContainsNotFound() throws SemanticException {
		assertEquals(Satisfiability.NOT_SATISFIED,
				domain.satisfiesBinaryExpression(mkBin(StringContains.INSTANCE),
						bss("hello"), bss("xyz"), PP, ORACLE));
	}

	@Test
	void satisfiesBinaryStartsWithTrue() throws SemanticException {
		assertEquals(Satisfiability.SATISFIED,
				domain.satisfiesBinaryExpression(mkBin(StringStartsWith.INSTANCE),
						bss("hello"), bss("hel"), PP, ORACLE));
	}

	@Test
	void satisfiesBinaryStartsWithFalse() throws SemanticException {
		assertEquals(Satisfiability.NOT_SATISFIED,
				domain.satisfiesBinaryExpression(mkBin(StringStartsWith.INSTANCE),
						bss("hello"), bss("xyz"), PP, ORACLE));
	}

	@Test
	void satisfiesBinaryEndsWithTrue() throws SemanticException {
		assertEquals(Satisfiability.SATISFIED,
				domain.satisfiesBinaryExpression(mkBin(StringEndsWith.INSTANCE),
						bss("hello"), bss("llo"), PP, ORACLE));
	}

	@Test
	void satisfiesBinaryEndsWithFalse() throws SemanticException {
		assertEquals(Satisfiability.NOT_SATISFIED,
				domain.satisfiesBinaryExpression(mkBin(StringEndsWith.INSTANCE),
						bss("hello"), bss("xyz"), PP, ORACLE));
	}

	@Test
	void satisfiesBinaryTopLeftReturnsUnknown() throws SemanticException {
		assertEquals(Satisfiability.UNKNOWN,
				domain.satisfiesBinaryExpression(mkBin(StringEquals.INSTANCE),
						domain.top(), bss("hello"), PP, ORACLE));
	}

	@Test
	void satisfiesBinaryTopRightReturnsUnknown() throws SemanticException {
		assertEquals(Satisfiability.UNKNOWN,
				domain.satisfiesBinaryExpression(mkBin(StringEquals.INSTANCE),
						bss("hello"), domain.top(), PP, ORACLE));
	}

	// ---- assumeBinaryExpression ----

	@Test
	void assumeEqRefinesIdentifierToIntersection() throws SemanticException {
		// x={"hello","world"}, y={"hello"}, assume x==y → x={"hello"}
		ValueEnvironment<BoundedStringSet.BSS> env = domain.makeLattice()
				.putState(VAR_X, bss("hello", "world"))
				.putState(VAR_Y, bss("hello"));
		ValueEnvironment<BoundedStringSet.BSS> result = domain.assumeBinaryExpression(env, mkBin(ComparisonEq.INSTANCE),
				PP, PP, ORACLE);
		assertEquals(bss("hello"), result.getState(VAR_X));
	}

	@Test
	void assumeEqIncompatibleSetsReturnsBottom() throws SemanticException {
		// x={"hello"}, y={"world"}, assume x==y → glb is empty → bottom
		ValueEnvironment<BoundedStringSet.BSS> env = domain.makeLattice()
				.putState(VAR_X, bss("hello"))
				.putState(VAR_Y, bss("world"));
		ValueEnvironment<BoundedStringSet.BSS> result = domain.assumeBinaryExpression(env, mkBin(ComparisonEq.INSTANCE),
				PP, PP, ORACLE);
		assertTrue(result.isBottom());
	}

	@Test
	void assumeNeRemovesElement() throws SemanticException {
		// x={"hello","world"}, y={"world"}, assume x!=y → x={"hello"}
		ValueEnvironment<BoundedStringSet.BSS> env = domain.makeLattice()
				.putState(VAR_X, bss("hello", "world"))
				.putState(VAR_Y, bss("world"));
		ValueEnvironment<BoundedStringSet.BSS> result = domain.assumeBinaryExpression(env, mkBin(ComparisonNe.INSTANCE),
				PP, PP, ORACLE);
		assertEquals(bss("hello"), result.getState(VAR_X));
	}

	@Test
	void assumeNeRemovesOnlyElementGoesToBottom() throws SemanticException {
		// x={"world"}, y={"world"}, assume x!=y → diff is empty → bottom
		ValueEnvironment<BoundedStringSet.BSS> env = domain.makeLattice()
				.putState(VAR_X, bss("world"))
				.putState(VAR_Y, bss("world"));
		ValueEnvironment<BoundedStringSet.BSS> result = domain.assumeBinaryExpression(env, mkBin(ComparisonNe.INSTANCE),
				PP, PP, ORACLE);
		assertTrue(result.isBottom());
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
	void constraintsOnSingletonBSSReturnsEqConstraint() throws SemanticException {
		ValueEnvironment<BoundedStringSet.BSS> env = domain.makeLattice().putState(VAR_X, bss("hello"));
		Set<BinaryExpression> cs = domain.constraints(null, env, VAR_X, PP, ORACLE);
		assertNotNull(cs);
		assertEquals(1, cs.size());
		BinaryExpression c = cs.iterator().next();
		assertEquals(ComparisonEq.INSTANCE, c.getOperator());
		assertEquals("hello", ((Constant) c.getLeft()).getValue());
	}

	@Test
	void constraintsOnMultipleElementsReturnsEmpty() throws SemanticException {
		// Multiple values → no precise constraint
		ValueEnvironment<BoundedStringSet.BSS> env = domain.makeLattice().putState(VAR_X, bss("hello", "world"));
		Set<BinaryExpression> cs = domain.constraints(null, env, VAR_X, PP, ORACLE);
		assertNotNull(cs);
		assertTrue(cs.isEmpty());
	}

	@Test
	void constraintsOnStringLengthGivesExactRange() throws SemanticException {
		// x in {"hi","hello"} → lengths in [2,5]
		ValueEnvironment<BoundedStringSet.BSS> env = domain.makeLattice().putState(VAR_X, bss("hi", "hello"));
		UnaryExpression lengthExpr = new UnaryExpression(Int32Type.INSTANCE, VAR_X,
				StringLength.INSTANCE, SyntheticLocation.INSTANCE);
		Set<BinaryExpression> cs = domain.constraints(null, env, lengthExpr, PP, ORACLE);
		assertNotNull(cs);
		assertTrue(cs.size() >= 1);
	}

	@Test
	void constraintsOnStringLengthSingletonGivesExactValue() throws SemanticException {
		// x = "hello" (len 5) → length constraint should give [5,5]
		ValueEnvironment<BoundedStringSet.BSS> env = domain.makeLattice().putState(VAR_X, bss("hello"));
		UnaryExpression lengthExpr = new UnaryExpression(Int32Type.INSTANCE, VAR_X,
				StringLength.INSTANCE, SyntheticLocation.INSTANCE);
		Set<BinaryExpression> cs = domain.constraints(null, env, lengthExpr, PP, ORACLE);
		assertNotNull(cs);
		assertTrue(cs.size() >= 1);
	}
}
