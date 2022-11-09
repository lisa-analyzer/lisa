package it.unive.lisa.program.cfg;

import it.unive.lisa.analysis.SemanticDomain;
import it.unive.lisa.program.CodeElement;
import it.unive.lisa.program.annotations.Annotation;
import it.unive.lisa.program.annotations.Annotations;
import it.unive.lisa.program.cfg.statement.Expression;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;
import java.util.Objects;

/**
 * A CFG parameter identified by its name and its type, containing the
 * information about the source file, line and column where the parameter is
 * defined. No information about the CFG where the parameter appears is
 * contained.
 * 
 * @author <a href="mailto:vincenzo.arceri@unive.it">Vincenzo Arceri</a>
 */
public class Parameter implements CodeElement {

	/**
	 * The name of this parameter
	 */
	private final String name;

	/**
	 * The static type of this parameter
	 */
	private final Type staticType;

	private final CodeLocation location;

	private final Annotations annotations;

	private final Expression defaultValue;

	/**
	 * Builds an untyped parameter reference, identified by its name. The type
	 * of this parameter is unknown (i.e. it is {#link Untyped#INSTANCE}).
	 * 
	 * @param location the location of this parameter
	 * @param name     the name of this parameter
	 */
	public Parameter(CodeLocation location, String name) {
		this(location, name, Untyped.INSTANCE, null, new Annotations());
	}

	/**
	 * Builds the parameter reference, identified by its name and its type,
	 * happening at the given location in the program.
	 * 
	 * @param location   the location where this parameter is defined within the
	 *                       program
	 * @param name       the name of this parameter
	 * @param staticType the type of this parameter. If unknown, use
	 *                       {@link Untyped#INSTANCE}
	 */
	public Parameter(CodeLocation location, String name, Type staticType) {
		this(location, name, staticType, null, new Annotations());
	}

	/**
	 * Builds the parameter reference, identified by its name and its type,
	 * happening at the given location in the program.
	 * 
	 * @param location     the location where this parameter is defined within
	 *                         the program
	 * @param name         the name of this parameter
	 * @param defaultValue the default value for this parameter that can be used
	 *                         when a call does not specify a value for it
	 */
	public Parameter(CodeLocation location, String name, Expression defaultValue) {
		this(location, name, defaultValue.getStaticType(), defaultValue, new Annotations());
	}

	/**
	 * Builds the parameter reference, identified by its name and its type,
	 * happening at the given location in the program.
	 * 
	 * @param location     the location where this parameter is defined within
	 *                         the program
	 * @param name         the name of this parameter
	 * @param staticType   the type of this parameter. If unknown, use
	 *                         {@link Untyped#INSTANCE}
	 * @param defaultValue the default value for this parameter that can be used
	 *                         when a call does not specify a value for it
	 * @param annotations  the annotations of this parameter
	 */
	public Parameter(CodeLocation location, String name, Type staticType, Expression defaultValue,
			Annotations annotations) {
		Objects.requireNonNull(name, "The name of a parameter cannot be null");
		Objects.requireNonNull(staticType, "The type of a parameter cannot be null");
		Objects.requireNonNull(location, "The location of a CFG cannot be null");
		this.location = location;
		this.name = name;
		this.staticType = staticType;
		this.annotations = annotations;
		this.defaultValue = defaultValue;
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
		result = prime * result + ((annotations == null) ? 0 : annotations.hashCode());
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
		Parameter other = (Parameter) obj;
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
	 * Yields the annotations of this parameter.
	 * 
	 * @return the annotations of this parameter
	 */
	public Annotations getAnnotations() {
		return annotations;
	}

	/**
	 * Adds an annotations to this parameter.
	 * 
	 * @param ann the annotation to be added
	 */
	public void addAnnotation(Annotation ann) {
		annotations.addAnnotation(ann);
	}

	/**
	 * Yields the default value for this parameter that can be used when a call
	 * does not specify a value for it.
	 * 
	 * @return the default value
	 */
	public Expression getDefaultValue() {
		return defaultValue;
	}

	/**
	 * Creates a {@link Variable} that represent this parameter, that can be
	 * used by {@link SemanticDomain}s to reference this parameter.
	 * 
	 * @return the variable representing this parameter
	 */
	public Variable toSymbolicVariable() {
		return new Variable(staticType, name, annotations, location);
	}
}
