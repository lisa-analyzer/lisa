package it.unive.lisa.analysis.string;

import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticDomain;
import it.unive.lisa.analysis.SemanticDomain.Satisfiability;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.nonrelational.value.BaseNonRelationalValueDomain;
import it.unive.lisa.analysis.representation.DomainRepresentation;
import it.unive.lisa.analysis.representation.StringRepresentation;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.operator.binary.*;
import it.unive.lisa.symbolic.value.operator.ternary.TernaryOperator;
import it.unive.lisa.symbolic.value.operator.unary.UnaryOperator;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;

public class CharInclusion extends BaseNonRelationalValueDomain<CharInclusion> {

	private final Collection<Character> certainlyContained;

	private final Collection<Character> maybeContained;

	private static final CharInclusion TOP = new CharInclusion();
	private static final CharInclusion BOTTOM = new CharInclusion(null, null);

	public CharInclusion() {
		this(new HashSet<>(), getAlphabet());
	}

	public CharInclusion(Collection<Character> certainlyContained,
			Collection<Character> maybeContained) {
		this.certainlyContained = certainlyContained;
		this.maybeContained = maybeContained;
	}

	@Override
	public CharInclusion lubAux(CharInclusion other) throws SemanticException {
		HashSet<Character> lubAuxCertainly = new HashSet<>();

		HashSet<Character> lubAuxMaybe = new HashSet<>(this.getMaybeContained());
		lubAuxMaybe.addAll(other.getMaybeContained());

		for (Character certainlyContainedChar : this.getCertainlyContained())
			if (other.getCertainlyContained().contains(certainlyContainedChar))
				lubAuxCertainly.add(certainlyContainedChar);

		return new CharInclusion(lubAuxCertainly, lubAuxMaybe);
	}

	@Override
	public CharInclusion wideningAux(CharInclusion other) throws SemanticException {
		return lubAux(other);
	}

	@Override
	public boolean lessOrEqualAux(CharInclusion other) throws SemanticException {
		if (this.getCertainlyContained().size() > other.getCertainlyContained().size() ||
				other.getMaybeContained().size() > this.getMaybeContained().size())
			return false;

		if (!other.getCertainlyContained().containsAll(this.getCertainlyContained()))
			return false;

		return this.getMaybeContained().containsAll(other.getMaybeContained());
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
	public boolean isTop() {
		return this.equals(TOP);
	}

	@Override
	public boolean isBottom() {
		return this.equals(BOTTOM);
	}

	@Override
	public DomainRepresentation representation() {
		if (isBottom())
			return Lattice.bottomRepresentation();
		if (isTop())
			return Lattice.topRepresentation();

		return new StringRepresentation(formatRepresentation());
	}

	public Collection<Character> getCertainlyContained() {
		return this.certainlyContained;
	}

	public Collection<Character> getMaybeContained() {
		return this.maybeContained;
	}

	private String formatRepresentation() {
		StringBuilder stringBuilder = new StringBuilder("CertainlyContained: {");
		int counter = 0;

		for (Character certainlyContainedChar : this.getCertainlyContained()) {
			String formattedCharacter;

			formattedCharacter = counter != this.getCertainlyContained().size() - 1 ? certainlyContainedChar + ", "
					: certainlyContainedChar + "}";
			counter++;

			stringBuilder.append(formattedCharacter);
		}

		counter = 0;
		stringBuilder.append(", MaybeContained: {");

		for (Character maybeContainedChar : this.getMaybeContained()) {
			String formattedCharacter;

			formattedCharacter = counter != this.getMaybeContained().size() - 1 ? maybeContainedChar + ", "
					: maybeContainedChar + "}";
			counter++;

			stringBuilder.append(formattedCharacter);
		}

		return stringBuilder.toString();
	}

	@Override
	public CharInclusion evalNullConstant(ProgramPoint pp) {
		return TOP;
	}

	@Override
	public CharInclusion evalNonNullConstant(Constant constant, ProgramPoint pp) {
		return TOP;
	}

	@Override
	public CharInclusion evalUnaryExpression(UnaryOperator operator, CharInclusion arg, ProgramPoint pp) {
		return TOP;
	}

	@Override
	public CharInclusion evalBinaryExpression(BinaryOperator operator, CharInclusion left, CharInclusion right,
			ProgramPoint pp) {
		if (operator == StringConcat.INSTANCE) {
			HashSet<Character> resultCertainlyContained = new HashSet<>();
			HashSet<Character> resultMaybeContained = new HashSet<>();

			resultCertainlyContained.addAll(left.getCertainlyContained());
			resultCertainlyContained.addAll(right.getCertainlyContained());

			resultMaybeContained.addAll(left.getMaybeContained());
			resultMaybeContained.addAll(right.getMaybeContained());

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
	public Satisfiability satisfiesNonNullConstant(Constant constant, ProgramPoint pp) {
		return Satisfiability.UNKNOWN;
	}

	@Override
	public SemanticDomain.Satisfiability satisfiesNullConstant(ProgramPoint pp) {
		return Satisfiability.UNKNOWN;
	}

	@Override
	public Satisfiability satisfiesBinaryExpression(BinaryOperator operator, CharInclusion left, CharInclusion right,
			ProgramPoint pp) {
		if (left.isTop() || right.isBottom())
			return Satisfiability.UNKNOWN;

		if (operator == StringContains.INSTANCE && left.getCertainlyContained().isEmpty()
				&& left.getMaybeContained().isEmpty())
			return Satisfiability.SATISFIED;

		return Satisfiability.UNKNOWN;
	}

	@Override
	public Satisfiability satisfiesTernaryExpression(TernaryOperator operator, CharInclusion left, CharInclusion middle,
			CharInclusion right, ProgramPoint pp) {
		return Satisfiability.UNKNOWN;
	}

	private static Collection<Character> getAlphabet() {
		HashSet<Character> alphabet = new HashSet<>();

		for (char character = 'a'; character <= 'z'; character++) {
			alphabet.add(character);
		}

		return alphabet;
	}
}
