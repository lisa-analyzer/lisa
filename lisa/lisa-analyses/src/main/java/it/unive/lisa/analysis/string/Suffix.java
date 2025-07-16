package it.unive.lisa.analysis.string;

import it.unive.lisa.analysis.BaseLattice;
import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.combination.constraints.WholeValueElement;
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
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
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
		SmashedSumStringDomain<Suffix.Suff>,
		WholeValueStringDomain<Suffix.Suff> {

	/**
	 * A lattice structure tracking suffixes of strings.
	 * 
	 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
	 */
	public static class Suff
			implements
			BaseLattice<Suff>,
			WholeValueElement<Suff> {

		private final static Suff TOP = new Suff();

		private final static Suff BOTTOM = new Suff(null);

		private final String suffix;

		/**
		 * Builds the top suffix abstract element.
		 */
		public Suff() {
			this("");
		}

		/**
		 * Builds a suffix abstract element.
		 *
		 * @param suffix the suffix
		 */
		public Suff(
				String suffix) {
			this.suffix = suffix;
		}

		@Override
		public Suff lubAux(
				Suff other)
				throws SemanticException {
			String otherSuffix = other.suffix;
			StringBuilder result = new StringBuilder();

			int i = suffix.length() - 1;
			int j = otherSuffix.length() - 1;

			while (i >= 0 && j >= 0 && suffix.charAt(i) == otherSuffix.charAt(j)) {
				result.append(suffix.charAt(i--));
				j--;
			}

			if (result.length() != 0)
				return new Suff(result.reverse().toString());

			else
				return TOP;
		}

		@Override
		public boolean lessOrEqualAux(
				Suff other)
				throws SemanticException {
			if (other.suffix.length() <= this.suffix.length()) {
				Suff lub = this.lubAux(other);
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
			Suff suffix1 = (Suff) o;
			return Objects.equals(suffix, suffix1.suffix);
		}

		@Override
		public int hashCode() {
			return Objects.hash(suffix);
		}

		@Override
		public Suff top() {
			return TOP;
		}

		@Override
		public Suff bottom() {
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
		public Set<BinaryExpression> constraints(
				ValueExpression e,
				ProgramPoint pp)
				throws SemanticException {
			if (isBottom())
				return null;

			BooleanType booleanType = pp.getProgram().getTypes().getBooleanType();
			UnaryExpression strlen = new UnaryExpression(
					pp.getProgram().getTypes().getIntegerType(),
					e,
					StringLength.INSTANCE,
					pp.getLocation());

			if (isTop())
				return Collections
						.singleton(
								new BinaryExpression(
										booleanType,
										new Constant(pp.getProgram().getTypes().getIntegerType(), 0, pp.getLocation()),
										strlen,
										ComparisonLe.INSTANCE,
										e.getCodeLocation()));

			return Set
					.of(
							new BinaryExpression(
									booleanType,
									new Constant(
											pp.getProgram().getTypes().getIntegerType(),
											suffix.length(),
											pp.getLocation()),
									strlen,
									ComparisonLe.INSTANCE,
									e.getCodeLocation()),
							new BinaryExpression(
									booleanType,
									new Constant(pp.getProgram().getTypes().getStringType(), suffix, pp.getLocation()),
									e,
									StringEndsWith.INSTANCE,
									e.getCodeLocation()));
		}

		@Override
		public Suff generate(
				Set<BinaryExpression> constraints,
				ProgramPoint pp)
				throws SemanticException {
			if (constraints == null)
				return bottom();

			for (BinaryExpression expr : constraints)
				if ((expr.getOperator() instanceof ComparisonEq || expr.getOperator() instanceof StringEndsWith)
						&& expr.getLeft() instanceof Constant
						&& ((Constant) expr.getLeft()).getValue() instanceof String)
					return new Suff(((Constant) expr.getLeft()).getValue().toString());

			return TOP;
		}

	}

	@Override
	public Suff evalNonNullConstant(
			Constant constant,
			ProgramPoint pp,
			SemanticOracle oracle) {
		if (constant.getValue() instanceof String) {
			String str = (String) constant.getValue();
			if (!str.isEmpty())
				return new Suff(str);
		}

		return Suff.TOP;
	}

	@Override
	public Suff evalBinaryExpression(
			BinaryExpression expression,
			Suff left,
			Suff right,
			ProgramPoint pp,
			SemanticOracle oracle) {
		if (expression.getOperator() == StringConcat.INSTANCE)
			return right;
		return Suff.TOP;
	}

	@Override
	public Satisfiability satisfiesBinaryExpression(
			BinaryExpression expression,
			Suff left,
			Suff right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (expression.getOperator() == StringEquals.INSTANCE && !left.suffix.endsWith(right.suffix))
			return Satisfiability.NOT_SATISFIED;
		return Satisfiability.UNKNOWN;
	}

	@Override
	public Suff substring(
			Suff current,
			long begin,
			long end) {
		return new Suff("");
	}

	@Override
	public IntInterval length(
			Suff current) {
		return new IntInterval(new MathNumber(current.suffix.length()), MathNumber.PLUS_INFINITY);
	}

	@Override
	public IntInterval indexOf(
			Suff current,
			Suff other) {
		return new IntInterval(MathNumber.MINUS_ONE, MathNumber.PLUS_INFINITY);
	}

	@Override
	public Satisfiability containsChar(
			Suff current,
			char c) {
		if (current.isTop())
			return Satisfiability.UNKNOWN;
		if (current.isBottom())
			return Satisfiability.BOTTOM;
		return current.suffix.contains(String.valueOf(c)) ? Satisfiability.SATISFIED : Satisfiability.UNKNOWN;
	}

	@Override
	public Suff substring(
			Suff current,
			Set<BinaryExpression> a1,
			Set<BinaryExpression> a2,
			ProgramPoint pp)
			throws SemanticException {
		return Suff.TOP;
	}

	@Override
	public Set<BinaryExpression> indexOf_constr(
			BinaryExpression expression,
			Suff current,
			Suff other,
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
	public Suff top() {
		return Suff.TOP;
	}

	@Override
	public Suff bottom() {
		return Suff.BOTTOM;
	}

}
