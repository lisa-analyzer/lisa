package it.unive.lisa.program.annotations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.program.annotations.values.IntAnnotationValue;
import java.util.List;
import org.junit.jupiter.api.Test;

public class AnnotationTest {

	@Test
	public void theShortConstructorsDefaultMembersToEmptyAndInheritedToFalse() {
		Annotation a = new Annotation("foo");
		assertTrue(a.getAnnotationMembers().isEmpty());
		assertFalse(a.isInherited());
	}

	@Test
	public void equalsAndHashCodeConsiderNameMembersAndInherited() {
		AnnotationMember member = new AnnotationMember("f", new IntAnnotationValue(1));
		Annotation a = new Annotation("foo", List.of(member), true);
		Annotation b = new Annotation("foo", List.of(member), true);
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
		assertFalse(a.equals(new Annotation("foo", List.of(member), false)));
		assertFalse(a.equals(new Annotation("bar", List.of(member), true)));
	}

	@Test
	public void compareToIsConsistentWithEqualsOnTheInheritedFlag() {
		// regression test: compareTo used to ignore the "inherited" field
		// entirely once name and members matched, returning 0 (equal order)
		// for two annotations that equals() considers different; since
		// Annotations stores its content in a TreeSet, this made one of the
		// two silently disappear as a "duplicate"
		Annotation notInherited = new Annotation("foo", List.of(), false);
		Annotation inherited = new Annotation("foo", List.of(), true);
		assertFalse(notInherited.equals(inherited));
		assertFalse(notInherited.compareTo(inherited) == 0);
		assertEquals(0, notInherited.compareTo(new Annotation("foo", List.of(), false)));
	}

	@Test
	public void compareToOrdersByNameFirst() {
		Annotation a = new Annotation("a");
		Annotation b = new Annotation("b");
		assertTrue(a.compareTo(b) < 0);
		assertTrue(b.compareTo(a) > 0);
	}

	@Test
	public void toStringOmitsMembersWhenThereAreNone() {
		assertEquals("foo", new Annotation("foo").toString());
	}

	@Test
	public void toStringIncludesMembersWhenPresent() {
		AnnotationMember member = new AnnotationMember("f", new IntAnnotationValue(1));
		Annotation a = new Annotation("foo", List.of(member));
		assertEquals("foo" + List.of(member), a.toString());
	}

}
