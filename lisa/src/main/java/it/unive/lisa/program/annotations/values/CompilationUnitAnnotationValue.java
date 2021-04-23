package it.unive.lisa.program.annotations.values;

/**
 * A compilation unit annotation value.
 * 
 * @author <a href="mailto:vincenzo.arceri@unive.it">Vincenzo Arceri</a>
 */
public class CompilationUnitAnnotationValue extends BasicAnnotationValue {

	private final String unitName;

	/**
	 * Builds a compilation unit annotation value.
	 * 
	 * @param unitName the name of the compilation unit
	 */
	public CompilationUnitAnnotationValue(String unitName) {
		this.unitName = unitName;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((unitName == null) ? 0 : unitName.hashCode());
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
		CompilationUnitAnnotationValue other = (CompilationUnitAnnotationValue) obj;
		if (unitName == null) {
			if (other.unitName != null)
				return false;
		} else if (!unitName.equals(other.unitName))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return unitName;
	}
}
