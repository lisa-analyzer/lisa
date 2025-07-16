package it.unive.lisa.analysis.string;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.string.Prefix.Pref;
import org.junit.Test;

public class PrefixTest {

	@Test
	public void testConstructor() {
		new Pref();
	}

	@Test
	public void testConstructor1() {
		new Pref("Hello World!");
	}

	@Test
	public void testLubAux()
			throws SemanticException {
		Pref result = new Pref("abc").lubAux(new Pref("abcdef"));

		assertEquals(result.getPrefix(), "abc");
	}

	@Test
	public void testLubAux1()
			throws SemanticException {
		Pref result = new Pref("Hello World!").lubAux(new Pref("Hello, World!"));

		assertEquals(result.getPrefix(), "Hello");
	}

	@Test
	public void testLubAux2()
			throws SemanticException {
		Pref result = new Pref("abc").lubAux(new Pref("def"));

		assertTrue(result.isTop());
	}

	@Test
	public void testLessOrEqualsAux()
			throws SemanticException {
		Pref result = new Pref("abc");

		assertFalse(result.lessOrEqualAux(new Pref("abcde")));
	}

	@Test
	public void testLessOrEqualsAux1()
			throws SemanticException {
		Pref result = new Pref("abcde");

		assertFalse(result.lessOrEqualAux(new Pref("abd")));
	}

	@Test
	public void testLessOrEqualsAux2()
			throws SemanticException {
		Pref result = new Pref("abde");

		assertTrue(result.lessOrEqualAux(new Pref("abd")));
	}

}
