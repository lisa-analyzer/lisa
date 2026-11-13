package it.unive.lisa.program.cfg;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.TestLanguageFeatures;
import it.unive.lisa.TestTypeSystem;
import it.unive.lisa.program.ClassUnit;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.SourceCodeLocation;
import it.unive.lisa.program.annotations.Annotations;
import it.unive.lisa.program.cfg.statement.VariableRef;
import it.unive.lisa.symbolic.value.Variable;
import it.unive.lisa.type.Untyped;
import org.junit.jupiter.api.Test;

public class ParameterTest {

	private static final SourceCodeLocation LOC = new SourceCodeLocation("fake", 0, 0);

	private static CFG cfg() {
		ClassUnit unit = new ClassUnit(LOC, new Program(new TestLanguageFeatures(), new TestTypeSystem()), "u", false);
		return new CFG(new CodeMemberDescriptor(LOC, unit, false, "m"));
	}

	@Test
	public void nameOnlyConstructorDefaultsToUntyped() {
		Parameter p = new Parameter(LOC, "x");
		assertSame(Untyped.INSTANCE, p.getStaticType());
		assertNull(p.getDefaultValue());
	}

	@Test
	public void defaultValueConstructorDerivesTypeFromTheDefaultValue() {
		VariableRef defaultValue = new VariableRef(cfg(), LOC, "d", Untyped.INSTANCE);
		Parameter p = new Parameter(LOC, "x", defaultValue);
		assertSame(defaultValue, p.getDefaultValue());
		assertSame(defaultValue.getStaticType(), p.getStaticType());
	}

	@Test
	public void toSymbolicVariableCarriesNameTypeAndLocation() {
		Parameter p = new Parameter(LOC, "x", Untyped.INSTANCE);
		Variable v = p.toSymbolicVariable();
		assertEquals("x", v.getName());
		assertEquals(Untyped.INSTANCE, v.getStaticType());
		assertEquals(LOC, v.getCodeLocation());
	}

	@Test
	public void equalsAndHashCodeAreBasedOnNameTypeLocationAndAnnotations() {
		Parameter a = new Parameter(LOC, "x", Untyped.INSTANCE);
		Parameter b = new Parameter(LOC, "x", Untyped.INSTANCE);
		assertEquals(a, b);
		assertEquals(a.hashCode(), b.hashCode());

		Parameter differentName = new Parameter(LOC, "y", Untyped.INSTANCE);
		assertFalse(a.equals(differentName));
	}

	// characterization test: the default value expression is intentionally
	// NOT part of equals()/hashCode() (see Parameter's fields vs. its
	// equals()/hashCode() implementation) - two parameters that only differ
	// in their default value are considered the same parameter
	@Test
	public void defaultValueDoesNotAffectEqualsOrHashCode() {
		CFG cfg = cfg();
		VariableRef d1 = new VariableRef(cfg, LOC, "d1", Untyped.INSTANCE);
		VariableRef d2 = new VariableRef(cfg, LOC, "d2", Untyped.INSTANCE);
		Parameter withD1 = new Parameter(LOC, "x", Untyped.INSTANCE, d1, new Annotations());
		Parameter withD2 = new Parameter(LOC, "x", Untyped.INSTANCE, d2, new Annotations());

		assertTrue(withD1.equals(withD2));
		assertEquals(withD1.hashCode(), withD2.hashCode());
	}

}
