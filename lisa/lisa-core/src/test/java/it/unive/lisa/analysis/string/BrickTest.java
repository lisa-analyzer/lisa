package it.unive.lisa.analysis.string;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import org.junit.Test;

public class BrickTest {

	@Test
	public void testGetReps() {
		HashSet<String> hashSet = new HashSet<>();
		hashSet.add("mo");
		hashSet.add("de");

		Brick brick = new Brick(1, 2, hashSet);

		HashSet<String> result = new HashSet<>();
		result.add("mo");
		result.add("de");
		result.add("momo");
		result.add("dede");
		result.add("mode");
		result.add("demo");

		assertEquals(brick.getReps(), result);
	}

	@Test
	public void testGetReps1() {
		HashSet<String> hashSet = new HashSet<>();
		hashSet.add("abc");

		Brick brick = new Brick(1, 1, hashSet);

		HashSet<String> result = new HashSet<>();
		result.add("abc");

		assertEquals(brick.getReps(), result);
	}

	@Test
	public void testGetReps2() {
		HashSet<String> hashSet = new HashSet<>();
		hashSet.add("mo");
		hashSet.add("de");
		hashSet.add("re");

		Brick brick = new Brick(1, 2, hashSet);

		HashSet<String> result = new HashSet<>();
		result.add("mo");
		result.add("de");
		result.add("re");
		result.add("momo");
		result.add("dede");
		result.add("rere");
		result.add("more");
		result.add("remo");
		result.add("dere");
		result.add("rede");
		result.add("mode");
		result.add("demo");

		assertEquals(brick.getReps(), result);
	}

	@Test
	public void testGetReps3() {
		HashSet<String> hashSet = new HashSet<>();
		hashSet.add("mo");
		hashSet.add("de");
		hashSet.add("re");
		hashSet.add("ve");

		Brick brick = new Brick(1, 4, hashSet);

		assertTrue(brick.getReps().contains("derevemo"));
	}
}
