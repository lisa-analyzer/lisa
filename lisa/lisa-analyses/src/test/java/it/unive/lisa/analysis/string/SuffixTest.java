package it.unive.lisa.analysis.string;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.string.Suffix.Suff;
import org.junit.Test;

public class SuffixTest {

	@Test
	public void testConstructor() {
		new Suffix();
	}

	@Test
	public void testConstructor1() {
		new Suff("Hello World!");
	}

	@Test
	public void testLubAux()
			throws SemanticException {
		Suff result = new Suff("Hello World!").lubAux(new Suff("World!"));

		assertEquals(result.getSuffix(), "World!");
	}

	@Test
	public void testLubAux1()
			throws SemanticException {
		Suff result = new Suff("abcde").lubAux(new Suff("cde"));

		assertEquals(result.getSuffix(), "cde");
	}

	@Test
	public void testLubAux2()
			throws SemanticException {
		Suff result = new Suff("Hello").lubAux(new Suff("World"));

		assertTrue(result.isTop());
	}

	@Test
	public void testLessOrEqual()
			throws SemanticException {
		Suff suffix = new Suff("fghabc");

		assertTrue(suffix.lessOrEqualAux(new Suff("abc")));
	}

	@Test
	public void testLessOrEqual1()
			throws SemanticException {
		Suff suffix = new Suff("fghabc");

		assertFalse(suffix.lessOrEqualAux(new Suff("abd")));
	}

}
