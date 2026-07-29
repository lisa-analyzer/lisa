package it.unive.lisa.analysis.string;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.TestParameterProvider;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.analysis.value.ValueDomain;
import it.unive.lisa.lattices.Satisfiability;
import it.unive.lisa.lattices.string.StrSuffix;
import it.unive.lisa.program.SyntheticLocation;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.program.type.Int32Type;
import it.unive.lisa.program.type.StringType;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonEq;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonNe;
import it.unive.lisa.symbolic.value.operator.binary.StringConcat;
import it.unive.lisa.symbolic.value.operator.binary.StringContains;
import it.unive.lisa.symbolic.value.operator.binary.StringEndsWith;
import it.unive.lisa.symbolic.value.operator.binary.StringEquals;
import it.unive.lisa.symbolic.value.operator.binary.StringIsSuffixOf;
import it.unive.lisa.symbolic.value.operator.binary.StringStartsWith;
import it.unive.lisa.symbolic.value.operator.unary.NumericToString;
import it.unive.lisa.symbolic.value.operator.unary.StringLength;
import it.unive.lisa.symbolic.value.operator.unary.StringReverse;
import it.unive.lisa.symbolic.value.operator.unary.StringToLowerCase;
import it.unive.lisa.symbolic.value.operator.unary.StringToUpperCase;
import it.unive.lisa.symbolic.value.operator.unary.StringTrim;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class SuffixTest {

	private static final ProgramPoint PP = TestParameterProvider.provideParam(null, ProgramPoint.class);
	private static final SemanticOracle ORACLE = TestParameterProvider.provideParam(null, SemanticOracle.class);

	private static final Variable VAR_X = new Variable(StringType.INSTANCE, "x", SyntheticLocation.INSTANCE);
	private static final Variable VAR_Y = new Variable(StringType.INSTANCE, "y", SyntheticLocation.INSTANCE);

	private final Suffix domain = new Suffix();

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
		Constant c = new Constant(StringType.INSTANCE, "world", SyntheticLocation.INSTANCE);
		assertEquals(new StrSuffix("world"), domain.evalConstant(c, PP, ORACLE));
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
		assertEquals(new StrSuffix("world"),
				domain.evalUnaryExpression(mkUnary(StringToLowerCase.INSTANCE),
						new StrSuffix("WORLD"), PP, ORACLE));
	}

	@Test
	void evalUnaryToUpperCase() throws SemanticException {
		assertEquals(new StrSuffix("WORLD"),
				domain.evalUnaryExpression(mkUnary(StringToUpperCase.INSTANCE),
						new StrSuffix("world"), PP, ORACLE));
	}

	@Test
	void evalUnaryTrimStripsTrailingSpaces() throws SemanticException {
		// StringUtils.stripEnd strips trailing whitespace
		assertEquals(new StrSuffix("hello"),
				domain.evalUnaryExpression(mkUnary(StringTrim.INSTANCE),
						new StrSuffix("hello   "), PP, ORACLE));
	}

	@Test
	void evalUnaryReverseReturnsTop() throws SemanticException {
		// reversing a suffix gives no suffix info (would need prefix info)
		assertTrue(domain.evalUnaryExpression(mkUnary(StringReverse.INSTANCE),
				new StrSuffix("world"), PP, ORACLE).isTop());
	}

	@Test
	void evalUnaryTopArgReturnsTop() throws SemanticException {
		assertTrue(domain.evalUnaryExpression(mkUnary(StringToUpperCase.INSTANCE),
				StrSuffix.TOP, PP, ORACLE).isTop());
	}

	@Test
	void evalUnaryNumericToStringWVA() throws SemanticException {
		assertEquals(new StrSuffix("42"),
				domain.evalUnaryExpression(mkUnary(NumericToString.INSTANCE),
						StrSuffix.TOP, PP, new WVAOracle(strConstraint("42"))));
	}

	// ---- evalBinaryExpression ----

	@Test
	void evalBinaryConcatReturnsRightSuffix() throws SemanticException {
		// suffix of "hello" + "world" = suffix of "world"
		assertEquals(new StrSuffix("world"),
				domain.evalBinaryExpression(mkBin(StringConcat.INSTANCE),
						new StrSuffix("hello"), new StrSuffix("world"), PP, ORACLE));
	}

	@Test
	void evalBinaryConcatWithTop() throws SemanticException {
		assertFalse(domain.evalBinaryExpression(mkBin(StringConcat.INSTANCE),
				StrSuffix.TOP, new StrSuffix("world"), PP, ORACLE).isTop());
		assertTrue(domain.evalBinaryExpression(mkBin(StringConcat.INSTANCE),
				new StrSuffix("hello"), StrSuffix.TOP, PP, ORACLE).isTop());
	}

	// ---- satisfiesBinaryExpression ----

	@Test
	void satisfiesBinaryEqSameSuffixReturnsUnknown() throws SemanticException {
		// Two strings with same suffix could be equal or not
		assertEquals(Satisfiability.UNKNOWN,
				domain.satisfiesBinaryExpression(mkBin(ComparisonEq.INSTANCE),
						new StrSuffix("world"), new StrSuffix("world"), PP, ORACLE));
	}

	@Test
	void satisfiesBinaryEqDifferentEndCharsReturnsNotSatisfied() throws SemanticException {
		// "world" vs "abc": last 3 chars are "rld" vs "abc" → strings can't be
		// equal
		assertEquals(Satisfiability.NOT_SATISFIED,
				domain.satisfiesBinaryExpression(mkBin(ComparisonEq.INSTANCE),
						new StrSuffix("world"), new StrSuffix("abc"), PP, ORACLE));
	}

	@Test
	void satisfiesBinaryEqCompatibleTailsReturnsUnknown() throws SemanticException {
		// "world" (len 5) vs "ld" (len 2): last 2 chars of "world" = "ld" =
		// suffix "ld"
		// So the strings could be equal → UNKNOWN
		assertEquals(Satisfiability.UNKNOWN,
				domain.satisfiesBinaryExpression(mkBin(ComparisonEq.INSTANCE),
						new StrSuffix("world"), new StrSuffix("ld"), PP, ORACLE));
	}

	@Test
	void satisfiesBinaryEndsWithSameSuffixReturnsUnknown() throws SemanticException {
		// "world" endsWith "world" → suffixes match → UNKNOWN
		assertEquals(Satisfiability.UNKNOWN,
				domain.satisfiesBinaryExpression(mkBin(StringEndsWith.INSTANCE),
						new StrSuffix("world"), new StrSuffix("world"), PP, ORACLE));
	}

	@Test
	void satisfiesBinaryEndsWithDifferentTailNotSatisfied() throws SemanticException {
		// "world" endsWith "abc" → last 3 chars of "world"="rld" vs "abc" →
		// NOT_SATISFIED
		assertEquals(Satisfiability.NOT_SATISFIED,
				domain.satisfiesBinaryExpression(mkBin(StringEndsWith.INSTANCE),
						new StrSuffix("world"), new StrSuffix("abc"), PP, ORACLE));
	}

	@Test
	void satisfiesBinaryNeDifferentSuffixReturnsSatisfied() throws SemanticException {
		// "world" != "abc" → definitely different → SATISFIED
		assertEquals(Satisfiability.SATISFIED,
				domain.satisfiesBinaryExpression(mkBin(ComparisonNe.INSTANCE),
						new StrSuffix("world"), new StrSuffix("abc"), PP, ORACLE));
	}

	@Test
	void satisfiesBinaryStartsWithReturnsUnknown() throws SemanticException {
		// Can't determine prefix from suffix info
		assertEquals(Satisfiability.UNKNOWN,
				domain.satisfiesBinaryExpression(mkBin(StringStartsWith.INSTANCE),
						new StrSuffix("world"), new StrSuffix("wo"), PP, ORACLE));
	}

	@Test
	void satisfiesBinaryContainsReturnsUnknown() throws SemanticException {
		assertEquals(Satisfiability.UNKNOWN,
				domain.satisfiesBinaryExpression(mkBin(StringContains.INSTANCE),
						new StrSuffix("world"), new StrSuffix("orl"), PP, ORACLE));
	}

	@Test
	void satisfiesBinaryTopReturnsUnknown() throws SemanticException {
		assertEquals(Satisfiability.UNKNOWN,
				domain.satisfiesBinaryExpression(mkBin(StringEquals.INSTANCE),
						StrSuffix.TOP, new StrSuffix("world"), PP, ORACLE));
	}

	// ---- assumeBinaryExpression ----

	@Test
	void assumeEqNotSatisfiedReturnsBottom() throws SemanticException {
		// x has suffix "world", y has suffix "abc" (incompatible tails) →
		// NOT_SATISFIED → bottom
		ValueEnvironment<StrSuffix> env = domain.makeLattice()
				.putState(VAR_X, new StrSuffix("world"))
				.putState(VAR_Y, new StrSuffix("abc"));
		ValueEnvironment<
				StrSuffix> result = domain.assumeBinaryExpression(env, mkBin(ComparisonEq.INSTANCE), PP, PP, ORACLE);
		assertTrue(result.isBottom());
	}

	@Test
	void assumeEqUnknownReturnsUnchanged() throws SemanticException {
		// x and y have same suffix → UNKNOWN → unchanged
		ValueEnvironment<StrSuffix> env = domain.makeLattice()
				.putState(VAR_X, new StrSuffix("world"))
				.putState(VAR_Y, new StrSuffix("world"));
		ValueEnvironment<
				StrSuffix> result = domain.assumeBinaryExpression(env, mkBin(ComparisonEq.INSTANCE), PP, PP, ORACLE);
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
	void constraintsOnConcreteSuffixReturnsSuffixConstraint() throws SemanticException {
		ValueEnvironment<StrSuffix> env = domain.makeLattice().putState(VAR_X, new StrSuffix("rld"));
		Set<BinaryExpression> cs = domain.constraints(null, env, VAR_X, PP, ORACLE);
		assertNotNull(cs);
		assertEquals(1, cs.size());
		// Constraint should be: "rld" isSuffixOf x
		BinaryExpression c = cs.iterator().next();
		assertEquals(StringIsSuffixOf.INSTANCE, c.getOperator());
		assertEquals("rld", ((Constant) c.getLeft()).getValue());
	}

	@Test
	void constraintsOnStringLengthGivesLowerBound() throws SemanticException {
		ValueEnvironment<StrSuffix> env = domain.makeLattice().putState(VAR_X, new StrSuffix("rld"));
		UnaryExpression lengthExpr = new UnaryExpression(Int32Type.INSTANCE, VAR_X,
				StringLength.INSTANCE, SyntheticLocation.INSTANCE);
		Set<BinaryExpression> cs = domain.constraints(null, env, lengthExpr, PP, ORACLE);
		assertNotNull(cs);
		// length of suffix "rld" = 3 → constraint: length(x) >= 3
		assertTrue(cs.size() >= 1);
	}
}
