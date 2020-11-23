package it.unive.lisa.cfg.type;

/**
 * The void type.
 * 
 * It implements the singleton design pattern, that is the instances of this
 * type are unique. The unique instance of this type can be retrieved by
 * {@link VoidType#INSTANCE}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class VoidType implements Type {

	/**
	 * Unique instance of {@link VoidType}.
	 */
	public static final VoidType INSTANCE = new VoidType();

	private VoidType() {
	}

	@Override
	public String toString() {
		return "void";
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof VoidType;
	}

	@Override
	public int hashCode() {
		return System.identityHashCode(this);
	}

	@Override
	public boolean canBeAssignedTo(Type other) {
		return false;
	}

	@Override
	public Type commonSupertype(Type other) {
		return other == this ? this : Untyped.INSTANCE;
	}
}
