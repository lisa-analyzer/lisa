package it.unive.lisa.analysis.string;

import java.util.Objects;

import it.unive.lisa.analysis.Lattice;
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
public class Suffix implements BaseNonRelationalValueDomain<Suffix> {

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
	public Suffix(String suffix) {
		this.suffix = suffix;
	}

	@Override
	public Suffix lubAux(Suffix other) throws SemanticException {
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
	public boolean lessOrEqualAux(Suffix other) throws SemanticException {
		if (other.suffix.length() <= this.suffix.length()) {
			Suffix lub = this.lubAux(other);

			return lub.suffix.length() == other.suffix.length();
		}

		return false;
	}

	@Override
	public boolean equals(Object o) {
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
	public DomainRepresentation representation() {
		if (isBottom())
			return Lattice.bottomRepresentation();
		if (isTop())
			return Lattice.topRepresentation();

		return new StringRepresentation('*' + suffix);
	}

	@Override
	public Suffix evalNonNullConstant(Constant constant, ProgramPoint pp) {
		if (constant.getValue() instanceof String) {
			String str = (String) constant.getValue();
			if (!str.isEmpty())
				return new Suffix(str);
		}

		return TOP;
	}

	@Override
	public Suffix evalBinaryExpression(BinaryOperator operator, Suffix left, Suffix right, ProgramPoint pp) {
		if (operator == StringConcat.INSTANCE) {
			return right;
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
	public Suffix evalTernaryExpression(TernaryOperator operator, Suffix left, Suffix middle, Suffix right,
			ProgramPoint pp) throws SemanticException {

		if (operator == StringReplace.INSTANCE) {
			String replace = right.getSuffix();
			String string = middle.getSuffix();
			String target = left.getSuffix();

			if (!target.contains(replace))
				return this;

			return new Suffix(target.replace(replace, string));
		}

		return TOP;
	}

	/**
	 * Yields the suffix of this abstract value.
	 *
	 * @return the suffix of this abstract value.
	 */
	public String getSuffix() {
		return this.suffix;
	}

	/**
	 * Yields the suffix corresponding to the substring of this suffix between
	 * two indexes.
	 * 
	 * @param begin where the substring starts
	 * @param end   where the substring ends
	 * 
	 * @return the suffix corresponding to the substring of this suffix between
	 *             two indexes
	 */
	public Suffix substring(long begin, long end) {
		return new Suffix("");
	}

	/**
	 * Yields the {@link IntInterval} containing the minimum and maximum length
	 * of this abstract value.
	 * 
	 * @return the minimum and maximum length of this abstract value
	 */
	public IntInterval length() {
		return new IntInterval(suffix.length(), Integer.MAX_VALUE);
	}

	/**
	 * Yields the {@link IntInterval} containing the minimum and maximum index
	 * of {@code s} in {@code this}.
	 *
	 * @param s the string to be searched
	 * 
	 * @return the minimum and maximum index of {@code s} in {@code this}
	 */
	public IntInterval indexOf(Suffix s) {
		return new IntInterval(-1, Integer.MAX_VALUE);
	}
}