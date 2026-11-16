package it.unive.lisa.triage.pdr;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class FramesTest {

	@Test
	public void framesAreMonotone() {
		Frames<String, String> frames = new Frames<>();
		frames.extend();
		frames.extend();
		assertEquals(3, frames.depth());
		assertTrue(frames.add("l", "a", 1));
		assertTrue(frames.add("l", "b", 3));
		assertEquals(Set.of("a", "b"), frames.frame("l", 1));
		assertEquals(Set.of("b"), frames.frame("l", 2));
		assertEquals(Set.of("b"), frames.frame("l", 3));
		assertEquals(Set.of(), frames.frame("other", 1));
	}

	@Test
	public void lemmasKeepTheirHighestLevel() {
		Frames<String, String> frames = new Frames<>();
		frames.extend();
		assertTrue(frames.add("l", "a", 1));
		assertTrue(frames.add("l", "a", 2));
		assertFalse(frames.add("l", "a", 1));
		assertEquals(2, frames.levelOf("l", "a"));
		assertEquals(0, frames.levelOf("l", "b"));
		assertEquals(Set.of(), frames.exactly("l", 1));
		assertEquals(Set.of("a"), frames.exactly("l", 2));
		assertTrue(frames.add("l", "a", Frames.INFINITY));
		assertEquals(Set.of("a"), frames.frame("l", 2));
	}

	@Test
	public void levelsAreChecked() {
		Frames<String, String> frames = new Frames<>();
		assertThrows(IllegalArgumentException.class, () -> frames.add("l", "a", 0));
		assertThrows(IllegalArgumentException.class, () -> frames.add("l", "a", 2));
		frames.add("l", "a", Frames.INFINITY);
	}

	@Test
	public void convergence() {
		Frames<String, String> frames = new Frames<>();
		frames.extend();
		frames.extend();
		frames.add("l1", "a", 1);
		frames.add("l2", "b", 2);
		frames.add("l2", "c", 3);
		// every level below the last has a lemma
		assertEquals(-1, frames.convergedLevel());
		// propagating a to level 2 empties level 1
		frames.add("l1", "a", 2);
		assertEquals(1, frames.convergedLevel());
		assertEquals(Map.of("l1", Set.of("a"), "l2", Set.of("b", "c")), frames.invariant(1));
	}
}
