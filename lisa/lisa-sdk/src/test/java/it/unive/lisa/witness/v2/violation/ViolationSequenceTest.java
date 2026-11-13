package it.unive.lisa.witness.v2.violation;

import static it.unive.lisa.witness.v2.TestValues.at;
import static it.unive.lisa.witness.v2.TestValues.metadata;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import it.unive.lisa.witness.v2.Format;
import it.unive.lisa.witness.v2.InvalidWitnessException;
import it.unive.lisa.witness.v2.Language;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Tests of the rules of a violation sequence.
 * 
 * @author <a href="mailto:giacomo.zanatta@unive.it">Giacomo Zanatta</a>
 */
public class ViolationSequenceTest {

	/**
	 * A segment made of a followed function return waypoint.
	 */
	private static final Segment VALUE = Segment
			.of(Waypoint.functionReturn(at(1), "\\result == 0", Format.EXT_JAVA_EXPRESSION));

	/**
	 * A segment made of a target waypoint.
	 */
	private static final Segment TARGET = Segment.of(Waypoint.target(at(2)));

	/**
	 * A segment made of a cycled branching waypoint.
	 */
	private static final Segment CYCLE = Segment
			.of(new Waypoint(WaypointType.BRANCHING, Action.CYCLE, at(3), Constraint.withoutFormat("true"), null));

	/**
	 * Checks that an empty sequence is rejected.
	 */
	@Test
	public void anEmptySequenceIsRejected() {
		assertThrows(InvalidWitnessException.class,
				() -> new ViolationSequence(metadata(Language.JAVA), List.of()));
	}

	/**
	 * Checks that the target segment must be the last one.
	 */
	@Test
	public void theTargetSegmentMustBeTheLastOne() {
		assertThrows(InvalidWitnessException.class,
				() -> new ViolationSequence(metadata(Language.JAVA), List.of(TARGET, VALUE)));
	}

	/**
	 * Checks that a sequence ending with the target is accepted.
	 */
	@Test
	public void aSequenceEndingWithTheTargetIsAccepted() {
		assertDoesNotThrow(() -> new ViolationSequence(metadata(Language.JAVA), List.of(VALUE, TARGET)));
	}

	/**
	 * Checks that a sequence without a target and without cycle segments is
	 * rejected.
	 */
	@Test
	public void aSequenceWithoutTargetOrCycleIsRejected() {
		assertThrows(InvalidWitnessException.class,
				() -> new ViolationSequence(metadata(Language.JAVA), List.of(VALUE)));
	}

	/**
	 * Checks that a stem of normal segments followed by cycle segments is
	 * accepted.
	 */
	@Test
	public void aStemFollowedByCyclesIsAccepted() {
		assertDoesNotThrow(() -> new ViolationSequence(metadata(Language.JAVA), List.of(VALUE, CYCLE, CYCLE)));
	}

	/**
	 * Checks that a cycle segment before a normal segment is rejected.
	 */
	@Test
	public void aCycleBeforeANormalSegmentIsRejected() {
		assertThrows(InvalidWitnessException.class,
				() -> new ViolationSequence(metadata(Language.JAVA), List.of(CYCLE, VALUE, CYCLE)));
	}

	/**
	 * Checks that a sequence cannot have both a target and cycle segments.
	 */
	@Test
	public void aTargetAndCyclesCannotBeTogether() {
		assertThrows(InvalidWitnessException.class,
				() -> new ViolationSequence(metadata(Language.JAVA), List.of(CYCLE, TARGET)));
	}
}
