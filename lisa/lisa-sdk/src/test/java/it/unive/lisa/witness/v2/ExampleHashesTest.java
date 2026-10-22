package it.unive.lisa.witness.v2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import it.unive.lisa.witness.v2.metadata.Task;
import it.unive.lisa.witness.v2.metadata.TaskLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests of the hashes written in the examples of the witness format.
 * 
 * @author <a href="mailto:giacomo.zanatta@unive.it">Giacomo Zanatta</a>
 */
public class ExampleHashesTest {

	/**
	 * The examples whose hashes are checked.
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
	 * The sources that differ from the ones their examples were made for, in
	 * sv-witnesses at commit {@code 0ef19deb}.
	 */
	private static final Set<String> CHANGED_SOURCES = Set.of(
			"./java/jpf-regression/ExDarko_true/Main.java",
			"./zero_div.py",
			"./safe-simple-concurrent.c");

	/**
	 * Checks that the hashes of the examples are the ones of their sources,
	 * apart from the sources known to differ.
	 */
	@Test
	public void theHashesAreTheOnesOfTheSources(
			@TempDir Path dir)
			throws Exception {
		Path examples = Path.of("witness-testcases");
		Path property = Files.writeString(dir.resolve("any.prp"), "", StandardCharsets.UTF_8);
		for (String name : EXAMPLES) {
			Task task = ExamplesFromBuildersTest.example(name).getEntries().get(0).getMetadata().getTask();
			for (Map.Entry<String, String> hash : task.getInputFileHashes().entrySet()) {
				Path source = examples.resolve(hash.getKey()).normalize();
				String computed = TaskLoader.load(List.of(source), property, task.getLanguage(), task.getDataModel())
						.getInputFileHashes().values().iterator().next();
				if (CHANGED_SOURCES.contains(hash.getKey()))
					assertNotEquals(hash.getValue(), computed, name + ": " + hash.getKey());
				else
					assertEquals(hash.getValue(), computed, name + ": " + hash.getKey());
			}
		}
	}
}
