package it.unive.lisa.witness.v2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/**
 * Tests of the locations of a witness.
 * 
 * @author <a href="mailto:giacomo.zanatta@unive.it">Giacomo Zanatta</a>
 */
public class LocationTest {

	/**
	 * Checks that a location rejects line zero.
	 */
	@Test
	public void aLocationRejectsLineZero() {
		assertThrows(InvalidWitnessException.class, () -> new Location("Main.java", 0));
	}

	/**
	 * Checks that a location rejects column zero.
	 */
	@Test
	public void aLocationRejectsColumnZero() {
		assertThrows(InvalidWitnessException.class, () -> new Location("Main.java", 1, 0));
	}

	/**
	 * Checks that column and function are optional.
	 */
	@Test
	public void columnAndFunctionAreOptional() {
		Location location = new Location("Main.java", 3);
		assertNull(location.getColumn());
		assertNull(location.getFunction());
	}

	/**
	 * Checks that {@code equals} and {@code hashCode} consider all fields.
	 */
	@Test
	public void equalsAndHashCodeConsiderAllFields() {
		Location a = new Location("Main.java", 3, 7, "main");
		Location b = new Location("Main.java", 3, 7, "main");
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
		assertNotEquals(a, new Location("Main.java", 3, 8, "main"));
		assertNotEquals(a, new Location("Main.java", 3, 7));
	}

	/**
	 * Checks that the text of a location names its function.
	 */
	@Test
	public void aLocationNamesItsFunction() {
		assertEquals("Main.java:3:5 (main)", new Location("Main.java", 3, 5, "main").toString());
	}
}
