package it.unive.lisa.program.annotations.values;

/**
 * A byte annotation value.
 * 
 * @author <a href="mailto:vincenzo.arceri@unive.it">Vincenzo Arceri</a>
 */
public class ByteAnnotationValue implements BasicAnnotationValue {

	private final byte b;

	/**
	 * Builds a byte annotation value.
	 * 
	 * @param b the byte value
	 */
	public ByteAnnotationValue(byte b) {
		this.b = b;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + b;
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
		ByteAnnotationValue other = (ByteAnnotationValue) obj;
		if (b != other.b)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return String.valueOf(b);
	}
}
