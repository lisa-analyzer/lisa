package it.unive.lisa.symbolic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

import it.unive.lisa.TestLanguageFeatures;
import it.unive.lisa.TestTypeSystem;
import it.unive.lisa.analysis.ScopeToken;
import it.unive.lisa.program.ClassUnit;
import it.unive.lisa.program.Program;
import it.unive.lisa.program.SyntheticLocation;
import it.unive.lisa.program.cfg.CFG;
import it.unive.lisa.program.cfg.CodeMemberDescriptor;
import it.unive.lisa.type.Untyped;
import org.junit.jupiter.api.Test;

public class CFGThrowTest {

	private static final ScopeToken TOKEN = new ScopeToken(() -> SyntheticLocation.INSTANCE);

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
		CFGThrow t = new CFGThrow(cfg("foo"), Untyped.INSTANCE, SyntheticLocation.INSTANCE);
		assertEquals("thrown@foo", t.getName());
	}

	@Test
	public void cannotBeScopedUnlikeAPlainVariable() {
		// CFGThrow must remain reachable from any caller's catch clauses
		// regardless of call depth, so it overrides Variable's scoping
		CFGThrow t = new CFGThrow(cfg("foo"), Untyped.INSTANCE, SyntheticLocation.INSTANCE);
		assertFalse(t.canBeScoped());
	}

	@Test
	public void pushAndPopScopeAreNoOps() throws Exception {
		CFGThrow t = new CFGThrow(cfg("foo"), Untyped.INSTANCE, SyntheticLocation.INSTANCE);
		assertSame(t, t.pushScope(TOKEN, null));
		assertSame(t, t.popScope(TOKEN, null));
	}

}
