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
import it.unive.lisa.symbolic.value.operator.binary.BitwiseAnd;
import it.unive.lisa.symbolic.value.operator.binary.BitwiseOr;
import it.unive.lisa.symbolic.value.operator.binary.BitwiseShiftLeft;
import it.unive.lisa.symbolic.value.operator.binary.BitwiseShiftRight;
import it.unive.lisa.symbolic.value.operator.binary.BitwiseUnsignedShiftRight;
import it.unive.lisa.symbolic.value.operator.binary.BitwiseXor;
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
import it.unive.lisa.triage.model.FreshValue;
import it.unive.lisa.triage.model.HeapModel;
import it.unive.lisa.triage.model.HeapOperators;
import it.unive.lisa.triage.model.InputValue;
import it.unive.lisa.triage.model.SummaryValue;
import it.unive.lisa.triage.smt.FloatingPoints;
import it.unive.lisa.triage.smt.Sort;
import it.unive.lisa.triage.smt.Term;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeTokenType;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
 * {@link SortPolicy}.<br>
 * <br>
 * This encoder knows only the operators defined by LiSA. Frontends defining
 * their own operators can encode them by subclassing this encoder and
 * overriding the methods encoding each kind of expression (e.g.,
 * {@link #encodeBinary(BinaryExpression, Term, Term)}), that contain the whole
 * encoding of the expressions of that kind and yield an unconstrained value for
 * unknown operators. Overriding methods should encode the operators they know
 * (possibly replacing the encoding of operators known by this encoder), and
 * invoke the overridden method for the other ones, relying on the helpers of
 * this class (e.g., {@link #coerce(Term, Sort, SymbolicExpression)},
 * {@link #asString(Term, SymbolicExpression)},
 * {@link #arithmetic(ValueExpression, List, Function)},
 * {@link #guarded(Term, Term, List, ValueExpression)}) and on the
 * {@link SymbolTable} ({@link #getSymbols()}) to record the conditions under
 * which their encoding is precise. The verifiers building encodings (e.g.,
 * {@link it.unive.lisa.triage.pdr.PdrVerifier}) can be instructed to use the
 * subclass through an {@link ExpressionEncoderFactory}.
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
	 * Yields the policy deciding the sorts of identifiers and of unknown
	 * values.
	 *
	 * @return the policy
	 */
	protected SortPolicy getSorts() {
		return sorts;
	}

	/**
	 * Yields the provider of the constants used in the encoding, where
	 * subclasses can also record the conditions under which their encodings are
	 * precise ({@link SymbolTable#exactOnlyIf(Term, SymbolicExpression)}) or
	 * can be evaluated ({@link SymbolTable#require(Term)}).
	 *
	 * @return the symbol table
	 */
	protected SymbolTable getSymbols() {
		return symbols;
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
		boolean top = encoded == null;
		if (top)
			encoded = new IdentityHashMap<>();
		try {
			return encodeShared(expression);
		} finally {
			if (top)
				encoded = null;
		}
	}

	/**
	 * The encodings of the sub-expressions encoded by the current call to
	 * {@link #encode(SymbolicExpression)}, by identity, or by all the calls
	 * since {@link #startSharing()}.
	 */
	private Map<SymbolicExpression, Term> encoded;

	/**
	 * Makes the following calls to {@link #encode(SymbolicExpression)}, until
	 * {@link #stopSharing()}, share the encodings of their compound
	 * sub-expressions, as a single call does: expressions that occur in more
	 * than one of them (e.g., a value written in two heap maps by the same
	 * transition) are encoded once, so that the auxiliary symbols introduced by
	 * their encodings are not repeated. This must only be used for expressions
	 * evaluated in the same state (e.g., the updates of a single transition).
	 */
	public void startSharing() {
		encoded = new IdentityHashMap<>();
	}

	/**
	 * Ends the sharing started by {@link #startSharing()}.
	 */
	public void stopSharing() {
		encoded = null;
	}

	/**
	 * Encodes the given expression, encoding each of its compound
	 * sub-expressions only once: expressions built by composing transitions
	 * share their sub-expressions (e.g., the value of a variable used more than
	 * once), so that their trees might be exponentially larger than the
	 * expressions themselves.
	 */
	private Term encodeShared(
			SymbolicExpression expression)
			throws SemanticException {
		Term known = encoded.get(expression);
		if (known != null)
			return known;
		Term result;
		if (expression instanceof UnaryExpression) {
			UnaryExpression u = (UnaryExpression) expression;
			result = visit(u, encodeShared(u.getExpression()));
		} else if (expression instanceof BinaryExpression) {
			BinaryExpression b = (BinaryExpression) expression;
			Term left = encodeShared(b.getLeft());
			result = visit(b, left, encodeShared(b.getRight()));
		} else if (expression instanceof TernaryExpression) {
			TernaryExpression t = (TernaryExpression) expression;
			Term left = encodeShared(t.getLeft());
			Term middle = encodeShared(t.getMiddle());
			result = visit(t, left, middle, encodeShared(t.getRight()));
		} else
			// leaves are not shared: each occurrence of an unknown value
			// (e.g., PushAny) stands for a different value
			return expression.accept(this);
		encoded.put(expression, result);
		return result;
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
	 *
	 * @param expression the expression that cannot be encoded
	 *
	 * @return the unconstrained value
	 */
	protected Term havoc(
			SymbolicExpression expression) {
		return symbols.fresh(ValueSort.SORT, expression);
	}

	/**
	 * Yields the value of an expression whose operator is not encoded: as for
	 * {@link #havoc(SymbolicExpression)}, the value is not precise, but it is
	 * the application of an unknown function of the operands, specific to the
	 * operator (see
	 * {@link SymbolTable#unknown(String, Sort, List, SymbolicExpression)}), so
	 * that the same operation over the same operands yields the same value.
	 * This assumes that operators are deterministic, and that equal operators
	 * (as by {@link Object#equals(Object)}) denote the same operation.
	 *
	 * @param expression the expression
	 * @param operands   the encodings of its operands
	 *
	 * @return the value
	 */
	protected Term unsupported(
			ValueExpression expression,
			Term... operands) {
		Object op = expression instanceof UnaryExpression ? ((UnaryExpression) expression).getOperator()
				: expression instanceof BinaryExpression ? ((BinaryExpression) expression).getOperator()
						: expression instanceof TernaryExpression ? ((TernaryExpression) expression).getOperator()
								: null;
		if (op == null)
			return havoc(expression);
		// operators that are not equal must denote different functions
		String function = "$" + op.getClass().getSimpleName() + ":" + op + "#" + Integer.toHexString(op.hashCode());
		return symbols.unknown(function, ValueSort.SORT, List.of(operands), expression);
	}

	/**
	 * Yields an unknown opaque value, standing for a floating point number
	 * produced by the given expression when floating point numbers are not
	 * encoded. As for {@link #havoc(SymbolicExpression)}, the value is not
	 * precise.
	 *
	 * @param expression the expression producing the number
	 *
	 * @return the opaque value
	 */
	protected Term opaque(
			SymbolicExpression expression) {
		Term value = symbols.fresh(ValueSort.SORT, expression);
		symbols.require(ValueSort.isOpaque(value));
		return value;
	}

	/**
	 * Yields an auxiliary constant of the given sort, used to encode the
	 * expression precisely: the caller must constrain it, through
	 * {@link SymbolTable#require(Term)}, so that its value is determined by the
	 * operands of the expression (e.g., the position of the last occurrence of
	 * a character in a string). Since its value is determined, using it does
	 * not make the encoding imprecise.
	 *
	 * @param sort   the sort of the constant
	 * @param origin the expression being encoded
	 *
	 * @return the constant
	 */
	protected Term auxiliary(
			Sort sort,
			SymbolicExpression origin) {
		return symbols.fresh(sort, origin, Term.TRUE);
	}

	/**
	 * Yields the floating point sort of an operation over the given operands:
	 * the widest floating point sort among the ones of the operands, or
	 * {@code null} if no operand is a floating point number.
	 *
	 * @param operands the encodings of the operands
	 *
	 * @return the sort, or {@code null}
	 */
	protected static Sort floatingSort(
			Term... operands) {
		Sort result = null;
		for (Term t : operands)
			if (t.getSort().equals(Sort.FLOAT64))
				return Sort.FLOAT64;
			else if (t.getSort().equals(Sort.FLOAT32))
				result = Sort.FLOAT32;
		return result;
	}

	/**
	 * Yields the rounding mode of floating point operations in the encoding
	 * (round to nearest, ties to even, as in Java).
	 *
	 * @return the rounding mode
	 */
	protected static Term rne() {
		return Term.value("RNE", Sort.ROUNDING_MODE);
	}

	/**
	 * Converts the given term to a floating point number of the given sort:
	 * integers and floating point numbers of the other sort are converted with
	 * rounding to nearest (as by Java's widening conversions), and values are
	 * read as floating point numbers (precise only if they are numbers of the
	 * given sort). Terms of other sorts yield unknown numbers.
	 *
	 * @param term   the term
	 * @param fp     the floating point sort
	 * @param origin the expression being encoded
	 *
	 * @return the converted term
	 */
	protected Term toFloat(
			Term term,
			Sort fp,
			SymbolicExpression origin) {
		return toFloat(term, fp, null, origin);
	}

	/**
	 * Converts the given term to a floating point number of the given sort, as
	 * {@link #toFloat(Term, Sort, SymbolicExpression)}, using the static type
	 * of the expression that produced the term (if known) to encode integer
	 * conversions more efficiently.
	 *
	 * @param term    the term to convert
	 * @param fp      the floating point sort
	 * @param operand the expression whose encoding is the term, or {@code null}
	 *                    if unknown
	 * @param origin  the expression being encoded
	 *
	 * @return the floating point number
	 */
	protected Term toFloat(
			Term term,
			Sort fp,
			SymbolicExpression operand,
			SymbolicExpression origin) {
		if (term.getSort().equals(fp))
			return term;
		String convert = "(_ to_fp " + fp.getExponentBits() + " " + fp.getSignificandBits() + ")";
		if (term.getSort().isFloatingPoint())
			return Term.app(convert, fp, rne(), term);
		if (term.getSort().equals(Sort.INT))
			return intToFloat(term, fp, bitsOf(operand), origin);
		if (term.getSort().equals(ValueSort.SORT)) {
			// values holding integers are converted as integers, and the
			// ones holding numbers of the given sort are read as they are
			Sort other = fp.equals(Sort.FLOAT64) ? Sort.FLOAT32 : Sort.FLOAT64;
			Term isInt = ValueSort.is(term, Sort.INT);
			Term isFloat = ValueSort.is(term, fp);
			Term isOther = ValueSort.is(term, other);
			symbols.exactOnlyIf(Term.or(isInt, isFloat, isOther), origin);
			return Term.ite(isInt, intToFloat(ValueSort.unwrap(term, Sort.INT), fp, bitsOf(operand), origin),
					Term.ite(isOther, Term.app(convert, fp, rne(), ValueSort.unwrap(term, other)),
							ValueSort.unwrap(term, fp)));
		}
		return symbols.fresh(fp, origin);
	}

	private static int bitsOf(
			SymbolicExpression operand) {
		if (operand != null && SortPolicy.isIntegral(operand.getStaticType())
				&& operand.getStaticType().asNumericType().getNBits() <= 32)
			return 32;
		return 64;
	}

	/**
	 * Converts the given integer to a floating point number of the given sort,
	 * rounding to the nearest one. The conversion goes through a bit-vector of
	 * the given width (smaller ones are much cheaper for solvers), since
	 * solvers often fail to decide conversions from real numbers: integers
	 * outside of the range of signed integers of that width yield an imprecise
	 * value.
	 *
	 * @param term   the integer
	 * @param fp     the floating point sort
	 * @param bits   the width of the bit-vector
	 * @param origin the expression being encoded
	 *
	 * @return the floating point number
	 */
	protected Term intToFloat(
			Term term,
			Sort fp,
			int bits,
			SymbolicExpression origin) {
		BigInteger bound = BigInteger.ONE.shiftLeft(bits - 1);
		Term inRange = Term.and(Term.app(">=", Sort.BOOL, term, Term.integer(bound.negate())),
				Term.app("<", Sort.BOOL, term, Term.integer(bound)));
		Term bv = Term.app("(_ int2bv " + bits + ")", Sort.bitVector(bits), term);
		Term converted = Term.app("(_ to_fp " + fp.getExponentBits() + " " + fp.getSignificandBits() + ")", fp,
				rne(), bv);
		return Term.ite(inRange, converted, symbols.fresh(fp, origin, inRange));
	}

	/**
	 * Converts the given floating point number to an integer of the given type,
	 * as by Java's narrowing conversions: NaN yields zero, numbers are rounded
	 * toward zero and saturated to the range of the type (of {@code int} for
	 * types narrower than {@code int}, whose values are then wrapped to the
	 * type).
	 *
	 * @param term the floating point number
	 * @param type the integral type
	 *
	 * @return the integer
	 */
	protected Term toIntegral(
			Term term,
			Type type) {
		boolean isLong = type.isNumericType() && type.asNumericType().is64Bits();
		int bits = isLong ? 64 : 32;
		BigInteger max = BigInteger.ONE.shiftLeft(bits - 1).subtract(BigInteger.ONE);
		BigInteger min = BigInteger.ONE.shiftLeft(bits - 1).negate();
		// the conversion goes through a bit-vector, since solvers often fail
		// to decide conversions through real numbers
		Sort bvSort = Sort.bitVector(bits);
		Term bv = Term.app("(_ fp.to_sbv " + bits + ")", bvSort, Term.value("RTZ", Sort.ROUNDING_MODE), term);
		Term unsigned = Term.app("bv2int", Sort.INT, bv);
		Term truncated = Term.ite(Term.app("bvslt", Sort.BOOL, bv, Term.value("(_ bv0 " + bits + ")", bvSort)),
				Term.app("-", Sort.INT, unsigned, Term.integer(BigInteger.ONE.shiftLeft(bits))), unsigned);
		// numbers beyond the bounds (including infinities) are saturated:
		// the bounds might not be representable, but no number lies between
		// them and their rounding
		String convert = "(_ to_fp " + term.getSort().getExponentBits() + " " + term.getSort().getSignificandBits()
				+ ")";
		Term fpMax = Term.app(convert, term.getSort(), rne(), Term.app("to_real", Sort.REAL, Term.integer(max)));
		Term fpMin = Term.app(convert, term.getSort(), rne(), Term.app("to_real", Sort.REAL, Term.integer(min)));
		Term result = Term.ite(Term.app("fp.isNaN", Sort.BOOL, term), Term.integer(0),
				Term.ite(Term.app("fp.geq", Sort.BOOL, term, fpMax), Term.integer(max),
						Term.ite(Term.app("fp.leq", Sort.BOOL, term, fpMin), Term.integer(min), truncated)));
		if (type.isNumericType() && (type.asNumericType().is8Bits() || type.asNumericType().is16Bits()))
			result = wrap(result, type.asNumericType().is8Bits() ? 8 : 16, type.asNumericType().isUnsigned());
		return result;
	}

	/**
	 * Yields the encoding of an expression that is precise only when the given
	 * guard holds (e.g., an operation encoded only over some of the values of
	 * its operands): the result is the given term if the guard holds, and an
	 * unknown, but deterministic, function of the given arguments otherwise
	 * (never precise, see
	 * {@link SymbolTable#apply(String, Sort, List, SymbolicExpression, Term)}).
	 * Encodings that are not valid for all the values of the operands must use
	 * this (or an equivalent construction) for soundness, rather than yielding
	 * the partial term directly.
	 *
	 * @param guard      the condition under which the term is precise
	 * @param term       the precise encoding of the expression, when the guard
	 *                       holds
	 * @param arguments  the arguments of the unknown function (usually, the
	 *                       operands of the expression)
	 * @param expression the expression being encoded
	 *
	 * @return the encoded term
	 */
	protected Term guarded(
			Term guard,
			Term term,
			List<Term> arguments,
			ValueExpression expression) {
		return Term.ite(guard, term,
				symbols.apply("$" + operatorOf(expression), term.getSort(), arguments, expression, guard));
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
			Object... params)
			throws SemanticException {
		return encodeValue(expression, subExpressions);
	}

	/**
	 * Encodes a value expression whose kind is not handled by the other methods
	 * of this encoder (e.g., an expression with an arbitrary number of operands
	 * defined by a frontend). This encoder knows no such expression, and yields
	 * an unconstrained value. Subclasses can override this to encode the
	 * expressions they know, invoking this method for the other ones.
	 *
	 * @param expression     the expression
	 * @param subExpressions the encodings of the sub-expressions, in the order
	 *                           they are visited by the expression
	 *
	 * @return the encoded term
	 *
	 * @throws SemanticException if the expression cannot be encoded
	 */
	protected Term encodeValue(
			ValueExpression expression,
			Term[] subExpressions)
			throws SemanticException {
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
			Object... params)
			throws SemanticException {
		return encodeConstant(expression);
	}

	/**
	 * Encodes a constant: integral numbers, booleans, strings and types are
	 * encoded, while other values (e.g., floating point numbers) yield an
	 * unconstrained value. Subclasses can override this to encode the values
	 * they know, or to encode some values differently, invoking this method for
	 * the other ones.
	 *
	 * @param expression the constant
	 *
	 * @return the encoded term
	 *
	 * @throws SemanticException if the expression cannot be encoded
	 */
	protected Term encodeConstant(
			Constant expression)
			throws SemanticException {
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
		if (value instanceof Double)
			return sorts.encodesFloats() ? FloatingPoints.literal((Double) value) : opaque(expression);
		if (value instanceof Float)
			return sorts.encodesFloats() ? FloatingPoints.literal((Float) value) : opaque(expression);
		if (value instanceof Character)
			return Term.integer((Character) value);
		return havoc(expression);
	}

	@Override
	public Term visit(
			Identifier expression,
			Object... params) {
		Term term = symbols.constant(expression, sorts.sortOf(expression));
		if (expression instanceof FreshValue && !sorts.encodesFloats() && term.getSort().equals(ValueSort.SORT)
				&& sorts.holdsOnlyFloats(expression))
			// floating point numbers that are not encoded are opaque values,
			// so that operations over them are never precise: this only
			// constrains unknown values (variables get them from initial
			// states, from fresh values, and from the expressions producing
			// floating point numbers, see opaque), as requiring it on any
			// identifier would exclude executions where an imprecise
			// encoding (e.g., of a library function) yields another kind of
			// value
			symbols.require(ValueSort.isOpaque(term));
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
			Object... params)
			throws SemanticException {
		return encodeUnary(expression, symbols.share(arg));
	}

	/**
	 * Encodes a unary expression, given the encoding of its argument. Unknown
	 * operators yield an unconstrained value. Subclasses can override this to
	 * encode the operators they know (e.g., the ones defined by a frontend), or
	 * to encode some operators differently, invoking this method for the other
	 * ones.
	 *
	 * @param expression the expression
	 * @param arg        the encoding of the argument
	 *
	 * @return the encoded term
	 *
	 * @throws SemanticException if the expression cannot be encoded
	 */
	protected Term encodeUnary(
			UnaryExpression expression,
			Term arg)
			throws SemanticException {
		UnaryOperator op = expression.getOperator();
		if (op instanceof HeapOperators.NewReference)
			return ValueSort.reference(coerce(arg, Sort.INT, expression));
		if (op instanceof LogicalNegation)
			return Term.not(coerce(arg, Sort.BOOL, expression));
		if (op instanceof NumericNegation)
			return arg.getSort().isFloatingPoint() ? Term.app("fp.neg", arg.getSort(), arg)
					: arithmetic(expression, List.of(arg), args -> neg(args.get(0)));
		if (op instanceof NumericAbs)
			return arg.getSort().isFloatingPoint() ? Term.app("fp.abs", arg.getSort(), arg)
					: arithmetic(expression, List.of(arg), args -> abs(args.get(0)));
		if (op instanceof BitwiseNegation)
			// ~x == -x - 1 in two's complement
			return arithmetic(expression, List.of(arg),
					args -> Term.app("-", Sort.INT, neg(args.get(0)), Term.integer(1)));
		if (op instanceof StringLength)
			return Term.app("str.len", Sort.INT, coerce(arg, Sort.STRING, expression));
		return unsupported(expression, arg);
	}

	@Override
	public Term visit(
			BinaryExpression expression,
			Term left,
			Term right,
			Object... params)
			throws SemanticException {
		return encodeBinary(expression, symbols.share(left), symbols.share(right));
	}

	/**
	 * Encodes a binary expression, given the encodings of its operands. Unknown
	 * operators yield an unconstrained value. Subclasses can override this to
	 * encode the operators they know (e.g., the ones defined by a frontend), or
	 * to encode some operators differently, invoking this method for the other
	 * ones.
	 *
	 * @param expression the expression
	 * @param left       the encoding of the left operand
	 * @param right      the encoding of the right operand
	 *
	 * @return the encoded term
	 *
	 * @throws SemanticException if the expression cannot be encoded
	 */
	protected Term encodeBinary(
			BinaryExpression expression,
			Term left,
			Term right)
			throws SemanticException {
		BinaryOperator op = expression.getOperator();

		if (op instanceof HeapOperators.CellKey)
			return CellKeySort.key(referenceKey(left, expression), coerce(right, Sort.INT, expression));
		if (op instanceof HeapOperators.DefinedRead) {
			// the value is known only if the variable has been written
			symbols.exactOnlyIfWritten(coerce(right, Sort.BOOL, expression), expression.getLeft());
			return left;
		}
		if (op instanceof HeapOperators.RuntimeTypeIn)
			return runtimeTypeIn(left, right, ((HeapOperators.RuntimeTypeIn) op).getTypes(), expression);
		if (op instanceof HeapOperators.WrittenAt)
			// locations of maps that do not track writes are never written
			return left.getSort().isArray() && left.getSort().getElement().equals(Sort.BOOL)
					? Term.app("select", Sort.BOOL, left, heapKey(right, expression))
					: Term.FALSE;

		if (op instanceof LogicalAnd)
			return Term.and(coerce(left, Sort.BOOL, expression), coerce(right, Sort.BOOL, expression));
		if (op instanceof LogicalOr)
			return Term.or(coerce(left, Sort.BOOL, expression), coerce(right, Sort.BOOL, expression));
		Sort fp = floatingSort(left, right);
		boolean floatOperation = op instanceof ComparisonEq || op instanceof ComparisonNe
				|| op instanceof ComparisonLt || op instanceof ComparisonLe || op instanceof ComparisonGt
				|| op instanceof ComparisonGe || op instanceof AdditionOperator || op instanceof SubtractionOperator
				|| op instanceof MultiplicationOperator || op instanceof DivisionOperator;
		if (fp != null && floatOperation) {
			// operations over floating point numbers, where the other operand
			// is converted to a floating point number as by numeric promotion
			Term l = toFloat(left, fp, expression.getLeft(), expression);
			Term r = toFloat(right, fp, expression.getRight(), expression);
			if (op instanceof ComparisonEq)
				return Term.app("fp.eq", Sort.BOOL, l, r);
			if (op instanceof ComparisonNe)
				return Term.not(Term.app("fp.eq", Sort.BOOL, l, r));
			if (op instanceof ComparisonLt)
				return Term.app("fp.lt", Sort.BOOL, l, r);
			if (op instanceof ComparisonLe)
				return Term.app("fp.leq", Sort.BOOL, l, r);
			if (op instanceof ComparisonGt)
				return Term.app("fp.gt", Sort.BOOL, l, r);
			if (op instanceof ComparisonGe)
				return Term.app("fp.geq", Sort.BOOL, l, r);
			if (op instanceof AdditionOperator)
				return Term.app("fp.add", fp, rne(), l, r);
			if (op instanceof SubtractionOperator)
				return Term.app("fp.sub", fp, rne(), l, r);
			if (op instanceof MultiplicationOperator)
				return Term.app("fp.mul", fp, rne(), l, r);
			if (op instanceof DivisionOperator)
				return Term.app("fp.div", fp, rne(), l, r);
		}

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
			return Term.app("str.++", Sort.STRING, asString(left, expression), asString(right, expression));
		if (op instanceof StringContains)
			return Term.app("str.contains", Sort.BOOL, asString(left, expression), asString(right, expression));
		if (op instanceof StringStartsWith)
			return Term.app("str.prefixof", Sort.BOOL, asString(right, expression), asString(left, expression));
		if (op instanceof StringEndsWith)
			return Term.app("str.suffixof", Sort.BOOL, asString(right, expression), asString(left, expression));
		if (op instanceof StringIsPrefixOf)
			return Term.app("str.prefixof", Sort.BOOL, asString(left, expression), asString(right, expression));
		if (op instanceof StringIsSuffixOf)
			return Term.app("str.suffixof", Sort.BOOL, asString(left, expression), asString(right, expression));
		if (op instanceof StringIsSubstringOf)
			return Term.app("str.contains", Sort.BOOL, asString(right, expression), asString(left, expression));
		if (op instanceof StringEquals)
			return Term.eq(asString(left, expression), asString(right, expression));
		if (op instanceof StringIndexOf)
			return Term.app("str.indexof", Sort.INT, asString(left, expression), asString(right, expression),
					Term.integer(0));
		if (op instanceof StringSubstringToEnd) {
			Term s = asString(left, expression);
			Term from = coerce(right, Sort.INT, expression);
			return Term.app("str.substr", Sort.STRING, s, from,
					Term.app("-", Sort.INT, Term.app("str.len", Sort.INT, s), from));
		}

		if (op instanceof BitwiseAnd || op instanceof BitwiseOr || op instanceof BitwiseXor
				|| op instanceof BitwiseShiftLeft || op instanceof BitwiseShiftRight
				|| op instanceof BitwiseUnsignedShiftRight)
			return bitwise(expression, left, right);

		if (op instanceof TypeCheck) {
			Type target = tokenOf(expression.getRight());
			// checks against types that are not unique cannot be encoded
			return target != null ? typeCheck(left, target, expression) : havoc(expression);
		}

		if (op instanceof TypeCast || op instanceof TypeConv) {
			if (!sorts.encodesFloats() && SortPolicy.isFloat(expression.getStaticType()))
				// conversions to floating point numbers that are not encoded
				return opaque(expression);
			Sort target = sorts.sortOf(expression.getStaticType());
			if (target.isFloatingPoint() && (left.getSort().equals(Sort.INT) || left.getSort().isFloatingPoint()
					|| left.getSort().equals(ValueSort.SORT)))
				return toFloat(left, target, expression.getLeft(), expression);
			if (target.equals(Sort.INT) && left.getSort().isFloatingPoint())
				return toIntegral(left, expression.getStaticType());
			if (target.equals(Sort.INT) && left.getSort().equals(ValueSort.SORT) && sorts.encodesFloats()) {
				// values holding floating point numbers are converted as
				// floating point numbers, and the ones holding integers are
				// read as they are
				Term isInt = ValueSort.is(left, Sort.INT);
				Term is32 = ValueSort.is(left, Sort.FLOAT32);
				Term is64 = ValueSort.is(left, Sort.FLOAT64);
				symbols.exactOnlyIf(Term.or(isInt, is32, is64), expression);
				return Term.ite(is64, toIntegral(ValueSort.unwrap(left, Sort.FLOAT64), expression.getStaticType()),
						Term.ite(is32, toIntegral(ValueSort.unwrap(left, Sort.FLOAT32), expression.getStaticType()),
								ValueSort.unwrap(left, Sort.INT)));
			}
			// the value is preserved if it already has the target sort, and
			// read as the target sort otherwise
			Term converted = coerce(left, target, expression);
			if (op instanceof TypeConv && target.equals(Sort.INT) && converted.getSort().equals(Sort.INT)) {
				// narrowing conversions between integral types keep the
				// lowest bits of the value (widening ones do not change it)
				int bits = integralBits(expression.getStaticType());
				if (bits > 0)
					converted = wrap(converted, bits, expression.getStaticType().asNumericType().isUnsigned());
			}
			return converted;
		}

		return unsupported(expression, left, right);
	}

	@Override
	public Term visit(
			TernaryExpression expression,
			Term left,
			Term middle,
			Term right,
			Object... params)
			throws SemanticException {
		return encodeTernary(expression, symbols.share(left), symbols.share(middle), symbols.share(right));
	}

	/**
	 * Encodes a ternary expression, given the encodings of its operands.
	 * Unknown operators yield an unconstrained value. Subclasses can override
	 * this to encode the operators they know (e.g., the ones defined by a
	 * frontend), or to encode some operators differently, invoking this method
	 * for the other ones.
	 *
	 * @param expression the expression
	 * @param left       the encoding of the left operand
	 * @param middle     the encoding of the middle operand
	 * @param right      the encoding of the right operand
	 *
	 * @return the encoded term
	 *
	 * @throws SemanticException if the expression cannot be encoded
	 */
	protected Term encodeTernary(
			TernaryExpression expression,
			Term left,
			Term middle,
			Term right)
			throws SemanticException {
		TernaryOperator op = expression.getOperator();
		if (op instanceof HeapOperators.ReferenceRead)
			return read(left, middle, heapKey(right, expression), expression);
		if (op instanceof HeapOperators.ReferenceStore)
			return store(left, heapKey(middle, expression), right, expression);
		if (op instanceof StringSubstring) {
			Term from = coerce(middle, Sort.INT, expression);
			Term to = coerce(right, Sort.INT, expression);
			return Term.app("str.substr", Sort.STRING, asString(left, expression), from,
					Term.app("-", Sort.INT, to, from));
		}
		if (op instanceof StringReplace)
			return Term.app("str.replace", Sort.STRING, asString(left, expression), asString(middle, expression),
					asString(right, expression));
		if (op instanceof StringIndexOfFromIndex)
			return Term.app("str.indexof", Sort.INT, asString(left, expression), asString(middle, expression),
					coerce(right, Sort.INT, expression));
		if (op instanceof StringStartsWithFromIndex) {
			Term s = asString(left, expression);
			Term from = coerce(right, Sort.INT, expression);
			Term suffix = Term.app("str.substr", Sort.STRING, s, from,
					Term.app("-", Sort.INT, Term.app("str.len", Sort.INT, s), from));
			return Term.app("str.prefixof", Sort.BOOL, asString(middle, expression), suffix);
		}
		return unsupported(expression, left, middle, right);
	}

	/**
	 * Reads the given term as a string, as an operand of a string operation: if
	 * the term is a value, the operation can be evaluated only if it is a
	 * string ({@link SymbolTable#require(Term)}).
	 *
	 * @param term   the term
	 * @param origin the expression using the term as a string
	 *
	 * @return the string term
	 */
	protected Term asString(
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
	 * opaque values is not modeled, and equality between them does not coincide
	 * with their identity (e.g., a floating point NaN is not equal to itself,
	 * and {@code 1 == 1.0} holds). Such equalities are thus an unknown, but
	 * deterministic, function of the operands (never precise).
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
		// floating point numbers are not equal by identity (e.g., NaN)
		Term modeled = sorts.encodesFloats()
				? Term.not(Term.or(ValueSort.isOpaque(l), ValueSort.isOpaque(r),
						ValueSort.is(l, Sort.FLOAT32), ValueSort.is(r, Sort.FLOAT32),
						ValueSort.is(l, Sort.FLOAT64), ValueSort.is(r, Sort.FLOAT64)))
				: Term.not(Term.or(ValueSort.isOpaque(l), ValueSort.isOpaque(r)));
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
	 * Encodes whether the runtime type of the given reference, as recorded by
	 * the given map of runtime types, is one of the given types: values that
	 * are not references have none of them, and the test is precise only if the
	 * runtime type of the object is known (see
	 * {@link it.unive.lisa.triage.model.HeapModel#TYPES}).
	 */
	private Term runtimeTypeIn(
			Term map,
			Term reference,
			Set<Type> types,
			SymbolicExpression origin) {
		if (!map.getSort().isArray() || !reference.getSort().equals(ValueSort.SORT))
			return symbols.fresh(Sort.BOOL, origin);
		Term isReference = ValueSort.isReference(reference);
		Term type = Term.app("select", ValueSort.SORT, map, ValueSort.referenceId(reference));
		symbols.exactOnlyIf(Term.or(Term.not(isReference),
				Term.not(Term.eq(type, ValueSort.typeToken(HeapModel.UNKNOWN_TYPE)))), origin);
		List<Term> matches = new ArrayList<>();
		for (Type t : types)
			matches.add(Term.eq(type, ValueSort.typeToken(t)));
		return Term.and(isReference, matches.isEmpty() ? Term.FALSE : Term.or(matches.toArray(Term[]::new)));
	}

	private Term heapKey(
			Term key,
			SymbolicExpression origin) {
		if (key.getSort().equals(CellKeySort.SORT))
			return key;
		return referenceKey(key, origin);
	}

	/**
	 * Encodes the key of a heap map for the given reference: the identifier of
	 * the reference (precise only if the value is a reference, e.g., not null).
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
	 * Encodes the value of the given map at the given key (unconstrained if the
	 * map is not an array).
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
	 * Yields the type denoted by the given type token: a constant whose value
	 * is the type, or whose static type is a {@link TypeTokenType} for a single
	 * type (whatever its value). Yields {@code null} if the expression does not
	 * denote a unique type.
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

	/**
	 * Signals that the given term is an operand of a numeric operation: if the
	 * term is a value, the operation can be evaluated only if it is a number
	 * ({@link SymbolTable#require(Term)}).
	 *
	 * @param term the term
	 */
	protected void requireNumeric(
			Term term) {
		if (term.getSort().equals(ValueSort.SORT))
			symbols.require(ValueSort.isNumeric(term, sorts.encodesFloats()));
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
	 * of all the possible runtime types of the operands. Results of the
	 * operators of LiSA with a fixed bit width (e.g., {@link Numeric32BitAdd})
	 * are wrapped to their range.
	 *
	 * @param expression the expression being encoded
	 * @param operands   the encodings of the operands
	 * @param op         the function building the result from the operands,
	 *                       read as integers
	 *
	 * @return the encoded term
	 */
	protected Term arithmetic(
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

	/**
	 * Encodes a bitwise operation over integers, through bit-vectors as wide as
	 * the (promoted) type of the operation: 32 bits, or 64 if it involves a
	 * 64-bits integer. Bitwise operations over booleans are logical ones. As in
	 * Java, shift distances are taken modulo the width.
	 */
	private Term bitwise(
			BinaryExpression expression,
			Term left,
			Term right) {
		BinaryOperator op = expression.getOperator();
		boolean shift = op instanceof BitwiseShiftLeft || op instanceof BitwiseShiftRight
				|| op instanceof BitwiseUnsignedShiftRight;
		if (!shift && left.getSort().equals(Sort.BOOL) && right.getSort().equals(Sort.BOOL)) {
			if (op instanceof BitwiseAnd)
				return Term.and(left, right);
			if (op instanceof BitwiseOr)
				return Term.or(left, right);
			return Term.not(Term.eq(left, right));
		}

		// the width of the promoted type of the operands (only the left one
		// for shifts), or of the result if they are not known
		int bits = Math.max(integralBits(expression.getLeft().getStaticType()),
				shift ? 0 : integralBits(expression.getRight().getStaticType()));
		if (bits == 0)
			bits = integralBits(expression.getStaticType());
		if (bits == 0)
			return unsupported(expression, left, right);
		int width = Math.max(bits, 32);
		Sort bv = Sort.bitVector(width);
		String toBits = "(_ int2bv " + width + ")";
		return arithmetic(expression, List.of(left, right), args -> {
			Term l = Term.app(toBits, bv, args.get(0));
			Term r = shift
					? Term.app(toBits, bv, Term.app("mod", Sort.INT, args.get(1), Term.integer(width)))
					: Term.app(toBits, bv, args.get(1));
			String function = op instanceof BitwiseAnd ? "bvand"
					: op instanceof BitwiseOr ? "bvor"
							: op instanceof BitwiseXor ? "bvxor"
									: op instanceof BitwiseShiftLeft ? "bvshl"
											: op instanceof BitwiseShiftRight ? "bvashr" : "bvlshr";
			Term unsigned = Term.app("bv2int", Sort.INT, Term.app(function, bv, l, r));
			// the bits are read in two's complement
			BigInteger half = BigInteger.ONE.shiftLeft(width - 1);
			return Term.ite(Term.app(">=", Sort.BOOL, unsigned, Term.integer(half)),
					Term.app("-", Sort.INT, unsigned, Term.integer(BigInteger.ONE.shiftLeft(width))), unsigned);
		});
	}

	/**
	 * Yields the number of bits of the given type, if its values are integers
	 * (see {@link SortPolicy#isIntegral(Type)}) and its size is known, or 0
	 * otherwise.
	 */
	private static int integralBits(
			Type type) {
		if (!SortPolicy.isIntegral(type))
			return 0;
		int bits = type.asNumericType().getNBits();
		return bits == 8 || bits == 16 || bits == 32 || bits == 64 ? bits : 0;
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
	 * Wraps the given integer to the range of the integers of the given bit
	 * width, as done by overflowing operations.
	 *
	 * @param t        the integer
	 * @param bits     the bit width
	 * @param unsigned whether or not the integers are unsigned
	 *
	 * @return the wrapped integer
	 */
	protected static Term wrap(
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
