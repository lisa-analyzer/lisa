package it.unive.lisa.witness.v2;

import static it.unive.lisa.witness.v2.TestValues.at;
import static it.unive.lisa.witness.v2.TestValues.task;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import it.unive.lisa.witness.v2.correctness.Invariant;
import it.unive.lisa.witness.v2.correctness.InvariantSet;
import it.unive.lisa.witness.v2.correctness.InvariantSetItem;
import it.unive.lisa.witness.v2.correctness.InvariantType;
import it.unive.lisa.witness.v2.metadata.Producer;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class WitnessWriterTest {

	private static final Producer PRODUCER = new Producer("Test", "1");

	private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());

	private static final List<String> VALUES = List.of("true", "null", "yes", "1", "-1", "2.4", "!flag", "a: b",
			"x # y", "*p", "&x", "x == 1 ? 2 : 3", "s.equals(\"a\")", "'c' == c", "[1]", "{x}", "@x", "%x", "-x",
			"x || y", "`x`");

	private static CorrectnessWitness witness() {
		List<InvariantSetItem> items = new ArrayList<>();
		for (int i = 0; i < VALUES.size(); i++)
			items.add(new Invariant(InvariantType.LOCATION_INVARIANT, TestValues.at(i + 1), VALUES.get(i),
					Format.JAVA_EXPRESSION));
		return new CorrectnessWitness(new InvariantSet(TestValues.metadata(Language.JAVA), items));
	}

	@Test
	public void everyValueIsReadBackAsTheSameString()
			throws IOException {
		JsonNode content = YAML.readTree(WitnessWriter.write(witness())).get(0).get("content");
		for (int i = 0; i < VALUES.size(); i++) {
			JsonNode value = content.get(i).get("invariant").get("value");
			assertTrue(value.isTextual(), VALUES.get(i));
			assertEquals(VALUES.get(i), value.asText());
		}
	}

	@Test
	public void theFormatVersionStaysAString()
			throws IOException {
		JsonNode metadata = YAML.readTree(WitnessWriter.write(witness())).get(0).get("metadata");
		assertTrue(metadata.get("format_version").isTextual());
	}

	@Test
	public void theOutputIsAListWithoutADocumentStartMarker()
			throws IOException {
		String text = WitnessWriter.write(witness());
		assertFalse(text.startsWith("---"));
		assertTrue(text.strip().startsWith("- entry_type: invariant_set"));
	}

	@Test
	public void writingToAFileGivesTheSameText(
			@TempDir Path dir)
			throws IOException {
		Path file = dir.resolve("witness.yml");
		WitnessWriter.write(witness(), file);
		assertEquals(WitnessWriter.write(witness()), Files.readString(file, StandardCharsets.UTF_8));
	}

	@Test
	public void writingToAStreamLeavesItOpen()
			throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		WitnessWriter.write(witness(), out);
		out.write('#');
		assertEquals(WitnessWriter.write(witness()) + "#", out.toString(StandardCharsets.UTF_8));
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
}
