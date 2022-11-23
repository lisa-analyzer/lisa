package it.unive.lisa.analysis.string;

import static org.junit.Assert.*;

import it.unive.lisa.analysis.SemanticException;
import org.junit.Test;

public class SuffixTest {

	@Test
	public void testConstructor() {
		new Suffix();
	}

	@Test
	public void testConstructor1() {
		new Suffix("Hello World!");
	}

	@Test
	public void testLubAux() throws SemanticException {
		Suffix result = new Suffix("Hello World!").lubAux(new Suffix("World!"));

		assertEquals(result.getSuffix(), "World!");
	}

	@Test
	public void testLubAux1() throws SemanticException {
		Suffix result = new Suffix("abcde").lubAux(new Suffix("cde"));

		assertEquals(result.getSuffix(), "cde");
	}

	@Test
	public void testLubAux2() throws SemanticException {
		Suffix result = new Suffix("Hello").lubAux(new Suffix("World"));

		assertTrue(result.isTop());
	}

	@Test
	public void testLessOrEqual() throws SemanticException {
		Suffix suffix = new Suffix("fghabc");

		assertTrue(suffix.lessOrEqualAux(new Suffix("abc")));
	}

	@Test
	public void testLessOrEqual1() throws SemanticException {
		Suffix suffix = new Suffix("fghabc");

		assertFalse(suffix.lessOrEqualAux(new Suffix("abd")));
	}
}
