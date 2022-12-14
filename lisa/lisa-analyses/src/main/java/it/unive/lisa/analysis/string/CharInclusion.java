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
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;

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
public class CharInclusion implements BaseNonRelationalValueDomain<CharInclusion> {

	private final Set<Character> certainlyContained;

	private final Set<Character> maybeContained;

	private static final CharInclusion TOP = new CharInclusion();
	private static final CharInclusion BOTTOM = new CharInclusion(null, null);

	/**
	 * Builds the top char inclusion abstract element.
	 */
	public CharInclusion() {
		this(new TreeSet<>(), getAlphabet());
	}

	/**
	 * Builds a char inclusion abstract element.
	 *
	 * @param certainlyContained the set of certainly contained characters
	 * @param maybeContained     the set of maybe contained characters
	 */
	public CharInclusion(Set<Character> certainlyContained,
			Set<Character> maybeContained) {
		this.certainlyContained = certainlyContained;
		this.maybeContained = maybeContained;
	}

	@Override
	public CharInclusion lubAux(CharInclusion other) throws SemanticException {
		Set<Character> lubAuxCertainly = new TreeSet<>();

		Set<Character> lubAuxMaybe = new TreeSet<>(this.maybeContained);
		lubAuxMaybe.addAll(other.maybeContained);

		for (Character certainlyContainedChar : this.certainlyContained)
			if (other.certainlyContained.contains(certainlyContainedChar))
				lubAuxCertainly.add(certainlyContainedChar);

		return new CharInclusion(lubAuxCertainly, lubAuxMaybe);
	}

	@Override
	public boolean lessOrEqualAux(CharInclusion other) throws SemanticException {
		if (this.certainlyContained.size() > other.certainlyContained.size() ||
				other.maybeContained.size() > this.maybeContained.size())
			return false;

		if (!other.certainlyContained.containsAll(this.certainlyContained))
			return false;

		return this.maybeContained.containsAll(other.maybeContained);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		CharInclusion that = (CharInclusion) o;
		return Objects.equals(certainlyContained, that.certainlyContained)
				&& Objects.equals(maybeContained, that.maybeContained);
	}

	@Override
	public int hashCode() {
		return Objects.hash(certainlyContained, maybeContained);
	}

	@Override
	public CharInclusion top() {
		return TOP;
	}

	@Override
	public CharInclusion bottom() {
		return BOTTOM;
	}

	@Override
	public DomainRepresentation representation() {
		if (isBottom())
			return Lattice.bottomRepresentation();
		if (isTop())
			return Lattice.topRepresentation();

		return new StringRepresentation(formatRepresentation());
	}

	/**
	 * Yields the set of certainly contained characters of this abstract value.
	 *
	 * @return the set of certainly contained characters of this abstract value.
	 */
	public Set<Character> getCertainlyContained() {
		return this.certainlyContained;
	}

	/**
	 * Yields the set of maybe contained characters of this abstract value.
	 *
	 * @return the set of maybe contained characters of this abstract value.
	 */
	public Set<Character> getMaybeContained() {
		return this.maybeContained;
	}

	private String formatRepresentation() {
		return "CertainlyContained: {" + StringUtils.join(this.certainlyContained, ", ") +
				"}, MaybeContained: {" +
				StringUtils.join(this.maybeContained, ", ") +
				"}";
	}

	@Override
	public CharInclusion evalNonNullConstant(Constant constant, ProgramPoint pp) {
		if (constant.getValue() instanceof String) {
			Set<Character> charsSet = ((String) constant.getValue()).chars()
					.mapToObj(e -> (char) e).collect(Collectors.toCollection(TreeSet::new));

			return new CharInclusion(charsSet, charsSet);
		}

		return TOP;
	}

	@Override
	public CharInclusion evalBinaryExpression(BinaryOperator operator, CharInclusion left, CharInclusion right,
			ProgramPoint pp) {
		if (operator == StringConcat.INSTANCE) {
			Set<Character> resultCertainlyContained = new TreeSet<>();
			Set<Character> resultMaybeContained = new TreeSet<>();

			resultCertainlyContained.addAll(left.certainlyContained);
			resultCertainlyContained.addAll(right.certainlyContained);

			resultMaybeContained.addAll(left.maybeContained);
			resultMaybeContained.addAll(right.maybeContained);

			return new CharInclusion(resultCertainlyContained, resultMaybeContained);
		}

		else if (operator == StringContains.INSTANCE ||
				operator == StringEndsWith.INSTANCE ||
				operator == StringEquals.INSTANCE ||
				operator == StringIndexOf.INSTANCE ||
				operator == StringStartsWith.INSTANCE) {
			return TOP;
		}

		return TOP;
	}

	@Override
	public Satisfiability satisfiesBinaryExpression(BinaryOperator operator, CharInclusion left, CharInclusion right,
			ProgramPoint pp) {
		if (left.isTop() || right.isBottom())
			return Satisfiability.UNKNOWN;

		if (operator == StringContains.INSTANCE)
			if (right.isEmptyString())
				return Satisfiability.SATISFIED;

		return Satisfiability.UNKNOWN;
	}

	/**
	 * Checks whether this char inclusion abstract value models the empty
	 * string, i.e., the sets of the maybe and certainly contained are both
	 * empty.
	 *
	 * @return whether this char inclusion abstract value models the empty
	 *             string
	 */
	private boolean isEmptyString() {
		return maybeContained.isEmpty() && certainlyContained.isEmpty();
	}

	private static Set<Character> getAlphabet() {
		Set<Character> alphabet = new TreeSet<>();

		for (char character = 'a'; character <= 'z'; character++) {
			alphabet.add(character);
		}

		return alphabet;
	}
}
