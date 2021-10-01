package it.unive.lisa.type.common;

import it.unive.lisa.type.NumericType;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;
import java.util.Collection;
import java.util.Collections;

/**
 * An unsigned 8-bit integral {@link NumericType}. The only singleton instance
 * of this class can be retrieved trough field {@link #INSTANCE}.<br>
 * <br>
 * Instances of this class are equal to all other classes that implement the
 * {@link NumericType} interface, and for which {@link #isIntegral()} and
 * {@link #is8Bits()} yield {@code true}. An instance of Int8 is assumed to be
 * assignable to any {@link NumericType}, with possible loss of information.
 * <br>
 * <br>
 * The common supertype between an Int8 instance {@code t1} and another type
 * instance {@code t2} is {@link Untyped} if {@code t2} is not a
 * {@link NumericType}. Otherwise, the supertype is chosen according to
 * {@link NumericType#supertype(NumericType, NumericType)}. <br>
 * <br>
 * Equality with other types is determined through
 * {@link NumericType#sameNumericTypes(NumericType, NumericType)}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public final class UInt8 implements NumericType {

	/**
	 * The unique singleton instance of this type.
	 */
	public static final UInt8 INSTANCE = new UInt8();

	private UInt8() {
	}

	@Override
	public boolean is8Bits() {
		return true;
	}

	@Override
	public boolean is16Bits() {
		return false;
	}

	@Override
	public boolean is32Bits() {
		return false;
	}

	@Override
	public boolean is64Bits() {
		return false;
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
		return "int8";
	}

	@Override
	public boolean equals(Object other) {
		if (!(other instanceof NumericType))
			return false;

		return NumericType.sameNumericTypes(this, (NumericType) other);
	}

	@Override
	public int hashCode() {
		return UInt8.class.getName().hashCode();
	}

	@Override
	public Collection<Type> allInstances() {
		return Collections.singleton(this);
	}
}
