package it.unive.lisa.imp.constructs;

import it.unive.lisa.program.ClassUnit;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeMemberDescriptor;
import it.unive.lisa.program.language.LanguageFeatures;
import it.unive.lisa.program.language.hierarchytraversal.HierarchyTraversalStrategy;
import it.unive.lisa.program.language.hierarchytraversal.SingleInheritanceTraversalStrategy;
import it.unive.lisa.program.language.parameterassignment.OrderPreservingAssigningStrategy;
import it.unive.lisa.program.language.parameterassignment.ParameterAssigningStrategy;
import it.unive.lisa.program.language.resolution.JavaLikeMatchingStrategy;
import it.unive.lisa.program.language.resolution.ParameterMatchingStrategy;
import it.unive.lisa.program.type.BoolType;
import it.unive.lisa.program.type.Int32Type;
import it.unive.lisa.program.type.StringType;
import it.unive.lisa.type.BooleanType;
import it.unive.lisa.type.CharacterType;
import it.unive.lisa.type.NumericType;
import it.unive.lisa.type.Type;
import it.unive.lisa.type.TypeSystem;
import java.util.Collections;
import java.util.Set;

// minimal, self-contained Program/ClassUnit/CFG fixture used to construct
// NativeCFGs and their nested statement classes without needing a real IMP
// source file to be parsed
final class ConstructTestFixtures {

	private ConstructTestFixtures() {
	}

	static final class TestCharacterType
			implements
			CharacterType {

		static final TestCharacterType INSTANCE = new TestCharacterType();

		@Override
		public boolean canBeAssignedTo(
				Type other) {
			return other == this || other.isUntyped();
		}

		@Override
		public Type commonSupertype(
				Type other) {
			return other == this ? this : it.unive.lisa.type.Untyped.INSTANCE;
		}

		@Override
		public Set<Type> allInstances(
				TypeSystem types) {
			return Collections.singleton(this);
		}

	}

	static final class TestTypeSystem
			extends
			TypeSystem {

		@Override
		public BooleanType getBooleanType() {
			return BoolType.INSTANCE;
		}

		@Override
		public it.unive.lisa.type.StringType getStringType() {
			return StringType.INSTANCE;
		}

		@Override
		public NumericType getIntegerType() {
			return Int32Type.INSTANCE;
		}

		@Override
		public CharacterType getCharacterType() {
			return TestCharacterType.INSTANCE;
		}

		@Override
		public boolean canBeReferenced(
				Type type) {
			return true;
		}

		@Override
		public int distanceBetweenTypes(
				Type first,
				Type second) {
			return first.equals(second) ? 0 : -1;
		}

	}

	static final class TestLanguageFeatures
			extends
			LanguageFeatures {

		@Override
		public ParameterMatchingStrategy getMatchingStrategy() {
			return JavaLikeMatchingStrategy.INSTANCE;
		}

		@Override
		public HierarchyTraversalStrategy getTraversalStrategy() {
			return SingleInheritanceTraversalStrategy.INSTANCE;
		}

		@Override
		public ParameterAssigningStrategy getAssigningStrategy() {
			return OrderPreservingAssigningStrategy.INSTANCE;
		}

	}

	static final SourceCodeLocation LOCATION = new SourceCodeLocation("test", 1, 1);

	static final Program PROGRAM = new Program(new TestLanguageFeatures(), new TestTypeSystem());

	static final ClassUnit UNIT = new ClassUnit(LOCATION, PROGRAM, "string", false);

	static final CFG CFG;

	static {
		PROGRAM.addUnit(UNIT);
		CFG = new CFG(new CodeMemberDescriptor(LOCATION, UNIT, false, "test"));
	}

}
