package it.unive.lisa.program.annotations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.program.annotations.matcher.BasicAnnotationMatcher;
import java.util.List;
import org.junit.jupiter.api.Test;

public class AnnotationsTest {

	@Test
	public void aFreshInstanceIsEmpty() {
		assertTrue(new Annotations().isEmpty());
	}

	@Test
	public void duplicateAnnotationsPerCompareToAreCollapsed() {
		// backed by a TreeSet, so uniqueness follows compareTo, not equals
		Annotations anns = new Annotations(new Annotation("foo"), new Annotation("foo"));
		assertEquals(1, anns.getAnnotations().size());
	}

	@Test
	public void annotationsDifferingOnlyByInheritedAreKeptDistinct() {
		// regression test: before Annotation#compareTo compared the
		// "inherited" flag, this pair collapsed into a single element
		Annotations anns = new Annotations(new Annotation("foo", false), new Annotation("foo", true));
		assertEquals(2, anns.getAnnotations().size());
	}

	@Test
	public void addAnnotationGrowsTheSet() {
		Annotations anns = new Annotations();
		anns.addAnnotation(new Annotation("foo"));
		assertFalse(anns.isEmpty());
		assertTrue(anns.contains(new BasicAnnotationMatcher("foo")));
	}

	@Test
	public void containsDelegatesToTheMatcher() {
		Annotations anns = new Annotations(new Annotation("foo"), new Annotation("bar"));
		assertTrue(anns.contains(new BasicAnnotationMatcher("bar")));
		assertFalse(anns.contains(new BasicAnnotationMatcher("baz")));
	}

	@Test
	public void getAnnotationsWithAMatcherFiltersTheSet() {
		Annotations anns = new Annotations(new Annotation("foo"), new Annotation("bar"));
		Annotations filtered = anns.getAnnotations(new BasicAnnotationMatcher("foo"));
		assertEquals(1, filtered.getAnnotations().size());
		assertTrue(filtered.contains(new BasicAnnotationMatcher("foo")));
	}

	@Test
	public void equalsAndHashCodeAreBasedOnTheAnnotationSet() {
		Annotations a = new Annotations(new Annotation("foo"));
		Annotations b = new Annotations(List.of(new Annotation("foo")));
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
		assertFalse(a.equals(new Annotations()));
	}

	@Test
	public void toStringListsAllAnnotationsCommaSeparated() {
		Annotations anns = new Annotations(new Annotation("bar"), new Annotation("foo"));
		assertEquals("[bar, foo]", anns.toString());
	}

}
