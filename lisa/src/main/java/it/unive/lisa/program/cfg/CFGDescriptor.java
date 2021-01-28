package it.unive.lisa.program.cfg;

import it.unive.lisa.program.CodeElement;
import it.unive.lisa.program.Unit;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;

/**
 * A descriptor of a CFG, containing the debug informations (source file, line,
 * column) as well as metadata.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class CFGDescriptor extends CodeElement {

	/**
	 * The unit the cfg belongs to
	 */
	private final Unit unit;

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
	 * Whether or not the cfg is an instance cfg
	 */
	private final boolean instance;

	/**
	 * Whether or not the cfg can be overridden
	 */
	private boolean overridable;

	private final Collection<CodeMember> overriddenBy;

	private final Collection<CodeMember> overrides;

	/**
	 * Builds the descriptor for a method that is defined at an unknown location
	 * (i.e. no source file/line/column is available) and with untyped return
	 * type, that is its type is {@link Untyped#INSTANCE}.
	 * 
	 * @param unit     the {@link Unit} containing the cfg associated to this
	 *                     descriptor
	 * @param instance whether or not the cfg associated to this descriptor is
	 *                     an instance cfg
	 * @param name     the name of the CFG associated with this descriptor
	 * @param args     the arguments of the CFG associated with this descriptor
	 */
	public CFGDescriptor(Unit unit, boolean instance, String name, Parameter... args) {
		this(null, -1, -1, unit, instance, name, Untyped.INSTANCE, args);
	}

	/**
	 * Builds the descriptor for a method that is defined at an unknown location
	 * (i.e. no source file/line/column is available).
	 * 
	 * @param unit       the {@link Unit} containing the cfg associated to this
	 *                       descriptor
	 * @param instance   whether or not the cfg associated to this descriptor is
	 *                       an instance cfg
	 * @param name       the name of the CFG associated with this descriptor
	 * @param returnType the return type of the CFG associated with this
	 *                       descriptor
	 * @param args       the arguments of the CFG associated with this
	 *                       descriptor
	 */
	public CFGDescriptor(Unit unit, boolean instance, String name, Type returnType, Parameter... args) {
		this(null, -1, -1, unit, instance, name, returnType, args);
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
	 * @param unit       the {@link Unit} containing the cfg associated to this
	 *                       descriptor
	 * @param instance   whether or not the cfg associated to this descriptor is
	 *                       an instance cfg
	 * @param name       the name of the CFG associated with this descriptor
	 * @param args       the arguments of the CFG associated with this
	 *                       descriptor
	 */
	public CFGDescriptor(String sourceFile, int line, int col, Unit unit, boolean instance, String name,
			Parameter... args) {
		this(sourceFile, line, col, unit, instance, name, Untyped.INSTANCE, args);
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
	 * @param unit       the {@link Unit} containing the cfg associated to this
	 *                       descriptor
	 * @param instance   whether or not the cfg associated to this descriptor is
	 *                       an instance cfg
	 * @param name       the name of the CFG associated with this descriptor
	 * @param returnType the return type of the CFG associated with this
	 *                       descriptor
	 * @param args       the arguments of the CFG associated with this
	 *                       descriptor
	 */
	public CFGDescriptor(String sourceFile, int line, int col, Unit unit, boolean instance, String name,
			Type returnType, Parameter... args) {
		super(sourceFile, line, col);
		Objects.requireNonNull(unit, "The unit of a CFG cannot be null");
		Objects.requireNonNull(name, "The name of a CFG cannot be null");
		Objects.requireNonNull(args, "The array of argument names of a CFG cannot be null");
		Objects.requireNonNull(returnType, "The return type of a CFG cannot be null");
		for (int i = 0; i < args.length; i++)
			Objects.requireNonNull(args[i], "The " + i + "-th argument name of a CFG cannot be null");
		this.unit = unit;
		this.name = name;
		this.args = args;
		this.returnType = returnType;
		this.instance = instance;

		overridable = instance;
		overriddenBy = new HashSet<>();
		overrides = new HashSet<>();

		this.variables = new LinkedList<>();
		int i = 0;
		for (Parameter arg : args)
			addVariable(new VariableTableEntry(arg.getSourceFile(), arg.getLine(), arg.getCol(), i++, null, null,
					arg.getName(), arg.getStaticType()));
	}

	/**
	 * Yields {@code true} if and only if the cfg associated to this descriptor
	 * is an instance cfg.
	 * 
	 * @return {@code true} only if that condition holds
	 */
	public boolean isInstance() {
		return instance;
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
		return unit.getName() + "::" + getName();
	}

	/**
	 * Yields the signature of this cfg, composed by its {@link #getFullName()}
	 * followed by its parameters types.
	 * 
	 * @return the signature
	 */
	public String getSignature() {
		Type[] types = new Type[args.length];
		for (int i = 0; i < types.length; i++)
			types[i] = args[i].getStaticType();
		return getFullName() + "(" + StringUtils.join(types, ", ") + ")";
	}

	/**
	 * Yields the signature of this cfg, composed by its {@link #getFullName()}
	 * followed by its parameters (types and names).
	 * 
	 * @return the signature with parameters names included
	 */
	public String getSignatureWithParNames() {
		return getFullName() + "(" + StringUtils.join(args, ", ") + ")";
	}

	/**
	 * Yields the full signature of this cfg, that is, {@link #getSignature()}
	 * preceded by the cfg's return type.
	 * 
	 * @return the full signature
	 */
	public String getFullSignature() {
		return returnType + " " + getSignature();
	}

	/**
	 * Yields the full signature of this cfg including parameters names, that
	 * is, {@link #getSignatureWithParNames()} preceded by the cfg's return
	 * type.
	 * 
	 * @return the full signature with parameters names included
	 */
	public String getFullSignatureWithParNames() {
		return returnType + " " + getSignatureWithParNames();
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
	 * Adds a {@link VariableTableEntry} at the end of the variable table. The
	 * index of the variable gets overwritten with the first free index for this
	 * descriptor.
	 * 
	 * @param variable the entry to add
	 */
	public void addVariable(VariableTableEntry variable) {
		if (variable.getIndex() != variables.size())
			variable.setIndex(variables.size());
		variables.add(variable);
	}

	/**
	 * Yields {@code true} if and only if the cfg associated to this descriptor
	 * is can be overridden by cfgs in {@link Unit}s that inherit for the cfg's
	 * unit.
	 * 
	 * @return {@code true} only if that condition holds
	 */
	public boolean isOverridable() {
		return overridable;
	}

	/**
	 * Sets whether or not the cfg associated to this descriptor can be
	 * overridden.
	 * 
	 * @param overridable the overridability of the cfg
	 */
	public void setOverridable(boolean overridable) {
		this.overridable = overridable;
	}

	/**
	 * Yields the {@link Unit} containing the cfg associated to this descriptor.
	 * 
	 * @return the unit
	 */
	public Unit getUnit() {
		return unit;
	}

	/**
	 * Yields the collection of {@link CodeMember} that override the cfg
	 * associated with this descriptor.
	 * 
	 * @return the collection of code members
	 */
	public Collection<CodeMember> overriddenBy() {
		return overriddenBy;
	}

	/**
	 * Yields the collection of {@link CodeMember} that the cfg associated with
	 * this descriptor overrides.
	 * 
	 * @return the collection of code members
	 */
	public Collection<CodeMember> overrides() {
		return overrides;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Boolean.hashCode(overridable);
		result = prime * result + Arrays.hashCode(args);
		result = prime * result + ((unit == null) ? 0 : unit.hashCode());
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
		if (overridable != other.overridable)
			return false;
		if (unit == null) {
			if (other.unit != null)
				return false;
		} else if (!unit.equals(other.unit))
			return false;
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
		return getFullSignature() + " [at '" + String.valueOf(getSourceFile()) + "':" + getLine() + ":" + getCol()
				+ "]";
	}

	/**
	 * Checks if the signature defined by the given descriptor is matched by the
	 * one this descriptor. For two signatures to match, it is required that:
	 * <ul>
	 * <li>both signatures have the same name</li>
	 * <li>both signatures have the same number of arguments</li>
	 * <li>for each argument, the static type of the matching signature (i.e.,
	 * {@code this}) can be assigned to the static type of the matched signature
	 * (i.e., {@code signature})</li>
	 * </ul>
	 * 
	 * @param signature the other signature
	 * 
	 * @return {@code true} if the two signatures are compatible, {@code false}
	 *             otherwise
	 */
	public boolean matchesSignature(CFGDescriptor signature) {
		if (!name.equals(signature.name))
			return false;

		if (args.length != signature.args.length)
			return false;

		for (int i = 0; i < args.length; i++)
			if (!args[i].getStaticType().canBeAssignedTo(signature.args[i].getStaticType()))
				// TODO not sure if this is generic enough
				return false;

		return true;
	}
}
