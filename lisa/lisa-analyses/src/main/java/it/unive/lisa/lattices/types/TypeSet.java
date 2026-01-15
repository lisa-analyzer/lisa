package it.unive.lisa.lattices.types;

import it.unive.lisa.analysis.nonrelational.type.TypeValue;
import it.unive.lisa.lattices.SetLattice;
import it.unive.lisa.symbolic.SymbolicExpression;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;
import java.util.Collections;
import java.util.Set;

/**
 * A set of {@link Type}s, representing the inferred runtime types of an
 * {@link SymbolicExpression}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class TypeSet
		extends
		SetLattice<TypeSet, Type>
		implements
		TypeValue<TypeSet> {

	/**
	 * The top element of the type lattice, representing all possible types.
	 */
	public static final TypeSet TOP = new TypeSet(true, Collections.emptySet());

	/**
	 * The bottom element of the type lattice, representing no types at all.
	 */
	public static final TypeSet BOTTOM = new TypeSet(false, Collections.emptySet());

	/**
	 * Builds the inferred types. The object built through this constructor
	 * represents an empty set of types.
	 */
	public TypeSet() {
		this(true, Collections.emptySet());
	}

	/**
	 * Builds the inferred types, representing only the given {@link Type}.
	 * 
	 * @param typeSystem the type system knowing about the types of the program
	 *                       where this element is created
	 * @param type       the type to be included in the set of inferred types
	 */
	public TypeSet(
			TypeSystem typeSystem,
			Type type) {
		this(typeSystem, Collections.singleton(type));
	}

	/**
	 * Builds the inferred types, representing only the given set of
	 * {@link Type}s.
	 * 
	 * @param typeSystem the type system knowing about the types of the program
	 *                       where this element is created
	 * @param types      the types to be included in the set of inferred types
	 */
	public TypeSet(
			TypeSystem typeSystem,
			Set<Type> types) {
		this(true, typeSystem != null && types.equals(typeSystem.getTypes()) ? Collections.emptySet() : types);
	}

	/**
	 * Builds the inferred types, representing only the given set of
	 * {@link Type}s.
	 * 
	 * @param isTop whether or not the set of types represents all possible
	 *                  types
	 * @param types the types to be included in the set of inferred types
	 */
	public TypeSet(
			boolean isTop,
			Set<Type> types) {
		super(types, isTop);
	}

	@Override
	public Set<Type> getRuntimeTypes() {
		if (elements == null)
			Collections.emptySet();
		return elements;
	}

	@Override
	public TypeSet top() {
		return TOP;
	}

	@Override
	public boolean isTop() {
		return this == TOP || super.isTop();
	}

	@Override
	public TypeSet bottom() {
		return BOTTOM;
	}

	@Override
	public boolean isBottom() {
		return this == BOTTOM || super.isBottom();
	}

	@Override
	public TypeSet mk(
			Set<Type> set) {
		return new TypeSet(true, set);
	}

}
