package it.unive.lisa.program.type;

import it.unive.lisa.type.NumericType;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;
import it.unive.lisa.type.Untyped;
import java.util.Collections;
import java.util.Set;

/**
 * A signed 32-bit floating point {@link NumericType}. The only singleton
 * instance of this class can be retrieved trough field {@link #INSTANCE}.<br>
 * <br>
 * Instances of this class are equal to all other classes that implement the
 * {@link NumericType} interface, and for which {@link #isIntegral()} yields
 * {@code false} and {@link #getNbits()} yields {@code 32}. An instance of
 * Float32 is assumed to be assignable to any {@link NumericType}, with possible
 * loss of information. <br>
 * <br>
 * The common supertype between an Float32 instance {@code t1} and another type
 * instance {@code t2} is {@link Untyped} if {@code t2} is not a
 * {@link NumericType}. Otherwise, the supertype is chosen according to
 * {@link NumericType#supertype(NumericType)}. <br>
 * <br>
 * Equality with other types is determined through
 * {@link NumericType#sameNumericTypes(NumericType)}.
 *
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class Float32Type implements NumericType {

	/**
	 * The unique singleton instance of this type.
	 */
	public static final Float32Type INSTANCE = new Float32Type();

	/**
	 * Builds the type. This constructor is visible to allow subclassing:
	 * instances of this class should be unique, and the singleton can be
	 * retrieved through field {@link #INSTANCE}.
	 */
	protected Float32Type() {
	}

	@Override
	public int getNbits() {
		return 32;
	}

	@Override
	public boolean isUnsigned() {
		return false;
	}

	@Override
	public boolean isIntegral() {
		return false;
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
		return "float32";
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
		return Float32Type.class.getName().hashCode();
	}

	@Override
	public Set<Type> allInstances(
			TypeSystem types) {
		return Collections.singleton(this);
	}
}
