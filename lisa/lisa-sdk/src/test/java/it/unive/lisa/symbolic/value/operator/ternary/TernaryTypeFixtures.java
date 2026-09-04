package it.unive.lisa.symbolic.value.operator.ternary;

import it.unive.lisa.type.BooleanType;
import it.unive.lisa.type.CharacterType;
import it.unive.lisa.type.NumericType;
import it.unive.lisa.type.StringType;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;
import it.unive.lisa.type.Untyped;
import java.util.Collections;
import java.util.Set;

// shared minimal Type/TypeSystem fixtures for meaning-based typeInference()
// tests across this package's operators
class TernaryTypeFixtures {

	private TernaryTypeFixtures() {
	}

	static class FakeStringType
			implements
			StringType {

		public FakeStringType() {
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

	static class FakeCharacterType
			implements
			CharacterType {

		public FakeCharacterType() {
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

	static class FakeBooleanType
			implements
			BooleanType {

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

	static class FakeNumericType
			implements
			NumericType {

		private final boolean integral;

		public FakeNumericType() {
			this(true);
		}

		FakeNumericType(
				boolean integral) {
			this.integral = integral;
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
			return equals(other) ? this : Untyped.INSTANCE;
		}

		@Override
		public Set<Type> allInstances(
				TypeSystem types) {
			return Collections.singleton(this);
		}
	}

	static class FakeOtherType
			implements
			Type {

		public FakeOtherType() {
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

	static class FakeTypeSystem
			extends
			TypeSystem {

		final BooleanType booleanType = new FakeBooleanType();
		final StringType stringType = new FakeStringType();
		final NumericType integerType = new FakeNumericType(true);
		final CharacterType characterType = new FakeCharacterType();

		@Override
		public BooleanType getBooleanType() {
			return booleanType;
		}

		@Override
		public StringType getStringType() {
			return stringType;
		}

		@Override
		public NumericType getIntegerType() {
			return integerType;
		}

		@Override
		public CharacterType getCharacterType() {
			return characterType;
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
