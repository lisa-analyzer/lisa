package it.unive.lisa.program.annotations.values;

/**
 * An enum annotation value.
 * 
 * @author <a href="mailto:vincenzo.arceri@unive.it">Vincenzo Arceri</a>
 */
public class EnumAnnotationValue extends BasicAnnotationValue {

	private final String name;
	private final String field;

	/**
	 * Builds an enum annotation value.
	 * 
	 * @param name  the name of the enum
	 * @param field the field of the enum
	 */
	public EnumAnnotationValue(String name, String field) {
		this.name = name;
		this.field = field;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((field == null) ? 0 : field.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
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
		EnumAnnotationValue other = (EnumAnnotationValue) obj;
		if (field == null) {
			if (other.field != null)
				return false;
		} else if (!field.equals(other.field))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return name + "." + field;
	}
}
