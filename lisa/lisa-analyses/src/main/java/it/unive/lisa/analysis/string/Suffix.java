package it.unive.lisa.analysis.string;

import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.combination.SmashedSumStringDomain;
import it.unive.lisa.analysis.lattices.Satisfiability;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.symbolic.value.operator.binary.StringConcat;
import it.unive.lisa.symbolic.value.operator.binary.StringContains;
import it.unive.lisa.symbolic.value.operator.binary.StringEquals;
import it.unive.lisa.util.numeric.IntInterval;
import it.unive.lisa.util.numeric.MathNumber;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;
import java.util.Objects;

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
public class Suffix implements SmashedSumStringDomain<Suffix> {

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
			BinaryOperator operator,
			Suffix left,
			Suffix right,
			ProgramPoint pp,
			SemanticOracle oracle) {
		if (operator == StringConcat.INSTANCE)
			return right;
		return TOP;
	}

	@Override
	public Satisfiability satisfiesBinaryExpression(
			BinaryOperator operator,
			Suffix left,
			Suffix right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (operator == StringContains.INSTANCE && !left.suffix.endsWith(right.suffix))
			return Satisfiability.NOT_SATISFIED;
		if (operator == StringEquals.INSTANCE && !left.suffix.endsWith(right.suffix))
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
}