package it.unive.lisa.witness.v2.violation;

import static it.unive.lisa.witness.v2.TestValues.at;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import it.unive.lisa.witness.v2.Format;
import it.unive.lisa.witness.v2.InvalidWitnessException;
import org.junit.jupiter.api.Test;

public class WaypointTest {

	private static Waypoint waypoint(
			WaypointType type,
			Action action,
			Constraint constraint) {
		return new Waypoint(type, action, at(1), constraint, null);
	}

	@Test
	public void aTargetRejectsActionAvoid() {
		assertThrows(InvalidWitnessException.class, () -> waypoint(WaypointType.TARGET, Action.AVOID, null));
	}

	@Test
	public void aTargetRejectsAConstraint() {
		assertThrows(InvalidWitnessException.class, () -> waypoint(WaypointType.TARGET, Action.FOLLOW,
				Constraint.of("x > 0", Format.JAVA_EXPRESSION)));
	}

	@Test
	public void aFunctionEnterRejectsAConstraint() {
		assertThrows(InvalidWitnessException.class, () -> waypoint(WaypointType.FUNCTION_ENTER, Action.FOLLOW,
				Constraint.of("x > 0", Format.JAVA_EXPRESSION)));
	}

	@Test
	public void aFunctionReturnNeedsAConstraint() {
		assertThrows(InvalidWitnessException.class,
				() -> waypoint(WaypointType.FUNCTION_RETURN, Action.FOLLOW, null));
	}

	@Test
	public void aFunctionReturnRejectsANonExtendedFormat() {
		assertThrows(InvalidWitnessException.class, () -> waypoint(WaypointType.FUNCTION_RETURN, Action.FOLLOW,
				Constraint.of("\\result == 0", Format.JAVA_EXPRESSION)));
	}

	@Test
	public void aFunctionReturnAcceptsAnExtendedFormat() {
		assertDoesNotThrow(() -> Waypoint.functionReturn(at(1), "\\result == 0", Format.EXT_JAVA_EXPRESSION));
	}

	@Test
	public void anAssumptionRejectsAnExtendedFormat() {
		assertThrows(InvalidWitnessException.class, () -> waypoint(WaypointType.ASSUMPTION, Action.FOLLOW,
				Constraint.of("x > 0", Format.EXT_JAVA_EXPRESSION)));
	}

	@Test
	public void anAssumptionAcceptsAPlainFormat() {
		assertDoesNotThrow(() -> waypoint(WaypointType.ASSUMPTION, Action.FOLLOW,
				Constraint.of("x > 0", Format.JAVA_EXPRESSION)));
	}

	@Test
	public void aBranchingRejectsAFormat() {
		assertThrows(InvalidWitnessException.class, () -> waypoint(WaypointType.BRANCHING, Action.FOLLOW,
				Constraint.of("true", Format.JAVA_EXPRESSION)));
	}

	@Test
	public void aBranchingAcceptsAConstraintWithoutFormat() {
		assertDoesNotThrow(
				() -> waypoint(WaypointType.BRANCHING, Action.AVOID, Constraint.withoutFormat("true")));
	}

	@Test
	public void aNegativeThreadIdIsRejected() {
		assertThrows(InvalidWitnessException.class,
				() -> new Waypoint(WaypointType.TARGET, Action.FOLLOW, at(1), null, -1));
	}

	@Test
	public void equalsAndHashCodeConsiderAllFields() {
		Waypoint a = Waypoint.functionReturn(at(3), "\\result == 0", Format.EXT_JAVA_EXPRESSION);
		Waypoint b = Waypoint.functionReturn(at(3), "\\result == 0", Format.EXT_JAVA_EXPRESSION);
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
		assertNotEquals(a, Waypoint.functionReturn(at(3), "\\result == 1", Format.EXT_JAVA_EXPRESSION));
		assertNotEquals(a, Waypoint.target(at(3)));
	}
}
