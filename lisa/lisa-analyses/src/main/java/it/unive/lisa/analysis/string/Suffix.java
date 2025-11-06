package it.unive.lisa.analysis.string;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.combination.constraints.WholeValueStringDomain;
import it.unive.lisa.analysis.combination.smash.SmashedSumStringDomain;
import it.unive.lisa.analysis.lattices.Satisfiability;
import it.unive.lisa.lattices.string.StrSuffix;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
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
 * The suffix string abstract domain.
 *
 * @author <a href="mailto:vincenzo.arceri@unipr.it">Vincenzo Arceri</a>
 * @author <a href="mailto:sergiosalvatore.evola@studenti.unipr.it">Sergio
 *             Salvatore Evola</a>
 * 
 * @see <a href=
 *          "https://link.springer.com/chapter/10.1007/978-3-642-24559-6_34">
 *          https://link.springer.com/chapter/10.1007/978-3-642-24559-6_34</a>
 */
public class Suffix
		implements
		SmashedSumStringDomain<StrSuffix>,
		WholeValueStringDomain<StrSuffix> {

	@Override
	public StrSuffix evalConstant(
			Constant constant,
			ProgramPoint pp,
			SemanticOracle oracle) {
		if (constant.getValue() instanceof String) {
			String str = (String) constant.getValue();
			if (!str.isEmpty())
				return new StrSuffix(str);
		}

		return StrSuffix.TOP;
	}

	@Override
	public StrSuffix evalBinaryExpression(
			BinaryExpression expression,
			StrSuffix left,
			StrSuffix right,
			ProgramPoint pp,
			SemanticOracle oracle) {
		if (expression.getOperator() == StringConcat.INSTANCE)
			return right;
		return StrSuffix.TOP;
	}

	@Override
	public Satisfiability satisfiesBinaryExpression(
			BinaryExpression expression,
			StrSuffix left,
			StrSuffix right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (expression.getOperator() == StringEquals.INSTANCE && !left.suffix.endsWith(right.suffix))
			return Satisfiability.NOT_SATISFIED;
		return Satisfiability.UNKNOWN;
	}

	@Override
	public StrSuffix substring(
			StrSuffix current,
			long begin,
			long end) {
		return new StrSuffix("");
	}

	@Override
	public IntInterval length(
			StrSuffix current) {
		return new IntInterval(new MathNumber(current.suffix.length()), MathNumber.PLUS_INFINITY);
	}

	@Override
	public IntInterval indexOf(
			StrSuffix current,
			StrSuffix other) {
		return new IntInterval(MathNumber.MINUS_ONE, MathNumber.PLUS_INFINITY);
	}

	@Override
	public Satisfiability containsChar(
			StrSuffix current,
			char c) {
		if (current.isTop())
			return Satisfiability.UNKNOWN;
		if (current.isBottom())
			return Satisfiability.BOTTOM;
		return current.suffix.contains(String.valueOf(c)) ? Satisfiability.SATISFIED : Satisfiability.UNKNOWN;
	}

	@Override
	public StrSuffix substring(
			StrSuffix current,
			Set<BinaryExpression> a1,
			Set<BinaryExpression> a2,
			ProgramPoint pp)
			throws SemanticException {
		return StrSuffix.TOP;
	}

	@Override
	public Set<BinaryExpression> indexOf_constr(
			BinaryExpression expression,
			StrSuffix current,
			StrSuffix other,
			ProgramPoint pp)
			throws SemanticException {
		if (current.isBottom() || other.isBottom())
			return null;

		IntInterval indexes = indexOf(current, other);
		BooleanType booleanType = pp.getProgram().getTypes().getBooleanType();

		Set<BinaryExpression> constr = new HashSet<>();
		try {
			constr.add(
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
				constr.add(
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
	public StrSuffix top() {
		return StrSuffix.TOP;
	}

	@Override
	public StrSuffix bottom() {
		return StrSuffix.BOTTOM;
	}

}
