package it.unive.lisa.witness.v2;

import static it.unive.lisa.witness.v2.TestValues.at;
import static it.unive.lisa.witness.v2.TestValues.metadata;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.witness.v2.correctness.FunctionContract;
import it.unive.lisa.witness.v2.correctness.Invariant;
import it.unive.lisa.witness.v2.correctness.InvariantSet;
import it.unive.lisa.witness.v2.correctness.InvariantType;
import it.unive.lisa.witness.v2.correctness.Label;
import it.unive.lisa.witness.v2.violation.Segment;
import it.unive.lisa.witness.v2.violation.ViolationSequence;
import it.unive.lisa.witness.v2.violation.Waypoint;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Tests of the witness reader.
 * 
 * @author <a href="mailto:giacomo.zanatta@unive.it">Giacomo Zanatta</a>
 */
public class WitnessReaderTest {

	/**
	 * Returns a Java violation witness with a function return and a target.
	 * 
	 * @return the witness
	 */
	private static ViolationWitness violation() {
		return new ViolationWitness(new ViolationSequence(metadata(Language.JAVA), List.of(
				Segment.of(Waypoint.functionReturn(at(1), "\\result == 0", Format.EXT_JAVA_EXPRESSION)),
				Segment.of(Waypoint.target(at(2))))));
	}

	/**
	 * Returns the text of the witness of {@link #violation()}.
	 * 
	 * @return the text
	 * 
	 * @throws Exception if the witness cannot be written
	 */
	private static String violationText()
			throws Exception {
		return WitnessWriter.write(violation());
	}

	/**
	 * Checks that the given text is rejected by the reader.
	 * 
	 * @param text the text of a witness
	 * 
	 * @return the exception thrown by the reader
	 */
	private static WitnessReadException rejected(
			String text) {
		return assertThrows(WitnessReadException.class, () -> WitnessReader.read(text));
	}

	/**
	 * Checks that a written violation witness is read back.
	 */
	@Test
	public void aWrittenViolationWitnessIsReadBack()
			throws Exception {
		assertEquals(violation(), WitnessReader.read(violationText()));
	}

	/**
	 * Checks that a written correctness witness is read back.
	 */
	@Test
	public void aWrittenCorrectnessWitnessIsReadBack()
			throws Exception {
		CorrectnessWitness witness = new CorrectnessWitness(new InvariantSet(metadata(Language.JAVA), List.of(
				new Invariant(InvariantType.LOOP_INVARIANT, at(3), "i >= 0", Format.JAVA_EXPRESSION,
						Set.of(Label.INDUCTIVE)),
				new FunctionContract(at(5), "x > 0", "\\result > x", Format.EXT_JAVA_EXPRESSION, null))));
		assertEquals(witness, WitnessReader.read(WitnessWriter.write(witness)));
	}

	/**
	 * Checks that an unknown key is rejected.
	 */
	@Test
	public void anUnknownKeyIsRejected()
			throws Exception {
		rejected(violationText().replace("action: follow", "action: follow\n            colour: red"));
	}

	/**
	 * Checks that a missing required key is rejected.
	 */
	@Test
	public void aMissingRequiredKeyIsRejected()
			throws Exception {
		WitnessReadException e = rejected(violationText().replace("line: 2\n", ""));
		assertTrue(e.getMessage().contains("'line'"), e.getMessage());
	}

	/**
	 * Checks that the reader applies the rules of the constructors and reports
	 * the line.
	 */
	@Test
	public void theRulesOfTheConstructorsApply()
			throws Exception {
		WitnessReadException e = rejected(violationText().replace("type: target", "type: assumption"));
		assertTrue(e.getMessage().startsWith("line "), e.getMessage());
		assertTrue(e.getMessage().contains("assumption waypoint"), e.getMessage());
	}

	/**
	 * Checks that two violation sequences are rejected.
	 */
	@Test
	public void twoViolationSequencesAreRejected()
			throws Exception {
		rejected(violationText() + violationText());
	}

	/**
	 * Checks that a creation time with an offset is read.
	 */
	@Test
	public void aCreationTimeWithAnOffsetIsRead()
			throws Exception {
		Witness witness = WitnessReader.read(
				violationText().replace("creation_time: 2026-09-08T17:36:11Z",
						"creation_time: 2026-09-08T19:36:11+02:00"));
		assertEquals(violation(), witness);
	}

	/**
	 * Checks that a creation time that is not a date is rejected.
	 */
	@Test
	public void aCreationTimeThatIsNotADateIsRejected()
			throws Exception {
		rejected(violationText().replace("creation_time: 2026-09-08T17:36:11Z", "creation_time: yesterday"));
	}

	/**
	 * Checks that an empty witness is rejected.
	 */
	@Test
	public void anEmptyWitnessIsRejected() {
		rejected("[]");
	}

	/**
	 * Checks that only format version 2.4 is read.
	 */
	@Test
	public void onlyFormatVersion24IsRead()
			throws Exception {
		WitnessReadException e = rejected(
				violationText().replace("format_version: \"2.4\"", "format_version: \"2.3\""));
		assertTrue(e.getMessage().contains("2.3"), e.getMessage());
	}

	/**
	 * Checks that a contract with another type is rejected.
	 */
	@Test
	public void aContractWithAnotherTypeIsRejected()
			throws Exception {
		CorrectnessWitness witness = new CorrectnessWitness(new InvariantSet(metadata(Language.JAVA),
				List.of(new FunctionContract(at(5), "x > 0", null, Format.JAVA_EXPRESSION, null))));
		rejected(WitnessWriter.write(witness).replace("type: function_contract", "type: loop_contract"));
	}

	/**
	 * Checks that the error keeps the cause.
	 */
	@Test
	public void theErrorKeepsTheCause()
			throws Exception {
		WitnessReadException e = rejected(violationText().replace("type: target", "type: assumption"));
		assertTrue(e.getCause() != null);
	}

	/**
	 * Checks that a contract with only ensures survives the round trip.
	 */
	@Test
	public void aContractWithOnlyEnsuresSurvivesTheRoundTrip()
			throws Exception {
		CorrectnessWitness witness = new CorrectnessWitness(new InvariantSet(metadata(Language.JAVA),
				List.of(new FunctionContract(at(1), null, "x > 0", Format.JAVA_EXPRESSION, null))));
		assertEquals(witness, WitnessReader.read(WitnessWriter.write(witness)));
	}
}
