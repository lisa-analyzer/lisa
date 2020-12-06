package it.unive.lisa.cfg;

import it.unive.lisa.cfg.type.Type;
import it.unive.lisa.cfg.type.Untyped;
import java.util.Arrays;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;

/**
 * A descriptor of a CFG, containing the debug informations (source file, line,
 * column) as well as metadata.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class CFGDescriptor {

	/**
	 * The source file where the CFG associated with this descriptor is defined.
	 * If it is unknown, this field might contain {@code null}.
	 */
	private final String sourceFile;

	/**
	 * The line where the CFG associated with this descriptor is defined in the
	 * source file. If it is unknown, this field might contain {@code -1}.
	 */
	private final int line;

	/**
	 * The column where the CFG associated with this descriptor is defined in
	 * the source file. If it is unknown, this field might contain {@code -1}.
	 */
	private final int col;

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
		Objects.requireNonNull(name, "The name of a CFG cannot be null");
		Objects.requireNonNull(args, "The array of argument names of a CFG cannot be null");
		Objects.requireNonNull(returnType, "The return type of a CFG cannot be null");
		for (int i = 0; i < args.length; i++)
			Objects.requireNonNull(args[i], "The " + i + "-th argument name of a CFG cannot be null");
		this.sourceFile = sourceFile;
		this.line = line;
		this.col = col;
		this.name = name;
		this.args = args;
		this.returnType = returnType;
	}

	/**
	 * Yields the source file name where the CFG associated with this descriptor
	 * is defined. This method returns {@code null} if the source file is
	 * unknown.
	 * 
	 * @return the source file, or {@code null}
	 */
	public final String getSourceFile() {
		return sourceFile;
	}

	/**
	 * Yields the line number where the CFG associated with this descriptor is
	 * defined in the source file. This method returns {@code -1} if the line
	 * number is unknown.
	 * 
	 * @return the line number, or {@code -1}
	 */
	public final int getLine() {
		return line;
	}

	/**
	 * Yields the column where the CFG associated with this descriptor is
	 * defined in the source file. This method returns {@code -1} if the line
	 * number is unknown.
	 * 
	 * @return the column, or {@code -1}
	 */
	public final int getCol() {
		return col;
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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(args);
		result = prime * result + col;
		result = prime * result + line;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((sourceFile == null) ? 0 : sourceFile.hashCode());
		result = prime * result + ((returnType == null) ? 0 : returnType.hashCode());
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
		CFGDescriptor other = (CFGDescriptor) obj;
		if (!Arrays.equals(args, other.args))
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
		if (!getReturnType().equals(other.getReturnType()))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return getFullSignature() + " [at '" + String.valueOf(sourceFile) + "':" + line + ":" + col + "]";
	}
}
