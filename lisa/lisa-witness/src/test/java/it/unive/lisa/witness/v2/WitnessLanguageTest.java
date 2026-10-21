package it.unive.lisa.witness.v2;

import static it.unive.lisa.witness.v2.TestValues.at;
import static it.unive.lisa.witness.v2.TestValues.metadata;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import it.unive.lisa.witness.v2.correctness.FunctionContract;
import it.unive.lisa.witness.v2.correctness.Invariant;
import it.unive.lisa.witness.v2.correctness.InvariantSet;
import it.unive.lisa.witness.v2.correctness.InvariantType;
import it.unive.lisa.witness.v2.correctness.ghost.Expression;
import it.unive.lisa.witness.v2.correctness.ghost.GhostContent;
import it.unive.lisa.witness.v2.correctness.ghost.GhostInstrumentation;
import it.unive.lisa.witness.v2.correctness.ghost.GhostScope;
import it.unive.lisa.witness.v2.correctness.ghost.GhostVariable;
import it.unive.lisa.witness.v2.metadata.Metadata;
import it.unive.lisa.witness.v2.metadata.Task;
import it.unive.lisa.witness.v2.violation.Segment;
import it.unive.lisa.witness.v2.violation.ViolationSequence;
import it.unive.lisa.witness.v2.violation.Waypoint;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class WitnessLanguageTest {

	private static InvariantSet invariants(
			Language language,
			InvariantType type,
			Format format) {
		return new InvariantSet(metadata(language), List.of(new Invariant(type, at(4), "x >= 0", format)));
	}

	private static GhostInstrumentation ghost(
			Language language) {
		GhostVariable lock = new GhostVariable("held", "int", GhostScope.GLOBAL,
				new Expression("0", Format.C_EXPRESSION));
		return new GhostInstrumentation(metadata(language), new GhostContent(List.of(lock), List.of()));
	}

	@Test
	public void aJavaViolationWitnessRejectsACFormat() {
		ViolationSequence sequence = new ViolationSequence(metadata(Language.JAVA),
				List.of(Segment.of(Waypoint.functionReturn(at(1), "\\result == 0", Format.EXT_C_EXPRESSION)),
						Segment.of(Waypoint.target(at(2)))));
		assertThrows(InvalidWitnessException.class, () -> new ViolationWitness(sequence));
	}

	@Test
	public void aJavaViolationWitnessAcceptsAJavaFormat() {
		ViolationSequence sequence = new ViolationSequence(metadata(Language.JAVA),
				List.of(Segment.of(Waypoint.functionReturn(at(1), "\\result == 0", Format.EXT_JAVA_EXPRESSION)),
						Segment.of(Waypoint.target(at(2)))));
		assertDoesNotThrow(() -> new ViolationWitness(sequence));
	}

	@Test
	public void aJavaCorrectnessWitnessRejectsTransitionInvariants() {
		assertThrows(InvalidWitnessException.class, () -> new CorrectnessWitness(
				invariants(Language.JAVA, InvariantType.LOOP_TRANSITION_INVARIANT, Format.JAVA_EXPRESSION)));
	}

	@Test
	public void aCCorrectnessWitnessAcceptsTransitionInvariants() {
		assertDoesNotThrow(() -> new CorrectnessWitness(
				invariants(Language.C, InvariantType.LOOP_TRANSITION_INVARIANT, Format.C_EXPRESSION)));
	}

	@Test
	public void aJavaCorrectnessWitnessRejectsGhostCode() {
		assertThrows(InvalidWitnessException.class, () -> new CorrectnessWitness(
				invariants(Language.JAVA, InvariantType.LOCATION_INVARIANT, Format.JAVA_EXPRESSION),
				ghost(Language.JAVA)));
	}

	@Test
	public void aCCorrectnessWitnessAcceptsGhostCode() {
		CorrectnessWitness witness = new CorrectnessWitness(
				invariants(Language.C, InvariantType.LOCATION_INVARIANT, Format.C_EXPRESSION), ghost(Language.C));
		assertEquals(2, witness.getEntries().size());
	}

	@Test
	public void aJavaCorrectnessWitnessAcceptsFunctionContracts() {
		InvariantSet set = new InvariantSet(metadata(Language.JAVA),
				List.of(new FunctionContract(at(3), "x > 0", "\\result > x", Format.EXT_JAVA_EXPRESSION, null)));
		assertDoesNotThrow(() -> new CorrectnessWitness(set));
	}

	@Test
	public void aFunctionContractNeedsRequiresOrEnsures() {
		assertThrows(InvalidWitnessException.class,
				() -> new FunctionContract(at(3), null, null, Format.JAVA_EXPRESSION, null));
	}

	@Test
	@SuppressWarnings("deprecation")
	public void aCInvariantRejectsAcsl() {
		assertThrows(InvalidWitnessException.class, () -> new CorrectnessWitness(
				invariants(Language.C, InvariantType.LOCATION_INVARIANT, Format.ACSL_EXPRESSION)));
	}

	@Test
	public void ghostCodeRejectsExtendedC() {
		GhostVariable lock = new GhostVariable("held", "int", GhostScope.GLOBAL,
				new Expression("0", Format.EXT_C_EXPRESSION));
		GhostInstrumentation ghost = new GhostInstrumentation(metadata(Language.C),
				new GhostContent(List.of(lock), List.of()));
		assertThrows(InvalidWitnessException.class, () -> new CorrectnessWitness(
				invariants(Language.C, InvariantType.LOCATION_INVARIANT, Format.C_EXPRESSION), ghost));
	}

	@Test
	public void aWaypointMustBeInAnInputFile() {
		ViolationSequence sequence = new ViolationSequence(metadata(Language.JAVA),
				List.of(Segment.of(Waypoint.target(new Location("Other.java", 2)))));
		assertThrows(InvalidWitnessException.class, () -> new ViolationWitness(sequence));
	}

	@Test
	public void theGhostCodeMustHaveTheTaskOfTheInvariants() {
		Metadata other = new Metadata("2.4", metadata(Language.C).getUuid(), metadata(Language.C).getCreationTime(),
				metadata(Language.C).getProducer(), new Task(List.of("Other.c"),
						Map.of("Other.c", TestValues.HASH), "G ! call(reach_error())", Language.C,
						DataModel.LP64));
		GhostVariable lock = new GhostVariable("held", "int", GhostScope.GLOBAL,
				new Expression("0", Format.C_EXPRESSION));
		GhostInstrumentation ghost = new GhostInstrumentation(other, new GhostContent(List.of(lock), List.of()));
		assertThrows(InvalidWitnessException.class, () -> new CorrectnessWitness(
				invariants(Language.C, InvariantType.LOCATION_INVARIANT, Format.C_EXPRESSION), ghost));
	}

	@Test
	public void anInvariantMustBeInAnInputFile() {
		InvariantSet set = new InvariantSet(metadata(Language.JAVA), List.of(new Invariant(
				InvariantType.LOCATION_INVARIANT, new Location("Other.java", 4), "x >= 0", Format.JAVA_EXPRESSION)));
		assertThrows(InvalidWitnessException.class, () -> new CorrectnessWitness(set));
	}
}
