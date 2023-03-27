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
public class IntegerConstantPropagation implements BaseNonRelationalValueDomain<IntegerConstantPropagation> {

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

	/**
	 * Builds the abstract value for the given constant.
	 * 
	 * @param value the constant
	 */
	public IntegerConstantPropagation(Integer value) {
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
			return Lattice.bottomRepresentation();
		if (isTop())
			return Lattice.topRepresentation();

		return new StringRepresentation(value.toString());
	}

	@Override
	public IntegerConstantPropagation evalNullConstant(ProgramPoint pp) {
		return top();
	}

	@Override
	public IntegerConstantPropagation evalNonNullConstant(Constant constant, ProgramPoint pp) {
		if (constant.getValue() instanceof Integer)
			return new IntegerConstantPropagation((Integer) constant.getValue());
		return top();
	}

	@Override
	public IntegerConstantPropagation evalUnaryExpression(UnaryOperator operator, IntegerConstantPropagation arg,
			ProgramPoint pp) {

		if (arg.isTop())
			return top();

		if (operator == NumericNegation.INSTANCE)
			return new IntegerConstantPropagation(-value);

		return top();
	}

	@Override
	public IntegerConstantPropagation evalBinaryExpression(BinaryOperator operator, IntegerConstantPropagation left,
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
		else if (operator instanceof ModuloOperator)
			// this is different from the semantics of java
			return left.isTop() || right.isTop() ? top()
					: new IntegerConstantPropagation(right.value < 0 ? -Math.abs(left.value % right.value)
							: -Math.abs(left.value % right.value));
		else if (operator instanceof RemainderOperator)
			// this matches the semantics of java
			return left.isTop() || right.isTop() ? top() : new IntegerConstantPropagation(left.value % right.value);
		else if (operator instanceof MultiplicationOperator)
			return left.isTop() || right.isTop() ? top() : new IntegerConstantPropagation(left.value * right.value);
		else if (operator instanceof SubtractionOperator)
			return left.isTop() || right.isTop() ? top() : new IntegerConstantPropagation(left.value - right.value);
		else
			return top();
	}

	@Override
	public IntegerConstantPropagation evalTernaryExpression(TernaryOperator operator,
			IntegerConstantPropagation left,
			IntegerConstantPropagation middle, IntegerConstantPropagation right, ProgramPoint pp) {
		return top();
	}

	@Override
	public IntegerConstantPropagation lubAux(IntegerConstantPropagation other) throws SemanticException {
		return TOP;
	}

	@Override
	public boolean lessOrEqualAux(IntegerConstantPropagation other) throws SemanticException {
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
	public Satisfiability satisfiesBinaryExpression(BinaryOperator operator, IntegerConstantPropagation left,
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
	public ValueEnvironment<IntegerConstantPropagation> assumeBinaryExpression(
			ValueEnvironment<IntegerConstantPropagation> environment,
			BinaryOperator operator,
			ValueExpression left,
			ValueExpression right,
			ProgramPoint src,
			ProgramPoint dest)
			throws SemanticException {
		if (operator == ComparisonEq.INSTANCE)
			if (left instanceof Identifier) {
				IntegerConstantPropagation eval = eval(right, environment, src);
				if (eval.isBottom())
					return environment.bottom();
				return environment.putState((Identifier) left, eval);
			} else if (right instanceof Identifier) {
				IntegerConstantPropagation eval = eval(left, environment, src);
				if (eval.isBottom())
					return environment.bottom();
				return environment.putState((Identifier) right, eval);
			}
		return environment;
	}
}
