package it.unive.lisa.analysis.numeric;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.nonrelational.value.BaseNonRelationalValueDomain;
import it.unive.lisa.analysis.nonrelational.value.ValueEnvironment;
import it.unive.lisa.lattices.numeric.ParityLattice;
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

/**
 * The overflow-insensitive Parity abstract domain, tracking if a numeric value
 * is even or odd, implemented as a {@link BaseNonRelationalValueDomain}.
 * 
 * @author <a href="mailto:vincenzo.arceri@unipr.it">Vincenzo Arceri</a>
 */
public class Parity
		implements
		BaseNonRelationalValueDomain<ParityLattice> {

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
