package it.unive.lisa.triage.encoding;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.symbolic.ExpressionVisitor;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.heap.HeapDereference;
import it.unive.lisa.symbolic.heap.HeapExpression;
import it.unive.lisa.symbolic.heap.HeapReference;
import it.unive.lisa.symbolic.heap.MemoryAllocation;
import it.unive.lisa.symbolic.heap.NullConstant;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.PushAny;
import it.unive.lisa.symbolic.value.PushInv;
import it.unive.lisa.symbolic.value.Skip;
import it.unive.lisa.symbolic.value.TernaryExpression;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.symbolic.value.operator.AdditionOperator;
import it.unive.lisa.symbolic.value.operator.DivisionOperator;
import it.unive.lisa.symbolic.value.operator.ModuloOperator;
import it.unive.lisa.symbolic.value.operator.MultiplicationOperator;
import it.unive.lisa.symbolic.value.operator.RemainderOperator;
import it.unive.lisa.symbolic.value.operator.SubtractionOperator;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonEq;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGe;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGt;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLe;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLt;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonNe;
import it.unive.lisa.symbolic.value.operator.binary.LogicalAnd;
import it.unive.lisa.symbolic.value.operator.binary.LogicalOr;
import it.unive.lisa.symbolic.value.operator.binary.Numeric16BitAdd;
import it.unive.lisa.symbolic.value.operator.binary.Numeric16BitDiv;
import it.unive.lisa.symbolic.value.operator.binary.Numeric16BitMod;
import it.unive.lisa.symbolic.value.operator.binary.Numeric16BitMul;
import it.unive.lisa.symbolic.value.operator.binary.Numeric16BitRem;
import it.unive.lisa.symbolic.value.operator.binary.Numeric16BitSub;
import it.unive.lisa.symbolic.value.operator.binary.Numeric32BitAdd;
import it.unive.lisa.symbolic.value.operator.binary.Numeric32BitDiv;
import it.unive.lisa.symbolic.value.operator.binary.Numeric32BitMod;
import it.unive.lisa.symbolic.value.operator.binary.Numeric32BitMul;
import it.unive.lisa.symbolic.value.operator.binary.Numeric32BitRem;
import it.unive.lisa.symbolic.value.operator.binary.Numeric32BitSub;
import it.unive.lisa.symbolic.value.operator.binary.Numeric64BitAdd;
import it.unive.lisa.symbolic.value.operator.binary.Numeric64BitDiv;
import it.unive.lisa.symbolic.value.operator.binary.Numeric64BitMod;
import it.unive.lisa.symbolic.value.operator.binary.Numeric64BitMul;
import it.unive.lisa.symbolic.value.operator.binary.Numeric64BitRem;
import it.unive.lisa.symbolic.value.operator.binary.Numeric64BitSub;
import it.unive.lisa.symbolic.value.operator.binary.Numeric8BitAdd;
import it.unive.lisa.symbolic.value.operator.binary.Numeric8BitDiv;
import it.unive.lisa.symbolic.value.operator.binary.Numeric8BitMod;
import it.unive.lisa.symbolic.value.operator.binary.Numeric8BitMul;
import it.unive.lisa.symbolic.value.operator.binary.Numeric8BitRem;
import it.unive.lisa.symbolic.value.operator.binary.Numeric8BitSub;
import it.unive.lisa.symbolic.value.operator.binary.NumericMax;
import it.unive.lisa.symbolic.value.operator.binary.NumericMin;
import it.unive.lisa.symbolic.value.operator.binary.StringConcat;
import it.unive.lisa.symbolic.value.operator.binary.StringContains;
import it.unive.lisa.symbolic.value.operator.binary.StringEndsWith;
import it.unive.lisa.symbolic.value.operator.binary.StringEquals;
import it.unive.lisa.symbolic.value.operator.binary.StringIndexOf;
import it.unive.lisa.symbolic.value.operator.binary.StringIsPrefixOf;
import it.unive.lisa.symbolic.value.operator.binary.StringIsSuffixOf;
import it.unive.lisa.symbolic.value.operator.binary.StringStartsWith;
import it.unive.lisa.symbolic.value.operator.binary.StringSubstringToEnd;
import it.unive.lisa.symbolic.value.operator.binary.TypeCast;
import it.unive.lisa.symbolic.value.operator.binary.TypeConv;
import it.unive.lisa.symbolic.value.operator.ternary.StringIndexOfFromIndex;
import it.unive.lisa.symbolic.value.operator.ternary.StringReplace;
import it.unive.lisa.symbolic.value.operator.ternary.StringStartsWithFromIndex;
import it.unive.lisa.symbolic.value.operator.ternary.StringSubstring;
import it.unive.lisa.symbolic.value.operator.ternary.TernaryOperator;
import it.unive.lisa.symbolic.value.operator.unary.BitwiseNegation;
import it.unive.lisa.symbolic.value.operator.unary.LogicalNegation;
import it.unive.lisa.symbolic.value.operator.unary.NumericAbs;
import it.unive.lisa.symbolic.value.operator.unary.NumericNegation;
import it.unive.lisa.symbolic.value.operator.unary.StringLength;
import it.unive.lisa.symbolic.value.operator.unary.UnaryOperator;
import it.unive.lisa.triage.smt.Sort;
import it.unive.lisa.triage.smt.Term;
import it.unive.lisa.type.Type;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * An encoder of {@link SymbolicExpression}s into SMT {@link Term}s. The
 * encoding is designed to be <b>sound</b>, in the sense that the set of values
 * that the encoded term can take always includes the concrete value of the
 * expression: operations that cannot be encoded precisely yield fresh,
 * unconstrained constants (provided by the {@link SymbolTable}) instead of
 * approximated results. In particular:
 * <ul>
 * <li>integral values are encoded as mathematical integers: non-overflowing
 * operators are encoded directly, while overflowing ones (e.g.,
 * {@link Numeric32BitAdd}) wrap their result to the range of their bit width,
 * and integer divisions truncate their result toward zero;</li>
 * <li>booleans and strings are encoded with the corresponding SMT sorts, with
 * the most common string operations encoded through the SMT-LIB strings
 * theory;</li>
 * <li>values of other types, and values whose type is unknown or not unique,
 * are encoded through the {@link ValueSort} datatype, where reading a value as
 * an integer, boolean, or string yields an unspecified value if the value is
 * of a different kind;</li>
 * <li>floating point arithmetic, bitwise operators (except negation),
 * regular-expression based and case-dependent string operations, character
 * operations, type checks, and heap expressions yield fresh constants.</li>
 * </ul>
 * The sort of identifiers and of unknown values is decided by a
 * {@link SortPolicy}.
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class ExpressionEncoder
		implements
		ExpressionVisitor<Term> {

	private static final Map<Class<?>, Integer> BIT_WIDTHS = new HashMap<>();

	static {
		for (Class<?> c : List.of(Numeric8BitAdd.class, Numeric8BitSub.class, Numeric8BitMul.class,
				Numeric8BitDiv.class, Numeric8BitMod.class, Numeric8BitRem.class))
			BIT_WIDTHS.put(c, 8);
		for (Class<?> c : List.of(Numeric16BitAdd.class, Numeric16BitSub.class, Numeric16BitMul.class,
				Numeric16BitDiv.class, Numeric16BitMod.class, Numeric16BitRem.class))
			BIT_WIDTHS.put(c, 16);
		for (Class<?> c : List.of(Numeric32BitAdd.class, Numeric32BitSub.class, Numeric32BitMul.class,
				Numeric32BitDiv.class, Numeric32BitMod.class, Numeric32BitRem.class))
			BIT_WIDTHS.put(c, 32);
		for (Class<?> c : List.of(Numeric64BitAdd.class, Numeric64BitSub.class, Numeric64BitMul.class,
				Numeric64BitDiv.class, Numeric64BitMod.class, Numeric64BitRem.class))
			BIT_WIDTHS.put(c, 64);
	}

	private final SortPolicy sorts;

	private final SymbolTable symbols;

	/**
	 * Builds the encoder.
	 *
	 * @param sorts   the policy deciding the sorts of identifiers and of
	 *                    unknown values
	 * @param symbols the provider of the constants used in the encoding
	 */
	public ExpressionEncoder(
			SortPolicy sorts,
			SymbolTable symbols) {
		this.sorts = sorts;
		this.symbols = symbols;
	}

	/**
	 * Encodes the given expression. The sort of the resulting term depends on
	 * the expression.
	 *
	 * @param expression the expression
	 *
	 * @return the encoded term
	 *
	 * @throws SemanticException if the expression cannot be visited
	 */
	public Term encode(
			SymbolicExpression expression)
			throws SemanticException {
		return expression.accept(this);
	}

	/**
	 * Encodes the given expression as a term of the given sort, converting the
	 * result if needed (see {@link #coerce(Term, Sort, SymbolicExpression)}).
	 *
	 * @param expression the expression
	 * @param sort       the sort of the resulting term
	 *
	 * @return the encoded term
	 *
	 * @throws SemanticException if the expression cannot be visited
	 */
	public Term encodeAs(
			SymbolicExpression expression,
			Sort sort)
			throws SemanticException {
		return coerce(encode(expression), sort, expression);
	}

	/**
	 * Encodes the given expression as a boolean term (e.g., a condition or a
	 * constraint).
	 *
	 * @param expression the expression
	 *
	 * @return the encoded boolean term
	 *
	 * @throws SemanticException if the expression cannot be visited
	 */
	public Term encodeCondition(
			SymbolicExpression expression)
			throws SemanticException {
		return encodeAs(expression, Sort.BOOL);
	}

	/**
	 * Converts the given term to the given sort: native terms are wrapped into
	 * {@link ValueSort} values, values are read as native terms (yielding an
	 * unspecified term if the value is of a different kind), and any other
	 * conversion yields a fresh constant.
	 *
	 * @param term   the term to convert
	 * @param sort   the target sort
	 * @param origin the expression that produced the term
	 *
	 * @return the converted term
	 */
	public Term coerce(
			Term term,
			Sort sort,
			SymbolicExpression origin) {
		if (term.getSort().equals(sort))
			return term;
		Term converted = null;
		if (sort.equals(ValueSort.SORT))
			converted = ValueSort.wrap(term);
		else if (term.getSort().equals(ValueSort.SORT))
			converted = ValueSort.unwrap(term, sort);
		return converted != null ? converted : symbols.fresh(sort, origin);
	}

	private Term havoc(
			SymbolicExpression expression) {
		return symbols.fresh(sorts.sortOf(expression.getStaticType()), expression);
	}

	@Override
	public Term visit(
			HeapExpression expression,
			Term[] subExpressions,
			Object... params) {
		return symbols.fresh(ValueSort.SORT, expression);
	}

	@Override
	public Term visit(
			AccessChild expression,
			Term receiver,
			Term child,
			Object... params) {
		return symbols.fresh(ValueSort.SORT, expression);
	}

	@Override
	public Term visit(
			MemoryAllocation expression,
			Object... params) {
		return symbols.fresh(ValueSort.SORT, expression);
	}

	@Override
	public Term visit(
			HeapReference expression,
			Term arg,
			Object... params) {
		return symbols.fresh(ValueSort.SORT, expression);
	}

	@Override
	public Term visit(
			HeapDereference expression,
			Term arg,
			Object... params) {
		return symbols.fresh(ValueSort.SORT, expression);
	}

	@Override
	public Term visit(
			ValueExpression expression,
			Term[] subExpressions,
			Object... params) {
		return havoc(expression);
	}

	@Override
	public Term visit(
			Skip expression,
			Object... params) {
		return symbols.fresh(ValueSort.SORT, expression);
	}

	@Override
	public Term visit(
			PushAny expression,
			Object... params) {
		return havoc(expression);
	}

	@Override
	public Term visit(
			PushInv expression,
			Object... params) {
		return havoc(expression);
	}

	@Override
	public Term visit(
			Constant expression,
			Object... params) {
		Object value = expression.getValue();
		if (value instanceof Integer || value instanceof Long || value instanceof Short || value instanceof Byte)
			return Term.integer(((Number) value).longValue());
		if (value instanceof BigInteger)
			return Term.integer((BigInteger) value);
		if (value instanceof Boolean)
			return Term.bool((Boolean) value);
		if (value instanceof String)
			return Term.string((String) value);
		// floats, characters, type tokens, ...
		return havoc(expression);
	}

	@Override
	public Term visit(
			Identifier expression,
			Object... params) {
		return symbols.constant(expression, sorts.sortOf(expression));
	}

	@Override
	public Term visit(
			NullConstant expression,
			Object... params) {
		return ValueSort.nullValue();
	}

	@Override
	public Term visit(
			UnaryExpression expression,
			Term arg,
			Object... params) {
		UnaryOperator op = expression.getOperator();
		if (op instanceof LogicalNegation)
			return Term.not(coerce(arg, Sort.BOOL, expression));
		if (op instanceof NumericNegation)
			return arithmetic(expression, List.of(arg), args -> neg(args.get(0)));
		if (op instanceof NumericAbs)
			return arithmetic(expression, List.of(arg), args -> abs(args.get(0)));
		if (op instanceof BitwiseNegation)
			// ~x == -x - 1 in two's complement
			return arithmetic(expression, List.of(arg),
					args -> Term.app("-", Sort.INT, neg(args.get(0)), Term.integer(1)));
		if (op instanceof StringLength)
			return Term.app("str.len", Sort.INT, coerce(arg, Sort.STRING, expression));
		return havoc(expression);
	}

	@Override
	public Term visit(
			BinaryExpression expression,
			Term left,
			Term right,
			Object... params) {
		BinaryOperator op = expression.getOperator();

		if (op instanceof LogicalAnd)
			return Term.and(coerce(left, Sort.BOOL, expression), coerce(right, Sort.BOOL, expression));
		if (op instanceof LogicalOr)
			return Term.or(coerce(left, Sort.BOOL, expression), coerce(right, Sort.BOOL, expression));
		if (op instanceof ComparisonEq)
			return equality(left, right, expression);
		if (op instanceof ComparisonNe)
			return Term.not(equality(left, right, expression));
		if (op instanceof ComparisonLt)
			return comparison("<", left, right, expression);
		if (op instanceof ComparisonLe)
			return comparison("<=", left, right, expression);
		if (op instanceof ComparisonGt)
			return comparison(">", left, right, expression);
		if (op instanceof ComparisonGe)
			return comparison(">=", left, right, expression);

		if (op instanceof AdditionOperator)
			return arithmetic(expression, List.of(left, right),
					args -> Term.app("+", Sort.INT, args.get(0), args.get(1)));
		if (op instanceof SubtractionOperator)
			return arithmetic(expression, List.of(left, right),
					args -> Term.app("-", Sort.INT, args.get(0), args.get(1)));
		if (op instanceof MultiplicationOperator)
			return arithmetic(expression, List.of(left, right),
					args -> Term.app("*", Sort.INT, args.get(0), args.get(1)));
		if (op instanceof DivisionOperator)
			return arithmetic(expression, List.of(left, right), args -> tdiv(args.get(0), args.get(1)));
		if (op instanceof RemainderOperator)
			return arithmetic(expression, List.of(left, right), args -> trem(args.get(0), args.get(1)));
		if (op instanceof ModuloOperator)
			return arithmetic(expression, List.of(left, right), args -> fmod(args.get(0), args.get(1)));
		if (op instanceof NumericMax)
			return arithmetic(expression, List.of(left, right),
					args -> Term.ite(Term.app(">=", Sort.BOOL, args.get(0), args.get(1)), args.get(0), args.get(1)));
		if (op instanceof NumericMin)
			return arithmetic(expression, List.of(left, right),
					args -> Term.ite(Term.app("<=", Sort.BOOL, args.get(0), args.get(1)), args.get(0), args.get(1)));

		if (op instanceof StringConcat)
			return Term.app("str.++", Sort.STRING, str(left, expression), str(right, expression));
		if (op instanceof StringContains)
			return Term.app("str.contains", Sort.BOOL, str(left, expression), str(right, expression));
		if (op instanceof StringStartsWith)
			return Term.app("str.prefixof", Sort.BOOL, str(right, expression), str(left, expression));
		if (op instanceof StringEndsWith)
			return Term.app("str.suffixof", Sort.BOOL, str(right, expression), str(left, expression));
		if (op instanceof StringIsPrefixOf)
			return Term.app("str.prefixof", Sort.BOOL, str(left, expression), str(right, expression));
		if (op instanceof StringIsSuffixOf)
			return Term.app("str.suffixof", Sort.BOOL, str(left, expression), str(right, expression));
		if (op instanceof StringEquals)
			return Term.eq(str(left, expression), str(right, expression));
		if (op instanceof StringIndexOf)
			return Term.app("str.indexof", Sort.INT, str(left, expression), str(right, expression),
					Term.integer(0));
		if (op instanceof StringSubstringToEnd) {
			Term s = str(left, expression);
			Term from = coerce(right, Sort.INT, expression);
			return Term.app("str.substr", Sort.STRING, s, from,
					Term.app("-", Sort.INT, Term.app("str.len", Sort.INT, s), from));
		}

		if (op instanceof TypeCast || op instanceof TypeConv)
			// the value is preserved if it already has the target sort, and
			// read as the target sort otherwise
			return coerce(left, sorts.sortOf(expression.getStaticType()), expression);

		return havoc(expression);
	}

	@Override
	public Term visit(
			TernaryExpression expression,
			Term left,
			Term middle,
			Term right,
			Object... params) {
		TernaryOperator op = expression.getOperator();
		if (op instanceof StringSubstring) {
			Term from = coerce(middle, Sort.INT, expression);
			Term to = coerce(right, Sort.INT, expression);
			return Term.app("str.substr", Sort.STRING, str(left, expression), from,
					Term.app("-", Sort.INT, to, from));
		}
		if (op instanceof StringReplace)
			return Term.app("str.replace", Sort.STRING, str(left, expression), str(middle, expression),
					str(right, expression));
		if (op instanceof StringIndexOfFromIndex)
			return Term.app("str.indexof", Sort.INT, str(left, expression), str(middle, expression),
					coerce(right, Sort.INT, expression));
		if (op instanceof StringStartsWithFromIndex) {
			Term s = str(left, expression);
			Term from = coerce(right, Sort.INT, expression);
			Term suffix = Term.app("str.substr", Sort.STRING, s, from,
					Term.app("-", Sort.INT, Term.app("str.len", Sort.INT, s), from));
			return Term.app("str.prefixof", Sort.BOOL, str(middle, expression), suffix);
		}
		return havoc(expression);
	}

	private Term str(
			Term term,
			SymbolicExpression origin) {
		return coerce(term, Sort.STRING, origin);
	}

	private Term equality(
			Term left,
			Term right,
			SymbolicExpression origin) {
		if (left.getSort().equals(right.getSort()))
			return Term.eq(left, right);
		// values of different sorts are compared as values
		return Term.eq(coerce(left, ValueSort.SORT, origin), coerce(right, ValueSort.SORT, origin));
	}

	private Term comparison(
			String op,
			Term left,
			Term right,
			BinaryExpression origin) {
		if (!numericOrValue(left) || !numericOrValue(right))
			return symbols.fresh(Sort.BOOL, origin);
		// values that are not integers are read as unspecified integers, so
		// that the result of the comparison is unconstrained
		return Term.app(op, Sort.BOOL, coerce(left, Sort.INT, origin), coerce(right, Sort.INT, origin));
	}

	private static boolean numericOrValue(
			Term term) {
		return term.getSort().equals(Sort.INT) || term.getSort().equals(ValueSort.SORT);
	}

	/**
	 * Encodes an arithmetic operation over integers. If all the operands are
	 * integers, the result is an integer. If some operands are values, the
	 * result is a value that is the integer result if all operands are integer
	 * values, and unconstrained otherwise (e.g., if some operand is a floating
	 * point number). Any other combination of sorts yields a fresh constant.
	 * The static type of the expression is not used to decide whether the
	 * operation is over integers, since frontends might use a common
	 * supertype of all the possible runtime types of the operands.
	 */
	private Term arithmetic(
			ValueExpression expression,
			List<Term> operands,
			Function<List<Term>, Term> op) {
		Type type = expression.getStaticType();
		boolean values = false;
		for (Term t : operands)
			if (t.getSort().equals(ValueSort.SORT))
				values = true;
			else if (!t.getSort().equals(Sort.INT))
				return havoc(expression);

		List<Term> ints = new ArrayList<>(operands.size());
		List<Term> guards = new ArrayList<>();
		for (Term t : operands) {
			ints.add(coerce(t, Sort.INT, expression));
			if (t.getSort().equals(ValueSort.SORT))
				guards.add(ValueSort.is(t, Sort.INT));
		}

		Term result = op.apply(ints);
		Integer bits = expression instanceof BinaryExpression
				? BIT_WIDTHS.get(((BinaryExpression) expression).getOperator().getClass())
				: null;
		if (bits != null)
			result = wrap(result, bits, type.isNumericType() && type.asNumericType().isUnsigned());

		if (!values)
			return result;
		return Term.ite(
				Term.and(guards.toArray(Term[]::new)),
				ValueSort.wrap(result),
				symbols.fresh(ValueSort.SORT, expression));
	}

	private static Term neg(
			Term t) {
		return Term.app("-", Sort.INT, t);
	}

	private static Term abs(
			Term t) {
		return Term.ite(Term.app(">=", Sort.BOOL, t, Term.integer(0)), t, neg(t));
	}

	/**
	 * Integer division truncating toward zero (SMT-LIB's {@code div} rounds
	 * toward negative infinity for positive divisors).
	 */
	private static Term tdiv(
			Term a,
			Term b) {
		Term zero = Term.integer(0);
		Term aNonNeg = Term.app(">=", Sort.BOOL, a, zero);
		Term bNonNeg = Term.app(">=", Sort.BOOL, b, zero);
		return Term.ite(aNonNeg,
				Term.ite(bNonNeg, div(a, b), neg(div(a, neg(b)))),
				Term.ite(bNonNeg, neg(div(neg(a), b)), div(neg(a), neg(b))));
	}

	private static Term div(
			Term a,
			Term b) {
		return Term.app("div", Sort.INT, a, b);
	}

	/**
	 * Remainder of the truncating division, taking the sign of the dividend.
	 */
	private static Term trem(
			Term a,
			Term b) {
		return Term.app("-", Sort.INT, a, Term.app("*", Sort.INT, b, tdiv(a, b)));
	}

	/**
	 * Modulo taking the sign of the divisor (SMT-LIB's {@code mod} is always
	 * non-negative).
	 */
	private static Term fmod(
			Term a,
			Term b) {
		Term r = Term.app("mod", Sort.INT, a, b);
		Term zero = Term.integer(0);
		return Term.ite(
				Term.and(Term.app("<", Sort.BOOL, b, zero), Term.not(Term.eq(r, zero))),
				Term.app("+", Sort.INT, r, b),
				r);
	}

	/**
	 * Wraps the given integer to the range of integers representable with the
	 * given number of bits.
	 */
	private static Term wrap(
			Term t,
			int bits,
			boolean unsigned) {
		Term modulus = Term.integer(BigInteger.ONE.shiftLeft(bits));
		if (unsigned)
			return Term.app("mod", Sort.INT, t, modulus);
		Term half = Term.integer(BigInteger.ONE.shiftLeft(bits - 1));
		return Term.app("-", Sort.INT,
				Term.app("mod", Sort.INT, Term.app("+", Sort.INT, t, half), modulus),
				half);
	}
}
