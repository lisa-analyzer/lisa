package it.unive.lisa.analysis.combination.smash;

import it.unive.lisa.analysis.BaseLattice;
import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.lattices.Satisfiability;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;

/**
 * A {@link BaseLattice} that represents a smashed value, which is either a
 * {@link Satisfiability} value, a lattice representing a number, or a lattice
 * representing a string. This is used in the smashed-sum abstract domain, which
 * combines a non-relational numeric abstract domain, a non-relational string
 * abstract domain, and a non-relational boolean abstract domain.
 * 
 * @author <a href="mailto:vincenzo.arceri@unipr.it">Vincenzo Arceri</a>
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 * 
 * @param <N> the type of the numeric lattice
 * @param <S> the type of the string lattice
 */
public class SmashedValue<N extends Lattice<N>,
		S extends Lattice<S>>
		implements
		BaseLattice<SmashedValue<N, S>> {

	private final N intValue;

	private final S stringValue;

	private final Satisfiability boolValue;

	/**
	 * Builds an abstract element of this lattice.
	 * 
	 * @param intValue    the abstract value for intergers
	 * @param stringValue the abstract value for strings
	 * @param boolValue   the abstract value for booleans
	 */
	public SmashedValue(
			N intValue,
			S stringValue,
			Satisfiability boolValue) {
		this.intValue = intValue;
		this.stringValue = stringValue;
		this.boolValue = boolValue;
	}

	/**
	 * Yields the integer abstract value (might be bottom if this value does not
	 * abstract a number).
	 * 
	 * @return the integer abstract value
	 */
	public N getIntValue() {
		return intValue;
	}

	/**
	 * Yields the string abstract value (might be bottom if this value does not
	 * abstract a string).
	 * 
	 * @return the string abstract value
	 */
	public S getStringValue() {
		return stringValue;
	}

	/**
	 * Yields the boolean abstract value (might be bottom if this value does not
	 * abstract a boolean).
	 *
	 * @return the boolean abstract value
	 */
	public Satisfiability getBoolValue() {
		return boolValue;
	}

	@Override
	public SmashedValue<N, S> lubAux(
			SmashedValue<N, S> other)
			throws SemanticException {
		return new SmashedValue<N, S>(
				intValue.lub(other.intValue),
				stringValue.lub(other.stringValue),
				boolValue.lub(other.boolValue));
	}

	@Override
	public SmashedValue<N, S> wideningAux(
			SmashedValue<N, S> other)
			throws SemanticException {
		return new SmashedValue<N, S>(
				intValue.widening(other.intValue),
				stringValue.widening(other.stringValue),
				boolValue.widening(other.boolValue));

	}

	@Override
	public boolean lessOrEqualAux(
			SmashedValue<N, S> other)
			throws SemanticException {
		return intValue.lessOrEqual(other.intValue)
				&& stringValue.lessOrEqual(other.stringValue)
				&& boolValue.lessOrEqual(other.boolValue);
	}

	@Override
	public boolean isTop() {
		return intValue.isTop() && stringValue.isTop() && boolValue.isTop();
	}

	@Override
	public SmashedValue<N, S> top() {
		return new SmashedValue<>(intValue.top(), stringValue.top(), boolValue.top());
	}

	@Override
	public boolean isBottom() {
		return intValue.isBottom() && stringValue.isBottom() && boolValue.isBottom();
	}

	@Override
	public SmashedValue<N, S> bottom() {
		return new SmashedValue<>(intValue.bottom(), stringValue.bottom(), boolValue.bottom());
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((intValue == null) ? 0 : intValue.hashCode());
		result = prime * result + ((stringValue == null) ? 0 : stringValue.hashCode());
		result = prime * result + ((boolValue == null) ? 0 : boolValue.hashCode());
		return result;
	}

	@Override
	public boolean equals(
			Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SmashedValue<?, ?> other = (SmashedValue<?, ?>) obj;
		if (intValue == null) {
			if (other.intValue != null)
				return false;
		} else if (!intValue.equals(other.intValue))
			return false;
		if (stringValue == null) {
			if (other.stringValue != null)
				return false;
		} else if (!stringValue.equals(other.stringValue))
			return false;
		if (boolValue != other.boolValue)
			return false;
		return true;
	}

	@Override
	public StructuredRepresentation representation() {
		if (isBottom())
			return Lattice.bottomRepresentation();
		if (isTop())
			return Lattice.topRepresentation();
		if (isString())
			return stringValue.representation();
		if (isNumber())
			return intValue.representation();
		if (isBool())
			return boolValue.representation();
		return new StringRepresentation(
				"(" + intValue.representation().toString() + ", " + stringValue.representation().toString() + ", "
						+ boolValue.representation().toString() + ")");
	}

	@Override
	public String toString() {
		return representation().toString();
	}

	/**
	 * Yields whether this smashed value is of the same kind as the given
	 * {@code other} smashed value, i.e., whether both are either top or bottom,
	 * or both are numbers, or both are strings, or both are booleans.
	 * 
	 * @param other the smashed value to compare with
	 * 
	 * @return {@code true} if this smashed value is of the same kind as the
	 *             given one, {@code false} otherwise
	 */
	public boolean sameKind(
			SmashedValue<N, S> other) {
		return (intValue.isBottom() == other.intValue.isBottom())
				&& (stringValue.isBottom() == other.stringValue.isBottom())
				&& (boolValue.isBottom() == other.boolValue.isBottom());
	}

	/**
	 * Returns {@code true} if this smashed value is a number, i.e., it either
	 * is top or it models a number value.
	 * 
	 * @return {@code true} if this smashed value is a number, {@code false}
	 *             otherwise
	 */
	public boolean isNumber() {
		return isTop() || (!isBottom() && stringValue.isBottom() && boolValue.isBottom());
	}

	/**
	 * Returns {@code true} if this smashed value is a string, i.e., it either
	 * is top or it models a string value.
	 * 
	 * @return {@code true} if this smashed value is a string, {@code false}
	 *             otherwise
	 */
	public boolean isString() {
		return isTop() || (!isBottom() && intValue.isBottom() && boolValue.isBottom());
	}

	/**
	 * Returns {@code true} if this smashed value is a boolean, i.e., it either
	 * is top or it models a boolean value.
	 * 
	 * @return {@code true} if this smashed value is a boolean, {@code false}
	 *             otherwise
	 */
	public boolean isBool() {
		return isTop() || (!isBottom() && stringValue.isBottom() && intValue.isBottom());
	}

}
