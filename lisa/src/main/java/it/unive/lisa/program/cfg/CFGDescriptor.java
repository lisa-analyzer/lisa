package it.unive.lisa.program.cfg;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;

import it.unive.lisa.program.CodeElement;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;

/**
 * A descriptor of a CFG, containing the debug informations (source file, line,
 * column) as well as metadata.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class CFGDescriptor extends CodeElement {

	/**
	 * The name of the CFG associated with this descriptor.
	 */
	private final String name;

	/**
	 * The arguments of the CFG associated with this descriptor.
	 */
	private final Parameter[] args;

	/**
	 * The return type of the CFG associated with this descriptor.
	 */
	private final Type returnType;

	/**
	 * The list of variables defined in the cfg
	 */
	private final List<VariableTableEntry> variables;

	/**
	 * Builds the descriptor for a method that is defined at an unknown location
	 * (i.e. no source file/line/column is available) and with untyped return
	 * type, that is its type is {@link Untyped#INSTANCE}.
	 * 
	 * @param name the name of the CFG associated with this descriptor
	 * @param args the arguments of the CFG associated with this descriptor
	 */
	public CFGDescriptor(String name, Parameter... args) {
		this(null, -1, -1, name, Untyped.INSTANCE, args);
	}

	/**
	 * Builds the descriptor for a method that is defined at an unknown location
	 * (i.e. no source file/line/column is available).
	 * 
	 * @param name       the name of the CFG associated with this descriptor
	 * @param returnType the return type of the CFG associated with this
	 *                       descriptor
	 * @param args       the arguments of the CFG associated with this
	 *                       descriptor
	 */
	public CFGDescriptor(String name, Type returnType, Parameter... args) {
		this(null, -1, -1, name, returnType, args);
	}

	/**
	 * Builds the descriptor with {@link Untyped} return type.
	 * 
	 * @param sourceFile the source file where the CFG associated with this
	 *                       descriptor is defined. If unknown, use {@code null}
	 * @param line       the line number where the CFG associated with this
	 *                       descriptor is defined in the source file. If
	 *                       unknown, use {@code -1}
	 * @param col        the column where the CFG associated with this
	 *                       descriptor is defined in the source file. If
	 *                       unknown, use {@code -1}
	 * @param name       the name of the CFG associated with this descriptor
	 * @param args       the arguments of the CFG associated with this
	 *                       descriptor
	 */
	public CFGDescriptor(String sourceFile, int line, int col, String name, Parameter... args) {
		this(sourceFile, line, col, name, Untyped.INSTANCE, args);
	}

	/**
	 * Builds the descriptor.
	 * 
	 * @param sourceFile the source file where the CFG associated with this
	 *                       descriptor is defined. If unknown, use {@code null}
	 * @param line       the line number where the CFG associated with this
	 *                       descriptor is defined in the source file. If
	 *                       unknown, use {@code -1}
	 * @param col        the column where the CFG associated with this
	 *                       descriptor is defined in the source file. If
	 *                       unknown, use {@code -1}
	 * @param name       the name of the CFG associated with this descriptor
	 * @param returnType the return type of the CFG associated with this
	 *                       descriptor
	 * @param args       the arguments of the CFG associated with this
	 *                       descriptor
	 */
	public CFGDescriptor(String sourceFile, int line, int col, String name, Type returnType, Parameter... args) {
		super(sourceFile, line, col);
		Objects.requireNonNull(name, "The name of a CFG cannot be null");
		Objects.requireNonNull(args, "The array of argument names of a CFG cannot be null");
		Objects.requireNonNull(returnType, "The return type of a CFG cannot be null");
		for (int i = 0; i < args.length; i++)
			Objects.requireNonNull(args[i], "The " + i + "-th argument name of a CFG cannot be null");
		this.name = name;
		this.args = args;
		this.returnType = returnType;

		this.variables = new LinkedList<>();
		int i = 0;
		for (Parameter arg : args)
			variables.add(new VariableTableEntry(arg.getSourceFile(), arg.getLine(), arg.getCol(), i++, -1, -1,
					arg.getName(), arg.getStaticType()));
	}

	/**
	 * Yields the name of the CFG associated with this descriptor.
	 * 
	 * @return the name of the CFG
	 */
	public String getName() {
		return name;
	}

	/**
	 * Yields the full name of the CFG associated with this descriptor. This
	 * might differ from its name (e.g. it might be fully qualified with the
	 * compilation unit it belongs to).
	 * 
	 * @return the full name of the CFG
	 */
	public String getFullName() {
		return name;
	}

	/**
	 * Yields the full signature of this cfg.
	 * 
	 * @return the full signature
	 */
	public String getFullSignature() {
		return returnType + " " + name + "(" + StringUtils.join(args, ", ") + ")";
	}

	/**
	 * Yields the array containing the arguments of the CFG associated with this
	 * descriptor.
	 * 
	 * @return the arguments
	 */
	public Parameter[] getArgs() {
		return args;
	}

	/**
	 * Yields the return type of the CFG associated with this descriptor.
	 * 
	 * @return the return type
	 */
	public Type getReturnType() {
		return returnType;
	}

	/**
	 * Yields the list of {@link VariableTableEntry}s that have been added to
	 * this descriptor.
	 * 
	 * @return the list of variables entries
	 */
	public List<VariableTableEntry> getVariables() {
		return variables;
	}

	/**
	 * Builds a new {@link VariableTableEntry}, populating it with the given
	 * data, and adds it at the end of the variable table. The variable is
	 * defined at an unknown code location. The variable has an {@link Untyped}
	 * static type and is visible from the beginning to the end of the
	 * {@link CFG}.
	 * 
	 * @param name the name of the variable being added
	 */
	public void addVariable(String name) {
		addVariable(null, -1, -1, -1, -1, name, Untyped.INSTANCE);
	}

	/**
	 * Builds a new {@link VariableTableEntry}, populating it with the given
	 * data, and adds it at the end of the variable table. The variable is
	 * defined at an unknown code location. The variable has an {@link Untyped}
	 * static type.
	 * 
	 * @param scopeStart the offset of the statement the variable being added is
	 *                       first visible, {@code -1} means that this variable
	 *                       is visible since the beginning of the cfg
	 * @param scopeEnd   the offset of the statement where the variable being
	 *                       added is last visible, {@code -1} means that this
	 *                       variable is visible until the end of the cfg
	 * @param name       the name of the variable being added
	 */
	public void addVariable(int scopeStart, int scopeEnd, String name) {
		addVariable(null, -1, -1, scopeStart, scopeEnd, name, Untyped.INSTANCE);
	}

	/**
	 * Builds a new {@link VariableTableEntry}, populating it with the given
	 * data, and adds it at the end of the variable table. The variable is
	 * defined at an unknown code location.
	 * 
	 * @param scopeStart the offset of the statement the variable being added is
	 *                       first visible, {@code -1} means that this variable
	 *                       is visible since the beginning of the cfg
	 * @param scopeEnd   the offset of the statement where the variable being
	 *                       added is last visible, {@code -1} means that this
	 *                       variable is visible until the end of the cfg
	 * @param name       the name of the variable being added
	 * @param staticType the type of the variable being added. If unknown, use
	 *                       {@link Untyped#INSTANCE}
	 */
	public void addVariable(int scopeStart, int scopeEnd, String name, Type staticType) {
		addVariable(null, -1, -1, scopeStart, scopeEnd, name, staticType);
	}

	/**
	 * Builds a new {@link VariableTableEntry}, populating it with the given
	 * data, and adds it at the end of the variable table.
	 * 
	 * @param sourceFile the source file where the variable being added is
	 *                       defined. If unknown, use {@code null}
	 * @param line       the line number where the the variable being added is
	 *                       defined in the source file. If unknown, use
	 *                       {@code -1}
	 * @param col        the column where the the variable being added is
	 *                       defined in the source file. If unknown, use
	 *                       {@code -1}
	 * @param scopeStart the offset of the statement the variable being added is
	 *                       first visible, {@code -1} means that this variable
	 *                       is visible since the beginning of the cfg
	 * @param scopeEnd   the offset of the statement where the variable being
	 *                       added is last visible, {@code -1} means that this
	 *                       variable is visible until the end of the cfg
	 * @param name       the name of the variable being added
	 * @param staticType the type of the variable being added. If unknown, use
	 *                       {@link Untyped#INSTANCE}
	 */
	public void addVariable(String sourceFile, int line, int col, int scopeStart, int scopeEnd, String name,
			Type staticType) {
		variables.add(new VariableTableEntry(sourceFile, line, col, variables.size(), scopeStart, scopeEnd, name,
				staticType));
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Arrays.hashCode(args);
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((returnType == null) ? 0 : returnType.hashCode());
		result = prime * result + ((variables == null) ? 0 : variables.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		CFGDescriptor other = (CFGDescriptor) obj;
		if (!Arrays.equals(args, other.args))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (returnType == null) {
			if (other.returnType != null)
				return false;
		} else if (!returnType.equals(other.returnType))
			return false;
		if (variables == null) {
			if (other.variables != null)
				return false;
		} else if (!variables.equals(other.variables))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return getFullSignature() + " [at '" + String.valueOf(getSourceFile()) + "':" + getLine() + ":" + getCol() + "]";
	}
}
