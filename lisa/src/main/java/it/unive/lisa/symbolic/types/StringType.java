package it.unive.lisa.symbolic.types;

import it.unive.lisa.cfg.type.BooleanType;
import it.unive.lisa.cfg.type.Type;
import it.unive.lisa.cfg.type.Untyped;

/**
 * An internal implementation of the {@link it.unive.lisa.cfg.type.StringType}
 * interface that can be used
 * by domains that need a concrete instance of that interface.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class StringType implements BooleanType {

	/**
	 * The singleton instance of this class.
	 */
	public static final StringType INSTANCE = new StringType();

	private StringType() {
	}

	@Override
	public boolean canBeAssignedTo(Type other) {
		return other.isStringType() || other.isUntyped();
	}

	@Override
	public Type commonSupertype(Type other) {
		return other.isStringType() ? this : Untyped.INSTANCE;
	}

	@Override
	public String toString() {
		return "string";
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof it.unive.lisa.cfg.type.StringType;
	}

	@Override
	public int hashCode() {
		return it.unive.lisa.cfg.type.StringType.class.getName().hashCode();
	}
}
