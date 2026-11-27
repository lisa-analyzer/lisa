package it.unive.lisa.triage.model;

import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.symbolic.value.BinaryExpression;
import it.unive.lisa.symbolic.value.Identifier;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonEq;
import it.unive.lisa.type.Untyped;

/**
 * A {@link FreshValue} naming the value of an identifier, whose tree is too
 * large to be handled as an expression, while its shared sub-expressions are
 * few (e.g., the contents of a field read and written many times within one
 * transition, that occur twice in each write). The value is defined by a
 * conjunct of the path condition of the case using it (see
 * {@link #definition()}), so that it is neither an input of the program nor a
 * source of imprecision. Values are named by their definitions: the same
 * definition always yields the same value (see
 * {@link FreshValueFactory#shared(Identifier, SymbolicExpression)}), so that
 * cases computing the same value still have equal values.
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class SharedValue
		extends
		FreshValue {

	private final Identifier named;

	private final SymbolicExpression value;

	/**
	 * Builds the value.
	 *
	 * @param named    the identifier whose value is named, deciding how it is
	 *                     encoded
	 * @param value    the named value
	 * @param location the location where the value is produced
	 * @param index    a number that uniquely identifies the value among the
	 *                     fresh values of a model
	 */
	public SharedValue(
			Identifier named,
			SymbolicExpression value,
			CodeLocation location,
			long index) {
		super(named.getStaticType(), "$shared#" + index, location);
		this.named = named;
		this.value = value;
	}

	/**
	 * Yields the identifier whose value is named by this one: the value is
	 * encoded as the ones of the identifier.
	 *
	 * @return the identifier
	 */
	public Identifier getNamed() {
		return named;
	}

	/**
	 * Yields the value named by this one.
	 *
	 * @return the value
	 */
	public SymbolicExpression getValue() {
		return value;
	}

	/**
	 * Yields the definition of this value, that is, the condition stating that
	 * it is equal to the value it names.
	 *
	 * @return the definition
	 */
	public BinaryExpression definition() {
		return new BinaryExpression(Untyped.INSTANCE, this, value, ComparisonEq.INSTANCE, getCodeLocation());
	}

	/**
	 * Yields {@code true} if the given condition is the definition of a
	 * {@link SharedValue} (see {@link #definition()}).
	 *
	 * @param condition the condition
	 *
	 * @return whether or not the condition is a definition
	 */
	public static boolean isDefinition(
			SymbolicExpression condition) {
		if (!(condition instanceof BinaryExpression))
			return false;
		BinaryExpression b = (BinaryExpression) condition;
		return b.getOperator() == ComparisonEq.INSTANCE && b.getLeft() instanceof SharedValue
				&& (b.getRight() == ((SharedValue) b.getLeft()).value
						|| b.getRight().equals(((SharedValue) b.getLeft()).value));
	}
}
