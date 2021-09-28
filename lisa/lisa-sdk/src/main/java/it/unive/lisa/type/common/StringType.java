package it.unive.lisa.type.common;

import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;
import java.util.Collection;
import java.util.Collections;

/**
 * An implementation of the {@link it.unive.lisa.type.StringType}. The only
 * singleton instance of this class can be retrieved trough field
 * {@link #INSTANCE}. <br>
 * <br>
 * Instances of this class are equal to all other classes that implement the
 * {@link it.unive.lisa.type.StringType}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public final class StringType implements it.unive.lisa.type.StringType {

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

	@Override
	public Collection<Type> allInstances() {
		return Collections.singleton(this);
	}
}
