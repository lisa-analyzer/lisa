package it.unive.lisa.witness.v2.metadata;

import static it.unive.lisa.witness.v2.TestValues.HASH;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import it.unive.lisa.witness.v2.DataModel;
import it.unive.lisa.witness.v2.InvalidWitnessException;
import it.unive.lisa.witness.v2.Language;
import it.unive.lisa.witness.v2.Location;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class TaskTest {

	private static final String SPEC = "CHECK( init(main()), LTL(G ! call(reach_error())) )";

	@Test
	public void aCTaskRequiresADataModel() {
		assertThrows(InvalidWitnessException.class,
				() -> new Task(List.of("a.c"), Map.of("a.c", HASH), SPEC, Language.C, null));
	}

	@Test
	public void aJavaTaskRejectsADataModel() {
		assertThrows(InvalidWitnessException.class, () -> new Task(List.of("Main.java"),
				Map.of("Main.java", HASH), SPEC, Language.JAVA, DataModel.LP64));
	}

	@Test
	public void everyInputFileNeedsAHash() {
		assertThrows(InvalidWitnessException.class, () -> new Task(List.of("Main.java", "Verifier.java"),
				Map.of("Main.java", HASH), SPEC, Language.JAVA, null));
	}

	@Test
	public void loadExpandsDirectoriesAndHashesEveryFile(
			@TempDir Path dir)
			throws IOException {
		Path common = Files.createDirectories(dir.resolve("common"));
		Files.writeString(common.resolve("b.java"), "", StandardCharsets.UTF_8);
		Files.writeString(common.resolve("a.java"), "abc", StandardCharsets.UTF_8);
		Path prp = Files.writeString(dir.resolve("valid-assert.prp"), "CHECK( init(Main.main()), LTL(G assert) )\n");

		Task task = TaskLoader.load(List.of(common), prp, Language.JAVA, null);

		assertEquals(List.of(common.resolve("a.java").toString(), common.resolve("b.java").toString()),
				task.getInputFiles());
		assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
				task.getInputFileHashes().get(common.resolve("a.java").toString()));
		assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
				task.getInputFileHashes().get(common.resolve("b.java").toString()));
		assertEquals("CHECK( init(Main.main()), LTL(G assert) )", task.getSpecification());
	}

	@Test
	public void anInputFileIsListedOnce() {
		assertThrows(InvalidWitnessException.class, () -> new Task(List.of("Main.java", "Main.java"),
				Map.of("Main.java", HASH), SPEC, Language.JAVA, null));
	}

	@Test
	public void aHashIsASha256Hash() {
		assertThrows(InvalidWitnessException.class,
				() -> new Task(List.of("Main.java"), Map.of("Main.java", "00"), SPEC, Language.JAVA, null));
	}

	@Test
	public void aLocationMustBeInAnInputFile() {
		Task task = new Task(List.of("Main.java"), Map.of("Main.java", HASH), SPEC, Language.JAVA, null);
		assertDoesNotThrow(() -> task.checkLocation(new Location("Main.java", 1)));
		assertThrows(InvalidWitnessException.class, () -> task.checkLocation(new Location("Other.java", 1)));
	}
}
