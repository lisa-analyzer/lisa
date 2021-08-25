package it.unive.lisa.program.annotations;

import it.unive.lisa.program.annotations.matcher.AnnotationMatcher;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;

/**
 * A collection of annotations.
 * 
 * @author <a href="mailto:vincenzo.arceri@unive.it">Vincenzo Arceri</a>
 */
public class Annotations implements Iterable<Annotation> {

	private final Collection<Annotation> annotations;

	/**
	 * Builds an empty list of annotations.
	 */
	public Annotations() {
		this(new ArrayList<>());
	}

	/**
	 * Builds a collection of annotations containing only the given one.
	 * 
	 * @param annotation the annotation
	 */
	public Annotations(Annotation annotation) {
		this(List.of(annotation));
	}

	/**
	 * Builds a collection of annotations from an array of annotations.
	 * 
	 * @param annotations the array of annotations
	 */
	public Annotations(Annotation... annotations) {
		this(List.of(annotations));
	}

	/**
	 * Builds a collection of annotations from a given collection.
	 * 
	 * @param annotations the collection of annotations
	 */
	public Annotations(Collection<Annotation> annotations) {
		this.annotations = annotations;
	}

	/**
	 * Yields the list of annotations.
	 * 
	 * @return the list of annotations
	 */
	public Collection<Annotation> getAnnotations() {
		return annotations;
	}

	@Override
	public Iterator<Annotation> iterator() {
		return annotations.iterator();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((annotations == null) ? 0 : annotations.hashCode());
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
		Annotations other = (Annotations) obj;
		if (annotations == null) {
			if (other.annotations != null)
				return false;
		} else if (!annotations.equals(other.annotations))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return annotations == null ? "[]" : "[" + StringUtils.join(annotations, ", ") + "]";
	}

	/**
	 * Adds an annotations to this annotation collection.
	 * 
	 * @param ann the annotation to be added
	 */
	public void addAnnotation(Annotation ann) {
		annotations.add(ann);
	}

	/**
	 * Returns {@code true} if {@code matcher} matches at least one of this
	 * annotations, {@code false} otherwise.
	 * 
	 * @param m the annotation matcher
	 * 
	 * @return {@code true} if {@code matcher} matches at least one of this
	 *             annotations, {@code false} otherwise
	 */
	public final boolean contains(AnnotationMatcher m) {
		return annotations.stream().anyMatch(m::matches);
	}

	/**
	 * Yields the annotations that are matched by the matcher {@code m}.
	 * 
	 * @param m the annotation matcher
	 * 
	 * @return the annotations that are matched by the matcher {@code m}
	 */
	public final Annotations getAnnotations(AnnotationMatcher m) {
		return new Annotations(annotations.stream().filter(m::matches).collect(Collectors.toList()));
	}

	/**
	 * Yields {@code true} if and only if the set of annotations represented by
	 * this instance is empty.
	 * 
	 * @return whether or not this set of annotations is empty
	 */
	public final boolean isEmpty() {
		return annotations.isEmpty();
	}
}
