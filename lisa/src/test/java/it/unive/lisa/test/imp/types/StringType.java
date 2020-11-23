package it.unive.lisa.test.imp.types;

import it.unive.lisa.cfg.type.Type;
import it.unive.lisa.cfg.type.Untyped;

public class StringType implements it.unive.lisa.cfg.type.StringType {

	public static StringType INSTANCE = new StringType();
	
	private StringType() {}

	@Override
	public boolean canBeAssignedTo(Type other) {
		return other.isStringType() || other.isUntyped();
	}

	@Override
	public Type commonSupertype(Type other) {
		return other.isStringType() ? this : Untyped.INSTANCE;
	}
}
