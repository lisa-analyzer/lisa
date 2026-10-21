package it.unive.lisa.witness.v2.metadata;

import static org.junit.jupiter.api.Assertions.assertThrows;

import it.unive.lisa.witness.v2.InvalidWitnessException;
import org.junit.jupiter.api.Test;

public class ProducerTest {

	@Test
	public void theNameIsRequired() {
		assertThrows(InvalidWitnessException.class, () -> new Producer(" ", "1.0"));
	}

	@Test
	public void theVersionIsRequired() {
		assertThrows(InvalidWitnessException.class, () -> new Producer("jlisa", ""));
	}

	@Test
	public void ofNeedsAVersionInTheManifest() {
		assertThrows(IllegalStateException.class, () -> Producer.of("Test", ProducerTest.class));
	}
}
