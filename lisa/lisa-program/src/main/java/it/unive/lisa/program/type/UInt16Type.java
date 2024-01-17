package it.unive.lisa.program.type;

import it.unive.lisa.type.NumericType;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;
import it.unive.lisa.type.Untyped;
import java.util.Collections;
import java.util.Set;

/**
 * An unsigned 16-bit integral {@link NumericType}. The only singleton instance
 * of this class can be retrieved trough field {@link #INSTANCE}.<br>
 * <br>
 * Instances of this class are equal to all other classes that implement the
 * {@link NumericType} interface, and for which {@link #isIntegral()} and
 * {@link #getNbits()} yields {@code 16}. An instance of Int16 is assumed to be
 * assignable to any {@link NumericType}, with possible loss of information.
 * <br>
 * <br>
 * The common supertype between an Int16 instance {@code t1} and another type
 * instance {@code t2} is {@link Untyped} if {@code t2} is not a
 * {@link NumericType}. Otherwise, the supertype is chosen according to
 * {@link NumericType#supertype(NumericType)}. <br>
 * <br>
 * Equality with other types is determined through
 * {@link NumericType#sameNumericTypes(NumericType)}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class UInt16Type implements NumericType {

	/**
	 * The unique singleton instance of this type.
	 */
	public static final UInt16Type INSTANCE = new UInt16Type();

	/**
	 * Builds the type. This constructor is visible to allow subclassing:
	 * instances of this class should be unique, and the singleton can be
	 * retrieved through field {@link #INSTANCE}.
	 */
	protected UInt16Type() {
	}

	@Override
	public int getNbits() {
		return 16;
	}

	@Override
	public boolean isUnsigned() {
		return true;
	}

	@Override
	public boolean isIntegral() {
		return true;
	}

	@Override
	public boolean canBeAssignedTo(
			Type other) {
		return other.isNumericType() || other.isUntyped();
	}

	@Override
	public Type commonSupertype(
			Type other) {
		if (!other.isNumericType())
			return Untyped.INSTANCE;

		return supertype(other.asNumericType());
	}

	@Override
	public String toString() {
		return "int16";
	}

	@Override
	public boolean equals(
			Object other) {
		if (!(other instanceof NumericType))
			return false;

		return sameNumericTypes((NumericType) other);
	}

	@Override
	public int hashCode() {
		return UInt16Type.class.getName().hashCode();
	}

	@Override
	public Set<Type> allInstances(
			TypeSystem types) {
		return Collections.singleton(this);
	}
}
