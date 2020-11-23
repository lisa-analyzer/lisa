package it.unive.lisa.test.imp.types;

import java.util.HashMap;
import java.util.Map;

import it.unive.lisa.cfg.type.PointerType;
import it.unive.lisa.cfg.type.Type;
import it.unive.lisa.cfg.type.Untyped;

public class ClassType implements PointerType {

	private static final Map<String, ClassType> types = new HashMap<>();

	public static ClassType lookup(String name, ClassType supertype) {
		return types.computeIfAbsent(name, x -> new ClassType(name, supertype));
	}

	private final String name;

	private final ClassType supertype;

	private ClassType(String name, ClassType supertype) {
		this.name = name;
		this.supertype = supertype;
	}

	@Override
	public final boolean canBeAssignedTo(Type other) {
		return other instanceof ClassType && subclass((ClassType) other);
	}

	private boolean subclass(ClassType other) {
		return this == other || (supertype != null && supertype.subclass(other));
	}

	@Override
	public Type commonSupertype(Type other) {
		if (canBeAssignedTo(other))
			return other;

		if (other.canBeAssignedTo(this))
			return this;

		if (other.isNullType())
			return this;

		if (other.isArrayType())
			return Untyped.INSTANCE;

		return scanForSupertypeOf((ClassType) other);
	}

	private Type scanForSupertypeOf(ClassType other) {
		ClassType current = this;
		while (current != null) {
			if (other.canBeAssignedTo(current))
				return current;
			
			current = current.supertype;
		}

		return Untyped.INSTANCE;
	}
}
