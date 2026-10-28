package it.unive.lisa.program;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.TestLanguageFeatures;
import it.unive.lisa.TestTypeSystem;
import it.unive.lisa.symbolic.value.GlobalVariable;
import it.unive.lisa.type.Untyped;
import org.junit.jupiter.api.Test;

public class GlobalTest {

	private static Unit unit() {
		Program p = new Program(new TestLanguageFeatures(), new TestTypeSystem());
		return new ClassUnit(new SourceCodeLocation("f", 1, 0), p, "fake", false);
	}

	@Test
	public void theShortConstructorDefaultsTheStaticTypeToUntyped() {
		Global g = new Global(new SourceCodeLocation("f", 1, 0), unit(), "x", true);
		assertSame(Untyped.INSTANCE, g.getStaticType());
		assertTrue(g.isInstance());
	}

	@Test
	public void equalsAndHashCodeAreBasedOnAllFields() {
		Unit u = unit();
		SourceCodeLocation loc = new SourceCodeLocation("f", 1, 0);
		Global a = new Global(loc, u, "x", true, Untyped.INSTANCE);
		Global b = new Global(loc, u, "x", true, Untyped.INSTANCE);
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
		assertFalse(a.equals(new Global(loc, u, "y", true, Untyped.INSTANCE)));
		assertFalse(a.equals(new Global(loc, u, "x", false, Untyped.INSTANCE)));
	}

	@Test
	public void toStringCombinesTypeContainerAndName() {
		Unit u = unit();
		Global g = new Global(new SourceCodeLocation("f", 1, 0), u, "x", true, Untyped.INSTANCE);
		assertEquals(Untyped.INSTANCE + " " + u.getName() + "#x", g.toString());
	}

	@Test
	public void toSymbolicVariableCarriesNameTypeAndAnnotations() {
		Unit u = unit();
		Global g = new Global(new SourceCodeLocation("f", 1, 0), u, "x", true, Untyped.INSTANCE);
		SourceCodeLocation where = new SourceCodeLocation("g", 2, 0);
		GlobalVariable var = g.toSymbolicVariable(where);
		assertEquals("x", var.getName());
		assertSame(Untyped.INSTANCE, var.getStaticType());
		assertSame(where, var.getCodeLocation());
	}

}
