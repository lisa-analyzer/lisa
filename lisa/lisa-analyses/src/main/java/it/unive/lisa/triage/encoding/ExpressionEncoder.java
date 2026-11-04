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
import it.unive.lisa.symbolic.value.operator.binary.StringIsSubstringOf;
import it.unive.lisa.symbolic.value.operator.binary.StringIsSuffixOf;
import it.unive.lisa.symbolic.value.operator.binary.StringStartsWith;
import it.unive.lisa.symbolic.value.operator.binary.StringSubstringToEnd;
import it.unive.lisa.symbolic.value.operator.binary.TypeCast;
import it.unive.lisa.symbolic.value.operator.binary.TypeCheck;
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
import it.unive.lisa.triage.model.SummaryValue;
import it.unive.lisa.triage.model.InputValue;
import it.unive.lisa.triage.model.HeapOperators;
import it.unive.lisa.triage.smt.Sort;
import it.unive.lisa.triage.smt.Term;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeTokenType;
import java.math.BigInteger;
import java.util.HashSet;
import java.util.Set;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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
 * an integer, boolean, or string yields an unspecified value if the value is of
 * a different kind;</li>
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

	private static final Map<Class<?>, Integer> BIT_WIDTHS = new LinkedHashMap<>();

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
		else if (term.getSort().equals(ValueSort.SORT)) {
			Term guard = ValueSort.is(term, sort);
			if (guard != null) {
				// values built with another constructor are read as
				// unspecified (but consistent) values of the sort
				converted = ValueSort.unwrap(term, sort);
				symbols.exactOnlyIf(guard, origin);
			}
		}
		return converted != null ? converted : symbols.fresh(sort, origin);
	}

	/**
	 * Yields an unconstrained value for an expression that cannot be encoded.
	 * The value can be of any kind, regardless of the static type of the
	 * expression: frontends might assign imprecise (or wrong) static types to
	 * the results of operations they model, and trusting them would exclude
	 * real executions. The value is not precise anyway, so this costs no
	 * precision.
	 */
	private Term havoc(
			SymbolicExpression expression) {
		return symbols.fresh(ValueSort.SORT, expression);
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
		// unknown values (e.g., inputs) have the type declared by the code
		// producing them
		return symbols.fresh(sorts.sortOf(expression.getStaticType()), expression);
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
		if (value instanceof Type)
			// type tokens, used by type operators
			return ValueSort.typeToken((Type) value);
		// floats, characters, ...
		return havoc(expression);
	}

	@Override
	public Term visit(
			Identifier expression,
			Object... params) {
		Term term = symbols.constant(expression, sorts.sortOf(expression));
		if (expression instanceof SummaryValue
				&& ((SummaryValue) expression).getReason().equals(SummaryValue.MERGED_CONDITIONS))
			// the conditions lost by merging cases do not constrain the
			// executions, but their use is recorded as an imprecision by
			// declaring the summary
			return Term.TRUE;
		if (expression instanceof InputValue && constraining.add((InputValue) expression))
			try {
				// inputs can only take values satisfying the constraints of
				// their sources, given in the form "constant OP value"
				Set<BinaryExpression> constraints = ((InputValue) expression).getSource().getConstraints();
				if (constraints != null)
					for (BinaryExpression constraint : constraints)
						symbols.require(encodeCondition(new BinaryExpression(
								constraint.getStaticType(),
								constraint.getLeft(),
								expression,
								constraint.getOperator(),
								constraint.getCodeLocation())));
			} catch (SemanticException e) {
				// the constraint cannot be encoded: not requiring it is sound
			} finally {
				constraining.remove(expression);
			}
		return term;
	}

	/**
	 * The inputs whose constraints are being encoded, whose encoding must not
	 * encode their constraints again.
	 */
	private final Set<InputValue> constraining = new HashSet<>();

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
		if (op instanceof HeapOperators.NewReference)
			return ValueSort.reference(coerce(arg, Sort.INT, expression));
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

		if (op instanceof HeapOperators.CellKey)
			return CellKeySort.key(referenceKey(left, expression), coerce(right, Sort.INT, expression));

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
		if (op instanceof StringIsSubstringOf)
			return Term.app("str.contains", Sort.BOOL, str(right, expression), str(left, expression));
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

		if (op instanceof TypeCheck) {
			Type target = tokenOf(expression.getRight());
			if (target != null)
				return typeCheck(left, target, expression);
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
		if (op instanceof HeapOperators.ReferenceRead)
			return read(left, middle, heapKey(right, expression), expression);
		if (op instanceof HeapOperators.ReferenceStore)
			return store(left, heapKey(middle, expression), right, expression);
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
		// operands of string operators are strings
		if (term.getSort().equals(ValueSort.SORT))
			symbols.require(ValueSort.is(term, Sort.STRING));
		return coerce(term, Sort.STRING, origin);
	}

	/**
	 * Encodes the equality of two terms. Values are equal if they are the same
	 * value, unless one of them is opaque and might be a floating point number
	 * ({@link SortPolicy#mayBeOpaque(SymbolicExpression)}): the meaning of
	 * opaque values is not modeled, and equality between them does not
	 * coincide with their identity (e.g., a floating point NaN is not equal to
	 * itself, and {@code 1 == 1.0} holds). Such equalities are thus an
	 * unknown, but deterministic, function of the operands (never precise).
	 */
	private Term equality(
			Term left,
			Term right,
			SymbolicExpression origin) {
		boolean opaque = mayBeOpaque(origin);
		if (left.getSort().equals(right.getSort()) && (!opaque || !left.getSort().equals(ValueSort.SORT)))
			return Term.eq(left, right);
		// values of different sorts are compared as values
		Term l = coerce(left, ValueSort.SORT, origin);
		Term r = coerce(right, ValueSort.SORT, origin);
		if (!opaque)
			return Term.eq(l, r);
		Term modeled = Term.not(Term.or(ValueSort.isOpaque(l), ValueSort.isOpaque(r)));
		return Term.ite(modeled, Term.eq(l, r),
				symbols.apply("$==", Sort.BOOL, List.of(l, r), origin, modeled));
	}

	/**
	 * Encodes a comparison between two numbers. Only integers are compared by
	 * their value: if an operand might be a floating point number
	 * ({@link SortPolicy#mayBeOpaque(SymbolicExpression)}), comparisons
	 * involving numbers that are not integers (that might be NaN, so that
	 * neither {@code x > y} nor {@code y >= x} holds) are an unknown, but
	 * deterministic, function of the operands (never precise). Otherwise, the
	 * operands are read as integers.
	 */
	private Term comparison(
			String op,
			Term left,
			Term right,
			BinaryExpression origin) {
		if (!numericOrValue(left) || !numericOrValue(right))
			return symbols.fresh(Sort.BOOL, origin);
		// operands of comparisons are numbers
		requireNumeric(left);
		requireNumeric(right);
		if (left.getSort().equals(Sort.INT) && right.getSort().equals(Sort.INT))
			return Term.app(op, Sort.BOOL, left, right);
		if (!mayBeOpaque(origin))
			// values that are not integers are read as unspecified integers
			return Term.app(op, Sort.BOOL, coerce(left, Sort.INT, origin), coerce(right, Sort.INT, origin));

		List<Term> guards = new ArrayList<>(2);
		for (Term t : List.of(left, right))
			if (t.getSort().equals(ValueSort.SORT))
				guards.add(ValueSort.is(t, Sort.INT));
		Term guard = Term.and(guards.toArray(Term[]::new));
		Term ints = Term.app(op, Sort.BOOL, asInt(left), asInt(right));
		return Term.ite(guard, ints, symbols.apply("$" + op, Sort.BOOL,
				List.of(ValueSort.wrap(left), ValueSort.wrap(right)), origin, guard));
	}

	/**
	 * Yields whether an operand of the given binary expression might be an
	 * opaque value whose meaning is not modeled (always {@code true} if the
	 * expression is not binary).
	 */
	private boolean mayBeOpaque(
			SymbolicExpression origin) {
		if (!(origin instanceof BinaryExpression))
			return true;
		BinaryExpression binary = (BinaryExpression) origin;
		return sorts.mayBeOpaque(binary.getLeft()) || sorts.mayBeOpaque(binary.getRight());
	}

	/**
	 * Encodes the key of a heap map: the key of a cell is used as it is, while
	 * references are keyed by their identifier.
	 */
	private Term heapKey(
			Term key,
			SymbolicExpression origin) {
		if (key.getSort().equals(CellKeySort.SORT))
			return key;
		return referenceKey(key, origin);
	}

	/**
	 * Encodes the key of a heap map for the given reference: the identifier
	 * of the reference (precise only if the value is a reference, e.g., not
	 * null).
	 */
	private Term referenceKey(
			Term reference,
			SymbolicExpression origin) {
		if (reference.getSort().equals(ValueSort.SORT)) {
			symbols.exactOnlyIf(ValueSort.isReference(reference), origin);
			return ValueSort.referenceId(reference);
		}
		return symbols.fresh(Sort.INT, origin);
	}

	/**
	 * Encodes the value of the given map at the given key (unconstrained if
	 * the map is not an array).
	 */
	private Term select(
			Term map,
			Term key,
			SymbolicExpression origin) {
		if (!map.getSort().isArray())
			return symbols.fresh(ValueSort.SORT, origin);
		return Term.app("select", map.getSort().getElement(), map, key);
	}

	/**
	 * Encodes the value of the given map at the given key, as read by the
	 * program: the read is precise only if the location has been written, as
	 * recorded by the given map of written locations.
	 */
	private Term read(
			Term map,
			Term written,
			Term key,
			SymbolicExpression origin) {
		if (written.getSort().isArray() && written.getSort().getElement().equals(Sort.BOOL))
			symbols.exactOnlyIfWritten(Term.app("select", Sort.BOOL, written, key), origin);
		else
			symbols.exactOnlyIfWritten(Term.FALSE, origin);
		return select(map, key, origin);
	}

	/**
	 * Encodes the map obtained by writing the given value at the given key in
	 * the given map (unconstrained if the map is not an array).
	 */
	private Term store(
			Term map,
			Term key,
			Term value,
			SymbolicExpression origin) {
		if (!map.getSort().isArray())
			return symbols.fresh(sorts.sortOf(origin.getStaticType()), origin);
		return Term.app("store", map.getSort(), map, key, coerce(value, map.getSort().getElement(), origin));
	}

	private static Term asInt(
			Term term) {
		return term.getSort().equals(ValueSort.SORT) ? ValueSort.unwrap(term, Sort.INT) : term;
	}

	/**
	 * Encodes whether the given term has a type that can be assigned to the
	 * given one. A value can have the type only if it has been built with the
	 * constructor of the sort of the type, and it surely has it if the type
	 * covers the sort ({@link SortPolicy#covers(Type, Sort)}): otherwise, the
	 * result is unconstrained among the values with the right constructor.
	 * Terms that are not values have the type if it covers their sort, and do
	 * not have it if their sort is different.
	 */
	/**
	 * Yields the type denoted by the given type token: a constant whose value
	 * is the type, or whose static type is a {@link TypeTokenType} for a
	 * single type (whatever its value). Yields {@code null} if the expression
	 * does not denote a unique type.
	 */
	private static Type tokenOf(
			SymbolicExpression token) {
		if (!(token instanceof Constant))
			return null;
		Object value = ((Constant) token).getValue();
		if (value instanceof Type)
			return (Type) value;
		Type type = token.getStaticType();
		if (type instanceof TypeTokenType && ((TypeTokenType) type).getTypes().size() == 1)
			return ((TypeTokenType) type).getTypes().iterator().next();
		return null;
	}

	private Term typeCheck(
			Term term,
			Type target,
			BinaryExpression origin) {
		Sort sort = sorts.sortOf(target);
		if (sort.equals(ValueSort.SORT))
			// types encoded as values (e.g., references) cannot be told
			// apart, but the test of a value is deterministic
			return term.getSort().equals(ValueSort.SORT) ? typeTest(term, target, origin)
					: symbols.fresh(Sort.BOOL, origin);
		boolean covers = sorts.covers(target, sort);
		if (term.getSort().equals(ValueSort.SORT)) {
			Term constructor = ValueSort.is(term, sort);
			return covers ? constructor : Term.and(constructor, typeTest(term, target, origin));
		}
		if (!term.getSort().equals(sort))
			return Term.FALSE;
		return covers ? Term.TRUE : typeTest(term, target, origin);
	}

	/**
	 * Yields an unknown, but deterministic, test of whether the given term has
	 * the given type (never precise).
	 */
	private Term typeTest(
			Term term,
			Type target,
			BinaryExpression origin) {
		return symbols.apply("$is " + target, Sort.BOOL, List.of(term), origin, Term.FALSE);
	}

	private static String operatorOf(
			ValueExpression expression) {
		if (expression instanceof UnaryExpression)
			return ((UnaryExpression) expression).getOperator().getClass().getSimpleName();
		if (expression instanceof BinaryExpression)
			return ((BinaryExpression) expression).getOperator().getClass().getSimpleName();
		if (expression instanceof TernaryExpression)
			return ((TernaryExpression) expression).getOperator().getClass().getSimpleName();
		return expression.getClass().getSimpleName();
	}

	private void requireNumeric(
			Term term) {
		if (term.getSort().equals(ValueSort.SORT))
			symbols.require(ValueSort.isNumeric(term));
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
	 * operation is over integers, since frontends might use a common supertype
	 * of all the possible runtime types of the operands.
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
			// the guard on the result covers non-integer operands
			ints.add(t.getSort().equals(ValueSort.SORT) ? ValueSort.unwrap(t, Sort.INT) : t);
			if (t.getSort().equals(ValueSort.SORT))
				guards.add(ValueSort.is(t, Sort.INT));
			// operands of arithmetic operators are numbers
			requireNumeric(t);
		}

		Term guard = Term.and(guards.toArray(Term[]::new));
		Term result = op.apply(ints);
		Integer bits = expression instanceof BinaryExpression
				? BIT_WIDTHS.get(((BinaryExpression) expression).getOperator().getClass())
				: null;
		if (bits != null)
			result = wrap(result, bits, type.isNumericType() && type.asNumericType().isUnsigned());

		if (!values)
			return result;
		// the result over values that are not integers is unknown, but it is
		// a function of the operands
		List<Term> wrapped = new ArrayList<>(operands.size());
		for (Term t : operands)
			wrapped.add(ValueSort.wrap(t));
		return Term.ite(guard, ValueSort.wrap(result),
				symbols.apply("$" + operatorOf(expression), ValueSort.SORT, wrapped, expression, guard));
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
