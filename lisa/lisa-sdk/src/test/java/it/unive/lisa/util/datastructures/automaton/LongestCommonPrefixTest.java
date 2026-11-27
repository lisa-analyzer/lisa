package it.unive.lisa.util.datastructures.automaton;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class LongestCommonPrefixTest {

	@Test
	public void singleStringYieldsTheWholeStringAsPrefix() {
		assertEquals("ab", new TestAutomaton("ab").longestCommonPrefix());
	}

	@Test
	public void divergingStringsShareTheCommonLeadingPart() {
		TestAutomaton ab = new TestAutomaton("ab");
		TestAutomaton ac = new TestAutomaton("ac");
		assertEquals("a", ab.union(ac).longestCommonPrefix());
	}

	@Test
	public void disjointFirstCharactersYieldTheEmptyPrefix() {
		TestAutomaton a = new TestAutomaton("a");
		TestAutomaton b = new TestAutomaton("b");
		assertEquals("", a.union(b).longestCommonPrefix());
	}

}
