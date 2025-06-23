package it.unive.lisa.analysis.numeric;

import it.unive.lisa.analysis.BaseLattice;
import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
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
import it.unive.lisa.symbolic.value.operator.unary.NumericNegation;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;

/**
 * The overflow-insensitive Parity abstract domain, tracking if a numeric value
 * is even or odd, implemented as a {@link BaseNonRelationalValueDomain},
 * handling top and bottom values for the expression evaluation and bottom
 * values for the expression satisfiability. Top and bottom cases for least
 * upper bound, widening and less or equals operations are handled by
 * {@link BaseLattice} in {@link BaseLattice#lub}, {@link BaseLattice#widening}
 * and {@link BaseLattice#lessOrEqual} methods, respectively.
 * 
 * @author <a href="mailto:vincenzo.arceri@unive.it">Vincenzo Arceri</a>
 */
public class Parity implements BaseNonRelationalValueDomain<Parity> {

	/**
	 * The abstract even element.
	 */
	public static final Parity EVEN = new Parity((byte) 3);

	/**
	 * The abstract odd element.
	 */
	public static final Parity ODD = new Parity((byte) 2);

	/**
	 * The abstract top element.
	 */
	public static final Parity TOP = new Parity((byte) 0);

	/**
	 * The abstract bottom element.
	 */
	public static final Parity BOTTOM = new Parity((byte) 1);

	private final byte parity;

	/**
	 * Builds the parity abstract domain, representing the top of the parity
	 * abstract domain.
	 */
	public Parity() {
		this((byte) 0);
	}

	/**
	 * Builds the parity instance for the given parity value.
	 * 
	 * @param parity the sign (0 = top, 1 = bottom, 2 = odd, 3 = even)
	 */
	public Parity(
			byte parity) {
		this.parity = parity;
	}

	@Override
	public Parity top() {
		return TOP;
	}

	@Override
	public Parity bottom() {
		return BOTTOM;
	}

	@Override
	public StructuredRepresentation representation() {
		if (isBottom())
			return Lattice.bottomRepresentation();
		if (isTop())
			return Lattice.topRepresentation();

		String repr;
		if (this == EVEN)
			repr = "Even";
		else
			repr = "Odd";

		return new StringRepresentation(repr);
	}

	@Override
	public Parity evalNullConstant(
			ProgramPoint pp,
			SemanticOracle oracle) {
		return top();
	}

	@Override
	public Parity evalNonNullConstant(
			Constant constant,
			ProgramPoint pp,
			SemanticOracle oracle) {
		if (constant.getValue() instanceof Integer) {
			Integer i = (Integer) constant.getValue();
			return i % 2 == 0 ? EVEN : ODD;
		}

		return top();
	}

	/**
	 * Yields whether or not this is the even parity.
	 * 
	 * @return {@code true} if that condition holds
	 */
	public boolean isEven() {
		return this == EVEN;
	}

	/**
	 * Yields whether or not this is the odd parity.
	 * 
	 * @return {@code true} if that condition holds
	 */
	public boolean isOdd() {
		return this == ODD;
	}

	@Override
	public Parity evalUnaryExpression(
			UnaryExpression expression,
			Parity arg,
			ProgramPoint pp,
			SemanticOracle oracle) {
		if (expression.getOperator() == NumericNegation.INSTANCE)
			return arg;
		return top();
	}

	@Override
	public Parity evalBinaryExpression(
			BinaryExpression expression,
			Parity left,
			Parity right,
			ProgramPoint pp,
			SemanticOracle oracle) {
		if (left.isTop() || right.isTop())
			return top();

		BinaryOperator operator = expression.getOperator();
		if (operator instanceof AdditionOperator || operator instanceof SubtractionOperator)
			if (right.equals(left))
				return EVEN;
			else
				return ODD;
		else if (operator instanceof MultiplicationOperator)
			if (left.isEven() || right.isEven())
				return EVEN;
			else
				return ODD;
		else if (operator instanceof DivisionOperator)
			if (left.isOdd())
				return right.isOdd() ? ODD : EVEN;
			else
				return right.isOdd() ? EVEN : TOP;
		else if (operator instanceof ModuloOperator || operator instanceof RemainderOperator)
			return TOP;

		return TOP;
	}

	@Override
	public Parity lubAux(
			Parity other)
			throws SemanticException {
		return TOP;
	}

	@Override
	public boolean lessOrEqualAux(
			Parity other)
			throws SemanticException {
		return false;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + parity;
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
		Parity other = (Parity) obj;
		if (parity != other.parity)
			return false;
		return true;
	}

	@Override
	public ValueEnvironment<Parity> assumeBinaryExpression(
			ValueEnvironment<Parity> environment,
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
				Parity eval = eval(right, environment, src, oracle);
				if (eval.isBottom())
					return environment.bottom();
				return environment.putState((Identifier) left, eval);
			} else if (right instanceof Identifier) {
				Parity eval = eval(left, environment, src, oracle);
				if (eval.isBottom())
					return environment.bottom();
				return environment.putState((Identifier) right, eval);
			}
		return environment;
	}
}
