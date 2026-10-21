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

	private static final Segment VALUE = Segment
			.of(Waypoint.functionReturn(at(1), "\\result == 0", Format.EXT_JAVA_EXPRESSION));

	private static final Segment TARGET = Segment.of(Waypoint.target(at(2)));

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

}
