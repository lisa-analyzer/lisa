package it.unive.lisa.witness.v2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class LocationTest {

	@Test
	public void aLocationRejectsLineZero() {
		assertThrows(InvalidWitnessException.class, () -> new Location("Main.java", 0));
	}

	@Test
	public void aLocationRejectsColumnZero() {
		assertThrows(InvalidWitnessException.class, () -> new Location("Main.java", 1, 0));
	}

	@Test
	public void columnAndFunctionAreOptional() {
		Location location = new Location("Main.java", 3);
		assertNull(location.getColumn());
		assertNull(location.getFunction());
	}

	@Test
	public void equalsAndHashCodeConsiderAllFields() {
		Location a = new Location("Main.java", 3, 7, "main");
		Location b = new Location("Main.java", 3, 7, "main");
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
		assertNotEquals(a, new Location("Main.java", 3, 8, "main"));
		assertNotEquals(a, new Location("Main.java", 3, 7));
	}

	@Test
	public void aLocationNamesItsFunction() {
		assertEquals("Main.java:3:5 (main)", new Location("Main.java", 3, 5, "main").toString());
	}
}
