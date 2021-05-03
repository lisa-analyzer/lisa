package it.unive.lisa.program.annotations.values;

/**
 * A Boolean annotation value.
 * 
 * @author <a href="mailto:vincenzo.arceri@unive.it">Vincenzo Arceri</a>
 */
public class BoolAnnotationValue extends BasicAnnotationValue {

	private final boolean b;

	/**
	 * Builds a Boolean annotation value.
	 * 
	 * @param b the boolean value
	 */
	public BoolAnnotationValue(boolean b) {
		this.b = b;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (b ? 1231 : 1237);
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
		BoolAnnotationValue other = (BoolAnnotationValue) obj;
		if (b != other.b)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return String.valueOf(b);
	}
}
