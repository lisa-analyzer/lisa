package it.unive.lisa.witness.v2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class LanguageTest {

	@Test
	@SuppressWarnings("deprecation")
	public void eachLanguageAllowsOnlyItsOwnFormats() {
		assertTrue(Language.JAVA.allows(Format.JAVA_EXPRESSION));
		assertTrue(Language.JAVA.allows(Format.EXT_JAVA_EXPRESSION));
		assertFalse(Language.JAVA.allows(Format.C_EXPRESSION));
		assertFalse(Language.C.allows(Format.PYTHON_EXPRESSION));
		assertTrue(Language.C.allows(Format.ACSL_EXPRESSION));
	}

	@Test
	public void onlyCRequiresADataModelAndSupportsGhostCode() {
		assertTrue(Language.C.requiresDataModel());
		assertTrue(Language.C.supportsGhost());
		assertFalse(Language.JAVA.requiresDataModel());
		assertFalse(Language.PYTHON.supportsGhost());
	}

	@Test
	@SuppressWarnings("deprecation")
	public void extendedFormatsAreTheExtOnes() {
		for (Format format : Format.values())
			assertEquals(format.getName().startsWith("ext_") || format == Format.ACSL_EXPRESSION,
					format.isExtended(), format.getName());
	}

	@Test
	public void namesAreTheOnesOfTheFormat() {
		assertEquals("Java", Language.JAVA.getName());
		assertEquals("ext_java_expression", Format.EXT_JAVA_EXPRESSION.getName());
		assertEquals("invariant_set", EntryType.INVARIANT_SET.getName());
	}

	@Test
	public void eachLanguageHasAnExpressionAndAnExtendedFormat() {
		assertEquals(Format.JAVA_EXPRESSION, Language.JAVA.getExpressionFormat());
		assertEquals(Format.EXT_JAVA_EXPRESSION, Language.JAVA.getExtendedExpressionFormat());
		assertEquals(Format.C_EXPRESSION, Language.C.getExpressionFormat());
		assertEquals(Format.EXT_C_EXPRESSION, Language.C.getExtendedExpressionFormat());
		assertEquals(Format.PYTHON_EXPRESSION, Language.PYTHON.getExpressionFormat());
		assertEquals(Format.EXT_PYTHON_EXPRESSION, Language.PYTHON.getExtendedExpressionFormat());
	}
}
