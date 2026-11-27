package it.unive.lisa.type;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class BooleanTypeTest {

	private static class FakeBooleanType
			implements
			BooleanType {

		// AccessModifiers#testVisibilityOfTypes requires every concrete
		// Type to expose a public/protected no-arg constructor
		public FakeBooleanType() {
		}

		@Override
		public boolean canBeAssignedTo(
				Type other) {
			return equals(other);
		}

		@Override
		public Type commonSupertype(
				Type other) {
			return equals(other) ? this : Untyped.INSTANCE;
		}

		@Override
		public Set<Type> allInstances(
				TypeSystem types) {
			return Collections.singleton(this);
		}
	}

	@Test
	public void castIsConversionDefaultsToTrueForBooleanTypes() {
		// overrides Type's own default of false, since a boolean cast (e.g.,
		// from a numeric type in languages that allow it) converts the value
		assertTrue(new FakeBooleanType().castIsConversion());
	}

}
