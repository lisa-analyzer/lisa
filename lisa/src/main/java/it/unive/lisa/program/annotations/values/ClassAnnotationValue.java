package it.unive.lisa.program.annotations.values;

/**
 * A class annotation value.
 * 
 * @author <a href="mailto:vincenzo.arceri@unive.it">Vincenzo Arceri</a>
 */
public class ClassAnnotationValue extends BasicAnnotationValue {

	private final String className;

	/**
	 * Builds a class annotation value.
	 * 
	 * @param className the name of the class
	 */
	public ClassAnnotationValue(String className) {
		this.className = className;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((className == null) ? 0 : className.hashCode());
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
		ClassAnnotationValue other = (ClassAnnotationValue) obj;
		if (className == null) {
			if (other.className != null)
				return false;
		} else if (!className.equals(other.className))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return className;
	}
}
