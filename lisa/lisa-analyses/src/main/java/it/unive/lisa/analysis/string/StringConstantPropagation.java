package it.unive.lisa.analysis.string;

import it.unive.lisa.analysis.BaseLattice;
import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.lattices.Satisfiability;
import it.unive.lisa.analysis.nonrelational.value.BaseNonRelationalValueDomain;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonEq;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGe;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGt;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLe;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLt;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonNe;
import it.unive.lisa.symbolic.value.operator.binary.StringConcat;
import it.unive.lisa.symbolic.value.operator.ternary.StringReplace;
import it.unive.lisa.symbolic.value.operator.ternary.TernaryOperator;
import it.unive.lisa.symbolic.value.operator.unary.UnaryOperator;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;

/**
 * The string constant propagation abstract domain, tracking if a certain string
 * value has constant value or not. Top and bottom cases for least upper bounds,
 * widening and less or equals operations are handled by {@link BaseLattice} in
 * {@link BaseLattice#lub}, {@link BaseLattice#widening} and
 * {@link BaseLattice#lessOrEqual}, respectively.
 * 
 * @author <a href="mailto:michele.martelli1@studenti.unipr.it">Michele
 *             Martelli</a>
 * @author <a href="mailto:vincenzo.arceri@unipr.it">Vincenzo Arceri</a>
 */
public class StringConstantPropagation implements BaseNonRelationalValueDomain<StringConstantPropagation> {

	private static final StringConstantPropagation TOP = new StringConstantPropagation(true, false);
	private static final StringConstantPropagation BOTTOM = new StringConstantPropagation(false, true);

	private final boolean isTop, isBottom;

	private final String value;

	/**
	 * Builds the top abstract value.
	 */
	public StringConstantPropagation() {
		this(null, true, false);
	}

	private StringConstantPropagation(
			String value,
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
	public StringConstantPropagation(
			String value) {
		this(value, false, false);
	}

	private StringConstantPropagation(
			boolean isTop,
			boolean isBottom) {
		this(null, isTop, isBottom);
	}

	@Override
	public StringConstantPropagation top() {
		return TOP;
	}

	@Override
	public boolean isTop() {
		return isTop;
	}

	@Override
	public StringConstantPropagation bottom() {
		return BOTTOM;
	}

	@Override
	public StructuredRepresentation representation() {
		if (isBottom())
			return Lattice.bottomRepresentation();
		if (isTop())
			return Lattice.topRepresentation();

		return new StringRepresentation(value);
	}

	@Override
	public StringConstantPropagation evalNullConstant(
			ProgramPoint pp,
			SemanticOracle oracle) {
		return top();
	}

	@Override
	public StringConstantPropagation evalNonNullConstant(
			Constant constant,
			ProgramPoint pp,
			SemanticOracle oracle) {
		if (constant.getValue() instanceof String)
			return new StringConstantPropagation((String) constant.getValue());

		return top();
	}

	@Override
	public StringConstantPropagation evalUnaryExpression(
			UnaryOperator operator,
			StringConstantPropagation arg,
			ProgramPoint pp,
			SemanticOracle oracle) {

		return top();
	}

	@Override
	public StringConstantPropagation evalBinaryExpression(
			BinaryOperator operator,
			StringConstantPropagation left,
			StringConstantPropagation right,
			ProgramPoint pp,
			SemanticOracle oracle) {

		if (operator instanceof StringConcat)
			return left.isTop() || right.isTop() ? top() : new StringConstantPropagation(left.value + right.value);

		return top();
	}

	@Override
	public StringConstantPropagation evalTernaryExpression(
			TernaryOperator operator,
			StringConstantPropagation left,
			StringConstantPropagation middle,
			StringConstantPropagation right,
			ProgramPoint pp,
			SemanticOracle oracle) {

		if (operator instanceof StringReplace) {
			if (left.isTop() || right.isTop() || middle.isTop())
				return top();

			String replaced = left.value;
			replaced = replaced.replace(middle.value, right.value);

			return new StringConstantPropagation(replaced);
		}

		return top();

	}

	@Override
	public StringConstantPropagation lubAux(
			StringConstantPropagation other)
			throws SemanticException {
		return TOP;
	}

	@Override
	public boolean lessOrEqualAux(
			StringConstantPropagation other)
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
		StringConstantPropagation other = (StringConstantPropagation) obj;
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
			BinaryOperator operator,
			StringConstantPropagation left,
			StringConstantPropagation right,
			ProgramPoint pp,
			SemanticOracle oracle) {

		if (left.isTop() || right.isTop())
			return Satisfiability.UNKNOWN;

		if (operator == ComparisonEq.INSTANCE)
			return left.value.equals(right.value) ? Satisfiability.SATISFIED
					: Satisfiability.NOT_SATISFIED;
		else if (operator == ComparisonGe.INSTANCE)
			return left.value.compareTo(right.value) >= 0 ? Satisfiability.SATISFIED : Satisfiability.NOT_SATISFIED;
		else if (operator == ComparisonGt.INSTANCE)
			return left.value.compareTo(right.value) > 0 ? Satisfiability.SATISFIED : Satisfiability.NOT_SATISFIED;
		else if (operator == ComparisonLe.INSTANCE)
			return left.value.compareTo(right.value) <= 0 ? Satisfiability.SATISFIED : Satisfiability.NOT_SATISFIED;
		else if (operator == ComparisonLt.INSTANCE)
			return left.value.compareTo(right.value) < 0 ? Satisfiability.SATISFIED : Satisfiability.NOT_SATISFIED;
		else if (operator == ComparisonNe.INSTANCE)
			return !left.value.equals(right.value) ? Satisfiability.SATISFIED
					: Satisfiability.NOT_SATISFIED;

		else
			return Satisfiability.UNKNOWN;
	}
	
	protected String getValue() {
		return value;
	}
}
