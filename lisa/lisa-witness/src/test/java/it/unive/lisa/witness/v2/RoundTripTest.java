package it.unive.lisa.witness.v2;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

public class RoundTripTest {

	private static final List<String> EXAMPLES = List.of(
			"java/ExDarko_false.witness-2.4.yml",
			"java/ExDarko_true.witness-2.4.yml",
			"sum.witness-2.4.yml",
			"zero_div.witness-2.4.yml",
			"concurrent-unreach.witness-2.2.yml",
			"concurrent-no-overflow.witness-2.2.yml",
			"concurrent-data-race.witness-2.2.yml",
			"nonterminating-program-example.witness-2.1.yml",
			"unsafe-program-example.cpachecker.witness-2.0.yml",
			"unsafe-program-example.symbiotic.witness-2.0.yml",
			"safe-program-example.witness-2.0.yml",
			"terminating-program-example.witness-2.1.yml",
			"multivar_1-1.witness-2.0.yml",
			"multivar_1-1.witness-2.1.yml",
			"safe-simple-concurrent.witness-2.1.yml");

	// the C examples are older versions with the same content in 2.4: only
	// their version is changed
	private static Witness read(
			String name)
			throws Exception {
		try (InputStream in = Files.newInputStream(Path.of("witness-testcases", name))) {
			String text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
			return WitnessReader
					.read(text.replaceAll("format_version: (['\"]?)2\\.[0-3]\\1", "format_version: \"2.4\""));
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
