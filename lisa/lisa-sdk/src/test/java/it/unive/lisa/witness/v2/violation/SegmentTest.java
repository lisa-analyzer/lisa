package it.unive.lisa.witness.v2.violation;

import static it.unive.lisa.witness.v2.TestValues.at;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.witness.v2.Format;
import it.unive.lisa.witness.v2.InvalidWitnessException;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Tests of the rules of the segments of a violation sequence.
 * 
 * @author <a href="mailto:giacomo.zanatta@unive.it">Giacomo Zanatta</a>
 */
public class SegmentTest {

	/**
	 * Returns a followed function return waypoint.
	 * 
	 * @param line the line of the waypoint
	 * 
	 * @return the waypoint
	 */
	private static Waypoint follow(
			int line) {
		return Waypoint.functionReturn(at(line), "\\result == 0", Format.EXT_JAVA_EXPRESSION);
	}

	/**
	 * Returns an avoided branching waypoint.
	 * 
	 * @param line the line of the waypoint
	 * 
	 * @return the waypoint
	 */
	private static Waypoint avoid(
			int line) {
		return new Waypoint(WaypointType.BRANCHING, Action.AVOID, at(line), Constraint.withoutFormat("true"), null);
	}

	/**
	 * Returns a cycled branching waypoint.
	 * 
	 * @param line the line of the waypoint
	 * 
	 * @return the waypoint
	 */
	private static Waypoint cycle(
			int line) {
		return new Waypoint(WaypointType.BRANCHING, Action.CYCLE, at(line), Constraint.withoutFormat("true"), null);
	}

	/**
	 * Checks that an empty segment is rejected.
	 */
	@Test
	public void anEmptySegmentIsRejected() {
		assertThrows(InvalidWitnessException.class, () -> new Segment(List.of()));
	}

	/**
	 * Checks that a segment cannot end with avoid.
	 */
	@Test
	public void aSegmentCannotEndWithAvoid() {
		assertThrows(InvalidWitnessException.class, () -> new Segment(List.of(follow(1), avoid(2))));
	}

	/**
	 * Checks that a segment of only avoids is rejected.
	 */
	@Test
	public void aSegmentOfOnlyAvoidsIsRejected() {
		assertThrows(InvalidWitnessException.class, () -> new Segment(List.of(avoid(1))));
	}

	/**
	 * Checks that avoids before the follow are accepted.
	 */
	@Test
	public void avoidsBeforeTheFollowAreAccepted() {
		assertDoesNotThrow(() -> new Segment(List.of(avoid(1), avoid(2), follow(3))));
	}

	/**
	 * Checks that two follows that are not targets are rejected.
	 */
	@Test
	public void twoFollowsThatAreNotTargetsAreRejected() {
		assertThrows(InvalidWitnessException.class, () -> new Segment(List.of(follow(1), follow(2))));
	}

	/**
	 * Checks that several target follows form a multi-follow segment.
	 */
	@Test
	public void severalTargetFollowsFormAMultiFollowSegment() {
		Segment segment = new Segment(List.of(Waypoint.target(at(1)), Waypoint.target(at(2))));
		assertTrue(segment.isFinal());
	}

	/**
	 * Checks that one cycle with avoids is accepted.
	 */
	@Test
	public void oneCycleWithAvoidsIsAccepted() {
		assertDoesNotThrow(() -> new Segment(List.of(avoid(1), cycle(2))));
	}

	/**
	 * Checks that a cycle and a follow cannot share a segment.
	 */
	@Test
	public void aCycleAndAFollowCannotShareASegment() {
		assertThrows(InvalidWitnessException.class, () -> new Segment(List.of(follow(1), cycle(2))));
	}

	/**
	 * Checks that only segments with a target are final.
	 */
	@Test
	public void onlySegmentsWithATargetAreFinal() {
		assertFalse(Segment.of(follow(1)).isFinal());
		assertTrue(Segment.of(Waypoint.target(at(1))).isFinal());
	}
}
