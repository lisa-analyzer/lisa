package it.unive.lisa.program.annotations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.program.annotations.values.IntAnnotationValue;
import it.unive.lisa.program.annotations.values.StringAnnotationValue;
import org.junit.jupiter.api.Test;

public class AnnotationMemberTest {

	@Test
	public void equalsAndHashCodeConsiderIdAndValue() {
		AnnotationMember a = new AnnotationMember("f", new IntAnnotationValue(1));
		AnnotationMember b = new AnnotationMember("f", new IntAnnotationValue(1));
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
		assertFalse(a.equals(new AnnotationMember("g", new IntAnnotationValue(1))));
		assertFalse(a.equals(new AnnotationMember("f", new IntAnnotationValue(2))));
	}

	@Test
	public void compareToOrdersByIdThenByValue() {
		AnnotationMember a = new AnnotationMember("a", new IntAnnotationValue(9));
		AnnotationMember b = new AnnotationMember("b", new IntAnnotationValue(1));
		assertTrue(a.compareTo(b) < 0);

		AnnotationMember low = new AnnotationMember("f", new IntAnnotationValue(1));
		AnnotationMember high = new AnnotationMember("f", new IntAnnotationValue(2));
		assertTrue(low.compareTo(high) < 0);
	}

	@Test
	public void toStringJoinsIdAndValueWithEquals() {
		AnnotationMember m = new AnnotationMember("f", new StringAnnotationValue("v"));
		assertEquals("f = v", m.toString());
	}

}
