package it.unive.lisa.symbolic.value;

import it.unive.lisa.type.NullType;

/**
 * A {@link Constant} that represent the {@code null} value. There is only one
 * instance of this class, that is available through field {@link #INSTANCE}.
 * 
 * @author <a href="mailto:luca.negrini@unive.it">Luca Negrini</a>
 */
public class NullConstant extends Constant {

	/**
	 * The singleton instance of {@link NullConstant}.
	 */
	public static final NullConstant INSTANCE = new NullConstant();

	private static final Object NULL_CONST = new Object();

	private NullConstant() {
		super(NullType.INSTANCE, NULL_CONST);
	}

	@Override
	public int hashCode() {
		return super.hashCode() ^ getClass().getName().hashCode();
	}

	@Override
	public boolean equals(Object obj) {
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
