package it.unive.lisa.witness.v2;

import static it.unive.lisa.witness.v2.TestValues.at;
import static org.junit.jupiter.api.Assertions.assertThrows;

import it.unive.lisa.witness.v2.correctness.ghost.Expression;
import it.unive.lisa.witness.v2.correctness.ghost.GhostAssignment;
import it.unive.lisa.witness.v2.correctness.ghost.GhostContent;
import it.unive.lisa.witness.v2.correctness.ghost.GhostScope;
import it.unive.lisa.witness.v2.correctness.ghost.GhostUpdate;
import it.unive.lisa.witness.v2.correctness.ghost.GhostVariable;
import java.util.List;
import org.junit.jupiter.api.Test;

public class GhostTest {

	@Test
	public void ghostVariablesAreDeclaredOnce() {
		GhostVariable g = new GhostVariable("g", "int", GhostScope.GLOBAL, new Expression("0", Format.C_EXPRESSION));
		assertThrows(InvalidWitnessException.class, () -> new GhostContent(List.of(g, g), List.of()));
	}

	@Test
	public void ghostUpdatesAssignDeclaredVariables() {
		GhostVariable g = new GhostVariable("g", "int", GhostScope.GLOBAL, new Expression("0", Format.C_EXPRESSION));
		GhostUpdate update = new GhostUpdate(at(1), List.of(new GhostAssignment("h", "1", Format.C_EXPRESSION)));
		assertThrows(InvalidWitnessException.class, () -> new GhostContent(List.of(g), List.of(update)));
	}

	@Test
	public void aGhostUpdateNeedsAnAssignment() {
		assertThrows(InvalidWitnessException.class, () -> new GhostUpdate(at(1), List.of()));
	}
}
