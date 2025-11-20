package it.unive.lisa.program.annotations.values;

/**
 * A short annotation value.
 * 
 * @author <a href="mailto:vincenzo.arceri@unipr.it">Vincenzo Arceri</a>
 */
public class ShortAnnotationValue
		implements
		BasicAnnotationValue {

	private final short s;

	/**
	 * Builds a short annotation value.
	 * 
	 * @param s the short value
	 */
	public ShortAnnotationValue(
			short s) {
		this.s = s;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + s;
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
		ShortAnnotationValue other = (ShortAnnotationValue) obj;
		if (s != other.s)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return String.valueOf(s);
	}

	@Override
	public int compareTo(
			AnnotationValue o) {
		if (!(o instanceof ShortAnnotationValue))
			return getClass().getName().compareTo(o.getClass().getName());

		ShortAnnotationValue other = (ShortAnnotationValue) o;
		return Short.compare(s, other.s);
	}

}
