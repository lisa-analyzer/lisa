package it.unive.lisa.analysis.string;

import static org.junit.Assert.*;

import it.unive.lisa.analysis.SemanticException;
import org.junit.Test;

public class PrefixTest {

	@Test
	public void testConstructor() {
		new Prefix();
	}

	@Test
	public void testConstructor1() {
		new Prefix("Hello World!");
	}

	@Test
	public void testLubAux() throws SemanticException {
		Prefix result = new Prefix("abc").lubAux(new Prefix("abcdef"));

		assertEquals(result.getPrefix(), "abc");
	}

	@Test
	public void testLubAux1() throws SemanticException {
		Prefix result = new Prefix("Hello World!").lubAux(new Prefix("Hello, World!"));

		assertEquals(result.getPrefix(), "Hello");
	}

	@Test
	public void testLubAux2() throws SemanticException {
		Prefix result = new Prefix("abc").lubAux(new Prefix("def"));

		assertTrue(result.isTop());
	}

	@Test
	public void testLessOrEqualsAux() throws SemanticException {
		Prefix result = new Prefix("abc");

		assertFalse(result.lessOrEqualAux(new Prefix("abcde")));
	}

	@Test
	public void testLessOrEqualsAux1() throws SemanticException {
		Prefix result = new Prefix("abcde");

		assertFalse(result.lessOrEqualAux(new Prefix("abd")));
	}

	@Test
	public void testLessOrEqualsAux2() throws SemanticException {
		Prefix result = new Prefix("abde");

		assertTrue(result.lessOrEqualAux(new Prefix("abd")));
	}
}
