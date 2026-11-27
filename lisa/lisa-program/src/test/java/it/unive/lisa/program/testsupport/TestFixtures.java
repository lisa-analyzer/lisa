package it.unive.lisa.program.testsupport;

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

// minimal, self-contained lisa-program-only substitutes for the frontend
// classes (e.g. IMPFeatures/IMPTypeSystem in lisa-imp) normally used to build
// a Program: lisa-program cannot depend on any frontend module, so these
// exist purely to let tests build a syntactically valid CFG/Program
public final class TestFixtures {

	private TestFixtures() {
	}

	public static final class TestCharacterType
			implements
			CharacterType {

		public static final TestCharacterType INSTANCE = new TestCharacterType();

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
		public java.util.Set<Type> allInstances(
				it.unive.lisa.type.TypeSystem types) {
			return java.util.Collections.singleton(this);
		}

	}

	public static final class TestTypeSystem
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

	public static final class TestLanguageFeatures
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

	public static final SourceCodeLocation LOCATION = new SourceCodeLocation("test", 1, 1);

	public static final Program PROGRAM = new Program(new TestLanguageFeatures(), new TestTypeSystem());

	public static final ClassUnit UNIT = new ClassUnit(LOCATION, PROGRAM, "TestUnit", false);

	public static final CFG CFG;

	static {
		PROGRAM.addUnit(UNIT);
		CFG = new CFG(new CodeMemberDescriptor(LOCATION, UNIT, false, "test"));
	}

}
