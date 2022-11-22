package it.unive.lisa.program.type;

import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;
import it.unive.lisa.type.Untyped;
import java.util.Collections;
import java.util.Set;

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
public class StringType implements it.unive.lisa.type.StringType {

	/**
	 * The singleton instance of this class.
	 */
	public static final StringType INSTANCE = new StringType();

	/**
	 * Builds the type. This constructor is visible to allow subclassing:
	 * instances of this class should be unique, and the singleton can be
	 * retrieved through field {@link #INSTANCE}.
	 */
	protected StringType() {
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
	public Set<Type> allInstances(TypeSystem types) {
		return Collections.singleton(this);
	}
}
