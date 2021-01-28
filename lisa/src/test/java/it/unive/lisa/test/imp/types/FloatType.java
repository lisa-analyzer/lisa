package it.unive.lisa.test.imp.types;

import it.unive.lisa.type.NumericType;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;
import java.util.Collection;
import java.util.Collections;

/**
 * The signed 64-bit floating point {@link it.unive.lisa.type.NumericType} of
 * the IMP language. The only singleton instance of this class can be retrieved
 * trough field {@link #INSTANCE}. Instances of this class are equal to all
 * other classes that implement the {@link it.unive.lisa.type.NumericType}
 * interface and are 64 bits.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class FloatType implements NumericType {

	/**
	 * The unique singleton instance of this type.
	 */
	public static final FloatType INSTANCE = new FloatType();

	private FloatType() {
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
		return false;
	}

	@Override
	public boolean is64Bits() {
		return true;
	}

	@Override
	public boolean isUnsigned() {
		return false;
	}

	@Override
	public boolean canBeAssignedTo(Type other) {
		return other == this || other.isUntyped();
	}

	@Override
	public Type commonSupertype(Type other) {
		return other == this ? this : Untyped.INSTANCE;
	}

	@Override
	public String toString() {
		return "float";
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof NumericType && ((NumericType) other).is64Bits();
	}

	@Override
	public int hashCode() {
		return FloatType.class.getName().hashCode();
	}

	@Override
	public Collection<Type> allInstances() {
		return Collections.singleton(this);
	}
}
