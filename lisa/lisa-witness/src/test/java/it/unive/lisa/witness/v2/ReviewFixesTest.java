package it.unive.lisa.witness.v2;

import static it.unive.lisa.witness.v2.TestValues.HASH;
import static it.unive.lisa.witness.v2.TestValues.at;
import static it.unive.lisa.witness.v2.TestValues.metadata;
import static it.unive.lisa.witness.v2.TestValues.task;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import it.unive.lisa.witness.v2.correctness.FunctionContract;
import it.unive.lisa.witness.v2.correctness.Invariant;
import it.unive.lisa.witness.v2.correctness.InvariantSet;
import it.unive.lisa.witness.v2.correctness.InvariantType;
import it.unive.lisa.witness.v2.correctness.ghost.Expression;
import it.unive.lisa.witness.v2.correctness.ghost.GhostAssignment;
import it.unive.lisa.witness.v2.correctness.ghost.GhostContent;
import it.unive.lisa.witness.v2.correctness.ghost.GhostScope;
import it.unive.lisa.witness.v2.correctness.ghost.GhostUpdate;
import it.unive.lisa.witness.v2.correctness.ghost.GhostVariable;
import it.unive.lisa.witness.v2.metadata.Metadata;
import it.unive.lisa.witness.v2.metadata.Producer;
import it.unive.lisa.witness.v2.metadata.Task;
import it.unive.lisa.witness.v2.metadata.TaskLoader;
import it.unive.lisa.witness.v2.violation.Action;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class ReviewFixesTest {

	private static final Producer PRODUCER = new Producer("Test", "1");

	private static final String SPEC = "CHECK( init(Main.main()), LTL(G assert) )";

	@Test
	public void onlyFormatVersion24CanBeBuilt() {
		assertThrows(InvalidWitnessException.class, () -> new Metadata("2.3", UUID.randomUUID(), Instant.now(),
				PRODUCER, task(Language.JAVA)));
	}

	@Test
	public void blankValuesAreRejected() {
		assertThrows(InvalidWitnessException.class,
				() -> new Invariant(InvariantType.LOCATION_INVARIANT, at(1), " ", Format.JAVA_EXPRESSION));
		assertThrows(InvalidWitnessException.class,
				() -> new FunctionContract(at(1), "", "x > 0", Format.JAVA_EXPRESSION, null));
		assertThrows(InvalidWitnessException.class, () -> new Expression("", Format.C_EXPRESSION));
	}

	@Test
	public void aContractWithOnlyEnsuresSurvivesTheRoundTrip()
			throws Exception {
		CorrectnessWitness witness = new CorrectnessWitness(new InvariantSet(metadata(Language.JAVA),
				List.of(new FunctionContract(at(1), null, "x > 0", Format.JAVA_EXPRESSION, null))));
		assertEquals(witness, WitnessReader.read(WitnessWriter.write(witness)));
	}

	@Test
	public void aTaskNeedsAnInputFile() {
		assertThrows(InvalidWitnessException.class,
				() -> new Task(List.of(), Map.of(), SPEC, Language.JAVA, null));
	}

	@Test
	public void theSameFileUnderTwoSpellingsIsRejected() {
		assertThrows(InvalidWitnessException.class, () -> new Task(List.of("a/B.java", "./a/B.java"),
				Map.of("a/B.java", HASH, "./a/B.java", HASH), SPEC, Language.JAVA, null));
	}

	@Test
	public void theLoaderKeepsTheSourcesOfTheLanguageOnce(
			@TempDir Path dir)
			throws Exception {
		Path a = Files.createDirectories(dir.resolve("a"));
		Path source = Files.writeString(a.resolve("B.java"), "class B {}\n", StandardCharsets.UTF_8);
		Files.writeString(a.resolve("B.class"), "compiled", StandardCharsets.UTF_8);
		Files.writeString(a.resolve("README.md"), "readme", StandardCharsets.UTF_8);
		Path prp = Files.writeString(dir.resolve("p.prp"), SPEC);
		Task task = TaskLoader.load(List.of(a, source, a.resolve(".").resolve("B.java")), prp, Language.JAVA, null);
		assertEquals(List.of(source.toString()), task.getInputFiles());
	}

	@Test
	public void ghostVariablesAreDeclaredOnce() {
		GhostVariable g = new GhostVariable("g", "int", GhostScope.GLOBAL, new Expression("0", Format.C_EXPRESSION));
		assertThrows(InvalidWitnessException.class, () -> new GhostContent(List.of(g, g), List.of()));
	}

	@Test
	public void ghostUpdatesAssignDeclaredVariables() {
		GhostVariable g = new GhostVariable("g", "int", GhostScope.GLOBAL, new Expression("0", Format.C_EXPRESSION));
		GhostUpdate update = new GhostUpdate(at(1), List.of(new GhostAssignment("h", "1", Format.C_EXPRESSION)));
		assertThrows(InvalidWitnessException.class, () -> new GhostContent(List.of(g), List.of(update)));
	}

	@Test
	public void theBuilderChecksAnItemWhenItIsAdded() {
		CorrectnessWitnessBuilder builder = new CorrectnessWitnessBuilder(task(Language.JAVA), PRODUCER);
		assertThrows(InvalidWitnessException.class,
				() -> builder.invariant(InvariantType.LOOP_TRANSITION_INVARIANT, at(1), "i >= 0"));
		assertThrows(InvalidWitnessException.class, () -> builder.item(
				new Invariant(InvariantType.LOCATION_INVARIANT, at(1), "x == 0", Format.C_EXPRESSION)));
	}

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

	@Test
	public void onlyTheExtensionKeywordsNeedTheExtendedFormat() {
		assertEquals(Format.C_EXPRESSION, Language.C.getFormatFor("c == '\\n'"));
		assertEquals(Format.JAVA_EXPRESSION, Language.JAVA.getFormatFor("s.equals(\"\\\\result\")"));
		assertEquals(Format.JAVA_EXPRESSION, Language.JAVA.getFormatFor("x > 0", null));
		assertEquals(Format.EXT_JAVA_EXPRESSION, Language.JAVA.getFormatFor("\\result > x"));
		assertEquals(Format.EXT_C_EXPRESSION, Language.C.getFormatFor("\\at(i, AnyPrev) > i"));
		assertEquals(Format.EXT_C_EXPRESSION, Language.C.getFormatFor("1", "\\return == 0"));
		assertEquals(Format.EXT_PYTHON_EXPRESSION, Language.PYTHON.getFormatFor("\\old(x) < x"));
	}

	@Test
	public void aLocationNamesItsFunction() {
		assertEquals("Main.java:3:5 (main)", new Location("Main.java", 3, 5, "main").toString());
	}

	@Test
	// a plain YAML mapper would write the type keys as YAML tags: the writer
	// turns them off
	public void aWitnessIsSerializedAsItsEntries()
			throws Exception {
		ViolationWitness witness = new ViolationWitnessBuilder(task(Language.JAVA), PRODUCER).target(at(2)).build();
		ObjectMapper yaml = new ObjectMapper(new YAMLFactory());
		assertEquals(yaml.readTree(WitnessWriter.write(witness)), new ObjectMapper().valueToTree(witness));
	}

	@Test
	public void anyBackslashOutsideLiteralsNeedsTheExtendedFormat() {
		assertEquals(Format.EXT_C_EXPRESSION, Language.C.getFormatFor("\\valid(p)"));
		assertEquals(Format.EXT_C_EXPRESSION, Language.C.getFormatFor("\\resultx == 0"));
		assertEquals(Format.EXT_JAVA_EXPRESSION,
				Language.JAVA.getFormatFor("s.equals(\"a\\\"b\") && \\old(x) < x"));
	}

	@Test
	public void theBuilderRejectsADuplicateGhostVariableAtTheCall() {
		CorrectnessWitnessBuilder builder = new CorrectnessWitnessBuilder(task(Language.C), PRODUCER)
				.ghostVariable("g", "int", "0");
		assertThrows(InvalidWitnessException.class, () -> builder.ghostVariable("g", "int", "1"));
	}

	@Test
	public void anUpdateBeforeItsDeclarationIsAccepted() {
		CorrectnessWitness witness = new CorrectnessWitnessBuilder(task(Language.C), PRODUCER)
				.ghostUpdate(at(2), "g", "1")
				.ghostVariable("g", "int", "0")
				.build();
		assertEquals(1, witness.getGhost().get().getGhostUpdates().size());
	}

	@Test
	public void aGhostUpdateNeedsAnAssignment() {
		assertThrows(InvalidWitnessException.class, () -> new GhostUpdate(at(1), List.of()));
	}

	@Test
	public void aLongLiteralIsHandledInLinearTime() {
		String literal = "s.equals(\"" + "\\n".repeat(500_000) + "\")";
		assertEquals(Format.JAVA_EXPRESSION, Language.JAVA.getFormatFor(literal));
		assertEquals(Format.EXT_JAVA_EXPRESSION, Language.JAVA.getFormatFor(literal + " && \\result > 0"));
		assertEquals(Format.JAVA_EXPRESSION, Language.JAVA.getFormatFor("s.equals(\"" + "a".repeat(1_000_000)));
	}
}
