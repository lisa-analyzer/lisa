package it.unive.lisa.witness.v2.metadata;

import static it.unive.lisa.witness.v2.TestValues.task;
import static org.junit.jupiter.api.Assertions.assertThrows;

import it.unive.lisa.witness.v2.InvalidWitnessException;
import it.unive.lisa.witness.v2.Language;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

public class MetadataTest {

	private static final Producer PRODUCER = new Producer("Test", "1");

	@Test
	public void onlyFormatVersion24CanBeBuilt() {
		assertThrows(InvalidWitnessException.class, () -> new Metadata("2.3", UUID.randomUUID(), Instant.now(),
				PRODUCER, task(Language.JAVA)));
	}
}
