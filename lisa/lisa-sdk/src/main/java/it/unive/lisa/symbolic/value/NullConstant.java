package it.unive.lisa.symbolic.value;

import it.unive.lisa.program.cfg.CodeLocation;
import it.unive.lisa.type.NullType;

/**
 * A {@link Constant} that represent the {@code null} value.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class NullConstant extends Constant {

	private static final Object NULL_CONST = new Object();

	/**
	 * Builds a null constant.
	 * 
	 * @param location the location where the expression is defined within the
	 *                     program
	 */
	public NullConstant(
			CodeLocation location) {
		super(NullType.INSTANCE, NULL_CONST, location);
	}

	@Override
	public int hashCode() {
		return super.hashCode() ^ getClass().getName().hashCode();
	}

	@Override
	public boolean equals(
			Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "null";
	}
}
