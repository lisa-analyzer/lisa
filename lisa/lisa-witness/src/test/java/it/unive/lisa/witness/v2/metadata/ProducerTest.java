package it.unive.lisa.witness.v2.metadata;

import static org.junit.jupiter.api.Assertions.assertThrows;

import it.unive.lisa.witness.v2.InvalidWitnessException;
import org.junit.jupiter.api.Test;

/**
 * Tests of the producer of an entry.
 * 
 * @author <a href="mailto:giacomo.zanatta@unive.it">Giacomo Zanatta</a>
 */
public class ProducerTest {

	/**
	 * Checks that the name is required.
	 */
	@Test
	public void theNameIsRequired() {
		assertThrows(InvalidWitnessException.class, () -> new Producer(" ", "1.0"));
	}

	/**
	 * Checks that the version is required.
	 */
	@Test
	public void theVersionIsRequired() {
		assertThrows(InvalidWitnessException.class, () -> new Producer("jlisa", ""));
	}

	/**
	 * Checks that {@code Producer.of} needs a version in the manifest.
	 */
	@Test
	public void ofNeedsAVersionInTheManifest() {
		assertThrows(IllegalStateException.class, () -> Producer.of("Test", ProducerTest.class));
	}
}
