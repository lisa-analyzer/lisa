package it.unive.lisa.imp.types;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.imp.IMPFrontend;
import it.unive.lisa.imp.ParsingException;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.ProgramValidationException;
import it.unive.lisa.program.type.BoolType;
import it.unive.lisa.program.type.Int32Type;
import it.unive.lisa.program.type.Int64Type;
import it.unive.lisa.program.type.StringType;
import it.unive.lisa.type.NullType;
import it.unive.lisa.type.ReferenceType;
import it.unive.lisa.type.Untyped;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class IMPTypeSystemTest {

	private static final String SOURCE = "class a { ~a() { } }\n"
			+ "class b extends a { ~b() { } }\n"
			+ "class c extends a { ~c() { } }\n";

	private final IMPTypeSystem types = new IMPTypeSystem();

	private ClassType a;

	private ClassType b;

	private ClassType c;

	@BeforeEach
	public void setup()
			throws ParsingException,
			ProgramValidationException {
		ClassType.clearAll();
		ArrayType.clearAll();
		Program prog = IMPFrontend.processText(SOURCE);
		prog.getFeatures().getProgramValidationLogic().validateAndFinalize(prog);
		a = ClassType.lookup("a");
		b = ClassType.lookup("b");
		c = ClassType.lookup("c");
	}

	@Test
	public void reportsTheLanguagesBuiltinTypes() {
		assertEquals(BoolType.INSTANCE, types.getBooleanType());
		assertEquals(StringType.INSTANCE, types.getStringType());
		assertEquals(Int32Type.INSTANCE, types.getIntegerType());
	}

	@Test
	public void impHasNoCharacterType() {
		assertThrows(UnsupportedOperationException.class, () -> types.getCharacterType());
	}

	@Test
	public void onlyInMemoryOrUntypedValuesCanBeReferenced() {
		assertTrue(types.canBeReferenced(a));
		assertTrue(types.canBeReferenced(ArrayType.register(Int32Type.INSTANCE, 1)));
		assertTrue(types.canBeReferenced(Untyped.INSTANCE));
		assertFalse(types.canBeReferenced(Int32Type.INSTANCE));
		assertFalse(types.canBeReferenced(BoolType.INSTANCE));
		assertFalse(types.canBeReferenced(StringType.INSTANCE));
	}

	@Test
	public void untypedIsAlwaysAtDistanceZero() {
		assertEquals(0, types.distanceBetweenTypes(Untyped.INSTANCE, Int32Type.INSTANCE));
		assertEquals(0, types.distanceBetweenTypes(Int32Type.INSTANCE, Untyped.INSTANCE));
	}

	@Test
	public void identicalNumericTypesAreAtDistanceZeroDifferentOnesAtOne() {
		assertEquals(0, types.distanceBetweenTypes(Int32Type.INSTANCE, Int32Type.INSTANCE));
		assertEquals(1, types.distanceBetweenTypes(Int32Type.INSTANCE, Int64Type.INSTANCE));
	}

	@Test
	public void matchingBooleanOrStringTypesAreAtDistanceZero() {
		assertEquals(0, types.distanceBetweenTypes(BoolType.INSTANCE, BoolType.INSTANCE));
		assertEquals(0, types.distanceBetweenTypes(StringType.INSTANCE, StringType.INSTANCE));
	}

	@Test
	public void unrelatedPrimitiveKindsAreNotComparable() {
		assertEquals(-1, types.distanceBetweenTypes(Int32Type.INSTANCE, BoolType.INSTANCE));
		assertEquals(-1, types.distanceBetweenTypes(StringType.INSTANCE, Int32Type.INSTANCE));
	}

	@Test
	public void aReferenceToNullIsAlwaysAtDistanceZero() {
		ReferenceType refA = new ReferenceType(a);
		ReferenceType refNull = new ReferenceType(NullType.INSTANCE);
		assertEquals(0, types.distanceBetweenTypes(refA, refNull));
	}

	@Test
	public void referenceToTheSameUnitTypeIsAtDistanceZero() {
		ReferenceType refA1 = new ReferenceType(a);
		ReferenceType refA2 = new ReferenceType(a);
		assertEquals(0, types.distanceBetweenTypes(refA1, refA2));
	}

	@Test
	public void referenceToAnAncestorUnitTypeIncreasesWithDistance() {
		// per ParameterMatchingStrategy's actual usage, distanceBetweenTypes
		// is called as (formalParamType, runtimeArgumentType): b directly
		// extends a, so passing a "b" actual to a param formally declared as
		// "a" is 1 hop up the hierarchy
		ReferenceType formalA = new ReferenceType(a);
		ReferenceType runtimeB = new ReferenceType(b);
		assertEquals(1, types.distanceBetweenTypes(formalA, runtimeB));
	}

	@Test
	public void referenceToAnUnrelatedUnitTypeIsNotComparable() {
		ReferenceType formalB = new ReferenceType(b);
		ReferenceType runtimeC = new ReferenceType(c);
		assertEquals(-1, types.distanceBetweenTypes(formalB, runtimeC));
	}

	@Test
	public void referenceToTheSameArrayTypeIsAtDistanceZero() {
		ReferenceType refInts1 = new ReferenceType(ArrayType.register(Int32Type.INSTANCE, 1));
		ReferenceType refInts2 = new ReferenceType(ArrayType.register(Int32Type.INSTANCE, 1));
		assertEquals(0, types.distanceBetweenTypes(refInts1, refInts2));
	}

	// SUSPECTED BUG: distanceBetweenTypes special-cases array/array
	// reference comparisons as "0 if the array types are exactly equal(),
	// otherwise -1 (not comparable)" - but ArrayType.canBeAssignedTo is
	// documented/implemented to be covariant on the base type (int32[] can
	// be assigned to int64[], since int32 can be assigned to int64). This
	// means a param declared as int64[] passed an actual argument of type
	// int32[] is a legal call per canBeAssignedTo, yet distanceBetweenTypes
	// reports it as -1 (not comparable / unusable for parameter matching),
	// contradicting the very assignability rule the array type itself
	// defines. Expecting a non-negative distance here, not -1.
	@Test
	public void referenceToACovariantlyAssignableArrayTypeShouldBeComparable() {
		// int32[] can be assigned to int64[] (ArrayType.canBeAssignedTo is
		// covariant on the base type), so passing an int32[] actual to a
		// param formally declared as int64[] is a legal call
		ReferenceType formalLongs = new ReferenceType(ArrayType.register(Int64Type.INSTANCE, 1));
		ReferenceType runtimeInts = new ReferenceType(ArrayType.register(Int32Type.INSTANCE, 1));
		int distance = types.distanceBetweenTypes(formalLongs, runtimeInts);
		assertTrue(distance >= 0,
				"int32[] is assignable to int64[] per ArrayType.canBeAssignedTo, so it should be comparable, but distance was "
						+ distance);
	}

}
