package it.unive.lisa.symbolic.value;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.program.SyntheticLocation;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonGt;
import it.unive.lisa.symbolic.value.operator.binary.ComparisonLt;
import it.unive.lisa.type.Untyped;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class PushFromConstraintsTest {

	private static Constant constant(
			int v) {
		return new Constant(Untyped.INSTANCE, v, SyntheticLocation.INSTANCE);
	}

	private static BinaryExpression constraint(
			int v,
			it.unive.lisa.symbolic.value.operator.binary.BinaryOperator op) {
		return new BinaryExpression(
				Untyped.INSTANCE, constant(v), new PushAny(Untyped.INSTANCE, SyntheticLocation.INSTANCE), op,
				SyntheticLocation.INSTANCE);
	}

	@Test
	public void isAPushAnySpecialization() {
		PushFromConstraints p = new PushFromConstraints(Untyped.INSTANCE, SyntheticLocation.INSTANCE);
		assertTrue(p instanceof PushAny);
	}

	@Test
	public void varargsAndSetConstructorsAreEquivalent() {
		BinaryExpression c1 = constraint(0, ComparisonGt.INSTANCE);
		BinaryExpression c2 = constraint(10, ComparisonLt.INSTANCE);

		PushFromConstraints viaVarargs = new PushFromConstraints(Untyped.INSTANCE, SyntheticLocation.INSTANCE, c1, c2);
		PushFromConstraints viaSet = new PushFromConstraints(
				Untyped.INSTANCE, SyntheticLocation.INSTANCE, Set.of(c1, c2));

		assertEquals(viaVarargs.getConstraints(), viaSet.getConstraints());
		assertEquals(viaVarargs, viaSet);
	}

	@Test
	public void getConstraintsReturnsWhatWasPassedIn() {
		BinaryExpression c1 = constraint(0, ComparisonGt.INSTANCE);
		PushFromConstraints p = new PushFromConstraints(Untyped.INSTANCE, SyntheticLocation.INSTANCE, c1);
		assertEquals(Set.of(c1), p.getConstraints());
	}

	@Test
	public void equalsComparesTheConstraintSetInAdditionToTheInheritedState() {
		BinaryExpression c1 = constraint(0, ComparisonGt.INSTANCE);
		BinaryExpression c2 = constraint(10, ComparisonLt.INSTANCE);

		PushFromConstraints a = new PushFromConstraints(Untyped.INSTANCE, SyntheticLocation.INSTANCE, c1);
		PushFromConstraints b = new PushFromConstraints(Untyped.INSTANCE, SyntheticLocation.INSTANCE, c1);
		PushFromConstraints different = new PushFromConstraints(Untyped.INSTANCE, SyntheticLocation.INSTANCE, c2);

		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
		assertFalse(a.equals(different));
		// a plain PushAny with no constraints must not be equal to one with
		// constraints, even ignoring the class-name difference already
		// enforced by getClass()
		assertFalse(a.equals(new PushAny(Untyped.INSTANCE, SyntheticLocation.INSTANCE)));
	}

	@Test
	public void toStringAppendsTheSortedConstraintsToThePushAnyMarker() {
		BinaryExpression c1 = constraint(0, ComparisonGt.INSTANCE);
		PushFromConstraints p = new PushFromConstraints(Untyped.INSTANCE, SyntheticLocation.INSTANCE, c1);
		assertEquals("PUSHANY [" + c1 + "]", p.toString());
	}

	@Test
	public void emptyConstraintsProduceAnEmptyBracketSuffix() {
		PushFromConstraints p = new PushFromConstraints(Untyped.INSTANCE, SyntheticLocation.INSTANCE);
		assertEquals("PUSHANY []", p.toString());
	}

}
