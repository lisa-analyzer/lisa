package it.unive.lisa.analysis.string;

import static org.junit.Assert.*;

import it.unive.lisa.analysis.SemanticException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.junit.Test;

public class BricksTest {

	@Test
	public void normBricksTest() {
		List<Brick> list = new ArrayList<>();

		Set<String> strings = new TreeSet<>();
		strings.add("a");

		Set<String> strings1 = new TreeSet<>();
		strings1.add("a");
		strings1.add("b");

		list.add(new Brick(1, 1, strings));
		list.add(new Brick(2, 3, strings1));
		list.add(new Brick(0, 1, strings1));

		Bricks bricks = new Bricks(list);

		bricks.normBricks();

		List<Brick> resultList = new ArrayList<>();

		Set<String> resultTreeSet = new TreeSet<>();
		resultTreeSet.add("aaa");
		resultTreeSet.add("aab");
		resultTreeSet.add("aba");
		resultTreeSet.add("abb");

		resultList.add(new Brick(1, 1, resultTreeSet));
		resultList.add(new Brick(0, 2, strings1));

		assertEquals(bricks, new Bricks(resultList));
	}

	@Test
	public void normBricksRule1Test() {
		List<Brick> list = new ArrayList<>();

		Set<String> strings = new TreeSet<>();
		strings.add("a");
		strings.add("b");

		list.add(new Brick(1, 1, strings));
		list.add(new Brick(0, 0, new TreeSet<>()));

		Bricks bricks = new Bricks(list);

		list.remove(1);

		assertEquals(bricks, new Bricks(list));
	}

	@Test
	public void normBricksRule2Test() {
		List<Brick> list = new ArrayList<>();

		Set<String> strings = new TreeSet<>();
		strings.add("a");
		strings.add("cd");

		Set<String> strings1 = new TreeSet<>();

		strings1.add("b");
		strings1.add("ef");

		list.add(new Brick(1, 1, strings));
		list.add(new Brick(1, 1, strings1));

		Bricks bricks = new Bricks(list);
		bricks.normBricks();

		Set<String> resultStrings = new TreeSet<>();

		List<Brick> resultList = new ArrayList<>();

		resultStrings.add("ab");
		resultStrings.add("aef");
		resultStrings.add("cdb");
		resultStrings.add("cdef");

		resultList.add(new Brick(1, 1, resultStrings));

		assertEquals(bricks, new Bricks(resultList));
	}

	@Test
	public void normBricksRule3Test() {
		List<Brick> list = new ArrayList<>();

		Set<String> strings = new TreeSet<>();
		strings.add("a");
		strings.add("b");
		strings.add("c");

		list.add(new Brick(2, 2, strings));

		Bricks bricks = new Bricks(list);

		bricks.normBricks();

		Set<String> resultStrings = new TreeSet<>();

		List<Brick> resultList = new ArrayList<>();

		resultStrings.add("aa");
		resultStrings.add("bb");
		resultStrings.add("cc");
		resultStrings.add("ab");
		resultStrings.add("ac");
		resultStrings.add("ba");
		resultStrings.add("bc");
		resultStrings.add("ca");
		resultStrings.add("cb");

		resultList.add(new Brick(1, 1, resultStrings));

		assertEquals(bricks, new Bricks(resultList));
	}

	@Test
	public void normBricksRule4Test() {
		List<Brick> list = new ArrayList<>();

		Set<String> strings = new TreeSet<>();
		strings.add("a");
		strings.add("b");

		Set<String> strings1 = new TreeSet<>();
		strings1.add("a");
		strings1.add("b");

		list.add(new Brick(0, 1, strings));
		list.add(new Brick(0, 2, strings1));

		Bricks bricks = new Bricks(list);

		bricks.normBricks();

		List<Brick> resultList = new ArrayList<>();

		resultList.add(new Brick(0, 3, strings));

		assertEquals(bricks, new Bricks(resultList));
	}

	@Test
	public void normBricksRule5Test() {
		List<Brick> list = new ArrayList<>();

		Set<String> strings = new TreeSet<>();
		strings.add("a");

		list.add(new Brick(2, 5, strings));

		Bricks bricks = new Bricks(list);

		bricks.normBricks();

		List<Brick> resultList = new ArrayList<>();

		Set<String> resultStrings = new TreeSet<>();
		resultStrings.add("aa");

		TreeSet<String> resultStrings1 = new TreeSet<>();
		resultStrings1.add("a");

		resultList.add(new Brick(1, 1, resultStrings));

		resultList.add(new Brick(0, 3, resultStrings1));

		assertEquals(bricks, new Bricks(resultList));
	}

	@Test
	public void testPadList() {
		Set<String> strings0 = new TreeSet<>();
		strings0.add("a");

		Brick b0 = new Brick(2, 5, strings0);

		Set<String> strings1 = new TreeSet<>();
		strings1.add("b");

		Brick b1 = new Brick(1, 3, strings1);

		Set<String> strings2 = new TreeSet<>();
		strings2.add("c");

		Brick b2 = new Brick(0, 2, strings2);

		Set<String> strings3 = new TreeSet<>();
		strings3.add("d");

		Brick b3 = new Brick(0, 1, strings3);

		Set<String> strings4 = new TreeSet<>();
		strings4.add("e");

		Brick b4 = new Brick(2, 2, strings4);

		Set<String> strings5 = new TreeSet<>();
		strings5.add("f");

		Brick b5 = new Brick(0, 2, strings5);

		List<Brick> bricksList1 = new ArrayList<>();
		List<Brick> bricksList2 = new ArrayList<>();

		bricksList1.add(b0);
		bricksList1.add(b1);
		bricksList1.add(b2);

		bricksList2.add(b3);
		bricksList2.add(b0);
		bricksList2.add(b1);
		bricksList2.add(b4);
		bricksList2.add(b5);

		Bricks bricks1 = new Bricks(bricksList1);
		Bricks bricks2 = new Bricks(bricksList2);

		List<Brick> resultList = new ArrayList<>();

		resultList.add(new Brick(0, 0, new TreeSet<>()));
		resultList.add(b0);
		resultList.add(b1);
		resultList.add(new Brick(0, 0, new TreeSet<>()));
		resultList.add(b2);

		assertEquals(bricks1.padList(bricks2), resultList);
	}

	@Test
	public void testLessOrEqualAux() throws SemanticException {
		List<Brick> bricksList = new ArrayList<>();
		List<Brick> bricksList1 = new ArrayList<>();

		Set<String> treeSet = new TreeSet<>();
		treeSet.add("mo");
		treeSet.add("de");

		Brick brick = new Brick(1, 4, treeSet);

		Set<String> treeSet1 = new TreeSet<>();
		treeSet1.add("mo");
		treeSet1.add("de");
		treeSet1.add("re");

		Brick brick1 = new Brick(0, 5, treeSet1);

		Set<String> treeSet2 = new TreeSet<>();
		treeSet2.add("ge");
		treeSet2.add("ze");

		Brick brick2 = new Brick(2, 3, treeSet2);

		Set<String> treeSet3 = new TreeSet<>();
		treeSet3.add("ge");
		treeSet3.add("ze");
		treeSet3.add("le");

		Brick brick3 = new Brick(1, 4, treeSet3);

		bricksList.add(brick);
		bricksList.add(brick2);

		bricksList1.add(brick1);
		bricksList1.add(brick3);

		Bricks bricks = new Bricks(bricksList);
		Bricks bricks1 = new Bricks(bricksList1);

		assertTrue(bricks.lessOrEqualAux(bricks1));

	}

	@Test
	public void testLessOrEqualAux1() throws SemanticException {
		List<Brick> bricksList = new ArrayList<>();
		List<Brick> bricksList1 = new ArrayList<>();

		Set<String> treeSet = new TreeSet<>();
		treeSet.add("mo");
		treeSet.add("de");

		Brick brick = new Brick(1, 4, treeSet);

		Set<String> treeSet1 = new TreeSet<>();
		treeSet1.add("mo");
		treeSet1.add("de");
		treeSet1.add("re");

		Brick brick1 = new Brick(0, 5, treeSet1);

		Set<String> treeSet2 = new TreeSet<>();
		treeSet2.add("ge");
		treeSet2.add("ze");

		Brick brick2 = new Brick(2, 3, treeSet2);

		bricksList.add(brick);
		bricksList.add(brick2);

		bricksList1.add(brick1);

		Bricks bricks = new Bricks(bricksList);
		Bricks bricks1 = new Bricks(bricksList1);

		assertFalse(bricks.lessOrEqualAux(bricks1));
	}

	@Test
	public void testLubAux() throws SemanticException {
		List<Brick> bricksList = new ArrayList<>();
		List<Brick> bricksList1 = new ArrayList<>();

		Set<String> treeSet = new TreeSet<>();
		treeSet.add("mo");
		treeSet.add("de");

		Brick brick = new Brick(1, 4, treeSet);

		Set<String> treeSet1 = new TreeSet<>();
		treeSet1.add("mo");
		treeSet1.add("de");
		treeSet1.add("re");

		Brick brick1 = new Brick(0, 5, treeSet1);

		Set<String> treeSet2 = new TreeSet<>();
		treeSet2.add("ge");
		treeSet2.add("ze");

		Brick brick2 = new Brick(2, 3, treeSet2);

		Set<String> treeSet3 = new TreeSet<>();
		treeSet3.add("ge");
		treeSet3.add("ze");
		treeSet3.add("le");

		Brick brick3 = new Brick(1, 4, treeSet3);

		bricksList.add(brick);
		bricksList.add(brick2);

		bricksList1.add(brick1);
		bricksList1.add(brick3);

		Bricks bricks = new Bricks(bricksList);
		Bricks bricks1 = new Bricks(bricksList1);

		List<Brick> resultList = new ArrayList<>();

		Set<String> resultStrings = new TreeSet<>();

		resultStrings.add("mo");
		resultStrings.add("de");
		resultStrings.add("re");

		Set<String> resultStrings1 = new TreeSet<>();

		resultStrings1.add("ge");
		resultStrings1.add("ze");
		resultStrings1.add("le");

		resultList.add(new Brick(0, 5, resultStrings));
		resultList.add(new Brick(1, 4, resultStrings1));

		assertEquals(bricks.lubAux(bricks1), new Bricks(resultList));

	}
}
