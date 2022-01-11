package it.unive.lisa.analysis.numeric;

import it.unive.lisa.analysis.BaseLattice;
import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticDomain.Satisfiability;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.nonrelational.value.BaseNonRelationalValueDomain;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.analysis.representation.DomainRepresentation;
import it.unive.lisa.analysis.representation.StringRepresentation;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.symbolic.value.operator.AdditionOperator;
import it.unive.lisa.symbolic.value.operator.DivisionOperator;
import it.unive.lisa.symbolic.value.operator.Module;
import it.unive.lisa.symbolic.value.operator.Multiplication;
import it.unive.lisa.symbolic.value.operator.SubtractionOperator;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonEq;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGe;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGt;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLe;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLt;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonNe;
import it.unive.lisa.symbolic.value.operator.ternary.TernaryOperator;
import it.unive.lisa.symbolic.value.operator.unary.NumericNegation;
import it.unive.lisa.symbolic.value.operator.unary.UnaryOperator;

/**
 * The overflow-insensitive basic integer constant propagation abstract domain,
 * tracking if a certain integer value has constant value or not, implemented as
 * a {@link BaseNonRelationalValueDomain}, handling top and bottom values for
 * the expression evaluation and bottom values for the expression
 * satisfiability. Top and bottom cases for least upper bounds, widening and
 * less or equals operations are handled by {@link BaseLattice} in
 * {@link BaseLattice#lub}, {@link BaseLattice#widening} and
 * {@link BaseLattice#lessOrEqual}, respectively.
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
	public DomainRepresentation representation() {
		if (isBottom())
			return Lattice.BOTTOM_REPR;
		if (isTop())
			return Lattice.TOP_REPR;

		return new StringRepresentation(value.toString());
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

		if (arg.isTop())
			return top();

		if (operator == NumericNegation.INSTANCE)
			return new IntegerConstantPropagation(-value);

		return top();
	}

	@Override
	protected IntegerConstantPropagation evalBinaryExpression(BinaryOperator operator, IntegerConstantPropagation left,
			IntegerConstantPropagation right, ProgramPoint pp) {

		if (operator instanceof AdditionOperator)
			return left.isTop() || right.isTop() ? top() : new IntegerConstantPropagation(left.value + right.value);
		else if (operator instanceof DivisionOperator)
			if (!left.isTop() && left.value == 0)
				return new IntegerConstantPropagation(0);
			else if (!right.isTop() && right.value == 0)
				return bottom();
			else if (left.isTop() || right.isTop() || left.value % right.value != 0)
				return top();
			else
				return new IntegerConstantPropagation(left.value / right.value);
		else if (operator instanceof Module)
			return left.isTop() || right.isTop() ? top() : new IntegerConstantPropagation(left.value % right.value);
		else if (operator instanceof Multiplication)
			return left.isTop() || right.isTop() ? top() : new IntegerConstantPropagation(left.value * right.value);
		else if (operator instanceof SubtractionOperator)
			return left.isTop() || right.isTop() ? top() : new IntegerConstantPropagation(left.value - right.value);
		else
			return top();
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
		return true;
	}

	@Override
	protected Satisfiability satisfiesBinaryExpression(BinaryOperator operator, IntegerConstantPropagation left,
			IntegerConstantPropagation right, ProgramPoint pp) {

		if (left.isTop() || right.isTop())
			return Satisfiability.UNKNOWN;

		if (operator == ComparisonEq.INSTANCE)
			return left.value.intValue() == right.value.intValue() ? Satisfiability.SATISFIED
					: Satisfiability.NOT_SATISFIED;
		else if (operator == ComparisonGe.INSTANCE)
			return left.value >= right.value ? Satisfiability.SATISFIED : Satisfiability.NOT_SATISFIED;
		else if (operator == ComparisonGt.INSTANCE)
			return left.value > right.value ? Satisfiability.SATISFIED : Satisfiability.NOT_SATISFIED;
		else if (operator == ComparisonLe.INSTANCE)
			return left.value <= right.value ? Satisfiability.SATISFIED : Satisfiability.NOT_SATISFIED;
		else if (operator == ComparisonLt.INSTANCE)
			return left.value < right.value ? Satisfiability.SATISFIED : Satisfiability.NOT_SATISFIED;
		else if (operator == ComparisonNe.INSTANCE)
			return left.value.intValue() != right.value.intValue() ? Satisfiability.SATISFIED
					: Satisfiability.NOT_SATISFIED;
		else
			return Satisfiability.UNKNOWN;
	}

	@Override
	protected ValueEnvironment<IntegerConstantPropagation> assumeBinaryExpression(
			ValueEnvironment<IntegerConstantPropagation> environment, BinaryOperator operator, ValueExpression left,
			ValueExpression right, ProgramPoint pp) throws SemanticException {
		if (operator == ComparisonEq.INSTANCE)
			if (left instanceof Identifier)
				environment = environment.assign((Identifier) left, right, pp);
			else if (right instanceof Identifier)
				environment = environment.assign((Identifier) right, left, pp);
			else
				return environment;
		return environment;
	}
}
