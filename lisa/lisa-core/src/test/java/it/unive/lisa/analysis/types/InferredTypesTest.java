package it.unive.lisa.analysis.types;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;

import it.unive.lisa.analysis.SemanticDomain.Satisfiability;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.nonrelational.inference.InferredValue.InferredPair;
import it.unive.lisa.caches.Caches;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.BinaryOperator;
import it.unive.lisa.symbolic.value.TernaryOperator;
import it.unive.lisa.symbolic.value.UnaryOperator;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeTokenType;
import it.unive.lisa.type.Untyped;
import it.unive.lisa.type.common.BoolType;
import it.unive.lisa.type.common.Float32;
import it.unive.lisa.type.common.Int32;
import it.unive.lisa.type.common.StringType;
import it.unive.lisa.util.collections.externalSet.ExternalSet;
import it.unive.lisa.util.collections.externalSet.ExternalSetCache;

public class InferredTypesTest {

	private static final String UNEXPECTED_BOTTOM = "Eval returned bottom for %s(%s)";
	private static final String WRONG_RESULT = "Wrong result for %s(%s)";
	private static final String RESULT_NOT_BOTTOM = "Result is not bottom for %s(%s)";

	private static final ExternalSetCache<Type> TYPES = Caches.types();

	private static final InferredTypes untyped = new InferredTypes(Untyped.INSTANCE);
	private static final InferredTypes bool = new InferredTypes(BoolType.INSTANCE);
	private static final InferredTypes string = new InferredTypes(StringType.INSTANCE);
	private static final InferredTypes bool_or_string = new InferredTypes(
			TYPES.mkSet(List.of(BoolType.INSTANCE, StringType.INSTANCE)));
	private static final InferredTypes integer = new InferredTypes(Int32.INSTANCE);
	private static final InferredTypes floating = new InferredTypes(Float32.INSTANCE);
	private static final InferredTypes numeric;
	private static final InferredTypes all;

	private static final Map<String, InferredTypes> combos = new HashMap<>();

	static {
		ExternalSet<Type> nums = TYPES.mkSingletonSet(Int32.INSTANCE);
		nums.add(Float32.INSTANCE);
		numeric = new InferredTypes(nums);
		ExternalSet<Type> full = nums.copy();
		full.add(StringType.INSTANCE);
		full.add(BoolType.INSTANCE);
		all = new InferredTypes(full);

		combos.put("bool", bool);
		combos.put("string", string);
		combos.put("(bool,string)", bool_or_string);
		combos.put("int", integer);
		combos.put("float", floating);
		combos.put("(int,float)", numeric);
		combos.put("(bool,string,int,float)", all);
	}

	private final InferredTypes domain = new InferredTypes();

	private final ProgramPoint fake = new ProgramPoint() {

		@Override
		public CFG getCFG() {
			return null;
		}

		@Override
		public CodeLocation getLocation() {
			return null;
		}
	};

	@Test
	public void testCastWithNoTokens() {
		// cast(str, x) = emptyset if x does not contain type tokens
		ExternalSet<Type> str = string.getRuntimeTypes();
		ExternalSet<Type> cast = domain.cast(str, str, null);
		assertTrue("Casting where the second arg does not have tokens succeded", cast.isEmpty());
	}

	@Test
	public void testCastIncompatible() {
		// cast(str, int) = emptyset
		ExternalSet<Type> str = string.getRuntimeTypes();
		ExternalSet<Type> in = TYPES.mkSingletonSet(new TypeTokenType(integer.getRuntimeTypes()));
		ExternalSet<Type> cast = domain.cast(str, in, null);
		assertTrue("Casting a string into an integer succeded", cast.isEmpty());
	}

	@Test
	public void testCastSame() {
		// cast(str, str) = str
		ExternalSet<Type> str = string.getRuntimeTypes();
		ExternalSet<Type> tok = TYPES.mkSingletonSet(new TypeTokenType(str));
		ExternalSet<Type> cast = domain.cast(str, tok, null);
		assertEquals("Casting a string into a string failed", str, cast);
	}

	@Test
	public void testCastMultiTokens() {
		// cast(str, ((str), (int))) = str
		ExternalSet<Type> str = string.getRuntimeTypes();
		ExternalSet<Type> tok = TYPES.mkSingletonSet(new TypeTokenType(str));
		tok.add(new TypeTokenType(integer.getRuntimeTypes()));
		ExternalSet<Type> cast = domain.cast(str, tok, null);
		assertEquals("Casting a string into a string failed", str, cast);
	}

	@Test
	public void testCastTokenWithMultiTypes() {
		// cast(str, (str, int)) = str
		ExternalSet<Type> str = string.getRuntimeTypes();
		ExternalSet<Type> tt = str.copy();
		tt.addAll(integer.getRuntimeTypes());
		ExternalSet<Type> tok = TYPES.mkSingletonSet(new TypeTokenType(tt));
		ExternalSet<Type> cast = domain.cast(str, tok, null);
		assertEquals("Casting a string into a string failed", str, cast);
	}

	@Test
	public void testCastMultiTypes() {
		// cast((str, int), str) = str
		ExternalSet<Type> str = string.getRuntimeTypes();
		ExternalSet<Type> tt = str.copy();
		tt.addAll(integer.getRuntimeTypes());
		ExternalSet<Type> tok = TYPES.mkSingletonSet(new TypeTokenType(str));
		ExternalSet<Type> cast = domain.cast(tt, tok, null);
		assertEquals("Casting a string into a string failed", str, cast);
	}

	@Test
	public void testCommonNumericalTypeIncompatible() {
		ExternalSet<Type> str = string.getRuntimeTypes();
		ExternalSet<Type> in = integer.getRuntimeTypes();
		ExternalSet<Type> common = domain.commonNumericalType(str, in);
		assertTrue("Common numerical type between a string and an integer exists", common.isEmpty());
	}

	@Test
	public void testCommonNumericalTypeSame() {
		ExternalSet<Type> in = integer.getRuntimeTypes();
		ExternalSet<Type> common = domain.commonNumericalType(in, in);
		assertEquals("Common numerical type between an integer and an integer does not exist", in, common);
	}

	@Test
	public void testCommonNumericalType() {
		ExternalSet<Type> in = integer.getRuntimeTypes();
		ExternalSet<Type> fl = floating.getRuntimeTypes();
		ExternalSet<Type> common = domain.commonNumericalType(in, fl);
		assertEquals("Common numerical type between an integer and a float is not a float", fl, common);
	}

	@Test
	public void testCommonNumericalTypeWithUntyped() {
		ExternalSet<Type> un = untyped.getRuntimeTypes();
		ExternalSet<Type> fl = floating.getRuntimeTypes();
		ExternalSet<Type> union = un.union(fl);
		ExternalSet<Type> common = domain.commonNumericalType(un, fl);
		assertEquals("Common numerical type between an untyped and a float is not a float", fl, common);
		common = domain.commonNumericalType(fl, un);
		assertEquals("Common numerical type between a float and un untyped is not a float", fl, common);
		common = domain.commonNumericalType(un, un);
		assertEquals("Common numerical type between two untyped is not empty", TYPES.mkEmptySet(), common);
		common = domain.commonNumericalType(fl, union);
		assertEquals("Common numerical type between a float and an (untyped,float) is not float", fl, common);
		common = domain.commonNumericalType(union, fl);
		assertEquals("Common numerical type between an (untyped,float) and a float is not float", fl, common);
		common = domain.commonNumericalType(union, union);
		assertEquals("Common numerical type between two (untyped,float) is not (untyped,float)", union, common);
	}

	private void unaryLE(UnaryOperator op, InferredTypes expected, InferredTypes operand) throws SemanticException {
		for (Entry<String, InferredTypes> first : combos.entrySet()) {
			InferredPair<InferredTypes> eval = domain.evalUnaryExpression(op, first.getValue(), domain.bottom(), fake);
			if (operand.lessOrEqual(first.getValue())) {
				assertFalse(String.format(UNEXPECTED_BOTTOM, op.name(), first.getKey()), eval.isBottom());
				assertEquals(String.format(WRONG_RESULT, op.name(), first.getKey()), expected, eval.getInferred());
			} else
				assertTrue(String.format(RESULT_NOT_BOTTOM, op.name(), first.getKey()), eval.isBottom());
		}
	}

	private void unaryMapping(UnaryOperator op, Map<InferredTypes, InferredTypes> expected) throws SemanticException {
		for (Entry<String, InferredTypes> first : combos.entrySet()) {
			InferredPair<InferredTypes> eval = domain.evalUnaryExpression(op, first.getValue(), domain.bottom(), fake);
			if (expected.containsKey(first.getValue())) {
				assertFalse(String.format(UNEXPECTED_BOTTOM, op.name(), first.getKey()), eval.isBottom());
				assertEquals(String.format(WRONG_RESULT, op.name(), first.getKey()), expected.get(first.getValue()),
						eval.getInferred());
			} else
				assertTrue(String.format(RESULT_NOT_BOTTOM, op.name(), first.getKey()), eval.isBottom());
		}
	}

	@Test
	public void testEvalUnary() throws SemanticException {
		unaryLE(UnaryOperator.LOGICAL_NOT, bool, bool);
		unaryLE(UnaryOperator.STRING_LENGTH, integer, string);

		unaryMapping(UnaryOperator.NUMERIC_NEG,
				Map.of(integer, integer, floating, floating, numeric, numeric, all, numeric));
		unaryMapping(UnaryOperator.TYPEOF, Map.of(bool, new InferredTypes(new TypeTokenType(bool.getRuntimeTypes())),
				string, new InferredTypes(new TypeTokenType(string.getRuntimeTypes())),
				integer, new InferredTypes(new TypeTokenType(integer.getRuntimeTypes())),
				floating, new InferredTypes(new TypeTokenType(floating.getRuntimeTypes())),
				numeric, new InferredTypes(new TypeTokenType(numeric.getRuntimeTypes())),
				all, new InferredTypes(new TypeTokenType(all.getRuntimeTypes())),
				bool_or_string, new InferredTypes(new TypeTokenType(bool_or_string.getRuntimeTypes()))));
	}

	private void binaryLE(BinaryOperator op, InferredTypes expected, InferredTypes left, InferredTypes right)
			throws SemanticException {
		for (Entry<String, InferredTypes> first : combos.entrySet())
			for (Entry<String, InferredTypes> second : combos.entrySet()) {
				InferredPair<InferredTypes> eval = domain.evalBinaryExpression(op, first.getValue(), second.getValue(),
						domain.bottom(), fake);
				if (left.lessOrEqual(first.getValue()) && right.lessOrEqual(second.getValue())) {
					assertFalse(String.format(UNEXPECTED_BOTTOM, op.name(), first.getKey() + "," + second.getKey()),
							eval.isBottom());
					assertEquals(String.format(WRONG_RESULT, op.name(), first.getKey() + "," + second.getKey()),
							expected, eval.getInferred());
				} else
					assertTrue(String.format(RESULT_NOT_BOTTOM, op.name(), first.getKey() + "," + second.getKey()),
							eval.isBottom());
			}
	}

	private void binaryFixed(BinaryOperator op, InferredTypes expected,
			List<Pair<InferredTypes, InferredTypes>> exclusions)
			throws SemanticException {
		for (Entry<String, InferredTypes> first : combos.entrySet())
			for (Entry<String, InferredTypes> second : combos.entrySet()) {
				InferredPair<InferredTypes> eval = domain.evalBinaryExpression(op, first.getValue(), second.getValue(),
						domain.bottom(), fake);
				if (notExcluded(exclusions, first, second)) {
					assertFalse(String.format(UNEXPECTED_BOTTOM, op.name(), first.getKey() + "," + second.getKey()),
							eval.isBottom());
					assertEquals(String.format(WRONG_RESULT, op.name(), first.getKey() + "," + second.getKey()),
							expected, eval.getInferred());
				} else
					assertTrue(String.format(RESULT_NOT_BOTTOM, op.name(), first.getKey() + "," + second.getKey()),
							eval.isBottom());
			}
	}

	private boolean notExcluded(List<Pair<InferredTypes, InferredTypes>> exclusions, Entry<String, InferredTypes> first,
			Entry<String, InferredTypes> second) {
		return !(exclusions.stream().anyMatch(p -> p.getLeft() == first.getValue() && p.getRight() == second.getValue())
				|| exclusions.stream().anyMatch(p -> p.getLeft() == first.getValue() && p.getRight() == null)
				|| exclusions.stream().anyMatch(p -> p.getLeft() == null && p.getRight() == first.getValue())
				|| exclusions.stream().anyMatch(p -> p.getLeft() == second.getValue() && p.getRight() == null)
				|| exclusions.stream().anyMatch(p -> p.getLeft() == null && p.getRight() == second.getValue()));
	}

	private void binaryTransform(BinaryOperator op, java.util.function.BinaryOperator<InferredTypes> expected,
			List<Pair<InferredTypes, InferredTypes>> exclusions)
			throws SemanticException {
		for (Entry<String, InferredTypes> first : combos.entrySet())
			for (Entry<String, InferredTypes> second : combos.entrySet()) {
				InferredPair<InferredTypes> eval = domain.evalBinaryExpression(op, first.getValue(), second.getValue(),
						domain.bottom(), fake);
				if (notExcluded(exclusions, first, second)) {
					assertFalse(String.format(UNEXPECTED_BOTTOM, op.name(), first.getKey() + "," + second.getKey()),
							eval.isBottom());
					assertEquals(String.format(WRONG_RESULT, op.name(), first.getKey() + "," + second.getKey()),
							expected.apply(first.getValue(), second.getValue()), eval.getInferred());
				} else
					assertTrue(String.format(RESULT_NOT_BOTTOM, op.name(), first.getKey() + "," + second.getKey()),
							eval.isBottom());
			}
	}

	private void binaryTransformSecond(BinaryOperator op, java.util.function.BinaryOperator<InferredTypes> expected,
			java.util.function.UnaryOperator<InferredTypes> transformer)
			throws SemanticException {
		for (Entry<String, InferredTypes> first : combos.entrySet())
			for (Entry<String, InferredTypes> second : combos.entrySet()) {
				InferredTypes st = transformer.apply(second.getValue());
				InferredPair<InferredTypes> eval = domain.evalBinaryExpression(op, first.getValue(), second.getValue(),
						domain.bottom(), fake);
				InferredPair<InferredTypes> evalT = domain.evalBinaryExpression(op, first.getValue(), st,
						domain.bottom(), fake);
				assertTrue(String.format(RESULT_NOT_BOTTOM, op.name(), first.getKey() + "," + second.getKey()),
						eval.isBottom());
				// we don't check for bottom: it might be the right result...
				assertEquals(
						String.format(WRONG_RESULT, op.name(),
								first.getKey() + "," + second.getKey() + "[transformed to " + st + "]"),
						expected.apply(first.getValue(), st), evalT.getInferred());
			}
	}

	@Test
	public void testEvalBinary() throws SemanticException {
		binaryFixed(BinaryOperator.COMPARISON_EQ, bool, Collections.emptyList());
		binaryFixed(BinaryOperator.COMPARISON_NE, bool, Collections.emptyList());
		List<Pair<InferredTypes, InferredTypes>> excluded = List.of(Pair.of(bool, null), Pair.of(null, bool),
				Pair.of(string, null), Pair.of(null, string), Pair.of(bool_or_string, null),
				Pair.of(null, bool_or_string));
		binaryFixed(BinaryOperator.COMPARISON_GE, bool, excluded);
		binaryFixed(BinaryOperator.COMPARISON_GT, bool, excluded);
		binaryFixed(BinaryOperator.COMPARISON_LE, bool, excluded);
		binaryFixed(BinaryOperator.COMPARISON_LT, bool, excluded);

		binaryLE(BinaryOperator.LOGICAL_AND, bool, bool, bool);
		binaryLE(BinaryOperator.LOGICAL_OR, bool, bool, bool);

		binaryLE(BinaryOperator.STRING_CONTAINS, bool, string, string);
		binaryLE(BinaryOperator.STRING_ENDS_WITH, bool, string, string);
		binaryLE(BinaryOperator.STRING_EQUALS, bool, string, string);
		binaryLE(BinaryOperator.STRING_STARTS_WITH, bool, string, string);
		binaryLE(BinaryOperator.STRING_INDEX_OF, integer, string, string);
		binaryLE(BinaryOperator.STRING_CONCAT, string, string, string);

		java.util.function.BinaryOperator<InferredTypes> commonNumbers = (l, r) -> {
			ExternalSet<Type> set = domain.commonNumericalType(l.getRuntimeTypes(), r.getRuntimeTypes());
			if (set.isEmpty())
				return domain.bottom();
			return new InferredTypes(set);
		};
		binaryTransform(BinaryOperator.NUMERIC_ADD, commonNumbers, excluded);
		binaryTransform(BinaryOperator.NUMERIC_DIV, commonNumbers, excluded);
		binaryTransform(BinaryOperator.NUMERIC_MUL, commonNumbers, excluded);
		binaryTransform(BinaryOperator.NUMERIC_SUB, commonNumbers, excluded);
		binaryTransform(BinaryOperator.NUMERIC_MOD, commonNumbers, excluded);

		binaryTransformSecond(BinaryOperator.TYPE_CAST, (l, r) -> {
			ExternalSet<Type> set = domain.cast(l.getRuntimeTypes(), r.getRuntimeTypes(), null);
			if (set.isEmpty())
				return domain.bottom();
			return new InferredTypes(set);
		}, it -> new InferredTypes(new TypeTokenType(it.getRuntimeTypes())));

		binaryTransformSecond(BinaryOperator.TYPE_CONV, (l, r) -> {
			ExternalSet<Type> set = domain.convert(l.getRuntimeTypes(), r.getRuntimeTypes());
			if (set.isEmpty())
				return domain.bottom();
			return new InferredTypes(set);
		}, it -> new InferredTypes(new TypeTokenType(it.getRuntimeTypes())));

		binaryTransformSecond(BinaryOperator.TYPE_CHECK, (l, r) -> bool,
				it -> new InferredTypes(new TypeTokenType(it.getRuntimeTypes())));
	}

	private void ternaryLE(TernaryOperator op, InferredTypes expected, InferredTypes left, InferredTypes middle,
			InferredTypes right) throws SemanticException {
		for (Entry<String, InferredTypes> first : combos.entrySet())
			for (Entry<String, InferredTypes> second : combos.entrySet())
				for (Entry<String, InferredTypes> third : combos.entrySet()) {
					InferredPair<InferredTypes> eval = domain.evalTernaryExpression(op, first.getValue(),
							second.getValue(), third.getValue(), domain.bottom(), fake);
					if (left.lessOrEqual(first.getValue()) && middle.lessOrEqual(second.getValue())
							&& right.lessOrEqual(third.getValue())) {
						assertFalse(
								String.format(UNEXPECTED_BOTTOM, op.name(),
										first.getKey() + "," + second.getKey() + "," + third.getKey()),
								eval.isBottom());
						assertEquals(String.format(WRONG_RESULT, op.name(),
								first.getKey() + "," + second.getKey() + "," + third.getKey()), expected,
								eval.getInferred());
					} else
						assertTrue(
								String.format(RESULT_NOT_BOTTOM, op.name(),
										first.getKey() + "," + second.getKey() + "," + third.getKey()),
								eval.isBottom());
				}
	}

	@Test
	public void testTernary() throws SemanticException {
		ternaryLE(TernaryOperator.STRING_SUBSTRING, string, string, integer, integer);
		ternaryLE(TernaryOperator.STRING_REPLACE, string, string, string, string);
	}

	private void satisfies(BinaryOperator op, InferredTypes left, InferredTypes right, Satisfiability expected) {
		assertEquals("Satisfies(" + left + " " + op + " " + right + ") returned wrong result", expected,
				domain.satisfiesBinaryExpression(op, left, right, domain.bottom(), fake));
	}

	@Test
	public void testSatisfies() {
		InferredTypes left = new InferredTypes(new TypeTokenType(TYPES.mkSingletonSet(Int32.INSTANCE)));
		satisfies(BinaryOperator.COMPARISON_EQ, left, left, Satisfiability.SATISFIED);
		satisfies(BinaryOperator.COMPARISON_NE, left, left, Satisfiability.NOT_SATISFIED);
		satisfies(BinaryOperator.TYPE_CHECK, integer, left, Satisfiability.SATISFIED);
		satisfies(BinaryOperator.TYPE_CHECK, floating, left, Satisfiability.SATISFIED);
		satisfies(BinaryOperator.TYPE_CHECK, string, left, Satisfiability.NOT_SATISFIED);
		satisfies(BinaryOperator.TYPE_CHECK, bool, left, Satisfiability.NOT_SATISFIED);
		satisfies(BinaryOperator.TYPE_CHECK, bool_or_string, left, Satisfiability.NOT_SATISFIED);

		InferredTypes right = new InferredTypes(new TypeTokenType(TYPES.mkSingletonSet(StringType.INSTANCE)));
		satisfies(BinaryOperator.COMPARISON_EQ, left, right, Satisfiability.NOT_SATISFIED);
		satisfies(BinaryOperator.COMPARISON_NE, left, right, Satisfiability.SATISFIED);
		satisfies(BinaryOperator.TYPE_CHECK, integer, right, Satisfiability.NOT_SATISFIED);
		satisfies(BinaryOperator.TYPE_CHECK, floating, right, Satisfiability.NOT_SATISFIED);
		satisfies(BinaryOperator.TYPE_CHECK, string, right, Satisfiability.SATISFIED);
		satisfies(BinaryOperator.TYPE_CHECK, bool, right, Satisfiability.NOT_SATISFIED);
		satisfies(BinaryOperator.TYPE_CHECK, bool_or_string, right, Satisfiability.UNKNOWN);

		right = new InferredTypes(new TypeTokenType(TYPES.mkSet(List.of(Int32.INSTANCE, StringType.INSTANCE))));
		satisfies(BinaryOperator.COMPARISON_EQ, left, right, Satisfiability.UNKNOWN);
		satisfies(BinaryOperator.COMPARISON_NE, left, right, Satisfiability.UNKNOWN);
		satisfies(BinaryOperator.TYPE_CHECK, integer, right, Satisfiability.UNKNOWN);
		satisfies(BinaryOperator.TYPE_CHECK, floating, right, Satisfiability.UNKNOWN);
		satisfies(BinaryOperator.TYPE_CHECK, string, right, Satisfiability.UNKNOWN);
		satisfies(BinaryOperator.TYPE_CHECK, bool, right, Satisfiability.NOT_SATISFIED);
		satisfies(BinaryOperator.TYPE_CHECK, bool_or_string, right, Satisfiability.UNKNOWN);

		right = new InferredTypes(TYPES.mkSet(List.of(new TypeTokenType(TYPES.mkSingletonSet(Int32.INSTANCE)),
				new TypeTokenType(TYPES.mkSingletonSet(StringType.INSTANCE)))));
		satisfies(BinaryOperator.COMPARISON_EQ, left, right, Satisfiability.UNKNOWN);
		satisfies(BinaryOperator.COMPARISON_NE, left, right, Satisfiability.UNKNOWN);
		satisfies(BinaryOperator.TYPE_CHECK, integer, right, Satisfiability.UNKNOWN);
		satisfies(BinaryOperator.TYPE_CHECK, floating, right, Satisfiability.UNKNOWN);
		satisfies(BinaryOperator.TYPE_CHECK, string, right, Satisfiability.UNKNOWN);
		satisfies(BinaryOperator.TYPE_CHECK, bool, right, Satisfiability.NOT_SATISFIED);
		satisfies(BinaryOperator.TYPE_CHECK, bool_or_string, right, Satisfiability.UNKNOWN);
	}
}
