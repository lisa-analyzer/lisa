package it.unive.lisa.analysis.string;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.combination.constraints.WholeValueStringDomain;
import it.unive.lisa.analysis.combination.smash.SmashedSumStringDomain;
import it.unive.lisa.analysis.lattices.Satisfiability;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonEq;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGe;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLe;
import it.unive.lisa.symbolic.value.operator.binary.StringConcat;
import it.unive.lisa.symbolic.value.operator.binary.StringEndsWith;
import it.unive.lisa.symbolic.value.operator.binary.StringEquals;
import it.unive.lisa.symbolic.value.operator.unary.StringLength;
import it.unive.lisa.type.BooleanType;
import it.unive.lisa.util.numeric.IntInterval;
import it.unive.lisa.util.numeric.MathNumber;
import it.unive.lisa.util.numeric.MathNumberConversionException;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;

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
		SmashedSumStringDomain<Suffix>,
		WholeValueStringDomain<Suffix> {

	private final static Suffix TOP = new Suffix();
	private final static Suffix BOTTOM = new Suffix(null);
	private final String suffix;

	/**
	 * Builds the top suffix abstract element.
	 */
	public Suffix() {
		this("");
	}

	/**
	 * Builds a suffix abstract element.
	 *
	 * @param suffix the suffix
	 */
	public Suffix(
			String suffix) {
		this.suffix = suffix;
	}

	@Override
	public Suffix lubAux(
			Suffix other)
			throws SemanticException {
		String otherSuffix = other.suffix;
		StringBuilder result = new StringBuilder();

		int i = suffix.length() - 1;
		int j = otherSuffix.length() - 1;

		while (i >= 0 && j >= 0 &&
				suffix.charAt(i) == otherSuffix.charAt(j)) {
			result.append(suffix.charAt(i--));
			j--;
		}

		if (result.length() != 0)
			return new Suffix(result.reverse().toString());

		else
			return TOP;
	}

	@Override
	public boolean lessOrEqualAux(
			Suffix other)
			throws SemanticException {
		if (other.suffix.length() <= this.suffix.length()) {
			Suffix lub = this.lubAux(other);

			return lub.suffix.length() == other.suffix.length();
		}

		return false;
	}

	@Override
	public boolean equals(
			Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		Suffix suffix1 = (Suffix) o;
		return Objects.equals(suffix, suffix1.suffix);
	}

	@Override
	public int hashCode() {
		return Objects.hash(suffix);
	}

	@Override
	public Suffix top() {
		return TOP;
	}

	@Override
	public Suffix bottom() {
		return BOTTOM;
	}

	@Override
	public StructuredRepresentation representation() {
		if (isBottom())
			return Lattice.bottomRepresentation();
		if (isTop())
			return Lattice.topRepresentation();

		return new StringRepresentation('*' + suffix);
	}

	/**
	 * Yields the suffix of this abstract value.
	 *
	 * @return the suffix of this abstract value.
	 */
	public String getSuffix() {
		return this.suffix;
	}

	@Override
	public Suffix evalNonNullConstant(
			Constant constant,
			ProgramPoint pp,
			SemanticOracle oracle) {
		if (constant.getValue() instanceof String) {
			String str = (String) constant.getValue();
			if (!str.isEmpty())
				return new Suffix(str);
		}

		return TOP;
	}

	@Override
	public Suffix evalBinaryExpression(
			BinaryExpression expression,
			Suffix left,
			Suffix right,
			ProgramPoint pp,
			SemanticOracle oracle) {
		if (expression.getOperator() == StringConcat.INSTANCE)
			return right;
		return TOP;
	}

	@Override
	public Satisfiability satisfiesBinaryExpression(
			BinaryExpression expression,
			Suffix left,
			Suffix right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (expression.getOperator() == StringEquals.INSTANCE && !left.suffix.endsWith(right.suffix))
			return Satisfiability.NOT_SATISFIED;
		return Satisfiability.UNKNOWN;
	}

	@Override
	public Suffix substring(
			long begin,
			long end) {
		return new Suffix("");
	}

	@Override
	public IntInterval length() {
		return new IntInterval(new MathNumber(suffix.length()), MathNumber.PLUS_INFINITY);
	}

	@Override
	public IntInterval indexOf(
			Suffix s) {
		return new IntInterval(MathNumber.MINUS_ONE, MathNumber.PLUS_INFINITY);
	}

	@Override
	public Satisfiability containsChar(
			char c) {
		if (isTop())
			return Satisfiability.UNKNOWN;
		if (isBottom())
			return Satisfiability.BOTTOM;
		return this.suffix.contains(String.valueOf(c)) ? Satisfiability.SATISFIED : Satisfiability.UNKNOWN;
	}

	@Override
	public Set<BinaryExpression> constraints(ValueExpression e, ProgramPoint pp) throws SemanticException {
		if (isBottom())
			return null;
		
		BooleanType booleanType = pp.getProgram().getTypes().getBooleanType();
		UnaryExpression strlen = new UnaryExpression(pp.getProgram().getTypes().getIntegerType(), e, StringLength.INSTANCE, pp.getLocation());
		
		if (isTop()) 
			return Collections.singleton(
				new BinaryExpression(
					booleanType, 
					new Constant(pp.getProgram().getTypes().getIntegerType(), 0, pp.getLocation()),
					strlen, 
					ComparisonLe.INSTANCE, 
					e.getCodeLocation()
			));
		
		return Set.of(
			new BinaryExpression(
					booleanType, 
					new Constant(pp.getProgram().getTypes().getIntegerType(), suffix.length(), pp.getLocation()),
					strlen, 
					ComparisonLe.INSTANCE, 
					e.getCodeLocation()
			), new BinaryExpression(
					booleanType, 
					new Constant(pp.getProgram().getTypes().getStringType(), suffix, pp.getLocation()),
					e, 
					StringEndsWith.INSTANCE, 
					e.getCodeLocation()
			));
	}

	@Override
	public Suffix generate(Set<BinaryExpression> constraints, ProgramPoint pp) throws SemanticException {
		if (constraints == null)
			return bottom();
		
		for (BinaryExpression expr : constraints) 
			if ((expr.getOperator() instanceof ComparisonEq || expr.getOperator() instanceof StringEndsWith)
					&& expr.getLeft() instanceof Constant con 
					&& con.getValue() instanceof String val)
				return new Suffix(val);

		return TOP;
	}

	@Override
	public Suffix substring(Set<BinaryExpression> a1, Set<BinaryExpression> a2, ProgramPoint pp) throws SemanticException {
		return TOP;
	}

	@Override
	public Set<BinaryExpression> indexOf_constr(BinaryExpression expression, Suffix other, ProgramPoint pp)
			throws SemanticException {
		if (isBottom() || other.isBottom())
			return null;

		IntInterval indexes = indexOf(other);
		BooleanType booleanType = pp.getProgram().getTypes().getBooleanType();

		Set<BinaryExpression> constr = new HashSet<>();
		try {
			constr.add(new BinaryExpression(
						booleanType, 
						new Constant(pp.getProgram().getTypes().getIntegerType(), indexes.getLow().toInt(), pp.getLocation()),
						expression, 
						ComparisonLe.INSTANCE, 
						pp.getLocation()
				));
			if (indexes.getHigh().isFinite()) 
				constr.add(new BinaryExpression(
						booleanType, 
						new Constant(pp.getProgram().getTypes().getIntegerType(), indexes.getHigh().toInt(), pp.getLocation()), 
						expression, 
						ComparisonGe.INSTANCE, 
						pp.getLocation()
				));
		} catch (MathNumberConversionException e1) {
			throw new SemanticException("Cannot convert stirng indexof bound to int", e1);
		}
		return constr;
	}
}