package it.unive.lisa.type.common;

import it.unive.lisa.type.BooleanType;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;
import it.unive.lisa.type.Untyped;
import java.util.Collections;
import java.util.Set;

/**
 * An internal implementation of the {@link BooleanType}. The only singleton
 * instance of this class can be retrieved trough field {@link #INSTANCE}. <br>
 * <br>
 * Instances of this class are equal to all other classes that implement the
 * {@link BooleanType}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class BoolType implements BooleanType {

	/**
	 * The singleton instance of this class.
	 */
	public static final BoolType INSTANCE = new BoolType();

	private BoolType() {
	}

	@Override
	public boolean canBeAssignedTo(Type other) {
		return other.isBooleanType() || other.isUntyped();
	}

	@Override
	public Type commonSupertype(Type other) {
		return other.isBooleanType() ? this : Untyped.INSTANCE;
	}

	@Override
	public String toString() {
		return "bool";
	}

	@Override
	public final boolean equals(Object other) {
		return other instanceof BooleanType;
	}

	@Override
	public final int hashCode() {
		return BooleanType.class.getName().hashCode();
	}

	@Override
	public Set<Type> allInstances(TypeSystem types) {
		return Collections.singleton(this);
	}
}
