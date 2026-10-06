package it.unive.lisa.symbolic.value.operator.binary;

import it.unive.lisa.type.BooleanType;
import it.unive.lisa.type.CharacterType;
import it.unive.lisa.type.NumericType;
import it.unive.lisa.type.StringType;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

// shared value-based fakes for testing typeInference() across this package's
// operator classes; kept minimal and reused instead of duplicated per file
class OperatorTestFixtures {

	private OperatorTestFixtures() {
	}

	static class FakeNumericType
			implements
			NumericType {

		final String label;
		private final int nbits;
		private final boolean unsigned;
		private final boolean integral;

		FakeNumericType(
				String label,
				int nbits,
				boolean unsigned,
				boolean integral) {
			this.label = label;
			this.nbits = nbits;
			this.unsigned = unsigned;
			this.integral = integral;
		}

		@Override
		public int getNBits() {
			return nbits;
		}

		@Override
		public boolean isUnsigned() {
			return unsigned;
		}

		@Override
		public boolean isIntegral() {
			return integral;
		}

		@Override
		public boolean canBeAssignedTo(
				Type other) {
			return equals(other);
		}

		@Override
		public Type commonSupertype(
				Type other) {
			return equals(other) ? this : it.unive.lisa.type.Untyped.INSTANCE;
		}

		@Override
		public Set<Type> allInstances(
				TypeSystem types) {
			return Collections.singleton(this);
		}

		@Override
		public boolean equals(
				Object obj) {
			return obj instanceof FakeNumericType && label.equals(((FakeNumericType) obj).label);
		}

		@Override
		public int hashCode() {
			return label.hashCode();
		}

		@Override
		public String toString() {
			return label;
		}
	}

	static class FakeBooleanType
			implements
			BooleanType {

		// AccessModifiers#testVisibilityOfTypes requires every concrete
		// Type to expose a public/protected no-arg constructor
		public FakeBooleanType() {
		}

		@Override
		public boolean canBeAssignedTo(
				Type other) {
			return other instanceof FakeBooleanType;
		}

		@Override
		public Type commonSupertype(
				Type other) {
			return equals(other) ? this : it.unive.lisa.type.Untyped.INSTANCE;
		}

		@Override
		public Set<Type> allInstances(
				TypeSystem types) {
			return Collections.singleton(this);
		}

		@Override
		public boolean equals(
				Object obj) {
			return obj instanceof FakeBooleanType;
		}

		@Override
		public int hashCode() {
			return FakeBooleanType.class.hashCode();
		}
	}

	static class FakeStringType
			implements
			StringType {

		public FakeStringType() {
		}

		@Override
		public boolean canBeAssignedTo(
				Type other) {
			return other instanceof FakeStringType;
		}

		@Override
		public Type commonSupertype(
				Type other) {
			return equals(other) ? this : it.unive.lisa.type.Untyped.INSTANCE;
		}

		@Override
		public Set<Type> allInstances(
				TypeSystem types) {
			return Collections.singleton(this);
		}

		@Override
		public boolean equals(
				Object obj) {
			return obj instanceof FakeStringType;
		}

		@Override
		public int hashCode() {
			return FakeStringType.class.hashCode();
		}
	}

	static class FakeCharacterType
			implements
			CharacterType {

		public FakeCharacterType() {
		}

		@Override
		public boolean canBeAssignedTo(
				Type other) {
			return other instanceof FakeCharacterType;
		}

		@Override
		public Type commonSupertype(
				Type other) {
			return equals(other) ? this : it.unive.lisa.type.Untyped.INSTANCE;
		}

		@Override
		public Set<Type> allInstances(
				TypeSystem types) {
			return Collections.singleton(this);
		}

		@Override
		public boolean equals(
				Object obj) {
			return obj instanceof FakeCharacterType;
		}

		@Override
		public int hashCode() {
			return FakeCharacterType.class.hashCode();
		}
	}

	// a generic value-based Type usable as an operand for TypeCast/TypeCheck
	// tests, with explicit control over what it can be assigned to
	static class Labeled
			implements
			Type {

		final String label;
		private final Set<Type> assignableTo;

		Labeled(
				String label,
				Type... assignableTo) {
			this.label = label;
			this.assignableTo = new HashSet<>(Arrays.asList(assignableTo));
		}

		@Override
		public boolean canBeAssignedTo(
				Type other) {
			return equals(other) || assignableTo.contains(other);
		}

		@Override
		public Type commonSupertype(
				Type other) {
			return equals(other) ? this : it.unive.lisa.type.Untyped.INSTANCE;
		}

		@Override
		public Set<Type> allInstances(
				TypeSystem types) {
			return Collections.singleton(this);
		}

		@Override
		public boolean equals(
				Object obj) {
			return obj instanceof Labeled && label.equals(((Labeled) obj).label);
		}

		@Override
		public int hashCode() {
			return Objects.hash(label);
		}

		@Override
		public String toString() {
			return label;
		}
	}

	static final FakeBooleanType BOOL = new FakeBooleanType();
	static final FakeStringType STR = new FakeStringType();
	static final FakeCharacterType CHAR = new FakeCharacterType();
	static final FakeNumericType INT32 = new FakeNumericType("int32", 32, false, true);
	static final FakeNumericType FLOAT32 = new FakeNumericType("float32", 32, false, false);

	static class FakeTypeSystem
			extends
			TypeSystem {

		@Override
		public BooleanType getBooleanType() {
			return BOOL;
		}

		@Override
		public StringType getStringType() {
			return STR;
		}

		@Override
		public NumericType getIntegerType() {
			return INT32;
		}

		@Override
		public CharacterType getCharacterType() {
			return CHAR;
		}

		@Override
		public boolean canBeReferenced(
				Type type) {
			return false;
		}

		@Override
		public int distanceBetweenTypes(
				Type first,
				Type second) {
			return 0;
		}
	}

}
