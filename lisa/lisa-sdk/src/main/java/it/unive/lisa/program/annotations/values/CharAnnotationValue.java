package it.unive.lisa.program.annotations.values;

/**
 * A char annotation value.
 * 
 * @author <a href="mailto:vincenzo.arceri@unipr.it">Vincenzo Arceri</a>
 */
public class CharAnnotationValue implements BasicAnnotationValue {

	private final char c;

	/**
	 * Builds a char annotation value.
	 * 
	 * @param c the char value
	 */
	public CharAnnotationValue(
			char c) {
		this.c = c;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + c;
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
		CharAnnotationValue other = (CharAnnotationValue) obj;
		if (c != other.c)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return String.valueOf(c);
	}

	@Override
	public int compareTo(
			AnnotationValue o) {
		if (!(o instanceof CharAnnotationValue))
			return getClass().getName().compareTo(o.getClass().getName());

		CharAnnotationValue other = (CharAnnotationValue) o;
		return Character.compare(c, other.c);
	}

}
