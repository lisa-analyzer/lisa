package it.unive.lisa.lattices.string;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.string.Suffix;
import org.junit.Test;

public class StrSuffixTest {

	@Test
	public void testConstructor() {
		new Suffix();
	}

	@Test
	public void testConstructor1() {
		new StrSuffix("Hello World!");
	}

	@Test
	public void testLubAux()
			throws SemanticException {
		StrSuffix result = new StrSuffix("Hello World!").lubAux(new StrSuffix("World!"));

		assertEquals(result.suffix, "World!");
	}

	@Test
	public void testLubAux1()
			throws SemanticException {
		StrSuffix result = new StrSuffix("abcde").lubAux(new StrSuffix("cde"));

		assertEquals(result.suffix, "cde");
	}

	@Test
	public void testLubAux2()
			throws SemanticException {
		StrSuffix result = new StrSuffix("Hello").lubAux(new StrSuffix("World"));

		assertTrue(result.isTop());
	}

	@Test
	public void testLessOrEqual()
			throws SemanticException {
		StrSuffix suffix = new StrSuffix("fghabc");

		assertTrue(suffix.lessOrEqualAux(new StrSuffix("abc")));
	}

	@Test
	public void testLessOrEqual1()
			throws SemanticException {
		StrSuffix suffix = new StrSuffix("fghabc");

		assertFalse(suffix.lessOrEqualAux(new StrSuffix("abd")));
	}

}
