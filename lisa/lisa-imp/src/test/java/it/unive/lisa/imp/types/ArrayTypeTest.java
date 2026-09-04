package it.unive.lisa.imp.types;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.imp.IMPFrontend;
import it.unive.lisa.imp.ParsingException;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.ProgramValidationException;
import it.unive.lisa.program.type.Int32Type;
import it.unive.lisa.program.type.Int64Type;
import it.unive.lisa.program.type.StringType;
import it.unive.lisa.type.NullType;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.Untyped;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ArrayTypeTest {

	// a small sibling hierarchy (b, c both extend a, sharing no other common
	// ancestor) used to probe commonSupertype for two array types whose base
	// types are related but neither is assignable to the other
	private static final String SOURCE = "class a { ~a() { } }\n"
			+ "class b extends a { ~b() { } }\n"
			+ "class c extends a { ~c() { } }\n";

	private ClassType a;

	private ClassType b;

	private ClassType c;

	@BeforeEach
	public void setup()
			throws ParsingException,
			ProgramValidationException {
		ArrayType.clearAll();
		ClassType.clearAll();
		Program prog = IMPFrontend.processText(SOURCE);
		prog.getFeatures().getProgramValidationLogic().validateAndFinalize(prog);
		a = ClassType.lookup("a");
		b = ClassType.lookup("b");
		c = ClassType.lookup("c");
	}

	@Test
	public void registerIsIdempotentAndUniqueByBaseAndDimension() {
		ArrayType first = ArrayType.register(Int32Type.INSTANCE, 1);
		ArrayType second = ArrayType.register(Int32Type.INSTANCE, 1);
		assertSame(first, second);
		assertSame(first, ArrayType.lookup(Int32Type.INSTANCE, 1));
		assertNotEquals(first, ArrayType.register(Int64Type.INSTANCE, 1));
	}

	@Test
	public void onlySingleDimensionArraysAreCurrentlySupported() {
		// the constructor explicitly rejects anything other than 1
		// dimension, so registering a multi-dimensional array must fail
		// rather than silently producing a broken instance
		assertThrows(IllegalArgumentException.class, () -> ArrayType.register(Int32Type.INSTANCE, 2));
	}

	@Test
	public void anArrayIsAlwaysAssignableToItsOwnType() {
		ArrayType ints = ArrayType.register(Int32Type.INSTANCE, 1);
		assertTrue(ints.canBeAssignedTo(ints));
		assertTrue(ArrayType.register(Int32Type.INSTANCE, 1).canBeAssignedTo(ints));
	}

	@Test
	public void arraysFollowTheAssignabilityOfTheirBaseType() {
		// per canBeAssignedTo's implementation, array assignability is
		// covariant on the base type: an array is assignable to another
		// array iff its base type is assignable to the other's base type.
		// numeric types are a poor choice to probe this with, since
		// NumericType.canBeAssignedTo is bidirectional ("any numeric type
		// is assignable to any other, with possible loss of information"),
		// so a genuinely asymmetric relationship (a class hierarchy) is used
		// instead
		ArrayType bArray = ArrayType.register(b, 1);
		ArrayType aArray = ArrayType.register(a, 1);
		assertTrue(bArray.canBeAssignedTo(aArray), "b can be assigned to a, so b[] should too");
		assertFalse(aArray.canBeAssignedTo(bArray), "a cannot be assigned to b");
	}

	@Test
	public void unrelatedBaseTypesAreNotAssignable() {
		ArrayType ints = ArrayType.register(Int32Type.INSTANCE, 1);
		ArrayType strings = ArrayType.register(StringType.INSTANCE, 1);
		assertFalse(ints.canBeAssignedTo(strings));
		assertFalse(strings.canBeAssignedTo(ints));
	}

	@Test
	public void anArrayIsNeverAssignableToANonArrayType() {
		ArrayType ints = ArrayType.register(Int32Type.INSTANCE, 1);
		assertFalse(ints.canBeAssignedTo(Int32Type.INSTANCE));
		assertFalse(ints.canBeAssignedTo(Untyped.INSTANCE));
	}

	@Test
	public void commonSupertypeOfAnArrayWithItselfIsItself() {
		ArrayType ints = ArrayType.register(Int32Type.INSTANCE, 1);
		assertEquals(ints, ints.commonSupertype(ArrayType.register(Int32Type.INSTANCE, 1)));
	}

	@Test
	public void commonSupertypeWhenOneArrayIsAssignableToTheOtherIsTheWiderOne() {
		ArrayType bArray = ArrayType.register(b, 1);
		ArrayType aArray = ArrayType.register(a, 1);
		assertEquals(aArray, bArray.commonSupertype(aArray));
		assertEquals(aArray, aArray.commonSupertype(bArray));
	}

	@Test
	public void commonSupertypeWithNullIsTheArrayItself() {
		ArrayType ints = ArrayType.register(Int32Type.INSTANCE, 1);
		assertEquals(ints, ints.commonSupertype(NullType.INSTANCE));
	}

	@Test
	public void commonSupertypeWithANonArrayNonNullTypeIsUntyped() {
		ArrayType ints = ArrayType.register(Int32Type.INSTANCE, 1);
		assertEquals(Untyped.INSTANCE, ints.commonSupertype(Int32Type.INSTANCE));
	}

	// SUSPECTED BUG: when two array types have base types that are related
	// (share a real common ancestor) but neither array is assignable to the
	// other, commonSupertype falls back to
	// "getInnerType().commonSupertype(other.asArrayType().getInnerType())"
	// (see ArrayType.java, marked with "// TODO not sure about this" in the
	// source itself) - this returns the BASE type's common supertype
	// unwrapped, e.g. plain "a" instead of "a[]", even though both operands
	// of commonSupertype were array types and the result of an array/array
	// comparison should itself be an array type. Expecting
	// b[].commonSupertype(c[])
	// to be a[] (an ArrayType wrapping the classes' real common ancestor),
	// not the bare class type "a".
	@Test
	public void commonSupertypeOfArraysWithRelatedButUnassignableBasesShouldStayAnArrayType() {
		ArrayType bArray = ArrayType.register(b, 1);
		ArrayType cArray = ArrayType.register(c, 1);
		Type result = bArray.commonSupertype(cArray);
		assertEquals(ArrayType.register(a, 1), result);
	}

	@Test
	public void equalsAndHashCodeConsiderBothBaseAndDimension() {
		ArrayType ints1 = ArrayType.register(Int32Type.INSTANCE, 1);
		ArrayType ints2 = ArrayType.register(Int32Type.INSTANCE, 1);
		ArrayType longs = ArrayType.register(Int64Type.INSTANCE, 1);
		assertEquals(ints1, ints2);
		assertEquals(ints1.hashCode(), ints2.hashCode());
		assertNotEquals(ints1, longs);
	}

	@Test
	public void accessorsReportTheDeclaredBaseTypeAndDimension() {
		ArrayType ints = ArrayType.register(Int32Type.INSTANCE, 1);
		assertEquals(Int32Type.INSTANCE, ints.getBaseType());
		assertEquals(1, ints.getDimensions());
		assertEquals(Int32Type.INSTANCE, ints.getInnerType());
	}

	@Test
	public void toStringAppendsBracketsPerDimension() {
		ArrayType ints = ArrayType.register(Int32Type.INSTANCE, 1);
		assertEquals("int32[]", ints.toString());
	}

}
