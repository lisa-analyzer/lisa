package it.unive.lisa.symbolic.types;

import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;

/**
 * An internal implementation of the {@link it.unive.lisa.type.StringType}
 * interface that can be used by domains that need a concrete instance of that
 * interface.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class StringType implements it.unive.lisa.type.StringType {

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
		return other instanceof it.unive.lisa.type.StringType;
	}

	@Override
	public int hashCode() {
		return it.unive.lisa.type.StringType.class.getName().hashCode();
	}
}
