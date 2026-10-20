package it.unive.lisa.witness.v2;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import it.unive.lisa.witness.v2.correctness.FunctionContract;
import it.unive.lisa.witness.v2.correctness.Invariant;
import it.unive.lisa.witness.v2.correctness.InvariantSet;
import it.unive.lisa.witness.v2.correctness.InvariantType;
import it.unive.lisa.witness.v2.correctness.ghost.Expression;
import it.unive.lisa.witness.v2.correctness.ghost.GhostAssignment;
import it.unive.lisa.witness.v2.correctness.ghost.GhostContent;
import it.unive.lisa.witness.v2.correctness.ghost.GhostInstrumentation;
import it.unive.lisa.witness.v2.correctness.ghost.GhostScope;
import it.unive.lisa.witness.v2.correctness.ghost.GhostUpdate;
import it.unive.lisa.witness.v2.correctness.ghost.GhostVariable;
import it.unive.lisa.witness.v2.metadata.Metadata;
import it.unive.lisa.witness.v2.metadata.Producer;
import it.unive.lisa.witness.v2.metadata.Task;
import it.unive.lisa.witness.v2.violation.Action;
import it.unive.lisa.witness.v2.violation.Constraint;
import it.unive.lisa.witness.v2.violation.Segment;
import it.unive.lisa.witness.v2.violation.ViolationSequence;
import it.unive.lisa.witness.v2.violation.Waypoint;
import it.unive.lisa.witness.v2.violation.WaypointType;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

public class ExamplesTest {

	private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());

	private static final String VERIFIER = "./java/common/org/sosy_lab/sv_benchmarks/Verifier.java";

	private static final String VERIFIER_HASH = "d544c9e74f86b4ed8e692b7b7bcb00ec3901e19f5b0d4740b49e5c3c672031ba";

	private static final String SPEC = "CHECK( init(Main.main()), LTL(G assert) )";

	private static JsonNode example(
			String name)
			throws IOException {
		try (InputStream in = ExamplesTest.class.getResourceAsStream("/examples/" + name)) {
			return YAML.readTree(in);
		}
	}

	private static JsonNode written(
			Witness witness)
			throws IOException {
		return YAML.readTree(WitnessWriter.write(witness));
	}

	private static Task cTask(
			String file,
			String hash) {
		return new Task(List.of(file), Map.of(file, hash), "G ! call(reach_error())", Language.C, DataModel.ILP32);
	}

	// some examples write their creation time with an offset (e.g. +01:00),
	// while we write the same instant
	// in UTC: the two trees are compared without it, and the instants are
	// compared separately
	private static void assertSameIgnoringCreationTime(
			JsonNode expected,
			JsonNode actual) {
		for (int i = 0; i < expected.size(); i++) {
			Instant expectedTime = OffsetDateTime.parse(expected.get(i).get("metadata").get("creation_time").asText())
					.toInstant();
			Instant actualTime = Instant.parse(actual.get(i).get("metadata").get("creation_time").asText());
			assertEquals(expectedTime, actualTime);
			((ObjectNode) expected.get(i).get("metadata")).remove("creation_time");
			((ObjectNode) actual.get(i).get("metadata")).remove("creation_time");
		}
		assertEquals(expected, actual);
	}

	@Test
	public void theViolationExampleIsReproduced()
			throws IOException {
		String main = "./java/jpf-regression/ExDarko_false/Main.java";
		Map<String, String> hashes = new LinkedHashMap<>();
		hashes.put(VERIFIER, VERIFIER_HASH);
		hashes.put(main, "44faed0b4c09127009615aa7c0699fb1acda5235fdcd1bbcbea867661d450d82");
		Task task = new Task(List.of(VERIFIER, main), hashes, SPEC, Language.JAVA, null);
		Metadata metadata = new Metadata("2.4", UUID.fromString("be742527-8e82-4418-9ba2-6cbb857e25a7"),
				Instant.parse("2026-09-08T17:36:11Z"), new Producer("GDart", "VERSION_PLACEHOLDER"), task);
		ViolationWitness witness = new ViolationWitness(new ViolationSequence(metadata, List.of(
				Segment.of(Waypoint.functionReturn(new Location(main, 34, 43), "\\result == 0",
						Format.EXT_JAVA_EXPRESSION)),
				Segment.of(Waypoint.functionReturn(new Location(main, 35, 41), "\\result == 0",
						Format.EXT_JAVA_EXPRESSION)),
				Segment.of(Waypoint.target(new Location(main, 48, 12))))));

		assertEquals(example("ExDarko_false.witness-2.4.yml"), written(witness));
	}

	@Test
	public void theCorrectnessExampleIsReproduced()
			throws IOException {
		String main = "./java/jpf-regression/ExDarko_true/Main.java";
		Map<String, String> hashes = new LinkedHashMap<>();
		hashes.put(main, "be9360466f076b2c5156aa1dff4479180025c5698cdfb03b0885f5007519a7d7");
		hashes.put(VERIFIER, VERIFIER_HASH);
		Task task = new Task(List.of(main, VERIFIER), hashes, SPEC, Language.JAVA, null);
		Metadata metadata = new Metadata("2.4", UUID.fromString("c66c3ee7-e0de-4bae-87f0-fd48fc7b981f"),
				Instant.parse("2026-10-09T13:41:43Z"), new Producer("Human", "42", "fall26", null, null), task);
		CorrectnessWitness witness = new CorrectnessWitness(new InvariantSet(metadata,
				List.of(new Invariant(InvariantType.LOCATION_INVARIANT, new Location(main, 43, 4, "unboxed"),
						"i != j", Format.JAVA_EXPRESSION))));

		assertSameIgnoringCreationTime(example("ExDarko_true.witness-2.4.yml"), written(witness));
	}

	@Test
	public void theCConcurrentViolationExampleIsReproduced()
			throws IOException {
		String file = "./concurrent-unreach.c";
		Metadata metadata = new Metadata("2.2", UUID.fromString("b1c2d3e4-f5a6-4789-abcd-ef0123456789"),
				Instant.parse("2025-10-01T12:00:00Z"), new Producer("Handcrafted", "1.0"),
				cTask(file, "43a2fe6641f8f4e3142bc55e614b442f698645f3f5af79e7ce68a32f9918b562"));
		ViolationWitness witness = new ViolationWitness(new ViolationSequence(metadata, List.of(
				Segment.of(new Waypoint(WaypointType.FUNCTION_ENTER, Action.FOLLOW,
						new Location(file, 25, 42, "main"), null, 0)),
				Segment.of(new Waypoint(WaypointType.ASSUMPTION, Action.FOLLOW,
						new Location(file, 20, 5, "writer"), Constraint.of("x == 1", Format.C_EXPRESSION), 1)),
				Segment.of(new Waypoint(WaypointType.BRANCHING, Action.FOLLOW,
						new Location(file, 26, 5, "main"), Constraint.withoutFormat("true"), 0)),
				Segment.of(new Waypoint(WaypointType.TARGET, Action.FOLLOW,
						new Location(file, 26, 17, "main"), null, 0)))));

		assertEquals(example("concurrent-unreach.witness-2.2.yml"), written(witness));
	}

	@Test
	public void theCCorrectnessExampleWithGhostCodeIsReproduced()
			throws IOException {
		String file = "./safe-simple-concurrent.c";
		Metadata metadata = new Metadata("2.1", UUID.fromString("c66c3ee7-e0de-4bae-87f0-fd48fc7b961d"),
				Instant.parse("2025-10-04T09:29:38Z"),
				new Producer("Handcrafted", "Inspired by: https://arxiv.org/pdf/2411.16612"),
				cTask(file, "939597023a4e5fc2915fcd9721261a49829641867ef067a9c88542826aefde8d"));
		InvariantSet invariants = new InvariantSet(metadata, List.of(new Invariant(InvariantType.LOCATION_INVARIANT,
				new Location(file, 20), "(ghost != 0) || (used == 0)", Format.C_EXPRESSION)));
		GhostVariable ghost = new GhostVariable("ghost", "int", GhostScope.GLOBAL,
				new Expression("0", Format.C_EXPRESSION));
		List<GhostUpdate> updates = List.of(
				new GhostUpdate(new Location(file, 20),
						List.of(new GhostAssignment("ghost", "1", Format.C_EXPRESSION))),
				new GhostUpdate(new Location(file, 22),
						List.of(new GhostAssignment("ghost", "0", Format.C_EXPRESSION))),
				new GhostUpdate(new Location(file, 9), List.of(new GhostAssignment("ghost", "1", Format.C_EXPRESSION))),
				new GhostUpdate(new Location(file, 12),
						List.of(new GhostAssignment("ghost", "0", Format.C_EXPRESSION))));
		CorrectnessWitness witness = new CorrectnessWitness(invariants,
				new GhostInstrumentation(metadata, new GhostContent(List.of(ghost), updates)));

		assertSameIgnoringCreationTime(example("safe-simple-concurrent.witness-2.1.yml"), written(witness));
	}

	@Test
	public void theCCorrectnessExampleWithContractsIsReproduced()
			throws IOException {
		String file = "./multivar_1-1.c";
		Metadata metadata = new Metadata("2.1", UUID.fromString("c66c3ee7-e0de-4bae-87f0-fd48fc7b961e"),
				Instant.parse("2023-11-21T09:29:38Z"), new Producer("CPAchecker", "2.2.1-svn", "svcomp24", null, null),
				cTask(file, "97265278cdbf3d456e80b67308db3a1e9b0ddca2be6450a8581510c7e5bc4295"));
		CorrectnessWitness witness = new CorrectnessWitness(new InvariantSet(metadata, List.of(
				new Invariant(InvariantType.LOOP_INVARIANT, new Location(file, 25, 3, "main"), "( y == x )",
						Format.C_EXPRESSION),
				new FunctionContract(new Location(file, 14, 1, "__VERIFIER_assert"), "cond != 0", "1",
						Format.C_EXPRESSION, null),
				new FunctionContract(new Location(file, 21, 1, "main"), "1", "\\return == 0",
						Format.EXT_C_EXPRESSION, null))));

		assertSameIgnoringCreationTime(example("multivar_1-1.witness-2.1.yml"), written(witness));
	}
}
