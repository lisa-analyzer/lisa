package it.unive.lisa.program.cfg.statement.call;

import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;

/**
 * A {@link Type} whose {@link #canBeAssignedTo(Type)} and
 * {@link #commonSupertype(Type)} are fully controlled by the test, used to
 * exercise the "running join" logic of {@code *Call#getCommonReturnType}
 * without depending on any real type hierarchy.
 */
class FakeType
		implements
		Type {

	private final String name;
	private final BiPredicate<FakeType, Type> assignable;
	private final BiFunction<FakeType, Type, Type> supertype;

	FakeType(
			String name,
			BiPredicate<FakeType, Type> assignable,
			BiFunction<FakeType, Type, Type> supertype) {
		this.name = name;
		this.assignable = assignable;
		this.supertype = supertype;
	}

	@Override
	public boolean canBeAssignedTo(
			Type other) {
		return assignable.test(this, other);
	}

	@Override
	public Type commonSupertype(
			Type other) {
		return supertype.apply(this, other);
	}

	@Override
	public Set<Type> allInstances(
			TypeSystem types) {
		return Set.of(this);
	}

	@Override
	public String toString() {
		return name;
	}

}
