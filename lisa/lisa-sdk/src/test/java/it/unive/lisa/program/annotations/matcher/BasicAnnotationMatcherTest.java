package it.unive.lisa.program.annotations.matcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import it.unive.lisa.program.annotations.Annotation;
import org.junit.jupiter.api.Test;

public class BasicAnnotationMatcherTest {

	@Test
	public void theAnnotationBasedConstructorUsesTheAnnotationsName() {
		BasicAnnotationMatcher fromAnnotation = new BasicAnnotationMatcher(new Annotation("foo"));
		BasicAnnotationMatcher fromName = new BasicAnnotationMatcher("foo");
		assertEquals(fromAnnotation, fromName);
		assertEquals(fromAnnotation.hashCode(), fromName.hashCode());
	}

	@Test
	public void equalsAndHashCodeAreBasedOnTheAnnotationName() {
		assertFalse(new BasicAnnotationMatcher("foo").equals(new BasicAnnotationMatcher("bar")));
	}

}
