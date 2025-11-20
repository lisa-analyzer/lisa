package it.unive.lisa.program.annotations.matcher;

import it.unive.lisa.program.annotations.Annotation;

/**
 * Interface for an annotation matcher.
 *
 * @author <a href="mailto:vincenzo.arceri@unipr.it">Vincenzo Arceri</a>
 */
@FunctionalInterface
public interface AnnotationMatcher {

	/**
	 * Returns {@code true} if this matcher matches the input annotation,
	 * {@code false} otherwise.
	 * 
	 * @param annotation the annotation
	 * 
	 * @return {@code true} if this matcher matches the input annotation,
	 *             {@code false} otherwise
	 */
	boolean matches(
			Annotation annotation);

}
