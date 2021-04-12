package it.unive.lisa.program;

import it.unive.lisa.program.annotations.Annotations;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;
import java.util.Objects;

/**
 * A global variable, scoped by its container. Instances of this class can refer
 * both to instance variables or static global variables.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class Global implements CodeElement {

	/**
	 * The name of this parameter
	 */
	private final String name;

	/**
	 * The static type of this parameter
	 */
	private final Type staticType;

	private final CodeLocation location;

	private Annotations annotations;

	/**
	 * Builds an untyped parameter reference, identified by its name. The
	 * location where this parameter reference happens is unknown (i.e. no
	 * source file/line/column is available) as well as its type (i.e. it is
	 * {#link Untyped#INSTANCE}).
	 * 
	 * @param name the name of this parameter
	 */
	public Global(String name) {
		this(name, Untyped.INSTANCE);
	}

	/**
	 * Builds a typed parameter reference, identified by its name and its type.
	 * The location where this parameter reference happens is unknown (i.e. no
	 * source file/line/column is available).
	 * 
	 * @param name       the name of this parameter
	 * @param staticType the type of this parameter
	 */
	public Global(String name, Type staticType) {
		this(null, name, staticType);
	}

	/**
	 * Builds the parameter reference, identified by its name and its type,
	 * happening at the given location in the program.
	 * 
	 * @param location   the location where this parameter is defined within the
	 *                       source file. If unknown, use {@code null}
	 * @param name       the name of this parameter
	 * @param staticType the type of this parameter. If unknown, use
	 *                       {@link Untyped#INSTANCE}
	 */
	public Global(CodeLocation location, String name, Type staticType) {
		Objects.requireNonNull(name, "The name of a parameter cannot be null");
		Objects.requireNonNull(staticType, "The type of a parameter cannot be null");
		this.location = location;
		this.name = name;
		this.staticType = staticType;
	}

	/**
	 * Yields the name of this parameter.
	 * 
	 * @return the name of this parameter
	 */
	public String getName() {
		return name;
	}

	/**
	 * Yields the static type of this parameter.
	 * 
	 * @return the static type of this parameter
	 */
	public Type getStaticType() {
		return staticType;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((staticType == null) ? 0 : staticType.hashCode());
		result = prime * result + ((annotations == null) ? 0 : annotations.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Global other = (Global) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (staticType == null) {
			if (other.staticType != null)
				return false;
		} else if (!staticType.equals(other.staticType))
			return false;
		if (annotations == null) {
			if (other.annotations != null)
				return false;
		} else if (!annotations.equals(other.annotations))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return staticType + " " + name;
	}

	@Override
	public CodeLocation getLocation() {
		return location;
	}

	/**
	 * Yields the annotations of this global element.
	 * 
	 * @return the annotations of this global element
	 */
	public Annotations getAnnotations() {
		return annotations;
	}

	/**
	 * Sets the annotations of this global element.
	 * 
	 * @param annotations the annotations to be set
	 */
	public void setAnnotations(Annotations annotations) {
		this.annotations = annotations;
	}
}
