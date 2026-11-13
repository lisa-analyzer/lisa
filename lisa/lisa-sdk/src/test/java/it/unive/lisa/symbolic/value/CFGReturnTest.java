package it.unive.lisa.symbolic.value;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unive.lisa.TestLanguageFeatures;
import it.unive.lisa.TestTypeSystem;
import it.unive.lisa.program.ClassUnit;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.SyntheticLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeMemberDescriptor;
import it.unive.lisa.type.Untyped;
import org.junit.jupiter.api.Test;

public class CFGReturnTest {

	private static CFG cfg(
			String name) {
		ClassUnit unit = new ClassUnit(
				SyntheticLocation.INSTANCE,
				new Program(new TestLanguageFeatures(), new TestTypeSystem()),
				"unit",
				false);
		return new CFG(new CodeMemberDescriptor(SyntheticLocation.INSTANCE, unit, false, name));
	}

	@Test
	public void nameEncodesTheOwningCfg() {
		CFGReturn r = new CFGReturn(cfg("foo"), Untyped.INSTANCE, SyntheticLocation.INSTANCE);
		assertEquals("ret_value@foo", r.getName());
	}

	@Test
	public void isAVariableAndThusScopedLikeOne() {
		// CFGReturn does not override canBeScoped/pushScope/popScope, so it
		// inherits Variable's full scoping behavior
		CFGReturn r = new CFGReturn(cfg("foo"), Untyped.INSTANCE, SyntheticLocation.INSTANCE);
		assertTrue(r instanceof Variable);
		assertTrue(r.canBeScoped());
	}

}
