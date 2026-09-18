package it.unive.lisa.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.TestLanguageFeatures;
import it.unive.lisa.TestTypeSystem;
import it.unive.lisa.program.ClassUnit;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeMemberDescriptor;
import it.unive.lisa.program.cfg.statement.DefaultParamInitialization;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class TypeTest {

	// a bare implementation of Type, with none of the marker sub-interfaces:
	// every isX() default should be false, every asX() default should be null
	private static class PlainType
			implements
			Type {

		// AccessModifiers#testVisibilityOfTypes requires every concrete
		// Type to expose a public/protected no-arg constructor
		public PlainType() {
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

	private static class FakeNumericType
			implements
			NumericType {

		public FakeNumericType() {
		}

		@Override
		public int getNBits() {
			return 32;
		}

		@Override
		public boolean isUnsigned() {
			return false;
		}

		@Override
		public boolean isIntegral() {
			return true;
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

	private static class FakeArrayType
			implements
			ArrayType {

		public FakeArrayType() {
		}

		@Override
		public Type getInnerType() {
			return this;
		}

		@Override
		public Type getBaseType() {
			return this;
		}

		@Override
		public int getDimensions() {
			return 1;
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
	public void plainTypeMatchesNoMarkerInterface() {
		PlainType t = new PlainType();
		assertFalse(t.isNumericType());
		assertFalse(t.isCharacterType());
		assertFalse(t.isBooleanType());
		assertFalse(t.isStringType());
		assertFalse(t.isUntyped());
		assertFalse(t.isVoidType());
		assertFalse(t.isPointerType());
		assertFalse(t.isInMemoryType());
		assertFalse(t.isArrayType());
		assertFalse(t.isNullType());
		assertFalse(t.isUnitType());
		assertFalse(t.isErrorType());
		assertFalse(t.isTypeTokenType());
		assertFalse(t.isReferenceType());

		assertNull(t.asNumericType());
		assertNull(t.asCharacterType());
		assertNull(t.asBooleanType());
		assertNull(t.asStringType());
		assertNull(t.asUntyped());
		assertNull(t.asVoidType());
		assertNull(t.asPointerType());
		assertNull(t.asInMemoryType());
		assertNull(t.asArrayType());
		assertNull(t.asNullType());
		assertNull(t.asUnitType());
		assertNull(t.asErrorType());
		assertNull(t.asTypeTokenType());
		assertNull(t.asReferenceType());
	}

	@Test
	public void numericTypeDispatchesToItsOwnMarkerOnly() {
		FakeNumericType t = new FakeNumericType();
		assertTrue(t.isNumericType());
		assertSame(t, t.asNumericType());
		assertFalse(t.isBooleanType());
		assertFalse(t.isStringType());
		assertFalse(t.isArrayType());
	}

	@Test
	public void arrayTypeIsAlsoAnInMemoryType() {
		// ArrayType extends InMemoryType: both markers should be recognized
		FakeArrayType t = new FakeArrayType();
		assertTrue(t.isArrayType());
		assertSame(t, t.asArrayType());
		assertTrue(t.isInMemoryType());
		assertSame(t, t.asInMemoryType());
		assertFalse(t.isUnitType());
		assertFalse(t.isNullType());
	}

	@Test
	public void nullTypeAndReferenceTypeDispatchCorrectly() {
		// NullType is an InMemoryType, ReferenceType is a PointerType: use
		// the real concrete implementations to double-check the dispatch
		// against actual production classes, not just fakes
		assertTrue(NullType.INSTANCE.isNullType());
		assertTrue(NullType.INSTANCE.isInMemoryType());
		assertFalse(NullType.INSTANCE.isPointerType());

		ReferenceType ref = new ReferenceType(Untyped.INSTANCE);
		assertTrue(ref.isReferenceType());
		assertTrue(ref.isPointerType());
		assertSame(ref, ref.asPointerType());
		assertFalse(ref.isInMemoryType());
	}

	@Test
	public void isValueTypeIsFalseOnlyForInMemoryOrPointerTypes() {
		assertTrue(Untyped.INSTANCE.isValueType());
		assertTrue(VoidType.INSTANCE.isValueType());
		assertFalse(NullType.INSTANCE.isValueType());
		assertFalse(new ReferenceType(Untyped.INSTANCE).isValueType());
	}

	@Test
	public void commonSupertypeOfCollectionFoldsPairwise() {
		Type fallback = Untyped.INSTANCE;
		assertSame(fallback, Type.commonSupertype(null, fallback));
		assertSame(fallback, Type.commonSupertype(Collections.emptyList(), fallback));
		assertSame(VoidType.INSTANCE, Type.commonSupertype(Collections.singletonList(VoidType.INSTANCE), fallback));

		// two equal VoidTypes fold to VoidType itself
		assertSame(
				VoidType.INSTANCE,
				Type.commonSupertype(Arrays.asList(VoidType.INSTANCE, VoidType.INSTANCE), fallback));

		// a VoidType and an unrelated type have no common supertype but
		// Untyped
		assertSame(
				Untyped.INSTANCE,
				Type.commonSupertype(Arrays.asList(VoidType.INSTANCE, NullType.INSTANCE), fallback));
	}

	@Test
	public void castIsConversionDefaultsToFalse() {
		assertFalse(new PlainType().castIsConversion());
	}

	@Test
	public void defaultValueDefaultsToNull() {
		assertNull(new PlainType().defaultValue(null, null));
	}

	@Test
	public void unknownValueDefaultsToADefaultParamInitializationOfThisType() {
		ClassUnit unit = new ClassUnit(
				new SourceCodeLocation("fake", 1, 0),
				new Program(new TestLanguageFeatures(), new TestTypeSystem()),
				"fake",
				false);
		CodeMemberDescriptor descriptor = new CodeMemberDescriptor(
				new SourceCodeLocation("fake", 2, 0),
				unit,
				false,
				"foo");
		CFG cfg = new CFG(descriptor);
		SourceCodeLocation loc = new SourceCodeLocation("fake", 3, 0);

		PlainType type = new PlainType();
		Object result = type.unknownValue(cfg, loc);
		assertNotNull(result);
		assertTrue(result instanceof DefaultParamInitialization);
		assertEquals(type, ((DefaultParamInitialization) result).getStaticType());
	}

}
