package it.unive.lisa.lattices.string;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.analysis.SemanticException;
import org.junit.jupiter.api.Test;

public class StrPrefixTest {

	@Test
	public void testConstructor() {
		new StrPrefix();
	}

	@Test
	public void testConstructor1() {
		new StrPrefix("Hello World!");
	}

	@Test
	public void testLubAux()
			throws SemanticException {
		StrPrefix result = new StrPrefix("abc").lubAux(new StrPrefix("abcdef"));

		assertEquals(result.prefix, "abc");
	}

	@Test
	public void testLubAux1()
			throws SemanticException {
		StrPrefix result = new StrPrefix("Hello World!").lubAux(new StrPrefix("Hello, World!"));

		assertEquals(result.prefix, "Hello");
	}

	@Test
	public void testLubAux2()
			throws SemanticException {
		StrPrefix result = new StrPrefix("abc").lubAux(new StrPrefix("def"));

		assertTrue(result.isTop());
	}

	@Test
	public void testLessOrEqualsAux()
			throws SemanticException {
		StrPrefix result = new StrPrefix("abc");

		assertFalse(result.lessOrEqualAux(new StrPrefix("abcde")));
	}

	@Test
	public void testLessOrEqualsAux1()
			throws SemanticException {
		StrPrefix result = new StrPrefix("abcde");

		assertFalse(result.lessOrEqualAux(new StrPrefix("abd")));
	}

	@Test
	public void testLessOrEqualsAux2()
			throws SemanticException {
		StrPrefix result = new StrPrefix("abde");

		assertTrue(result.lessOrEqualAux(new StrPrefix("abd")));
	}

}
