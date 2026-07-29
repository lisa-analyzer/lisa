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
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class CharInclusionTest {

	private static final ProgramPoint PP = TestParameterProvider.provideParam(null, ProgramPoint.class);
	private static final SemanticOracle ORACLE = TestParameterProvider.provideParam(null, SemanticOracle.class);

	private static final Variable VAR_X = new Variable(StringType.INSTANCE, "x", SyntheticLocation.INSTANCE);
	private static final Variable VAR_Y = new Variable(StringType.INSTANCE, "y", SyntheticLocation.INSTANCE);
	private static final Variable VAR_Z = new Variable(StringType.INSTANCE, "z", SyntheticLocation.INSTANCE);

	private final CharInclusion domain = new CharInclusion();

	private static CharInclusion.CI ci(
			Set<Character> certainly,
			Set<Character> maybe) {
		return new CharInclusion.CI(certainly, maybe);
	}

	private static Set<Character> chars(
			String s) {
		Set<Character> set = new HashSet<>();
		for (char c : s.toCharArray())
			set.add(c);
		return set;
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

	// Oracle that returns different constraints for middle vs right
	// sub-expressions
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
		// "hello" has chars h,e,l,o (note: two l's → still one 'l' in the set)
		Set<Character> expected = chars("hello");
		Constant c = new Constant(StringType.INSTANCE, "hello", SyntheticLocation.INSTANCE);
		CharInclusion.CI result = domain.evalConstant(c, PP, ORACLE);
		assertEquals(ci(expected, expected), result);
	}

	@Test
	void evalConstantEmptyStringGivesEmptyCI() throws SemanticException {
		Constant c = new Constant(StringType.INSTANCE, "", SyntheticLocation.INSTANCE);
		CharInclusion.CI result = domain.evalConstant(c, PP, ORACLE);
		assertTrue(result.getCertainlyContained().isEmpty());
		assertNotNull(result.getMaybeContained());
		assertTrue(result.getMaybeContained().isEmpty());
	}

	@Test
	void evalConstantNonStringReturnsTop() throws SemanticException {
		Constant c = new Constant(Int32Type.INSTANCE, 42, SyntheticLocation.INSTANCE);
		assertTrue(domain.evalConstant(c, PP, ORACLE).isTop());
	}

	// ---- evalUnaryExpression ----

	@Test
	void evalUnaryReverseReturnsSameCI() throws SemanticException {
		// reverse does not change character inclusion
		Set<Character> cs = chars("hello");
		CharInclusion.CI arg = ci(cs, cs);
		assertEquals(arg,
				domain.evalUnaryExpression(mkUnary(StringReverse.INSTANCE), arg, PP, ORACLE));
	}

	@Test
	void evalUnaryTrimReturnsSameCI() throws SemanticException {
		Set<Character> cs = chars("hello");
		CharInclusion.CI arg = ci(cs, cs);
		assertEquals(arg,
				domain.evalUnaryExpression(mkUnary(StringTrim.INSTANCE), arg, PP, ORACLE));
	}

	@Test
	void evalUnaryToLowerCase() throws SemanticException {
		Set<Character> upper = chars("HELLO");
		Set<Character> lower = chars("hello");
		CharInclusion.CI arg = ci(upper, upper);
		CharInclusion.CI result = domain.evalUnaryExpression(mkUnary(StringToLowerCase.INSTANCE), arg, PP, ORACLE);
		assertEquals(ci(lower, lower), result);
	}

	@Test
	void evalUnaryToUpperCase() throws SemanticException {
		Set<Character> lower = chars("hello");
		Set<Character> upper = chars("HELLO");
		CharInclusion.CI arg = ci(lower, lower);
		CharInclusion.CI result = domain.evalUnaryExpression(mkUnary(StringToUpperCase.INSTANCE), arg, PP, ORACLE);
		assertEquals(ci(upper, upper), result);
	}

	@Test
	void evalUnaryTopArgReturnsTop() throws SemanticException {
		assertTrue(domain.evalUnaryExpression(mkUnary(StringToUpperCase.INSTANCE),
				domain.top(), PP, ORACLE).isTop());
	}

	@Test
	void evalUnaryUnknownOperatorReturnsTop() throws SemanticException {
		Set<Character> cs = chars("hello");
		assertTrue(domain.evalUnaryExpression(mkUnary(StringLength.INSTANCE),
				ci(cs, cs), PP, ORACLE).isTop());
	}

	@Test
	void evalUnaryNumericToStringWVA() throws SemanticException {
		// WVA: constraint says expr == "42" → CI for "42" = CI({4,2},{4,2})
		Set<Character> expected = chars("42");
		CharInclusion.CI result = domain.evalUnaryExpression(mkUnary(NumericToString.INSTANCE),
				domain.top(), PP, new WVAOracle(strConstraint("42")));
		assertEquals(ci(expected, expected), result);
	}

	// ---- evalBinaryExpression ----

	@Test
	void evalBinaryConcatUnionsBothSets() throws SemanticException {
		// "hello" concat "world"
		// CI("hello") = CI({h,e,l,o},{h,e,l,o})
		// CI("world") = CI({w,o,r,l,d},{w,o,r,l,d})
		// result: certainly = union = {h,e,l,o,w,r,d}, maybe = same
		Set<Character> helloChars = chars("hello");
		Set<Character> worldChars = chars("world");
		Set<Character> bothChars = chars("helloworld");
		CharInclusion.CI result = domain.evalBinaryExpression(mkBin(StringConcat.INSTANCE),
				ci(helloChars, helloChars), ci(worldChars, worldChars), PP, ORACLE);
		assertEquals(ci(bothChars, bothChars), result);
	}

	@Test
	void evalBinaryCharAtWVAReturnsAllCharsAsMaybe() throws SemanticException {
		// "hello".charAt(0): all chars from left are possibly included, none
		// for sure
		Set<Character> helloChars = chars("hello");
		CharInclusion.CI left = ci(helloChars, helloChars);
		BinaryExpression expr = new BinaryExpression(StringType.INSTANCE, VAR_X, VAR_Y,
				StringCharAt.INSTANCE, SyntheticLocation.INSTANCE);
		CharInclusion.CI result = domain.evalBinaryExpression(expr, left, domain.top(), PP,
				new WVAOracle(intConstraint(0)));
		assertTrue(result.getCertainlyContained().isEmpty());
		assertEquals(helloChars, result.getMaybeContained());
	}

	@Test
	void evalBinarySubstringToEndWVAReturnsAllCharsAsMaybe() throws SemanticException {
		Set<Character> helloChars = chars("hello");
		CharInclusion.CI left = ci(helloChars, helloChars);
		BinaryExpression expr = new BinaryExpression(StringType.INSTANCE, VAR_X, VAR_Y,
				StringSubstringToEnd.INSTANCE, SyntheticLocation.INSTANCE);
		CharInclusion.CI result = domain.evalBinaryExpression(expr, left, domain.top(), PP,
				new WVAOracle(intConstraint(2)));
		assertTrue(result.getCertainlyContained().isEmpty());
		assertEquals(helloChars, result.getMaybeContained());
	}

	// ---- evalTernaryExpression ----

	@Test
	void evalTernaryReplaceAllSomeCharsMoveToMaybe() throws SemanticException {
		// "hello".replaceAll("l", "r"):
		// certainly had {h,e,l,o}; after potentially replacing 'l' with 'r':
		// - 'l' moved out of certain (might be replaced), 'r' added to maybe
		// - h,e,o remain certain
		Set<Character> helloChars = chars("hello");
		CharInclusion.CI left = ci(helloChars, helloChars);
		CharInclusion.CI middle = ci(chars("l"), chars("l"));
		CharInclusion.CI right = ci(chars("r"), chars("r"));
		CharInclusion.CI result = domain.evalTernaryExpression(mkTernary(StringReplaceAll.INSTANCE),
				left, middle, right, PP, ORACLE);
		// h, e, o must still be certainly there; r is maybe there
		assertTrue(result.getCertainlyContained().contains('h'));
		assertTrue(result.getCertainlyContained().contains('e'));
		assertTrue(result.getCertainlyContained().contains('o'));
		// 'l' was moved out of certain since replacement might have happened
		assertTrue(!result.getCertainlyContained().contains('l') || result.getMaybeContained().contains('l'));
		// 'r' might be there from the replacement
		assertTrue(result.getMaybeContained() == null || result.getMaybeContained().contains('r'));
	}

	@Test
	void evalTernaryTopLeftReturnsTop() throws SemanticException {
		assertTrue(domain.evalTernaryExpression(mkTernary(StringReplaceAll.INSTANCE),
				domain.top(), ci(chars("l"), chars("l")), ci(chars("r"), chars("r")), PP, ORACLE).isTop());
	}

	@Test
	void evalTernarySubstringWVA() throws SemanticException {
		// "hello".substring(1, 4): all chars possibly included, none for sure
		Set<Character> helloChars = chars("hello");
		CharInclusion.CI left = ci(helloChars, helloChars);
		Set<BinaryExpression> midCs = intConstraint(1);
		Set<BinaryExpression> rigCs = new HashSet<>();
		rigCs.add(new BinaryExpression(Int32Type.INSTANCE,
				new Constant(Int32Type.INSTANCE, 4, SyntheticLocation.INSTANCE),
				VAR_Z, ComparisonEq.INSTANCE, SyntheticLocation.INSTANCE));
		TwoArgWVAOracle oracle = new TwoArgWVAOracle(VAR_Y, midCs, VAR_Z, rigCs);
		CharInclusion.CI result = domain.evalTernaryExpression(mkTernary(StringSubstring.INSTANCE),
				left, domain.top(), domain.top(), PP, oracle);
		assertTrue(result.getCertainlyContained().isEmpty());
		assertEquals(helloChars, result.getMaybeContained());
	}

	// ---- satisfiesBinaryExpression ----

	@Test
	void satisfiesBinaryRightEmptyStringContainsAlwaysSat() throws SemanticException {
		// any string contains the empty string
		Set<Character> helloChars = chars("hello");
		CharInclusion.CI emptyCI = ci(new HashSet<>(), new HashSet<>());
		assertEquals(Satisfiability.SATISFIED,
				domain.satisfiesBinaryExpression(mkBin(StringContains.INSTANCE),
						ci(helloChars, helloChars), emptyCI, PP, ORACLE));
	}

	@Test
	void satisfiesBinaryRightEmptyStringStartsWithAlwaysSat() throws SemanticException {
		Set<Character> helloChars = chars("hello");
		CharInclusion.CI emptyCI = ci(new HashSet<>(), new HashSet<>());
		assertEquals(Satisfiability.SATISFIED,
				domain.satisfiesBinaryExpression(mkBin(StringStartsWith.INSTANCE),
						ci(helloChars, helloChars), emptyCI, PP, ORACLE));
	}

	@Test
	void satisfiesBinaryRightEmptyStringEndsWithAlwaysSat() throws SemanticException {
		Set<Character> helloChars = chars("hello");
		CharInclusion.CI emptyCI = ci(new HashSet<>(), new HashSet<>());
		assertEquals(Satisfiability.SATISFIED,
				domain.satisfiesBinaryExpression(mkBin(StringEndsWith.INSTANCE),
						ci(helloChars, helloChars), emptyCI, PP, ORACLE));
	}

	@Test
	void satisfiesBinaryLeftEmptyContainsNonEmptyNotSat() throws SemanticException {
		// empty string contains nothing non-empty
		Set<Character> helloChars = chars("hello");
		CharInclusion.CI emptyCI = ci(new HashSet<>(), new HashSet<>());
		assertEquals(Satisfiability.NOT_SATISFIED,
				domain.satisfiesBinaryExpression(mkBin(StringContains.INSTANCE),
						emptyCI, ci(helloChars, helloChars), PP, ORACLE));
	}

	@Test
	void satisfiesBinaryLeftEmptyStartsWithNonEmptyNotSat() throws SemanticException {
		Set<Character> helloChars = chars("hello");
		CharInclusion.CI emptyCI = ci(new HashSet<>(), new HashSet<>());
		assertEquals(Satisfiability.NOT_SATISFIED,
				domain.satisfiesBinaryExpression(mkBin(StringStartsWith.INSTANCE),
						emptyCI, ci(helloChars, helloChars), PP, ORACLE));
	}

	@Test
	void satisfiesBinaryBothNonEmptyReturnsUnknown() throws SemanticException {
		Set<Character> helloChars = chars("hello");
		Set<Character> worldChars = chars("world");
		assertEquals(Satisfiability.UNKNOWN,
				domain.satisfiesBinaryExpression(mkBin(StringContains.INSTANCE),
						ci(helloChars, helloChars), ci(worldChars, worldChars), PP, ORACLE));
	}

	@Test
	void satisfiesBinaryTopReturnsUnknown() throws SemanticException {
		assertEquals(Satisfiability.UNKNOWN,
				domain.satisfiesBinaryExpression(mkBin(StringEquals.INSTANCE),
						domain.top(), ci(chars("hello"), chars("hello")), PP, ORACLE));
	}

	// ---- assumeBinaryExpression ----

	@Test
	void assumeEqRefinesIdentifier() throws SemanticException {
		// x is top, y is ci("hello"). Assume x == y → x refined to ci("hello")
		Set<Character> helloChars = chars("hello");
		CharInclusion.CI helloCI = ci(helloChars, helloChars);
		ValueEnvironment<CharInclusion.CI> env = domain.makeLattice()
				.putState(VAR_X, domain.top())
				.putState(VAR_Y, helloCI);
		ValueEnvironment<CharInclusion.CI> result = domain.assumeBinaryExpression(env, mkBin(ComparisonEq.INSTANCE), PP,
				PP, ORACLE);
		assertEquals(helloCI, result.getState(VAR_X));
	}

	@Test
	void assumeNonEqOperatorReturnsUnchanged() throws SemanticException {
		// CharInclusion.assumeBinaryExpression only handles ComparisonEq
		ValueEnvironment<CharInclusion.CI> env = domain.makeLattice()
				.putState(VAR_X, domain.top())
				.putState(VAR_Y, ci(chars("hello"), chars("hello")));
		ValueEnvironment<CharInclusion.CI> result = domain.assumeBinaryExpression(env, mkBin(StringContains.INSTANCE),
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
	void constraintsOnConcreteStringReturnsStringContainsForEachChar() throws SemanticException {
		// ci("hi") = CI({h,i},{h,i}) → constraints: "h" StringContains x, "i"
		// StringContains x
		Set<Character> hiChars = chars("hi");
		ValueEnvironment<CharInclusion.CI> env = domain.makeLattice().putState(VAR_X, ci(hiChars, hiChars));
		Set<BinaryExpression> cs = domain.constraints(null, env, VAR_X, PP, ORACLE);
		assertNotNull(cs);
		// one constraint per certainly-contained char: 'h' and 'i'
		assertEquals(hiChars.size(), cs.size());
		for (BinaryExpression c : cs) {
			assertEquals(StringContains.INSTANCE, c.getOperator());
			// left is the char as a string
			String charStr = ((Constant) c.getLeft()).getValue().toString();
			assertEquals(1, charStr.length());
			assertTrue(hiChars.contains(charStr.charAt(0)));
		}
	}

	@Test
	void constraintsOnTopCIReturnsEmpty() throws SemanticException {
		// top CI → no specific chars certainly contained → empty constraints
		ValueEnvironment<CharInclusion.CI> env = domain.makeLattice().putState(VAR_X, domain.top());
		Set<BinaryExpression> cs = domain.constraints(null, env, VAR_X, PP, ORACLE);
		assertNotNull(cs);
		assertTrue(cs.isEmpty());
	}

	@Test
	void constraintsOnStringLengthReturnsLowerBound() throws SemanticException {
		// ci("hi") has 2 certainly-contained chars → length >= 2
		Set<Character> hiChars = chars("hi");
		ValueEnvironment<CharInclusion.CI> env = domain.makeLattice().putState(VAR_X, ci(hiChars, hiChars));
		UnaryExpression lengthExpr = new UnaryExpression(Int32Type.INSTANCE, VAR_X,
				StringLength.INSTANCE, SyntheticLocation.INSTANCE);
		Set<BinaryExpression> cs = domain.constraints(null, env, lengthExpr, PP, ORACLE);
		assertNotNull(cs);
		assertTrue(cs.size() >= 1);
	}
}
