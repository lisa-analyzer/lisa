package it.unive.lisa.witness.v2.metadata;

import static it.unive.lisa.witness.v2.TestValues.HASH;
import static it.unive.lisa.witness.v2.TestValues.task;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import it.unive.lisa.witness.v2.DataModel;
import it.unive.lisa.witness.v2.InvalidWitnessException;
import it.unive.lisa.witness.v2.Language;
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
}
