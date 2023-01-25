package it.unive.lisa.analysis.string;

import static org.junit.Assert.*;

import it.unive.lisa.analysis.SemanticException;
import java.util.HashSet;
import java.util.Set;
import org.junit.Test;

public class BrickTest {
	@Test
	public void testGetReps() {
		Set<String> hashSet = new HashSet<>();
		hashSet.add("mo");
		hashSet.add("de");

		Brick brick = new Brick(1, 2, hashSet);

		Set<String> result = new HashSet<>();
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
		Set<String> hashSet = new HashSet<>();
		hashSet.add("abc");

		Brick brick = new Brick(1, 1, hashSet);

		Set<String> result = new HashSet<>();
		result.add("abc");

		assertEquals(brick.getReps(), result);
	}

	@Test
	public void testGetReps2() {
		Set<String> hashSet = new HashSet<>();
		hashSet.add("mo");
		hashSet.add("de");
		hashSet.add("re");

		Brick brick = new Brick(1, 2, hashSet);

		Set<String> result = new HashSet<>();
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
		Set<String> hashSet = new HashSet<>();
		hashSet.add("mo");
		hashSet.add("de");
		hashSet.add("re");
		hashSet.add("ve");

		Brick brick = new Brick(1, 4, hashSet);

		assertTrue(brick.getReps().contains("derevemo"));
	}

	@Test
	public void testGetReps4() {
		Set<String> hashSet = new HashSet<>();

		hashSet.add("straw");

		Brick brick = new Brick(0, 1, hashSet);

		Set<String> result = new HashSet<>();

		result.add("");
		result.add("straw");

		assertEquals(brick.getReps(), result);
	}

	@Test
	public void testGetReps5() {
		Set<String> set = new HashSet<>();

		set.add("a");
		set.add("b");

		Brick brick = new Brick(2, 2, set);

		Set<String> result = new HashSet<>();

		result.add("aa");
		result.add("bb");
		result.add("ab");
		result.add("ba");

		assertEquals(brick.getReps(), result);
	}

	@Test
	public void testRepresentation() {
		Set<String> hashSet = new HashSet<>();
		hashSet.add("mo");
		hashSet.add("de");
		hashSet.add("re");

		Brick brick = new Brick(1, 2, hashSet);

		assertEquals(brick.representation().toString(), "[ (min: 1, max: 2), strings: (de, mo, re) ]");
	}

	@Test
	public void testLessOrEqualsAux() throws SemanticException {
		Set<String> hashSet = new HashSet<>();
		hashSet.add("mo");
		hashSet.add("de");

		Brick brick = new Brick(1, 2, hashSet);

		Set<String> hashSet1 = new HashSet<>();
		hashSet1.add("mo");
		hashSet1.add("de");
		hashSet1.add("re");

		Brick brick1 = new Brick(0, 3, hashSet1);

		assertTrue(brick.lessOrEqualAux(brick1));
	}

	@Test
	public void testLessOrEqualsAux1() throws SemanticException {
		Set<String> hashSet = new HashSet<>();
		hashSet.add("mo");
		hashSet.add("de");
		hashSet.add("re");

		Brick brick = new Brick(1, 2, hashSet);

		Set<String> hashSet1 = new HashSet<>();
		hashSet1.add("mo");
		hashSet1.add("de");

		Brick brick1 = new Brick(0, 3, hashSet1);

		assertFalse(brick.lessOrEqualAux(brick1));
	}

	@Test
	public void testLessOrEqualsAux2() throws SemanticException {
		Set<String> hashSet = new HashSet<>();
		hashSet.add("mo");
		hashSet.add("de");

		Brick brick = new Brick(0, 2, hashSet);

		Set<String> hashSet1 = new HashSet<>();
		hashSet1.add("mo");
		hashSet1.add("de");
		hashSet1.add("re");

		Brick brick1 = new Brick(1, 3, hashSet1);

		assertFalse(brick.lessOrEqualAux(brick1));
	}

	@Test
	public void testLessOrEqualsAux3() throws SemanticException {
		Set<String> hashSet = new HashSet<>();
		hashSet.add("mo");
		hashSet.add("de");

		Brick brick = new Brick(1, 4, hashSet);

		Set<String> hashSet1 = new HashSet<>();
		hashSet1.add("mo");
		hashSet1.add("de");
		hashSet1.add("re");

		Brick brick1 = new Brick(0, 3, hashSet1);

		assertFalse(brick.lessOrEqualAux(brick1));
	}

	@Test
	public void testLubAux() throws SemanticException {
		Set<String> hashSet = new HashSet<>();
		hashSet.add("mo");
		hashSet.add("de");
		hashSet.add("re");

		Brick brick = new Brick(1, 4, hashSet);

		Set<String> hashSet1 = new HashSet<>();
		hashSet1.add("le");
		hashSet1.add("de");
		hashSet1.add("lo");

		Brick brick1 = new Brick(0, 3, hashSet1);

		Set<String> resultHashSet = new HashSet<>();

		resultHashSet.add("mo");
		resultHashSet.add("de");
		resultHashSet.add("re");
		resultHashSet.add("le");
		resultHashSet.add("lo");

		Brick resultBrick = new Brick(0, 4, resultHashSet);

		assertEquals(brick.lubAux(brick1), resultBrick);
	}

	@Test
	public void testLubAux1() throws SemanticException {
		Set<String> hashSet = new HashSet<>();
		hashSet.add("a");
		hashSet.add("b");

		Brick brick = new Brick(1, 3, hashSet);

		Set<String> hashSet1 = new HashSet<>();
		hashSet1.add("a");
		hashSet1.add("c");

		Brick brick1 = new Brick(0, 2, hashSet1);

		Set<String> resultHashSet = new HashSet<>();

		resultHashSet.add("a");
		resultHashSet.add("c");
		resultHashSet.add("b");

		Brick resultBrick = new Brick(0, 3, resultHashSet);

		assertEquals(brick.lubAux(brick1), resultBrick);
	}
}
