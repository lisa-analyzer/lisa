package it.unive.lisa.symbolic.value.operator.binary;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.symbolic.value.operator.binary.NumericOperationTest.FakeNonNumericType;
import it.unive.lisa.symbolic.value.operator.binary.NumericOperationTest.FakeNumericType;
import it.unive.lisa.symbolic.value.operator.binary.NumericOperationTest.FakeTypeSystem;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class NumericComparisonTest {

	private static final FakeTypeSystem TS = new FakeTypeSystem();
	private static final FakeNumericType INT32 = new FakeNumericType(32, false, true);
	private static final FakeNumericType UINT32 = new FakeNumericType(32, true, true);

	// a concrete, representative leaf class: it must not override
	// typeInference, so exercising it exercises NumericComparison itself
	private static final NumericComparison OP = ComparisonGe.INSTANCE;

	private static Set<Type> set(
			Type... types) {
		return new HashSet<>(Arrays.asList(types));
	}

	@Test
	public void bothSidesComparableYieldsTheBooleanTypeOfTheTypeSystem() {
		assertEquals(Collections.singleton(TS.getBooleanType()), OP.typeInference(TS, set(INT32), set(INT32)));
	}

	@Test
	public void bothSidesNumericButIncompatibleStillYieldsBoolean() {
		// unlike NumericOperation, the result type here does not depend on
		// which numeric type "wins": any successful comparison always
		// produces a boolean, so an incompatible pair still yields boolean
		assertEquals(Collections.singleton(TS.getBooleanType()), OP.typeInference(TS, set(INT32), set(UINT32)));
	}

	@Test
	public void oneSidePurelyUntypedStillYieldsBoolean() {
		// regression test, mirroring NumericOperationTest: Untyped paired
		// with a concrete numeric type must not be discarded
		assertEquals(Collections.singleton(TS.getBooleanType()),
				OP.typeInference(TS, set(Untyped.INSTANCE), set(INT32)));
	}

	@Test
	public void bothSidesPurelyUntypedYieldsEmpty() {
		assertTrue(OP.typeInference(TS, set(Untyped.INSTANCE), set(Untyped.INSTANCE)).isEmpty());
	}

	@Test
	public void neitherSideHasAnyNumericOrUntypedTypeYieldsEmpty() {
		assertTrue(OP.typeInference(TS, set(new FakeNonNumericType()), set(new FakeNonNumericType())).isEmpty());
	}

}
