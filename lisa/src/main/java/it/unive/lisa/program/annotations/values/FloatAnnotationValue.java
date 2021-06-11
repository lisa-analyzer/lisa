package it.unive.lisa.program.annotations.values;

/**
 * A float annotation value.
 * 
 * @author <a href="mailto:vincenzo.arceri@unive.it">Vincenzo Arceri</a>
 */
public class FloatAnnotationValue implements BasicAnnotationValue {

	private final float f;

	/**
	 * Builds a float annotation value.
	 * 
	 * @param f the float value
	 */
	public FloatAnnotationValue(float f) {
		this.f = f;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Float.floatToIntBits(f);
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
		FloatAnnotationValue other = (FloatAnnotationValue) obj;
		if (Float.floatToIntBits(f) != Float.floatToIntBits(other.f))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return String.valueOf(f);
	}
}
