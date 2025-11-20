package it.unive.lisa.lattices.types;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import it.unive.lisa.TestParameterProvider;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.lattices.Satisfiability;
import it.unive.lisa.analysis.nonrelational.type.TypeEnvironment;
import it.unive.lisa.analysis.types.InferredTypes;
import it.unive.lisa.imp.types.IMPTypeSystem;
import it.unive.lisa.program.SyntheticLocation;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.program.type.BoolType;
import it.unive.lisa.program.type.Float32Type;
import it.unive.lisa.program.type.Int32Type;
import it.unive.lisa.program.type.StringType;
import it.unive.lisa.symbolic.heap.NullConstant;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.TernaryExpression;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonEq;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGe;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGt;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLe;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLt;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonNe;
import it.unive.lisa.symbolic.value.operator.binary.LogicalAnd;
import it.unive.lisa.symbolic.value.operator.binary.LogicalOr;
import it.unive.lisa.symbolic.value.operator.binary.NumericNonOverflowingAdd;
import it.unive.lisa.symbolic.value.operator.binary.NumericNonOverflowingDiv;
import it.unive.lisa.symbolic.value.operator.binary.NumericNonOverflowingMod;
import it.unive.lisa.symbolic.value.operator.binary.NumericNonOverflowingMul;
import it.unive.lisa.symbolic.value.operator.binary.NumericNonOverflowingSub;
import it.unive.lisa.symbolic.value.operator.binary.StringConcat;
import it.unive.lisa.symbolic.value.operator.binary.StringContains;
import it.unive.lisa.symbolic.value.operator.binary.StringEndsWith;
import it.unive.lisa.symbolic.value.operator.binary.StringEquals;
import it.unive.lisa.symbolic.value.operator.binary.StringIndexOf;
import it.unive.lisa.symbolic.value.operator.binary.StringStartsWith;
import it.unive.lisa.symbolic.value.operator.binary.TypeCast;
import it.unive.lisa.symbolic.value.operator.binary.TypeCheck;
import it.unive.lisa.symbolic.value.operator.binary.TypeConv;
import it.unive.lisa.symbolic.value.operator.ternary.StringReplace;
import it.unive.lisa.symbolic.value.operator.ternary.StringSubstring;
import it.unive.lisa.symbolic.value.operator.ternary.TernaryOperator;
import it.unive.lisa.symbolic.value.operator.unary.LogicalNegation;
import it.unive.lisa.symbolic.value.operator.unary.NumericNegation;
import it.unive.lisa.symbolic.value.operator.unary.StringLength;
import it.unive.lisa.symbolic.value.operator.unary.TypeOf;
import it.unive.lisa.symbolic.value.operator.unary.UnaryOperator;
import it.unive.lisa.type.NumericType;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;
import it.unive.lisa.type.TypeTokenType;
import it.unive.lisa.type.Untyped;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.apache.commons.collections4.SetUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;

public class TypeSetTest {

	private static final String UNEXPECTED_BOTTOM = "Eval returned bottom for %s(%s)";

	private static final String WRONG_RESULT = "Wrong result for %s(%s)";

	private static final String RESULT_NOT_BOTTOM = "Result is not bottom for %s(%s)";

	private static final TypeSystem types = new IMPTypeSystem();

	private static final TypeSet untyped = new TypeSet(types, Untyped.INSTANCE);

	private static final TypeSet bool = new TypeSet(types, BoolType.INSTANCE);

	private static final TypeSet string = new TypeSet(types, StringType.INSTANCE);

	private static final TypeSet bool_or_string = new TypeSet(types, Set.of(BoolType.INSTANCE, StringType.INSTANCE));

	private static final TypeSet integer = new TypeSet(types, Int32Type.INSTANCE);

	private static final TypeSet floating = new TypeSet(types, Float32Type.INSTANCE);

	private static final TypeSet numeric;

	private static final TypeSet all;

	private static final Map<String, TypeSet> combos = new HashMap<>();

	static {
		Set<Type> nums = new HashSet<>();
		nums.add(Int32Type.INSTANCE);
		nums.add(Float32Type.INSTANCE);
		numeric = new TypeSet(types, nums);
		Set<Type> full = new HashSet<>(nums);
		full.add(StringType.INSTANCE);
		full.add(BoolType.INSTANCE);
		all = new TypeSet(types, full);

		combos.put("bool", bool);
		combos.put("string", string);
		combos.put("(bool,string)", bool_or_string);
		combos.put("int", integer);
		combos.put("float", floating);
		combos.put("(int,float)", numeric);
		combos.put("(bool,string,int,float)", all);
	}

	private final InferredTypes domain = new InferredTypes();

	private final ProgramPoint fake = TestParameterProvider.provideParam(null, ProgramPoint.class);

	private final SemanticOracle oracle = TestParameterProvider.provideParam(null, SemanticOracle.class);

	@Test
	public void testCastWithNoTokens() {
		// cast(str, x) = emptyset if x does not contain type tokens
		Set<Type> str = string.getRuntimeTypes();
		Set<Type> cast = types.cast(str, str, null);
		assertTrue("Casting where the second arg does not have tokens succeded", cast.isEmpty());
	}

	@Test
	public void testCastIncompatible() {
		// cast(str, int) = emptyset
		Set<Type> str = string.getRuntimeTypes();
		Set<Type> in = Collections.singleton(new TypeTokenType(integer.getRuntimeTypes()));
		Set<Type> cast = types.cast(str, in, null);
		assertTrue("Casting a string into an integer succeded", cast.isEmpty());
	}

	@Test
	public void testCastSame() {
		// cast(str, str) = str
		Set<Type> str = string.getRuntimeTypes();
		Set<Type> tok = Collections.singleton(new TypeTokenType(str));
		Set<Type> cast = types.cast(str, tok, null);
		assertEquals("Casting a string into a string failed", str, cast);
	}

	@Test
	public void testCastMultiTokens() {
		// cast(str, ((str), (int))) = str
		Set<Type> str = string.getRuntimeTypes();
		Set<Type> tok = new HashSet<>();
		tok.add(new TypeTokenType(str));
		tok.add(new TypeTokenType(integer.getRuntimeTypes()));
		Set<Type> cast = types.cast(str, tok, null);
		assertEquals("Casting a string into a string failed", str, cast);
	}

	@Test
	public void testCastTokenWithMultiTypes() {
		// cast(str, (str, int)) = str
		Set<Type> str = string.getRuntimeTypes();
		Set<Type> tt = new HashSet<>(str);
		tt.addAll(integer.getRuntimeTypes());
		Set<Type> tok = Collections.singleton(new TypeTokenType(tt));
		Set<Type> cast = types.cast(str, tok, null);
		assertEquals("Casting a string into a string failed", str, cast);
	}

	@Test
	public void testCastMultiTypes() {
		// cast((str, int), str) = str
		Set<Type> str = string.getRuntimeTypes();
		Set<Type> tt = new HashSet<>(str);
		tt.addAll(integer.getRuntimeTypes());
		Set<Type> tok = Collections.singleton(new TypeTokenType(str));
		Set<Type> cast = types.cast(tt, tok, null);
		assertEquals("Casting a string into a string failed", str, cast);
	}

	@Test
	public void testCommonNumericalTypeIncompatible() {
		Set<Type> str = string.getRuntimeTypes();
		Set<Type> in = integer.getRuntimeTypes();
		Set<Type> common = NumericType.commonNumericalType(str, in);
		assertTrue("Common numerical type between a string and an integer exists", common.isEmpty());
	}

	@Test
	public void testCommonNumericalTypeSame() {
		Set<Type> in = integer.getRuntimeTypes();
		Set<Type> common = NumericType.commonNumericalType(in, in);
		assertEquals("Common numerical type between an integer and an integer does not exist", in, common);
	}

	@Test
	public void testCommonNumericalType() {
		Set<Type> in = integer.getRuntimeTypes();
		Set<Type> fl = floating.getRuntimeTypes();
		Set<Type> common = NumericType.commonNumericalType(in, fl);
		assertEquals("Common numerical type between an integer and a float is not a float", fl, common);
	}

	@Test
	public void testCommonNumericalTypeWithUntyped() {
		Set<Type> un = untyped.getRuntimeTypes();
		Set<Type> fl = floating.getRuntimeTypes();
		Set<Type> union = SetUtils.union(un, fl);
		Set<Type> common = NumericType.commonNumericalType(un, fl);
		assertEquals("Common numerical type between an untyped and a float is not a float", fl, common);
		common = NumericType.commonNumericalType(fl, un);
		assertEquals("Common numerical type between a float and un untyped is not a float", fl, common);
		common = NumericType.commonNumericalType(un, un);
		assertEquals("Common numerical type between two untyped is not empty", Collections.emptySet(), common);
		common = NumericType.commonNumericalType(fl, union);
		assertEquals("Common numerical type between a float and an (untyped,float) is not float", fl, common);
		common = NumericType.commonNumericalType(union, fl);
		assertEquals("Common numerical type between an (untyped,float) and a float is not float", fl, common);
		common = NumericType.commonNumericalType(union, union);
		assertEquals("Common numerical type between two (untyped,float) is not (untyped,float)", union, common);
	}

	private void unaryLE(
			UnaryOperator op,
			TypeSet expected,
			TypeSet operand)
			throws SemanticException {
		for (Entry<String, TypeSet> first : combos.entrySet()) {
			TypeSet eval = domain.evalUnaryExpression(
					new UnaryExpression(
							Untyped.INSTANCE,
							new NullConstant(SyntheticLocation.INSTANCE),
							op,
							SyntheticLocation.INSTANCE),
					first.getValue(),
					fake,
					oracle);
			if (operand.lessOrEqual(first.getValue())) {
				assertFalse(
						String.format(UNEXPECTED_BOTTOM, op.getClass().getSimpleName(), first.getKey()),
						eval.isBottom());
				assertEquals(
						String.format(WRONG_RESULT, op.getClass().getSimpleName(), first.getKey()),
						expected.getRuntimeTypes(),
						eval.getRuntimeTypes());
			} else
				assertTrue(
						String.format(RESULT_NOT_BOTTOM, op.getClass().getSimpleName(), first.getKey()),
						eval.isBottom());
		}
	}

	private void unaryMapping(
			UnaryOperator op,
			Map<TypeSet, TypeSet> expected)
			throws SemanticException {
		for (Entry<String, TypeSet> first : combos.entrySet()) {
			TypeSet eval = domain.evalUnaryExpression(
					new UnaryExpression(
							Untyped.INSTANCE,
							new NullConstant(SyntheticLocation.INSTANCE),
							op,
							SyntheticLocation.INSTANCE),
					first.getValue(),
					fake,
					oracle);
			if (expected.containsKey(first.getValue())) {
				assertFalse(
						String.format(UNEXPECTED_BOTTOM, op.getClass().getSimpleName(), first.getKey()),
						eval.isBottom());
				assertEquals(
						String.format(WRONG_RESULT, op.getClass().getSimpleName(), first.getKey()),
						expected.get(first.getValue()).getRuntimeTypes(),
						eval.getRuntimeTypes());
			} else
				assertTrue(
						String.format(RESULT_NOT_BOTTOM, op.getClass().getSimpleName(), first.getKey()),
						eval.isBottom());
		}
	}

	@Test
	public void testEvalUnary()
			throws SemanticException {
		unaryLE(LogicalNegation.INSTANCE, bool, bool);
		unaryLE(StringLength.INSTANCE, integer, string);

		unaryMapping(
				NumericNegation.INSTANCE,
				Map.of(integer, integer, floating, floating, numeric, numeric, all, numeric));
		unaryMapping(
				TypeOf.INSTANCE,
				Map.of(
						bool,
						new TypeSet(types, new TypeTokenType(bool.getRuntimeTypes())),
						string,
						new TypeSet(types, new TypeTokenType(string.getRuntimeTypes())),
						integer,
						new TypeSet(types, new TypeTokenType(integer.getRuntimeTypes())),
						floating,
						new TypeSet(types, new TypeTokenType(floating.getRuntimeTypes())),
						numeric,
						new TypeSet(types, new TypeTokenType(numeric.getRuntimeTypes())),
						all,
						new TypeSet(types, new TypeTokenType(all.getRuntimeTypes())),
						bool_or_string,
						new TypeSet(types, new TypeTokenType(bool_or_string.getRuntimeTypes()))));
	}

	private void binaryLE(
			BinaryOperator op,
			TypeSet expected,
			TypeSet left,
			TypeSet right)
			throws SemanticException {
		for (Entry<String, TypeSet> first : combos.entrySet())
			for (Entry<String, TypeSet> second : combos.entrySet()) {
				TypeSet eval = domain.evalBinaryExpression(
						new BinaryExpression(
								Untyped.INSTANCE,
								new NullConstant(SyntheticLocation.INSTANCE),
								new NullConstant(SyntheticLocation.INSTANCE),
								op,
								SyntheticLocation.INSTANCE),
						first.getValue(),
						second.getValue(),
						fake,
						oracle);
				if (left.lessOrEqual(first.getValue()) && right.lessOrEqual(second.getValue())) {
					assertFalse(
							String.format(
									UNEXPECTED_BOTTOM,
									op.getClass().getSimpleName(),
									first.getKey() + "," + second.getKey()),
							eval.isBottom());
					assertEquals(
							String.format(
									WRONG_RESULT,
									op.getClass().getSimpleName(),
									first.getKey() + "," + second.getKey()),
							expected.getRuntimeTypes(),
							eval.getRuntimeTypes());
				} else
					assertTrue(
							String.format(
									RESULT_NOT_BOTTOM,
									op.getClass().getSimpleName(),
									first.getKey() + "," + second.getKey()),
							eval.isBottom());
			}
	}

	private void binaryFixed(
			BinaryOperator op,
			TypeSet expected,
			List<Pair<TypeSet, TypeSet>> exclusions)
			throws SemanticException {
		for (Entry<String, TypeSet> first : combos.entrySet())
			for (Entry<String, TypeSet> second : combos.entrySet()) {
				TypeSet eval = domain.evalBinaryExpression(
						new BinaryExpression(
								Untyped.INSTANCE,
								new NullConstant(SyntheticLocation.INSTANCE),
								new NullConstant(SyntheticLocation.INSTANCE),
								op,
								SyntheticLocation.INSTANCE),
						first.getValue(),
						second.getValue(),
						fake,
						oracle);
				if (notExcluded(exclusions, first, second)) {
					assertFalse(
							String.format(
									UNEXPECTED_BOTTOM,
									op.getClass().getSimpleName(),
									first.getKey() + "," + second.getKey()),
							eval.isBottom());
					assertEquals(
							String.format(
									WRONG_RESULT,
									op.getClass().getSimpleName(),
									first.getKey() + "," + second.getKey()),
							expected.getRuntimeTypes(),
							eval.getRuntimeTypes());
				} else
					assertTrue(
							String.format(
									RESULT_NOT_BOTTOM,
									op.getClass().getSimpleName(),
									first.getKey() + "," + second.getKey()),
							eval.isBottom());
			}
	}

	private boolean notExcluded(
			List<Pair<TypeSet, TypeSet>> exclusions,
			Entry<String, TypeSet> first,
			Entry<String, TypeSet> second) {
		return !(exclusions.stream().anyMatch(p -> p.getLeft() == first.getValue() && p.getRight() == second.getValue())
				|| exclusions.stream().anyMatch(p -> p.getLeft() == first.getValue() && p.getRight() == null)
				|| exclusions.stream().anyMatch(p -> p.getLeft() == null && p.getRight() == first.getValue())
				|| exclusions.stream().anyMatch(p -> p.getLeft() == second.getValue() && p.getRight() == null)
				|| exclusions.stream().anyMatch(p -> p.getLeft() == null && p.getRight() == second.getValue()));
	}

	private void binaryTransform(
			BinaryOperator op,
			java.util.function.BinaryOperator<TypeSet> expected,
			List<Pair<TypeSet, TypeSet>> exclusions)
			throws SemanticException {
		for (Entry<String, TypeSet> first : combos.entrySet())
			for (Entry<String, TypeSet> second : combos.entrySet()) {
				TypeSet eval = domain.evalBinaryExpression(
						new BinaryExpression(
								Untyped.INSTANCE,
								new NullConstant(SyntheticLocation.INSTANCE),
								new NullConstant(SyntheticLocation.INSTANCE),
								op,
								SyntheticLocation.INSTANCE),
						first.getValue(),
						second.getValue(),
						fake,
						oracle);
				if (notExcluded(exclusions, first, second)) {
					assertFalse(
							String.format(
									UNEXPECTED_BOTTOM,
									op.getClass().getSimpleName(),
									first.getKey() + "," + second.getKey()),
							eval.isBottom());
					assertEquals(
							String.format(
									WRONG_RESULT,
									op.getClass().getSimpleName(),
									first.getKey() + "," + second.getKey()),
							expected.apply(first.getValue(), second.getValue()).getRuntimeTypes(),
							eval.getRuntimeTypes());
				} else
					assertTrue(
							String.format(
									RESULT_NOT_BOTTOM,
									op.getClass().getSimpleName(),
									first.getKey() + "," + second.getKey()),
							eval.isBottom());
			}
	}

	private void binaryTransformSecond(
			BinaryOperator op,
			java.util.function.BinaryOperator<TypeSet> expected,
			java.util.function.UnaryOperator<TypeSet> transformer)
			throws SemanticException {
		for (Entry<String, TypeSet> first : combos.entrySet())
			for (Entry<String, TypeSet> second : combos.entrySet()) {
				TypeSet st = transformer.apply(second.getValue());
				TypeSet eval = domain.evalBinaryExpression(
						new BinaryExpression(
								Untyped.INSTANCE,
								new NullConstant(SyntheticLocation.INSTANCE),
								new NullConstant(SyntheticLocation.INSTANCE),
								op,
								SyntheticLocation.INSTANCE),
						first.getValue(),
						second.getValue(),
						fake,
						oracle);
				TypeSet evalT = domain.evalBinaryExpression(
						new BinaryExpression(
								Untyped.INSTANCE,
								new NullConstant(SyntheticLocation.INSTANCE),
								new NullConstant(SyntheticLocation.INSTANCE),
								op,
								SyntheticLocation.INSTANCE),
						first.getValue(),
						st,
						fake,
						oracle);
				assertTrue(
						String.format(
								RESULT_NOT_BOTTOM,
								op.getClass().getSimpleName(),
								first.getKey() + "," + second.getKey()),
						eval.isBottom());
				// we don't check for bottom: it might be the right result...
				assertEquals(
						String.format(
								WRONG_RESULT,
								op.getClass().getSimpleName(),
								first.getKey() + "," + second.getKey() + "[transformed to " + st + "]"),
						expected.apply(first.getValue(), st).getRuntimeTypes(),
						evalT.getRuntimeTypes());
			}
	}

	@Test
	public void testEvalBinary()
			throws SemanticException {
		binaryFixed(ComparisonEq.INSTANCE, bool, Collections.emptyList());
		binaryFixed(ComparisonNe.INSTANCE, bool, Collections.emptyList());
		List<Pair<TypeSet, TypeSet>> excluded = List.of(
				Pair.of(bool, null),
				Pair.of(null, bool),
				Pair.of(string, null),
				Pair.of(null, string),
				Pair.of(bool_or_string, null),
				Pair.of(null, bool_or_string));
		binaryFixed(ComparisonGe.INSTANCE, bool, excluded);
		binaryFixed(ComparisonGt.INSTANCE, bool, excluded);
		binaryFixed(ComparisonLe.INSTANCE, bool, excluded);
		binaryFixed(ComparisonLt.INSTANCE, bool, excluded);

		binaryLE(LogicalAnd.INSTANCE, bool, bool, bool);
		binaryLE(LogicalOr.INSTANCE, bool, bool, bool);

		binaryLE(StringContains.INSTANCE, bool, string, string);
		binaryLE(StringEndsWith.INSTANCE, bool, string, string);
		binaryLE(StringEquals.INSTANCE, bool, string, string);
		binaryLE(StringStartsWith.INSTANCE, bool, string, string);
		binaryLE(StringIndexOf.INSTANCE, integer, string, string);
		binaryLE(StringConcat.INSTANCE, string, string, string);

		java.util.function.BinaryOperator<TypeSet> commonNumbers = (
				l,
				r) -> {
			Set<Type> set = NumericType.commonNumericalType(l.getRuntimeTypes(), r.getRuntimeTypes());
			if (set.isEmpty())
				return domain.bottom();
			return new TypeSet(types, set);
		};
		binaryTransform(NumericNonOverflowingAdd.INSTANCE, commonNumbers, excluded);
		binaryTransform(NumericNonOverflowingDiv.INSTANCE, commonNumbers, excluded);
		binaryTransform(NumericNonOverflowingMul.INSTANCE, commonNumbers, excluded);
		binaryTransform(NumericNonOverflowingSub.INSTANCE, commonNumbers, excluded);
		binaryTransform(NumericNonOverflowingMod.INSTANCE, commonNumbers, excluded);

		binaryTransformSecond(
				TypeCast.INSTANCE,
				(
						l,
						r) -> {
					Set<Type> set = types.cast(l.getRuntimeTypes(), r.getRuntimeTypes(), null);
					if (set.isEmpty())
						return domain.bottom();
					return new TypeSet(types, set);
				},
				it -> new TypeSet(types, new TypeTokenType(it.getRuntimeTypes())));

		binaryTransformSecond(
				TypeConv.INSTANCE,
				(
						l,
						r) -> {
					Set<Type> set = types.convert(l.getRuntimeTypes(), r.getRuntimeTypes());
					if (set.isEmpty())
						return domain.bottom();
					return new TypeSet(types, set);
				},
				it -> new TypeSet(types, new TypeTokenType(it.getRuntimeTypes())));

		binaryTransformSecond(
				TypeCheck.INSTANCE,
				(
						l,
						r) -> bool,
				it -> new TypeSet(types, new TypeTokenType(it.getRuntimeTypes())));
	}

	private void ternaryLE(
			TernaryOperator op,
			TypeSet expected,
			TypeSet left,
			TypeSet middle,
			TypeSet right)
			throws SemanticException {
		for (Entry<String, TypeSet> first : combos.entrySet())
			for (Entry<String, TypeSet> second : combos.entrySet())
				for (Entry<String, TypeSet> third : combos.entrySet()) {
					TypeSet eval = domain.evalTernaryExpression(
							new TernaryExpression(
									Untyped.INSTANCE,
									new NullConstant(SyntheticLocation.INSTANCE),
									new NullConstant(SyntheticLocation.INSTANCE),
									new NullConstant(SyntheticLocation.INSTANCE),
									op,
									SyntheticLocation.INSTANCE),
							first.getValue(),
							second.getValue(),
							third.getValue(),
							fake,
							oracle);
					if (left.lessOrEqual(first.getValue())
							&& middle.lessOrEqual(second.getValue())
							&& right.lessOrEqual(third.getValue())) {
						assertFalse(
								String.format(
										UNEXPECTED_BOTTOM,
										op.getClass().getSimpleName(),
										first.getKey() + "," + second.getKey() + "," + third.getKey()),
								eval.isBottom());
						assertEquals(
								String.format(
										WRONG_RESULT,
										op.getClass().getSimpleName(),
										first.getKey() + "," + second.getKey() + "," + third.getKey()),
								expected.getRuntimeTypes(),
								eval.getRuntimeTypes());
					} else
						assertTrue(
								String.format(
										RESULT_NOT_BOTTOM,
										op.getClass().getSimpleName(),
										first.getKey() + "," + second.getKey() + "," + third.getKey()),
								eval.isBottom());
				}
	}

	@Test
	public void testTernary()
			throws SemanticException {
		ternaryLE(StringSubstring.INSTANCE, string, string, integer, integer);
		ternaryLE(StringReplace.INSTANCE, string, string, string, string);
	}

	private void satisfies(
			BinaryOperator op,
			TypeSet left,
			TypeSet right,
			Satisfiability expected) {
		assertEquals(
				"Satisfies(" + left + " " + op + " " + right + ") returned wrong result",
				expected,
				domain.satisfiesBinaryExpression(
						new BinaryExpression(
								Untyped.INSTANCE,
								new NullConstant(SyntheticLocation.INSTANCE),
								new NullConstant(SyntheticLocation.INSTANCE),
								op,
								SyntheticLocation.INSTANCE),
						left,
						right,
						fake,
						oracle));
	}

	@Test
	public void testSatisfies() {
		TypeSet left = new TypeSet(types, new TypeTokenType(Collections.singleton(Int32Type.INSTANCE)));
		satisfies(ComparisonEq.INSTANCE, left, left, Satisfiability.SATISFIED);
		satisfies(ComparisonNe.INSTANCE, left, left, Satisfiability.NOT_SATISFIED);
		satisfies(TypeCheck.INSTANCE, integer, left, Satisfiability.SATISFIED);
		satisfies(TypeCheck.INSTANCE, floating, left, Satisfiability.SATISFIED);
		satisfies(TypeCheck.INSTANCE, string, left, Satisfiability.NOT_SATISFIED);
		satisfies(TypeCheck.INSTANCE, bool, left, Satisfiability.NOT_SATISFIED);
		satisfies(TypeCheck.INSTANCE, bool_or_string, left, Satisfiability.NOT_SATISFIED);

		TypeSet right = new TypeSet(types, new TypeTokenType(Collections.singleton(StringType.INSTANCE)));
		satisfies(ComparisonEq.INSTANCE, left, right, Satisfiability.NOT_SATISFIED);
		satisfies(ComparisonNe.INSTANCE, left, right, Satisfiability.SATISFIED);
		satisfies(TypeCheck.INSTANCE, integer, right, Satisfiability.NOT_SATISFIED);
		satisfies(TypeCheck.INSTANCE, floating, right, Satisfiability.NOT_SATISFIED);
		satisfies(TypeCheck.INSTANCE, string, right, Satisfiability.SATISFIED);
		satisfies(TypeCheck.INSTANCE, bool, right, Satisfiability.NOT_SATISFIED);
		satisfies(TypeCheck.INSTANCE, bool_or_string, right, Satisfiability.UNKNOWN);

		right = new TypeSet(types, new TypeTokenType(Set.of(Int32Type.INSTANCE, StringType.INSTANCE)));
		satisfies(ComparisonEq.INSTANCE, left, right, Satisfiability.UNKNOWN);
		satisfies(ComparisonNe.INSTANCE, left, right, Satisfiability.UNKNOWN);
		satisfies(TypeCheck.INSTANCE, integer, right, Satisfiability.UNKNOWN);
		satisfies(TypeCheck.INSTANCE, floating, right, Satisfiability.UNKNOWN);
		satisfies(TypeCheck.INSTANCE, string, right, Satisfiability.UNKNOWN);
		satisfies(TypeCheck.INSTANCE, bool, right, Satisfiability.NOT_SATISFIED);
		satisfies(TypeCheck.INSTANCE, bool_or_string, right, Satisfiability.UNKNOWN);

		right = new TypeSet(
				types,
				Set.of(
						new TypeTokenType(Collections.singleton(Int32Type.INSTANCE)),
						new TypeTokenType(Collections.singleton(StringType.INSTANCE))));
		satisfies(ComparisonEq.INSTANCE, left, right, Satisfiability.UNKNOWN);
		satisfies(ComparisonNe.INSTANCE, left, right, Satisfiability.UNKNOWN);
		satisfies(TypeCheck.INSTANCE, integer, right, Satisfiability.UNKNOWN);
		satisfies(TypeCheck.INSTANCE, floating, right, Satisfiability.UNKNOWN);
		satisfies(TypeCheck.INSTANCE, string, right, Satisfiability.UNKNOWN);
		satisfies(TypeCheck.INSTANCE, bool, right, Satisfiability.NOT_SATISFIED);
		satisfies(TypeCheck.INSTANCE, bool_or_string, right, Satisfiability.UNKNOWN);
	}

	private final Variable variable = new Variable(Int32Type.INSTANCE, "x", fake.getLocation());

	private final TypeEnvironment<TypeSet> env = new TypeEnvironment<>(string).putState(variable, bool_or_string);

	private void assume(
			BinaryOperator op,
			ValueExpression expr,
			TypeSet expected)
			throws SemanticException {
		TypeEnvironment<TypeSet> act = domain.assumeBinaryExpression(
				env,
				new BinaryExpression(BoolType.INSTANCE, variable, expr, op, fake.getLocation()),
				fake,
				fake,
				oracle);

		assertEquals("Assume(x " + op + " " + expr + ") returned wrong result", expected, act.getState(variable));
	}

	@Test
	public void testAssume()
			throws SemanticException {
		assume(
				ComparisonEq.INSTANCE,
				new Constant(
						new TypeTokenType(Set.of(StringType.INSTANCE, BoolType.INSTANCE)),
						"bool or string",
						fake.getLocation()),
				bool_or_string);
		assume(
				ComparisonEq.INSTANCE,
				new Constant(
						new TypeTokenType(Collections.singleton(BoolType.INSTANCE)),
						BoolType.INSTANCE,
						fake.getLocation()),
				bool);
		assume(
				ComparisonEq.INSTANCE,
				new Constant(
						new TypeTokenType(Collections.singleton(Int32Type.INSTANCE)),
						Int32Type.INSTANCE,
						fake.getLocation()),
				domain.bottom());

		assume(
				ComparisonNe.INSTANCE,
				new Constant(
						new TypeTokenType(Set.of(StringType.INSTANCE, BoolType.INSTANCE)),
						"bool or string",
						fake.getLocation()),
				domain.bottom());
		assume(
				ComparisonNe.INSTANCE,
				new Constant(
						new TypeTokenType(Collections.singleton(BoolType.INSTANCE)),
						BoolType.INSTANCE,
						fake.getLocation()),
				string);
		assume(
				ComparisonNe.INSTANCE,
				new Constant(
						new TypeTokenType(Collections.singleton(Int32Type.INSTANCE)),
						Int32Type.INSTANCE,
						fake.getLocation()),
				bool_or_string);

		assume(
				TypeCheck.INSTANCE,
				new Constant(
						new TypeTokenType(Set.of(StringType.INSTANCE, BoolType.INSTANCE)),
						"bool or string",
						fake.getLocation()),
				bool_or_string);
		assume(
				TypeCheck.INSTANCE,
				new Constant(
						new TypeTokenType(Collections.singleton(BoolType.INSTANCE)),
						BoolType.INSTANCE,
						fake.getLocation()),
				bool);
		assume(
				TypeCheck.INSTANCE,
				new Constant(
						new TypeTokenType(Collections.singleton(Int32Type.INSTANCE)),
						Int32Type.INSTANCE,
						fake.getLocation()),
				domain.bottom());
	}

}
