package it.unive.lisa.program.annotations.values;

/**
 * A double annotation value.
 * 
 * @author <a href="mailto:vincenzo.arceri@unive.it">Vincenzo Arceri</a>
 */
public class DoubleAnnotationValue implements BasicAnnotationValue {

	private final double d;

	/**
	 * Builds a double annotation value.
	 * 
	 * @param d the double value
	 */
	public DoubleAnnotationValue(double d) {
		this.d = d;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(d);
		result = prime * result + (int) (temp ^ (temp >>> 32));
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
		DoubleAnnotationValue other = (DoubleAnnotationValue) obj;
		if (Double.doubleToLongBits(d) != Double.doubleToLongBits(other.d))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return String.valueOf(d);
	}
}