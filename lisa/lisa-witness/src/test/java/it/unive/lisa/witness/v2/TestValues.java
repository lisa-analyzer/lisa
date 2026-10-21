package it.unive.lisa.witness.v2;

import it.unive.lisa.witness.v2.metadata.Metadata;
import it.unive.lisa.witness.v2.metadata.Producer;
import it.unive.lisa.witness.v2.metadata.Task;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class TestValues {

	public static final String FILE = "Main.java";

	public static final String HASH = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";

	private TestValues() {
	}

	public static Location at(
			int line) {
		return new Location(FILE, line);
	}

	public static Task task(
			Language language) {
		return new Task(List.of(FILE), Map.of(FILE, HASH), "CHECK( init(main()), LTL(G assert) )", language,
				language.requiresDataModel() ? DataModel.LP64 : null);
	}

	public static Metadata metadata(
			Language language) {
		return new Metadata("2.4", UUID.fromString("be742527-8e82-4418-9ba2-6cbb857e25a7"),
				Instant.parse("2026-09-08T17:36:11Z"), new Producer("Test", "1"), task(language));
	}
}
