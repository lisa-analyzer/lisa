package it.unive.lisa.program.annotations.values;

/**
 * An integer annotation value.
 * 
 * @author <a href="mailto:vincenzo.arceri@unipr.it">Vincenzo Arceri</a>
 */
public class IntAnnotationValue implements BasicAnnotationValue {

	private final int i;

	/**
	 * Builds an integer annotation value.
	 * 
	 * @param i the integer value
	 */
	public IntAnnotationValue(
			int i) {
		this.i = i;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + i;
		return result;
	}

	@Override
	public boolean equals(
			Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		IntAnnotationValue other = (IntAnnotationValue) obj;
		if (i != other.i)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return String.valueOf(i);
	}

	@Override
	public int compareTo(
			AnnotationValue o) {
		if (!(o instanceof IntAnnotationValue))
			return getClass().getName().compareTo(o.getClass().getName());

		IntAnnotationValue other = (IntAnnotationValue) o;
		return Integer.compare(i, other.i);
	}

}
