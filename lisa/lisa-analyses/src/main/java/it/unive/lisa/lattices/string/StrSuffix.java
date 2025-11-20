package it.unive.lisa.lattices.string;

import it.unive.lisa.analysis.BaseLattice;
import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.combination.constraints.WholeValueElement;
import it.unive.lisa.program.cfg.ProgramPoint;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.symbolic.value.UnaryExpression;
import it.unive.lisa.symbolic.value.ValueExpression;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonEq;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLe;
import it.unive.lisa.symbolic.value.operator.binary.StringEndsWith;
import it.unive.lisa.symbolic.value.operator.unary.StringLength;
import it.unive.lisa.type.BooleanType;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;

/**
 * A lattice structure tracking suffixes of strings.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class StrSuffix
		implements
		BaseLattice<StrSuffix>,
		WholeValueElement<StrSuffix> {

	/**
	 * The top element of the lattice, representing an empty suffix.
	 */
	public final static StrSuffix TOP = new StrSuffix();

	/**
	 * The bottom element of the lattice, representing an invalid suffix.
	 */
	public final static StrSuffix BOTTOM = new StrSuffix(null);

	/**
	 * The suffix string represented by this element. If this is {@code null},
	 * then this element represents the bottom element of the lattice.
	 */
	public final String suffix;

	/**
	 * Builds the top suffix abstract element.
	 */
	public StrSuffix() {
		this("");
	}

	/**
	 * Builds a suffix abstract element.
	 *
	 * @param suffix the suffix
	 */
	public StrSuffix(
			String suffix) {
		this.suffix = suffix;
	}

	@Override
	public StrSuffix lubAux(
			StrSuffix other)
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
			return new StrSuffix(result.reverse().toString());

		else
			return TOP;
	}

	@Override
	public boolean lessOrEqualAux(
			StrSuffix other)
			throws SemanticException {
		if (other.suffix.length() <= this.suffix.length()) {
			StrSuffix lub = this.lubAux(other);
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
		StrSuffix suffix1 = (StrSuffix) o;
		return Objects.equals(suffix, suffix1.suffix);
	}

	@Override
	public int hashCode() {
		return Objects.hash(suffix);
	}

	@Override
	public StrSuffix top() {
		return TOP;
	}

	@Override
	public StrSuffix bottom() {
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
			return Collections.singleton(
					new BinaryExpression(
							booleanType,
							new Constant(pp.getProgram().getTypes().getIntegerType(), 0, pp.getLocation()),
							strlen,
							ComparisonLe.INSTANCE,
							e.getCodeLocation()));

		return Set.of(
				new BinaryExpression(
						booleanType,
						new Constant(pp.getProgram().getTypes().getIntegerType(), suffix.length(), pp.getLocation()),
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
	public StrSuffix generate(
			Set<BinaryExpression> constraints,
			ProgramPoint pp)
			throws SemanticException {
		if (constraints == null)
			return bottom();

		for (BinaryExpression expr : constraints)
			if ((expr.getOperator() instanceof ComparisonEq || expr.getOperator() instanceof StringEndsWith)
					&& expr.getLeft() instanceof Constant
					&& ((Constant) expr.getLeft()).getValue() instanceof String)
				return new StrSuffix(((Constant) expr.getLeft()).getValue().toString());

		return TOP;
	}

}
