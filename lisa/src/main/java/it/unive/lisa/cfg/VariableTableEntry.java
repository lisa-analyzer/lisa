package it.unive.lisa.cfg;

import it.unive.lisa.cfg.statement.VariableRef;
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
public class VariableTableEntry {

	/**
	 * The source file where this variable happens. If it is unknown, this field
	 * might contain {@code null}.
	 */
	private final String sourceFile;

	/**
	 * The line where this variable happens in the source file. If it is
	 * unknown, this field might contain {@code -1}.
	 */
	private final int line;

	/**
	 * The column where this variable happens in the source file. If it is
	 * unknown, this field might contain {@code -1}.
	 */
	private final int col;

	/**
	 * The index of the variable
	 */
	private final int index;

	/**
	 * The name of this variable
	 */
	private final String name;

	/**
	 * The static type of this variable
	 */
	private final Type staticType;

	/**
	 * The offset of the statement where this variable is first visible.
	 * {@code -1} means that this variable is visible since the beginning of the
	 * cfg.
	 */
	private final int scopeStart;

	/**
	 * The offset of the statement where this variable is last visible.
	 * {@code -1} means that this variable is visible until the end of the cfg.
	 */
	private final int scopeEnd;

	/**
	 * Builds an untyped variable table entry, identified by its index. The
	 * location where the variable is defined is unknown (i.e. no source
	 * file/line/column is available) as well as its type (i.e. it is {#link
	 * Untyped#INSTANCE}).
	 * 
	 * @param index the index of the variable entry
	 * @param name  the name of this variable
	 */
	public VariableTableEntry(int index, String name) {
		this(null, -1, -1, index, -1, -1, name, Untyped.INSTANCE);
	}

	/**
	 * Builds an untyped variable table entry, identified by its index. The
	 * location where the variable is defined is unknown (i.e. no source
	 * file/line/column is available) as well as its type (i.e. it is {#link
	 * Untyped#INSTANCE}).
	 * 
	 * @param index      the index of the variable entry
	 * @param scopeStart the offset of the statement where this variable is
	 *                       first visible, {@code -1} means that this variable
	 *                       is visible since the beginning of the cfg
	 * @param scopeEnd   the offset of the statement where this variable is last
	 *                       visible, {@code -1} means that this variable is
	 *                       visible until the end of the cfg
	 * @param name       the name of this variable
	 */
	public VariableTableEntry(int index, int scopeStart, int scopeEnd, String name) {
		this(null, -1, -1, index, scopeStart, scopeEnd, name, Untyped.INSTANCE);
	}

	/**
	 * Builds a typed variable table entry, identified by its index. The
	 * location where the variable is defined is unknown (i.e. no source
	 * file/line/column is available).
	 * 
	 * @param index      the index of the variable entry
	 * @param scopeStart the offset of the statement where this variable is
	 *                       first visible, {@code -1} means that this variable
	 *                       is visible since the beginning of the cfg
	 * @param scopeEnd   the offset of the statement where this variable is last
	 *                       visible, {@code -1} means that this variable is
	 *                       visible until the end of the cfg
	 * @param name       the name of this variable
	 * @param staticType the type of this variable
	 */
	public VariableTableEntry(int index, int scopeStart, int scopeEnd, String name, Type staticType) {
		this(null, -1, -1, index, scopeStart, scopeEnd, name, staticType);
	}

	/**
	 * Builds the variable table entry, identified by its index, representing a
	 * variable defined at the given location in the program.
	 * 
	 * @param sourceFile the source file where this variable happens. If
	 *                       unknown, use {@code null}
	 * @param line       the line number where this variable happens in the
	 *                       source file. If unknown, use {@code -1}
	 * @param col        the column where this variable happens in the source
	 *                       file. If unknown, use {@code -1}
	 * @param index      the index of the variable entry
	 * @param scopeStart the offset of the statement where this variable is
	 *                       first visible, {@code -1} means that this variable
	 *                       is visible since the beginning of the cfg
	 * @param scopeEnd   the offset of the statement where this variable is last
	 *                       visible, {@code -1} means that this variable is
	 *                       visible until the end of the cfg
	 * @param name       the name of this variable
	 * @param staticType the type of this variable. If unknown, use
	 *                       {@link Untyped#INSTANCE}
	 */
	public VariableTableEntry(String sourceFile, int line, int col, int index, int scopeStart, int scopeEnd,
			String name, Type staticType) {
		Objects.requireNonNull(name, "The name of a variable cannot be null");
		Objects.requireNonNull(staticType, "The type of a variable cannot be null");
		this.sourceFile = sourceFile;
		this.line = line;
		this.col = col;
		this.index = index;
		this.name = name;
		this.staticType = staticType;
		this.scopeStart = scopeStart;
		this.scopeEnd = scopeEnd;
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
	 * Yields the offset of the statement where this variable is first visible.
	 * {@code -1} means that this variable is visible since the beginning of the
	 * cfg.
	 * 
	 * @return the scope start, or {@code -1}
	 */
	public int getScopeStart() {
		return scopeStart;
	}

	/**
	 * Yields the offset of the statement where this variable is last visible.
	 * {@code -1} means that this variable is visible until the end of the cfg.
	 * 
	 * @return the scope start, or {@code -1}
	 */
	public int getScopeEnd() {
		return scopeEnd;
	}

	/**
	 * Yields the source file name where the variable is defined. This method
	 * returns {@code null} if the source file is unknown.
	 * 
	 * @return the source file, or {@code null}
	 */
	public String getSourceFile() {
		return sourceFile;
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
	 * Yields the line number where the variable is defined in the source file.
	 * This method returns {@code -1} if the line number is unknown.
	 * 
	 * @return the line number, or {@code -1}
	 */
	public final int getLine() {
		return line;
	}

	/**
	 * Yields the column where the variable is defined in the source file. This
	 * method returns {@code -1} if the line number is unknown.
	 * 
	 * @return the column, or {@code -1}
	 */
	public final int getCol() {
		return col;
	}

	/**
	 * Creates a {@link VariableRef} for the variable depicted by this entry,
	 * happening in the given {@link CFG} at the source file location of its
	 * descriptor.
	 * 
	 * @param cfg the cfg that the returned variable reference will be linked to
	 * 
	 * @return a reference to the variable depicted by this entry
	 */
	public VariableRef createReference(CFG cfg) {
		return new VariableRef(cfg, cfg.getDescriptor().getSourceFile(), cfg.getDescriptor().getLine(),
				cfg.getDescriptor().getCol(), name, staticType);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + index;
		result = prime * result + scopeStart;
		result = prime * result + scopeEnd;
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
		VariableTableEntry other = (VariableTableEntry) obj;
		if (index != other.index)
			return false;
		if (scopeStart != other.scopeStart)
			return false;
		if (scopeEnd != other.scopeEnd)
			return false;
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
		return "[" + index + "]" + staticType + " " + name;
	}
}
