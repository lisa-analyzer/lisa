package it.unive.lisa.triage.pdr;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import it.unive.lisa.program.SyntheticLocation;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.type.Untyped;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class CubeTest {

	private static final Variable X = new Variable(Untyped.INSTANCE, "x", SyntheticLocation.INSTANCE);

	private static final Variable Y = new Variable(Untyped.INSTANCE, "y", SyntheticLocation.INSTANCE);

	@Test
	public void weakening() {
		Cube cube = new Cube(Map.of(X, "(Value.String \"a\")", Y, "3"));
		Cube weak = cube.weaken(X);
		assertEquals(new Cube(Map.of(Y, "3"), Map.of(X, "Value.String")), weak);
		assertEquals(Set.of(X, Y), weak.getIdentifiers());
		// plain integers and nullary constructors cannot be weakened
		assertNull(cube.weaken(Y));
		assertNull(new Cube(Map.of(X, "Value.Null")).weaken(X));
		// constructor tests are dropped like equalities
		assertEquals(new Cube(Map.of(Y, "3")), weak.without(X));
	}
}
