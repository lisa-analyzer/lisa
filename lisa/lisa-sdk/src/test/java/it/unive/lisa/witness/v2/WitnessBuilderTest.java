package it.unive.lisa.witness.v2;

import static it.unive.lisa.witness.v2.TestValues.at;
import static it.unive.lisa.witness.v2.TestValues.task;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.witness.v2.correctness.FunctionContract;
import it.unive.lisa.witness.v2.correctness.Invariant;
import it.unive.lisa.witness.v2.correctness.InvariantType;
import it.unive.lisa.witness.v2.correctness.Label;
import it.unive.lisa.witness.v2.metadata.Metadata;
import it.unive.lisa.witness.v2.metadata.Producer;
import it.unive.lisa.witness.v2.violation.Action;
import it.unive.lisa.witness.v2.violation.Constraint;
import it.unive.lisa.witness.v2.violation.Segment;
import it.unive.lisa.witness.v2.violation.Waypoint;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests of the violation and correctness witness builders.
 * 
 * @author <a href="mailto:giacomo.zanatta@unive.it">Giacomo Zanatta</a>
 */
public class WitnessBuilderTest {

	/**
	 * The producer of the witnesses of the tests.
	 */
	private static final Producer PRODUCER = new Producer("Test", "1");

	/**
	 * Checks that the violation builder fills the metadata.
	 */
	@Test
	public void theViolationBuilderFillsTheMetadata() {
		Metadata metadata = new ViolationWitnessBuilder(task(Language.JAVA), PRODUCER).target(at(2)).build()
				.getSequence().getMetadata();
		assertEquals(Metadata.FORMAT_VERSION, metadata.getFormatVersion());
		assertEquals(PRODUCER, metadata.getProducer());
		assertEquals(task(Language.JAVA), metadata.getTask());
	}

	/**
	 * Checks that the formats follow the language.
	 */
	@Test
	public void theFormatsFollowTheLanguage() {
		ViolationWitness witness = new ViolationWitnessBuilder(task(Language.C), PRODUCER)
				.assumption(at(3), "x == 1")
				.functionReturn(at(4), "\\result == 0")
				.target(at(5))
				.build();
		List<Segment> segments = witness.getSequence().getSegments();
		assertEquals(Format.C_EXPRESSION, segments.get(0).getWaypoints().get(0).getConstraint().getFormat());
		assertEquals(Format.EXT_C_EXPRESSION, segments.get(1).getWaypoints().get(0).getConstraint().getFormat());
	}

	/**
	 * Checks that the correctness builder chooses the formats.
	 */
	@Test
	public void theCorrectnessBuilderChoosesTheFormats() {
		CorrectnessWitness witness = new CorrectnessWitnessBuilder(task(Language.JAVA), PRODUCER)
				.loopInvariant(at(3), "i >= 0")
				.contract(at(5), "x > 0", "\\result > x")
				.build();
		assertEquals(List.of(new Invariant(InvariantType.LOOP_INVARIANT, at(3), "i >= 0", Format.JAVA_EXPRESSION),
				new FunctionContract(at(5), "x > 0", "\\result > x", Format.EXT_JAVA_EXPRESSION, null)),
				witness.getInvariants().getItems());
		assertFalse(witness.getGhost().isPresent());
	}

	/**
	 * Checks that ghost code makes a second entry with the same metadata.
	 */
	@Test
	public void ghostCodeMakesASecondEntryWithTheSameMetadata() {
		CorrectnessWitness witness = new CorrectnessWitnessBuilder(task(Language.C), PRODUCER)
				.locationInvariant(at(20), "(ghost != 0) || (used == 0)")
				.ghostVariable("ghost", "int", "0")
				.ghostUpdate(at(20), "ghost", "1")
				.build();
		assertTrue(witness.getGhost().isPresent());
		assertEquals(witness.getInvariants().getMetadata(), witness.getGhost().get().getMetadata());
	}

	/**
	 * Checks that ghost code is rejected for Java when added.
	 */
	@Test
	public void ghostCodeIsRejectedForJavaWhenAdded() {
		CorrectnessWitnessBuilder builder = new CorrectnessWitnessBuilder(task(Language.JAVA), PRODUCER);
		assertThrows(InvalidWitnessException.class, () -> builder.ghostVariable("ghost", "int", "0"));
		assertThrows(InvalidWitnessException.class, () -> builder.ghostUpdate(at(1), "ghost", "1"));
	}

	/**
	 * Checks that {@code forInputs} hashes the files and keeps their names as
	 * given.
	 */
	@Test
	public void forInputsHashesTheFilesAsGiven(
			@TempDir Path dir)
			throws Exception {
		Path main = Files.writeString(dir.resolve("Main.java"), "class Main {}\n", StandardCharsets.UTF_8);
		Path prp = Files.writeString(dir.resolve("valid-assert.prp"), "CHECK( init(Main.main()), LTL(G assert) )\n");
		ViolationWitness witness = ViolationWitnessBuilder
				.forInputs(List.of(main), prp, Language.JAVA, null, PRODUCER)
				.target(new Location(main.toString(), 1))
				.build();
		assertEquals(List.of(main.toString()), witness.getSequence().getMetadata().getTask().getInputFiles());
		assertEquals(witness, WitnessReader.read(WitnessWriter.write(witness)));
	}

	/**
	 * Checks that avoid waypoints come before the follow waypoint.
	 */
	@Test
	public void avoidWaypointsComeBeforeTheFollowWaypoint() {
		ViolationWitness witness = new ViolationWitnessBuilder(task(Language.C), PRODUCER)
				.segment(s -> s
						.branching(Action.AVOID, at(3), false)
						.functionEnter(Action.AVOID, at(4))
						.assumption(Action.FOLLOW, at(5), "x == 1"))
				.target(at(6))
				.build();
		List<Waypoint> first = witness.getSequence().getSegments().get(0).getWaypoints();
		assertEquals(List.of(Action.AVOID, Action.AVOID, Action.FOLLOW),
				List.of(first.get(0).getAction(), first.get(1).getAction(), first.get(2).getAction()));
		assertEquals(Constraint.withoutFormat("false"), first.get(0).getConstraint());
		assertEquals(2, witness.getSequence().getSegments().size());
	}

	/**
	 * Checks that a cycle segment can be built.
	 */
	@Test
	public void aCycleSegment() {
		ViolationWitness witness = new ViolationWitnessBuilder(task(Language.C), PRODUCER)
				.functionEnter(at(2))
				.segment(s -> s.branching(Action.CYCLE, at(3), true))
				.build();
		assertEquals(Action.CYCLE, witness.getSequence().getSegments().get(1).getWaypoints().get(0).getAction());
	}

	/**
	 * Checks that a data race ends with two targets.
	 */
	@Test
	public void aDataRaceEndsWithTwoTargets() {
		ViolationWitness witness = new ViolationWitnessBuilder(task(Language.C), PRODUCER)
				.segment(s -> s.target(at(10), 1).target(at(20), 2))
				.build();
		Segment last = witness.getSequence().getSegments().get(0);
		assertEquals(2, last.getWaypoints().size());
		assertEquals(Integer.valueOf(2), last.getWaypoints().get(1).getThreadId());
	}

	/**
	 * Checks that assignments at the same location form one update.
	 */
	@Test
	public void assignmentsAtTheSameLocationFormOneUpdate() {
		CorrectnessWitness witness = new CorrectnessWitnessBuilder(task(Language.C), PRODUCER)
				.ghostVariable("a", "int", "0")
				.ghostVariable("b", "int", "0")
				.ghostUpdate(at(5), "a", "1")
				.ghostUpdate(at(5), "b", "1")
				.ghostUpdate(at(6), "a", "0")
				.build();
		assertEquals(2, witness.getGhost().get().getGhostUpdates().size());
		assertEquals(2, witness.getGhost().get().getGhostUpdates().get(0).getUpdates().size());
	}

	/**
	 * Checks that a plain expression keeps the plain format.
	 */
	@Test
	public void aPlainExpressionKeepsThePlainFormat() {
		CorrectnessWitness witness = new CorrectnessWitnessBuilder(task(Language.C), PRODUCER)
				.contract(at(3), "x > 0", "1")
				.invariant(InvariantType.LOOP_TRANSITION_INVARIANT, at(4), "\\at(i, Pre) <= i", Label.INDUCTIVE)
				.build();
		assertEquals(Format.C_EXPRESSION, witness.getInvariants().getItems().get(0).getFormat());
		assertEquals(Format.EXT_C_EXPRESSION, witness.getInvariants().getItems().get(1).getFormat());
		assertTrue(witness.getInvariants().getItems().get(1).isInductive());
	}

	/**
	 * Checks that the builder checks an item when it is added.
	 */
	@Test
	public void theBuilderChecksAnItemWhenItIsAdded() {
		CorrectnessWitnessBuilder builder = new CorrectnessWitnessBuilder(task(Language.JAVA), PRODUCER);
		assertThrows(InvalidWitnessException.class,
				() -> builder.invariant(InvariantType.LOOP_TRANSITION_INVARIANT, at(1), "i >= 0"));
		assertThrows(InvalidWitnessException.class, () -> builder.item(
				new Invariant(InvariantType.LOCATION_INVARIANT, at(1), "x == 0", Format.C_EXPRESSION)));
	}

	/**
	 * Checks that the builder rejects a duplicate ghost variable at the call.
	 */
	@Test
	public void theBuilderRejectsADuplicateGhostVariableAtTheCall() {
		CorrectnessWitnessBuilder builder = new CorrectnessWitnessBuilder(task(Language.C), PRODUCER)
				.ghostVariable("g", "int", "0");
		assertThrows(InvalidWitnessException.class, () -> builder.ghostVariable("g", "int", "1"));
	}

	/**
	 * Checks that an update before its declaration is accepted.
	 */
	@Test
	public void anUpdateBeforeItsDeclarationIsAccepted() {
		CorrectnessWitness witness = new CorrectnessWitnessBuilder(task(Language.C), PRODUCER)
				.ghostUpdate(at(2), "g", "1")
				.ghostVariable("g", "int", "0")
				.build();
		assertEquals(1, witness.getGhost().get().getGhostUpdates().size());
	}

	/**
	 * Checks that an explicit format is checked when added.
	 */
	@Test
	@SuppressWarnings("deprecation")
	public void anExplicitFormatIsCheckedWhenAdded() {
		ViolationWitnessBuilder builder = new ViolationWitnessBuilder(task(Language.JAVA), PRODUCER);
		assertThrows(InvalidWitnessException.class, () -> builder.segment(
				s -> s.functionReturn(Action.FOLLOW, at(1), "\\result == 0", Format.ACSL_EXPRESSION)));
		ViolationWitness witness = new ViolationWitnessBuilder(task(Language.C), PRODUCER)
				.segment(s -> s.functionReturn(Action.FOLLOW, at(1), "\\result == 0", Format.ACSL_EXPRESSION, 1))
				.target(at(2))
				.build();
		assertEquals(Integer.valueOf(1),
				witness.getSequence().getSegments().get(0).getWaypoints().get(0).getThreadId());
	}

	/**
	 * Checks that a label given twice is kept once.
	 */
	@Test
	public void aRepeatedLabelIsKeptOnce() {
		CorrectnessWitness witness = new CorrectnessWitnessBuilder(task(Language.C), PRODUCER)
				.invariant(InvariantType.LOOP_INVARIANT, at(4), "i >= 0", Label.INDUCTIVE, Label.INDUCTIVE)
				.build();
		assertEquals(Set.of(Label.INDUCTIVE), witness.getInvariants().getItems().get(0).getLabels());
	}

	/**
	 * Checks that a {@code null} label is rejected as an invalid witness.
	 */
	@Test
	public void aNullLabelIsRejected() {
		CorrectnessWitnessBuilder builder = new CorrectnessWitnessBuilder(task(Language.C), PRODUCER);
		assertThrows(InvalidWitnessException.class,
				() -> builder.invariant(InvariantType.LOOP_INVARIANT, at(4), "i >= 0", (Label) null));
	}

	/**
	 * Checks that an explicit format is kept for invariants and contracts.
	 */
	@Test
	public void anExplicitFormatIsKeptForInvariantsAndContracts() {
		CorrectnessWitness witness = new CorrectnessWitnessBuilder(task(Language.C), PRODUCER)
				.invariant(InvariantType.LOOP_INVARIANT, at(4), "i >= 0", Format.EXT_C_EXPRESSION)
				.contract(at(5), "x > 0", "1", Format.EXT_C_EXPRESSION)
				.build();
		assertEquals(Format.EXT_C_EXPRESSION, witness.getInvariants().getItems().get(0).getFormat());
		assertEquals(Format.EXT_C_EXPRESSION, witness.getInvariants().getItems().get(1).getFormat());
	}
}
