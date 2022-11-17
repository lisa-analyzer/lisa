package it.unive.lisa.analysis.string;

import static org.junit.Assert.assertEquals;

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

		System.out.println(brick.getReps());

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
}
