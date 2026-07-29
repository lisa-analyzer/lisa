package it.unive.lisa.analysis.string.tarsis;

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
import it.unive.lisa.lattices.string.tarsis.RegexAutomaton;
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
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonEq;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonNe;
import it.unive.lisa.symbolic.value.operator.binary.StringCharAt;
import it.unive.lisa.symbolic.value.operator.binary.StringConcat;
import it.unive.lisa.symbolic.value.operator.binary.StringContains;
import it.unive.lisa.symbolic.value.operator.binary.StringEquals;
import it.unive.lisa.symbolic.value.operator.binary.StringEqualsIgnoreCase;
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

public class TarsisTest {

	private static final ProgramPoint PP = TestParameterProvider.provideParam(null, ProgramPoint.class);
	private static final SemanticOracle ORACLE = TestParameterProvider.provideParam(null, SemanticOracle.class);

	private static final Variable VAR_X = new Variable(StringType.INSTANCE, "x", SyntheticLocation.INSTANCE);
	private static final Variable VAR_Y = new Variable(StringType.INSTANCE, "y", SyntheticLocation.INSTANCE);

	private final Tarsis domain = new Tarsis();

	private static BinaryExpression mkBin(
			BinaryOperator op) {
		return new BinaryExpression(StringType.INSTANCE, VAR_X, VAR_Y, op, SyntheticLocation.INSTANCE);
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

	// ---- evalConstant ----

	@Test
	void evalConstantConcreteString()
			throws SemanticException {
		RegexAutomaton result = domain.evalConstant(
				new Constant(StringType.INSTANCE, "hello", SyntheticLocation.INSTANCE), PP, ORACLE);
		assertTrue(result.isEqualTo(RegexAutomaton.string("hello")));
	}

	@Test
	void evalConstantEmptyString()
			throws SemanticException {
		RegexAutomaton result = domain.evalConstant(
				new Constant(StringType.INSTANCE, "", SyntheticLocation.INSTANCE), PP, ORACLE);
		assertTrue(result.isEqualTo(RegexAutomaton.string("")));
	}

	@Test
	void evalConstantNonStringReturnsTop()
			throws SemanticException {
		RegexAutomaton result = domain.evalConstant(
				new Constant(Int32Type.INSTANCE, 42, SyntheticLocation.INSTANCE), PP, ORACLE);
		assertTrue(result.isTop());
	}

	// ---- evalUnaryExpression ----

	@Test
	void evalUnaryReverseReturnsReversed()
			throws SemanticException {
		RegexAutomaton arg = RegexAutomaton.string("hello");
		UnaryExpression expr = new UnaryExpression(StringType.INSTANCE, VAR_X,
				StringReverse.INSTANCE, SyntheticLocation.INSTANCE);
		RegexAutomaton result = domain.evalUnaryExpression(expr, arg, PP, ORACLE);
		assertTrue(result.isEqualTo(RegexAutomaton.string("olleh")));
	}

	@Test
	void evalUnaryToLowerCase()
			throws SemanticException {
		RegexAutomaton arg = RegexAutomaton.string("HELLO");
		UnaryExpression expr = new UnaryExpression(StringType.INSTANCE, VAR_X,
				StringToLowerCase.INSTANCE, SyntheticLocation.INSTANCE);
		RegexAutomaton result = domain.evalUnaryExpression(expr, arg, PP, ORACLE);
		assertTrue(result.isEqualTo(RegexAutomaton.string("hello")));
	}

	@Test
	void evalUnaryToUpperCase()
			throws SemanticException {
		RegexAutomaton arg = RegexAutomaton.string("hello");
		UnaryExpression expr = new UnaryExpression(StringType.INSTANCE, VAR_X,
				StringToUpperCase.INSTANCE, SyntheticLocation.INSTANCE);
		RegexAutomaton result = domain.evalUnaryExpression(expr, arg, PP, ORACLE);
		assertTrue(result.isEqualTo(RegexAutomaton.string("HELLO")));
	}

	@Test
	void evalUnaryTrim()
			throws SemanticException {
		RegexAutomaton arg = RegexAutomaton.string("  hello  ");
		UnaryExpression expr = new UnaryExpression(StringType.INSTANCE, VAR_X,
				StringTrim.INSTANCE, SyntheticLocation.INSTANCE);
		RegexAutomaton result = domain.evalUnaryExpression(expr, arg, PP, ORACLE);
		assertTrue(result.isEqualTo(RegexAutomaton.string("hello")));
	}

	@Test
	void evalUnaryTopArgReturnsTop()
			throws SemanticException {
		UnaryExpression expr = new UnaryExpression(StringType.INSTANCE, VAR_X,
				StringReverse.INSTANCE, SyntheticLocation.INSTANCE);
		RegexAutomaton result = domain.evalUnaryExpression(expr, RegexAutomaton.TOP, PP, ORACLE);
		assertTrue(result.isTop());
	}

	@Test
	void evalUnaryUnknownOperatorReturnsTop()
			throws SemanticException {
		RegexAutomaton arg = RegexAutomaton.string("hello");
		UnaryExpression expr = new UnaryExpression(Int32Type.INSTANCE, VAR_X,
				StringLength.INSTANCE, SyntheticLocation.INSTANCE);
		RegexAutomaton result = domain.evalUnaryExpression(expr, arg, PP, ORACLE);
		assertTrue(result.isTop());
	}

	@Test
	void evalUnaryNumericToStringWVA()
			throws SemanticException {
		UnaryExpression numToStrExpr = new UnaryExpression(StringType.INSTANCE, VAR_X,
				NumericToString.INSTANCE, SyntheticLocation.INSTANCE);
		BinaryExpression constraint = new BinaryExpression(StringType.INSTANCE,
				new Constant(StringType.INSTANCE, "42", SyntheticLocation.INSTANCE),
				numToStrExpr, ComparisonEq.INSTANCE, SyntheticLocation.INSTANCE);
		Set<BinaryExpression> cs = new HashSet<>();
		cs.add(constraint);
		WVAOracle wva = new WVAOracle(cs);
		RegexAutomaton result = domain.evalUnaryExpression(numToStrExpr, RegexAutomaton.TOP, PP, wva);
		assertTrue(result.isEqualTo(RegexAutomaton.string("42")));
	}

	// ---- evalBinaryExpression ----

	@Test
	void evalBinaryConcat()
			throws SemanticException {
		RegexAutomaton left = RegexAutomaton.string("hello");
		RegexAutomaton right = RegexAutomaton.string("world");
		BinaryExpression expr = new BinaryExpression(StringType.INSTANCE, VAR_X, VAR_Y,
				StringConcat.INSTANCE, SyntheticLocation.INSTANCE);
		RegexAutomaton result = domain.evalBinaryExpression(expr, left, right, PP, ORACLE);
		assertTrue(result.isEqualTo(RegexAutomaton.string("helloworld")));
	}

	@Test
	void evalBinaryCharAtWVA()
			throws SemanticException {
		// "hello".charAt(1) = "e"
		RegexAutomaton left = RegexAutomaton.string("hello");
		ValueExpression idxExpr = new Constant(Int32Type.INSTANCE, 1, SyntheticLocation.INSTANCE);
		BinaryExpression charAtExpr = new BinaryExpression(StringType.INSTANCE, VAR_X, idxExpr,
				StringCharAt.INSTANCE, SyntheticLocation.INSTANCE);
		BinaryExpression constraint = new BinaryExpression(Int32Type.INSTANCE,
				new Constant(Int32Type.INSTANCE, 1, SyntheticLocation.INSTANCE),
				idxExpr, ComparisonEq.INSTANCE, SyntheticLocation.INSTANCE);
		Set<BinaryExpression> cs = new HashSet<>();
		cs.add(constraint);
		WVAOracle wva = new WVAOracle(cs);
		RegexAutomaton result = domain.evalBinaryExpression(charAtExpr, left, RegexAutomaton.TOP, PP, wva);
		assertTrue(result.isEqualTo(RegexAutomaton.string("e")));
	}

	@Test
	void evalBinarySubstringToEndWVA()
			throws SemanticException {
		// "hello".substringToEnd(2) = "llo"
		RegexAutomaton left = RegexAutomaton.string("hello");
		ValueExpression idxExpr = new Constant(Int32Type.INSTANCE, 2, SyntheticLocation.INSTANCE);
		BinaryExpression substrExpr = new BinaryExpression(StringType.INSTANCE, VAR_X, idxExpr,
				StringSubstringToEnd.INSTANCE, SyntheticLocation.INSTANCE);
		BinaryExpression constraint = new BinaryExpression(Int32Type.INSTANCE,
				new Constant(Int32Type.INSTANCE, 2, SyntheticLocation.INSTANCE),
				idxExpr, ComparisonEq.INSTANCE, SyntheticLocation.INSTANCE);
		Set<BinaryExpression> cs = new HashSet<>();
		cs.add(constraint);
		WVAOracle wva = new WVAOracle(cs);
		RegexAutomaton result = domain.evalBinaryExpression(substrExpr, left, RegexAutomaton.TOP, PP, wva);
		assertTrue(result.isEqualTo(RegexAutomaton.string("llo")));
	}

	@Test
	void evalBinarySubstringToEndAtLengthWVA()
			throws SemanticException {
		// "hello".substringToEnd(5) = "" (index == length)
		RegexAutomaton left = RegexAutomaton.string("hello");
		ValueExpression idxExpr = new Constant(Int32Type.INSTANCE, 5, SyntheticLocation.INSTANCE);
		BinaryExpression substrExpr = new BinaryExpression(StringType.INSTANCE, VAR_X, idxExpr,
				StringSubstringToEnd.INSTANCE, SyntheticLocation.INSTANCE);
		BinaryExpression constraint = new BinaryExpression(Int32Type.INSTANCE,
				new Constant(Int32Type.INSTANCE, 5, SyntheticLocation.INSTANCE),
				idxExpr, ComparisonEq.INSTANCE, SyntheticLocation.INSTANCE);
		Set<BinaryExpression> cs = new HashSet<>();
		cs.add(constraint);
		WVAOracle wva = new WVAOracle(cs);
		RegexAutomaton result = domain.evalBinaryExpression(substrExpr, left, RegexAutomaton.TOP, PP, wva);
		assertTrue(result.isEqualTo(RegexAutomaton.string("")));
	}

	// ---- evalTernaryExpression ----

	@Test
	void evalTernaryReplaceAll()
			throws SemanticException {
		// "hello".replaceAll("l", "r") = "herro"
		RegexAutomaton left = RegexAutomaton.string("hello");
		RegexAutomaton middle = RegexAutomaton.string("l");
		RegexAutomaton right = RegexAutomaton.string("r");
		TernaryExpression expr = new TernaryExpression(StringType.INSTANCE, VAR_X, VAR_Y, VAR_Y,
				StringReplaceAll.INSTANCE, SyntheticLocation.INSTANCE);
		RegexAutomaton result = domain.evalTernaryExpression(expr, left, middle, right, PP, ORACLE);
		assertTrue(result.isEqualTo(RegexAutomaton.string("herro")));
	}

	@Test
	void evalTernaryTopLeftReturnsTop()
			throws SemanticException {
		TernaryExpression expr = new TernaryExpression(StringType.INSTANCE, VAR_X, VAR_Y, VAR_Y,
				StringReplaceAll.INSTANCE, SyntheticLocation.INSTANCE);
		RegexAutomaton result = domain.evalTernaryExpression(expr, RegexAutomaton.TOP,
				RegexAutomaton.string("l"), RegexAutomaton.string("r"), PP, ORACLE);
		assertTrue(result.isTop());
	}

	@Test
	void evalTernarySubstringWVA()
			throws SemanticException {
		// "hello".substring(1, 4) = "ell"
		RegexAutomaton left = RegexAutomaton.string("hello");
		ValueExpression midExpr = new Constant(Int32Type.INSTANCE, 1, SyntheticLocation.INSTANCE);
		ValueExpression rigExpr = new Constant(Int32Type.INSTANCE, 4, SyntheticLocation.INSTANCE);
		TernaryExpression substrExpr = new TernaryExpression(StringType.INSTANCE, VAR_X,
				midExpr, rigExpr, StringSubstring.INSTANCE, SyntheticLocation.INSTANCE);
		BinaryExpression midConstraint = new BinaryExpression(Int32Type.INSTANCE,
				new Constant(Int32Type.INSTANCE, 1, SyntheticLocation.INSTANCE),
				midExpr, ComparisonEq.INSTANCE, SyntheticLocation.INSTANCE);
		BinaryExpression rigConstraint = new BinaryExpression(Int32Type.INSTANCE,
				new Constant(Int32Type.INSTANCE, 4, SyntheticLocation.INSTANCE),
				rigExpr, ComparisonEq.INSTANCE, SyntheticLocation.INSTANCE);
		Set<BinaryExpression> midSet = new HashSet<>();
		midSet.add(midConstraint);
		Set<BinaryExpression> rigSet = new HashSet<>();
		rigSet.add(rigConstraint);
		TwoArgWVAOracle wva = new TwoArgWVAOracle(midExpr, midSet, rigExpr, rigSet);
		RegexAutomaton result = domain.evalTernaryExpression(substrExpr, left,
				RegexAutomaton.TOP, RegexAutomaton.TOP, PP, wva);
		assertTrue(result.isEqualTo(RegexAutomaton.string("ell")));
	}

	@Test
	void evalTernarySubstringWVAAtFullLength()
			throws SemanticException {
		// "hello".substring(0, 5) = "hello"
		RegexAutomaton left = RegexAutomaton.string("hello");
		ValueExpression midExpr = new Constant(Int32Type.INSTANCE, 0, SyntheticLocation.INSTANCE);
		ValueExpression rigExpr = new Constant(Int32Type.INSTANCE, 5, SyntheticLocation.INSTANCE);
		TernaryExpression substrExpr = new TernaryExpression(StringType.INSTANCE, VAR_X,
				midExpr, rigExpr, StringSubstring.INSTANCE, SyntheticLocation.INSTANCE);
		BinaryExpression midConstraint = new BinaryExpression(Int32Type.INSTANCE,
				new Constant(Int32Type.INSTANCE, 0, SyntheticLocation.INSTANCE),
				midExpr, ComparisonEq.INSTANCE, SyntheticLocation.INSTANCE);
		BinaryExpression rigConstraint = new BinaryExpression(Int32Type.INSTANCE,
				new Constant(Int32Type.INSTANCE, 5, SyntheticLocation.INSTANCE),
				rigExpr, ComparisonEq.INSTANCE, SyntheticLocation.INSTANCE);
		Set<BinaryExpression> midSet = new HashSet<>();
		midSet.add(midConstraint);
		Set<BinaryExpression> rigSet = new HashSet<>();
		rigSet.add(rigConstraint);
		TwoArgWVAOracle wva = new TwoArgWVAOracle(midExpr, midSet, rigExpr, rigSet);
		RegexAutomaton result = domain.evalTernaryExpression(substrExpr, left,
				RegexAutomaton.TOP, RegexAutomaton.TOP, PP, wva);
		assertTrue(result.isEqualTo(RegexAutomaton.string("hello")));
	}

	// ---- satisfiesBinaryExpression ----

	@Test
	void satisfiesBinaryEqSame()
			throws SemanticException {
		assertEquals(Satisfiability.SATISFIED,
				domain.satisfiesBinaryExpression(mkBin(ComparisonEq.INSTANCE),
						RegexAutomaton.string("hello"), RegexAutomaton.string("hello"), PP, ORACLE));
	}

	@Test
	void satisfiesBinaryEqDiff()
			throws SemanticException {
		assertEquals(Satisfiability.NOT_SATISFIED,
				domain.satisfiesBinaryExpression(mkBin(ComparisonEq.INSTANCE),
						RegexAutomaton.string("hello"), RegexAutomaton.string("world"), PP, ORACLE));
	}

	@Test
	void satisfiesBinaryNeSameStrings()
			throws SemanticException {
		assertEquals(Satisfiability.NOT_SATISFIED,
				domain.satisfiesBinaryExpression(mkBin(ComparisonNe.INSTANCE),
						RegexAutomaton.string("hello"), RegexAutomaton.string("hello"), PP, ORACLE));
	}

	@Test
	void satisfiesBinaryNeDiffStrings()
			throws SemanticException {
		assertEquals(Satisfiability.SATISFIED,
				domain.satisfiesBinaryExpression(mkBin(ComparisonNe.INSTANCE),
						RegexAutomaton.string("hello"), RegexAutomaton.string("world"), PP, ORACLE));
	}

	@Test
	void satisfiesBinaryContainsFound()
			throws SemanticException {
		assertEquals(Satisfiability.SATISFIED,
				domain.satisfiesBinaryExpression(mkBin(StringContains.INSTANCE),
						RegexAutomaton.string("hello"), RegexAutomaton.string("ell"), PP, ORACLE));
	}

	@Test
	void satisfiesBinaryContainsNotFound()
			throws SemanticException {
		assertEquals(Satisfiability.NOT_SATISFIED,
				domain.satisfiesBinaryExpression(mkBin(StringContains.INSTANCE),
						RegexAutomaton.string("hello"), RegexAutomaton.string("xyz"), PP, ORACLE));
	}

	@Test
	void satisfiesBinaryStringEquals()
			throws SemanticException {
		assertEquals(Satisfiability.SATISFIED,
				domain.satisfiesBinaryExpression(mkBin(StringEquals.INSTANCE),
						RegexAutomaton.string("hello"), RegexAutomaton.string("hello"), PP, ORACLE));
	}

	@Test
	void satisfiesBinaryEqualsIgnoreCase()
			throws SemanticException {
		assertEquals(Satisfiability.SATISFIED,
				domain.satisfiesBinaryExpression(mkBin(StringEqualsIgnoreCase.INSTANCE),
						RegexAutomaton.string("HELLO"), RegexAutomaton.string("hello"), PP, ORACLE));
	}

	@Test
	void satisfiesBinaryTopLeftReturnsUnknown()
			throws SemanticException {
		assertEquals(Satisfiability.UNKNOWN,
				domain.satisfiesBinaryExpression(mkBin(ComparisonEq.INSTANCE),
						RegexAutomaton.TOP, RegexAutomaton.string("hello"), PP, ORACLE));
	}

	@Test
	void satisfiesBinaryTopRightReturnsUnknown()
			throws SemanticException {
		assertEquals(Satisfiability.UNKNOWN,
				domain.satisfiesBinaryExpression(mkBin(ComparisonEq.INSTANCE),
						RegexAutomaton.string("hello"), RegexAutomaton.TOP, PP, ORACLE));
	}

	// ---- assumeBinaryExpression ----

	@Test
	void assumeEqRefinesIdentifier()
			throws SemanticException {
		RegexAutomaton helloAut = RegexAutomaton.string("hello");
		ValueEnvironment<RegexAutomaton> env = domain.makeLattice()
				.putState(VAR_X, domain.top())
				.putState(VAR_Y, helloAut);
		ValueEnvironment<RegexAutomaton> result = domain.assumeBinaryExpression(env,
				mkBin(ComparisonEq.INSTANCE), PP, PP, ORACLE);
		assertTrue(result.getState(VAR_X).isEqualTo(helloAut));
	}

	@Test
	void assumeNonEqOperatorReturnsUnchanged()
			throws SemanticException {
		ValueEnvironment<RegexAutomaton> env = domain.makeLattice()
				.putState(VAR_X, domain.top())
				.putState(VAR_Y, RegexAutomaton.string("hello"));
		ValueEnvironment<RegexAutomaton> result = domain.assumeBinaryExpression(env,
				mkBin(StringContains.INSTANCE), PP, PP, ORACLE);
		assertEquals(env, result);
	}

	// ---- constraints ----

	@Test
	void constraintsOnBottomStateReturnsNull()
			throws SemanticException {
		assertNull(domain.constraints(null, domain.makeLattice().bottom(), VAR_X, PP, ORACLE));
	}

	@Test
	void constraintsOnTopStateReturnsEmpty()
			throws SemanticException {
		assertTrue(domain.constraints(null, domain.makeLattice(), VAR_X, PP, ORACLE).isEmpty());
	}

	@Test
	void constraintsOnSingleStringReturnsEqConstraint()
			throws SemanticException {
		ValueEnvironment<RegexAutomaton> env = domain.makeLattice()
				.putState(VAR_X, RegexAutomaton.string("hello"));
		Set<BinaryExpression> cs = domain.constraints(null, env, VAR_X, PP, ORACLE);
		assertNotNull(cs);
		assertEquals(1, cs.size());
		BinaryExpression c = cs.iterator().next();
		assertEquals(ComparisonEq.INSTANCE, c.getOperator());
		assertEquals("hello", ((Constant) c.getLeft()).getValue());
	}

	@Test
	void constraintsOnTopVarReturnsEmpty()
			throws SemanticException {
		ValueEnvironment<RegexAutomaton> env = domain.makeLattice()
				.putState(VAR_X, domain.top());
		Set<BinaryExpression> cs = domain.constraints(null, env, VAR_X, PP, ORACLE);
		assertNotNull(cs);
		assertTrue(cs.isEmpty());
	}

	@Test
	void constraintsOnStringLengthReturnsBound()
			throws SemanticException {
		// x = "hello" → length = 5
		ValueEnvironment<RegexAutomaton> env = domain.makeLattice()
				.putState(VAR_X, RegexAutomaton.string("hello"));
		UnaryExpression lengthExpr = new UnaryExpression(Int32Type.INSTANCE, VAR_X,
				StringLength.INSTANCE, SyntheticLocation.INSTANCE);
		Set<BinaryExpression> cs = domain.constraints(null, env, lengthExpr, PP, ORACLE);
		assertNotNull(cs);
		assertTrue(cs.size() >= 1);
	}
}
