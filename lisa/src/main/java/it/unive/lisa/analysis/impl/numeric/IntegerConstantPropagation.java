package it.unive.lisa.analysis.impl.numeric;

import it.unive.lisa.analysis.BaseLattice;
import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticDomain.Satisfiability;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.nonrelational.value.BaseNonRelationalValueDomain;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.BinaryOperator;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.TernaryOperator;
import it.unive.lisa.symbolic.value.UnaryOperator;

/**
 * The basic integer constant propagation abstract domain, tracking if a certain
 * integer value has constant value or not, implemented as a
 * {@link BaseNonRelationalValueDomain}, handling top and bottom values for the
 * expression evaluation and bottom values for the expression satisfiability.
 * Top and bottom cases for least upper bounds, widening and less or equals
 * operations are handled by {@link BaseLattice} in {@link BaseLattice#lub},
 * {@link BaseLattice#widening} and {@link BaseLattice#lessOrEqual},
 * respectively.
 * 
 * @author <a href="mailto:vincenzo.arceri@unive.it">Vincenzo Arceri</a>
 */
public class IntegerConstantPropagation extends BaseNonRelationalValueDomain<IntegerConstantPropagation> {

	private static final IntegerConstantPropagation TOP = new IntegerConstantPropagation(true, false);
	private static final IntegerConstantPropagation BOTTOM = new IntegerConstantPropagation(false, true);

	private final boolean isTop, isBottom;

	private final Integer value;

	/**
	 * Builds the top abstract value.
	 */
	public IntegerConstantPropagation() {
		this(null, true, false);
	}

	private IntegerConstantPropagation(Integer value, boolean isTop, boolean isBottom) {
		this.value = value;
		this.isTop = isTop;
		this.isBottom = isBottom;
	}

	private IntegerConstantPropagation(Integer value) {
		this(value, false, false);
	}

	private IntegerConstantPropagation(boolean isTop, boolean isBottom) {
		this(null, isTop, isBottom);
	}

	@Override
	public IntegerConstantPropagation top() {
		return TOP;
	}

	@Override
	public boolean isTop() {
		return isTop;
	}

	@Override
	public IntegerConstantPropagation bottom() {
		return BOTTOM;
	}

	@Override
	public String representation() {
		return isTop() ? Lattice.TOP_STRING : isBottom() ? Lattice.BOTTOM_STRING : value.toString();
	}

	@Override
	protected IntegerConstantPropagation evalNullConstant(ProgramPoint pp) {
		return top();
	}

	@Override
	protected IntegerConstantPropagation evalNonNullConstant(Constant constant, ProgramPoint pp) {
		if (constant.getValue() instanceof Integer)
			return new IntegerConstantPropagation((Integer) constant.getValue());
		return top();
	}

	@Override
	protected IntegerConstantPropagation evalUnaryExpression(UnaryOperator operator, IntegerConstantPropagation arg,
			ProgramPoint pp) {
		switch (operator) {
		case NUMERIC_NEG:
			return new IntegerConstantPropagation(-value);
		case STRING_LENGTH:
			return top();
		default:
			return top();
		}
	}

	@Override
	protected IntegerConstantPropagation evalBinaryExpression(BinaryOperator operator, IntegerConstantPropagation left,
			IntegerConstantPropagation right, ProgramPoint pp) {
		switch (operator) {
		case NUMERIC_ADD:
			return new IntegerConstantPropagation(left.value + right.value);
		case NUMERIC_DIV:
			if (left.value % right.value != 0)
				return top();
			else
				return new IntegerConstantPropagation(left.value / right.value);
		case NUMERIC_MOD:
			return new IntegerConstantPropagation(left.value % right.value);
		case NUMERIC_MUL:
			return new IntegerConstantPropagation(left.value * right.value);
		case NUMERIC_SUB:
			return new IntegerConstantPropagation(left.value - right.value);
		default:
			return top();
		}
	}

	@Override
	protected IntegerConstantPropagation evalTernaryExpression(TernaryOperator operator,
			IntegerConstantPropagation left,
			IntegerConstantPropagation middle, IntegerConstantPropagation right, ProgramPoint pp) {
		return top();
	}

	@Override
	protected IntegerConstantPropagation lubAux(IntegerConstantPropagation other) throws SemanticException {
		return TOP;
	}

	@Override
	protected IntegerConstantPropagation wideningAux(IntegerConstantPropagation other) throws SemanticException {
		return lubAux(other);
	}

	@Override
	protected boolean lessOrEqualAux(IntegerConstantPropagation other) throws SemanticException {
		return false;
	}

	@Override
	public int hashCode() {
		if (isTop())
			return 1;
		else if (isBottom())
			return 2;

		final int prime = 31;
		int result = 1;
		result = prime * result + (isBottom ? 1231 : 1237);
		result = prime * result + (isTop ? 1231 : 1237);
		result = prime * result + ((value == null) ? 0 : value.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		IntegerConstantPropagation other = (IntegerConstantPropagation) obj;
		if (isBottom != other.isBottom)
			return false;
		if (isTop != other.isTop)
			return false;
		if (value == null) {
			if (other.value != null)
				return false;
		} else if (!value.equals(other.value))
			return false;
		return isTop && other.isTop;
	}

	@Override
	protected Satisfiability satisfiesAbstractValue(IntegerConstantPropagation value, ProgramPoint pp) {
		return Satisfiability.UNKNOWN;
	}

	@Override
	protected Satisfiability satisfiesNullConstant(ProgramPoint pp) {
		return Satisfiability.UNKNOWN;
	}

	@Override
	protected Satisfiability satisfiesNonNullConstant(Constant constant, ProgramPoint pp) {
		return Satisfiability.UNKNOWN;
	}

	@Override
	protected Satisfiability satisfiesUnaryExpression(UnaryOperator operator, IntegerConstantPropagation arg,
			ProgramPoint pp) {
		return Satisfiability.UNKNOWN;
	}

	@Override
	protected Satisfiability satisfiesBinaryExpression(BinaryOperator operator, IntegerConstantPropagation left,
			IntegerConstantPropagation right, ProgramPoint pp) {

		if (left.isTop() || right.isTop())
			return Satisfiability.UNKNOWN;

		switch (operator) {
		case COMPARISON_EQ:
			return left.value == right.value ? Satisfiability.SATISFIED : Satisfiability.NOT_SATISFIED;
		case COMPARISON_GE:
			return left.value >= right.value ? Satisfiability.SATISFIED : Satisfiability.NOT_SATISFIED;
		case COMPARISON_GT:
			return left.value > right.value ? Satisfiability.SATISFIED : Satisfiability.NOT_SATISFIED;
		case COMPARISON_LE:
			return left.value <= right.value ? Satisfiability.SATISFIED : Satisfiability.NOT_SATISFIED;
		case COMPARISON_LT:
			return left.value < right.value ? Satisfiability.SATISFIED : Satisfiability.NOT_SATISFIED;
		case COMPARISON_NE:
			return left.value != right.value ? Satisfiability.SATISFIED : Satisfiability.NOT_SATISFIED;
		default:
			return Satisfiability.UNKNOWN;
		}
	}

	@Override
	protected Satisfiability satisfiesTernaryExpression(TernaryOperator operator, IntegerConstantPropagation left,
			IntegerConstantPropagation middle, IntegerConstantPropagation right, ProgramPoint pp) {
		return Satisfiability.UNKNOWN;
	}
}
