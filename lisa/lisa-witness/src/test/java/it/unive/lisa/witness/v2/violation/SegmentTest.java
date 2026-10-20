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

public class SegmentTest {

	private static Waypoint follow(
			int line) {
		return Waypoint.functionReturn(at(line), "\\result == 0", Format.EXT_JAVA_EXPRESSION);
	}

	private static Waypoint avoid(
			int line) {
		return new Waypoint(WaypointType.BRANCHING, Action.AVOID, at(line), Constraint.withoutFormat("true"), null);
	}

	private static Waypoint cycle(
			int line) {
		return new Waypoint(WaypointType.BRANCHING, Action.CYCLE, at(line), Constraint.withoutFormat("true"), null);
	}

	@Test
	public void anEmptySegmentIsRejected() {
		assertThrows(InvalidWitnessException.class, () -> new Segment(List.of()));
	}

	@Test
	public void aSegmentCannotEndWithAvoid() {
		assertThrows(InvalidWitnessException.class, () -> new Segment(List.of(follow(1), avoid(2))));
	}

	@Test
	public void aSegmentOfOnlyAvoidsIsRejected() {
		assertThrows(InvalidWitnessException.class, () -> new Segment(List.of(avoid(1))));
	}

	@Test
	public void avoidsBeforeTheFollowAreAccepted() {
		assertDoesNotThrow(() -> new Segment(List.of(avoid(1), avoid(2), follow(3))));
	}

	@Test
	public void twoFollowsThatAreNotTargetsAreRejected() {
		assertThrows(InvalidWitnessException.class, () -> new Segment(List.of(follow(1), follow(2))));
	}

	@Test
	public void severalTargetFollowsFormAMultiFollowSegment() {
		Segment segment = new Segment(List.of(Waypoint.target(at(1)), Waypoint.target(at(2))));
		assertTrue(segment.isFinal());
	}

	@Test
	public void oneCycleWithAvoidsIsAccepted() {
		assertDoesNotThrow(() -> new Segment(List.of(avoid(1), cycle(2))));
	}

	@Test
	public void aCycleAndAFollowCannotShareASegment() {
		assertThrows(InvalidWitnessException.class, () -> new Segment(List.of(follow(1), cycle(2))));
	}

	@Test
	public void onlySegmentsWithATargetAreFinal() {
		assertFalse(Segment.of(follow(1)).isFinal());
		assertTrue(Segment.of(Waypoint.target(at(1))).isFinal());
	}
}
