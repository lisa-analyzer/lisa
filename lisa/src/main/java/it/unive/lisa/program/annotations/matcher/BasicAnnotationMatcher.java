package it.unive.lisa.program.annotations.matcher;

import it.unive.lisa.program.annotations.Annotation;

/**
 * An annotation matcher based on the annotation name.
 * 
 * @author <a href="mailto:vincenzo.arceri@unive.it">Vincenzo Arceri</a>
 */
public class BasicAnnotationMatcher implements AnnotationMatcher {

	private final String annotationName;

	/**
	 * Builds the matcher from the name of the annotation.
	 * 
	 * @param annotation the annotation whose name is to be matched
	 */
	public BasicAnnotationMatcher(Annotation annotation) {
		this.annotationName = annotation.getAnnotationName();
	}/**
	 * Builds the matcher from the name of the annotation.
	 * 
	 * @param annotationName the name of the annotation
	 */
	public BasicAnnotationMatcher(String annotationName) {
		this.annotationName = annotationName;
	}

	@Override
	public boolean matches(Annotation annotation) {
		return annotation.getAnnotationName().equals(annotationName);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((annotationName == null) ? 0 : annotationName.hashCode());
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
		BasicAnnotationMatcher other = (BasicAnnotationMatcher) obj;
		if (annotationName == null) {
			if (other.annotationName != null)
				return false;
		} else if (!annotationName.equals(other.annotationName))
			return false;
		return true;
	}
}
