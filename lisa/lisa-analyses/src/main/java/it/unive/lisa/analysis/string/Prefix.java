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
import it.unive.lisa.symbolic.value.operator.binary.StringEquals;
import it.unive.lisa.symbolic.value.operator.binary.StringStartsWith;
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
		SmashedSumStringDomain<Prefix.Pref>,
		WholeValueStringDomain<Prefix.Pref> {

	/**
	 * A lattice structure tracking prefixes of strings.
	 * 
	 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
	 */
	public static class Pref
			implements
			BaseLattice<Pref>,
			WholeValueElement<Pref> {

		private final static Pref TOP = new Pref();

		private final static Pref BOTTOM = new Pref(null);

		private final String prefix;

		/**
		 * Builds the top prefix abstract element.
		 */
		public Pref() {
			this("");
		}

		/**
		 * Builds a prefix abstract element.
		 * 
		 * @param prefix the prefix
		 */
		public Pref(
				String prefix) {
			this.prefix = prefix;
		}

		@Override
		public Pref lubAux(
				Pref other)
				throws SemanticException {
			String otherPrefixString = other.prefix;
			StringBuilder result = new StringBuilder();

			int i = 0;
			while (i <= prefix.length() - 1
					&& i <= otherPrefixString.length() - 1
					&& prefix.charAt(i) == otherPrefixString.charAt(i)) {
				result.append(prefix.charAt(i++));
			}

			if (result.length() != 0)
				return new Pref(result.toString());

			else
				return TOP;
		}

		@Override
		public boolean lessOrEqualAux(
				Pref other)
				throws SemanticException {
			if (other.prefix.length() <= this.prefix.length()) {
				Pref lub = this.lubAux(other);

				return lub.prefix.length() == other.prefix.length();
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
			Pref prefix1 = (Pref) o;
			return Objects.equals(prefix, prefix1.prefix);
		}

		@Override
		public int hashCode() {
			return Objects.hash(prefix);
		}

		@Override
		public Pref top() {
			return TOP;
		}

		@Override
		public Pref bottom() {
			return BOTTOM;
		}

		@Override
		public StructuredRepresentation representation() {
			if (isBottom())
				return Lattice.bottomRepresentation();
			if (isTop())
				return Lattice.topRepresentation();

			return new StringRepresentation(prefix + '*');
		}

		/**
		 * Yields the prefix of this abstract value.
		 * 
		 * @return the prefix of this abstract value.
		 */
		public String getPrefix() {
			return this.prefix;
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
											prefix.length(),
											pp.getLocation()),
									strlen,
									ComparisonLe.INSTANCE,
									e.getCodeLocation()),
							new BinaryExpression(
									booleanType,
									new Constant(pp.getProgram().getTypes().getStringType(), prefix, pp.getLocation()),
									e,
									StringStartsWith.INSTANCE,
									e.getCodeLocation()));
		}

		@Override
		public Pref generate(
				Set<BinaryExpression> constraints,
				ProgramPoint pp)
				throws SemanticException {
			if (constraints == null)
				return bottom();

			for (BinaryExpression expr : constraints)
				if ((expr.getOperator() instanceof ComparisonEq || expr.getOperator() instanceof StringStartsWith)
						&& expr.getLeft() instanceof Constant
						&& ((Constant) expr.getLeft()).getValue() instanceof String)
					return new Pref(((Constant) expr.getLeft()).getValue().toString());

			return TOP;
		}

	}

	@Override
	public Pref evalNonNullConstant(
			Constant constant,
			ProgramPoint pp,
			SemanticOracle oracle) {
		if (constant.getValue() instanceof String) {
			String str = (String) constant.getValue();
			if (!str.isEmpty())
				return new Pref(str);

		}

		return Pref.TOP;
	}

	@Override
	public Pref evalBinaryExpression(
			BinaryExpression expression,
			Pref left,
			Pref right,
			ProgramPoint pp,
			SemanticOracle oracle) {
		if (expression.getOperator() == StringConcat.INSTANCE)
			return left;
		return Pref.TOP;
	}

	@Override
	public Satisfiability satisfiesBinaryExpression(
			BinaryExpression expression,
			Pref left,
			Pref right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (expression.getOperator() == StringEquals.INSTANCE && !left.prefix.startsWith(right.prefix))
			return Satisfiability.NOT_SATISFIED;
		return Satisfiability.UNKNOWN;
	}

	@Override
	public Pref substring(
			Pref s,
			long begin,
			long end) {
		if (s.isTop() || s.isBottom())
			return s;

		if (end <= s.getPrefix().length())
			return new Pref(s.getPrefix().substring((int) begin, (int) end));
		else if (begin < s.getPrefix().length())
			return new Pref(s.getPrefix().substring((int) begin));

		return new Pref("");
	}

	@Override
	public IntInterval length(
			Pref s) {
		return new IntInterval(new MathNumber(s.getPrefix().length()), MathNumber.PLUS_INFINITY);
	}

	@Override
	public IntInterval indexOf(
			Pref current,
			Pref other) {
		return new IntInterval(MathNumber.MINUS_ONE, MathNumber.PLUS_INFINITY);
	}

	@Override
	public Satisfiability containsChar(
			Pref current,
			char c) {
		if (current.isTop())
			return Satisfiability.UNKNOWN;
		if (current.isBottom())
			return Satisfiability.BOTTOM;
		return current.getPrefix().contains(String.valueOf(c)) ? Satisfiability.SATISFIED : Satisfiability.UNKNOWN;
	}

	@Override
	public Pref substring(
			Pref prefix,
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
		if (minJ != null && minJ <= prefix.getPrefix().length())
			return new Pref(prefix.getPrefix().substring(minI, minJ));
		if (minI <= prefix.getPrefix().length())
			return new Pref(prefix.getPrefix().substring(minI));
		return Pref.TOP;
	}

	@Override
	public Set<BinaryExpression> indexOf_constr(
			BinaryExpression expression,
			Pref current,
			Pref other,
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
	public Pref top() {
		return Pref.TOP;
	}

	@Override
	public Pref bottom() {
		return Pref.BOTTOM;
	}

}
