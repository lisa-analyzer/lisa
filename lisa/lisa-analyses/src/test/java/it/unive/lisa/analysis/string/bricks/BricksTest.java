package it.unive.lisa.analysis.string.bricks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import it.unive.lisa.analysis.SemanticException;
import it.unive.lisa.analysis.string.Bricks;
import it.unive.lisa.analysis.string.Bricks.Brick;
import it.unive.lisa.analysis.string.Bricks.BrickList;
import it.unive.lisa.util.numeric.IntInterval;
import it.unive.lisa.util.numeric.MathNumber;
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

		Bricks domain = new Bricks();
		list.add(domain.new Brick(1, 1, strings));
		list.add(domain.new Brick(2, 3, strings1));
		list.add(domain.new Brick(0, 1, strings1));

		BrickList bricks = domain.new BrickList(list);

		List<Brick> resultList = new ArrayList<>();

		Set<String> resultTreeSet = new TreeSet<>();
		resultTreeSet.add("aaa");
		resultTreeSet.add("aab");
		resultTreeSet.add("aba");
		resultTreeSet.add("abb");

		resultList.add(domain.new Brick(1, 1, resultTreeSet));
		resultList.add(domain.new Brick(0, 2, strings1));

		bricks.normBricks();

		assertEquals(domain.new BrickList(resultList), bricks);
	}

	@Test
	public void normBricksRule1Test() {
		List<Brick> list = new ArrayList<>();

		Set<String> strings = new TreeSet<>();
		strings.add("a");
		strings.add("b");

		Bricks domain = new Bricks();
		list.add(domain.new Brick(1, 1, strings));
		list.add(domain.new Brick(0, 0, new TreeSet<>()));

		BrickList bricks = domain.new BrickList(list);

		List<Brick> resultList = new ArrayList<>();

		resultList.add(domain.new Brick(1, 1, strings));

		bricks.normBricks();

		assertEquals(domain.new BrickList(resultList), bricks);
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

		Bricks domain = new Bricks();
		list.add(domain.new Brick(1, 1, strings));
		list.add(domain.new Brick(1, 1, strings1));

		BrickList bricks = domain.new BrickList(list);
		bricks.normBricks();

		Set<String> resultStrings = new TreeSet<>();

		List<Brick> resultList = new ArrayList<>();

		resultStrings.add("ab");
		resultStrings.add("aef");
		resultStrings.add("cdb");
		resultStrings.add("cdef");

		resultList.add(domain.new Brick(1, 1, resultStrings));

		bricks.normBricks();

		assertEquals(domain.new BrickList(resultList), bricks);
	}

	@Test
	public void normBricksRule3Test() {
		List<Brick> list = new ArrayList<>();

		Set<String> strings = new TreeSet<>();
		strings.add("a");
		strings.add("b");
		strings.add("c");

		Bricks domain = new Bricks();
		list.add(domain.new Brick(2, 2, strings));

		BrickList bricks = domain.new BrickList(list);

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

		resultList.add(domain.new Brick(1, 1, resultStrings));

		bricks.normBricks();

		assertEquals(domain.new BrickList(resultList), bricks);
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

		Bricks domain = new Bricks();
		list.add(domain.new Brick(0, 1, strings));
		list.add(domain.new Brick(0, 2, strings1));

		BrickList bricks = domain.new BrickList(list);

		bricks.normBricks();

		List<Brick> resultList = new ArrayList<>();

		resultList.add(domain.new Brick(0, 3, strings));

		bricks.normBricks();

		assertEquals(domain.new BrickList(resultList), bricks);
	}

	@Test
	public void normBricksRule5Test() {
		List<Brick> list = new ArrayList<>();

		Set<String> strings = new TreeSet<>();
		strings.add("a");

		Bricks domain = new Bricks();
		list.add(domain.new Brick(2, 5, strings));

		BrickList bricks = domain.new BrickList(list);

		bricks.normBricks();

		List<Brick> resultList = new ArrayList<>();

		Set<String> resultStrings = new TreeSet<>();
		resultStrings.add("aa");

		TreeSet<String> resultStrings1 = new TreeSet<>();
		resultStrings1.add("a");

		resultList.add(domain.new Brick(1, 1, resultStrings));

		resultList.add(domain.new Brick(0, 3, resultStrings1));

		bricks.normBricks();

		assertEquals(domain.new BrickList(resultList), bricks);
	}

	@Test
	public void testPadList() {
		Set<String> strings0 = new TreeSet<>();
		strings0.add("a");

		Bricks domain = new Bricks();
		Brick b0 = domain.new Brick(1, 5, strings0);

		Set<String> strings1 = new TreeSet<>();
		strings1.add("b");

		Brick b1 = domain.new Brick(1, 3, strings1);

		Set<String> strings2 = new TreeSet<>();
		strings2.add("c");

		Brick b2 = domain.new Brick(0, 2, strings2);

		Set<String> strings3 = new TreeSet<>();
		strings3.add("d");

		Brick b3 = domain.new Brick(0, 1, strings3);

		Set<String> strings4 = new TreeSet<>();
		strings4.add("e");

		Brick b4 = domain.new Brick(2, 2, strings4);

		Set<String> strings5 = new TreeSet<>();
		strings5.add("f");

		Brick b5 = domain.new Brick(0, 2, strings5);

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

		BrickList bricks1 = domain.new BrickList(bricksList1);
		BrickList bricks2 = domain.new BrickList(bricksList2);

		List<Brick> resultList = new ArrayList<>();

		resultList.add(domain.new Brick(0, 0, new TreeSet<>()));
		resultList.add(b0);
		resultList.add(b1);
		resultList.add(domain.new Brick(0, 0, new TreeSet<>()));
		resultList.add(b2);

		assertEquals(resultList, bricks1.padList(bricks2));
	}

	@Test
	public void testLessOrEqualAux()
			throws SemanticException {
		List<Brick> bricksList = new ArrayList<>();
		List<Brick> bricksList1 = new ArrayList<>();

		Set<String> treeSet = new TreeSet<>();
		treeSet.add("mo");
		treeSet.add("de");

		Bricks domain = new Bricks();
		Brick brick = domain.new Brick(1, 4, treeSet);

		Set<String> treeSet1 = new TreeSet<>();
		treeSet1.add("mo");
		treeSet1.add("de");
		treeSet1.add("re");

		Brick brick1 = domain.new Brick(0, 5, treeSet1);

		Set<String> treeSet2 = new TreeSet<>();
		treeSet2.add("ge");
		treeSet2.add("ze");

		Brick brick2 = domain.new Brick(1, 3, treeSet2);

		Set<String> treeSet3 = new TreeSet<>();
		treeSet3.add("ge");
		treeSet3.add("ze");
		treeSet3.add("le");

		Brick brick3 = domain.new Brick(1, 4, treeSet3);

		bricksList.add(brick);
		bricksList.add(brick2);

		bricksList1.add(brick1);
		bricksList1.add(brick3);

		BrickList bricks = domain.new BrickList(bricksList);
		BrickList bricks1 = domain.new BrickList(bricksList1);

		assertTrue(bricks.lessOrEqualAux(bricks1));
	}

	@Test
	public void testLessOrEqualAux1()
			throws SemanticException {
		List<Brick> bricksList = new ArrayList<>();
		List<Brick> bricksList1 = new ArrayList<>();

		Set<String> treeSet = new TreeSet<>();
		treeSet.add("mo");
		treeSet.add("de");

		Bricks domain = new Bricks();
		Brick brick = domain.new Brick(1, 4, treeSet);

		Set<String> treeSet1 = new TreeSet<>();
		treeSet1.add("mo");
		treeSet1.add("de");
		treeSet1.add("re");

		Brick brick1 = domain.new Brick(0, 5, treeSet1);

		Set<String> treeSet2 = new TreeSet<>();
		treeSet2.add("ge");
		treeSet2.add("ze");

		Brick brick2 = domain.new Brick(2, 3, treeSet2);

		bricksList.add(brick);
		bricksList.add(brick2);

		bricksList1.add(brick1);

		BrickList bricks = domain.new BrickList(bricksList);
		BrickList bricks1 = domain.new BrickList(bricksList1);

		assertFalse(bricks.lessOrEqualAux(bricks1));
	}

	@Test
	public void testLubAux()
			throws SemanticException {
		List<Brick> bricksList = new ArrayList<>();
		List<Brick> bricksList1 = new ArrayList<>();

		Set<String> treeSet = new TreeSet<>();
		treeSet.add("mo");
		treeSet.add("de");

		Bricks domain = new Bricks();
		Brick brick = domain.new Brick(1, 4, treeSet);

		Set<String> treeSet1 = new TreeSet<>();
		treeSet1.add("mo");
		treeSet1.add("de");
		treeSet1.add("re");

		Brick brick1 = domain.new Brick(0, 5, treeSet1);

		Set<String> treeSet2 = new TreeSet<>();
		treeSet2.add("ge");
		treeSet2.add("ze");

		Brick brick2 = domain.new Brick(1, 3, treeSet2);

		Set<String> treeSet3 = new TreeSet<>();
		treeSet3.add("ge");
		treeSet3.add("ze");
		treeSet3.add("le");

		Brick brick3 = domain.new Brick(1, 4, treeSet3);

		bricksList.add(brick);
		bricksList.add(brick2);

		bricksList1.add(brick1);
		bricksList1.add(brick3);

		BrickList bricks = domain.new BrickList(bricksList);
		BrickList bricks1 = domain.new BrickList(bricksList1);

		List<Brick> resultList = new ArrayList<>();

		Set<String> resultStrings = new TreeSet<>();

		resultStrings.add("mo");
		resultStrings.add("de");
		resultStrings.add("re");

		Set<String> resultStrings1 = new TreeSet<>();

		resultStrings1.add("ge");
		resultStrings1.add("ze");
		resultStrings1.add("le");

		resultList.add(domain.new Brick(0, 5, resultStrings));
		resultList.add(domain.new Brick(1, 4, resultStrings1));

		assertEquals(domain.new BrickList(resultList), bricks.lubAux(bricks1));

	}

	@Test
	public void wideningAuxTest()
			throws SemanticException {
		List<Brick> bricksList = new ArrayList<>();
		List<Brick> bricksList1 = new ArrayList<>();

		Set<String> treeSet = new TreeSet<>();
		treeSet.add("a");
		treeSet.add("b");
		treeSet.add("c");
		treeSet.add("d");
		treeSet.add("e");
		treeSet.add("f");
		treeSet.add("g");

		Set<String> treeSet1 = new TreeSet<>();
		treeSet1.add("h");
		treeSet1.add("i");
		treeSet1.add("l");
		treeSet1.add("m");
		treeSet1.add("n");
		treeSet1.add("o");
		treeSet1.add("p");
		treeSet1.add("q");
		treeSet1.add("r");
		treeSet1.add("s");
		treeSet1.add("t");
		treeSet1.add("u");
		treeSet1.add("v");
		treeSet1.add("z");

		Bricks domain = new Bricks();
		bricksList.add(domain.new Brick(1, 1, treeSet));

		bricksList1.add(domain.new Brick(1, 1, treeSet1));

		BrickList bricks = domain.new BrickList(bricksList);

		BrickList bricks1 = domain.new BrickList(bricksList1);

		assertEquals(bricks.top(), bricks.wideningAux(bricks1));
	}

	@Test
	public void wideningAuxTest2()
			throws SemanticException {
		List<Brick> bricksList = new ArrayList<>();
		List<Brick> bricksList1 = new ArrayList<>();

		Set<String> treeSet = new TreeSet<>();
		treeSet.add("a");

		Set<String> treeSet1 = new TreeSet<>();
		treeSet1.add("a");

		Bricks domain = new Bricks();
		bricksList.add(domain.new Brick(1, 23, treeSet));

		bricksList1.add(domain.new Brick(1, 12, treeSet1));

		BrickList bricks = domain.new BrickList(bricksList);

		BrickList bricks1 = domain.new BrickList(bricksList1);

		Set<String> resultSet = new TreeSet<>();

		resultSet.addAll(treeSet);
		resultSet.addAll(treeSet1);

		List<Brick> resultList = new ArrayList<>();

		IntInterval interval = new IntInterval(MathNumber.ZERO, MathNumber.PLUS_INFINITY);
		resultSet.add("a");
		resultList.add(domain.new Brick(interval, resultSet));

		assertEquals(domain.new BrickList(resultList), bricks.wideningAux(bricks1));
	}

	@Test
	public void wideningAuxTest3()
			throws SemanticException {
		List<Brick> bricksList = new ArrayList<>();
		List<Brick> bricksList1 = new ArrayList<>();

		Set<String> treeSet = new TreeSet<>();
		treeSet.add("a");

		Set<String> treeSet1 = new TreeSet<>();
		treeSet1.add("a");

		Bricks domain = new Bricks();
		bricksList.add(domain.new Brick(1, 6, treeSet));

		bricksList1.add(domain.new Brick(1, 5, treeSet1));

		BrickList bricks = domain.new BrickList(bricksList);

		BrickList bricks1 = domain.new BrickList(bricksList1);

		Set<String> resultSet = new TreeSet<>();

		resultSet.addAll(treeSet);
		resultSet.addAll(treeSet1);

		List<Brick> resultList = new ArrayList<>();

		resultList.add(domain.new Brick(1, 6, resultSet));

		assertEquals(domain.new BrickList(resultList), bricks.wideningAux(bricks1));
	}

}
