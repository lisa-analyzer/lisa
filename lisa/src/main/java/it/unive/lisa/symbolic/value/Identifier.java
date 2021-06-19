package it.unive.lisa.symbolic.value;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.program.annotations.Annotation;
import it.unive.lisa.program.annotations.Annotations;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.type.Type;
import it.unive.lisa.util.collections.externalSet.ExternalSet;

/**
 * An identifier of a program variable, representing either a program variable
 * (as an instance of {@link Variable}), or a resolved memory location (as an
 * instance of {@link HeapLocation}).
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public abstract class Identifier extends ValueExpression {

	/**
	 * The name of the identifier
	 */
	private final String name;

	/**
	 * Whether or not this identifier is weak, meaning that it should only
	 * receive weak assignments
	 */
	private final boolean weak;

	private Annotations annotations;

	/**
	 * Builds the identifier.
	 * 
	 * @param types the runtime types of this expression
	 * @param name  the name of the identifier
	 * @param weak  whether or not this identifier is weak, meaning that it
	 *                  should only receive weak assignments
	 */
	protected Identifier(ExternalSet<Type> types, String name, boolean weak, CodeLocation location) {
		this(types, name, weak, new Annotations(), location);
	}

	/**
	 * Builds the identifier.
	 * 
	 * @param types       the runtime types of this expression
	 * @param name        the name of the identifier
	 * @param weak        whether or not this identifier is weak, meaning that
	 *                        it should only receive weak assignments
	 * @param annotations the annotations of this identifier
	 */
	protected Identifier(ExternalSet<Type> types, String name, boolean weak, Annotations annotations, CodeLocation location) {
		super(types, location);
		this.name = name;
		this.weak = weak;
		this.annotations = annotations;
	}

	/**
	 * Yields the name of this identifier.
	 * 
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Yields whether or not this identifier is weak. Weak identifiers should
	 * only receive weak assignments, that is, the value of the identifier after
	 * the assignment should be the least upper bound between its old value and
	 * the fresh value being assigned. With strong identifiers instead, the new
	 * value corresponds to the freshly provided value.
	 * 
	 * @return {@code true} if this identifier is weak, {@code false} otherwise
	 */
	public boolean isWeak() {
		return weak;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		// we do not call super here since variables should be uniquely
		// identified by their name, regardless of their type
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		// we do not call super here since variables should be uniquely
		// identified by their name, regardless of their type
		if (getClass() != obj.getClass())
			return false;
		Identifier other = (Identifier) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}

	/**
	 * Yields the annotations of this identifier.
	 * 
	 * @return the annotations of this identifier
	 */
	public Annotations getAnnotations() {
		return annotations;
	}

	/**
	 * Adds an annotation to the annotations of this identifier.
	 * 
	 * @param ann the annotation to be added
	 */
	public void addAnnotation(Annotation ann) {
		annotations.addAnnotation(ann);
	}

	/**
	 * Yields the least upper bounds between two identifiers.
	 * 
	 * @param other the other identifier
	 * 
	 * @return the least upper bounds between two identifiers.
	 * 
	 * @throws SemanticException if this and other are not equal.
	 */
	public Identifier lub(Identifier other) throws SemanticException {
		if (!equals(other))
			throw new SemanticException("Cannot perform the least upper bound between different identifiers: '" + this
					+ "' and '" + other + "'");
		return this;
	}
}
