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

/**
 * Tests writing and reading back every example of the witness format.
 * 
 * @author <a href="mailto:giacomo.zanatta@unive.it">Giacomo Zanatta</a>
 */
public class RoundTripTest {

	/**
	 * Every example of the witness format.
	 */
	private static final List<String> EXAMPLES = List.of(
			"java/ExDarko_false.witness-2.4.yml",
			"java/ExDarko_true.witness-2.4.yml",
			"sum.witness-2.4.yml",
			"zero_div.witness-2.4.yml",
			"concurrent-unreach.witness-2.2.yml",
			"multivar_1-1.witness-2.1.yml",
			"safe-simple-concurrent.witness-2.1.yml");

	/**
	 * Reads an example of the witness format. The older examples have the same
	 * content in version 2.4, and only their version is changed.
	 * 
	 * @param name the name of the example in the test cases
	 * 
	 * @return the witness
	 * 
	 * @throws Exception if the example cannot be read
	 */
	private static Witness read(
			String name)
			throws Exception {
		try (InputStream in = Files.newInputStream(Path.of("witness-testcases", name))) {
			String text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
			return WitnessReader
					.read(text.replaceAll("format_version: (['\"]?)2\\.[0-3]\\1", "format_version: \"2.4\""));
		}
	}

	/**
	 * Checks, for every example, that a witness written and read again is the
	 * same.
	 */
	@TestFactory
	public Stream<DynamicTest> aWitnessWrittenAndReadAgainIsTheSame() {
		return EXAMPLES.stream().map(name -> DynamicTest.dynamicTest(name, () -> {
			Witness first = read(name);
			Witness second = WitnessReader.read(WitnessWriter.write(first));
			assertEquals(first, second);
		}));
	}
}
