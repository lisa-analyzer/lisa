package it.unive.lisa.analysis.string;

import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.SemanticOracle;
import it.unive.lisa.analysis.lattices.Satisfiability;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.operator.binary.BinaryOperator;
import it.unive.lisa.symbolic.value.operator.binary.StringConcat;
import it.unive.lisa.symbolic.value.operator.binary.StringContains;
import it.unive.lisa.symbolic.value.operator.ternary.StringReplace;
import it.unive.lisa.symbolic.value.operator.ternary.TernaryOperator;
import it.unive.lisa.util.numeric.IntInterval;
import it.unive.lisa.util.numeric.MathNumber;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;
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
public class CharInclusion implements SmashedSumStringDomain<CharInclusion> {

	private final Set<Character> certainlyContained;

	private final Set<Character> maybeContained;

	private static final CharInclusion TOP = new CharInclusion();
	private static final CharInclusion BOTTOM = new CharInclusion(null, null);

	/**
	 * Builds the top char inclusion abstract element.
	 */
	public CharInclusion() {
		this(new TreeSet<>(), null);
	}

	/**
	 * Builds a char inclusion abstract element.
	 *
	 * @param certainlyContained the set of certainly contained characters
	 * @param maybeContained     the set of maybe contained characters
	 */
	public CharInclusion(
			Set<Character> certainlyContained,
			Set<Character> maybeContained) {
		this.certainlyContained = certainlyContained;
		this.maybeContained = maybeContained;
	}

	@Override
	public CharInclusion lubAux(
			CharInclusion other)
			throws SemanticException {
		Set<Character> lubAuxCertainly = new TreeSet<>();

		Set<Character> lubAuxMaybe;
		if (maybeContained == null || other.maybeContained == null)
			lubAuxMaybe = null;
		else {
			lubAuxMaybe = new TreeSet<>();
			lubAuxMaybe.addAll(maybeContained);
			lubAuxMaybe.addAll(other.maybeContained);
		}

		for (Character certainlyContainedChar : this.certainlyContained)
			if (other.certainlyContained.contains(certainlyContainedChar))
				lubAuxCertainly.add(certainlyContainedChar);

		return new CharInclusion(lubAuxCertainly, lubAuxMaybe);
	}

	@Override
	public boolean lessOrEqualAux(
			CharInclusion other)
			throws SemanticException {
		if (this.certainlyContained.size() > other.certainlyContained.size())
			return false;
		if (!other.certainlyContained.containsAll(certainlyContained))
			return false;
		if (other.maybeContained == null)
			return true;
		if (maybeContained == null)
			return false;
		if (this.maybeContained.size() > other.maybeContained.size())
			return false;
		return other.maybeContained.containsAll(this.maybeContained);
	}

	@Override
	public boolean equals(
			Object o) {
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
	public StructuredRepresentation representation() {
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
	 * @return the set of maybe contained characters of this abstract value, or
	 *             {@code null} if the whole alphabet might be part of the
	 *             string
	 */
	public Set<Character> getMaybeContained() {
		return this.maybeContained;
	}

	private String formatRepresentation() {
		return "CertainlyContained: {" + StringUtils.join(this.certainlyContained, ", ") +
				"}, MaybeContained: {" + (maybeContained == null ? "Σ" : StringUtils.join(this.maybeContained, ", ")) +
				"}";
	}

	@Override
	public CharInclusion evalNonNullConstant(
			Constant constant,
			ProgramPoint pp,
			SemanticOracle oracle) {
		if (constant.getValue() instanceof String) {
			Set<Character> charsSet = ((String) constant.getValue()).chars()
					.mapToObj(e -> (char) e).collect(Collectors.toCollection(TreeSet::new));

			return new CharInclusion(charsSet, charsSet);
		}

		return TOP;
	}

	@Override
	public CharInclusion evalBinaryExpression(
			BinaryOperator operator,
			CharInclusion left,
			CharInclusion right,
			ProgramPoint pp,
			SemanticOracle oracle) {
		if (operator == StringConcat.INSTANCE) {
			Set<Character> resultCertainlyContained = new TreeSet<>();
			resultCertainlyContained.addAll(left.certainlyContained);
			resultCertainlyContained.addAll(right.certainlyContained);

			Set<Character> resultMaybeContained;
			if (left.maybeContained == null || right.maybeContained == null)
				resultMaybeContained = null;
			else {
				resultMaybeContained = new TreeSet<>();
				resultMaybeContained.addAll(left.maybeContained);
				resultMaybeContained.addAll(right.maybeContained);
			}

			return new CharInclusion(resultCertainlyContained, resultMaybeContained);
		}

		return TOP;
	}

	@Override
	public CharInclusion evalTernaryExpression(
			TernaryOperator operator,
			CharInclusion left,
			CharInclusion middle,
			CharInclusion right,
			ProgramPoint pp,
			SemanticOracle oracle)
			throws SemanticException {
		if (operator == StringReplace.INSTANCE) {

			if (!left.certainlyContained.containsAll(middle.certainlyContained))
				// no replace for sure
				return this;

			Set<Character> included = new TreeSet<>(left.certainlyContained);
			Set<Character> possibly = new TreeSet<>(left.maybeContained);
			// since we do not know if the replace will happen, we move
			// everything to the
			// possibly included characters
			included.removeAll(middle.certainlyContained);
			possibly.addAll(middle.certainlyContained);

			included.removeAll(middle.maybeContained);
			Set<Character> tmp = new TreeSet<>(middle.maybeContained);
			tmp.retainAll(left.certainlyContained); // just the ones that
													// we removed before
			possibly.addAll(tmp);

			// add the second string
			possibly.addAll(right.certainlyContained);
			possibly.addAll(right.maybeContained);

			return new CharInclusion(included, possibly);
		}

		return TOP;
	}

	@Override
	public Satisfiability satisfiesBinaryExpression(
			BinaryOperator operator,
			CharInclusion left,
			CharInclusion right,
			ProgramPoint pp,
			SemanticOracle oracle) {
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
		return (maybeContained != null && maybeContained.isEmpty()) && certainlyContained.isEmpty();
	}

	@Override
	public CharInclusion substring(
			long begin,
			long end) {
		if (isTop() || isBottom())
			return this;
		return new CharInclusion(new TreeSet<>(), maybeContained);
	}

	@Override
	public IntInterval length() {
		return new IntInterval(new MathNumber(certainlyContained.size()), MathNumber.PLUS_INFINITY);
	}

	@Override
	public IntInterval indexOf(
			CharInclusion s) {
		return new IntInterval(MathNumber.MINUS_ONE, MathNumber.PLUS_INFINITY);
	}

	@Override
	public Satisfiability containsChar(
			char c) {
		if (isTop())
			return Satisfiability.UNKNOWN;
		if (isBottom())
			return Satisfiability.BOTTOM;
		if (this.certainlyContained.contains(c))
			return Satisfiability.SATISFIED;
		else if (this.maybeContained == null || this.maybeContained.contains(c))
			return Satisfiability.UNKNOWN;
		else
			return Satisfiability.NOT_SATISFIED;
	}
}
