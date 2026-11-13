package it.unive.lisa.symbolic.value.operator.unary;

import it.unive.lisa.type.BooleanType;
import it.unive.lisa.type.CharacterType;
import it.unive.lisa.type.NumericType;
import it.unive.lisa.type.StringType;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;
import it.unive.lisa.type.Untyped;
import java.util.Collections;
import java.util.Set;

// a TypeSystem whose getBooleanType()/getStringType()/getIntegerType()/
// getCharacterType() consistently return the same cached singleton instance
// on every call (unlike TestTypeSystem in it.unive.lisa, which allocates a
// fresh anonymous instance per call, unsuitable for equals()-based Set
// comparisons against typeInference()'s output); the INT/STR/BOOL/CHAR
// fields double as ready-made argument types for typeInference() tests since
// they already satisfy isNumericType()/isStringType()/etc.
//
// named (not anonymous) classes are used here, each with an explicit public
// no-arg constructor, because AccessModifiers#testVisibilityOfTypes reflects
// over every concrete Type on the classpath (test classes included) and
// requires a public/protected no-arg constructor
class FixtureTypeSystem
		extends
		TypeSystem {

	static class BoolTypeImpl
			implements
			BooleanType {

		public BoolTypeImpl() {
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

	static class StringTypeImpl
			implements
			StringType {

		public StringTypeImpl() {
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

	static class IntTypeImpl
			implements
			NumericType {

		public IntTypeImpl() {
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

	static class CharTypeImpl
			implements
			CharacterType {

		public CharTypeImpl() {
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

	// a plain Type matching none of the marker interfaces above, used to
	// exercise the "argument has no compatible type" branch
	static class OtherTypeImpl
			implements
			Type {

		public OtherTypeImpl() {
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

	static final BooleanType BOOL = new BoolTypeImpl();

	static final StringType STR = new StringTypeImpl();

	static final NumericType INT = new IntTypeImpl();

	static final CharacterType CHAR = new CharTypeImpl();

	static final Type OTHER = new OtherTypeImpl();

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
		return INT;
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
