package it.unive.lisa.lattices.types;

import it.unive.lisa.analysis.BaseLattice;
import it.unive.lisa.analysis.Lattice;
import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.nonrelational.type.TypeValue;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;
import it.unive.lisa.type.Untyped;
import it.unive.lisa.util.representation.StringRepresentation;
import it.unive.lisa.util.representation.StructuredRepresentation;
import java.util.Collections;
import java.util.Set;
import org.apache.commons.collections4.SetUtils;

/**
 * A lattice structure tracking the common supertype of a set of {@link Type}s.
 * The set of types represented by values of this class correspond to all
 * possible instances of the common supertype.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class Supertype implements TypeValue<Supertype>, BaseLattice<Supertype> {

	/**
	 * The bottom element of this lattice, representing an invalid type.
	 */
	public static final Supertype BOTTOM = new Supertype(null, null);

	/**
	 * The type represented by this instance. If this is the bottom element,
	 * this is {@code null}.
	 */
	public final Type type;

	private final TypeSystem types;

	/**
	 * Builds the inferred types. The object built through this constructor
	 * represents an empty set of types.
	 */
	public Supertype() {
		this(null, Untyped.INSTANCE);
	}

	/**
	 * Builds the inferred types, representing only the given {@link Type}.
	 * 
	 * @param types the type system knowing about the types of the program where
	 *                  this element is created
	 * @param type  the type to be included in the set of inferred types
	 */
	public Supertype(
			TypeSystem types,
			Type type) {
		this.type = type;
		this.types = types;
	}

	@Override
	public Set<Type> getRuntimeTypes() {
		if (this.isBottom())
			Collections.emptySet();
		return type.allInstances(types);
	}

	@Override
	public Supertype top() {
		return new Supertype(types, Untyped.INSTANCE);
	}

	@Override
	public boolean isTop() {
		return type == Untyped.INSTANCE;
	}

	@Override
	public Supertype bottom() {
		return BOTTOM;
	}

	@Override
	public StructuredRepresentation representation() {
		if (isTop())
			return Lattice.topRepresentation();

		if (isBottom())
			return Lattice.bottomRepresentation();

		return new StringRepresentation(type.toString());
	}

	@Override
	public Supertype lubAux(
			Supertype other)
			throws SemanticException {
		return new Supertype(types, type.commonSupertype(other.type));
	}

	@Override
	public boolean lessOrEqualAux(
			Supertype other)
			throws SemanticException {
		return type.canBeAssignedTo(other.type);
	}

	@Override
	public Supertype glbAux(
			Supertype other)
			throws SemanticException {
		Type sup = Type
			.commonSupertype(SetUtils.intersection(type.allInstances(types), other.type.allInstances(types)), null);
		if (sup == null)
			return BOTTOM;
		if (sup == Untyped.INSTANCE)
			return top();
		return new Supertype(types, sup);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		return result;
	}

	@Override
	public boolean equals(
			Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Supertype other = (Supertype) obj;
		if (type == null) {
			if (other.type != null)
				return false;
		} else if (!type.equals(other.type))
			return false;
		return true;
	}

}
