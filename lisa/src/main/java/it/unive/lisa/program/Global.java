package it.unive.lisa.program;

import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;
import java.util.Objects;

/**
 * A global variable, scoped by its container. Instances of this class can refer
 * both to instance variables or static global variables.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class Global extends CodeElement {

	/**
	 * The name of this parameter
	 */
	private final String name;

	/**
	 * The static type of this parameter
	 */
	private final Type staticType;

	/**
	 * Builds an untyped parameter reference, identified by its name. The
	 * location where this parameter reference happens is unknown (i.e. no
	 * source file/line/column is available) as well as its type (i.e. it is
	 * {#link Untyped#INSTANCE}).
	 * 
	 * @param name the name of this parameter
	 */
	public Global(String name) {
		this(null, -1, -1, name, Untyped.INSTANCE);
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
		this(null, -1, -1, name, staticType);
	}

	/**
	 * Builds the parameter reference, identified by its name and its type,
	 * happening at the given location in the program.
	 * 
	 * @param sourceFile the source file where this parameter happens. If
	 *                       unknown, use {@code null}
	 * @param line       the line number where this parameter happens in the
	 *                       source file. If unknown, use {@code -1}
	 * @param col        the column where this parameter happens in the source
	 *                       file. If unknown, use {@code -1}
	 * @param name       the name of this parameter
	 * @param staticType the type of this parameter. If unknown, use
	 *                       {@link Untyped#INSTANCE}
	 */
	public Global(String sourceFile, int line, int col, String name, Type staticType) {
		super(sourceFile, line, col);
		Objects.requireNonNull(name, "The name of a parameter cannot be null");
		Objects.requireNonNull(staticType, "The type of a parameter cannot be null");
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
		return true;
	}

	@Override
	public String toString() {
		return staticType + " " + name;
	}
}
