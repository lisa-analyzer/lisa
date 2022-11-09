package it.unive.lisa.program;

import it.unive.lisa.analysis.SemanticDomain;
import it.unive.lisa.program.annotations.Annotation;
import it.unive.lisa.program.annotations.Annotations;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.symbolic.value.Variable;
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
	 * The name of this global variable
	 */
	private final String name;

	/**
	 * The static type of this global variable
	 */
	private final Type staticType;

	private final CodeLocation location;

	private final Annotations annotations;

	private final boolean isInstance;

	private final Unit container;

	/**
	 * Builds an untyped global variable, identified by its name. The location
	 * where this global happens is unknown (e.g. no source file/line/column is
	 * available) as well as its type (i.e. it is {#link Untyped#INSTANCE}).
	 * 
	 * @param location   the location of this global variable
	 * @param container  the {@link Unit} containing this global
	 * @param name       the name of this global
	 * @param isInstance whether or not this is an instance global
	 */
	public Global(CodeLocation location, Unit container, String name, boolean isInstance) {
		this(location, container, name, isInstance, Untyped.INSTANCE);
	}

	/**
	 * Builds the global reference, identified by its name and its type,
	 * happening at the given location in the program.
	 * 
	 * @param location   the location where this global is defined within the
	 *                       program
	 * @param container  the {@link Unit} containing this global
	 * @param name       the name of this global
	 * @param isInstance whether or not this is an instance global
	 * @param staticType the type of this global. If unknown, use
	 *                       {@link Untyped#INSTANCE}
	 */
	public Global(CodeLocation location, Unit container, String name, boolean isInstance, Type staticType) {
		this(location, container, name, isInstance, staticType, new Annotations());
	}

	/**
	 * Builds the global reference, identified by its name and its type,
	 * happening at the given location in the program.
	 * 
	 * @param location    the location where this global is defined within the
	 *                        program
	 * @param container   the {@link Unit} containing this global
	 * @param name        the name of this global
	 * @param isInstance  whether or not this is an instance global
	 * @param staticType  the type of this global. If unknown, use
	 *                        {@link Untyped#INSTANCE}
	 * @param annotations the annotations of this global variable
	 */
	public Global(CodeLocation location, Unit container, String name, boolean isInstance, Type staticType,
			Annotations annotations) {
		Objects.requireNonNull(name, "The name of a global cannot be null");
		Objects.requireNonNull(staticType, "The type of a global cannot be null");
		Objects.requireNonNull(location, "The location of a global cannot be null");
		Objects.requireNonNull(container, "The container of a global cannot be null");
		this.location = location;
		this.name = name;
		this.staticType = staticType;
		this.annotations = annotations;
		this.container = container;
		this.isInstance = isInstance;
	}

	/**
	 * Yields the name of this global.
	 * 
	 * @return the name of this global
	 */
	public String getName() {
		return name;
	}

	/**
	 * Yields the static type of this global.
	 * 
	 * @return the static type of this global
	 */
	public Type getStaticType() {
		return staticType;
	}

	/**
	 * Yields the unit containing this global.
	 * 
	 * @return the container
	 */
	public Unit getContainer() {
		return container;
	}

	/**
	 * Yields {@code true} if and only if this is an instance global.
	 * 
	 * @return {@code true} only if that condition holds
	 */
	public boolean isInstance() {
		return isInstance;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((annotations == null) ? 0 : annotations.hashCode());
		result = prime * result + ((container == null) ? 0 : container.hashCode());
		result = prime * result + (isInstance ? 1231 : 1237);
		result = prime * result + ((location == null) ? 0 : location.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((staticType == null) ? 0 : staticType.hashCode());
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
		if (annotations == null) {
			if (other.annotations != null)
				return false;
		} else if (!annotations.equals(other.annotations))
			return false;
		if (container == null) {
			if (other.container != null)
				return false;
		} else if (!container.equals(other.container))
			return false;
		if (isInstance != other.isInstance)
			return false;
		if (location == null) {
			if (other.location != null)
				return false;
		} else if (!location.equals(other.location))
			return false;
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
		return true;
	}

	@Override
	public String toString() {
		return staticType + " " + container.getName() + "#" + name;
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
	 * Adds an annotation to the annotations of this global.
	 * 
	 * @param ann the annotation to be added
	 */
	public void addAnnotation(Annotation ann) {
		annotations.addAnnotation(ann);
	}

	/**
	 * Creates a {@link Variable} that represent this global, that can be used
	 * by {@link SemanticDomain}s to reference it.
	 * 
	 * @param where the location where the variable will be generated
	 * 
	 * @return the variable representing this parameter
	 */
	public Variable toSymbolicVariable(CodeLocation where) {
		return new Variable(staticType, name, annotations, where);
	}
}
