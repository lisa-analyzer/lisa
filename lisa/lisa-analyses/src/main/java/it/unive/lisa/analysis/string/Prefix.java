package it.unive.lisa.analysis.string;

import java.util.Objects;

import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.combination.SmashedSumStringDomain;
import it.unive.lisa.analysis.lattices.Satisfiability;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.symbolic.value.operator.binary.StringConcat;
import it.unive.lisa.symbolic.value.operator.binary.StringEquals;
import it.unive.lisa.util.numeric.IntInterval;
import it.unive.lisa.util.numeric.MathNumber;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;

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
public class Prefix implements SmashedSumStringDomain<Prefix> {

	private final static Prefix TOP = new Prefix();
	private final static Prefix BOTTOM = new Prefix(null);
	private final String prefix;

	/**
	 * Builds the top prefix abstract element.
	 */
	public Prefix() {
		this("");
	}

	/**
	 * Builds a prefix abstract element.
	 * 
	 * @param prefix the prefix
	 */
	public Prefix(
			String prefix) {
		this.prefix = prefix;
	}

	@Override
	public Prefix lubAux(
			Prefix other)
			throws SemanticException {
		String otherPrefixString = other.prefix;
		StringBuilder result = new StringBuilder();

		int i = 0;
		while (i <= prefix.length() - 1 &&
				i <= otherPrefixString.length() - 1 &&
				prefix.charAt(i) == otherPrefixString.charAt(i)) {
			result.append(prefix.charAt(i++));
		}

		if (result.length() != 0)
			return new Prefix(result.toString());

		else
			return TOP;
	}

	@Override
	public boolean lessOrEqualAux(
			Prefix other)
			throws SemanticException {
		if (other.prefix.length() <= this.prefix.length()) {
			Prefix lub = this.lubAux(other);

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
		Prefix prefix1 = (Prefix) o;
		return Objects.equals(prefix, prefix1.prefix);
	}

	@Override
	public int hashCode() {
		return Objects.hash(prefix);
	}

	@Override
	public Prefix top() {
		return TOP;
	}

	@Override
	public Prefix bottom() {
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
	public Prefix evalNonNullConstant(
			Constant constant,
			ProgramPoint pp,
			SemanticOracle oracle) {
		if (constant.getValue() instanceof String) {
			String str = (String) constant.getValue();
			if (!str.isEmpty())
				return new Prefix(str);

		}

		return TOP;
	}

	@Override
	public Prefix evalBinaryExpression(
			BinaryOperator operator,
			Prefix left,
			Prefix right,
			ProgramPoint pp,
			SemanticOracle oracle) {
		if (operator == StringConcat.INSTANCE)
			return left;
		return TOP;
	}

	@Override
	public Satisfiability satisfiesBinaryExpression(
			BinaryOperator operator,
			Prefix left,
			Prefix right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (operator == StringEquals.INSTANCE && !left.prefix.startsWith(right.prefix))
			return Satisfiability.NOT_SATISFIED;
		return Satisfiability.UNKNOWN;
	}

	@Override
	public Prefix substring(
			long begin,
			long end) {
		if (isTop() || isBottom())
			return this;

		if (end <= getPrefix().length())
			return new Prefix(getPrefix().substring((int) begin, (int) end));
		else if (begin < getPrefix().length())
			return new Prefix(getPrefix().substring((int) begin));

		return new Prefix("");
	}

	@Override
	public IntInterval length() {
		return new IntInterval(new MathNumber(prefix.length()), MathNumber.PLUS_INFINITY);
	}

	@Override
	public IntInterval indexOf(
			Prefix s) {
		return new IntInterval(MathNumber.MINUS_ONE, MathNumber.PLUS_INFINITY);
	}

	@Override
	public Satisfiability containsChar(
			char c) {
		if (isTop())
			return Satisfiability.UNKNOWN;
		if (isBottom())
			return Satisfiability.BOTTOM;
		return this.prefix.contains(String.valueOf(c)) ? Satisfiability.SATISFIED : Satisfiability.UNKNOWN;
	}
}
