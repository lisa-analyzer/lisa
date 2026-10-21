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
	public void eachLanguageHasAnExpressionAndAnExtendedFormat() {
		assertEquals(Format.JAVA_EXPRESSION, Language.JAVA.getExpressionFormat());
		assertEquals(Format.EXT_JAVA_EXPRESSION, Language.JAVA.getExtendedExpressionFormat());
		assertEquals(Format.C_EXPRESSION, Language.C.getExpressionFormat());
		assertEquals(Format.EXT_C_EXPRESSION, Language.C.getExtendedExpressionFormat());
		assertEquals(Format.PYTHON_EXPRESSION, Language.PYTHON.getExpressionFormat());
		assertEquals(Format.EXT_PYTHON_EXPRESSION, Language.PYTHON.getExtendedExpressionFormat());
	}

	@Test
	public void anyBackslashOutsideLiteralsNeedsTheExtendedFormat() {
		assertEquals(Format.EXT_C_EXPRESSION, Language.C.getFormatFor("\\valid(p)"));
		assertEquals(Format.EXT_C_EXPRESSION, Language.C.getFormatFor("\\resultx == 0"));
		assertEquals(Format.EXT_JAVA_EXPRESSION,
				Language.JAVA.getFormatFor("s.equals(\"a\\\"b\") && \\old(x) < x"));
		assertEquals(Format.C_EXPRESSION, Language.C.getFormatFor("c == '\\n'"));
		assertEquals(Format.JAVA_EXPRESSION, Language.JAVA.getFormatFor("s.equals(\"\\\\result\")"));
		assertEquals(Format.JAVA_EXPRESSION, Language.JAVA.getFormatFor("x > 0", null));
		assertEquals(Format.EXT_JAVA_EXPRESSION, Language.JAVA.getFormatFor("\\result > x"));
		assertEquals(Format.EXT_C_EXPRESSION, Language.C.getFormatFor("\\at(i, AnyPrev) > i"));
		assertEquals(Format.EXT_C_EXPRESSION, Language.C.getFormatFor("1", "\\return == 0"));
		assertEquals(Format.EXT_PYTHON_EXPRESSION, Language.PYTHON.getFormatFor("\\old(x) < x"));
	}

	@Test
	public void aLongLiteralIsHandledInLinearTime() {
		String literal = "s.equals(\"" + "\\n".repeat(500_000) + "\")";
		assertEquals(Format.JAVA_EXPRESSION, Language.JAVA.getFormatFor(literal));
		assertEquals(Format.EXT_JAVA_EXPRESSION, Language.JAVA.getFormatFor(literal + " && \\result > 0"));
		assertEquals(Format.JAVA_EXPRESSION, Language.JAVA.getFormatFor("s.equals(\"" + "a".repeat(1_000_000)));
	}
}
