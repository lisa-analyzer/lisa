package it.unive.lisa.type.common;

import java.util.Collection;
import java.util.Collections;

import it.unive.lisa.type.NumericType;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;

/**
 * A signed 16-bit integral {@link NumericType}. The only singleton instance of
 * this class can be retrieved trough field {@link #INSTANCE}.<br>
 * <br>
 * Instances of this class are equal to all other classes that implement the
 * {@link NumericType} interface, and for which {@link #isIntegral()} and
 * {@link #is16Bits()} yield {@code true}. An instance of Int16 is assumed to be
 * assignable to any {@link NumericType}, with possible loss of information.
 * <br>
 * <br>
 * The common supertype between an Int16 instance {@code t1} and another type
 * instance {@code t2} is {@link Untyped} if {@code t2} is not a
 * {@link NumericType}. Otherwise, the supertype is chosen according to
 * {@link NumericType#supertype(NumericType, NumericType)}. <br>
 * <br>
 * Equality with other types is determined through
 * {@link NumericType#sameNumericTypes(NumericType, NumericType)}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public final class Int16 implements NumericType {

	/**
	 * The unique singleton instance of this type.
	 */
	public static final Int16 INSTANCE = new Int16();

	private Int16() {
	}

	@Override
	public boolean is8Bits() {
		return false;
	}

	@Override
	public boolean is16Bits() {
		return true;
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
		return false;
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
		return "int16";
	}

	@Override
	public boolean equals(Object other) {
		if (!(other instanceof NumericType))
			return false;

		return NumericType.sameNumericTypes(this, (NumericType) other);
	}

	@Override
	public int hashCode() {
		return Int16.class.getName().hashCode();
	}

	@Override
	public Collection<Type> allInstances() {
		return Collections.singleton(this);
	}
}
