package it.unive.lisa.util.datastructures.automaton;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class RecognizesExactlyOneStringTest {

	@Test
	public void trueForASingleStringAutomaton() {
		assertTrue(new TestAutomaton("ab").recognizesExactlyOneString());
	}

	@Test
	public void falseWhenMultipleStringsAreAccepted() {
		TestAutomaton a = new TestAutomaton("a");
		TestAutomaton b = new TestAutomaton("b");
		assertFalse(a.union(b).recognizesExactlyOneString());
	}

	@Test
	public void falseWhenTheLanguageIsInfinite() {
		assertFalse(new TestAutomaton("a").star().recognizesExactlyOneString());
	}

}
