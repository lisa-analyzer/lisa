package it.unive.lisa.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class StringUtilitiesTest {

	@Test
	public void testOrdinalSuffixes() {
		assertEquals("1st", StringUtilities.ordinal(1));
		assertEquals("2nd", StringUtilities.ordinal(2));
		assertEquals("3rd", StringUtilities.ordinal(3));
		assertEquals("4th", StringUtilities.ordinal(4));
		assertEquals("0th", StringUtilities.ordinal(0));
	}

	@Test
	public void testOrdinalTeensAreAlwaysTh() {
		assertEquals("11th", StringUtilities.ordinal(11));
		assertEquals("12th", StringUtilities.ordinal(12));
		assertEquals("13th", StringUtilities.ordinal(13));
	}

	@Test
	public void testOrdinalRepeatsForEveryHundred() {
		assertEquals("21st", StringUtilities.ordinal(21));
		assertEquals("22nd", StringUtilities.ordinal(22));
		assertEquals("23rd", StringUtilities.ordinal(23));
		assertEquals("101st", StringUtilities.ordinal(101));
		assertEquals("111th", StringUtilities.ordinal(111));
		assertEquals("112th", StringUtilities.ordinal(112));
		assertEquals("113th", StringUtilities.ordinal(113));
	}

	@Test
	public void testIndentPrependsToEveryLine() {
		assertEquals("  foo\n  bar", StringUtilities.indent("foo\nbar", " ", 2));
		assertEquals("foo\nbar", StringUtilities.indent("foo\nbar", " ", 0));
	}

	@Test
	public void testFlattenRemovesNewlinesAndTabs() {
		assertEquals("foobar", StringUtilities.flatten("foo\n\tbar"));
		assertEquals("nochange", StringUtilities.flatten("nochange"));
	}

	@Test
	public void testGreatestCommonPrefix() {
		assertEquals("fooba", StringUtilities.gcp("foobar", "foobaz"));
		assertEquals("", StringUtilities.gcp("abc", "xyz"));
		assertEquals("abc", StringUtilities.gcp("abc", "abc"));
		assertEquals("", StringUtilities.gcp("", "abc"));
		assertEquals("ab", StringUtilities.gcp("ab", "abcdef"));
	}

}
