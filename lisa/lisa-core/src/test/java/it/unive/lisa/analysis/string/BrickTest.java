package it.unive.lisa.analysis.string;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.HashSet;
import org.junit.Test;

public class BrickTest {

	@Test
	public void testGetReps() {
		Collection<String> hashSet = new HashSet<>();
		hashSet.add("mo");
		hashSet.add("de");

		Brick brick = new Brick(1, 2, hashSet);

		Collection<String> result = new HashSet<>();
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
		Collection<String> hashSet = new HashSet<>();
		hashSet.add("abc");

		Brick brick = new Brick(1, 1, hashSet);

		Collection<String> result = new HashSet<>();
		result.add("abc");

		assertEquals(brick.getReps(), result);
	}

	@Test
	public void testGetReps2() {
		Collection<String> hashSet = new HashSet<>();
		hashSet.add("mo");
		hashSet.add("de");
		hashSet.add("re");

		Brick brick = new Brick(1, 2, hashSet);

		Collection<String> result = new HashSet<>();
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
		Collection<String> hashSet = new HashSet<>();
		hashSet.add("mo");
		hashSet.add("de");
		hashSet.add("re");
		hashSet.add("ve");

		Brick brick = new Brick(1, 4, hashSet);

		assertTrue(brick.getReps().contains("derevemo"));
	}

	@Test
	public void testGetReps4() {
		Collection<String> hashSet = new HashSet<>();

		hashSet.add("straw");

		Brick brick = new Brick(0, 1, hashSet);

		Collection<String> result = new HashSet<>();

		result.add("");
		result.add("straw");

		assertEquals(brick.getReps(), result);
	}
	@Test
	public void testGetReps5() {
		Collection<String> set = new HashSet<>();

		set.add("a");
		set.add("b");

		Brick brick = new Brick(2,2 , set);

		Collection<String> result = new HashSet<>();

		result.add("aa");
		result.add("bb");
		result.add("ab");
		result.add("ab");
		result.add("ba");

		assertEquals(brick.getReps(), result);
	}

	@Test
	public void testToString(){
		Collection<String> hashSet = new HashSet<>();
		hashSet.add("mo");
		hashSet.add("de");
		hashSet.add("re");

		Brick brick = new Brick(1, 2, hashSet);

		assertEquals(brick.toString(), "[ {min: 1}, {max: 2}, {strings: de, mo, re} ]");
	}
}
