package it.unive.lisa.analysis.numeric;

import it.unive.lisa.analysis.BaseLattice;
import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.combination.constraints.WholeValueDomain;
import it.unive.lisa.analysis.combination.smash.SmashedSumIntDomain;
import it.unive.lisa.analysis.lattices.Satisfiability;
import it.unive.lisa.analysis.nonrelational.value.BaseNonRelationalValueDomain;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.Identifier;
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
import it.unive.lisa.symbolic.value.operator.unary.NumericNegation;
import it.unive.lisa.util.numeric.IntInterval;
import it.unive.lisa.util.numeric.MathNumberConversionException;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;
import java.util.Collections;
import java.util.Set;

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
public class IntegerConstantPropagation
		implements
		SmashedSumIntDomain<IntegerConstantPropagation>,
		WholeValueDomain<IntegerConstantPropagation> {

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

	private IntegerConstantPropagation(
			Integer value,
			boolean isTop,
			boolean isBottom) {
		this.value = value;
		this.isTop = isTop;
		this.isBottom = isBottom;
	}

	/**
	 * Builds the abstract value for the given constant.
	 * 
	 * @param value the constant
	 */
	public IntegerConstantPropagation(
			Integer value) {
		this(value, false, false);
	}

	private IntegerConstantPropagation(
			boolean isTop,
			boolean isBottom) {
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
	public StructuredRepresentation representation() {
		if (isBottom())
			return Lattice.bottomRepresentation();
		if (isTop())
			return Lattice.topRepresentation();

		return new StringRepresentation(value.toString());
	}

	@Override
	public IntegerConstantPropagation evalNullConstant(
			ProgramPoint pp,
			SemanticOracle oracle) {
		return top();
	}

	@Override
	public IntegerConstantPropagation evalNonNullConstant(
			Constant constant,
			ProgramPoint pp,
			SemanticOracle oracle) {
		if (constant.getValue() instanceof Integer)
			return new IntegerConstantPropagation((Integer) constant.getValue());
		return top();
	}

	@Override
	public IntegerConstantPropagation evalUnaryExpression(
			UnaryExpression expression,
			IntegerConstantPropagation arg,
			ProgramPoint pp,
			SemanticOracle oracle) {

		if (arg.isTop())
			return top();

		if (expression.getOperator() == NumericNegation.INSTANCE)
			return new IntegerConstantPropagation(-value);

		return top();
	}

	@Override
	public IntegerConstantPropagation evalBinaryExpression(
			BinaryExpression expression,
			IntegerConstantPropagation left,
			IntegerConstantPropagation right,
			ProgramPoint pp,
			SemanticOracle oracle) {
		BinaryOperator operator = expression.getOperator();
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
	public IntegerConstantPropagation lubAux(
			IntegerConstantPropagation other)
			throws SemanticException {
		return TOP;
	}

	@Override
	public boolean lessOrEqualAux(
			IntegerConstantPropagation other)
			throws SemanticException {
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
	public boolean equals(
			Object obj) {
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
	public Satisfiability satisfiesBinaryExpression(
			BinaryExpression expression,
			IntegerConstantPropagation left,
			IntegerConstantPropagation right,
			ProgramPoint pp,
			SemanticOracle oracle) {

		if (left.isTop() || right.isTop())
			return Satisfiability.UNKNOWN;

		BinaryOperator operator = expression.getOperator();
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
			BinaryExpression expression,
			ProgramPoint src,
			ProgramPoint dest,
			SemanticOracle oracle)
			throws SemanticException {
		BinaryOperator operator = expression.getOperator();
		ValueExpression left = (ValueExpression) expression.getLeft();
		ValueExpression right = (ValueExpression) expression.getRight();
		if (operator == ComparisonEq.INSTANCE)
			if (left instanceof Identifier) {
				IntegerConstantPropagation eval = eval(right, environment, src, oracle);
				if (eval.isBottom())
					return environment.bottom();
				return environment.putState((Identifier) left, eval);
			} else if (right instanceof Identifier) {
				IntegerConstantPropagation eval = eval(left, environment, src, oracle);
				if (eval.isBottom())
					return environment.bottom();
				return environment.putState((Identifier) right, eval);
			}
		return environment;
	}

	@Override
	public IntegerConstantPropagation fromInterval(
			IntInterval intv)
			throws SemanticException {
		if (intv.isFinite() && intv.getHigh().equals(intv.getLow()))
			try {
				return new IntegerConstantPropagation(intv.getLow().toInt());
			} catch (MathNumberConversionException e) {
				throw new SemanticException("Cannot convert " + intv + " to an integer constant", e);
			}
		return top();
	}

	@Override
	public IntInterval toInterval() throws SemanticException {
		if (isBottom())
			return null;
		if (isTop())
			return IntInterval.INFINITY;
		return new IntInterval(value, value);
	}

	@Override
	public Set<BinaryExpression> constraints(
			ValueExpression e,
			ProgramPoint pp)
			throws SemanticException {
		if (isTop())
			return Collections.emptySet();
		if (isBottom())
			return null;
		return Collections.singleton(new BinaryExpression(
				pp.getProgram().getTypes().getBooleanType(),
				new Constant(pp.getProgram().getTypes().getIntegerType(), value, e.getCodeLocation()),
				e,
				ComparisonEq.INSTANCE,
				pp.getLocation()));
	}

	@Override
	public IntegerConstantPropagation generate(
			Set<BinaryExpression> constraints,
			ProgramPoint pp)
			throws SemanticException {
		if (constraints == null)
			return BOTTOM;

		Integer ge = null, le = null;
		for (BinaryExpression expr : constraints)
			if (expr.getLeft() instanceof Constant
					&& ((Constant) expr.getLeft()).getValue() instanceof Integer) {
				Integer val = (Integer) ((Constant) expr.getLeft()).getValue();
				if (expr.getOperator() instanceof ComparisonEq)
					return new IntegerConstantPropagation(val);
				else if (expr.getOperator() instanceof ComparisonGe)
					ge = val;
				else if (expr.getOperator() instanceof ComparisonLe)
					le = val;
			}

		if (ge != null && ge.equals(le))
			return new IntegerConstantPropagation(ge);

		return TOP;
	}
}
