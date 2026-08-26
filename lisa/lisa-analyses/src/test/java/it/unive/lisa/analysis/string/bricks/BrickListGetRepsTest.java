package it.unive.lisa.analysis.string.bricks;

import static org.junit.jupiter.api.Assertions.assertEquals;

import it.unive.lisa.analysis.string.Bricks;
import it.unive.lisa.analysis.string.Bricks.Brick;
import it.unive.lisa.analysis.string.Bricks.BrickList;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link BrickList#getReps()}, which must yield the set of strings
 * obtained by concatenating, in order, one representative from each brick's own
 * {@link Brick#getReps()}. The expected results below are derived directly from
 * this definition.
 */
public class BrickListGetRepsTest {

	private final Bricks domain = new Bricks();

	private Set<String> set(
			String... strings) {
		Set<String> s = new TreeSet<>();
		for (String str : strings)
			s.add(str);
		return s;
	}

	private BrickList list(
			Brick... bricks) {
		List<Brick> l = new ArrayList<>();
		for (Brick b : bricks)
			l.add(b);
		return domain.new BrickList(l);
	}

	@Test
	public void singleBrickDelegatesToItsOwnReps() {
		Brick b = domain.new Brick(1, 1, set("a", "b"));
		assertEquals(set("a", "b"), list(b).getReps());
	}

	@Test
	public void twoBricksConcatenateAsCartesianProduct() {
		Brick b1 = domain.new Brick(1, 1, set("a", "b"));
		Brick b2 = domain.new Brick(1, 1, set("x", "y"));
		assertEquals(set("ax", "ay", "bx", "by"), list(b1, b2).getReps());
	}

	@Test
	public void emptyStringBrickActsAsIdentity() {
		Brick before = domain.new Brick(1, 1, set("a"));
		Brick empty = domain.new Brick(0, 0, set());
		Brick after = domain.new Brick(1, 1, set("b"));
		assertEquals(set("ab"), list(before, empty, after).getReps());
	}

	@Test
	public void impossibleBrickMakesTheWholeListImpossible() {
		Brick before = domain.new Brick(1, 1, set("a"));
		// nothing can be picked from an empty set to satisfy a minimum of 1
		Brick impossible = domain.new Brick(1, 1, set());
		Brick after = domain.new Brick(1, 1, set("b"));
		assertEquals(set(), list(before, impossible, after).getReps());
	}

	@Test
	public void rangedBrickFollowedByFixedBrick() {
		Brick ranged = domain.new Brick(1, 2, set("a"));
		Brick fixed = domain.new Brick(1, 1, set("b"));
		assertEquals(set("ab", "aab"), list(ranged, fixed).getReps());
	}

}
