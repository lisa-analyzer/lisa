package it.unive.lisa.program.annotations.values;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

public class AnnotationValuesTest {

	private static <T extends AnnotationValue> void checkEqualsHashCodeAndToString(
			Supplier<T> makeA,
			Supplier<T> makeB,
			Supplier<T> makeDifferent,
			String expectedToString) {
		T a = makeA.get();
		T b = makeB.get();
		T different = makeDifferent.get();
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
		assertFalse(a.equals(different));
		assertEquals(expectedToString, a.toString());
	}

	@Test
	public void boolAnnotationValue() {
		checkEqualsHashCodeAndToString(() -> new BoolAnnotationValue(true), () -> new BoolAnnotationValue(true),
				() -> new BoolAnnotationValue(false), "true");
		assertTrue(new BoolAnnotationValue(false).compareTo(new BoolAnnotationValue(true)) < 0);
	}

	@Test
	public void byteAnnotationValue() {
		checkEqualsHashCodeAndToString(() -> new ByteAnnotationValue((byte) 1), () -> new ByteAnnotationValue((byte) 1),
				() -> new ByteAnnotationValue((byte) 2), "1");
		assertTrue(new ByteAnnotationValue((byte) 1).compareTo(new ByteAnnotationValue((byte) 2)) < 0);
	}

	@Test
	public void charAnnotationValue() {
		checkEqualsHashCodeAndToString(() -> new CharAnnotationValue('a'), () -> new CharAnnotationValue('a'),
				() -> new CharAnnotationValue('b'), "a");
		assertTrue(new CharAnnotationValue('a').compareTo(new CharAnnotationValue('b')) < 0);
	}

	@Test
	public void doubleAnnotationValue() {
		checkEqualsHashCodeAndToString(() -> new DoubleAnnotationValue(1.5), () -> new DoubleAnnotationValue(1.5),
				() -> new DoubleAnnotationValue(2.5), "1.5");
		assertTrue(new DoubleAnnotationValue(1.5).compareTo(new DoubleAnnotationValue(2.5)) < 0);
	}

	@Test
	public void floatAnnotationValue() {
		checkEqualsHashCodeAndToString(() -> new FloatAnnotationValue(1.5f), () -> new FloatAnnotationValue(1.5f),
				() -> new FloatAnnotationValue(2.5f), "1.5");
		assertTrue(new FloatAnnotationValue(1.5f).compareTo(new FloatAnnotationValue(2.5f)) < 0);
	}

	@Test
	public void intAnnotationValue() {
		checkEqualsHashCodeAndToString(() -> new IntAnnotationValue(1), () -> new IntAnnotationValue(1),
				() -> new IntAnnotationValue(2), "1");
		assertTrue(new IntAnnotationValue(1).compareTo(new IntAnnotationValue(2)) < 0);
	}

	@Test
	public void longAnnotationValue() {
		checkEqualsHashCodeAndToString(() -> new LongAnnotationValue(1L), () -> new LongAnnotationValue(1L),
				() -> new LongAnnotationValue(2L), "1");
		assertTrue(new LongAnnotationValue(1L).compareTo(new LongAnnotationValue(2L)) < 0);
	}

	@Test
	public void shortAnnotationValue() {
		checkEqualsHashCodeAndToString(() -> new ShortAnnotationValue((short) 1),
				() -> new ShortAnnotationValue((short) 1),
				() -> new ShortAnnotationValue((short) 2), "1");
		assertTrue(new ShortAnnotationValue((short) 1).compareTo(new ShortAnnotationValue((short) 2)) < 0);
	}

	@Test
	public void stringAnnotationValue() {
		checkEqualsHashCodeAndToString(() -> new StringAnnotationValue("a"), () -> new StringAnnotationValue("a"),
				() -> new StringAnnotationValue("b"), "a");
		assertTrue(new StringAnnotationValue("a").compareTo(new StringAnnotationValue("b")) < 0);
	}

	@Test
	public void compilationUnitAnnotationValue() {
		checkEqualsHashCodeAndToString(() -> new CompilationUnitAnnotationValue("Foo"),
				() -> new CompilationUnitAnnotationValue("Foo"), () -> new CompilationUnitAnnotationValue("Bar"),
				"Foo");
		assertTrue(
				new CompilationUnitAnnotationValue("Bar").compareTo(new CompilationUnitAnnotationValue("Foo")) < 0);
	}

	@Test
	public void enumAnnotationValueConsidersBothNameAndField() {
		checkEqualsHashCodeAndToString(() -> new EnumAnnotationValue("E", "A"), () -> new EnumAnnotationValue("E", "A"),
				() -> new EnumAnnotationValue("E", "B"), "E.A");
		assertFalse(new EnumAnnotationValue("E", "A").equals(new EnumAnnotationValue("F", "A")));
		assertTrue(new EnumAnnotationValue("E", "A").compareTo(new EnumAnnotationValue("E", "B")) < 0);
		assertTrue(new EnumAnnotationValue("E", "A").compareTo(new EnumAnnotationValue("F", "A")) < 0);
	}

	@Test
	public void differentConcreteTypesCompareByClassName() {
		AnnotationValue i = new IntAnnotationValue(1);
		AnnotationValue s = new StringAnnotationValue("a");
		assertEquals(i.getClass().getName().compareTo(s.getClass().getName()) < 0, i.compareTo(s) < 0);
		assertFalse(i.equals(s));
	}

	@Test
	public void arrayAnnotationValueWrapsBasicValuesAndComparesElementwise() {
		ArrayAnnotationValue a = new ArrayAnnotationValue(new BasicAnnotationValue[] {
				new IntAnnotationValue(1), new IntAnnotationValue(2) });
		ArrayAnnotationValue b = new ArrayAnnotationValue(new BasicAnnotationValue[] {
				new IntAnnotationValue(1), new IntAnnotationValue(2) });
		ArrayAnnotationValue shorter = new ArrayAnnotationValue(new BasicAnnotationValue[] {
				new IntAnnotationValue(1) });
		ArrayAnnotationValue different = new ArrayAnnotationValue(new BasicAnnotationValue[] {
				new IntAnnotationValue(1), new IntAnnotationValue(3) });

		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
		assertFalse(a.equals(different));
		assertEquals("[1, 2]", a.toString());
		assertEquals(0, a.compareTo(b));
		assertTrue(shorter.compareTo(a) < 0);
		assertTrue(a.compareTo(different) < 0);
		assertEquals(List.of(new IntAnnotationValue(1), new IntAnnotationValue(2)), List.of(a.getArray()));
	}

}
