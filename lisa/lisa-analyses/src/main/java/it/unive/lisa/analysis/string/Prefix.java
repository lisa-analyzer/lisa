package it.unive.lisa.analysis.string;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.combination.constraints.WholeValueStringDomain;
import it.unive.lisa.analysis.combination.smash.SmashedSumStringDomain;
import it.unive.lisa.analysis.lattices.Satisfiability;
import it.unive.lisa.lattices.string.StrPrefix;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonEq;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGe;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLe;
import it.unive.lisa.symbolic.value.operator.binary.StringConcat;
import it.unive.lisa.symbolic.value.operator.binary.StringEquals;
import it.unive.lisa.type.BooleanType;
import it.unive.lisa.util.numeric.IntInterval;
import it.unive.lisa.util.numeric.MathNumber;
import it.unive.lisa.util.numeric.MathNumberConversionException;
import java.util.HashSet;
import java.util.Set;

/**
 * The prefix string abstract domain.
 *
 * @author <a href="mailto:vincenzo.arceri@unipr.it">Vincenzo Arceri</a>
 * @author <a href="mailto:sergiosalvatore.evola@studenti.unipr.it">Sergio
 *             Salvatore Evola</a>
 * 
 * @see <a href=
 *          "https://link.springer.com/chapter/10.1007/978-3-642-24559-6_34">
 *          https://link.springer.com/chapter/10.1007/978-3-642-24559-6_34</a>
 */
public class Prefix
		implements
		SmashedSumStringDomain<StrPrefix>,
		WholeValueStringDomain<StrPrefix> {

	@Override
	public StrPrefix evalNonNullConstant(
			Constant constant,
			ProgramPoint pp,
			SemanticOracle oracle) {
		if (constant.getValue() instanceof String) {
			String str = (String) constant.getValue();
			if (!str.isEmpty())
				return new StrPrefix(str);

		}

		return StrPrefix.TOP;
	}

	@Override
	public StrPrefix evalBinaryExpression(
			BinaryExpression expression,
			StrPrefix left,
			StrPrefix right,
			ProgramPoint pp,
			SemanticOracle oracle) {
		if (expression.getOperator() == StringConcat.INSTANCE)
			return left;
		return StrPrefix.TOP;
	}

	@Override
	public Satisfiability satisfiesBinaryExpression(
			BinaryExpression expression,
			StrPrefix left,
			StrPrefix right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (expression.getOperator() == StringEquals.INSTANCE && !left.prefix.startsWith(right.prefix))
			return Satisfiability.NOT_SATISFIED;
		return Satisfiability.UNKNOWN;
	}

	@Override
	public StrPrefix substring(
			StrPrefix s,
			long begin,
			long end) {
		if (s.isTop() || s.isBottom())
			return s;

		if (end <= s.prefix.length())
			return new StrPrefix(s.prefix.substring((int) begin, (int) end));
		else if (begin < s.prefix.length())
			return new StrPrefix(s.prefix.substring((int) begin));

		return new StrPrefix("");
	}

	@Override
	public IntInterval length(
			StrPrefix s) {
		return new IntInterval(new MathNumber(s.prefix.length()), MathNumber.PLUS_INFINITY);
	}

	@Override
	public IntInterval indexOf(
			StrPrefix current,
			StrPrefix other) {
		return new IntInterval(MathNumber.MINUS_ONE, MathNumber.PLUS_INFINITY);
	}

	@Override
	public Satisfiability containsChar(
			StrPrefix current,
			char c) {
		if (current.isTop())
			return Satisfiability.UNKNOWN;
		if (current.isBottom())
			return Satisfiability.BOTTOM;
		return current.prefix.contains(String.valueOf(c)) ? Satisfiability.SATISFIED : Satisfiability.UNKNOWN;
	}

	@Override
	public StrPrefix substring(
			StrPrefix prefix,
			Set<BinaryExpression> a1,
			Set<BinaryExpression> a2,
			ProgramPoint pp)
			throws SemanticException {
		if (prefix.isBottom() || a1 == null || a2 == null)
			return bottom();

		Integer minI = null;
		for (BinaryExpression expr : a1)
			if (expr.getLeft() instanceof Constant && ((Constant) expr.getLeft()).getValue() instanceof Integer) {
				Integer val = (Integer) ((Constant) expr.getLeft()).getValue();
				if (expr.getOperator() instanceof ComparisonEq)
					minI = val;
				else if (expr.getOperator() instanceof ComparisonLe)
					minI = val;
			}
		if (minI == null || minI < 0)
			minI = 0;

		Integer minJ = null;
		for (BinaryExpression expr : a2)
			if (expr.getLeft() instanceof Constant && ((Constant) expr.getLeft()).getValue() instanceof Integer) {
				Integer val = (Integer) ((Constant) expr.getLeft()).getValue();
				if (expr.getOperator() instanceof ComparisonEq)
					minJ = val;
				else if (expr.getOperator() instanceof ComparisonLe)
					minJ = val;
			}
		if (minJ != null && minJ < minI)
			minJ = minI;

		// minI is always >= 0
		// minJ is null (infinity) or >= minI
		if (minJ != null && minJ <= prefix.prefix.length())
			return new StrPrefix(prefix.prefix.substring(minI, minJ));
		if (minI <= prefix.prefix.length())
			return new StrPrefix(prefix.prefix.substring(minI));
		return StrPrefix.TOP;
	}

	@Override
	public Set<BinaryExpression> indexOf_constr(
			BinaryExpression expression,
			StrPrefix current,
			StrPrefix other,
			ProgramPoint pp)
			throws SemanticException {
		if (current.isBottom() || other.isBottom())
			return null;

		IntInterval indexes = indexOf(current, other);
		BooleanType booleanType = pp.getProgram().getTypes().getBooleanType();

		Set<BinaryExpression> constr = new HashSet<>();
		try {
			constr
					.add(
							new BinaryExpression(
									booleanType,
									new Constant(
											pp.getProgram().getTypes().getIntegerType(),
											indexes.getLow().toInt(),
											pp.getLocation()),
									expression,
									ComparisonLe.INSTANCE,
									pp.getLocation()));
			if (indexes.getHigh().isFinite())
				constr
						.add(
								new BinaryExpression(
										booleanType,
										new Constant(
												pp.getProgram().getTypes().getIntegerType(),
												indexes.getHigh().toInt(),
												pp.getLocation()),
										expression,
										ComparisonGe.INSTANCE,
										pp.getLocation()));
		} catch (MathNumberConversionException e1) {
			throw new SemanticException("Cannot convert stirng indexof bound to int", e1);
		}
		return constr;
	}

	@Override
	public StrPrefix top() {
		return StrPrefix.TOP;
	}

	@Override
	public StrPrefix bottom() {
		return StrPrefix.BOTTOM;
	}

}
