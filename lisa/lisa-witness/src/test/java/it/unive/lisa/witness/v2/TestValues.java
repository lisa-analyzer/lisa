package it.unive.lisa.witness.v2;

import it.unive.lisa.witness.v2.metadata.Metadata;
import it.unive.lisa.witness.v2.metadata.Producer;
import it.unive.lisa.witness.v2.metadata.Task;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Values shared by the tests: a Java file, a hash, and the tasks, locations and
 * metadata built on them.
 * 
 * @author <a href="mailto:giacomo.zanatta@unive.it">Giacomo Zanatta</a>
 */
public final class TestValues {

	/**
	 * The name of the input file of the tasks of the tests.
	 */
	public static final String FILE = "Main.java";

	/**
	 * A valid SHA-256 hash, the one of an empty file.
	 */
	public static final String HASH = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";

	/**
	 * This class only has static methods, and it is never instantiated.
	 */
	private TestValues() {
	}

	/**
	 * Returns a location in {@link #FILE}.
	 * 
	 * @param line the line of the location
	 * 
	 * @return the location
	 */
	public static Location at(
			int line) {
		return new Location(FILE, line);
	}

	/**
	 * Returns a task of the given language, with {@link #FILE} as its only
	 * input file. C tasks get the data model {@link DataModel#LP64}.
	 * 
	 * @param language the language of the task
	 * 
	 * @return the task
	 */
	public static Task task(
			Language language) {
		return new Task(List.of(FILE), Map.of(FILE, HASH), "CHECK( init(main()), LTL(G assert) )", language,
				language.requiresDataModel() ? DataModel.LP64 : null);
	}

	/**
	 * Returns fixed metadata for a task of the given language.
	 * 
	 * @param language the language of the task
	 * 
	 * @return the metadata
	 */
	public static Metadata metadata(
			Language language) {
		return new Metadata("2.4", UUID.fromString("be742527-8e82-4418-9ba2-6cbb857e25a7"),
				Instant.parse("2026-09-08T17:36:11Z"), new Producer("Test", "1"), task(language));
	}
}
