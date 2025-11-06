package it.unive.lisa.analysis.string;

import it.unive.lisa.analysis.BaseLattice;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.lattices.Satisfiability;
import it.unive.lisa.analysis.nonrelational.value.BaseNonRelationalValueDomain;
import it.unive.lisa.lattices.string.StringConstant;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.TernaryExpression;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonEq;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGe;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGt;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLe;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLt;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonNe;
import it.unive.lisa.symbolic.value.operator.binary.StringConcat;
import it.unive.lisa.symbolic.value.operator.ternary.StringReplace;

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
public class StringConstantPropagation
		implements
		BaseNonRelationalValueDomain<StringConstant> {

	@Override
	public StringConstant evalConstant(
			Constant constant,
			ProgramPoint pp,
			SemanticOracle oracle) {
		if (constant.getValue() instanceof String)
			return new StringConstant((String) constant.getValue());

		return StringConstant.TOP;
	}

	@Override
	public StringConstant evalUnaryExpression(
			UnaryExpression expression,
			StringConstant arg,
			ProgramPoint pp,
			SemanticOracle oracle) {

		return StringConstant.TOP;
	}

	@Override
	public StringConstant evalBinaryExpression(
			BinaryExpression expression,
			StringConstant left,
			StringConstant right,
			ProgramPoint pp,
			SemanticOracle oracle) {
		if (expression.getOperator() instanceof StringConcat)
			return left.isTop() || right.isTop() ? StringConstant.TOP : new StringConstant(left.value + right.value);

		return StringConstant.TOP;
	}

	@Override
	public StringConstant evalTernaryExpression(
			TernaryExpression expression,
			StringConstant left,
			StringConstant middle,
			StringConstant right,
			ProgramPoint pp,
			SemanticOracle oracle) {

		if (expression.getOperator() instanceof StringReplace) {
			if (left.isTop() || right.isTop() || middle.isTop())
				return StringConstant.TOP;

			String replaced = left.value;
			replaced = replaced.replace(middle.value, right.value);

			return new StringConstant(replaced);
		}

		return StringConstant.TOP;

	}

	@Override
	public Satisfiability satisfiesBinaryExpression(
			BinaryExpression expression,
			StringConstant left,
			StringConstant right,
			ProgramPoint pp,
			SemanticOracle oracle) {

		if (left.isTop() || right.isTop())
			return Satisfiability.UNKNOWN;

		BinaryOperator operator = expression.getOperator();
		if (operator == ComparisonEq.INSTANCE)
			return left.value.equals(right.value) ? Satisfiability.SATISFIED : Satisfiability.NOT_SATISFIED;
		else if (operator == ComparisonGe.INSTANCE)
			return left.value.compareTo(right.value) >= 0 ? Satisfiability.SATISFIED : Satisfiability.NOT_SATISFIED;
		else if (operator == ComparisonGt.INSTANCE)
			return left.value.compareTo(right.value) > 0 ? Satisfiability.SATISFIED : Satisfiability.NOT_SATISFIED;
		else if (operator == ComparisonLe.INSTANCE)
			return left.value.compareTo(right.value) <= 0 ? Satisfiability.SATISFIED : Satisfiability.NOT_SATISFIED;
		else if (operator == ComparisonLt.INSTANCE)
			return left.value.compareTo(right.value) < 0 ? Satisfiability.SATISFIED : Satisfiability.NOT_SATISFIED;
		else if (operator == ComparisonNe.INSTANCE)
			return !left.value.equals(right.value) ? Satisfiability.SATISFIED : Satisfiability.NOT_SATISFIED;

		else
			return Satisfiability.UNKNOWN;
	}

	@Override
	public StringConstant top() {
		return StringConstant.TOP;
	}

	@Override
	public StringConstant bottom() {
		return StringConstant.BOTTOM;
	}

}
