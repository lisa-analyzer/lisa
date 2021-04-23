package it.unive.lisa.symbolic.types;

import it.unive.lisa.type.NumericType;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;
import java.util.Collection;
import java.util.Collections;

/**
 * An internal implementation of the {@link NumericType} interface that can be
 * used by domains that need a concrete instance for integer values.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class IntType implements NumericType {

	/**
	 * The singleton instance of this class.
	 */
	public static final IntType INSTANCE = new IntType();

	private IntType() {
	}

	@Override
	public boolean canBeAssignedTo(Type other) {
		return other.isNumericType() || other.isUntyped();
	}

	@Override
	public Type commonSupertype(Type other) {
		if (!other.isNumericType())
			return Untyped.INSTANCE;

		NumericType o = other.asNumericType();
		if (o.is64Bits())
			return o;

		if (!o.isIntegral())
			return o;

		return this;
	}

	@Override
	public String toString() {
		return "int";
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof NumericType && ((NumericType) other).is32Bits() && ((NumericType) other).isIntegral()
				&& !((NumericType) other).isUnsigned();
	}

	@Override
	public int hashCode() {
		return IntType.class.getName().hashCode();
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
		return true;
	}

	@Override
	public Collection<Type> allInstances() {
		return Collections.singleton(this);
	}
}
