package it.unive.lisa.analysis.string;

import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticDomain.Satisfiability;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.nonrelational.value.BaseNonRelationalValueDomain;
import it.unive.lisa.analysis.representation.DomainRepresentation;
import it.unive.lisa.analysis.representation.StringRepresentation;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.symbolic.value.operator.binary.StringConcat;
import it.unive.lisa.symbolic.value.operator.binary.StringContains;
import it.unive.lisa.symbolic.value.operator.binary.StringEndsWith;
import it.unive.lisa.symbolic.value.operator.binary.StringEquals;
import it.unive.lisa.symbolic.value.operator.binary.StringIndexOf;
import it.unive.lisa.symbolic.value.operator.binary.StringStartsWith;
import it.unive.lisa.symbolic.value.operator.ternary.StringReplace;
import it.unive.lisa.symbolic.value.operator.ternary.TernaryOperator;
import it.unive.lisa.util.numeric.IntInterval;
import it.unive.lisa.util.numeric.MathNumber;
import java.util.Objects;

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
public class Prefix implements BaseNonRelationalValueDomain<Prefix>, ContainsCharProvider {

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
	public Prefix(String prefix) {
		this.prefix = prefix;
	}

	@Override
	public Prefix lubAux(Prefix other) throws SemanticException {
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
	public boolean lessOrEqualAux(Prefix other) throws SemanticException {
		if (other.prefix.length() <= this.prefix.length()) {
			Prefix lub = this.lubAux(other);

			return lub.prefix.length() == other.prefix.length();
		}

		return false;
	}

	@Override
	public boolean equals(Object o) {
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
	public DomainRepresentation representation() {
		if (isBottom())
			return Lattice.bottomRepresentation();
		if (isTop())
			return Lattice.topRepresentation();

		return new StringRepresentation(prefix + '*');
	}

	@Override
	public Prefix evalNonNullConstant(Constant constant, ProgramPoint pp) {
		if (constant.getValue() instanceof String) {
			String str = (String) constant.getValue();
			if (!str.isEmpty())
				return new Prefix(str);

		}

		return TOP;
	}

	@Override
	public Prefix evalBinaryExpression(BinaryOperator operator, Prefix left, Prefix right, ProgramPoint pp) {
		if (operator == StringConcat.INSTANCE) {
			return left;
		} else if (operator == StringContains.INSTANCE ||
				operator == StringEndsWith.INSTANCE ||
				operator == StringEquals.INSTANCE ||
				operator == StringIndexOf.INSTANCE ||
				operator == StringStartsWith.INSTANCE) {
			return TOP;
		}

		return TOP;
	}

	@Override
	public Prefix evalTernaryExpression(TernaryOperator operator, Prefix left, Prefix middle, Prefix right,
			ProgramPoint pp) throws SemanticException {

		if (operator == StringReplace.INSTANCE) {
			String replace = right.getPrefix();
			String string = middle.getPrefix();
			String target = left.getPrefix();

			if (!target.contains(replace))
				return this;

			return new Prefix(target.replace(replace, string));
		}

		return TOP;
	}

	/**
	 * Yields the prefix of this abstract value.
	 * 
	 * @return the prefix of this abstract value.
	 */
	public String getPrefix() {
		return this.prefix;
	}

	/**
	 * Yields the prefix corresponding to the substring of this prefix between
	 * two indexes.
	 * 
	 * @param begin where the substring starts
	 * @param end   where the substring ends
	 * 
	 * @return the prefix corresponding to the substring of this prefix between
	 *             two indexes
	 */
	public Prefix substring(long begin, long end) {
		if (isTop() || isBottom())
			return this;

		if (end <= getPrefix().length())
			return new Prefix(getPrefix().substring((int) begin, (int) end));
		else if (begin < getPrefix().length())
			return new Prefix(getPrefix().substring((int) begin));

		return new Prefix("");
	}

	/**
	 * Yields the {@link IntInterval} containing the minimum and maximum length
	 * of this abstract value.
	 * 
	 * @return the minimum and maximum length of this abstract value
	 */
	public IntInterval length() {
		return new IntInterval(new MathNumber(prefix.length()), MathNumber.PLUS_INFINITY);
	}

	/**
	 * Yields the {@link IntInterval} containing the minimum and maximum index
	 * of {@code s} in {@code this}.
	 *
	 * @param s the string to be searched
	 * 
	 * @return the minimum and maximum index of {@code s} in {@code this}
	 */
	public IntInterval indexOf(Prefix s) {
		return new IntInterval(MathNumber.MINUS_ONE, MathNumber.PLUS_INFINITY);
	}

	@Override
	public Satisfiability containsChar(char c) {
		if (isTop())
			return Satisfiability.UNKNOWN;
		if (isBottom())
			return Satisfiability.BOTTOM;
		return this.prefix.contains(String.valueOf(c)) ? Satisfiability.SATISFIED : Satisfiability.UNKNOWN;
	}
}
