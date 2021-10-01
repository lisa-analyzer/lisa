package it.unive.lisa.type.common;

import it.unive.lisa.type.NumericType;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;
import java.util.Collection;
import java.util.Collections;

/**
 * A signed 32-bit floating point {@link NumericType}. The only singleton
 * instance of this class can be retrieved trough field {@link #INSTANCE}.<br>
 * <br>
 * Instances of this class are equal to all other classes that implement the
 * {@link NumericType} interface, and for which {@link #isIntegral()} yields
 * {@code false} and {@link #is32Bits()} yields {@code true}. An instance of
 * Float32 is assumed to be assignable to any {@link NumericType}, with possible
 * loss of information. <br>
 * <br>
 * The common supertype between an Float32 instance {@code t1} and another type
 * instance {@code t2} is {@link Untyped} if {@code t2} is not a
 * {@link NumericType}. Otherwise, the supertype is chosen according to
 * {@link NumericType#supertype(NumericType, NumericType)}. <br>
 * <br>
 * Equality with other types is determined through
 * {@link NumericType#sameNumericTypes(NumericType, NumericType)}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public final class Float32 implements NumericType {

	/**
	 * The unique singleton instance of this type.
	 */
	public static final Float32 INSTANCE = new Float32();

	private Float32() {
	}

	@Override
	public boolean is8Bits() {
		return false;
	}

	@Override
	public boolean is16Bits() {
		return false;
	}

	@Override
	public boolean is32Bits() {
		return true;
	}

	@Override
	public boolean is64Bits() {
		return false;
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
	public boolean canBeAssignedTo(Type other) {
		return other.isNumericType() || other.isUntyped();
	}

	@Override
	public Type commonSupertype(Type other) {
		if (!other.isNumericType())
			return Untyped.INSTANCE;

		return NumericType.supertype(this, other.asNumericType());
	}

	@Override
	public String toString() {
		return "float32";
	}

	@Override
	public boolean equals(Object other) {
		if (!(other instanceof NumericType))
			return false;

		return NumericType.sameNumericTypes(this, (NumericType) other);
	}

	@Override
	public int hashCode() {
		return Float32.class.getName().hashCode();
	}

	@Override
	public Collection<Type> allInstances() {
		return Collections.singleton(this);
	}
}
