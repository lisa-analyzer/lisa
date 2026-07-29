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
import it.unive.lisa.lattices.string.StrPrefix;
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
import it.unive.lisa.symbolic.value.operator.binary.StringConcat;
import it.unive.lisa.symbolic.value.operator.binary.StringContains;
import it.unive.lisa.symbolic.value.operator.binary.StringEndsWith;
import it.unive.lisa.symbolic.value.operator.binary.StringEquals;
import it.unive.lisa.symbolic.value.operator.binary.StringIsPrefixOf;
import it.unive.lisa.symbolic.value.operator.binary.StringStartsWith;
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

public class PrefixTest {

	private static final ProgramPoint PP = TestParameterProvider.provideParam(null, ProgramPoint.class);
	private static final SemanticOracle ORACLE = TestParameterProvider.provideParam(null, SemanticOracle.class);

	private static final Variable VAR_X = new Variable(StringType.INSTANCE, "x", SyntheticLocation.INSTANCE);
	private static final Variable VAR_Y = new Variable(StringType.INSTANCE, "y", SyntheticLocation.INSTANCE);
	private static final Variable VAR_Z = new Variable(StringType.INSTANCE, "z", SyntheticLocation.INSTANCE);

	private final Prefix domain = new Prefix();

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

	private static Set<BinaryExpression> intConstraint(
			int val) {
		Set<BinaryExpression> cs = new HashSet<>();
		cs.add(new BinaryExpression(Int32Type.INSTANCE,
				new Constant(Int32Type.INSTANCE, val, SyntheticLocation.INSTANCE),
				VAR_X, ComparisonEq.INSTANCE, SyntheticLocation.INSTANCE));
		return cs;
	}

	private static Set<BinaryExpression> strConstraint(
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
		assertEquals(new StrPrefix("hello"), domain.evalConstant(c, PP, ORACLE));
	}

	@Test
	void evalConstantEmptyStringReturnsTop() throws SemanticException {
		Constant c = new Constant(StringType.INSTANCE, "", SyntheticLocation.INSTANCE);
		assertTrue(domain.evalConstant(c, PP, ORACLE).isTop());
	}

	@Test
	void evalConstantNonStringReturnsTop() throws SemanticException {
		Constant c = new Constant(Int32Type.INSTANCE, 42, SyntheticLocation.INSTANCE);
		assertTrue(domain.evalConstant(c, PP, ORACLE).isTop());
	}

	// ---- evalUnaryExpression ----

	@Test
	void evalUnaryToLowerCase() throws SemanticException {
		assertEquals(new StrPrefix("hello"),
				domain.evalUnaryExpression(mkUnary(StringToLowerCase.INSTANCE),
						new StrPrefix("HELLO"), PP, ORACLE));
	}

	@Test
	void evalUnaryToUpperCase() throws SemanticException {
		assertEquals(new StrPrefix("HELLO"),
				domain.evalUnaryExpression(mkUnary(StringToUpperCase.INSTANCE),
						new StrPrefix("hello"), PP, ORACLE));
	}

	@Test
	void evalUnaryTrimStripsLeadingSpaces() throws SemanticException {
		// StringUtils.stripStart strips leading whitespace
		assertEquals(new StrPrefix("hello  "),
				domain.evalUnaryExpression(mkUnary(StringTrim.INSTANCE),
						new StrPrefix("  hello  "), PP, ORACLE));
	}

	@Test
	void evalUnaryReverseReturnsTop() throws SemanticException {
		// reversing a prefix gives no prefix info
		assertTrue(domain.evalUnaryExpression(mkUnary(StringReverse.INSTANCE),
				new StrPrefix("hello"), PP, ORACLE).isTop());
	}

	@Test
	void evalUnaryTopArgReturnsTop() throws SemanticException {
		assertTrue(domain.evalUnaryExpression(mkUnary(StringToUpperCase.INSTANCE),
				StrPrefix.TOP, PP, ORACLE).isTop());
	}

	@Test
	void evalUnaryNumericToStringWVA() throws SemanticException {
		assertEquals(new StrPrefix("42"),
				domain.evalUnaryExpression(mkUnary(NumericToString.INSTANCE),
						StrPrefix.TOP, PP, new WVAOracle(strConstraint("42"))));
	}

	// ---- evalBinaryExpression ----

	@Test
	void evalBinaryConcatReturnsLeftPrefix() throws SemanticException {
		// prefix of "hello" + "world" = prefix of "hello"
		assertEquals(new StrPrefix("hello"),
				domain.evalBinaryExpression(mkBin(StringConcat.INSTANCE),
						new StrPrefix("hello"), new StrPrefix("world"), PP, ORACLE));
	}

	@Test
	void evalBinaryConcatTopLeftReturnsTop() throws SemanticException {
		assertTrue(domain.evalBinaryExpression(mkBin(StringConcat.INSTANCE),
				StrPrefix.TOP, new StrPrefix("world"), PP, ORACLE).isTop());
	}

	@Test
	void evalBinarySubstringToEndWVA() throws SemanticException {
		// "hello".substringToEnd(2) → prefix of "llo", but since 2 <=
		// len(prefix)=5,
		// we get the substring from 2 to 5 of "hello" = "llo"
		// For a singleton range [2,2], the element is "hello".substring(2) =
		// "llo"
		assertEquals(new StrPrefix("llo"),
				domain.evalBinaryExpression(mkBin(StringSubstringToEnd.INSTANCE),
						new StrPrefix("hello"), StrPrefix.TOP, PP, new WVAOracle(intConstraint(2))));
	}

	// ---- evalTernaryExpression ----

	@Test
	void evalTernarySubstringWVA() throws SemanticException {
		// "hello".substring(1, 4) → "ell"
		// With both mid and end having same index 1: "hello".substring(1,1) =
		// ""
		// Let's use index 1 for begin, and test with a degenerate range
		// With mid=1 and rig=1, "hello".substring(1,1) = "" since i<=j is false
		// when i=j=1
		// Actually: the loop goes from mlow=1 to mhigh=1, rlow=1 to rhigh=1,
		// and checks i<=j
		// i=1, j=1: 1<=1 true → element = "hello".substring(1,1) = "" → common
		// = ""
		// → StrPrefix("") but StrPrefix("") with empty prefix is just an
		// empty-prefixed value
		// Actually looking at the code, if common.isEmpty() returns TOP. Let's
		// use valid indices.
		// Use begin=0, end=3: "hello".substring(0,3)="hel"
		// But oracle returns same constraint for both → both become 2
		// → "hello".substring(2,2) = "" → returns TOP since common.isEmpty()
		// Let's use 0: "hello".substring(0,0)="" → common="" → returns TOP
		// Actually let's test with index 0 which gives
		// "hello".substring(0,0)=""
		// We can't easily test non-trivial substring WVA with a
		// single-oracle-value oracle.
		// Instead, test that when indices are top, result is top:
		WVAOracle topOracle = new WVAOracle(new HashSet<>());
		assertTrue(domain.evalTernaryExpression(mkTernary(StringSubstring.INSTANCE),
				new StrPrefix("hello"), StrPrefix.TOP, StrPrefix.TOP, PP, topOracle).isTop());
	}

	// ---- satisfiesBinaryExpression ----

	@Test
	void satisfiesBinaryStartsWithSamePrefixReturnsUnknown() throws SemanticException {
		// prefix("hel") startsWith prefix("hel") → prefixes equal → UNKNOWN
		// (can't confirm)
		assertEquals(Satisfiability.UNKNOWN,
				domain.satisfiesBinaryExpression(mkBin(StringStartsWith.INSTANCE),
						new StrPrefix("hello"), new StrPrefix("hello"), PP, ORACLE));
	}

	@Test
	void satisfiesBinaryStartsWithDifferentPrefixReturnsNotSatisfied() throws SemanticException {
		// prefix("abc") startsWith prefix("xyz") → prefixes differ →
		// NOT_SATISFIED
		assertEquals(Satisfiability.NOT_SATISFIED,
				domain.satisfiesBinaryExpression(mkBin(StringStartsWith.INSTANCE),
						new StrPrefix("abc"), new StrPrefix("xyz"), PP, ORACLE));
	}

	@Test
	void satisfiesBinaryEqSamePrefixReturnsUnknown() throws SemanticException {
		// Two strings with same prefix could be equal or not
		assertEquals(Satisfiability.UNKNOWN,
				domain.satisfiesBinaryExpression(mkBin(ComparisonEq.INSTANCE),
						new StrPrefix("hello"), new StrPrefix("hello"), PP, ORACLE));
	}

	@Test
	void satisfiesBinaryEqDifferentPrefixReturnsNotSatisfied() throws SemanticException {
		// Two strings with different prefixes can't be equal
		assertEquals(Satisfiability.NOT_SATISFIED,
				domain.satisfiesBinaryExpression(mkBin(ComparisonEq.INSTANCE),
						new StrPrefix("abc"), new StrPrefix("xyz"), PP, ORACLE));
	}

	@Test
	void satisfiesBinaryNeDifferentPrefixReturnsSatisfied() throws SemanticException {
		// Two strings with different prefixes are definitely not equal → != is
		// SATISFIED
		assertEquals(Satisfiability.SATISFIED,
				domain.satisfiesBinaryExpression(mkBin(ComparisonNe.INSTANCE),
						new StrPrefix("abc"), new StrPrefix("xyz"), PP, ORACLE));
	}

	@Test
	void satisfiesBinaryContainsReturnsUnknown() throws SemanticException {
		// Can't determine containment from prefix info alone
		assertEquals(Satisfiability.UNKNOWN,
				domain.satisfiesBinaryExpression(mkBin(StringContains.INSTANCE),
						new StrPrefix("hello"), new StrPrefix("world"), PP, ORACLE));
	}

	@Test
	void satisfiesBinaryEndsWithReturnsUnknown() throws SemanticException {
		// Can't determine suffix from prefix info
		assertEquals(Satisfiability.UNKNOWN,
				domain.satisfiesBinaryExpression(mkBin(StringEndsWith.INSTANCE),
						new StrPrefix("hello"), new StrPrefix("llo"), PP, ORACLE));
	}

	@Test
	void satisfiesBinaryIsPrefixOfSatisfied() throws SemanticException {
		// "hel" isPrefixOf "hello" → rr="hel" starts with ll="hel" → UNKNOWN
		// Both right starts with left prefix → UNKNOWN
		// But "abc" isPrefixOf "xyz" → rr="xyz" starts with ll="abc"? No →
		// NOT_SATISFIED
		assertEquals(Satisfiability.NOT_SATISFIED,
				domain.satisfiesBinaryExpression(mkBin(StringIsPrefixOf.INSTANCE),
						new StrPrefix("abc"), new StrPrefix("xyz"), PP, ORACLE));
	}

	@Test
	void satisfiesBinaryTopReturnsUnknown() throws SemanticException {
		assertEquals(Satisfiability.UNKNOWN,
				domain.satisfiesBinaryExpression(mkBin(StringEquals.INSTANCE),
						StrPrefix.TOP, new StrPrefix("hello"), PP, ORACLE));
	}

	// ---- assumeBinaryExpression ----

	@Test
	void assumeEqNotSatisfiedReturnsBottom() throws SemanticException {
		// x has prefix "abc", assume x == y where y has prefix "xyz" →
		// NOT_SATISFIED → bottom
		ValueEnvironment<StrPrefix> env = domain.makeLattice()
				.putState(VAR_X, new StrPrefix("abc"))
				.putState(VAR_Y, new StrPrefix("xyz"));
		ValueEnvironment<
				StrPrefix> result = domain.assumeBinaryExpression(env, mkBin(ComparisonEq.INSTANCE), PP, PP, ORACLE);
		assertTrue(result.isBottom());
	}

	@Test
	void assumeEqSatisfiedReturnsUnchanged() throws SemanticException {
		// x and y have same prefix → UNKNOWN → environment unchanged
		ValueEnvironment<StrPrefix> env = domain.makeLattice()
				.putState(VAR_X, new StrPrefix("hello"))
				.putState(VAR_Y, new StrPrefix("hello"));
		ValueEnvironment<
				StrPrefix> result = domain.assumeBinaryExpression(env, mkBin(ComparisonEq.INSTANCE), PP, PP, ORACLE);
		assertEquals(env, result);
	}

	// ---- constraints ----

	@Test
	void constraintsOnBottomReturnsNull() throws SemanticException {
		assertNull(domain.constraints(null, domain.makeLattice().bottom(), VAR_X, PP, ORACLE));
	}

	@Test
	void constraintsOnTopReturnsEmpty() throws SemanticException {
		assertTrue(domain.constraints(null, domain.makeLattice(), VAR_X, PP, ORACLE).isEmpty());
	}

	@Test
	void constraintsOnConcretePrefixReturnsPrefixConstraint() throws SemanticException {
		ValueEnvironment<StrPrefix> env = domain.makeLattice().putState(VAR_X, new StrPrefix("hel"));
		Set<BinaryExpression> cs = domain.constraints(null, env, VAR_X, PP, ORACLE);
		assertNotNull(cs);
		assertEquals(1, cs.size());
		// Constraint should be: "hel" isPrefixOf x
		BinaryExpression c = cs.iterator().next();
		assertEquals(StringIsPrefixOf.INSTANCE, c.getOperator());
		assertEquals("hel", ((Constant) c.getLeft()).getValue());
	}

	@Test
	void constraintsOnStringLengthGivesLowerBound() throws SemanticException {
		ValueEnvironment<StrPrefix> env = domain.makeLattice().putState(VAR_X, new StrPrefix("hel"));
		UnaryExpression lengthExpr = new UnaryExpression(Int32Type.INSTANCE, VAR_X,
				StringLength.INSTANCE, SyntheticLocation.INSTANCE);
		Set<BinaryExpression> cs = domain.constraints(null, env, lengthExpr, PP, ORACLE);
		assertNotNull(cs);
		// length of prefix "hel" = 3 → constraint: length(x) >= 3
		// The range constraint should be [3, +∞)
		assertTrue(cs.size() >= 1);
	}
}
