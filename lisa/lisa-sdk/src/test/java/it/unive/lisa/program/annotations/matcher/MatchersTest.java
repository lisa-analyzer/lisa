package it.unive.lisa.program.annotations.matcher;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import it.unive.lisa.program.annotations.Annotation;
import it.unive.lisa.program.annotations.AnnotationMember;
import it.unive.lisa.program.annotations.Annotations;
import it.unive.lisa.program.annotations.values.IntAnnotationValue;
import java.util.List;
import org.junit.Test;

public class MatchersTest {

	@Test
	public void testBasicMatcher() {
		BasicAnnotationMatcher matcher = new BasicAnnotationMatcher("foo");
		assertFalse(new Annotations().contains(matcher));
		assertTrue(new Annotations(new Annotation("foo")).contains(matcher));
		assertFalse(new Annotations(new Annotation("bar")).contains(matcher));
		assertTrue(new Annotations(new Annotation("bar"), new Annotation("foo")).contains(matcher));
		assertTrue(new Annotations(new Annotation("foo"), new Annotation("bar")).contains(matcher));
		assertTrue(new Annotations(new Annotation("foo"), new Annotation("foo")).contains(matcher));

		Annotation ann = new Annotation("foo", List.of(new AnnotationMember("f", new IntAnnotationValue(5))));
		assertTrue(new Annotations(new Annotation("bar"), ann).contains(matcher));
	}
}
