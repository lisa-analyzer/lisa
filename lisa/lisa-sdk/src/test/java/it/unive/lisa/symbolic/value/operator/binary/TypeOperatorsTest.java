package it.unive.lisa.symbolic.value.operator.binary;

import static it.unive.lisa.symbolic.value.operator.binary.OperatorTestFixtures.BOOL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.symbolic.value.operator.binary.OperatorTestFixtures.Labeled;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeTokenType;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class TypeOperatorsTest {

	private static final OperatorTestFixtures.FakeTypeSystem TS = new OperatorTestFixtures.FakeTypeSystem();

	private static final Labeled A = new Labeled("A");
	private static final Labeled B = new Labeled("B", A);
	private static final Labeled C = new Labeled("C");
	private static final TypeTokenType TOKEN_A = new TypeTokenType(Collections.singleton(A));

	@Test
	public void typeCastKeepsOnlySourceTypesAssignableToTheToken() {
		assertEquals("cast-as", TypeCast.INSTANCE.toString());
		Set<Type> sources = new HashSet<>(Arrays.asList(B, C));
		Set<Type> result = TypeCast.INSTANCE.typeInference(TS, sources, Collections.singleton(TOKEN_A));
		// B can be assigned to A (configured above), C cannot
		assertEquals(Collections.singleton(B), result);
	}

	@Test
	public void typeCastRejectsANonTokenRightOperand() {
		assertTrue(TypeCast.INSTANCE.typeInference(TS, Collections.singleton(B), Collections.singleton(A)).isEmpty());
	}

	@Test
	public void typeConvYieldsTheTokenTypesReachableFromASource() {
		assertEquals("conv-as", TypeConv.INSTANCE.toString());
		Set<Type> result = TypeConv.INSTANCE.typeInference(TS, Collections.singleton(B),
				Collections.singleton(TOKEN_A));
		// unlike cast, the result is the TARGET type (A), not the source (B)
		assertEquals(Collections.singleton(A), result);
	}

	@Test
	public void typeConvRejectsANonTokenRightOperand() {
		assertTrue(TypeConv.INSTANCE.typeInference(TS, Collections.singleton(B), Collections.singleton(A)).isEmpty());
	}

	@Test
	public void typeCheckYieldsBooleanWheneverTheRightOperandIsAToken() {
		assertEquals("is", TypeCheck.INSTANCE.toString());
		assertEquals(Collections.singleton(BOOL), TypeCheck.INSTANCE.typeInference(TS, Collections.singleton(C),
				Collections.singleton(TOKEN_A)));
	}

	@Test
	public void typeCheckRejectsANonTokenRightOperand() {
		assertTrue(TypeCheck.INSTANCE.typeInference(TS, Collections.singleton(B), Collections.singleton(A)).isEmpty());
	}

}
