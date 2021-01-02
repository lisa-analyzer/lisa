package it.unive.lisa.cfg;

import java.util.Objects;

import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;

/**
 * A CFG parameter identified by its name and its type, containing the
 * information about the source file, line and column where the parameter is
 * defined. No information about the CFG where the parameter appears is
 * contained.
 * 
 * @author <a href="mailto:vincenzo.arceri@unive.it">Vincenzo Arceri</a>
 */
public class Parameter {

	/**
	 * The source file where this parameter happens. If it is unknown, this
	 * field might contain {@code null}.
	 */
	private final String sourceFile;

	/**
	 * The line where this parameter happens in the source file. If it is
	 * unknown, this field might contain {@code -1}.
	 */
	private final int line;

	/**
	 * The column where this parameter happens in the source file. If it is
	 * unknown, this field might contain {@code -1}.
	 */
	private final int col;

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
	public Parameter(String name) {
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
	public Parameter(String name, Type staticType) {
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
	public Parameter(String sourceFile, int line, int col, String name, Type staticType) {
		Objects.requireNonNull(name, "The name of a parameter cannot be null");
		Objects.requireNonNull(staticType, "The type of a parameter cannot be null");
		this.sourceFile = sourceFile;
		this.line = line;
		this.col = col;
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

	/**
	 * Yields the source file name where this parameter happens. This method
	 * returns {@code null} if the source file is unknown.
	 * 
	 * @return the source file, or {@code null}
	 */
	public String getSourceFile() {
		return sourceFile;
	}

	/**
	 * Yields the line number where this parameter happens in the source file.
	 * This method returns {@code -1} if the line number is unknown.
	 * 
	 * @return the line number, or {@code -1}
	 */
	public final int getLine() {
		return line;
	}

	/**
	 * Yields the column where this parameter happens in the source file. This
	 * method returns {@code -1} if the line number is unknown.
	 * 
	 * @return the column, or {@code -1}
	 */
	public final int getCol() {
		return col;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + col;
		result = prime * result + line;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((sourceFile == null) ? 0 : sourceFile.hashCode());
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
		if (col != other.col)
			return false;
		if (line != other.line)
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (sourceFile == null) {
			if (other.sourceFile != null)
				return false;
		} else if (!sourceFile.equals(other.sourceFile))
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
