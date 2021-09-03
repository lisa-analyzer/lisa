package it.unive.lisa.program.annotations;

import java.util.Collections;
import java.util.List;

/**
 * A single annotation.
 * 
 * @author <a href="mailto:vincenzo.arceri@unive.it">Vincenzo Arceri</a>
 */
public class Annotation {

	private final String annotationName;

	private final List<AnnotationMember> annotationMembers;

	private final boolean inherited;

	/**
	 * Builds an annotation from its name .
	 * 
	 * @param annotationName the name of the annotation
	 */
	public Annotation(String annotationName) {
		this(annotationName, Collections.emptyList(), false);
	}

	/**
	 * Builds an annotation from its name.
	 * 
	 * @param annotationName the name of the annotation
	 * @param inherited      denotes whether the annotation can be inherited
	 */
	public Annotation(String annotationName, boolean inherited) {
		this(annotationName, Collections.emptyList(), inherited);
	}

	/**
	 * Builds an annotation from its name and its members that cannot be
	 * inherited.
	 * 
	 * @param annotationName    the name of the annotation
	 * @param annotationMembers the annotation members
	 */
	public Annotation(String annotationName, List<AnnotationMember> annotationMembers) {
		this(annotationName, annotationMembers, false);
	}

	/**
	 * Builds an annotation from its name and its members.
	 * 
	 * @param annotationName    the name of the annotation
	 * @param annotationMembers the annotation members
	 * @param inherited         denotes whether the annotation can be inherited
	 */
	public Annotation(String annotationName, List<AnnotationMember> annotationMembers, boolean inherited) {
		this.annotationMembers = annotationMembers;
		this.annotationName = annotationName;
		this.inherited = inherited;
	}

	/**
	 * Yields the annotation members of this annotation.
	 * 
	 * @return the annotation members of this annotation
	 */
	public List<AnnotationMember> getAnnotationMembers() {
		return annotationMembers;
	}

	/**
	 * Yields the annotation name of this annotation.
	 * 
	 * @return the annotaiton name of this annotaiton
	 */
	public String getAnnotationName() {
		return annotationName;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((annotationMembers == null) ? 0 : annotationMembers.hashCode());
		result = prime * result + ((annotationName == null) ? 0 : annotationName.hashCode());
		result = prime * result + (inherited ? 1231 : 1237);
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
		Annotation other = (Annotation) obj;
		if (annotationMembers == null) {
			if (other.annotationMembers != null)
				return false;
		} else if (!annotationMembers.equals(other.annotationMembers))
			return false;
		if (annotationName == null) {
			if (other.annotationName != null)
				return false;
		} else if (!annotationName.equals(other.annotationName))
			return false;
		if (inherited != other.inherited)
			return false;
		return true;
	}

	/**
	 * Yields {@code true} if this annotation can be inherited, {@code false}
	 * otherwise.
	 * 
	 * @return {@code true} if this annotation can be inherited, {@code false}
	 *             otherwise
	 */
	public boolean isInherited() {
		return inherited;
	}

	@Override
	public String toString() {
		if (annotationMembers == null)
			return annotationName;

		return annotationName + annotationMembers.toString();
	}
}
