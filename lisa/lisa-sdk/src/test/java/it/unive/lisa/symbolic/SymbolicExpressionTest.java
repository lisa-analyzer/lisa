package it.unive.lisa.symbolic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.SyntheticLocation;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.type.Untyped;
import it.unive.lisa.type.VoidType;
import org.junit.jupiter.api.Test;

public class SymbolicExpressionTest {

	@Test
	public void constructorRejectsNullStaticType() {
		assertThrows(NullPointerException.class, () -> new Variable(null, "x", SyntheticLocation.INSTANCE));
	}

	@Test
	public void constructorRejectsNullLocation() {
		assertThrows(NullPointerException.class, () -> new Variable(Untyped.INSTANCE, "x", null));
	}

	@Test
	public void getStaticTypeAndCodeLocationReturnConstructorArguments() {
		SourceCodeLocation loc = new SourceCodeLocation("f", 1, 0);
		Variable v = new Variable(VoidType.INSTANCE, "x", loc);
		assertSame(VoidType.INSTANCE, v.getStaticType());
		assertSame(loc, v.getCodeLocation());
	}

	@Test
	public void equalsIgnoresCodeLocation() {
		// the base SymbolicExpression contract only compares staticType (and
		// whatever subclasses add); the code location must never affect
		// equality, per its own javadoc
		Variable a = new Variable(Untyped.INSTANCE, "x", new SourceCodeLocation("f", 1, 0));
		Variable b = new Variable(Untyped.INSTANCE, "x", new SourceCodeLocation("f", 99, 3));
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
	}

	@Test
	public void equalsRequiresSameStaticTypeAtTheBaseLevel() {
		// Identifier (Variable's superclass) overrides equals() to compare
		// only the name, so use a class whose equality still relies on the
		// SymbolicExpression base (Constant does, since it only adds `value`)
		it.unive.lisa.symbolic.value.Constant a = new it.unive.lisa.symbolic.value.Constant(
				Untyped.INSTANCE, 1, SyntheticLocation.INSTANCE);
		it.unive.lisa.symbolic.value.Constant b = new it.unive.lisa.symbolic.value.Constant(
				VoidType.INSTANCE, 1, SyntheticLocation.INSTANCE);
		assertNotEquals(a, b);
	}

	@Test
	public void equalsIsFalseAcrossDifferentConcreteClasses() {
		Variable v = new Variable(Untyped.INSTANCE, "x", SyntheticLocation.INSTANCE);
		assertFalse(v.equals("x"));
		assertFalse(v.equals(null));
		assertTrue(v.equals(v));
	}

}
