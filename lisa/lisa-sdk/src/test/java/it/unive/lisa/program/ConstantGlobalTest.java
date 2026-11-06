package it.unive.lisa.program;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

import it.unive.lisa.TestLanguageFeatures;
import it.unive.lisa.TestTypeSystem;
import it.unive.lisa.symbolic.value.Constant;
import it.unive.lisa.type.Untyped;
import org.junit.jupiter.api.Test;

public class ConstantGlobalTest {

	private static Unit unit() {
		Program p = new Program(new TestLanguageFeatures(), new TestTypeSystem());
		return new ClassUnit(new SourceCodeLocation("f", 1, 0), p, "fake", false);
	}

	@Test
	public void theStaticTypeIsInheritedFromTheConstant() {
		SourceCodeLocation loc = new SourceCodeLocation("f", 1, 0);
		Constant c = new Constant(Untyped.INSTANCE, 5, loc);
		ConstantGlobal g = new ConstantGlobal(loc, unit(), "x", c);
		assertSame(Untyped.INSTANCE, g.getStaticType());
		assertSame(c, g.getConstant());
		assertFalse(g.isInstance());
	}

	@Test
	public void equalsAndHashCodeAlsoConsiderTheConstant() {
		Unit u = unit();
		SourceCodeLocation loc = new SourceCodeLocation("f", 1, 0);
		ConstantGlobal a = new ConstantGlobal(loc, u, "x", new Constant(Untyped.INSTANCE, 5, loc));
		ConstantGlobal b = new ConstantGlobal(loc, u, "x", new Constant(Untyped.INSTANCE, 5, loc));
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());
		assertFalse(a.equals(new ConstantGlobal(loc, u, "x", new Constant(Untyped.INSTANCE, 6, loc))));
	}

	@Test
	public void toStringPrependsConstAndAppendsTheValue() {
		Unit u = unit();
		SourceCodeLocation loc = new SourceCodeLocation("f", 1, 0);
		Constant c = new Constant(Untyped.INSTANCE, 5, loc);
		ConstantGlobal g = new ConstantGlobal(loc, u, "x", c);
		assertEquals("const " + Untyped.INSTANCE + " " + u.getName() + "#x = " + c, g.toString());
	}

}
