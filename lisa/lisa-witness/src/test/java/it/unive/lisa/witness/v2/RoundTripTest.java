package it.unive.lisa.witness.v2;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

public class RoundTripTest {

	private static final List<String> EXAMPLES = List.of(
			"ExDarko_false.witness-2.4.yml",
			"ExDarko_true.witness-2.4.yml",
			"concurrent-unreach.witness-2.2.yml",
			"safe-simple-concurrent.witness-2.1.yml",
			"multivar_1-1.witness-2.1.yml");

	// the C examples are older versions with the same content in 2.4: only
	// their version is changed
	private static Witness read(
			String name)
			throws Exception {
		try (InputStream in = RoundTripTest.class.getResourceAsStream("/examples/" + name)) {
			String text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
			return WitnessReader.read(text.replaceAll("format_version: \"2\\.[0-3]\"", "format_version: \"2.4\""));
		}
	}

	@TestFactory
	public Stream<DynamicTest> aWitnessWrittenAndReadAgainIsTheSame() {
		return EXAMPLES.stream().map(name -> DynamicTest.dynamicTest(name, () -> {
			Witness first = read(name);
			Witness second = WitnessReader.read(WitnessWriter.write(first));
			assertEquals(first, second);
		}));
	}
}
