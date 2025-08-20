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
import it.unive.lisa.symbolic.value.operator.binary.StringStartsWith;
import it.unive.lisa.symbolic.value.operator.unary.StringLength;
import it.unive.lisa.type.BooleanType;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;

/**
 * A lattice structure tracking prefixes of strings.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class StrPrefix
		implements
		BaseLattice<StrPrefix>,
		WholeValueElement<StrPrefix> {

	/**
	 * The top element of this lattice, representing the empty prefix.
	 */
	public final static StrPrefix TOP = new StrPrefix();

	/**
	 * The bottom element of this lattice, representing an invalid prefix.
	 */
	public final static StrPrefix BOTTOM = new StrPrefix(null);

	/**
	 * The prefix string of this abstract value. If this is the bottom element,
	 * this is {@code null}.
	 */
	public final String prefix;

	/**
	 * Builds the top prefix abstract element.
	 */
	public StrPrefix() {
		this("");
	}

	/**
	 * Builds a prefix abstract element.
	 * 
	 * @param prefix the prefix
	 */
	public StrPrefix(
			String prefix) {
		this.prefix = prefix;
	}

	@Override
	public StrPrefix lubAux(
			StrPrefix other)
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
			return new StrPrefix(result.toString());

		else
			return TOP;
	}

	@Override
	public boolean lessOrEqualAux(
			StrPrefix other)
			throws SemanticException {
		if (other.prefix.length() <= this.prefix.length()) {
			StrPrefix lub = this.lubAux(other);

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
		StrPrefix prefix1 = (StrPrefix) o;
		return Objects.equals(prefix, prefix1.prefix);
	}

	@Override
	public int hashCode() {
		return Objects.hash(prefix);
	}

	@Override
	public StrPrefix top() {
		return TOP;
	}

	@Override
	public StrPrefix bottom() {
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
						new Constant(pp.getProgram().getTypes().getIntegerType(), prefix.length(), pp.getLocation()),
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
	public StrPrefix generate(
			Set<BinaryExpression> constraints,
			ProgramPoint pp)
			throws SemanticException {
		if (constraints == null)
			return bottom();

		for (BinaryExpression expr : constraints)
			if ((expr.getOperator() instanceof ComparisonEq || expr.getOperator() instanceof StringStartsWith)
					&& expr.getLeft() instanceof Constant
					&& ((Constant) expr.getLeft()).getValue() instanceof String)
				return new StrPrefix(((Constant) expr.getLeft()).getValue().toString());

		return TOP;
	}

}
