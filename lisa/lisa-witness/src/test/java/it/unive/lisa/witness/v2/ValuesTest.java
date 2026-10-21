package it.unive.lisa.witness.v2;

import static it.unive.lisa.witness.v2.TestValues.at;
import static org.junit.jupiter.api.Assertions.assertThrows;

import it.unive.lisa.witness.v2.correctness.FunctionContract;
import it.unive.lisa.witness.v2.correctness.Invariant;
import it.unive.lisa.witness.v2.correctness.InvariantType;
import it.unive.lisa.witness.v2.correctness.ghost.Expression;
import org.junit.jupiter.api.Test;

public class ValuesTest {

	@Test
	public void blankValuesAreRejected() {
		assertThrows(InvalidWitnessException.class,
				() -> new Invariant(InvariantType.LOCATION_INVARIANT, at(1), " ", Format.JAVA_EXPRESSION));
		assertThrows(InvalidWitnessException.class,
				() -> new FunctionContract(at(1), "", "x > 0", Format.JAVA_EXPRESSION, null));
		assertThrows(InvalidWitnessException.class, () -> new Expression("", Format.C_EXPRESSION));
	}
}
