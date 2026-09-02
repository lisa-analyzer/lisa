package it.unive.lisa.program.language.resolution;

import it.unive.lisa.TestLanguageFeatures;
import it.unive.lisa.TestTypeSystem;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeMemberDescriptor;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;
import it.unive.lisa.type.Untyped;
import java.util.Collections;
import java.util.Set;

// shared fixtures for the language.resolution package tests: two unrelated
// leaf types (numberType/stringType) plus a superType that both can be
// assigned to, and a TypeSystem whose distanceBetweenTypes actually
// discriminates between an exact match, an assignable-but-not-exact match,
// and an incomparable pair, since TestTypeSystem's own implementation always
// returns 0 and is therefore useless for exercising
// ParameterMatchingStrategy#distanceFromPerfectTarget
class ResolutionTestFixtures {

	static final Type SUPERTYPE = new SuperType();

	static final Type NUMBER_TYPE = new NumberType();

	static final Type STRING_TYPE = new StringType();

	// a type unrelated to both NUMBER_TYPE and STRING_TYPE, and NOT
	// assignable to SUPERTYPE either - genuinely incomparable with anything
	// else in this fixture set, for exercising the "-1 = incomparable" case
	static final Type UNRELATED_TYPE = new UnrelatedType();

	static Program mkProgram() {
		return new Program(new TestLanguageFeatures(), new DistanceAwareTypeSystem());
	}

	static CFG mkCfg(
			Program p) {
		return new CFG(new CodeMemberDescriptor(new SourceCodeLocation("fake", 0, 0), p, false, "cfg"));
	}

	// each Type fixture is a named static class - rather than an anonymous
	// one - with an explicit public no-arg constructor, so that the
	// AccessModifiers meta-test (which requires every concrete Type to have a
	// visible no-arg constructor) accepts it
	static final class SuperType
			implements
			Type {

		public SuperType() {
		}

		@Override
		public boolean canBeAssignedTo(
				Type other) {
			return other == this;
		}

		@Override
		public Type commonSupertype(
				Type other) {
			return this == other ? this : Untyped.INSTANCE;
		}

		@Override
		public Set<Type> allInstances(
				TypeSystem types) {
			return Collections.singleton(this);
		}

		@Override
		public String toString() {
			return "super";
		}
	}

	static final class NumberType
			implements
			Type {

		public NumberType() {
		}

		@Override
		public boolean canBeAssignedTo(
				Type other) {
			return other == this || other == SUPERTYPE;
		}

		@Override
		public Type commonSupertype(
				Type other) {
			return canBeAssignedTo(other) ? other : Untyped.INSTANCE;
		}

		@Override
		public Set<Type> allInstances(
				TypeSystem types) {
			return Collections.singleton(this);
		}

		@Override
		public String toString() {
			return "number";
		}
	}

	static final class StringType
			implements
			Type {

		public StringType() {
		}

		@Override
		public boolean canBeAssignedTo(
				Type other) {
			return other == this || other == SUPERTYPE;
		}

		@Override
		public Type commonSupertype(
				Type other) {
			return canBeAssignedTo(other) ? other : Untyped.INSTANCE;
		}

		@Override
		public Set<Type> allInstances(
				TypeSystem types) {
			return Collections.singleton(this);
		}

		@Override
		public String toString() {
			return "string";
		}
	}

	static final class UnrelatedType
			implements
			Type {

		public UnrelatedType() {
		}

		@Override
		public boolean canBeAssignedTo(
				Type other) {
			return other == this;
		}

		@Override
		public Type commonSupertype(
				Type other) {
			return this == other ? this : Untyped.INSTANCE;
		}

		@Override
		public Set<Type> allInstances(
				TypeSystem types) {
			return Collections.singleton(this);
		}

		@Override
		public String toString() {
			return "unrelated";
		}
	}

	// 0 for an exact match, 1 for number<->super (assignable but not exact),
	// -1 (incomparable) for number<->string
	private static class DistanceAwareTypeSystem
			extends
			TestTypeSystem {

		@Override
		public int distanceBetweenTypes(
				Type first,
				Type second) {
			if (first == second)
				return 0;
			if ((first == NUMBER_TYPE && second == SUPERTYPE) || (first == SUPERTYPE && second == NUMBER_TYPE))
				return 1;
			if ((first == STRING_TYPE && second == SUPERTYPE) || (first == SUPERTYPE && second == STRING_TYPE))
				return 1;
			return -1;
		}
	}

}
