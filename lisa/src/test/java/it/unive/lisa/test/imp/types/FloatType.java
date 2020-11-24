package it.unive.lisa.test.imp.types;

import it.unive.lisa.cfg.type.NumericType;
import it.unive.lisa.cfg.type.Type;
import it.unive.lisa.cfg.type.Untyped;

public class FloatType implements NumericType {

	public static final FloatType INSTANCE = new FloatType();
	
	private FloatType() {}

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
}
