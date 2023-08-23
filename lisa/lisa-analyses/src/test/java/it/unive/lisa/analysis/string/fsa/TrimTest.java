package it.unive.lisa.analysis.string.fsa;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class TrimTest {

	@Test
	public void test01() {
		SimpleAutomaton a = new SimpleAutomaton(" a ");
		assertTrue(a.trim().isEqualTo(new SimpleAutomaton("a")));
	}

	@Test
	public void test02() {
		SimpleAutomaton b = new SimpleAutomaton(" a ").union(new SimpleAutomaton(" b "));
		assertTrue(b.trim().isEqualTo(new SimpleAutomaton("a").union(new SimpleAutomaton("b"))));
	}

	@Test
	public void test03() {
		SimpleAutomaton c = new SimpleAutomaton("a").union(new SimpleAutomaton(" b "));
		assertTrue(c.trim().isEqualTo(new SimpleAutomaton("a").union(new SimpleAutomaton("b"))));
	}
}
