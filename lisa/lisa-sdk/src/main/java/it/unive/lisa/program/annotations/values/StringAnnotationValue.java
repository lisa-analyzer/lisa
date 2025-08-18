package it.unive.lisa.program.annotations.values;

/**
 * A string annotation value.
 * 
 * @author <a href="mailto:vincenzo.arceri@unipr.it">Vincenzo Arceri</a>
 */
public class StringAnnotationValue implements BasicAnnotationValue {

	private final String s;

	/**
	 * Builds a string annotation value.
	 * 
	 * @param s the string value
	 */
	public StringAnnotationValue(
			String s) {
		this.s = s;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((s == null) ? 0 : s.hashCode());
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
		StringAnnotationValue other = (StringAnnotationValue) obj;
		if (s == null) {
			if (other.s != null)
				return false;
		} else if (!s.equals(other.s))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return s;
	}

	@Override
	public int compareTo(
			AnnotationValue o) {
		if (!(o instanceof StringAnnotationValue))
			return getClass().getName().compareTo(o.getClass().getName());

		StringAnnotationValue other = (StringAnnotationValue) o;
		return s.compareTo(other.s);
	}

}
