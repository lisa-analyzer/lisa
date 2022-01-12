package it.unive.lisa.program.cfg;

import it.unive.lisa.program.CodeElement;
import it.unive.lisa.program.annotations.Annotation;
import it.unive.lisa.program.annotations.Annotations;
import it.unive.lisa.program.cfg.statement.Statement;
import it.unive.lisa.program.cfg.statement.VariableRef;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;
import java.util.Objects;

/**
 * An entry in the variable table representing a CFG variable identified by its
 * index, containing the information about the source file, line and column
 * where the variable is defined.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class VariableTableEntry implements CodeElement {

	/**
	 * The index of the variable
	 */
	private int index;

	/**
	 * The name of this variable
	 */
	private final String name;

	/**
	 * The static type of this variable
	 */
	private final Type staticType;

	/**
	 * The statement where this variable is first visible. {@code -1} means that
	 * this variable is visible since the beginning of the cfg.
	 */
	private Statement scopeStart;

	/**
	 * The statement where this variable is last visible. {@code -1} means that
	 * this variable is visible until the end of the cfg.
	 */
	private Statement scopeEnd;

	/**
	 * The location where this variable is defined.
	 */
	private final CodeLocation location;

	private final Annotations annotations;

	/**
	 * Builds an untyped variable table entry, identified by its index. Its type
	 * is unknown (i.e. it is {#link Untyped#INSTANCE}).
	 * 
	 * @param location the location of this variable
	 * @param index    the index of the variable entry
	 * @param name     the name of this variable
	 */
	public VariableTableEntry(CodeLocation location, int index, String name) {
		this(location, index, null, null, name, Untyped.INSTANCE);
	}

	/**
	 * Builds an untyped variable table entry, identified by its index. Its type
	 * is unknown (i.e. it is {#link Untyped#INSTANCE}).
	 * 
	 * @param location   the location of this variable
	 * @param index      the index of the variable entry
	 * @param scopeStart the statement where this variable is first visible,
	 *                       {@code null} means that this variable is visible
	 *                       since the beginning of the cfg
	 * @param scopeEnd   the statement where this variable is last visible,
	 *                       {@code null} means that this variable is visible
	 *                       until the end of the cfg
	 * @param name       the name of this variable
	 */
	public VariableTableEntry(CodeLocation location, int index, Statement scopeStart, Statement scopeEnd, String name) {
		this(location, index, scopeStart, scopeEnd, name, Untyped.INSTANCE);
	}

	/**
	 * Builds the variable table entry, identified by its index, representing a
	 * variable defined at the given location in the program.
	 * 
	 * @param location   the location where the expression is defined within the
	 *                       program
	 * @param index      the index of the variable entry
	 * @param scopeStart the statement where this variable is first visible,
	 *                       {@code null} means that this variable is visible
	 *                       since the beginning of the cfg
	 * @param scopeEnd   the statement where this variable is last visible,
	 *                       {@code null} means that this variable is visible
	 *                       until the end of the cfg
	 * @param name       the name of this variable
	 * @param staticType the type of this variable. If unknown, use
	 *                       {@link Untyped#INSTANCE}
	 */
	public VariableTableEntry(CodeLocation location, int index, Statement scopeStart, Statement scopeEnd,
			String name, Type staticType) {
		this(location, index, scopeStart, scopeEnd, name, staticType, new Annotations());
	}

	/**
	 * Builds the variable table entry with its annotations, identified by its
	 * index, representing a variable defined at the given location in the
	 * program.
	 * 
	 * @param location    the location where the expression is defined within
	 *                        the program
	 * @param index       the index of the variable entry
	 * @param scopeStart  the statement where this variable is first visible,
	 *                        {@code null} means that this variable is visible
	 *                        since the beginning of the cfg
	 * @param scopeEnd    the statement where this variable is last visible,
	 *                        {@code null} means that this variable is visible
	 *                        until the end of the cfg
	 * @param name        the name of this variable
	 * @param staticType  the type of this variable. If unknown, use
	 *                        {@link Untyped#INSTANCE}
	 * @param annotations the annotations of this variable
	 */
	public VariableTableEntry(CodeLocation location, int index, Statement scopeStart, Statement scopeEnd,
			String name, Type staticType, Annotations annotations) {
		Objects.requireNonNull(name, "The name of a variable cannot be null");
		Objects.requireNonNull(staticType, "The type of a variable cannot be null");
		Objects.requireNonNull(location, "The location of a variable cannot be null");
		this.location = location;
		this.index = index;
		this.name = name;
		this.staticType = staticType;
		this.scopeStart = scopeStart;
		this.scopeEnd = scopeEnd;
		this.annotations = annotations;
	}

	/**
	 * Yields the index of this variable.
	 * 
	 * @return the index of this variable
	 */
	public int getIndex() {
		return index;
	}

	/**
	 * Sets the index of this variable.
	 * 
	 * @param index the new index of this variable
	 */
	public void setIndex(int index) {
		this.index = index;
	}

	/**
	 * Yields the statement where this variable is first visible. {@code null}
	 * means that this variable is visible since the beginning of the cfg.
	 * 
	 * @return the scope start, or {@code null}
	 */
	public Statement getScopeStart() {
		return scopeStart;
	}

	/**
	 * Sets the statement where this variable is first visible. {@code null}
	 * means that this variable is visible since the beginning of the cfg.
	 * Changing the starting scope should be performed whenever the starting
	 * scope is removed from the cfg due to simplifications or code
	 * transformations.
	 * 
	 * @param scopeStart the scope start, or {@code null}
	 */
	public void setScopeStart(Statement scopeStart) {
		this.scopeStart = scopeStart;
	}

	/**
	 * Yields the statement where this variable is last visible. {@code null}
	 * means that this variable is visible until the end of the cfg.
	 * 
	 * @return the scope end, or {@code null}
	 */
	public Statement getScopeEnd() {
		return scopeEnd;
	}

	/**
	 * Sets the statement where this variable is last visible. {@code null}
	 * means that this variable is visible until the end of the cfg. Changing
	 * the ending scope should be performed whenever the ending scope is removed
	 * from the cfg due to simplifications or code transformations.
	 * 
	 * @param scopeEnd the scope end, or {@code null}
	 */
	public void setScopeEnd(Statement scopeEnd) {
		this.scopeEnd = scopeEnd;
	}

	/**
	 * Yields the name of this variable.
	 * 
	 * @return the name of this variable
	 */
	public String getName() {
		return name;
	}

	/**
	 * Yields the static type of this variable.
	 * 
	 * @return the static type of this variable
	 */
	public Type getStaticType() {
		return staticType;
	}

	/**
	 * Creates a {@link VariableRef} for the variable depicted by this entry,
	 * happening in the given {@link CFG} at the location of its descriptor.
	 * 
	 * @param cfg the cfg that the returned variable reference will be linked to
	 * 
	 * @return a reference to the variable depicted by this entry
	 */
	public VariableRef createReference(CFG cfg) {
		return new VariableRef(cfg, cfg.getDescriptor().getLocation(), name, staticType);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + index;
		result = prime * result + ((annotations == null) ? 0 : annotations.hashCode());
		result = prime * result + ((location == null) ? 0 : location.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((scopeEnd == null) ? 0 : scopeEnd.hashCode());
		result = prime * result + ((scopeStart == null) ? 0 : scopeStart.hashCode());
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
		VariableTableEntry other = (VariableTableEntry) obj;
		if (annotations == null) {
			if (other.annotations != null)
				return false;
		} else if (!annotations.equals(other.annotations))
			return false;
		if (index != other.index)
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
		if (scopeEnd == null) {
			if (other.scopeEnd != null)
				return false;
		} else if (!scopeEnd.equals(other.scopeEnd))
			return false;
		if (scopeStart == null) {
			if (other.scopeStart != null)
				return false;
		} else if (!scopeStart.equals(other.scopeStart))
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
		return "[" + index + "] " + staticType + " " + name;
	}

	@Override
	public CodeLocation getLocation() {
		return location;
	}

	/**
	 * Yields the annotations of this variable.
	 * 
	 * @return the annotations of this variable
	 */
	public Annotations getAnnotations() {
		return annotations;
	}

	/**
	 * Adds an annotation to the annotations of this variable table entry.
	 * 
	 * @param ann the annotation to be added
	 */
	public void addAnnotation(Annotation ann) {
		annotations.addAnnotation(ann);
	}
}
