package it.unive.lisa.analysis.types;

import it.unive.lisa.type.NumericType;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;
import java.util.Collection;
import java.util.Collections;

public class FloatType implements NumericType {

	public static final FloatType INSTANCE = new FloatType();

	private FloatType() {
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

		return this;
	}

	@Override
	public String toString() {
		return "float";
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof NumericType && ((NumericType) other).is32Bits() && !((NumericType) other).isIntegral()
				&& !((NumericType) other).isUnsigned();
	}

	@Override
	public int hashCode() {
		return FloatType.class.getName().hashCode();
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
	public Collection<Type> allInstances() {
		return Collections.singleton(this);
	}
}
