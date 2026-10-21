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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class WitnessBuilderTest {

	private static final Producer PRODUCER = new Producer("Test", "1");

	@Test
	public void theViolationBuilderMakesOneSegmentPerWaypoint() {
		ViolationWitness witness = new ViolationWitnessBuilder(task(Language.JAVA), PRODUCER)
				.functionReturn(at(34), "\\result == 0")
				.target(at(48))
				.build();
		assertEquals(List.of(
				Segment.of(Waypoint.functionReturn(at(34), "\\result == 0", Format.EXT_JAVA_EXPRESSION)),
				Segment.of(Waypoint.target(at(48)))), witness.getSequence().getSegments());
	}

	@Test
	public void theViolationBuilderFillsTheMetadata() {
		Metadata metadata = new ViolationWitnessBuilder(task(Language.JAVA), PRODUCER).target(at(2)).build()
				.getSequence().getMetadata();
		assertEquals(Metadata.FORMAT_VERSION, metadata.getFormatVersion());
		assertEquals(PRODUCER, metadata.getProducer());
		assertEquals(task(Language.JAVA), metadata.getTask());
	}

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

	@Test
	public void ghostCodeIsRejectedForJava() {
		CorrectnessWitnessBuilder builder = new CorrectnessWitnessBuilder(task(Language.JAVA), PRODUCER)
				.ghostVariable("ghost", "int", "0");
		assertThrows(InvalidWitnessException.class, builder::build);
	}

	@Test
	public void aLocationOutsideTheInputFilesIsRejected() {
		ViolationWitnessBuilder builder = new ViolationWitnessBuilder(task(Language.JAVA), PRODUCER)
				.target(new Location("Other.java", 2));
		assertThrows(InvalidWitnessException.class, builder::build);
	}

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

	@Test
	public void aSegmentOfOnlyAvoidWaypointsIsRejected() {
		ViolationWitnessBuilder builder = new ViolationWitnessBuilder(task(Language.C), PRODUCER);
		assertThrows(InvalidWitnessException.class,
				() -> builder.segment(s -> s.assumption(Action.AVOID, at(7), "x == 0")));
	}

	@Test
	public void anEmptySegmentIsRejected() {
		ViolationWitnessBuilder builder = new ViolationWitnessBuilder(task(Language.C), PRODUCER);
		assertThrows(InvalidWitnessException.class, () -> builder.segment(s -> {
		}));
	}

	@Test
	public void aCycleSegment() {
		ViolationWitness witness = new ViolationWitnessBuilder(task(Language.C), PRODUCER)
				.functionEnter(at(2))
				.segment(s -> s.branching(Action.CYCLE, at(3), true))
				.build();
		assertEquals(Action.CYCLE, witness.getSequence().getSegments().get(1).getWaypoints().get(0).getAction());
	}

	@Test
	public void aDataRaceEndsWithTwoTargets() {
		ViolationWitness witness = new ViolationWitnessBuilder(task(Language.C), PRODUCER)
				.segment(s -> s.target(at(10), 1).target(at(20), 2))
				.build();
		Segment last = witness.getSequence().getSegments().get(0);
		assertEquals(2, last.getWaypoints().size());
		assertEquals(Integer.valueOf(2), last.getWaypoints().get(1).getThreadId());
	}

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
}
