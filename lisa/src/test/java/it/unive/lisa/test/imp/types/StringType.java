package it.unive.lisa.test.imp.types;

import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;
import java.util.Collection;
import java.util.Collections;

/**
 * The {@link it.unive.lisa.type.StringType} of the IMP language. The only
 * singleton instance of this class can be retrieved trough field
 * {@link #INSTANCE}. Instances of this class are equal to all other classes
 * that implement the {@link it.unive.lisa.type.StringType} interface.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class StringType implements it.unive.lisa.type.StringType {

	/**
	 * The unique singleton instance of this type.
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
		return other instanceof it.unive.lisa.symbolic.types.StringType;
	}

	@Override
	public int hashCode() {
		return it.unive.lisa.symbolic.types.StringType.class.getName().hashCode();
	}

	@Override
	public Collection<Type> allInstances() {
		return Collections.singleton(this);
	}
}
