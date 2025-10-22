package it.unive.lisa.util.octagon;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.ExpressionVisitor;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.heap.AccessChild;
import it.unive.lisa.symbolic.heap.HeapDereference;
import it.unive.lisa.symbolic.heap.HeapExpression;
import it.unive.lisa.symbolic.heap.HeapReference;
import it.unive.lisa.symbolic.heap.MemoryAllocation;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.PushAny;
import it.unive.lisa.symbolic.value.PushInv;
import it.unive.lisa.symbolic.value.Skip;
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
import it.unive.lisa.symbolic.value.operator.binary.NumericNonOverflowingSub;
import it.unive.lisa.symbolic.value.operator.unary.LogicalNegation;
import it.unive.lisa.symbolic.value.operator.unary.NumericNegation;
import it.unive.lisa.type.Type;
import it.unive.lisa.util.numeric.MathNumber;
import java.util.Set;

/**
 * An ExpressionVisitor that normalizes boolean expressions into canonical forms
 * to be used in the octagon domain numeric analysis It implements the following
 * normalizations:
 * <h3>1. Elimination of the NOT operator (De Morgan's Laws)</h3>
 * <ul>
 * <li>{@code not(t1 and t2)} → {@code (not t1) or (not t2)}</li>
 * <li>{@code not(t1 or t2)} → {@code (not t1) and (not t2)}</li>
 * <li>{@code not(not t)} → {@code t}</li>
 * <li>{@code not(e1 ≤ e2)} → {@code e1 > e2}</li>
 * <li>{@code not(e1 < e2)} → {@code e1 ≥ e2}</li>
 * <li>{@code not(e1 > e2)} → {@code e1 ≤ e2}</li>
 * <li>{@code not(e1 ≥ e2)} → {@code e1 < e2}</li>
 * <li>{@code not(e1 = e2)} → {@code e1 ≠ e2}</li>
 * <li>{@code not(e1 ≠ e2)} → {@code e1 = e2}</li>
 * </ul>
 * <h3>2. Normalization of atomic comparisons to the form {@code e <= 0}</h3>
 * <ul>
 * <li>{@code a > b} → {@code b - a < 0}</li>
 * <li>{@code a >= b} → {@code b - a <= 0}</li>
 * <li>{@code a < b} → {@code a - b < 0}</li>
 * <li>{@code a <= b} → {@code a - b <= 0}</li>
 * <li>{@code a == b} → {@code (a - b == 0) and (-a + b == 0)} →
 * {@code (a - b <= 0) and (b - a <= 0)}</li>
 * <li>{@code a != b} → {@code (a - b != 0) or (-a + b != 0)}</li>
 * </ul>
 * <h3>3. Special handling for integer types</h3> For integer numerical domains
 * (when {@code type.isInteger() == true}):
 * <ul>
 * <li>{@code e < 0} → {@code e + 1 <= 0}</li>
 * <li>{@code e > 0} → {@code -e + 1 <= 0}</li>
 * <li>{@code e != 0} → {@code (e + 1 <= 0) or (-e + 1 <= 0)}</li>
 * </ul>
 * 
 * @author <a href="mailto:lorenzo.mioso@studenti.univr.it">Lorenzo Mioso </a>
 * @author <a href="mailto:marjo.shytermeja@studenti.univr.it">Marjo
 *             Shytermeja</a>
 */
public class BooleanExpressionNormalizer implements ExpressionVisitor<SymbolicExpression> {

	/**
	 * The current program point, used for creating new expressions.
	 */
	private final CodeLocation location;
	private final ProgramPoint pp;
	private final SemanticOracle oracle;

	/**
	 * Constructs a new boolean expression normalizer.
	 * 
	 * @param location the current program point
	 * @param pp       the program point
	 * @param oracle   the semantic oracle
	 */
	public BooleanExpressionNormalizer(
			CodeLocation location,
			ProgramPoint pp,
			SemanticOracle oracle) {
		this.location = location;
		this.pp = pp;
		this.oracle = oracle;
	}

	/**
	 * Constructs a new boolean expression normalizer without known variables.
	 * 
	 * @param location the current program point
	 */
	public BooleanExpressionNormalizer(
			CodeLocation location) {
		this.location = location;
		this.pp = null;
		this.oracle = null;
	}

	/**
	 * Normalizes a symbolic expression using this visitor.
	 * 
	 * @param expression the expression to normalize
	 * @param location   the current program point
	 * @param pp         the program point
	 * @param oracle     the semantic oracle
	 * 
	 * @return the normalized expression
	 * 
	 * @throws SemanticException if an error occurs during normalization
	 */
	public static SymbolicExpression normalize(
			SymbolicExpression expression,
			CodeLocation location,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		BooleanExpressionNormalizer normalizer = new BooleanExpressionNormalizer(location, pp, oracle);
		SymbolicExpression result = expression.accept(normalizer);
		return result;
	}

	/**
	 * Normalizes a symbolic expression using this visitor.
	 * 
	 * @param expression the expression to normalize
	 * @param location   the current program point
	 * 
	 * @return the normalized expression
	 * 
	 * @throws SemanticException if an error occurs during normalization
	 */
	public static SymbolicExpression normalize(
			SymbolicExpression expression,
			CodeLocation location)
			throws SemanticException {
		BooleanExpressionNormalizer normalizer = new BooleanExpressionNormalizer(location);
		SymbolicExpression result = expression.accept(normalizer);
		return result;
	}

	private Constant zero(
			Type t) {
		return new Constant(t, new MathNumber(0), location);
	}

	private Constant one(
			Type t) {
		return new Constant(t, new MathNumber(1), location);
	}

	private Constant inc(
			Constant c,
			int k) {
		if (c.getValue() instanceof Integer) {
			Integer v = (Integer) c.getValue();
			return new Constant(c.getStaticType(), v + k, location);
		}
		MathNumber v = (MathNumber) c.getValue();
		return new Constant(c.getStaticType(), v.add(new MathNumber(k)), location);
	}

	private UnaryExpression neg(
			SymbolicExpression e) {
		return new UnaryExpression(e.getStaticType(), e, NumericNegation.INSTANCE, location);
	}

	private BinaryExpression add(
			SymbolicExpression a,
			SymbolicExpression b) {
		return new BinaryExpression(a.getStaticType(), a, b, NumericNonOverflowingAdd.INSTANCE, location);
	}

	private BinaryExpression sub(
			SymbolicExpression a,
			SymbolicExpression b) {
		return new BinaryExpression(a.getStaticType(), a, b, NumericNonOverflowingSub.INSTANCE, location);
	}

	/**
	 * Creates a simplified subtraction that handles special cases:
	 * <ul>
	 * <li>a - 0 => a</li>
	 * <li>0 - a => -a</li>
	 * <li>a - (-c) => a + c (converts subtraction of a negative into an
	 * addition)</li>
	 * </ul>
	 * 
	 * @param a the first operand
	 * @param b the second operand
	 * 
	 * @return the simplified expression
	 */
	private SymbolicExpression simplifiedSub(
			SymbolicExpression a,
			SymbolicExpression b) {
		// Check if b is a constant
		if (b instanceof Constant) {
			Object val = ((Constant) b).getValue();
			MathNumber num;
			if (val instanceof MathNumber) {
				num = (MathNumber) val;
			} else if (val instanceof Integer) {
				num = new MathNumber((Integer) val);
			} else {
				return sub(a, b);
			}

			// a - 0 => a
			if (num.isZero()) {
				return a;
			}

			// a - (-c) => a + c (convert to positive constant)
			if (num.compareTo(MathNumber.ZERO) < 0) {
				Constant positiveConst = new Constant(b.getStaticType(), num.multiply(new MathNumber(-1)),
						location);
				return add(a, positiveConst);
			}
		}

		// Check if a is zero constant
		if (a instanceof Constant) {
			Object val = ((Constant) a).getValue();
			if ((val instanceof MathNumber && ((MathNumber) val).isZero()
					|| val instanceof Integer && (Integer) val == 0)) {
				// 0 - b => -b
				return neg(b);
			}
		}

		return sub(a, b);
	}

	private SymbolicExpression normalizeLeZeroLeft(
			SymbolicExpression left)
			throws SemanticException {
		if (left instanceof UnaryExpression) {
			UnaryExpression unary = (UnaryExpression) left;
			if (unary.getOperator() instanceof NumericNegation) {
				SymbolicExpression inner = unary.getExpression();
				if (inner instanceof BinaryExpression) {
					BinaryExpression innerBin = (BinaryExpression) inner;

					if (innerBin.getOperator() instanceof NumericNonOverflowingAdd) {
						SymbolicExpression l = innerBin.getLeft();
						SymbolicExpression r = innerBin.getRight();
						if (r instanceof Constant) {
							SymbolicExpression reshaped = simplifiedSub(neg(l), r);
							if (shouldTightenForInteger(r)) {
								Constant tightened = inc((Constant) r, -1);
								reshaped = simplifiedSub(neg(l), tightened);
							}
							return reshaped;
						}
						if (l instanceof Constant) {
							SymbolicExpression reshaped = simplifiedSub(neg(r), l);
							if (shouldTightenForInteger(l)) {
								Constant tightened = inc((Constant) l, -1);
								reshaped = simplifiedSub(neg(r), tightened);
							}
							return reshaped;
						}
					}
				}
			}
		}

		return left;
	}

	private boolean shouldTightenForInteger(
			SymbolicExpression constantExpr)
			throws SemanticException {
		if (!(constantExpr instanceof Constant))
			return false;
		Constant constant = (Constant) constantExpr;
		if (!isIntegerType(constant))
			return false;
		Object value = constant.getValue();
		if (!(value instanceof MathNumber))
			return false;
		MathNumber num = (MathNumber) value;
		return num.compareTo(MathNumber.ZERO) > 0;
	}

	private BinaryExpression cmp(
			SymbolicExpression l,
			SymbolicExpression r,
			BinaryOperator op,
			Type resultType) {
		return new BinaryExpression(resultType, l, r, op, location);
	}

	private boolean isIntegerPair(
			SymbolicExpression a,
			SymbolicExpression b)
			throws SemanticException {
		return isIntegerType(a) && isIntegerType(b);
	}

	@Override
	public SymbolicExpression visit(
			BinaryExpression expression,
			SymbolicExpression left,
			SymbolicExpression right,
			Object... params)
			throws SemanticException {

		BinaryOperator operator = expression.getOperator();

		// Handle logical AND/OR operators
		if (operator instanceof LogicalAnd) {
			// For AND, we keep the original expression with normalized operands
			return new BinaryExpression(expression.getStaticType(), left, right, operator, location);
		} else if (operator instanceof LogicalOr) {
			// For OR, we keep the original expression with normalized operands
			return new BinaryExpression(expression.getStaticType(), left, right, operator, location);
		}

		// Transform comparison operators into normalized forms
		if (operator instanceof ComparisonGt) {
			// a > b becomes b - a < 0

			if (left instanceof BinaryExpression && right instanceof Constant) {
				// case x + y > c
				BinaryExpression leftBinExpr = (BinaryExpression) left;
				Constant rightConst = (Constant) right;

				// Handle different binary operators in the left expression
				if (leftBinExpr.getOperator() instanceof NumericNonOverflowingAdd) {
					// (x + y) > c
					// - For integers: -(x + y) + (c + 1) <= 0
					// - For non-integers: (-x) + (-y) [+ c] <= 0 (relax < to
					// <=)

					if (isIntegerType(left) && isIntegerType(rightConst)) {
						// For integers: (x + y) > c becomes -(x + y) + (c + 1)
						// <= 0
						Constant incrementedConst = inc(rightConst, 1);
						BinaryExpression finalSum = add(neg(left), incrementedConst);
						return cmp(finalSum, zero(left.getStaticType()), ComparisonLe.INSTANCE,
								expression.getStaticType());
					} else {
						// For non-integers: distribute negation over addition
						// to match expected shape
						// (x + y) > c => (-x + -y [+ c]) <= 0
						SymbolicExpression nx = neg(leftBinExpr.getLeft());
						SymbolicExpression ny = neg(leftBinExpr.getRight());
						SymbolicExpression sumNegs = add(nx, ny);
						// If c != 0, keep it on the left side as +c; for c ==
						// 0, avoid adding +0
						SymbolicExpression lhs = ((rightConst.getValue().equals(new MathNumber(0)))
								? sumNegs
								: (SymbolicExpression) add(sumNegs, rightConst));
						return cmp(lhs, zero(left.getStaticType()), ComparisonLe.INSTANCE,
								expression.getStaticType());
					}
				} else if (leftBinExpr.getOperator() instanceof NumericNonOverflowingSub) {
					// (x - y) > c becomes (x - y) - c > 0, then normalize as
					// -(x - y - c) < 0

					// Create (x - y - c)
					SymbolicExpression leftMinusRight = simplifiedSub(left, right);

					if (isIntegerType(left) && isIntegerType(rightConst)) {
						// For integers: (x - y) > c becomes -(x - y - c) + 1 <=
						// 0
						UnaryExpression negatedDiff = neg(leftMinusRight);
						BinaryExpression negatedDiffPlusOne = add(negatedDiff, one(left.getStaticType()));
						return cmp(negatedDiffPlusOne, zero(left.getStaticType()), ComparisonLe.INSTANCE,
								expression.getStaticType());
					} else {
						// For non-integers: relax < to <=
						return cmp(neg(leftMinusRight), zero(left.getStaticType()), ComparisonLe.INSTANCE,
								expression.getStaticType());
					}
				} else {
					// For other binary operators, use general transformation

					if (isIntegerType(left) && isIntegerType(rightConst)) {
						// General case for integers: expr > c becomes -expr +
						// (c + 1) <= 0
						Constant incrementedConst = inc(rightConst, 1);
						return cmp(add(neg(left), incrementedConst), zero(left.getStaticType()),
								ComparisonLe.INSTANCE, expression.getStaticType());
					} else {
						// General case for non-integers: relax < to <=
						return cmp(add(neg(left), right), zero(left.getStaticType()), ComparisonLe.INSTANCE,
								expression.getStaticType());
					}
				}

			}

			if (left instanceof Variable && right instanceof Constant) {
				Constant rightConst = (Constant) right;

				// integer case, if a > 5 it becomes -a + 6 <= 0 (increment for
				// >)
				if (isIntegerType(left)
						&& isIntegerType(rightConst)) {
					// a > c becomes -a + (c+1) <= 0 for integers
					Constant incrementedConst = inc(rightConst, 1);
					return cmp(add(neg(left), incrementedConst), zero(left.getStaticType()), ComparisonLe.INSTANCE,
							expression.getStaticType());
				}

				// Special case: a > c becomes -a + c <= 0
				return cmp(add(neg(left), right), zero(left.getStaticType()), ComparisonLe.INSTANCE,
						expression.getStaticType());
			}

			if (left instanceof Identifier && right instanceof Identifier) {
				boolean isInteger = isIntegerType(left) && isIntegerType(right);
				// Integer: use LT (which will be converted to +1 <= 0)
				// Non-integer: relax to LE
				return createNormalizedComparison(right, left,
						isInteger ? ComparisonLt.INSTANCE : ComparisonLe.INSTANCE,
						expression.getStaticType(), isInteger);
			}

			throw new SemanticException("ComparisonGt not handled for non-Identifier operands");
		} else if (operator instanceof ComparisonGe) {

			// Special cases when comparing with a constant 0
			if (right instanceof Constant && ((Constant) right).getValue().equals(new MathNumber(0))) {
				// Handle (-a - b) >= 0 -> (a + b) <= 0 (first to avoid the
				// generic e1-e2
				// case)
				if (left instanceof BinaryExpression
						&& ((BinaryExpression) left).getOperator() instanceof NumericNonOverflowingSub) {
					BinaryExpression lb = (BinaryExpression) left;
					if (lb.getLeft() instanceof UnaryExpression
							&& ((UnaryExpression) lb.getLeft()).getOperator() instanceof NumericNegation) {
						SymbolicExpression a = ((UnaryExpression) lb.getLeft()).getExpression();
						SymbolicExpression b = lb.getRight();
						return cmp(add(a, b), zero(a.getStaticType()), ComparisonLe.INSTANCE,
								expression.getStaticType());
					}
				}

				// Handle (-a + b) >= 0 -> (a - b) <= 0 and (a + -b) >= 0 -> (b
				// - a) <= 0
				if (left instanceof BinaryExpression
						&& ((BinaryExpression) left).getOperator() instanceof NumericNonOverflowingAdd) {
					BinaryExpression lb = (BinaryExpression) left;
					SymbolicExpression l = lb.getLeft();
					SymbolicExpression r = lb.getRight();
					if (l instanceof UnaryExpression
							&& ((UnaryExpression) l).getOperator() instanceof NumericNegation) {
						SymbolicExpression a = ((UnaryExpression) l).getExpression();
						return cmp(simplifiedSub(a, r), zero(a.getStaticType()), ComparisonLe.INSTANCE,
								expression.getStaticType());
					}
					if (r instanceof UnaryExpression
							&& ((UnaryExpression) r).getOperator() instanceof NumericNegation) {
						SymbolicExpression b = ((UnaryExpression) r).getExpression();
						return cmp(simplifiedSub(b, l), zero(b.getStaticType()), ComparisonLe.INSTANCE,
								expression.getStaticType());
					}
				}

				// Handle (e1 - e2) >= 0 -> (e2 - e1) <= 0 (generic case)
				if (left instanceof BinaryExpression
						&& ((BinaryExpression) left).getOperator() instanceof NumericNonOverflowingSub) {
					BinaryExpression lb = (BinaryExpression) left;
					SymbolicExpression e1 = lb.getLeft();
					SymbolicExpression e2 = lb.getRight();
					return cmp(simplifiedSub(e2, e1), zero(e1.getStaticType()), ComparisonLe.INSTANCE,
							expression.getStaticType());
				}
			}

			if (left instanceof Variable && right instanceof Constant) {
				// x >= c becomes -x + c <= 0
				return cmp(add(neg(left), right), zero(left.getStaticType()), ComparisonLe.INSTANCE,
						expression.getStaticType());
			}

			// a >= b becomes b - a <= 0 for identifiers
			return createNormalizedComparison(right, left, ComparisonLe.INSTANCE, expression.getStaticType());
		} else if (operator instanceof ComparisonLt) {

			if (left instanceof Variable && right instanceof Constant) {
				// x < c becomes x - (c+1) <= 0 for integers
				if (isIntegerType(left) && isIntegerType(right)) {
					Constant rightConst = (Constant) right;
					Constant incrementedRight = inc(rightConst, 1);
					return cmp(simplifiedSub(left, incrementedRight), zero(left.getStaticType()), ComparisonLe.INSTANCE,
							expression.getStaticType());
				}
			}

			// a < b: for integers keep LT (will become +1 <= 0), for
			// non-integers relax to
			// LE
			boolean isInteger = isIntegerPair(left, right);
			return createNormalizedComparison(left, right,
					isInteger ? ComparisonLt.INSTANCE : ComparisonLe.INSTANCE,
					expression.getStaticType(), isInteger);
		} else if (operator instanceof ComparisonLe) {
			// If right is exactly 0, keep the left side as-is to avoid creating
			// (left - 0)
			if (right instanceof Constant && ((Constant) right).getValue().equals(new MathNumber(0))) {
				SymbolicExpression normalizedLeft = normalizeLeZeroLeft(left);
				return cmp(normalizedLeft, right, ComparisonLe.INSTANCE, expression.getStaticType());
			}
			// a <= b becomes a - b <= 0
			return createNormalizedComparison(left, right, ComparisonLe.INSTANCE, expression.getStaticType());
		} else if (operator instanceof ComparisonEq) {
			// a == b becomes (a - b <= 0) AND (-a + b <= 0)
			return createEqualityNormalization(left, right, expression.getStaticType());
		} else if (operator instanceof ComparisonNe) {
			// a != b is handled based on the numeric type
			return createInequalityNormalization(left, right, expression.getStaticType());
		}

		// For other operators, we keep the original expression with normalized
		// operands
		return new BinaryExpression(expression.getStaticType(), left, right, operator, location);
	}

	/**
	 * Creates a normalized comparison expression of the form (left - right) op
	 * 0, with possible adjustments for integer types.
	 * 
	 * @param left       the first operand of the subtraction
	 * @param right      the second operand of the subtraction
	 * @param comparison the comparison operator to apply
	 * @param resultType the resulting type of the expression
	 * @param isInteger  if true, applies special rules for integer types
	 * 
	 * @return the normalized expression
	 */
	private SymbolicExpression createNormalizedComparison(
			SymbolicExpression left,
			SymbolicExpression right,
			BinaryOperator comparison,
			Type resultType,
			boolean isInteger) {

		// Create the simplified subtraction (left - right)
		SymbolicExpression subtraction = simplifiedSub(left, right);

		// For integer types, apply special rules
		if (isInteger && comparison instanceof ComparisonLt) {

			// Try to fold the +1 into a nearby constant when possible:
			// - (expr - c) + 1 -> expr - (c - 1)
			// - (c - expr) + 1 -> (c + 1) - expr
			// - constant - constant -> folded constant
			SymbolicExpression folded;

			if (subtraction instanceof BinaryExpression) {
				BinaryExpression subBin = (BinaryExpression) subtraction;

				if (subBin.getOperator() instanceof NumericNonOverflowingAdd) {
					SymbolicExpression addLeft = subBin.getLeft();
					SymbolicExpression addRight = subBin.getRight();
					if (addRight instanceof Constant) {
						Constant updated = inc((Constant) addRight, 1);
						SymbolicExpression combined = add(addLeft, updated);
						return cmp(combined, zero(left.getStaticType()), ComparisonLe.INSTANCE, resultType);
					}
					if (addLeft instanceof Constant) {
						Constant updated = inc((Constant) addLeft, 1);
						SymbolicExpression combined = add(updated, addRight);
						return cmp(combined, zero(left.getStaticType()), ComparisonLe.INSTANCE, resultType);
					}
				}

				if (subBin.getOperator() instanceof NumericNonOverflowingSub) {
					if (subBin.getRight() instanceof Constant) {
						// expr - c + 1 -> expr - (c - 1)
						Constant c = (Constant) subBin.getRight();
						Constant newC = inc(c, -1);
						folded = simplifiedSub(subBin.getLeft(), newC);
						return cmp(folded, zero(left.getStaticType()), ComparisonLe.INSTANCE, resultType);
					} else if (subBin.getLeft() instanceof Constant) {
						// c - expr + 1 -> (c + 1) - expr
						Constant c = (Constant) subBin.getLeft();
						Constant newC = inc(c, 1);
						folded = simplifiedSub(newC, subBin.getRight());
						return cmp(folded, zero(left.getStaticType()), ComparisonLe.INSTANCE, resultType);
					}
				}
			}

			if (left instanceof Constant && right instanceof Constant) {
				// both constants: compute c1 - c2 + 1
				MathNumber v1 = (MathNumber) ((Constant) left).getValue();
				MathNumber v2 = (MathNumber) ((Constant) right).getValue();
				Constant foldedConst = new Constant(left.getStaticType(), v1.subtract(v2).add(new MathNumber(1)),
						location);
				return cmp(foldedConst, zero(left.getStaticType()), ComparisonLe.INSTANCE, resultType);
			}

			// fallback: just add 1 to whatever subtraction returned
			SymbolicExpression subtractionPlusOne = add(subtraction, one(left.getStaticType()));
			return cmp(subtractionPlusOne, zero(left.getStaticType()), ComparisonLe.INSTANCE, resultType);
		}

		// Create the constant 0
		Constant zero = zero(left.getStaticType());

		// Create the comparison with 0
		BinaryExpression result = cmp(subtraction, zero, comparison, resultType);

		return result;
	}

	/**
	 * Creates a normalized comparison expression of the form (left - right) op
	 * 0.
	 * 
	 * @param left       the first operand of the subtraction
	 * @param right      the second operand of the subtraction
	 * @param comparison the comparison operator to apply
	 * @param resultType the resulting type of the expression
	 * 
	 * @return the normalized expression
	 * 
	 * @throws SemanticException if an error occurs during normalization
	 */
	private SymbolicExpression createNormalizedComparison(
			SymbolicExpression left,
			SymbolicExpression right,
			BinaryOperator comparison,
			Type resultType)
			throws SemanticException {

		// Determine if we are working with integer types
		boolean isInteger = isIntegerPair(left, right);

		return createNormalizedComparison(left, right, comparison, resultType, isInteger);
	}

	/**
	 * Creates the normalization for equality: a == b becomes (a - b <= 0) AND
	 * (-a + b <= 0).
	 * 
	 * @param left       the first operand
	 * @param right      the second operand
	 * @param resultType the resulting type of the expression
	 * 
	 * @return the normalized expression
	 * 
	 * @throws SemanticException if an error occurs during normalization
	 */
	private SymbolicExpression createEqualityNormalization(
			SymbolicExpression left,
			SymbolicExpression right,
			Type resultType)
			throws SemanticException {

		// (a - b <= 0)
		SymbolicExpression leftMinusRight = createNormalizedComparison(left, right, ComparisonLe.INSTANCE, resultType);

		// (-a + b <= 0) equivalent to (b - a <= 0)
		SymbolicExpression rightMinusLeft = createNormalizedComparison(right, left, ComparisonLe.INSTANCE, resultType);

		// AND of the two comparisons
		BinaryExpression result = new BinaryExpression(resultType, leftMinusRight, rightMinusLeft, LogicalAnd.INSTANCE,
				location);

		return result;
	}

	/**
	 * Creates the normalization for inequality: a != b. For integer types: (a -
	 * b + 1 <= 0) OR (-a + b + 1 <= 0) For real types: keeps the original
	 * expression or uses different approaches
	 * 
	 * @param left       the first operand
	 * @param right      the second operand
	 * @param resultType the resulting type of the expression
	 * 
	 * @return the normalized expression
	 * 
	 * @throws SemanticException if an error occurs during normalization
	 */
	private SymbolicExpression createInequalityNormalization(
			SymbolicExpression left,
			SymbolicExpression right,
			Type resultType)
			throws SemanticException {

		boolean isInteger = isIntegerPair(left, right);

		if (isInteger) {
			// For integers: (a - b + 1 <= 0) OR (-a + b + 1 <= 0)

			// Try to fold constants: if left or right are constants or the
			// subtraction
			// already contains a constant on the right, merge the +1 into that
			// constant.

			// Build leftMinusRight + 1 but fold where possible
			SymbolicExpression leftMinusRight = simplifiedSub(left, right);
			SymbolicExpression leftCond;
			// If subtraction is of the form (e - c) with c constant, fold: e -
			// (c - 1)
			if (leftMinusRight instanceof BinaryExpression
					&& ((BinaryExpression) leftMinusRight).getRight() instanceof Constant) {
				BinaryExpression lmr = (BinaryExpression) leftMinusRight;
				Constant c = (Constant) lmr.getRight();
				// left - c + 1 => left - (c - 1)
				Constant newC = inc(c, -1);
				SymbolicExpression folded = simplifiedSub(lmr.getLeft(), newC);
				leftCond = cmp(folded, zero(left.getStaticType()), ComparisonLe.INSTANCE, resultType);
			} else if (left instanceof Constant && right instanceof Constant) {
				// both constants: compute c1 - c2 + 1 and produce a single
				// constant comparison
				MathNumber v1 = (MathNumber) ((Constant) left).getValue();
				MathNumber v2 = (MathNumber) ((Constant) right).getValue();
				Constant foldedConst = new Constant(left.getStaticType(), v1.subtract(v2).add(new MathNumber(1)),
						location);
				leftCond = cmp(foldedConst, zero(left.getStaticType()), ComparisonLe.INSTANCE, resultType);
			} else {
				SymbolicExpression leftMinusRightPlusOne = add(leftMinusRight, one(left.getStaticType()));
				leftCond = cmp(leftMinusRightPlusOne, zero(left.getStaticType()), ComparisonLe.INSTANCE, resultType);
			}

			// Build rightMinusLeft + 1 with folding
			SymbolicExpression rightMinusLeft = simplifiedSub(right, left);
			SymbolicExpression rightCond;
			if (rightMinusLeft instanceof BinaryExpression
					&& ((BinaryExpression) rightMinusLeft).getRight() instanceof Constant) {
				// shape: (e - c) + 1 -> e - (c - 1)
				BinaryExpression rml = (BinaryExpression) rightMinusLeft;
				Constant c = (Constant) rml.getRight();
				Constant newC = inc(c, -1);
				SymbolicExpression folded = simplifiedSub(rml.getLeft(), newC);
				rightCond = cmp(folded, zero(left.getStaticType()), ComparisonLe.INSTANCE, resultType);
			} else if (rightMinusLeft instanceof BinaryExpression
					&& ((BinaryExpression) rightMinusLeft).getLeft() instanceof Constant) {
				// shape: (c - e) + 1 -> (c + 1) - e
				BinaryExpression rml = (BinaryExpression) rightMinusLeft;
				Constant c = (Constant) rml.getLeft();
				Constant newC = inc(c, 1);
				SymbolicExpression folded = simplifiedSub(newC, rml.getRight());
				rightCond = cmp(folded, zero(left.getStaticType()), ComparisonLe.INSTANCE, resultType);
			} else if (left instanceof Constant && right instanceof Constant) {
				// handled above, but keep symmetry
				MathNumber v1 = (MathNumber) ((Constant) right).getValue();
				MathNumber v2 = (MathNumber) ((Constant) left).getValue();
				Constant foldedConst = new Constant(left.getStaticType(), v1.subtract(v2).add(new MathNumber(1)),
						location);
				rightCond = cmp(foldedConst, zero(left.getStaticType()), ComparisonLe.INSTANCE, resultType);
			} else {
				SymbolicExpression rightMinusLeftPlusOne = add(rightMinusLeft, one(left.getStaticType()));
				rightCond = cmp(rightMinusLeftPlusOne, zero(left.getStaticType()), ComparisonLe.INSTANCE, resultType);
			}

			// OR of the two comparisons
			BinaryExpression result = new BinaryExpression(resultType, leftCond, rightCond, LogicalOr.INSTANCE,
					location);

			return result;
		} else {
			// For real/rational types, keep the original expression
			// or it might not modify the DBM
			return createNormalizedComparison(left, right, ComparisonNe.INSTANCE, resultType, false);
		}
	}

	/**
	 * Implements the "pushing negation inward" algorithm (De Morgan's Laws).
	 * 
	 * @param expression the expression to push the negation into
	 * 
	 * @return the expression with the negation pushed inward
	 * 
	 * @throws SemanticException if an error occurs during the transformation
	 */
	private SymbolicExpression pushNegationInward(
			SymbolicExpression expression)
			throws SemanticException {

		if (expression instanceof BinaryExpression) {
			BinaryExpression binExpr = (BinaryExpression) expression;
			BinaryOperator op = binExpr.getOperator();

			// not(t1 and t2) -> (not t1) or (not t2)
			if (op instanceof LogicalAnd) {
				SymbolicExpression notLeft = pushNegationInward(binExpr.getLeft());
				SymbolicExpression notRight = pushNegationInward(binExpr.getRight());
				return new BinaryExpression(expression.getStaticType(), notLeft, notRight, LogicalOr.INSTANCE,
						location);
			}
			// not(t1 or t2) -> (not t1) and (not t2)
			else if (op instanceof LogicalOr) {
				SymbolicExpression notLeft = pushNegationInward(binExpr.getLeft());
				SymbolicExpression notRight = pushNegationInward(binExpr.getRight());
				return new BinaryExpression(expression.getStaticType(), notLeft, notRight, LogicalAnd.INSTANCE,
						location);
			}
			// Inversion of comparison operators
			// We recognize specific patterns for octagon domains
			else if (op instanceof ComparisonLe) {

				SymbolicExpression left = binExpr.getLeft();
				SymbolicExpression right = binExpr.getRight();

				// Special pattern: not(A - B <= 0) -> B - A + 1 <= 0 (for
				// integers) or B - A
				// <= 0 (for reals)
				if (left instanceof BinaryExpression
						&& ((BinaryExpression) left).getOperator() instanceof NumericNonOverflowingSub
						&& right instanceof Constant && ((Constant) right).getValue().equals(new MathNumber(0))) {

					BinaryExpression leftSub = (BinaryExpression) left;
					SymbolicExpression A = leftSub.getLeft();
					SymbolicExpression B = leftSub.getRight();

					// We check the type of the original operands
					if (isIntegerType(A) && isIntegerType(B)) {
						// not(A - B <= 0) -> B - A + 1 <= 0, try folding +1
						SymbolicExpression diff = simplifiedSub(B, A);

						// If diff has a constant on the right: (e - c) + 1 -> e
						// - (c - 1)
						if (diff instanceof BinaryExpression
								&& ((BinaryExpression) diff).getRight() instanceof Constant) {
							BinaryExpression be = (BinaryExpression) diff;
							Constant c = (Constant) be.getRight();
							Constant newC = inc(c, -1);
							SymbolicExpression folded = simplifiedSub(be.getLeft(), newC);
							return cmp(folded, zero(A.getStaticType()), ComparisonLe.INSTANCE,
									expression.getStaticType()).accept(this);
						}

						// If diff has a constant on the left: (c - e) + 1 -> (c
						// + 1) - e
						if (diff instanceof BinaryExpression
								&& ((BinaryExpression) diff).getLeft() instanceof Constant) {
							BinaryExpression be = (BinaryExpression) diff;
							Constant c = (Constant) be.getLeft();
							Constant newC = inc(c, 1);
							SymbolicExpression folded = simplifiedSub(newC, be.getRight());
							return cmp(folded, zero(A.getStaticType()), ComparisonLe.INSTANCE,
									expression.getStaticType()).accept(this);
						}

						// fallback to add
						SymbolicExpression plus1 = add(diff, one(A.getStaticType()));
						return cmp(plus1, zero(A.getStaticType()), ComparisonLe.INSTANCE,
								expression.getStaticType()).accept(this);
					} else {
						// not(A - B <= 0) -> B - A <= 0 (for floats, use <=)
						SymbolicExpression diff = simplifiedSub(B, A);
						return cmp(diff, zero(A.getStaticType()), ComparisonLe.INSTANCE,
								expression.getStaticType()).accept(this);
					}
				}

				// Special pattern: not(A - B + 1 <= 0) -> B - A <= 0 (to handle
				// not(x < y))
				if (left instanceof BinaryExpression) {
					BinaryExpression leftExpr = (BinaryExpression) left;
					if (leftExpr.getOperator() instanceof NumericNonOverflowingAdd &&
							leftExpr.getRight() instanceof Constant &&
							((Constant) leftExpr.getRight()).getValue().equals(new MathNumber(1)) &&
							right instanceof Constant &&
							((Constant) right).getValue().equals(new MathNumber(0))) {

						SymbolicExpression leftOfAdd = leftExpr.getLeft();
						if (leftOfAdd instanceof BinaryExpression &&
								((BinaryExpression) leftOfAdd).getOperator() instanceof NumericNonOverflowingSub) {

							BinaryExpression subExpr = (BinaryExpression) leftOfAdd;
							SymbolicExpression A = subExpr.getLeft();
							SymbolicExpression B = subExpr.getRight();

							// not(A - B + 1 <= 0) -> B - A <= 0
							SymbolicExpression diff = simplifiedSub(B, A);
							return cmp(diff, zero(A.getStaticType()), ComparisonLe.INSTANCE,
									expression.getStaticType()).accept(this);
						}
					}
				}

				// General pattern: not(X <= Y)
				// We check the type of the operands
				if (isIntegerPair(left, right)) {
					// For integers: not(x <= y) -> y - x + 1 <= 0
					// Special case: if right is a constant, use (-x + (c+1))
					// form instead of (c+1
					// -
					// x)
					if (right instanceof Constant) {
						Constant rightConst = (Constant) right;
						Constant incrementedConst = inc(rightConst, 1);
						SymbolicExpression negLeft = neg(left);
						SymbolicExpression result = add(negLeft, incrementedConst);
						return cmp(result, zero(left.getStaticType()), ComparisonLe.INSTANCE,
								expression.getStaticType()).accept(this);
					} else {
						SymbolicExpression diff = simplifiedSub(right, left);
						SymbolicExpression plus1 = add(diff, one(left.getStaticType()));
						return cmp(plus1, zero(left.getStaticType()), ComparisonLe.INSTANCE,
								expression.getStaticType()).accept(this);
					}
				} else {
					// For reals: not(x <= y) -> y - x <= 0 (use <= for floats)
					// Special case: if right is a constant, use (-x + c) form
					// instead of (c - x)
					if (right instanceof Constant) {
						SymbolicExpression negLeft = neg(left);
						SymbolicExpression result = add(negLeft, right);
						return cmp(result, zero(left.getStaticType()), ComparisonLe.INSTANCE,
								expression.getStaticType()).accept(this);
					} else {
						SymbolicExpression diff = simplifiedSub(right, left);
						return cmp(diff, zero(left.getStaticType()), ComparisonLe.INSTANCE,
								expression.getStaticType()).accept(this);
					}
				}

			} else if (op instanceof ComparisonLt) {

				SymbolicExpression left = binExpr.getLeft();
				SymbolicExpression right = binExpr.getRight();

				// General pattern: not(x < y) -> x >= y -> y - x <= 0
				SymbolicExpression diff = simplifiedSub(right, left);
				return cmp(diff, zero(left.getStaticType()), ComparisonLe.INSTANCE, expression.getStaticType())
						.accept(this);
			} else if (op instanceof ComparisonGt) {
				// not(e1 > e2) -> e1 <= e2 -> e1 - e2 <= 0
				return createNormalizedComparison(binExpr.getLeft(), binExpr.getRight(), ComparisonLe.INSTANCE,
						expression.getStaticType());
			} else if (op instanceof ComparisonGe) {
				// not(e1 >= e2) -> e1 < e2
				// For integers keep strict inequality (will be converted to +1
				// <= 0),
				// for non-integers relax to <= 0 as per float semantics/tests.
				SymbolicExpression left = binExpr.getLeft();
				SymbolicExpression right = binExpr.getRight();
				boolean isInteger = isIntegerPair(left, right);
				return createNormalizedComparison(left, right,
						isInteger ? ComparisonLt.INSTANCE : ComparisonLe.INSTANCE,
						expression.getStaticType(), isInteger);
			} else if (op instanceof ComparisonEq) {
				// not(e1 = e2) -> e1 != e2
				return new BinaryExpression(expression.getStaticType(), binExpr.getLeft(), binExpr.getRight(),
						ComparisonNe.INSTANCE, location).accept(this);
			} else if (op instanceof ComparisonNe) {
				// not(e1 != e2) -> e1 = e2
				return new BinaryExpression(expression.getStaticType(), binExpr.getLeft(), binExpr.getRight(),
						ComparisonEq.INSTANCE, location).accept(this);
			}
		} else if (expression instanceof UnaryExpression) {
			UnaryExpression unExpr = (UnaryExpression) expression;

			// not(not t) -> t
			if (unExpr.getOperator() instanceof LogicalNegation) {
				return unExpr.getExpression().accept(this);
			}
		}

		// For other expression types, apply the negation directly
		return new UnaryExpression(expression.getStaticType(), expression, LogicalNegation.INSTANCE, location);
	}

	/**
	 * Checks if a type is an integer type.
	 * 
	 * @param e the expression to check
	 * 
	 * @return true if the type is integral, false otherwise
	 * 
	 * @throws SemanticException if an error occurs during type checking
	 */
	private boolean isIntegerType(
			SymbolicExpression e)
			throws SemanticException {
		Type type = e.getStaticType();
		boolean result = type.isNumericType() && type.asNumericType().isIntegral();
		if (result)
			return result;

		if (!type.isUntyped())
			return false;

		// Try to infer from Identifier type if possible
		if (e instanceof BinaryExpression) {
			BinaryExpression be = (BinaryExpression) e;
			return isIntegerType(be.getLeft()) && isIntegerType(be.getRight());
		} else if (e instanceof UnaryExpression) {
			UnaryExpression ue = (UnaryExpression) e;
			return isIntegerType(ue.getExpression());
		} else if (e instanceof Identifier) {
			Identifier id = (Identifier) e;
			Type idType = id.getStaticType();
			if (idType.isNumericType() && idType.asNumericType().isIntegral()) {
				return true;
			}

			if (!idType.isUntyped()) {
				return false;
			}

			// use oracle to detect type
			Set<Type> types = oracle.getRuntimeTypesOf(id, pp, oracle);
			for (Type t : types) {
				if (t.isNumericType() && t.asNumericType().isIntegral()) {
					return true;
				}
			}

		} else if (e instanceof Variable) {
			// use oracle to detect type
			Set<Type> types = oracle.getRuntimeTypesOf(e, pp, oracle);
			for (Type t : types) {
				if (t.isNumericType() && t.asNumericType().isIntegral()) {
					return true;
				}
			}

		} else {
		}

		return false;
	}

	@Override
	public SymbolicExpression visit(
			UnaryExpression expression,
			SymbolicExpression arg,
			Object... params)
			throws SemanticException {

		// Handle the NOT operator (LogicalNegation)
		if (expression.getOperator() instanceof LogicalNegation) {
			// Important: we push the negation inside the ORIGINAL expression,
			// not the already normalized one (arg), to preserve the correct
			// form
			// of cases like not(x < y) on non-integer types (-> <=) and not(x
			// == y) (->
			// !=).
			return pushNegationInward(expression.getExpression());
		}

		// For other unary expressions, we only normalize the argument
		return new UnaryExpression(expression.getStaticType(), arg, expression.getOperator(), location);
	}

	@Override
	public SymbolicExpression visit(
			TernaryExpression expression,
			SymbolicExpression left,
			SymbolicExpression middle,
			SymbolicExpression right,
			Object... params)
			throws SemanticException {
		// For ternary expressions, we normalize all arguments
		return new TernaryExpression(expression.getStaticType(), left, middle, right, expression.getOperator(),
				location);
	}

	// Default implementations that return the original expression
	// for all other expression types

	@Override
	public SymbolicExpression visit(
			HeapExpression expression,
			SymbolicExpression[] subExpressions,
			Object... params)
			throws SemanticException {
		return expression;
	}

	@Override
	public SymbolicExpression visit(
			AccessChild expression,
			SymbolicExpression receiver,
			SymbolicExpression child,
			Object... params)
			throws SemanticException {
		return expression;
	}

	@Override
	public SymbolicExpression visit(
			MemoryAllocation expression,
			Object... params)
			throws SemanticException {
		return expression;
	}

	@Override
	public SymbolicExpression visit(
			HeapReference expression,
			SymbolicExpression arg,
			Object... params)
			throws SemanticException {
		return expression;
	}

	@Override
	public SymbolicExpression visit(
			HeapDereference expression,
			SymbolicExpression arg,
			Object... params)
			throws SemanticException {
		return expression;
	}

	@Override
	public SymbolicExpression visit(
			ValueExpression expression,
			SymbolicExpression[] subExpressions,
			Object... params)
			throws SemanticException {
		return expression;
	}

	@Override
	public SymbolicExpression visit(
			Skip expression,
			Object... params)
			throws SemanticException {
		return expression;
	}

	@Override
	public SymbolicExpression visit(
			PushAny expression,
			Object... params)
			throws SemanticException {
		return expression;
	}

	@Override
	public SymbolicExpression visit(
			PushInv expression,
			Object... params)
			throws SemanticException {
		return expression;
	}

	@Override
	public SymbolicExpression visit(
			Constant expression,
			Object... params)
			throws SemanticException {
		return expression;
	}

	@Override
	public SymbolicExpression visit(
			Identifier expression,
			Object... params)
			throws SemanticException {
		return expression;
	}
}