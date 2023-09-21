package it.unive.lisa.program;

import java.util.Objects;

import it.unive.lisa.program.annotations.Annotations;
import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.symbolic.value.Constant;

/**
 * A global variable, scoped by its container, that is fixed to a statically
 * constant (and immutable) value. A constant global is never an instance
 * global.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class ConstantGlobal extends Global {

	private final Constant constant;

	/**
	 * Builds an constant global variable, identified by its name. The type of
	 * this global is the one of the constant.
	 * 
	 * @param location  the location of this global variable
	 * @param container the {@link Unit} containing this global
	 * @param name      the name of this global
	 * @param constant  the constant value of this global
	 */
	public ConstantGlobal(CodeLocation location, Unit container, String name, Constant constant) {
		this(location, container, name, constant, new Annotations());
	}

	/**
	 * Builds the constant global, identified by its name, happening at the
	 * given location in the program.
	 * 
	 * @param location    the location where this global is defined within the
	 *                        program
	 * @param container   the {@link Unit} containing this global
	 * @param name        the name of this global
	 * @param constant    the constant value of this global
	 * @param annotations the annotations of this global variable
	 */
	public ConstantGlobal(CodeLocation location, Unit container, String name, Constant constant,
			Annotations annotations) {
		super(location, container, name, false, constant.getStaticType(), annotations);
		Objects.requireNonNull(constant, "The constant of a constant global cannot be null");
		this.constant = constant;
	}

	/**
	 * Yields the constant value associated to this global.
	 * 
	 * @return the constant value
	 */
	public Constant getConstant() {
		return constant;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((constant == null) ? 0 : constant.hashCode());
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
		ConstantGlobal other = (ConstantGlobal) obj;
		if (constant == null) {
			if (other.constant != null)
				return false;
		} else if (!constant.equals(other.constant))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "const " + super.toString() + " = " + constant;
	}
}
