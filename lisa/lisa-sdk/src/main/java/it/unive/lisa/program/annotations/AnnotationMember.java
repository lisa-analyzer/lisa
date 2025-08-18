package it.unive.lisa.program.annotations;

import it.unive.lisa.program.annotations.values.AnnotationValue;

/**
 * A member of an annotation.
 * 
 * @author <a href="mailto:vincenzo.arceri@unipr.it">Vincenzo Arceri</a>
 */
public class AnnotationMember implements Comparable<AnnotationMember> {

	private final String id;

	private final AnnotationValue value;

	/**
	 * Builds an annotation member from its identifier and its annotation value.
	 * 
	 * @param id    the identifier
	 * @param value the annotation value
	 */
	public AnnotationMember(
			String id,
			AnnotationValue value) {
		this.id = id;
		this.value = value;
	}

	/**
	 * Yields the identifier of this annotation member.
	 * 
	 * @return the identifier of this annotation member
	 */
	public String getId() {
		return id;
	}

	/**
	 * Yields the annotation value of this annotation member.
	 * 
	 * @return the annotation value of this annotation member
	 */
	public AnnotationValue getValue() {
		return value;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((value == null) ? 0 : value.hashCode());
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
		AnnotationMember other = (AnnotationMember) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (value == null) {
			if (other.value != null)
				return false;
		} else if (!value.equals(other.value))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return id + " = " + value;
	}

	@Override
	public int compareTo(
			AnnotationMember o) {
		int cmp = 0;
		if ((cmp = id.compareTo(o.id)) != 0)
			return cmp;
		return value.compareTo(o.value);
	}

}
