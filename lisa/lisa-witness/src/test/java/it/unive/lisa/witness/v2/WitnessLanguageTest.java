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

/**
 * Tests of the rules a witness checks against the language and the input files
 * of its task.
 * 
 * @author <a href="mailto:giacomo.zanatta@unive.it">Giacomo Zanatta</a>
 */
public class WitnessLanguageTest {

	/**
	 * Returns an invariant set with one invariant of the given type and format.
	 * 
	 * @param language the language of the task
	 * @param type     the type of the invariant
	 * @param format   the format of the invariant
	 * 
	 * @return the invariant set
	 */
	private static InvariantSet invariants(
			Language language,
			InvariantType type,
			Format format) {
		return new InvariantSet(metadata(language), List.of(new Invariant(type, at(4), "x >= 0", format)));
	}

	/**
	 * Returns a ghost instrumentation declaring one ghost variable.
	 * 
	 * @param language the language of the task
	 * 
	 * @return the ghost instrumentation
	 */
	private static GhostInstrumentation ghost(
			Language language) {
		GhostVariable lock = new GhostVariable("held", "int", GhostScope.GLOBAL,
				new Expression("0", Format.C_EXPRESSION));
		return new GhostInstrumentation(metadata(language), new GhostContent(List.of(lock), List.of()));
	}

	/**
	 * Checks that a Java violation witness rejects a C format.
	 */
	@Test
	public void aJavaViolationWitnessRejectsACFormat() {
		ViolationSequence sequence = new ViolationSequence(metadata(Language.JAVA),
				List.of(Segment.of(Waypoint.functionReturn(at(1), "\\result == 0", Format.EXT_C_EXPRESSION)),
						Segment.of(Waypoint.target(at(2)))));
		assertThrows(InvalidWitnessException.class, () -> new ViolationWitness(sequence));
	}

	/**
	 * Checks that a Java violation witness accepts a Java format.
	 */
	@Test
	public void aJavaViolationWitnessAcceptsAJavaFormat() {
		ViolationSequence sequence = new ViolationSequence(metadata(Language.JAVA),
				List.of(Segment.of(Waypoint.functionReturn(at(1), "\\result == 0", Format.EXT_JAVA_EXPRESSION)),
						Segment.of(Waypoint.target(at(2)))));
		assertDoesNotThrow(() -> new ViolationWitness(sequence));
	}

	/**
	 * Checks that a Java correctness witness rejects transition invariants.
	 */
	@Test
	public void aJavaCorrectnessWitnessRejectsTransitionInvariants() {
		assertThrows(InvalidWitnessException.class, () -> new CorrectnessWitness(
				invariants(Language.JAVA, InvariantType.LOOP_TRANSITION_INVARIANT, Format.JAVA_EXPRESSION)));
	}

	/**
	 * Checks that a C correctness witness accepts transition invariants.
	 */
	@Test
	public void aCCorrectnessWitnessAcceptsTransitionInvariants() {
		assertDoesNotThrow(() -> new CorrectnessWitness(
				invariants(Language.C, InvariantType.LOOP_TRANSITION_INVARIANT, Format.C_EXPRESSION)));
	}

	/**
	 * Checks that a Java correctness witness rejects ghost code.
	 */
	@Test
	public void aJavaCorrectnessWitnessRejectsGhostCode() {
		assertThrows(InvalidWitnessException.class, () -> new CorrectnessWitness(
				invariants(Language.JAVA, InvariantType.LOCATION_INVARIANT, Format.JAVA_EXPRESSION),
				ghost(Language.JAVA)));
	}

	/**
	 * Checks that a C correctness witness accepts ghost code.
	 */
	@Test
	public void aCCorrectnessWitnessAcceptsGhostCode() {
		CorrectnessWitness witness = new CorrectnessWitness(
				invariants(Language.C, InvariantType.LOCATION_INVARIANT, Format.C_EXPRESSION), ghost(Language.C));
		assertEquals(2, witness.getEntries().size());
	}

	/**
	 * Checks that a Java correctness witness accepts function contracts.
	 */
	@Test
	public void aJavaCorrectnessWitnessAcceptsFunctionContracts() {
		InvariantSet set = new InvariantSet(metadata(Language.JAVA),
				List.of(new FunctionContract(at(3), "x > 0", "\\result > x", Format.EXT_JAVA_EXPRESSION, null)));
		assertDoesNotThrow(() -> new CorrectnessWitness(set));
	}

	/**
	 * Checks that a function contract needs requires or ensures.
	 */
	@Test
	public void aFunctionContractNeedsRequiresOrEnsures() {
		assertThrows(InvalidWitnessException.class,
				() -> new FunctionContract(at(3), null, null, Format.JAVA_EXPRESSION, null));
	}

	/**
	 * Checks that a C invariant rejects the ACSL format.
	 */
	@Test
	@SuppressWarnings("deprecation")
	public void aCInvariantRejectsAcsl() {
		assertThrows(InvalidWitnessException.class, () -> new CorrectnessWitness(
				invariants(Language.C, InvariantType.LOCATION_INVARIANT, Format.ACSL_EXPRESSION)));
	}

	/**
	 * Checks that ghost code rejects the extended C format.
	 */
	@Test
	public void ghostCodeRejectsExtendedC() {
		GhostVariable lock = new GhostVariable("held", "int", GhostScope.GLOBAL,
				new Expression("0", Format.EXT_C_EXPRESSION));
		GhostInstrumentation ghost = new GhostInstrumentation(metadata(Language.C),
				new GhostContent(List.of(lock), List.of()));
		assertThrows(InvalidWitnessException.class, () -> new CorrectnessWitness(
				invariants(Language.C, InvariantType.LOCATION_INVARIANT, Format.C_EXPRESSION), ghost));
	}

	/**
	 * Checks that a waypoint must be in an input file.
	 */
	@Test
	public void aWaypointMustBeInAnInputFile() {
		ViolationSequence sequence = new ViolationSequence(metadata(Language.JAVA),
				List.of(Segment.of(Waypoint.target(new Location("Other.java", 2)))));
		assertThrows(InvalidWitnessException.class, () -> new ViolationWitness(sequence));
	}

	/**
	 * Checks that the ghost code must have the task of the invariants.
	 */
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

	/**
	 * Checks that an invariant must be in an input file.
	 */
	@Test
	public void anInvariantMustBeInAnInputFile() {
		InvariantSet set = new InvariantSet(metadata(Language.JAVA), List.of(new Invariant(
				InvariantType.LOCATION_INVARIANT, new Location("Other.java", 4), "x >= 0", Format.JAVA_EXPRESSION)));
		assertThrows(InvalidWitnessException.class, () -> new CorrectnessWitness(set));
	}

	/**
	 * Checks that a location matches an input file written with {@code ./}, and
	 * the other way round.
	 */
	@Test
	public void aLocationMatchesAnInputFileWithADotSlash() {
		Metadata dotSlash = new Metadata("2.4", metadata(Language.JAVA).getUuid(),
				metadata(Language.JAVA).getCreationTime(), metadata(Language.JAVA).getProducer(),
				new Task(List.of("./Main.java"), Map.of("./Main.java", TestValues.HASH),
						"CHECK( init(Main.main()), LTL(G assert) )", Language.JAVA, null));
		assertDoesNotThrow(() -> new ViolationWitness(
				new ViolationSequence(dotSlash, List.of(Segment.of(Waypoint.target(new Location("Main.java", 2)))))));
		assertDoesNotThrow(() -> new ViolationWitness(new ViolationSequence(metadata(Language.JAVA),
				List.of(Segment.of(Waypoint.target(new Location("./Main.java", 2)))))));
	}

	/**
	 * Checks that a Python correctness witness rejects ghost code.
	 */
	@Test
	public void aPythonCorrectnessWitnessRejectsGhostCode() {
		assertThrows(InvalidWitnessException.class, () -> new CorrectnessWitness(
				invariants(Language.PYTHON, InvariantType.LOCATION_INVARIANT, Format.PYTHON_EXPRESSION),
				ghost(Language.PYTHON)));
	}
}
