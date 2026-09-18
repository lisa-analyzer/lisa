package it.unive.lisa.analysis.string.bricks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import it.unive.lisa.analysis.string.Bricks;
import it.unive.lisa.analysis.string.Bricks.Brick;
import it.unive.lisa.util.numeric.IntInterval;
import it.unive.lisa.util.numeric.MathNumber;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;

public class GetRepsTest {

	private final Bricks domain = new Bricks();

	private Set<String> set(
			String... strings) {
		Set<String> s = new TreeSet<>();
		for (String str : strings)
			s.add(str);
		return s;
	}

	private Set<String> set() {
		return new TreeSet<>();
	}

	@Test
	public void zeroRepetitionsOfAnyBrickIsTheEmptyString() {
		// concatenating exactly zero elements always yields "", regardless
		// of what the (possibly empty) string set contains
		Brick brick = domain.new Brick(0, 0, set());
		assertEquals(set(""), brick.getReps());
	}

	@Test
	public void zeroRepetitionsOfNonEmptySetIsAlsoTheEmptyString() {
		Brick brick = domain.new Brick(0, 0, set("a", "b"));
		assertEquals(set(""), brick.getReps());
	}

	@Test
	public void atLeastOneRepetitionOfAnEmptySetIsImpossible() {
		// there is nothing to pick from an empty set, so no string can be
		// built by concatenating one (or more) elements of it
		Brick brick = domain.new Brick(1, 1, set());
		assertEquals(set(), brick.getReps());
	}

	@Test
	public void singleStringExactRepetition() {
		Brick brick = domain.new Brick(1, 1, set("a"));
		assertEquals(set("a"), brick.getReps());
	}

	@Test
	public void singleStringExactRepetitionGreaterThanOne() {
		Brick brick = domain.new Brick(3, 3, set("x"));
		assertEquals(set("xxx"), brick.getReps());
	}

	@Test
	public void singleStringRangeIncludesEveryLengthInBetween() {
		// [a]_{2,4} must contain "aa", "aaa" and "aaaa": every repetition
		// count between min and max, not just the two extremes
		Brick brick = domain.new Brick(2, 4, set("a"));
		assertEquals(set("aa", "aaa", "aaaa"), brick.getReps());
	}

	@Test
	public void multipleStringsExactRepetition() {
		Brick brick = domain.new Brick(1, 1, set("a", "b"));
		assertEquals(set("a", "b"), brick.getReps());
	}

	@Test
	public void multipleStringsTwoRepetitionsIsFullCartesianProduct() {
		Brick brick = domain.new Brick(2, 2, set("a", "b"));
		assertEquals(set("aa", "ab", "ba", "bb"), brick.getReps());
	}

	@Test
	public void multipleStringsRangeUnionsEveryRepetitionCount() {
		Brick brick = domain.new Brick(1, 2, set("a", "b"));
		assertEquals(set("a", "b", "aa", "ab", "ba", "bb"), brick.getReps());
	}

	@Test
	public void multipleStringsRangeStartingAtZero() {
		Brick brick = domain.new Brick(0, 1, set("a", "b"));
		assertEquals(set("", "a", "b"), brick.getReps());
	}

	@Test
	public void nonFiniteBrickThrows() {
		Brick brick = domain.new Brick(new IntInterval(MathNumber.ONE, MathNumber.PLUS_INFINITY), set("a"));
		assertThrows(IllegalStateException.class, brick::getReps);
	}

}
