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
 * is even or odd, implemented as a {@link BaseNonRelationalValueDomain}.
 * 
 * @author <a href="mailto:vincenzo.arceri@unipr.it">Vincenzo Arceri</a>
 */
public class Parity
		implements
		BaseNonRelationalValueDomain<Parity.ParityLattice> {

	/**
	 * A lattice structure for parity values, which can be even, odd, top, or
	 * bottom.
	 * 
	 * @author <a href="mailto:vincenzo.arceri@unipr.it">Vincenzo Arceri</a>
	 */
	public static class ParityLattice
			implements
			BaseLattice<ParityLattice> {

		/**
		 * The abstract even element.
		 */
		public static final ParityLattice EVEN = new ParityLattice((byte) 3);

		/**
		 * The abstract odd element.
		 */
		public static final ParityLattice ODD = new ParityLattice((byte) 2);

		/**
		 * The abstract top element.
		 */
		public static final ParityLattice TOP = new ParityLattice((byte) 0);

		/**
		 * The abstract bottom element.
		 */
		public static final ParityLattice BOTTOM = new ParityLattice((byte) 1);

		private final byte parity;

		/**
		 * Builds the parity abstract domain, representing the top of the parity
		 * abstract domain.
		 */
		public ParityLattice() {
			this((byte) 0);
		}

		private ParityLattice(
				byte parity) {
			this.parity = parity;
		}

		@Override
		public ParityLattice top() {
			return TOP;
		}

		@Override
		public ParityLattice bottom() {
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
		public ParityLattice lubAux(
				ParityLattice other)
				throws SemanticException {
			return TOP;
		}

		@Override
		public boolean lessOrEqualAux(
				ParityLattice other)
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
			ParityLattice other = (ParityLattice) obj;
			if (parity != other.parity)
				return false;
			return true;
		}

		@Override
		public String toString() {
			if (isBottom())
				return "BOTTOM";
			if (isTop())
				return "TOP";
			return isEven() ? "EVEN" : "ODD";
		}

	}

	@Override
	public ParityLattice evalNullConstant(
			ProgramPoint pp,
			SemanticOracle oracle) {
		return ParityLattice.TOP;
	}

	@Override
	public ParityLattice evalNonNullConstant(
			Constant constant,
			ProgramPoint pp,
			SemanticOracle oracle) {
		if (constant.getValue() instanceof Integer) {
			Integer i = (Integer) constant.getValue();
			return i % 2 == 0 ? ParityLattice.EVEN : ParityLattice.ODD;
		}

		return ParityLattice.TOP;
	}

	@Override
	public ParityLattice evalUnaryExpression(
			UnaryExpression expression,
			ParityLattice arg,
			ProgramPoint pp,
			SemanticOracle oracle) {
		if (expression.getOperator() == NumericNegation.INSTANCE)
			return arg;
		return ParityLattice.TOP;
	}

	@Override
	public ParityLattice evalBinaryExpression(
			BinaryExpression expression,
			ParityLattice left,
			ParityLattice right,
			ProgramPoint pp,
			SemanticOracle oracle) {
		if (left.isTop() || right.isTop())
			return ParityLattice.TOP;

		BinaryOperator operator = expression.getOperator();
		if (operator instanceof AdditionOperator || operator instanceof SubtractionOperator)
			if (right.equals(left))
				return ParityLattice.EVEN;
			else
				return ParityLattice.ODD;
		else if (operator instanceof MultiplicationOperator)
			if (left.isEven() || right.isEven())
				return ParityLattice.EVEN;
			else
				return ParityLattice.ODD;
		else if (operator instanceof DivisionOperator)
			if (left.isOdd())
				return right.isOdd() ? ParityLattice.ODD : ParityLattice.EVEN;
			else
				return right.isOdd() ? ParityLattice.EVEN : ParityLattice.TOP;
		else if (operator instanceof ModuloOperator || operator instanceof RemainderOperator)
			return ParityLattice.TOP;

		return ParityLattice.TOP;
	}

	@Override
	public ValueEnvironment<ParityLattice> assumeBinaryExpression(
			ValueEnvironment<ParityLattice> environment,
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
				ParityLattice eval = eval(environment, right, src, oracle);
				if (eval.isBottom())
					return environment.bottom();
				return environment.putState((Identifier) left, eval);
			} else if (right instanceof Identifier) {
				ParityLattice eval = eval(environment, left, src, oracle);
				if (eval.isBottom())
					return environment.bottom();
				return environment.putState((Identifier) right, eval);
			}
		return environment;
	}

	@Override
	public ParityLattice top() {
		return ParityLattice.TOP;
	}

	@Override
	public ParityLattice bottom() {
		return ParityLattice.BOTTOM;
	}

}
